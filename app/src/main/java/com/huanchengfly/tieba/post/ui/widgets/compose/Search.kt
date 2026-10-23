package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import top.yukonga.miuix.kmp.basic.InputField
import top.yukonga.miuix.kmp.basic.SearchBar
import top.yukonga.miuix.kmp.basic.SearchBarDefaults
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.squircle.squircleClip
import com.huanchengfly.tieba.post.ui.miuix.SquircleCornerRadius
import com.huanchengfly.tieba.post.ui.miuix.rememberSearchBarGlass
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.huanchengfly.tieba.post.LocalHabitSettings
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.activities.VideoViewActivity
import com.huanchengfly.tieba.post.api.models.SearchThreadBean
import com.huanchengfly.tieba.post.ui.models.search.SearchMedia
import com.huanchengfly.tieba.post.ui.models.search.SearchThreadInfo
import com.huanchengfly.tieba.post.ui.page.videoview.VideoViewer
import com.huanchengfly.tieba.post.ui.widgets.compose.morph.MorphAnchor
import com.huanchengfly.tieba.post.ui.widgets.compose.video.VideoThumbnail
import kotlin.math.min

@Composable
fun QuotePostCard(
    quoteContent: AnnotatedString,
    mainPostTitle: AnnotatedString,
    mainPostContent: AnnotatedString?,
    onMainPostClick: () -> Unit,
    modifier: Modifier = Modifier,
    medias: @Composable () -> Unit,
) {
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PbContentText(
            text = quoteContent,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        MainPostCard(
            mainPostTitle = mainPostTitle,
            mainPostContent = mainPostContent,
            modifier = Modifier
                .fillMaxWidth()
                .squircleClip(SquircleCornerRadius.Small)
                .background(MiuixTheme.colorScheme.surfaceContainer)
                .clickable(onClick = onMainPostClick),
            medias = medias,
        )
    }
}

@Composable
fun MainPostCard(
    modifier: Modifier = Modifier,
    mainPostTitle: AnnotatedString,
    mainPostContent: AnnotatedString?,
    medias: @Composable () -> Unit,
) {
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        PbContentText(
            text = mainPostTitle,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        if (!mainPostContent.isNullOrBlank()) {
            PbContentText(
                text = mainPostContent,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        medias()
    }
}

@Composable
fun SearchThreadItem(
    item: SearchThreadInfo,
    onClick: (SearchThreadInfo) -> Unit,
    onValidUserClick: () -> Unit,
    onForumClick: ((SearchThreadBean.ForumInfo, transitionKey: String) -> Unit)?, // Null to hide Forum
    modifier: Modifier = Modifier,
    onQuotePostClick: () -> Unit = {},
    onMainPostClick: () -> Unit = {},
) {
    Card(
        modifier = modifier,
        header = {
            SharedTransitionUserHeader(
                user = item.author,
                extraKey = item.lazyListKey,
                desc = item.timeDesc,
                onClick = onValidUserClick.takeUnless { item.author.id == -1L }
            )
        },
        content = {
            PbContentText(
                text = item.content,
                modifier = Modifier.fillMaxWidth(),
                lineSpacing = 0.5.sp,
                overflow = TextOverflow.Ellipsis,
                maxLines = 3,
                style = MaterialTheme.typography.bodyMedium,
            )

            val medias: @Composable () -> Unit = {
                if (!LocalHabitSettings.current.hideMedia) {
                    if (item.pictures != null) {
                        SearchPhoto(pics = item.pictures)
                    } else if (item.video != null) {
                        SearchVideo(video = item.video)
                    }
                }
            }

            if (item.mainPostTitle != null) {
                val cardModifier = Modifier
                    .fillMaxWidth()
                    .squircleClip(SquircleCornerRadius.Small)
                    .background(MiuixTheme.colorScheme.surfaceContainer)

                if (item.postInfoContent != null) {
                    QuotePostCard(
                        quoteContent = item.postInfoContent,
                        mainPostTitle = item.mainPostTitle,
                        mainPostContent = item.mainPostContent,
                        onMainPostClick = onMainPostClick,
                        modifier = cardModifier.clickable(onClick = onQuotePostClick),
                        medias = medias,
                    )
                } else {
                    MainPostCard(
                        mainPostTitle = item.mainPostTitle,
                        mainPostContent = item.mainPostContent,
                        modifier = cardModifier.clickable(onClick = onMainPostClick),
                        medias = medias,
                    )
                }
            } else {
                medias()
            }
            if (onForumClick != null && item.forumInfo.forumName.isNotEmpty()) {
                ForumInfoChip(
                    forumName = item.forumInfo.forumName,
                    avatarUrl = item.forumInfo.avatar,
                ) {
                    onForumClick(item.forumInfo, item.pid.toString())
                }
            }
        },
        onClick = { onClick(item) },
    )
}

@Composable
private fun SearchVideo(modifier: Modifier = Modifier, video: SearchMedia.Video) {
    val context = LocalContext.current
    val density = LocalDensity.current
    // 容器形变的起点：这一格视频封面在窗口里的矩形。
    var bounds by remember { mutableStateOf(Rect.Zero) }
    VideoThumbnail(
        modifier = modifier
            .fillMaxWidth(singleMediaFraction)
            .aspectRatio(ratio = 2.0f)
            .onGloballyPositioned { bounds = it.boundsInWindow() }
            .squircleClip(SquircleCornerRadius.Small),
        thumbnailUrl = video.thumbnail,
        onClick = {
            val opened = VideoViewer.open(
                context = context,
                videoUrl = video.url,
                thumbnailUrl = video.thumbnail,
                aspectRatio = 2.0f,
                anchor = MorphAnchor(
                    url = video.url,
                    bounds = bounds,
                    cornerRadiusPx = with(density) { 8.dp.toPx() },
                    ratio = 2.0f,
                    thumbUrl = video.thumbnail,
                ),
            )
            if (!opened) VideoViewActivity.launch(context, videoUrl = video.url, thumbnailUrl = video.thumbnail)
        }
    )
}

@Composable
private fun SearchPhoto(modifier: Modifier = Modifier, pics: List<SearchMedia.Picture>) {
    if (pics.isEmpty()) {
        return
    }

    val picCount = pics.size
    val isSinglePhoto = picCount == 1
    val mediaWidthFraction = if (isSinglePhoto) singleMediaFraction else 1f
    val mediaAspectRatio = if (isSinglePhoto) 2f else 3f
    val hasMoreMedia = picCount > MAX_PHOTO_IN_ROW

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth(mediaWidthFraction)
                .aspectRatio(mediaAspectRatio)
                .squircleClip(SquircleCornerRadius.Small),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            for (i in 0 until min(picCount, MAX_PHOTO_IN_ROW)) {
                NetworkImage(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f),
                    imageUrl = pics[i].url,
                )
            }
        }

        if (hasMoreMedia) {
            MediaSizeBadge(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp),
                size = picCount,
            )
        }
    }
}

/**
 * The app's unified search field, built on Miuix [SearchBar] + [InputField].
 *
 * Two states, driven by Miuix itself: collapsed (the capsule with the label) and expanded (an
 * active search session — the field is focused, the label gives way to the query, and the system
 * back gesture collapses instead of leaving the page). The transition between them is Miuix's own
 * animation; nothing custom is needed.
 *
 * `onBack` takes the leading slot (instead of the default magnifier), matching every other page's
 * toolbar layout.
 *
 * With a non-null [backdrop] the capsule becomes glass — the shared
 * [rememberSearchBarGlass] treatment, the same one the home page's entry uses — blurring the
 * content that scrolls beneath it. Layout, focus/keyboard flow and callbacks are untouched: the
 * blur is a draw-only modifier on the field and the capsule fill swap is exactly the
 * `InputField.color` arrangement its doc prescribes. Null (no backdrop recorded, e.g. API < 33)
 * keeps the flat capsule.
 *
 * @param backdrop the page-content [LayerBackdrop] behind the bar, or null for the flat capsule.
 */
@Composable
fun MiuixSearchTopBar(
    keyword: String,
    onKeywordChange: (String) -> Unit,
    onKeywordSubmit: (String) -> Unit = {},
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
    label: String = stringResource(id = R.string.hint_search),
    backdrop: LayerBackdrop? = null,
) {
    // SearchBar treats `expanded` as "search session active": its InputField clears the query and
    // drops focus whenever expanded goes false while focused, so this must track real focus rather
    // than being pinned to false. Focusing the field flips it on via onExpandedChange.
    var expanded by remember { mutableStateOf(false) }
    val glass = rememberSearchBarGlass(backdrop)

    SearchBar(
        inputField = {
            InputField(
                query = keyword,
                onQueryChange = onKeywordChange,
                onSearch = onKeywordSubmit,
                expanded = expanded,
                onExpandedChange = { expanded = it },
                label = label,
                modifier = glass.modifier,
                color = glass.capsuleColor,
                leadingIcon = {
                    Icon(
                        modifier = Modifier
                            .padding(
                                start = SearchBarDefaults.LeadingIconStartPadding,
                                end = SearchBarDefaults.LeadingIconEndPadding,
                            )
                            .clip(CircleShape)
                            .clickable(onClick = onBack),
                        imageVector = MiuixIcons.Back,
                        tint = MiuixTheme.colorScheme.onSurfaceContainerHigh,
                        contentDescription = stringResource(id = R.string.button_back),
                    )
                },
            )
        },
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier.fillMaxWidth(),
    ) {
        // The page renders suggestions and results in its own body, so the bar's expanded slot
        // stays empty: expanding only switches the field into an editing session.
    }
}

