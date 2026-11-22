package com.tesseraui

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import com.tesseraui.foundation.TesseraShape
import com.tesseraui.samples.GallerySection
import com.tesseraui.samples.TesseraGallery
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalInspectionMode
import org.junit.Rule
import org.junit.Test

/**
 * 截图测试：在 JVM 上渲染组件总览（拼贴 / 圆融 / 流韵 × 浅色 / 深色 / 增强对比度），不需要模拟器。
 * 录制基准图：gradle :tessera:recordPaparazziDebug
 * 对比基准图：gradle :tessera:verifyPaparazziDebug
 */
class GallerySnapshotTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5.copy(softButtons = false),
        renderingMode = SessionParams.RenderingMode.SHRINK,
        showSystemUi = false,
        maxPercentDifference = 0.1,
    )

    /** 每个分区截 9 张：拼贴 / 圆融 / 流韵 × 浅色 / 深色 / 增强对比度（浅色） */
    private fun shoot(section: GallerySection) {
        for (shape in TesseraShape.entries) for ((dark, hc) in listOf(false to false, true to false, false to true)) {
            val prefix = when (shape) { TesseraShape.Round -> "round-"; TesseraShape.Flow -> "flow-"; else -> "" }
            val theme = when { hc -> "light-hc"; dark -> "dark"; else -> "light" }
            paparazzi.snapshot(name = "$prefix${section.name.lowercase()}-$theme") {
                // 流韵在“移除动画”下会自动改用圆融，所以流韵截图不打开它（静态截图不会触发动效）
                TesseraTheme(darkTheme = dark, highContrast = hc, reduceMotion = shape != TesseraShape.Flow, shape = shape, requireCapableDevice = false) {
                    // 标记为预览：入场动效（结果页图标落下、提示气泡淡入）直接显示落定后的样子
                    CompositionLocalProvider(LocalInspectionMode provides true) { TesseraGallery(section) }
                }
            }
        }
    }

    @Test fun actions() = shoot(GallerySection.Actions)
    @Test fun inputs() = shoot(GallerySection.Inputs)
    @Test fun selection() = shoot(GallerySection.Selection)
    @Test fun navigation() = shoot(GallerySection.Navigation)
    @Test fun display() = shoot(GallerySection.Display)
    @Test fun feedback() = shoot(GallerySection.Feedback)
    // v0.5：表单与输入
    @Test fun moreActions() = shoot(GallerySection.MoreActions)
    @Test fun moreInputs() = shoot(GallerySection.MoreInputs)
    @Test fun range() = shoot(GallerySection.Range)
    @Test fun pickers() = shoot(GallerySection.Pickers)
    @Test fun pickerFields() = shoot(GallerySection.PickerFields)
    @Test fun color() = shoot(GallerySection.Color)
    @Test fun forms() = shoot(GallerySection.Forms)
    // v0.6：数据展示
    @Test fun table() = shoot(GallerySection.Table)
    @Test fun tableCards() = shoot(GallerySection.TableCards)
    @Test fun tree() = shoot(GallerySection.Tree)
    @Test fun details() = shoot(GallerySection.Details)
    @Test fun timeline() = shoot(GallerySection.Timeline)
    @Test fun overlays() = shoot(GallerySection.Overlays)
    @Test fun media() = shoot(GallerySection.Media)
    @Test fun progress() = shoot(GallerySection.Progress)
    @Test fun results() = shoot(GallerySection.Results)
    @Test fun resultsMore() = shoot(GallerySection.ResultsMore)
    // v0.6.1：开发者组件
    @Test fun devTools() = shoot(GallerySection.DevTools)
}
