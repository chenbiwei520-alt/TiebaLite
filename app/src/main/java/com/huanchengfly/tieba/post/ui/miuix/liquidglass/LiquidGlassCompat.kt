package com.huanchengfly.tieba.post.ui.miuix.liquidglass

import androidx.compose.runtime.compositionLocalOf
import com.kyant.backdrop.Backdrop

/*
 * 被 vendor 进来的参考弹窗（ListPopup/ListPopupLayout/OverlayListPopup）原先依赖参考 App 自己的
 * 工具模块（拖选、材质档、edgeLight），这些文件已按参考原样一起 vendor 到本包：
 * DropdownPanelDragSelect.kt / AppMaterialSettings.kt / EdgeLight*.kt / InteractiveHighlight.kt，
 * 所以这里只剩「弹窗该采样哪一层」这一条本项目胶水。
 */

/**
 * 参考弹窗（vendor 进来的 OverlayListPopup/ListPopupLayout）采样的 Kyant 页面层。

 * 与 `ui.miuix.LocalLiquidGlassBackdrop` 分开：那个是 Miuix 模糊管线的捕获层
 * （`textureBlur`），被 vendor 的参考弹窗走的是 Kyant 的 `com.kyant.backdrop.drawBackdrop`
 * + `effects.blur`/`vibrancy`，需要一个 Kyant `LayerBackdrop`。两库各录制各的层，
 * 所以页面必须同时提供两个 CompositionLocal。
 */
val LocalLiquidGlassPopupBackdrop = compositionLocalOf<Backdrop?> { null }
