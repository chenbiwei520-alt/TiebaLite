package com.huanchengfly.tieba.post.ui.page.user.followlist

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.toRoute
import com.huanchengfly.tieba.post.api.models.FansListBean.FanUserBean
import com.huanchengfly.tieba.post.api.retrofit.exception.getErrorMessage
import com.huanchengfly.tieba.post.arch.BaseStateViewModel
import com.huanchengfly.tieba.post.arch.CommonUiEvent
import com.huanchengfly.tieba.post.arch.UiState
import com.huanchengfly.tieba.post.repository.UserProfileRepository
import com.huanchengfly.tieba.post.ui.models.user.ConcernType
import com.huanchengfly.tieba.post.ui.models.user.FollowUser
import com.huanchengfly.tieba.post.ui.page.Destination
import com.huanchengfly.tieba.post.utils.StringUtil
import com.huanchengfly.tieba.post.utils.extension.set
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import javax.inject.Inject

@Immutable
data class FansListUiState(
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: Throwable? = null,
    val currentPage: Int = 1,
    val hasMore: Boolean = false,
    val totalCount: Int = 0,
    val users: List<FollowUser> = emptyList(),
) : UiState

@HiltViewModel
class FansListViewModel @Inject constructor(
    @param:ApplicationContext val context: Context,
    private val userProfileRepo: UserProfileRepository,
    savedStateHandle: SavedStateHandle
) : BaseStateViewModel<FansListUiState>() {

    override val errorHandler = CoroutineExceptionHandler { _, e ->
        Log.e(TAG, "onError: ", e)
        _uiState.update { it.copy(isRefreshing = false, isLoadingMore = false, error = e) }
    }

    private val param = savedStateHandle.toRoute<Destination.UserFansList>()
    val uid: Long = param.uid

    override fun createInitialState(): FansListUiState = FansListUiState(isRefreshing = true)

    init {
        refreshInternal()
    }

    private fun refreshInternal() {
        _uiState.set { createInitialState() }
        launchInVM {
            val result = userProfileRepo.loadFansList(uid, page = 1)
            val users = result.userList.mapToUiModel()
            _uiState.set {
                FansListUiState(
                    currentPage = 1,
                    hasMore = (result.page?.hasMore ?: 0) == 1,
                    totalCount = result.page?.totalCount ?: users.size,
                    users = users,
                )
            }
        }
    }

    fun onRefresh() {
        if (!currentState.isRefreshing) refreshInternal()
    }

    fun onLoadMore() {
        val oldState = currentState
        if (oldState.isLoadingMore || oldState.isRefreshing) return

        _uiState.update { it.copy(isLoadingMore = true) }
        launchInVM {
            runCatching {
                userProfileRepo.loadFansList(uid, page = oldState.currentPage + 1)
            }
            .onFailure { e ->
                _uiState.update { it.copy(isRefreshing = false, isLoadingMore = false, error = null) }
                sendUiEvent(CommonUiEvent.ToastError(e))
            }
            .onSuccess { result ->
                val fans = result.userList.mapToUiModel()
                val users = withContext(Dispatchers.Default) {
                    (oldState.users + fans).distinctBy { it.uid }
                }
                _uiState.update {
                    it.copy(
                        isLoadingMore = false,
                        currentPage = result.page?.currentPage ?: (oldState.currentPage + 1),
                        hasMore = (result.page?.hasMore ?: 0) == 1,
                        users = users
                    )
                }
            }
        }
    }

    fun onFollowClicked(user: FollowUser) = launchInVM {
        updateConcertTypeInternal(user, ConcernType.MUTUAL) {
            userProfileRepo.requestFollowUser(uid = user.uid, portrait = user.portrait)
        }
        .onFailure { e ->
            sendUiEvent(FollowListUiEvent.FollowFailed(message = e.getErrorMessage()))
        }
    }

    fun onUnfollowClicked(user: FollowUser) = launchInVM {
        updateConcertTypeInternal(user, ConcernType.FANS) {
            userProfileRepo.requestUnfollowUser(uid = user.uid, portrait = user.portrait)
        }
        .onFailure { e ->
            sendUiEvent(FollowListUiEvent.UnfollowFailed(message = e.getErrorMessage()))
        }
    }

    private suspend fun <T> updateConcertTypeInternal(
        user: FollowUser,
        newConcernType: Int,
        block: suspend () -> T,
    ): Result<T> {
        val oldConcernType = user.concernType
        _uiState.update {
            it.copy(users = it.users.updateConcernType(uid = user.uid, type = ConcernType.UPDATING))
        }
        return runCatching { block() }
            .onFailure { e ->
                Log.w(TAG, "onUpdateConcertTypeInternal", e)
                _uiState.update {
                    it.copy(users = it.users.updateConcernType(uid = user.uid, type = oldConcernType))
                }
            }
            .onSuccess {
                _uiState.update {
                    it.copy(users = it.users.updateConcernType(uid = user.uid, type = newConcernType))
                }
            }
    }

    companion object {
        private const val TAG = "FansListViewModel"

        private suspend fun List<FanUserBean>.mapToUiModel(): List<FollowUser> = if (isNotEmpty()) {
            withContext(Dispatchers.Default) {
                map {
                    FollowUser(
                        uid = it.id,
                        avatar = StringUtil.getAvatarUrl(it.portrait),
                        displayName = StringUtil.getUserNameString(
                            showBoth = true,
                            username = it.name.orEmpty(),
                            nickname = it.nameShow
                        ),
                        portrait = it.portrait!!,
                        intro = it.intro?.takeUnless { intro -> intro.isEmpty() || intro.isBlank() },
                        // In the fans list every entry follows the list owner; has_concerned marks
                        // whether the owner follows them back (mutual)
                        concernType = if (it.hasConcerned == 1) ConcernType.MUTUAL else ConcernType.FANS,
                    )
                }
            }
        } else {
            emptyList()
        }

        private suspend fun List<FollowUser>.updateConcernType(
            uid: Long,
            @ConcernType type: Int
        ): List<FollowUser> = withContext(Dispatchers.Default) {
            map {
                if (it.uid != uid) it else it.copy(concernType = type)
            }
        }
    }
}
