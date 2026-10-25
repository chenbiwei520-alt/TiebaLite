package com.huanchengfly.tieba.post.ui.page.photoview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.huanchengfly.tieba.post.api.TiebaApi
import com.huanchengfly.tieba.post.api.models.PicPageBean
import com.huanchengfly.tieba.post.api.models.bestQualitySrc
import com.huanchengfly.tieba.post.api.models.isLongPic
import com.huanchengfly.tieba.post.api.retrofit.exception.TiebaApiException
import com.huanchengfly.tieba.post.api.retrofit.exception.TiebaException
import com.huanchengfly.tieba.post.arch.TbLiteExceptionHandler
import com.huanchengfly.tieba.post.arch.firstOrThrow
import com.huanchengfly.tieba.post.models.LoadPicPageData
import com.huanchengfly.tieba.post.models.PhotoViewData
import com.huanchengfly.tieba.post.models.PicItem
import com.huanchengfly.tieba.post.utils.TiebaLog
import com.huanchengfly.tieba.post.utils.extension.set
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 看图页的数据源。
 *
 * 旧实现把分页交给 imageviewer 库的 [com.github.iielse.imageviewer.core.DataProvider]，
 * 那两个回调都带着「以光标位置为准」的语义；换成分页器之后只需要知道「现在停在第几张」，
 * 由 [onPageShown] 在接近两端时按需补一页。
 * */
class PhotoViewViewModel : ViewModel() {

    private val _state: MutableStateFlow<PhotoViewUiState> = MutableStateFlow(PhotoViewUiState())
    val state: StateFlow<PhotoViewUiState> get() = _state

    private val handler = TbLiteExceptionHandler(TAG) { _, e, _ ->
        _state.update { it.copy(error = e) }
    }

    private var data: LoadPicPageData? = null

    /**
     * 卡片已知图片的 picId → 地址表。接口返回的同一张图常是另一档 CDN 地址（
     * [com.huanchengfly.tieba.post.api.models.bestQualitySrc] 的 waterUrl/bigCdnSrc 与卡片
     * originUrl 是不同字符串、同一文件）。看图页的内存缓存命中、Pager 页 key、收拢槽位
     * 查找全部按 URL 字符串：列表换代时若把正在显示的那张换成接口地址，重建后的新页缓存
     * 必 miss——空白一帧再淡入（「打开图片闪一下」），收拢也找不到卡片槽（slot MISSING）。
     * 同 picId 一律沿用卡片地址即可让 URL 跨换代稳定。
     * */
    private var cardUrlByPicId: Map<String, String> = emptyMap()

    private fun stabilizeUrls(items: List<PhotoViewItem>): List<PhotoViewItem> {
        if (cardUrlByPicId.isEmpty()) return items
        return items.map { item ->
            val cardUrl = cardUrlByPicId[item.picId] ?: return@map item
            if (cardUrl != item.originUrl) item.copy(originUrl = cardUrl) else item
        }
    }

    private var initialized = false

    @Volatile
    private var loadingNext = false

    @Volatile
    private var loadingPrev = false

    private fun List<PicPageBean.PicBean>.toUniquePhotoViewItems(old: List<PhotoViewItem>): List<PhotoViewItem> {
        val oldDataIds = old.mapTo(HashSet()) { it.picId }
        return this
            .filterNot { oldDataIds.contains(it.img.original.id) }
            .map { it.toPhotoItem() }
    }

    fun initData(viewData: PhotoViewData) {
        if (initialized) return
        initialized = true
        this.data = viewData.data
        cardUrlByPicId = viewData.picItems.associate { it.picId to it.originUrl }

        if (viewData.data == null) {
            _state.set {
                copy(
                    data = viewData.picItems
                        .mapIndexed { i, item -> PhotoViewItem(item = item, overallIndex = i + 1) },
                    totalAmount = viewData.picItems.size,
                    initialIndex = viewData.index
                )
            }
        } else {
            viewModelScope.launch(Dispatchers.Default + handler) {
                val picPageBean = viewData.data
                    .toPageFlow(viewData.data.picId, viewData.data.picIndex, prev = false)
                    .firstOrThrow()

                val stateSnapshot = _state.first()
                val picAmount = picPageBean.picAmount ?: throw TiebaException("加载列表失败, 远古坟贴?")
                val fetchedItems = picPageBean.picList.toUniquePhotoViewItems(old = stateSnapshot.data)
                // 接口给了空列表还往下走，fetchedItems.first() 会直接抛 NoSuchElementException，
                // 这里换成可读的错误，交给统一异常处理弹 toast。
                if (fetchedItems.isEmpty()) throw TiebaException("加载列表失败")
                val firstItemIndex = fetchedItems.first().overallIndex
                val localItems =
                    if (viewData.data.picIndex == 1) emptyList() else viewData.picItems.subList(
                        0,
                        viewData.data.picIndex - 1
                    ).mapIndexed { index, item ->
                        PhotoViewItem(
                            item = item,
                            overallIndex = firstItemIndex - (viewData.data.picIndex - 1 - index),
                        )
                    }
                val items = stabilizeUrls(localItems + fetchedItems)
                val hasNext = items.last().overallIndex < picAmount
                val hasPrev = items.first().overallIndex > 1
                val initialIndex: Int? = items
                    .indexOfFirst { it.picId == viewData.data.picId }
                    .takeIf { it != -1 }
                val newState = PhotoViewUiState(
                    data = items.toImmutableList(),
                    hasNext = hasNext,
                    hasPrev = hasPrev,
                    totalAmount = picAmount,
                    initialIndex = initialIndex ?: (viewData.data.picIndex - 1),
                )

                TiebaLog.d(
                    "PhotoViewVM",
                    "picAmount=" + picAmount + " fetched=" + fetchedItems.size +
                        " items=" + items.size + " openedPicId=" + viewData.data.picId +
                        " initialIndex=" + newState.initialIndex +
                        " ids=" + items.take(6).map { it.picId }
                )

                withContext(Dispatchers.Main.immediate) {
                    _state.set { newState }
                }
            }
        }
    }

    /**
     * 分页器停在 [index] 时调用：快滑到尽头时提前把下一页取回来。
     * */
    fun onPageShown(index: Int) {
        val uiState = _state.value
        if (uiState.data.isEmpty()) return
        if (uiState.hasNext && (index >= uiState.data.lastIndex - 1 || index < 0)) {
            loadNext()
        }
        if (uiState.hasPrev && index <= 1) {
            loadPrev()
        }
    }

    private fun loadNext() {
        if (loadingNext) return
        val loadData = data ?: return
        val last = _state.value.data.lastOrNull() ?: return

        loadingNext = true
        viewModelScope.launch(Dispatchers.Default + handler) {
            try {
                val bean = loadData.toPageFlow(last.picId, last.overallIndex, prev = false)
                    .retryWhen { cause, attempt -> cause !is TiebaApiException && attempt < 3 }
                    .firstOrThrow()

                val picAmount = bean.picAmount ?: throw TiebaException("加载列表失败, 远古坟贴?")
                val uniqueItems = bean.picList.toUniquePhotoViewItems(_state.value.data)
                val hasNext = bean.picList.last().overAllIndex.toInt() < picAmount && uniqueItems.isNotEmpty()

                withContext(Dispatchers.Main.immediate) {
                    _state.set {
                        copy(
                            data = (data + stabilizeUrls(uniqueItems)).toImmutableList(),
                            hasNext = hasNext,
                            totalAmount = picAmount,
                        )
                    }
                    loadingNext = false
                }
            } catch (e: Throwable) {
                loadingNext = false
                throw e
            }
        }
    }

    private fun loadPrev() {
        if (loadingPrev) return
        val loadData = data ?: return
        val first = _state.value.data.firstOrNull() ?: return

        loadingPrev = true
        viewModelScope.launch(Dispatchers.Default + handler) {
            try {
                val bean = loadData.toPageFlow(first.picId, first.overallIndex, prev = true)
                    .retryWhen { cause, attempt -> cause !is TiebaApiException && attempt < 3 }
                    .firstOrThrow()

                val hasPrev = bean.picList.first().overAllIndex.toInt() > 1
                val uniqueItems = bean.picList.toUniquePhotoViewItems(old = _state.value.data)

                withContext(Dispatchers.Main.immediate) {
                    if (uniqueItems.isEmpty()) {
                        _state.set { copy(hasPrev = false) }
                    } else {
                        _state.set {
                            copy(
                                data = (stabilizeUrls(uniqueItems) + data).toImmutableList(),
                                hasPrev = hasPrev,
                            )
                        }
                    }
                    loadingPrev = false
                }
            } catch (e: Throwable) {
                loadingPrev = false
                throw e
            }
        }
    }

    companion object {
        private const val TAG = "PhotoViewViewModel"

        private fun LoadPicPageData.toPageFlow(picId: String, picIndex: Int, prev: Boolean): Flow<PicPageBean> {
            return TiebaApi.getInstance().picPageFlow(
                forumId = forumId.toString(),
                forumName = forumName,
                threadId = threadId.toString(),
                seeLz = seeLz,
                picId = picId,
                picIndex = picIndex.toString(),
                objType = objType,
                prev = prev
            )
        }

        /**
         * 中等档的地址：原图太大（或还没决定要不要下原图）时，先用这一张把屏幕铺满。
         *
         * 接口给了 medium / screen 两档，能取到哪张用哪张；都没有就返回 null，由调用方退回缩略图。
         * */
        private fun PicPageBean.ImgBean.mediumSrc(): String? {
            // screen 比 medium 大、也更接近屏幕比例，先取它；解析不到再退到 medium。
            return (screen?.url?.takeIf { it.isNotBlank() } ?: medium?.url?.takeIf { it.isNotBlank() })
                ?.let { url -> if (url.startsWith("http:")) url.replaceFirst("http:", "https:") else url }
        }

        private fun PicPageBean.PicBean.toPhotoItem(): PhotoViewItem {
            val width = img.original.width?.toFloatOrNull() ?: 0f
            val height = img.original.height?.toFloatOrNull() ?: 0f
            val aspectRatio = if (width > 0f && height > 0f) width / height else 1f

            return PhotoViewItem(
                picId = img.original.id,
                originUrl = img.bestQualitySrc,
                overallIndex = overAllIndex.toInt(),
                postId = postId?.toLongOrNull(),
                aspectRatio = aspectRatio,
                isLongPic = img.original.isLongPic(),
                thumbUrl = img.mediumSrc(),
            )
        }
    }
}

data class PhotoViewUiState(
    val data: List<PhotoViewItem> = persistentListOf(),
    val totalAmount: Int = 0,
    val hasNext: Boolean = false,
    val hasPrev: Boolean = false,
    val initialIndex: Int = 0,
    val error: Throwable? = null,
)

data class PhotoViewItem(
    val picId: String,
    val originUrl: String,
    val overallIndex: Int,
    val postId: Long? = null,
    /** 原图宽高比，用于长图按整宽铺满（一屏看不完要能上下拖）。 */
    val aspectRatio: Float = 1f,
    val isLongPic: Boolean = false,
    /**
     * 中等档（接口的 medium / screen）地址，先铺满屏幕用。
     *
     * 原图还没到手时画它，原图到了再淡入盖上去；原图体积过大、需要用户手点「查看原图」时，
     * 它也是那段时间里唯一能看的内容。为 null 时退回卡牌缩略图。
     * */
    val thumbUrl: String? = null,
) {

    constructor(item: PicItem, overallIndex: Int) : this(
        picId = item.picId,
        originUrl = item.originUrl,
        overallIndex = overallIndex,
        postId = item.postId,
    )
}