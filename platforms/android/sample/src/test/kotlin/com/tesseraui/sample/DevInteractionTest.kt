package com.tesseraui.sample

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.withKeyDown
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.tesseraui.TesseraTheme
import com.tesseraui.TesseraLightColors
import com.tesseraui.components.*
import com.tesseraui.samples.sampleBytes
import com.tesseraui.samples.sampleHexRanges
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** v0.6.1 开发者组件（代码编辑器、十六进制查看）的交互测试 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w420dp-h2000dp-xhdpi", sdk = [34])
class DevInteractionTest {
    @get:Rule val rule = createComposeRule()
    private fun content(block: @androidx.compose.runtime.Composable () -> Unit) =
        rule.setContent { TesseraTheme(darkTheme = false, highContrast = false, reduceMotion = true) { block() } }

    private fun tfv(text: String, sel: Int = text.length, end: Int = sel) = TextFieldValue(text, TextRange(sel, end))

    @Test fun indentOutdentAndBlockIndent() {
        val unit = "  "
        assertEquals("  ab", tesseraIndent(tfv("ab", 0), unit).text)
        assertEquals("ab", tesseraOutdent(tfv("  ab", 4), unit).text)
        // 选中两行：整块缩进，再整块反缩进
        val two = "a\nb\nc"
        val indented = tesseraIndent(tfv(two, 0, 3), unit)
        assertEquals("  a\n  b\nc", indented.text)
        assertEquals(two, tesseraOutdent(indented, unit).text)
    }

    @Test fun enterKeepsIndentAndOpensBlock() {
        val unit = "  "
        // 普通行：保持缩进
        val old = tfv("  let x = 1", 11)
        assertEquals("  let x = 1\n  ", tesseraAutoIndent(old, tfv("  let x = 1\n", 12), unit).text)
        // { 之后多缩一级；光标在括号中间时右括号放到下一行
        val o2 = tfv("if (x) {}", 8)
        val r = tesseraAutoIndent(o2, tfv("if (x) {\n}", 9), unit)
        assertEquals("if (x) {\n  \n}", r.text)
        assertEquals(TextRange(11), r.selection)
        // 输入法组合中不处理
        val composing = TextFieldValue("你\n", TextRange(2), composition = TextRange(0, 1))
        assertEquals(composing, tesseraAutoIndent(tfv("你", 1), composing, unit))
    }

    @Test fun highlightColorsKeywordsStringsAndComments() {
        val a = tesseraHighlight("val s = \"hi\" // note", "kotlin", TesseraLightColors)
        assertEquals("val s = \"hi\" // note", a.text)
        val styles = a.spanStyles.map { a.text.substring(it.start, it.end) }
        assertTrue(styles.toString(), "val" in styles && "\"hi\"" in styles && "// note" in styles)
        assertTrue(tesseraHighlight("whatever", "unknown", TesseraLightColors).spanStyles.isEmpty())
    }

    @OptIn(ExperimentalTestApi::class)
    @Test fun editorTabIndentsAndEscReleasesFocus() {
        var v by mutableStateOf(tfv("ab", 0))
        content {
            androidx.compose.foundation.layout.Column {
                TesseraCodeEditor(v, { v = it }, language = "kotlin", label = "脚本", diagnostics = listOf(TesseraDiagnostic(1, "示例错误")))
                androidx.compose.foundation.text.BasicTextField("", {}, androidx.compose.ui.Modifier.semantics { contentDescription = "下一个控件" })
            }
        }
        val field = rule.onNodeWithContentDescription("脚本")
        field.assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Error))
        field.requestFocus()
        field.performKeyInput { pressKey(Key.Tab) }
        assertEquals("  ab", v.text)
        field.performKeyInput { withKeyDown(Key.ShiftLeft) { pressKey(Key.Tab) } }
        assertEquals("ab", v.text)
        // Esc 之后 Tab 不再插入缩进（也不插入制表符），焦点移出编辑器
        field.performKeyInput { pressKey(Key.Escape); pressKey(Key.Tab) }
        assertEquals("ab", v.text)
        field.assert(SemanticsMatcher.expectValue(SemanticsProperties.Focused, false))
        rule.onNodeWithContentDescription("下一个控件").assert(SemanticsMatcher.expectValue(SemanticsProperties.Focused, true))
        rule.onNodeWithText("示例错误").assertIsDisplayed()
    }

    @Test fun hexDescribeMatchesWeb() {
        assertEquals("偏移 0x1（1）：0x50 = 80 = 0b01010000，字符“P”，PNG 签名", tesseraHexDescribe(sampleBytes, 1, 1, 1, sampleHexRanges))
        assertEquals("偏移 0x5（5）：0x0A = 10 = 0b00001010，PNG 签名，已选 3 个字节（3–5）", tesseraHexDescribe(sampleBytes, 3, 5, 5, sampleHexRanges))
        assertEquals(489, sampleBytes.size)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test fun hexTapSelectsKeyboardMovesAndCopies() {
        var picked: IntRange? = null
        var cm: ClipboardManager? = null
        content {
            cm = LocalClipboardManager.current
            TesseraHexView(sampleBytes, highlights = sampleHexRanges, label = "字节", onSelect = { picked = it })
        }
        // 第一行：点按第 2 个字节（0x50）
        rule.onNodeWithContentDescription("偏移 00000000", substring = true).performTouchInput { click(androidx.compose.ui.geometry.Offset((64 + 21 * 1.5f) * density, centerY)) }
        assertEquals(1..1, picked)
        rule.onNodeWithText("偏移 0x1（1）", substring = true).assertIsDisplayed()
        // 方向键移动、Shift 扩展
        val grid = rule.onNodeWithContentDescription("字节")
        grid.requestFocus()
        grid.performKeyInput { pressKey(Key.DirectionRight); withKeyDown(Key.ShiftLeft) { pressKey(Key.DirectionRight) } }
        assertEquals(2..3, picked)
        rule.onNodeWithContentDescription("复制选中的字节").performClick()
        assertEquals("4E 47", cm?.getText()?.text)
        rule.onNodeWithText("已复制 2 个字节").assertIsDisplayed()
    }
}
