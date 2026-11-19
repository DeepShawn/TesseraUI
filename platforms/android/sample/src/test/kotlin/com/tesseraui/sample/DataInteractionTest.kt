package com.tesseraui.sample

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasStateDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.tesseraui.TesseraText
import com.tesseraui.TesseraTheme
import com.tesseraui.components.*
import com.tesseraui.samples.sampleColumns
import com.tesseraui.samples.sampleOrders
import com.tesseraui.samples.sampleTree
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** v0.6 数据展示组件的交互测试（Robolectric，JVM 上运行） */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w420dp-h2000dp-xhdpi", sdk = [34])
class DataInteractionTest {
    @get:Rule val rule = createComposeRule()
    private fun content(block: @androidx.compose.runtime.Composable () -> Unit) =
        rule.setContent { TesseraTheme(darkTheme = false, highContrast = false, reduceMotion = true) { block() } }

    private fun top(text: String) = rule.onNodeWithText(text).getBoundsInRoot().top

    @Test fun tableSortsNumbersAndChineseAndCyclesBack() {
        content { TesseraTable(sampleOrders, sampleColumns, { it.id }, cardBreakpoint = 0.dp) }
        // 金额：升序按数值（89 < 356 < 1,280 < 2,400 < 12,040），不是按字符串
        rule.onNodeWithContentDescription("金额，排序").performClick()
        rule.onNodeWithContentDescription("金额，排序").assert(hasStateDescription("升序"))
        assertTrue(top("张安") < top("李思") && top("李思") < top("王小明") && top("赵宁") < top("陈一"))
        // 再点一次降序，第三次回到原顺序
        rule.onNodeWithContentDescription("金额，排序").performClick()
        rule.onNodeWithContentDescription("金额，排序").assert(hasStateDescription("降序"))
        assertTrue(top("陈一") < top("赵宁"))
        rule.onNodeWithContentDescription("金额，排序").performClick()
        rule.onNodeWithContentDescription("金额，排序").assert(hasStateDescription("未排序"))
        assertTrue(top("王小明") < top("李思"))
        // 客户：中文按拼音（陈 < 李 < 王 < 张 < 赵）
        rule.onNodeWithContentDescription("客户，排序").performClick()
        assertTrue(top("陈一") < top("李思") && top("李思") < top("王小明") && top("王小明") < top("张安") && top("张安") < top("赵宁"))
    }

    @Test fun tableSelectAllHasIndeterminateState() {
        var sel by mutableStateOf(setOf<Any>("A-1025"))
        content { TesseraTable(sampleOrders, sampleColumns, { it.id }, selectable = true, selection = sel, onSelectionChange = { sel = it }, cardBreakpoint = 0.dp) }
        val all = rule.onNodeWithContentDescription("全选")
        all.assert(SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, ToggleableState.Indeterminate))
        all.performClick()
        assertEquals(sampleOrders.map { it.id }.toSet(), sel)
        all.assert(SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, ToggleableState.On))
        all.performClick()
        assertTrue(sel.isEmpty())
        rule.onNodeWithContentDescription("选择 李思").performClick()
        assertEquals(setOf<Any>("A-1025"), sel)
    }

    @Test fun tableFilterAndPagination() {
        var q by mutableStateOf("")
        content { TesseraTable(sampleOrders, sampleColumns, { it.id }, filter = q, pageSize = 2, cardBreakpoint = 0.dp) }
        rule.onNodeWithText("共 5 条，第 1–2 条").assertIsDisplayed()
        rule.onNodeWithContentDescription("上一页").assertIsNotEnabled()
        rule.onNodeWithContentDescription("下一页").performClick()
        rule.onNodeWithText("共 5 条，第 3–4 条").assertIsDisplayed()
        rule.onNodeWithContentDescription("第 2 页，共 3 页").assertIsDisplayed()
        // 筛选后回到第 1 页
        q = "北京"; rule.waitForIdle()
        rule.onNodeWithText("共 1 条，第 1–1 条").assertIsDisplayed()
        rule.onAllNodesWithText("李思").assertCountEquals(1)
        q = "深圳"; rule.waitForIdle()
        rule.onNodeWithText("没有符合条件的数据").assertIsDisplayed()
    }

    @Test fun tableCardsOnNarrowWidth() {
        content { TesseraTable(sampleOrders.take(2), sampleColumns, { it.id }) }
        // 窄屏卡片：列名作为每一项的标签
        rule.onAllNodesWithText("订单号").assertCountEquals(2)
        rule.onNodeWithText("A-1024").assertIsDisplayed()
    }

    @Test fun treeExpandsAndSelects() {
        var open by mutableStateOf(setOf("src"))
        var selected by mutableStateOf<String?>(null)
        content { TesseraTree(sampleTree, open, { open = it }, selected, { selected = it.id }) }
        rule.onNodeWithContentDescription("docs").assert(hasStateDescription("已收起"))
        rule.onAllNodesWithText("spec.md").assertCountEquals(0)
        rule.onNodeWithContentDescription("docs").performClick()
        rule.onNodeWithContentDescription("docs").assert(hasStateDescription("已展开")).assertIsSelected()
        rule.onNodeWithText("spec.md").assertIsDisplayed()
        assertEquals("docs", selected)
        rule.onNodeWithContentDescription("README.md").performClick()
        assertEquals("readme", selected)
    }

    @Test fun accordionSingleKeepsOneOpen() {
        content {
            TesseraAccordion(
                listOf(
                    TesseraAccordionItem("第一项") { TesseraText("内容一") },
                    TesseraAccordionItem("第二项") { TesseraText("内容二") },
                ),
                single = true, initiallyExpanded = setOf(0),
            )
        }
        rule.onNodeWithText("内容一").assertIsDisplayed()
        rule.onNodeWithText("第二项").performClick()
        rule.onNodeWithText("内容二").assertIsDisplayed()
        rule.onAllNodesWithText("内容一").assertCountEquals(0)
        rule.onNodeWithText("第二项").performClick()
        rule.onAllNodesWithText("内容二").assertCountEquals(0)
    }

    @Test fun codeBlockCopiesAndShowsCheck() {
        var cm: androidx.compose.ui.platform.ClipboardManager? = null
        content {
            cm = LocalClipboardManager.current
            TesseraCodeBlock("val a = 1\nval b = 2\n", language = "kotlin", lineNumbers = true)
        }
        rule.onNodeWithContentDescription("复制代码").performClick()
        rule.onNodeWithContentDescription("已复制").assertIsDisplayed()
        // 复制的是代码本身：不带行号、去掉末尾空行
        assertEquals("val a = 1\nval b = 2", cm?.getText()?.text)
    }

    @Test fun carouselButtonsMovePagesAndDisableAtEnds() {
        content { Box(Modifier.fillMaxSize()) { TesseraCarousel(3) { TesseraText("页 ${it + 1}") } } }
        rule.onAllNodes(hasContentDescription("上一张")).assertCountEquals(0)
        rule.onNodeWithContentDescription("下一张").performClick()
        rule.waitForIdle()
        rule.onNodeWithContentDescription("第 2 张，共 3 张").assertIsDisplayed()
        rule.onNodeWithContentDescription("下一张").performClick()
        rule.waitForIdle()
        rule.onAllNodes(hasContentDescription("下一张")).assertCountEquals(0)
    }

    @Test fun statusSemantics() {
        var dismissed = false
        content {
            androidx.compose.foundation.layout.Column {
                TesseraProgress(0.62f, label = "上传中")
                TesseraProgress(null, label = "准备中")
                TesseraProgressRing(0.36f, label = "存储空间")
                TesseraKbd("Ctrl", "K")
                TesseraStats(listOf(TesseraStatItem("新增用户", "1,208", unit = "人", delta = "3.1%", trend = TesseraTrend.Down)))
                TesseraBanner("付款失败", tone = TesseraTone.Danger, onDismiss = { dismissed = true })
            }
        }
        rule.onNodeWithContentDescription("上传中").assert(SemanticsMatcher.expectValue(SemanticsProperties.ProgressBarRangeInfo, ProgressBarRangeInfo(0.62f, 0f..1f))).assert(hasStateDescription("62%"))
        rule.onNodeWithContentDescription("准备中").assert(SemanticsMatcher.expectValue(SemanticsProperties.ProgressBarRangeInfo, ProgressBarRangeInfo.Indeterminate))
        rule.onNodeWithContentDescription("存储空间").assert(hasStateDescription("36%"))
        rule.onNodeWithContentDescription("快捷键 Ctrl 加 K").assertIsDisplayed()
        rule.onNodeWithContentDescription("新增用户 1,208人，较上期下降 3.1%").assertIsDisplayed()
        rule.onNodeWithContentDescription("关闭提示").performClick()
        assertTrue(dismissed)
    }

    @Test fun sortHelperKeepsStableOrderForNone() {
        val col = sampleColumns[1]
        assertEquals(sampleOrders, tesseraSort(sampleOrders, col, TesseraSortDirection.None))
        assertEquals(listOf("89", "356", "1,280", "2,400", "12,040"), tesseraSort(sampleOrders, col, TesseraSortDirection.Ascending).map { it.amount })
    }
}

