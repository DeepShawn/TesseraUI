package com.tesseraui.sample
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.unit.dp
import com.tesseraui.TesseraTheme
import com.tesseraui.foundation.TesseraShape
import com.tesseraui.samples.GallerySection
import com.tesseraui.samples.TesseraGallery
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * 无障碍检查（JVM 上用 Robolectric 运行，不需要模拟器）：三种外观 × 6 个分区，
 * 每个可点击、可切换的元素都要有读屏名称（文字、内容描述或状态描述），触摸区域不小于 44dp。
 * 触摸区域按 touchBoundsInRoot 计算：Compose 会把小于 48dp 的可点击元素的触摸范围自动扩展到 48dp（不改变外观），
 * 只有被相邻元素挡住、扩展不开时才会不足。
 * 同样 18 个画面（三种外观 × 6 个分区）的截图由 tessera 模块的 Paparazzi 测试生成。
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w420dp-h3000dp-xhdpi", sdk = [34])
class AccessibilityTest {
    @get:Rule val rule = createComposeRule()

    private fun SemanticsNode.label(): String? {
        val c = config
        c.getOrNull(SemanticsProperties.ContentDescription)?.joinToString()?.takeIf { it.isNotBlank() }?.let { return it }
        c.getOrNull(SemanticsProperties.Text)?.joinToString()?.takeIf { it.isNotBlank() }?.let { return it }
        // 合并后的子节点文字（按钮里的文字、带标签的开关）
        fun collect(n: SemanticsNode): String = (n.config.getOrNull(SemanticsProperties.Text)?.joinToString() ?: "") +
            (n.config.getOrNull(SemanticsProperties.ContentDescription)?.joinToString() ?: "") + n.children.joinToString("") { collect(it) }
        return collect(this).takeIf { it.isNotBlank() }
    }

    @Test fun everyInteractiveNodeHasNameAndTouchTarget() {
        var shape by mutableStateOf(TesseraShape.Tile)
        var section by mutableStateOf(GallerySection.Actions)
        rule.setContent { TesseraTheme(darkTheme = false, highContrast = false, reduceMotion = true, shape = shape, requireCapableDevice = false) { TesseraGallery(section) } }
        val problems = mutableListOf<String>()
        var checked = 0
        for (s in TesseraShape.entries) for (sec in GallerySection.entries) {
            shape = s; section = sec
            rule.waitForIdle()
            val nodes = rule.onAllNodes(SemanticsMatcher("可交互") { it.config.contains(SemanticsActions.OnClick) || it.config.contains(SemanticsProperties.ToggleableState) || it.config.contains(SemanticsProperties.Selected) })
                .fetchSemanticsNodes()
            val density = rule.density
            checked += nodes.size
            for (n in nodes) {
                val name = n.label()
                val w = with(density) { n.touchBoundsInRoot.width.toDp() }
                val h = with(density) { n.touchBoundsInRoot.height.toDp() }
                if (name == null) problems += "$s/$sec：可交互元素没有读屏名称（${n.boundsInRoot}）"
                if (w < 44.dp || h < 44.dp) problems += "$s/$sec：“${name ?: "?"}”触摸区域只有 ${w.value.toInt()}×${h.value.toInt()}dp"
            }
        }
        println("无障碍检查：共检查 $checked 个可交互元素")
        assertTrue("没有找到可交互元素，检查本身失效", checked > 60)
        assertTrue(problems.distinct().joinToString("\n"), problems.isEmpty())
    }
}
