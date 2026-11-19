package com.tesseraui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tesseraui.LocalContentColor
import com.tesseraui.LocalReduceMotion
import com.tesseraui.Tessera
import com.tesseraui.TesseraMotion
import com.tesseraui.TesseraRadius
import com.tesseraui.TesseraRound
import com.tesseraui.TesseraSize
import com.tesseraui.TesseraSpace
import com.tesseraui.TesseraText
import com.tesseraui.TesseraTile
import com.tesseraui.foundation.flowRipple
import com.tesseraui.foundation.isRound
import com.tesseraui.foundation.roundRadius
import com.tesseraui.foundation.shapeOf
import com.tesseraui.foundation.showOutline
import com.tesseraui.foundation.tilePosition
import com.tesseraui.icons.TesseraIcon
import com.tesseraui.icons.TesseraIcons
import com.tesseraui.motion
import java.text.Collator
import java.util.Locale

// ------------------------------------------------------------------
// 表格 Table：每一行是一块砖，砖缝分隔，不用横线
// - 点表头排序：升序 → 降序 → 原顺序；数字按数值、中文按拼音
// - 筛选：传入 filter（建议用 TesseraSearchField 的已选定文字）
// - 多选：选择列、全选（含半选）
// - 固定首列：横向滚动后首列右侧出现分隔阴影
// - 窄屏（默认小于 560dp）：每行变成一张卡片，列名在左、值在右
// ------------------------------------------------------------------

enum class TesseraSortDirection { None, Ascending, Descending }

/**
 * 表格列。
 * @param text 这一列的文字（用于显示、筛选、排序和读屏）
 * @param numeric 数字列：右对齐，按数值排序
 * @param cell 自定义单元格内容（不传则显示 text）
 */
class TesseraColumn<T>(
    val title: String,
    val text: (T) -> String,
    val width: Dp = 120.dp,
    val sortable: Boolean = false,
    val numeric: Boolean = false,
    val cell: (@Composable (T) -> Unit)? = null,
)

private val zhCollator: Collator = Collator.getInstance(Locale.CHINA)
private fun numberOf(s: String): Double? = s.replace(Regex("[,，¥$%\\s]"), "").toDoubleOrNull()

/** 按表格的规则排序（数字列按数值、其余按中文拼音），供表格内部和测试使用 */
fun <T> tesseraSort(rows: List<T>, column: TesseraColumn<T>, direction: TesseraSortDirection): List<T> {
    if (direction == TesseraSortDirection.None) return rows
    val numeric = column.numeric || rows.all { numberOf(column.text(it)) != null }
    val cmp: Comparator<T> = if (numeric) compareBy { numberOf(column.text(it)) ?: Double.NaN } else Comparator { a, b -> zhCollator.compare(column.text(a), column.text(b)) }
    return rows.sortedWith(if (direction == TesseraSortDirection.Ascending) cmp else cmp.reversed())
}

@Composable
fun <T> TesseraTable(
    rows: List<T>,
    columns: List<TesseraColumn<T>>,
    rowKey: (T) -> Any,
    modifier: Modifier = Modifier,
    filter: String = "",
    selectable: Boolean = false,
    selection: Set<Any> = emptySet(),
    onSelectionChange: (Set<Any>) -> Unit = {},
    stickyFirst: Boolean = true,
    pageSize: Int = 0,
    loading: Boolean = false,
    cardBreakpoint: Dp = 560.dp,
    emptyText: String = "没有符合条件的数据",
    rowActions: (@Composable RowScope.(T) -> Unit)? = null,
) {
    var sortCol by remember { mutableIntStateOf(-1) }
    var sortDir by remember { mutableStateOf(TesseraSortDirection.None) }
    var page by remember { mutableIntStateOf(1) }
    val q = filter.trim().lowercase()
    val filtered = remember(rows, q) { if (q.isEmpty()) rows else rows.filter { r -> columns.any { it.text(r).lowercase().contains(q) } } }
    val sorted = remember(filtered, sortCol, sortDir) { if (sortCol < 0) filtered else tesseraSort(filtered, columns[sortCol], sortDir) }
    LaunchedEffect(q, sortCol, sortDir) { page = 1 }
    val pages = if (pageSize > 0) maxOf(1, (sorted.size + pageSize - 1) / pageSize) else 1
    val current = page.coerceIn(1, pages)
    val shown = if (pageSize > 0) sorted.drop((current - 1) * pageSize).take(pageSize) else sorted
    val onSort: (Int) -> Unit = { i ->
        sortDir = if (sortCol != i) TesseraSortDirection.Ascending else when (sortDir) {
            TesseraSortDirection.Ascending -> TesseraSortDirection.Descending
            TesseraSortDirection.Descending -> TesseraSortDirection.None
            TesseraSortDirection.None -> TesseraSortDirection.Ascending
        }
        sortCol = if (sortDir == TesseraSortDirection.None) -1 else i
    }
    val visibleKeys = sorted.map(rowKey)
    val inView = visibleKeys.count { it in selection }
    val toggleAll: () -> Unit = {
        onSelectionChange(if (inView == visibleKeys.size && visibleKeys.isNotEmpty()) selection - visibleKeys.toSet() else selection + visibleKeys)
    }
    val toggleRow: (Any) -> Unit = { k -> onSelectionChange(if (k in selection) selection - k else selection + k) }

    Column(modifier) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            if (maxWidth < cardBreakpoint) {
                TableCards(shown, columns, rowKey, selectable, selection, toggleRow, loading, pageSize, emptyText, rowActions)
            } else {
                TableGrid(
                    shown, columns, rowKey, selectable, selection, toggleRow, toggleAll,
                    allState = when { inView == 0 -> false; inView == visibleKeys.size -> true; else -> null },
                    stickyFirst, sortCol, sortDir, onSort, loading, pageSize, emptyText, rowActions,
                )
            }
        }
        if (pageSize > 0 && !loading) TableFooter(sorted.size, current, pages, pageSize, shown.size) { page = it }
    }
}

private val headHeight = 44.dp
private val rowHeight = 48.dp

@Composable
private fun rowBackground(index: Int, selected: Boolean): Color {
    val c = Tessera.colors
    return when {
        selected -> lerp(c.bgSurface, c.accentSolid, if (isRound) 0.12f else 0.10f)
        isRound && index % 2 == 1 -> lerp(c.bgSurface, c.textPrimary, 0.025f)
        else -> c.bgSurface
    }
}

/** 一行在左（start）或右（end）半边的形状：首行、末行外角大，其余是砖缝小角 */
@Composable
private fun rowShape(index: Int, count: Int, start: Boolean, end: Boolean): Shape {
    val outer = if (isRound) roundRadius(TesseraRound.row) else TesseraRadius.lg
    val inner = if (isRound) 0.dp else TesseraRadius.grout
    val top = if (index == 0) outer else inner
    val bottom = if (index == count - 1) outer else inner
    return RoundedCornerShape(
        topStart = if (start) top else 0.dp, bottomStart = if (start) bottom else 0.dp,
        topEnd = if (end) top else 0.dp, bottomEnd = if (end) bottom else 0.dp,
    )
}

@Composable
private fun <T> TableGrid(
    rows: List<T>,
    columns: List<TesseraColumn<T>>,
    rowKey: (T) -> Any,
    selectable: Boolean,
    selection: Set<Any>,
    toggleRow: (Any) -> Unit,
    toggleAll: () -> Unit,
    allState: Boolean?,
    stickyFirst: Boolean,
    sortCol: Int,
    sortDir: TesseraSortDirection,
    onSort: (Int) -> Unit,
    loading: Boolean,
    pageSize: Int,
    emptyText: String,
    rowActions: (@Composable RowScope.(T) -> Unit)?,
) {
    val c = Tessera.colors
    val scroll = rememberScrollState()
    val gap = if (isRound) 0.dp else TesseraTile.grout
    val fixedCols = if (stickyFirst) 1 else 0
    val hasFixed = selectable || fixedCols > 0
    val count = if (loading) (if (pageSize > 0) pageSize else 5) else rows.size
    val selectW = 48.dp

    @Composable
    fun headCell(i: Int) {
        val col = columns[i]
        val dir = if (sortCol == i) sortDir else TesseraSortDirection.None
        val align = if (col.numeric) Alignment.CenterEnd else Alignment.CenterStart
        // 可排序的表头按钮自带 6dp 内边距，外层少留 6dp，文字才和下面的单元格对齐
        Box(Modifier.width(col.width).height(headHeight).padding(horizontal = if (col.sortable) 10.dp else 16.dp), contentAlignment = align) {
            if (col.sortable) {
                val active = dir != TesseraSortDirection.None
                Row(
                    Modifier.height(headHeight).clip(RoundedCornerShape(8.dp))
                        .clickable(role = Role.Button) { onSort(i) }
                        .semantics {
                            contentDescription = "${col.title}，排序"
                            stateDescription = when (dir) { TesseraSortDirection.Ascending -> "升序"; TesseraSortDirection.Descending -> "降序"; else -> "未排序" }
                        }
                        .padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    val fg = if (active) c.accentText else c.textSecondary
                    if (!col.numeric) TesseraText(col.title, style = Tessera.type.label.copy(fontSize = 13.sp), color = fg, maxLines = 1)
                    TesseraIcon(
                        when (dir) { TesseraSortDirection.Ascending -> TesseraIcons.ArrowUp; TesseraSortDirection.Descending -> TesseraIcons.ArrowDown; else -> TesseraIcons.ChevronsUpDown },
                        null, Modifier.alpha(if (active) 1f else 0.4f), tint = fg, size = 14.dp,
                    )
                    if (col.numeric) TesseraText(col.title, style = Tessera.type.label.copy(fontSize = 13.sp), color = fg, maxLines = 1)
                }
            } else TesseraText(col.title, style = Tessera.type.label.copy(fontSize = 13.sp), color = c.textSecondary, maxLines = 1)
        }
    }

    @Composable
    fun bodyCell(i: Int, row: T) {
        val col = columns[i]
        Box(Modifier.width(col.width).height(rowHeight).padding(horizontal = 16.dp), contentAlignment = if (col.numeric) Alignment.CenterEnd else Alignment.CenterStart) {
            CompositionLocalProvider(LocalContentColor provides c.textPrimary) {
                if (col.cell != null) col.cell.invoke(row)
                else TesseraText(col.text(row), style = Tessera.type.bodySm.copy(fontFeatureSettings = "tnum"), color = c.textPrimary, maxLines = 1)
            }
        }
    }

    @Composable
    fun skeletonCell(w: Dp, i: Int) {
        Box(Modifier.width(w).height(rowHeight).padding(horizontal = 16.dp), contentAlignment = Alignment.CenterStart) {
            TesseraSkeleton(Modifier.fillMaxWidth(0.4f + ((i * 23) % 50) / 100f).height(12.dp))
        }
    }

    if (!loading && rows.isEmpty()) {
        Column(Modifier.fillMaxWidth()) {
            Row(Modifier.horizontalScroll(scroll)) { if (selectable) Spacer(Modifier.width(selectW)); columns.indices.forEach { headCell(it) } }
            Box(Modifier.fillMaxWidth().clip(rowShape(0, 1, true, true)).background(c.bgSurface).padding(TesseraSpace.s10), contentAlignment = Alignment.Center) {
                TesseraText(emptyText, style = Tessera.type.bodySm, color = c.textTertiary, textAlign = TextAlign.Center)
            }
        }
        return
    }

    BoxWithConstraints(Modifier.fillMaxWidth()) {
    // 列宽加起来比容器窄时，在末尾补一段空白，让每一行仍然铺满整个宽度
    val fixedW = (if (selectable) selectW else 0.dp) + (if (fixedCols > 0) columns[0].width else 0.dp)
    val scrollW = columns.drop(fixedCols).fold(0.dp) { acc, col -> acc + col.width } + (if (rowActions != null) 112.dp else 0.dp)
    val extra = (maxWidth - fixedW - scrollW).coerceAtLeast(0.dp)
    Row(Modifier.fillMaxWidth()) {
        if (hasFixed) Column(
            Modifier.width(IntrinsicSize.Max).drawBehind {
                // 横向滚动后，固定列右侧出现一道淡阴影
                if (scroll.value > 0) drawRect(
                    Brush.horizontalGradient(listOf(c.shadowColor.copy(alpha = 0.18f), Color.Transparent), startX = size.width, endX = size.width + 10.dp.toPx()),
                    topLeft = Offset(size.width, 0f), size = androidx.compose.ui.geometry.Size(10.dp.toPx(), size.height),
                )
            },
            verticalArrangement = Arrangement.spacedBy(gap),
        ) {
            Row {
                if (selectable) Box(Modifier.width(selectW).height(headHeight).padding(start = 12.dp), contentAlignment = Alignment.CenterStart) {
                    TesseraCheckbox(allState == true, { toggleAll() }, Modifier.width(36.dp).semantics { contentDescription = "全选" }, indeterminate = allState == null)
                }
                if (fixedCols > 0) headCell(0)
            }
            for (r in 0 until count) {
                val row = if (loading) null else rows[r]
                val key = row?.let(rowKey)
                val sel = key != null && key in selection
                val shape = rowShape(r, count, start = true, end = false)
                val bar = if (sel && !isRound) c.accentSolid else Color.Transparent
                Row(
                    Modifier.clip(shape).background(rowBackground(r, sel))
                        // 拼贴：选中行左侧一道 3dp 釉色竖条；圆融只靠底色
                        .drawBehind { drawRect(bar, size = androidx.compose.ui.geometry.Size(3.dp.toPx(), size.height)) },
                ) {
                    if (selectable) Box(Modifier.width(selectW).height(rowHeight).padding(start = 12.dp), contentAlignment = Alignment.CenterStart) {
                        if (row != null) {
                            val label = columns.firstOrNull()?.text?.invoke(row) ?: "第 ${r + 1} 行"
                            TesseraCheckbox(sel, { toggleRow(key!!) }, Modifier.width(36.dp).semantics { contentDescription = "选择 $label" })
                        }
                    }
                    if (fixedCols > 0) { if (row != null) bodyCell(0, row) else skeletonCell(columns[0].width, 0) }
                }
            }
        }
        Column(Modifier.weight(1f).horizontalScroll(scroll), verticalArrangement = Arrangement.spacedBy(gap)) {
            Row { for (i in fixedCols until columns.size) headCell(i); if (rowActions != null) Spacer(Modifier.width(112.dp)); Spacer(Modifier.width(extra)) }
            for (r in 0 until count) {
                val row = if (loading) null else rows[r]
                val sel = row != null && rowKey(row) in selection
                Row(Modifier.clip(rowShape(r, count, start = !hasFixed, end = true)).background(rowBackground(r, sel)), verticalAlignment = Alignment.CenterVertically) {
                    for (i in fixedCols until columns.size) { if (row != null) bodyCell(i, row) else skeletonCell(columns[i].width, i) }
                    if (rowActions != null) Row(Modifier.width(112.dp).height(rowHeight).padding(end = 6.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                        if (row != null) rowActions(row)
                    }
                    Spacer(Modifier.width(extra))
                }
            }
        }
    }
    }
}

@Composable
private fun <T> TableCards(
    rows: List<T>,
    columns: List<TesseraColumn<T>>,
    rowKey: (T) -> Any,
    selectable: Boolean,
    selection: Set<Any>,
    toggleRow: (Any) -> Unit,
    loading: Boolean,
    pageSize: Int,
    emptyText: String,
    rowActions: (@Composable RowScope.(T) -> Unit)?,
) {
    val c = Tessera.colors
    val round = isRound
    val gap = if (round) TesseraRound.gap else TesseraTile.grout
    if (!loading && rows.isEmpty()) {
        Box(Modifier.fillMaxWidth().clip(rowShape(0, 1, true, true)).background(c.bgSurface).padding(TesseraSpace.s10), contentAlignment = Alignment.Center) {
            TesseraText(emptyText, style = Tessera.type.bodySm, color = c.textTertiary, textAlign = TextAlign.Center)
        }
        return
    }
    val count = if (loading) (if (pageSize > 0) pageSize else 3) else rows.size
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(gap)) {
        for (r in 0 until count) {
            val outer = if (round) roundRadius(TesseraRound.row) else TesseraRadius.lg
            val shape = if (round) RoundedCornerShape(outer) else rowShape(r, count, true, true)
            if (loading) {
                Column(Modifier.fillMaxWidth().clip(shape).background(c.bgSurface).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    TesseraSkeleton(Modifier.fillMaxWidth(0.5f).height(14.dp))
                    TesseraSkeleton(Modifier.fillMaxWidth(0.8f).height(12.dp))
                }
                continue
            }
            val row = rows[r]
            val key = rowKey(row)
            val sel = key in selection
            Column(Modifier.fillMaxWidth().clip(shape).background(if (sel) lerp(c.bgSurface, c.accentSolid, if (round) 0.12f else 0.10f) else c.bgSurface).padding(vertical = 6.dp)) {
                Row(Modifier.fillMaxWidth().heightIn(min = 44.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (selectable) TesseraCheckbox(sel, { toggleRow(key) }, Modifier.width(36.dp).semantics { contentDescription = "选择 ${columns.first().text(row)}" })
                    Box(Modifier.weight(1f)) {
                        val first = columns.first()
                        if (first.cell != null) first.cell.invoke(row)
                        else TesseraText(first.text(row), style = Tessera.type.label.copy(fontSize = 15.sp), color = c.textPrimary)
                    }
                }
                for (i in 1 until columns.size) {
                    val col = columns[i]
                    Row(Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}.padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        TesseraText(col.title, Modifier.weight(1f), style = Tessera.type.caption, color = c.textSecondary)
                        if (col.cell != null) col.cell.invoke(row)
                        else TesseraText(col.text(row), style = Tessera.type.bodySm.copy(fontFeatureSettings = "tnum"), color = c.textPrimary, textAlign = TextAlign.End)
                    }
                }
                if (rowActions != null) Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.End) { rowActions(row) }
            }
        }
    }
}

@Composable
private fun TableFooter(total: Int, page: Int, pages: Int, pageSize: Int, shownCount: Int, onPage: (Int) -> Unit) {
    val c = Tessera.colors
    val from = if (total == 0) 0 else (page - 1) * pageSize + 1
    Row(Modifier.fillMaxWidth().padding(top = TesseraSpace.s3), verticalAlignment = Alignment.CenterVertically) {
        TesseraText(
            if (total == 0) "共 0 条" else "共 $total 条，第 $from–${from + shownCount - 1} 条",
            Modifier.weight(1f), style = Tessera.type.caption, color = c.textSecondary,
        )
        TesseraIconButton(TesseraIcons.ChevronLeft, "上一页", { onPage(page - 1) }, enabled = page > 1, size = TesseraButtonSize.Sm)
        TesseraText("$page / $pages", Modifier.padding(horizontal = 4.dp).semantics { contentDescription = "第 $page 页，共 $pages 页" }, style = Tessera.type.caption.copy(fontFeatureSettings = "tnum"), color = c.textSecondary)
        TesseraIconButton(TesseraIcons.ChevronRight, "下一页", { onPage(page + 1) }, enabled = page < pages, size = TesseraButtonSize.Sm)
    }
}

// ------------------------------------------------------------------
// 树 Tree：可展开的层级列表；展开箭头带弹簧转动，子级淡入下滑
// ------------------------------------------------------------------

data class TesseraTreeNode(
    val id: String,
    val label: String,
    val icon: ImageVector? = null,
    val children: List<TesseraTreeNode> = emptyList(),
)

@Composable
fun TesseraTree(
    nodes: List<TesseraTreeNode>,
    expanded: Set<String>,
    onExpandedChange: (Set<String>) -> Unit,
    selectedId: String?,
    onSelect: (TesseraTreeNode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) { TreeLevel(nodes, 0, expanded, onExpandedChange, selectedId, onSelect) }
}

@Composable
private fun TreeLevel(
    nodes: List<TesseraTreeNode>,
    depth: Int,
    expanded: Set<String>,
    onExpandedChange: (Set<String>) -> Unit,
    selectedId: String?,
    onSelect: (TesseraTreeNode) -> Unit,
) {
    val c = Tessera.colors
    val round = isRound
    val reduce = LocalReduceMotion.current
    nodes.forEach { n ->
        val open = n.id in expanded
        val hasKids = n.children.isNotEmpty()
        val sel = n.id == selectedId
        val interaction = remember { MutableInteractionSource() }
        val pressed by interaction.collectIsPressedAsState()
        val turn by animateFloatAsState(if (open) 90f else 0f, motion(TesseraMotion.snappy()), label = "turn")
        Row(
            Modifier.fillMaxWidth().heightIn(min = TesseraSize.touchMin)
                .clip(if (round) RoundedCornerShape(50) else RoundedCornerShape(10.dp))
                .background(if (sel) c.accentSubtle else if (pressed) c.statePressed else Color.Transparent)
                .flowRipple(interaction, c.textPrimary.copy(alpha = 0.13f))
                .clickable(interaction, null, role = Role.Button) {
                    if (hasKids) onExpandedChange(if (open) expanded - n.id else expanded + n.id)
                    onSelect(n)
                }
                .semantics {
                    contentDescription = n.label
                    selected = sel
                    if (hasKids) stateDescription = if (open) "已展开" else "已收起"
                }
                .padding(start = 4.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(Modifier.size(18.dp).rotate(turn).alpha(if (hasKids) 1f else 0f), contentAlignment = Alignment.Center) {
                TesseraIcon(TesseraIcons.ChevronRight, null, tint = c.textTertiary, size = 14.dp)
            }
            if (n.icon != null) TesseraIcon(n.icon, null, tint = if (sel) c.accentText else c.textSecondary, size = 18.dp)
            TesseraText(
                n.label, Modifier.weight(1f),
                style = Tessera.type.bodySm.copy(fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal),
                color = if (sel) c.accentText else c.textPrimary, maxLines = 1,
            )
        }
        if (hasKids) AnimatedVisibility(
            open,
            enter = if (reduce) fadeIn() else fadeIn(motion(TesseraMotion.smooth())) + expandVertically(motion(TesseraMotion.smooth())),
            exit = if (reduce) fadeOut() else fadeOut(motion(TesseraMotion.snappy())) + shrinkVertically(motion(TesseraMotion.snappy())),
        ) {
            Box(
                Modifier.padding(start = 20.dp).drawBehind {
                    // 拼贴：子级左侧一道细竖线；圆融不画线
                    if (!round) drawRoundRect(
                        c.borderSubtle, topLeft = Offset(-9.dp.toPx(), 2.dp.toPx()),
                        size = androidx.compose.ui.geometry.Size(1.5.dp.toPx(), size.height - 8.dp.toPx()),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.dp.toPx()),
                    )
                },
            ) { Column { TreeLevel(n.children, depth + 1, expanded, onExpandedChange, selectedId, onSelect) } }
        }
    }
}

// ------------------------------------------------------------------
// 折叠面板 Accordion：拼贴是一列砖；圆融是一整块面板，展开的那一行底色加深
// ------------------------------------------------------------------

class TesseraAccordionItem(val title: String, val content: @Composable () -> Unit)

@Composable
fun TesseraAccordion(
    items: List<TesseraAccordionItem>,
    modifier: Modifier = Modifier,
    single: Boolean = false,
    initiallyExpanded: Set<Int> = emptySet(),
) {
    val c = Tessera.colors
    val round = isRound
    var open by remember { mutableStateOf(initiallyExpanded) }
    val reduce = LocalReduceMotion.current
    val outerShape = RoundedCornerShape(roundRadius(TesseraRound.card))
    Column(
        modifier.fillMaxWidth().then(
            if (round) Modifier.shadow(3.dp, outerShape, ambientColor = c.shadowColor, spotColor = c.shadowColor).clip(outerShape).background(c.bgSurface).padding(6.dp)
            else Modifier,
        ),
        verticalArrangement = Arrangement.spacedBy(if (round) 0.dp else TesseraTile.grout),
    ) {
        items.forEachIndexed { i, item ->
            val isOpen = i in open
            val shape: Shape = if (round) RoundedCornerShape(roundRadius(TesseraRound.row)) else com.tesseraui.foundation.tileShape(tilePosition(i, items.size), TesseraRadius.lg, vertical = true)
            val turn by animateFloatAsState(if (isOpen) 180f else 0f, motion(TesseraMotion.snappy()), label = "chev")
            val interaction = remember { MutableInteractionSource() }
            val pressed by interaction.collectIsPressedAsState()
            Column(
                Modifier.fillMaxWidth().clip(shape)
                    .background(if (round) (if (isOpen) c.fillDefault else Color.Transparent) else c.bgSurface)
                    .then(if (!round && showOutline) Modifier.border(1.dp, c.borderSurface, shape) else Modifier),
            ) {
                Row(
                    Modifier.fillMaxWidth().height(56.dp)
                        .background(if (pressed) c.statePressed else Color.Transparent)
                        .flowRipple(interaction, c.textPrimary.copy(alpha = 0.13f))
                        .clickable(interaction, null, role = Role.Button) {
                            open = when {
                                isOpen -> open - i
                                single -> setOf(i)
                                else -> open + i
                            }
                        }
                        .semantics { heading(); stateDescription = if (isOpen) "已展开" else "已收起" }
                        .padding(horizontal = if (round) TesseraSpace.s4 else TesseraSpace.s5),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TesseraText(item.title, Modifier.weight(1f), style = Tessera.type.label.copy(fontSize = 15.sp), color = c.textPrimary)
                    TesseraIcon(TesseraIcons.ChevronDown, null, Modifier.rotate(turn), tint = c.textTertiary, size = 16.dp)
                }
                AnimatedVisibility(
                    isOpen,
                    enter = if (reduce) fadeIn() else expandVertically(motion(TesseraMotion.smooth())) + fadeIn(),
                    exit = if (reduce) fadeOut() else shrinkVertically(motion(TesseraMotion.smooth())) + fadeOut(),
                ) {
                    Box(Modifier.fillMaxWidth().padding(start = if (round) 16.dp else 20.dp, end = if (round) 16.dp else 20.dp, bottom = if (round) 16.dp else 20.dp)) {
                        CompositionLocalProvider(LocalContentColor provides c.textSecondary) { item.content() }
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------
// 描述列表 DescriptionList：每一项是左右两块砖（标签 / 值）；窄屏上下排
// ------------------------------------------------------------------

@Composable
fun TesseraDescriptionList(
    items: List<Pair<String, String>>,
    modifier: Modifier = Modifier,
    labelWidth: Dp = 96.dp,
    stackBelow: Dp = 360.dp,
) {
    val c = Tessera.colors
    val round = isRound
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val stacked = maxWidth < stackBelow
        val gap = if (round) 0.dp else TesseraTile.grout
        val container = RoundedCornerShape(roundRadius(TesseraRound.row))
        Column(
            Modifier.fillMaxWidth().then(if (round) Modifier.clip(container).background(c.bgSurface).padding(vertical = 6.dp) else Modifier),
            verticalArrangement = Arrangement.spacedBy(gap),
        ) {
            items.forEachIndexed { i, (label, value) ->
                val outer = TesseraRadius.lg
                val inner = TesseraRadius.grout
                val top = if (i == 0) outer else inner
                val bottom = if (i == items.size - 1) outer else inner
                val bg = if (round) Color.Transparent else c.bgSurface
                if (stacked) {
                    Column(
                        Modifier.fillMaxWidth().clip(if (round) RoundedCornerShape(0.dp) else RoundedCornerShape(top, top, bottom, bottom)).background(bg)
                            .semantics(mergeDescendants = true) {}.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        TesseraText(label, style = Tessera.type.bodySm, color = c.textSecondary)
                        TesseraText(value, style = Tessera.type.bodySm.copy(fontWeight = FontWeight.Medium), color = c.textPrimary)
                    }
                } else {
                    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).semantics(mergeDescendants = true) {}, horizontalArrangement = Arrangement.spacedBy(gap)) {
                        Box(
                            Modifier.width(labelWidth).fillMaxHeight().clip(if (round) RoundedCornerShape(0.dp) else RoundedCornerShape(topStart = top, bottomStart = bottom, topEnd = inner, bottomEnd = inner))
                                .background(bg).padding(horizontal = 16.dp, vertical = 12.dp),
                        ) { TesseraText(label, style = Tessera.type.bodySm, color = c.textSecondary) }
                        Box(
                            Modifier.weight(1f).fillMaxHeight().clip(if (round) RoundedCornerShape(0.dp) else RoundedCornerShape(topEnd = top, bottomEnd = bottom, topStart = inner, bottomStart = inner))
                                .background(bg).padding(horizontal = 16.dp, vertical = 12.dp),
                        ) { TesseraText(value, style = Tessera.type.bodySm.copy(fontWeight = FontWeight.Medium), color = c.textPrimary) }
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------
// 时间线 Timeline：小砖节点 + 竖向砖缝连线
// ------------------------------------------------------------------

data class TesseraTimelineItem(
    val title: String,
    val time: String? = null,
    val body: String? = null,
    val tone: TesseraTone = TesseraTone.Neutral,
)

@Composable
fun TesseraTimeline(items: List<TesseraTimelineItem>, modifier: Modifier = Modifier) {
    val c = Tessera.colors
    val round = isRound
    Column(modifier.fillMaxWidth()) {
        items.forEachIndexed { i, it ->
            val dot = when (it.tone) {
                TesseraTone.Accent -> c.accentControl
                TesseraTone.Success -> c.successSolid
                TesseraTone.Warning -> c.warningSolid
                TesseraTone.Danger -> c.dangerSolid
                else -> c.fillControlOff
            }
            val last = i == items.size - 1
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).semantics(mergeDescendants = true) {}, horizontalArrangement = Arrangement.spacedBy(TesseraSpace.s3)) {
                Box(Modifier.width(28.dp).fillMaxHeight()) {
                    if (!last) Box(
                        Modifier.align(Alignment.TopCenter).padding(top = 22.dp).width(3.dp).fillMaxHeight()
                            .clip(RoundedCornerShape(if (round) 50 else 30)).background(c.fillDefault),
                    )
                    Box(
                        Modifier.align(Alignment.TopCenter).padding(top = 4.dp).size(14.dp).clip(if (round) CircleShape else RoundedCornerShape(5.dp)).background(dot)
                            .then(if (it.tone == TesseraTone.Accent) Modifier.drawBehind { drawRect(Brush.verticalGradient(listOf(c.sheen, Color.Transparent))) } else Modifier),
                    )
                }
                Column(Modifier.weight(1f).padding(bottom = if (last) 0.dp else TesseraSpace.s5, top = 1.dp)) {
                    if (it.time != null) TesseraText(it.time, style = Tessera.type.caption, color = c.textTertiary)
                    TesseraText(it.title, style = Tessera.type.label, color = c.textPrimary)
                    if (it.body != null) TesseraText(it.body, Modifier.padding(top = 2.dp), style = Tessera.type.bodySm, color = c.textSecondary)
                }
            }
        }
    }
}

// ------------------------------------------------------------------
// 统计数字 Stat：一排大数字砖，首尾外角大；圆融是一整块面板
// ------------------------------------------------------------------

enum class TesseraTrend { Up, Down }

data class TesseraStatItem(
    val label: String,
    val value: String,
    val unit: String? = null,
    val delta: String? = null,
    val trend: TesseraTrend? = null,
)

@Composable
fun TesseraStats(items: List<TesseraStatItem>, modifier: Modifier = Modifier, columns: Int = 2) {
    val c = Tessera.colors
    val round = isRound
    val rows = items.chunked(columns)
    val gap = if (round) 0.dp else TesseraTile.grout
    val panel = RoundedCornerShape(roundRadius(TesseraRound.card))
    Column(
        modifier.fillMaxWidth().then(
            if (round) Modifier.shadow(3.dp, panel, ambientColor = c.shadowColor, spotColor = c.shadowColor).clip(panel).background(c.bgSurface) else Modifier,
        ),
        verticalArrangement = Arrangement.spacedBy(gap),
    ) {
        rows.forEachIndexed { r, line ->
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(gap)) {
                for (k in 0 until columns) {
                    val item = line.getOrNull(k)
                    if (item == null) { Spacer(Modifier.weight(1f)); continue }
                    val xl = TesseraRadius.xl; val g = TesseraRadius.grout
                    val lastInRow = k == line.size - 1
                    val shape = RoundedCornerShape(
                        topStart = if (r == 0 && k == 0) xl else g,
                        topEnd = if (r == 0 && (lastInRow || k == columns - 1)) xl else g,
                        bottomStart = if (r == rows.size - 1 && k == 0) xl else g,
                        bottomEnd = if (r == rows.size - 1 && lastInRow) xl else g,
                    )
                    StatTile(item, Modifier.weight(1f).fillMaxHeight().then(if (round) Modifier else Modifier.clip(shape).background(c.bgSurface).then(if (showOutline) Modifier.border(1.dp, c.borderSurface, shape) else Modifier)))
                }
            }
        }
    }
}

@Composable
private fun StatTile(item: TesseraStatItem, modifier: Modifier) {
    val c = Tessera.colors
    val trendText = when (item.trend) { TesseraTrend.Up -> "上升"; TesseraTrend.Down -> "下降"; null -> "" }
    Column(
        modifier.clearAndSetSemantics {
            contentDescription = buildString {
                append("${item.label} ${item.value}${item.unit ?: ""}")
                if (item.delta != null) append("，较上期$trendText ${item.delta}")
            }
        }.padding(TesseraSpace.s5),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        TesseraText(item.label, style = Tessera.type.caption, color = c.textSecondary)
        Row(verticalAlignment = Alignment.Bottom) {
            TesseraText(item.value, style = Tessera.type.display.copy(fontSize = 32.sp, lineHeight = 37.sp, fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum", letterSpacing = (-0.6).sp), color = c.textPrimary, maxLines = 1)
            if (item.unit != null) TesseraText(item.unit, Modifier.padding(start = 4.dp, bottom = 6.dp), style = Tessera.type.bodySm.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium), color = c.textSecondary)
        }
        if (item.delta != null) {
            val fg = when (item.trend) { TesseraTrend.Up -> c.successText; TesseraTrend.Down -> c.dangerText; null -> c.textSecondary }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                if (item.trend != null) TesseraIcon(if (item.trend == TesseraTrend.Up) TesseraIcons.ArrowUp else TesseraIcons.ArrowDown, null, tint = fg, size = 14.dp)
                TesseraText(item.delta, style = Tessera.type.caption.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold), color = fg)
            }
        }
    }
}
