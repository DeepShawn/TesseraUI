package com.tesseraui.sample

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import com.tesseraui.TesseraTheme
import com.tesseraui.components.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** v0.5 表单与输入组件的交互测试（Robolectric，JVM 上运行） */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w420dp-h2000dp-xhdpi", sdk = [34])
class FormInteractionTest {
    @get:Rule val rule = createComposeRule()
    private fun content(block: @androidx.compose.runtime.Composable () -> Unit) =
        rule.setContent { TesseraTheme(darkTheme = false, highContrast = false, reduceMotion = true) { block() } }

    @Test fun numberInputStepsClampsAndAcceptsFullWidth() {
        var v by mutableDoubleStateOf(98.0)
        content { TesseraNumberInput(v, { v = it }, label = "数量", min = 1.0, max = 99.0) }
        rule.onNodeWithContentDescription("增加").performSemanticsAction(SemanticsActions.OnClick)
        assertEquals(99.0, v, 0.0)
        // 到上限后“增加”变为不可用（读屏读作“已停用”）
        rule.onNodeWithContentDescription("增加").assertIsNotEnabled()
        rule.onNodeWithContentDescription("减少").performClick()
        assertEquals(98.0, v, 0.0)
        // 按住 + 连续增加（先停 400ms，然后每 120ms 一次）
        v = 10.0; rule.waitForIdle()
        rule.mainClock.autoAdvance = false
        rule.onNodeWithContentDescription("增加").performTouchInput { down(center) }
        rule.mainClock.advanceTimeBy(400 + 120 * 5 + 60)
        rule.onNodeWithContentDescription("增加").performTouchInput { up() }
        rule.mainClock.autoAdvance = true
        assertTrue("按住没有连续增加：$v", v >= 15.0)
        // 全角数字
        rule.onNodeWithContentDescription("数量").performTextReplacement("４２")
        rule.onNodeWithContentDescription("数量").performSemanticsAction(SemanticsActions.OnImeAction)
        assertEquals(42.0, v, 0.0)
    }

    @Test fun tagInputSplitsOnSeparatorsAndSkipsDuplicates() {
        var tags by mutableStateOf(listOf("设计"))
        content { TesseraTagInput(tags, { tags = it }, label = "标签") }
        rule.onNodeWithContentDescription("标签").performTextInput("动效，")
        rule.onNodeWithContentDescription("标签").performTextInput("设计、")
        rule.onNodeWithContentDescription("标签").performTextInput("组件")
        rule.onNodeWithContentDescription("标签").performSemanticsAction(SemanticsActions.OnImeAction)
        assertEquals(listOf("设计", "动效", "组件"), tags)
        rule.onNodeWithContentDescription("删除标签 动效").performClick()
        assertEquals(listOf("设计", "组件"), tags)
    }

    @Test fun otpPasteFillsAndCompletes() {
        var code by mutableStateOf("")
        var done = ""
        content { TesseraOtpInput(code, { code = it }, onComplete = { done = it }) }
        rule.onNodeWithContentDescription("验证码，共 6 位").performTextReplacement("验证码：８２６ 014，5 分钟内有效")
        assertEquals("826014", code)
        assertEquals("826014", done)
    }

    @Test fun ratingTapsAndClears() {
        var r by mutableIntStateOf(0)
        content { TesseraRating(r, { r = it }) }
        rule.onNodeWithContentDescription("4 星").performClick(); assertEquals(4, r)
        rule.onNodeWithContentDescription("4 星").performClick(); assertEquals("点同一颗星清零", 0, r)
    }

    @Test fun buttonGroupMultiSelect() {
        var sel by mutableStateOf(setOf(0))
        content { TesseraButtonGroup(listOf("加粗", "斜体", "下划线"), sel, { sel = it }, multiple = true) }
        rule.onNodeWithText("下划线").performClick()
        rule.onNodeWithText("加粗").performClick()
        assertEquals(setOf(2), sel)
    }

    @Test fun sliderAndWheelAdjustByAccessibilityAction() {
        var vol by mutableFloatStateOf(50f)
        var idx by mutableIntStateOf(3)
        content {
            androidx.compose.foundation.layout.Column {
                TesseraSlider(vol, { vol = it }, label = "音量", step = 10f)
                TesseraWheel(listOf("北京", "上海", "广东", "浙江", "江苏"), idx, { idx = it }, label = "省份")
            }
        }
        rule.onNodeWithContentDescription("音量").performSemanticsAction(SemanticsActions.SetProgress) { it(73f) }
        assertEquals("按步长吸附", 70f, vol, 0f)
        rule.onNodeWithContentDescription("省份").performSemanticsAction(SemanticsActions.SetProgress) { it(1f) }
        rule.waitForIdle()
        assertEquals(1, idx)
    }

    @Test fun calendarRangePick() {
        var s by mutableStateOf<TesseraDate?>(null)
        var e by mutableStateOf<TesseraDate?>(null)
        content { TesseraCalendar(rangeStart = s, rangeEnd = e, onRangeChange = { a, b -> s = a; e = b }, initialMonth = TesseraDate(2026, 11, 1), today = TesseraDate(2026, 11, 19)) }
        rule.onNode(hasContentDescription("2026 年 11 月 27 日", substring = true)).performClick()
        rule.onNode(hasContentDescription("2026 年 11 月 24 日", substring = true)).performClick()
        assertEquals("先点的晚、后点的早，自动调换", TesseraDate(2026, 11, 24), s)
        assertEquals(TesseraDate(2026, 11, 27), e)
    }

    @Test fun formSubmitShowsChineseErrors() {
        val form = TesseraFormState()
        var name by mutableStateOf("")
        var errorShown: String? = "unset"
        content {
            val err = form.field("name", name, TesseraRules.required("姓名"))
            errorShown = err
            TesseraTextField(name, { name = it }, label = "姓名", error = err)
        }
        assertEquals("提交前不提前报错", null, errorShown)
        rule.runOnIdle { assertEquals("name", form.submit()) }
        rule.waitForIdle()
        rule.onNodeWithText("请填写姓名").assertExists()
        rule.onNodeWithContentDescription("姓名").performTextInput("林")
        rule.waitForIdle()
        assertEquals(null, errorShown)
    }
}
