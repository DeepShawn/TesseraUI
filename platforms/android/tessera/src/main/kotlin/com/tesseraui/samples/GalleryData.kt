package com.tesseraui.samples

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import com.tesseraui.Tessera
import com.tesseraui.TesseraText
import com.tesseraui.components.TesseraAccordion
import com.tesseraui.components.TesseraAccordionItem
import com.tesseraui.components.TesseraBanner
import com.tesseraui.components.TesseraButton
import com.tesseraui.components.TesseraButtonSize
import com.tesseraui.components.TesseraButtonVariant
import com.tesseraui.components.TesseraCarousel
import com.tesseraui.components.TesseraCodeBlock
import com.tesseraui.components.TesseraColumn
import com.tesseraui.components.TesseraDescriptionList
import com.tesseraui.components.TesseraEmptyState
import com.tesseraui.components.TesseraIconButton
import com.tesseraui.components.TesseraImageViewerContent
import com.tesseraui.components.TesseraKbd
import com.tesseraui.components.TesseraPopoverPanel
import com.tesseraui.components.TesseraProgress
import com.tesseraui.components.TesseraProgressRing
import com.tesseraui.components.TesseraResult
import com.tesseraui.components.TesseraResultStatus
import com.tesseraui.components.TesseraSpinner
import com.tesseraui.components.TesseraStatItem
import com.tesseraui.components.TesseraStats
import com.tesseraui.components.TesseraTable
import com.tesseraui.components.TesseraTag
import com.tesseraui.components.TesseraTimeline
import com.tesseraui.components.TesseraTimelineItem
import com.tesseraui.components.TesseraTone
import com.tesseraui.components.TesseraTooltip
import com.tesseraui.components.TesseraTooltipBubble
import com.tesseraui.components.TesseraTree
import com.tesseraui.components.TesseraTreeNode
import com.tesseraui.components.TesseraTrend
import com.tesseraui.icons.TesseraIcons

/** 示例数据：订单 */
data class SampleOrder(val id: String, val customer: String, val city: String, val amount: String, val status: String)

val sampleOrders = listOf(
    SampleOrder("A-1024", "王小明", "杭州", "1,280", "已发货"),
    SampleOrder("A-1025", "李思", "北京", "356", "待付款"),
    SampleOrder("A-1026", "陈一", "广州", "12,040", "已完成"),
    SampleOrder("A-1027", "张安", "成都", "89", "已退款"),
    SampleOrder("A-1028", "赵宁", "上海", "2,400", "已发货"),
)

@Composable
private fun statusTag(s: String) = TesseraTag(
    s,
    tone = when (s) { "已完成" -> TesseraTone.Success; "待付款" -> TesseraTone.Warning; "已退款" -> TesseraTone.Neutral; else -> TesseraTone.Accent },
)

val sampleColumns = listOf(
    TesseraColumn<SampleOrder>("客户", { it.customer }, width = 96.dp, sortable = true),
    TesseraColumn("金额", { it.amount }, width = 100.dp, sortable = true, numeric = true),
    TesseraColumn("城市", { it.city }, width = 84.dp, sortable = true),
    TesseraColumn("状态", { it.status }, width = 100.dp, cell = { statusTag(it.status) }),
    TesseraColumn("订单号", { it.id }, width = 100.dp),
)

val sampleTree = listOf(
    TesseraTreeNode(
        "src", "src", TesseraIcons.Folder,
        listOf(
            TesseraTreeNode("components", "components", TesseraIcons.Folder, listOf(
                TesseraTreeNode("button", "Button.kt", TesseraIcons.FileText),
                TesseraTreeNode("table", "Table.kt", TesseraIcons.FileText),
            )),
            TesseraTreeNode("theme", "Theme.kt", TesseraIcons.FileText),
        ),
    ),
    TesseraTreeNode("docs", "docs", TesseraIcons.Folder, listOf(TesseraTreeNode("spec", "spec.md", TesseraIcons.FileText))),
    TesseraTreeNode("readme", "README.md", TesseraIcons.FileText),
)

/** 示例图片：一片釉色马赛克（不依赖图片资源） */
class MosaicPainter(private val base: Color, private val accent: Color) : Painter() {
    override val intrinsicSize = Size(800f, 520f)
    override fun DrawScope.onDraw() {
        drawRect(base)
        val cols = 8; val rows = 5
        val gap = size.width / 120
        val w = (size.width - gap * (cols + 1)) / cols
        val h = (size.height - gap * (rows + 1)) / rows
        for (r in 0 until rows) for (c in 0 until cols) {
            val k = (r * 7 + c * 3) % 5
            val col = when (k) { 0 -> accent; 1 -> accent.copy(alpha = 0.7f); 2 -> Color.White.copy(alpha = 0.18f); 3 -> Color.Black.copy(alpha = 0.12f); else -> Color.White.copy(alpha = 0.06f) }
            drawRoundRect(col, Offset(gap + c * (w + gap), gap + r * (h + gap)), Size(w, h), CornerRadius(w / 6))
        }
    }
}

/** v0.6 数据展示组件的总览：开发预览、截图测试、无障碍检查与示例 App 共用 */
@Composable
internal fun GalleryData(section: GallerySection) {
    val c = Tessera.colors
    when (section) {
        GallerySection.Table -> Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            var sel by remember { mutableStateOf(setOf<Any>("A-1025")) }
            TesseraText("宽屏：固定首列、可排序、多选、分页", style = Tessera.type.caption, color = c.textSecondary)
            TesseraTable(sampleOrders, sampleColumns, { it.id }, selectable = true, selection = sel, onSelectionChange = { sel = it }, pageSize = 4, cardBreakpoint = 0.dp)
            TesseraText("加载中", style = Tessera.type.caption, color = c.textSecondary)
            TesseraTable(sampleOrders, sampleColumns.take(3), { it.id }, loading = true, pageSize = 2, cardBreakpoint = 0.dp)
        }
        GallerySection.TableCards -> Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            var sel by remember { mutableStateOf(setOf<Any>("A-1024")) }
            TesseraText("窄屏：每行一张卡片", style = Tessera.type.caption, color = c.textSecondary)
            TesseraTable(sampleOrders.take(3), sampleColumns, { it.id }, selectable = true, selection = sel, onSelectionChange = { sel = it })
            TesseraText("筛选后没有结果", style = Tessera.type.caption, color = c.textSecondary)
            TesseraTable(sampleOrders, sampleColumns, { it.id }, filter = "深圳")
        }
        GallerySection.Tree -> Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            var open by remember { mutableStateOf(setOf("src", "components")) }
            var selected by remember { mutableStateOf<String?>("table") }
            TesseraTree(sampleTree, open, { open = it }, selected, { selected = it.id })
            TesseraAccordion(
                listOf(
                    TesseraAccordionItem("如何退换货？") { TesseraText("收货 7 天内可申请无理由退货，商品需保持完好。", style = Tessera.type.bodySm) },
                    TesseraAccordionItem("多久发货？") { TesseraText("付款后 48 小时内发货。", style = Tessera.type.bodySm) },
                    TesseraAccordionItem("支持哪些付款方式？") { TesseraText("支付宝、微信支付、银行卡。", style = Tessera.type.bodySm) },
                ),
                single = true, initiallyExpanded = setOf(0),
            )
        }
        GallerySection.Details -> Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            TesseraDescriptionList(listOf("订单号" to "A-1024", "下单时间" to "2026 年 11 月 19 日 09:30", "收货地址" to "浙江省杭州市西湖区文三路 90 号", "备注" to "工作日送货"))
            TesseraStats(
                listOf(
                    TesseraStatItem("本月收入", "¥86,240", delta = "12.5%", trend = TesseraTrend.Up),
                    TesseraStatItem("新增用户", "1,208", unit = "人", delta = "3.1%", trend = TesseraTrend.Down),
                    TesseraStatItem("转化率", "4.6", unit = "%"),
                    TesseraStatItem("退款率", "0.8", unit = "%", delta = "0.2%", trend = TesseraTrend.Down),
                ),
            )
        }
        GallerySection.Timeline -> Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            TesseraTimeline(
                listOf(
                    TesseraTimelineItem("已签收", "11 月 19 日 14:02", "本人签收，感谢使用", TesseraTone.Success),
                    TesseraTimelineItem("派送中", "11 月 19 日 09:15", "快递员：周师傅 138 0000 0000", TesseraTone.Accent),
                    TesseraTimelineItem("已到达杭州转运中心", "11 月 18 日 22:40"),
                    TesseraTimelineItem("已发货", "11 月 18 日 10:05"),
                ),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                TesseraText("搜索", style = Tessera.type.bodySm)
                TesseraKbd("Ctrl", "K")
                TesseraText("保存", style = Tessera.type.bodySm)
                TesseraKbd("⌘", "S")
            }
        }
        GallerySection.Overlays -> Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                TesseraTooltip("复制链接") { TesseraIconButton(TesseraIcons.Copy, "复制", {}) }
                TesseraTooltipBubble("长按或悬停显示提示")
            }
            TesseraPopoverPanel(title = "配送说明") {
                TesseraText("江浙沪次日达，其他地区 2～3 天送达。", style = Tessera.type.bodySm, color = c.textSecondary)
                Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TesseraButton("知道了", {}, size = TesseraButtonSize.Sm)
                    TesseraButton("查看详情", {}, variant = TesseraButtonVariant.Secondary, size = TesseraButtonSize.Sm)
                }
            }
            TesseraCodeBlock(
                "TesseraTable(\n    rows = orders,\n    columns = columns,\n    rowKey = { it.id },\n    selectable = true,\n)",
                language = "kotlin", lineNumbers = true,
            )
        }
        GallerySection.Media -> Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            val tones = listOf(c.accentSolid to c.accentControl, c.pigmentLapis to c.pigmentOchre, c.pigmentOlive to c.pigmentCinnabar)
            TesseraCarousel(3, height = 180.dp, peek = true, initialPage = 1) { i ->
                val (a, b) = tones[i]
                Box(Modifier.fillMaxSize()) {
                    androidx.compose.foundation.Image(MosaicPainter(a, b), null, Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
                    TesseraText(listOf("秋季上新", "会员日", "以旧换新")[i], Modifier.align(Alignment.BottomStart).padding(16.dp), style = Tessera.type.title3, color = Color.White)
                }
            }
            Box(Modifier.fillMaxWidth().height(300.dp).clip(RoundedCornerShape(16.dp))) {
                TesseraImageViewerContent(
                    listOf(MosaicPainter(c.accentSolid, c.accentControl), MosaicPainter(c.pigmentLapis, c.pigmentOchre)),
                    {}, descriptions = listOf("釉色马赛克", "青金马赛克"),
                )
            }
        }
        GallerySection.Progress -> Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            TesseraProgress(0.62f, label = "上传中")
            TesseraProgress(1f, label = "已完成", tone = TesseraTone.Success)
            TesseraProgress(null, label = "正在准备…")
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
                TesseraProgressRing(0.36f, label = "存储空间")
                TesseraProgressRing(0.82f, size = 96.dp, label = "本周目标")
                TesseraSpinner()
                TesseraSpinner(size = 32.dp)
            }
            TesseraBanner("新版本已发布，重启后生效。", title = "更新可用", onDismiss = {}, actions = {
                TesseraButton("立即重启", {}, size = TesseraButtonSize.Sm)
            })
            TesseraBanner("网络连接不稳定，部分内容可能无法加载。", tone = TesseraTone.Warning)
            TesseraBanner("付款失败，请检查银行卡余额。", tone = TesseraTone.Danger, onDismiss = {})
        }
        GallerySection.Results -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TesseraResult(TesseraResultStatus.Success, "付款成功", text = "订单 A-1024 已支付，预计明天送达。", actions = {
                TesseraButton("查看订单", {})
                TesseraButton("继续购物", {}, variant = TesseraButtonVariant.Secondary)
            })
            TesseraEmptyState("还没有收藏", text = "看到喜欢的商品，点一下心形就能收藏在这里。", action = {
                TesseraButton("去逛逛", {}, variant = TesseraButtonVariant.Secondary)
            })
        }
        GallerySection.ResultsMore -> Column(Modifier.fillMaxWidth()) {
            TesseraResult(TesseraResultStatus.Error, "提交失败", text = "网络中断，填写的内容已保存，请稍后重试。", actions = { TesseraButton("重试", {}) })
            TesseraResult(TesseraResultStatus.NotFound, "页面不存在", text = "链接可能已失效。", actions = { TesseraButton("返回首页", {}, variant = TesseraButtonVariant.Secondary) })
        }
        else -> Unit
    }
}
