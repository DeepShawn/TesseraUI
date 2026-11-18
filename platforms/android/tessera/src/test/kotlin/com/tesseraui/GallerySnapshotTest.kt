package com.tesseraui

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import com.tesseraui.foundation.TesseraShape
import com.tesseraui.samples.GallerySection
import com.tesseraui.samples.TesseraGallery
import org.junit.Rule
import org.junit.Test

/**
 * 截图测试：在 JVM 上渲染组件总览（拼贴 / 圆融 × 浅色 / 深色），不需要模拟器。
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

    /** 每个分区截 4 张：拼贴 / 圆融 × 浅色 / 深色 */
    private fun shoot(section: GallerySection) {
        for (shape in TesseraShape.entries) for (dark in listOf(false, true)) {
            val prefix = if (shape == TesseraShape.Round) "round-" else ""
            paparazzi.snapshot(name = "$prefix${section.name.lowercase()}-${if (dark) "dark" else "light"}") {
                TesseraTheme(darkTheme = dark, highContrast = false, reduceMotion = true, shape = shape) { TesseraGallery(section) }
            }
        }
    }

    @Test fun actions() = shoot(GallerySection.Actions)
    @Test fun inputs() = shoot(GallerySection.Inputs)
    @Test fun selection() = shoot(GallerySection.Selection)
    @Test fun navigation() = shoot(GallerySection.Navigation)
    @Test fun display() = shoot(GallerySection.Display)
    @Test fun feedback() = shoot(GallerySection.Feedback)
}
