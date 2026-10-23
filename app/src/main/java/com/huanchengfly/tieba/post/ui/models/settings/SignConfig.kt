package com.huanchengfly.tieba.post.ui.models.settings

import androidx.compose.runtime.Immutable
import com.huanchengfly.tieba.post.utils.HmTime

@Immutable
data class SignConfig(
    val autoSign: Boolean = false,
    val autoSignSlow: Boolean = true,
    val autoSignTime: HmTime = randomSignTime(),
    val okSignOfficial: Boolean = true,
    /**
     * 单个吧签到失败是否中止后续签到。
     *
     * 默认开启（同 fork 的 `oksign_fail_auto_stop`）：一个吧失败通常意味着会话/tbs 已失效，
     * 继续签下去只会为每个吧都留一条失败通知。
     */
    val failAutoStop: Boolean = true,
)

fun randomSignTime(): HmTime = HmTime(hourOfDay = (9..16).random(), minute = 0) // 9:00--16:00