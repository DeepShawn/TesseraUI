package com.tesseraui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.tesseraui.LocalReduceMotion
import com.tesseraui.Tessera
import com.tesseraui.TesseraMotion
import com.tesseraui.TesseraRadius
import com.tesseraui.TesseraSize
import com.tesseraui.TesseraSpace
import com.tesseraui.TesseraText
import com.tesseraui.foundation.TesseraHaptic
import com.tesseraui.foundation.isFlow
import com.tesseraui.foundation.isRound
import com.tesseraui.foundation.raisedSolid
import com.tesseraui.foundation.rememberFlowHaptic
import com.tesseraui.foundation.shapeOf
import com.tesseraui.foundation.shapeRadius
import com.tesseraui.foundation.showOutline
import com.tesseraui.foundation.sunken
import com.tesseraui.icons.TesseraIcon
import com.tesseraui.icons.TesseraIcons
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

// ------------------------------------------------------------------
// 日期（不依赖 java.time：minSdk 24 上 java.time 需要额外的 desugaring）
// ------------------------------------------------------------------

/** 公历日期。month 为 1–12 */
data class TesseraDate(val year: Int, val month: Int, val day: Int) : Comparable<TesseraDate> {
    override fun compareTo(other: TesseraDate) = compareValuesBy(this, other, { it.year }, { it.month }, { it.day })
    /** 星期：1 = 周一 … 7 = 周日 */
    val dayOfWeek: Int get() {
        // Zeller 同余（以周六为 0），换算成周一为 1
        var y = year; var m = month
        if (m < 3) { m += 12; y -= 1 }
        val h = (day + (13 * (m + 1)) / 5 + y + y / 4 - y / 100 + y / 400) % 7
        return ((h + 5) % 7) + 1
    }
    fun plusDays(n: Int): TesseraDate = fromEpochDay(epochDay + n)
    fun plusMonths(n: Int): TesseraDate {
        val total = year * 12 + (month - 1) + n
        val y = Math.floorDiv(total, 12); val m = Math.floorMod(total, 12) + 1
        return TesseraDate(y, m, minOf(day, daysInMonth(y, m)))
    }
    val epochDay: Long get() {
        var y = year.toLong(); val m = month.toLong()
        var total = 365 * y
        total += if (y >= 0) (y + 3) / 4 - (y + 99) / 100 + (y + 399) / 400 else -(y / -4 - y / -100 + y / -400)
        total += (367 * m - 362) / 12
        total += day - 1
        if (m > 2) { total--; if (!isLeap(year)) total-- }
        return total - 719528
    }
    /** “2026 年 11 月 19 日” */
    override fun toString() = "$year 年 $month 月 $day 日"

    companion object {
        fun isLeap(y: Int) = (y % 4 == 0 && y % 100 != 0) || y % 400 == 0
        fun daysInMonth(y: Int, m: Int) = when (m) { 2 -> if (isLeap(y)) 29 else 28; 4, 6, 9, 11 -> 30; else -> 31 }
        fun today(): TesseraDate {
            val c = java.util.Calendar.getInstance()
            return TesseraDate(c.get(java.util.Calendar.YEAR), c.get(java.util.Calendar.MONTH) + 1, c.get(java.util.Calendar.DAY_OF_MONTH))
        }
        fun fromEpochDay(epoch: Long): TesseraDate {
            // 来自 java.time.LocalDate.ofEpochDay 的同一算法
            var zeroDay = epoch + 719528 - 60
            var adjust = 0L
            if (zeroDay < 0) { val c = (zeroDay + 1) / 146097 - 1; adjust = c * 400; zeroDay += -c * 146097 }
            var yearEst = (400 * zeroDay + 591) / 146097
            var doyEst = zeroDay - (365 * yearEst + yearEst / 4 - yearEst / 100 + yearEst / 400)
            if (doyEst < 0) { yearEst--; doyEst = zeroDay - (365 * yearEst + yearEst / 4 - yearEst / 100 + yearEst / 400) }
            yearEst += adjust
            val marchDoy0 = doyEst.toInt()
            val marchMonth0 = (marchDoy0 * 5 + 2) / 153
            val month = (marchMonth0 + 2) % 12 + 1
            val dom = marchDoy0 - (marchMonth0 * 306 + 5) / 10 + 1
            yearEst += marchMonth0 / 10
            return TesseraDate(yearEst.toInt(), month, dom)
        }
    }
}

private val WEEK = listOf("一", "二", "三", "四", "五", "六", "日")

// ------------------------------------------------------------------
// 日历 Calendar：日期是一块块小砖；选中砖用弹簧滑过去，换月时整月推入推出
// ------------------------------------------------------------------

/**
 * 日历。单选传 selected / onSelect；范围选择传 rangeStart / rangeEnd / onRangeChange（第一次点起点，第二次点终点）。
 * 中国习惯，周一开始；今天下方有一个小点。
 */
@Composable
fun TesseraCalendar(
    modifier: Modifier = Modifier,
    selected: TesseraDate? = null,
    onSelect: ((TesseraDate) -> Unit)? = null,
    rangeStart: TesseraDate? = null,
    rangeEnd: TesseraDate? = null,
    onRangeChange: ((TesseraDate?, TesseraDate?) -> Unit)? = null,
    min: TesseraDate? = null,
    max: TesseraDate? = null,
    initialMonth: TesseraDate = selected ?: rangeStart ?: TesseraDate.today(),
    /** “今天”（下方有小点）；一般不用传，截图测试里固定一个日期 */
    today: TesseraDate = remember { TesseraDate.today() },
) {
    val c = Tessera.colors
    val haptic = rememberFlowHaptic()
    var view by remember { mutableStateOf(TesseraDate(initialMonth.year, initialMonth.month, 1)) }
    var dir by remember { mutableIntStateOf(1) }
    val range = onRangeChange != null
    Column(modifier.width(308.dp), verticalArrangement = Arrangement.spacedBy(TesseraSpace.s2)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TesseraIconButton(TesseraIcons.ChevronLeft, "上个月", { dir = -1; view = view.plusMonths(-1) }, size = TesseraButtonSize.Sm)
            TesseraText("${view.year} 年 ${view.month} 月", style = Tessera.type.title3, modifier = Modifier.weight(1f).padding(start = TesseraSpace.s2))
            TesseraIconButton(TesseraIcons.ChevronRight, "下个月", { dir = 1; view = view.plusMonths(1) }, size = TesseraButtonSize.Sm)
        }
        Row(Modifier.fillMaxWidth()) {
            WEEK.forEach { w -> Box(Modifier.weight(1f).height(28.dp), contentAlignment = Alignment.Center) { TesseraText(w, style = Tessera.type.caption, color = c.textTertiary) } }
        }
        // 换月：旧的一个月整块滑出，新的从另一侧推入
        AnimatedContent(
            view,
            transitionSpec = {
                (slideInHorizontally(spring(stiffness = Spring.StiffnessMediumLow)) { it * dir } + fadeIn(tween(200)))
                    .togetherWith(slideOutHorizontally(spring(stiffness = Spring.StiffnessMediumLow)) { -it * dir } + fadeOut(tween(200)))
            },
            modifier = Modifier.clipToBounds(),
            label = "month",
        ) { month ->
            MonthGrid(month, today, selected, rangeStart, rangeEnd, min, max) { d ->
                haptic(TesseraHaptic.Selection)
                if (range) {
                    if (rangeStart == null || rangeEnd != null) onRangeChange!!(d, null)
                    else if (d < rangeStart) onRangeChange!!(d, rangeStart) else onRangeChange!!(rangeStart, d)
                } else onSelect?.invoke(d)
                if (d.month != month.month) { dir = if (d > month) 1 else -1; view = TesseraDate(d.year, d.month, 1) }
            }
        }
    }
}

@Composable
private fun MonthGrid(
    month: TesseraDate, today: TesseraDate, selected: TesseraDate?, start: TesseraDate?, end: TesseraDate?,
    min: TesseraDate?, max: TesseraDate?, onPick: (TesseraDate) -> Unit,
) {
    val c = Tessera.colors
    val first = TesseraDate(month.year, month.month, 1)
    val origin = first.plusDays(-(first.dayOfWeek - 1))
    val density = LocalDensity.current
    val gapPx = with(density) { 3.dp.toPx() }
    val round = isRound
    val selShape = if (round) CircleShape else RoundedCornerShape(12.dp)
    // 选中砖的位置（单选）：弹簧滑到新日期
    val selIndex = selected?.let { (it.epochDay - origin.epochDay).toInt() }?.takeIf { it in 0..41 && selected.month == month.month }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val cellDp = (maxWidth - 3.dp * 6) / 7
        // 格子尺寸由约束直接算出：选中砖第一帧就在正确的日期上，不会从左上角飞过来
        val cell = with(density) { cellDp.toPx() }
        val target = if (selIndex != null) Offset((selIndex % 7) * (cell + gapPx), (selIndex / 7) * (cell + gapPx)) else Offset.Zero
        val pos by animateOffsetAsState(target, spring(dampingRatio = 0.72f, stiffness = 500f), label = "sel")
        if (selIndex != null) {
            Box(
                Modifier
                    .offset { IntOffset(pos.x.roundToInt(), pos.y.roundToInt()) }
                    .size(cellDp)
                    .clip(selShape)
                    .background(c.textPrimary)
                    .raisedSolid(),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            for (row in 0 until 6) Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                for (col in 0 until 7) {
                    val d = origin.plusDays(row * 7 + col)
                    val outside = d.month != month.month
                    val disabled = (min != null && d < min) || (max != null && d > max)
                    val isSel = d == selected || d == start || d == end
                    val inRange = start != null && end != null && d > start && d < end
                    val endpoint = (d == start || d == end) && start != null
                    val shape = when {
                        round && inRange -> RoundedCornerShape(0.dp)
                        round && endpoint && end != null && start != end -> if (d == start) RoundedCornerShape(topStartPercent = 50, bottomStartPercent = 50) else RoundedCornerShape(topEndPercent = 50, bottomEndPercent = 50)
                        round -> CircleShape
                        inRange -> RoundedCornerShape(6.dp)
                        else -> RoundedCornerShape(12.dp)
                    }
                    val bg = when {
                        endpoint -> c.textPrimary
                        inRange -> c.accentSubtle
                        else -> Color.Transparent
                    }
                    val fg = when {
                        isSel -> c.bgSurface
                        inRange -> c.accentText
                        outside -> c.textTertiary
                        col >= 5 -> c.textSecondary
                        else -> c.textPrimary
                    }
                    // 圆融 / 流韵：范围是一条连续的浅色带，跨过格子之间的 3dp 缝；端点的深色胶囊压在带子上
                    val band = round && start != null && end != null && start != end && (inRange || endpoint)
                    Box(
                        Modifier
                            .size(cellDp)
                            .then(if (band) Modifier.drawBehind {
                                // 每格只往右画到下一格（盖住 3dp 缝），互不重叠，半透明的浅色带不会出现深浅接缝
                                val g = 3.dp.toPx()
                                val left = if (d == start) size.width / 2 else 0f
                                val right = when { d == end -> size.width / 2; col == 6 -> size.width; else -> size.width + g }
                                drawRect(c.accentSubtle, Offset(left, 0f), androidx.compose.ui.geometry.Size(right - left, size.height))
                            } else Modifier)
                            .clip(shape)
                            .background(if (band && inRange) Color.Transparent else bg)
                            .then(if (!disabled) Modifier.clickable(role = Role.Button) { onPick(d) } else Modifier)
                            .semantics {
                                contentDescription = "$d，星期${WEEK[d.dayOfWeek - 1]}${if (d == today) "，今天" else ""}"
                                this.selected = isSel
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        TesseraText(
                            d.day.toString(),
                            style = Tessera.type.bodySm.copy(fontWeight = if (isSel) FontWeight.SemiBold else FontWeight.Normal),
                            color = if (disabled) fg.copy(alpha = 0.3f) else fg,
                        )
                        if (d == today) Box(
                            Modifier.align(Alignment.BottomCenter).padding(bottom = 5.dp).size(4.dp)
                                .clip(if (round) CircleShape else RoundedCornerShape(1.5.dp))
                                .background(if (isSel) c.bgSurface else c.accentText),
                        )
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------
// 滚轮 WheelPicker：与 Web 端同一套手感
// 拖动 1:1 跟手、拉出两端有橡皮筋阻力；松手按速度投射到最近一行，带着手指的速度进入略欠阻尼的弹簧，落定前轻轻回弹。
// 选项沿圆柱排开：每行 18°，越远越倾斜、越淡。
// ------------------------------------------------------------------

private const val WHEEL_OMEGA = 13f
private const val WHEEL_ZETA = 0.7f
private val WHEEL_ROW = 36.dp

/** 滚轮的一列。selectedIndex 落定（弹簧停下）后才回调 onSelect */
@Composable
fun TesseraWheel(
    items: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "滚轮",
    visibleRows: Int = 5,
) {
    val c = Tessera.colors
    val density = LocalDensity.current
    val rowPx = with(density) { WHEEL_ROW.toPx() }
    val maxPos = ((items.size - 1).coerceAtLeast(0)) * rowPx
    val pos = remember { Animatable(selectedIndex * rowPx) }
    val scope = rememberCoroutineScope()
    val haptic = rememberFlowHaptic()
    val flow = isFlow
    val reduce = LocalReduceMotion.current
    val select by rememberUpdatedState(onSelect)
    var lastMark by remember { mutableIntStateOf(selectedIndex) }
    LaunchedEffect(selectedIndex, items.size) {
        val t = selectedIndex.coerceIn(0, items.lastIndex.coerceAtLeast(0)) * rowPx
        if (!pos.isRunning && abs(pos.value - t) > 0.5f) pos.snapTo(t)
    }
    val wheelSpring = spring<Float>(dampingRatio = WHEEL_ZETA, stiffness = WHEEL_OMEGA * WHEEL_OMEGA)
    fun settleTo(index: Int, velocity: Float = 0f) {
        val i = index.coerceIn(0, items.lastIndex)
        scope.launch {
            if (reduce) pos.snapTo(i * rowPx) else pos.animateTo(i * rowPx, wheelSpring, velocity)
            select(i)
        }
    }
    val center = (pos.value / rowPx).roundToInt().coerceIn(0, items.lastIndex.coerceAtLeast(0))
    LaunchedEffect(center) { if (center != lastMark) { if (flow) haptic(TesseraHaptic.Selection); lastMark = center } }
    val step = 18f * PI.toFloat() / 180f
    val radius = rowPx / step
    Box(
        modifier
            .height(WHEEL_ROW * visibleRows)
            .clipToBounds()
            // 上下边缘淡出
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                drawRect(Brush.verticalGradient(0f to Color.Transparent, 0.28f to Color.Black, 0.72f to Color.Black, 1f to Color.Transparent), blendMode = BlendMode.DstIn)
            }
            .pointerInput(items.size) {
                val tracker = VelocityTracker()
                detectVerticalDragGestures(
                    onDragStart = { scope.launch { pos.stop() }; tracker.resetTracking() },
                    onDragEnd = {
                        val v = -tracker.calculateVelocity().y
                        val projected = pos.value + v * (2 * WHEEL_ZETA / WHEEL_OMEGA)
                        settleTo((projected / rowPx).roundToInt(), v)
                    },
                    onDragCancel = { settleTo((pos.value / rowPx).roundToInt()) },
                ) { change, dy ->
                    change.consume()
                    tracker.addPosition(change.uptimeMillis, change.position)
                    val raw = pos.value - dy
                    val h = size.height.toFloat()
                    // 橡皮筋：拉出两端时阻力越来越大
                    fun rubber(over: Float) = (1 - 1 / (abs(over) * 0.55f / h + 1)) * h * kotlin.math.sign(over)
                    val next = when { raw < 0 -> if (pos.value < 0) pos.value - dy * 0.35f else rubber(raw); raw > maxPos -> if (pos.value > maxPos) pos.value - dy * 0.35f else maxPos + rubber(raw - maxPos); else -> raw }
                    scope.launch { pos.snapTo(next) }
                }
            }
            .pointerInput(items.size) {
                detectTapGestures { o -> settleTo(((pos.value + o.y - size.height / 2f) / rowPx).roundToInt()) }
            }
            .semantics {
                contentDescription = label
                stateDescription = items.getOrNull(center) ?: ""
                progressBarRangeInfo = ProgressBarRangeInfo(center.toFloat(), 0f..items.lastIndex.coerceAtLeast(1).toFloat(), (items.size - 2).coerceAtLeast(0))
                setProgress { t -> settleTo(t.roundToInt()); true }
            },
        contentAlignment = Alignment.Center,
    ) {
        val cIdx = pos.value / rowPx
        val from = (cIdx - 4.5f).toInt().coerceAtLeast(0)
        val to = (cIdx + 4.5f).toInt().coerceAtMost(items.lastIndex)
        for (i in from..to) {
            val a = ((i - cIdx) * step).coerceIn(-PI.toFloat() / 2, PI.toFloat() / 2)
            val sel = i == center
            TesseraText(
                items[i],
                style = Tessera.type.body.copy(fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal),
                color = if (sel) c.textPrimary else c.textSecondary,
                maxLines = 1,
                modifier = Modifier.graphicsLayer {
                    translationY = radius * sin(a)
                    rotationX = -a * 180f / PI.toFloat()
                    cameraDistance = 12f * density.density
                    alpha = cos(a).coerceAtLeast(0f).let { Math.pow(it.toDouble(), 1.6).toFloat() }
                },
            )
        }
    }
}

/** 多列滚轮：中间一条凹槽选中带，列之间可插入分隔符（例如时间的冒号） */
@Composable
fun TesseraWheelPicker(
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit,
) {
    val c = Tessera.colors
    val shape = shapeOf(12.dp)
    Box(modifier.height(WHEEL_ROW * 5), contentAlignment = Alignment.Center) {
        Box(Modifier.fillMaxWidth().height(WHEEL_ROW).clip(shape).background(c.fillDefault).sunken())
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp), content = content)
    }
}

// ------------------------------------------------------------------
// 日期 / 时间 / 日期范围选择器：输入框外观的触发器，点开是底部抽屉
// ------------------------------------------------------------------

@Composable
private fun PickerField(label: String?, text: String, placeholder: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit, modifier: Modifier) {
    val c = Tessera.colors
    val shape = shapeOf(TesseraRadius.md)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (label != null) TesseraText(label, style = Tessera.type.label)
        Row(
            Modifier
                .fillMaxWidth()
                .height(TesseraSize.md)
                .clip(shape)
                .background(c.fillDefault)
                .sunken()
                .then(if (showOutline) Modifier.border(1.dp, c.borderField, shape) else Modifier)
                .clickable(role = Role.DropdownList, onClick = onClick)
                .semantics { contentDescription = label ?: placeholder; stateDescription = text.ifEmpty { "未选择" } }
                .padding(horizontal = if (isRound) TesseraSpace.s4 else TesseraSpace.s3),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TesseraSpace.s2),
        ) {
            TesseraIcon(icon, null, size = 18.dp, tint = c.textSecondary)
            TesseraText(text.ifEmpty { placeholder }, style = Tessera.type.bodySm, color = if (text.isEmpty()) c.textTertiary else c.textPrimary, maxLines = 1)
        }
    }
}

@Composable
private fun SheetActions(left: String, onLeft: () -> Unit, onConfirm: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = TesseraSpace.s4), horizontalArrangement = Arrangement.SpaceBetween) {
        TesseraButton(left, onLeft, variant = TesseraButtonVariant.Secondary)
        TesseraButton("确定", onConfirm)
    }
}

/** 日期选择器：手机上是年 / 月 / 日三列滚轮（换月份时只替换“日”那一列的天数） */
@Composable
fun TesseraDatePicker(
    value: TesseraDate?,
    onValueChange: (TesseraDate) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String = "选择日期",
    yearRange: IntRange = (TesseraDate.today().year - 50)..(TesseraDate.today().year + 50),
) {
    var open by remember { mutableStateOf(false) }
    PickerField(label, value?.toString() ?: "", placeholder, TesseraIcons.Calendar, { open = true }, modifier)
    val start = value ?: TesseraDate.today()
    var y by remember(open) { mutableIntStateOf(start.year) }
    var m by remember(open) { mutableIntStateOf(start.month) }
    var d by remember(open) { mutableIntStateOf(start.day) }
    TesseraBottomSheet(open, { open = false }, title = "选择日期", detents = listOf(0.5f)) {
        val years = yearRange.toList()
        val days = TesseraDate.daysInMonth(y, m)
        if (d > days) d = days
        TesseraWheelPicker(Modifier.fillMaxWidth()) {
            TesseraWheel(years.map { "$it 年" }, years.indexOf(y).coerceAtLeast(0), { y = years[it] }, Modifier.weight(1.2f), label = "年")
            TesseraWheel((1..12).map { "$it 月" }, m - 1, { m = it + 1 }, Modifier.weight(1f), label = "月")
            TesseraWheel((1..days).map { "$it 日" }, d - 1, { d = it + 1 }, Modifier.weight(1f), label = "日")
        }
        SheetActions("取消", { open = false }) { onValueChange(TesseraDate(y, m, d)); open = false }
    }
}

/** 时间选择器：时 / 分两列滚轮，分钟可按 step 取整（例如 5 分钟一格） */
@Composable
fun TesseraTimePicker(
    hour: Int?,
    minute: Int?,
    onValueChange: (hour: Int, minute: Int) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String = "选择时间",
    step: Int = 1,
) {
    var open by remember { mutableStateOf(false) }
    val text = if (hour != null && minute != null) "%02d:%02d".format(hour, minute) else ""
    PickerField(label, text, placeholder, TesseraIcons.Clock, { open = true }, modifier)
    var h by remember(open) { mutableIntStateOf(hour ?: 9) }
    var mi by remember(open) { mutableIntStateOf(((minute ?: 0) / step) * step) }
    TesseraBottomSheet(open, { open = false }, title = "选择时间", detents = listOf(0.5f)) {
        val minutes = (0 until 60 step step).toList()
        TesseraWheelPicker(Modifier.fillMaxWidth().padding(horizontal = TesseraSpace.s8)) {
            TesseraWheel((0..23).map { "%02d".format(it) }, h, { h = it }, Modifier.weight(1f), label = "时")
            TesseraText(":", style = Tessera.type.title3, color = Tessera.colors.textTertiary)
            TesseraWheel(minutes.map { "%02d".format(it) }, minutes.indexOf(mi).coerceAtLeast(0), { mi = minutes[it] }, Modifier.weight(1f), label = "分")
        }
        SheetActions("现在", {
            val c = java.util.Calendar.getInstance()
            onValueChange(c.get(java.util.Calendar.HOUR_OF_DAY), (c.get(java.util.Calendar.MINUTE) / step) * step); open = false
        }) { onValueChange(h, mi); open = false }
    }
}

/** 日期范围选择器：抽屉里是日历，第一次点起点、第二次点终点 */
@Composable
fun TesseraDateRangePicker(
    start: TesseraDate?,
    end: TesseraDate?,
    onValueChange: (TesseraDate, TesseraDate) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String = "选择日期范围",
) {
    var open by remember { mutableStateOf(false) }
    val text = if (start != null && end != null) "${start.month} 月 ${start.day} 日 – ${end.month} 月 ${end.day} 日" else ""
    PickerField(label, text, placeholder, TesseraIcons.Calendar, { open = true }, modifier)
    var s by remember(open) { mutableStateOf(start) }
    var e by remember(open) { mutableStateOf(end) }
    TesseraBottomSheet(open, { open = false }, title = "选择日期范围", detents = listOf(0.7f)) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            TesseraCalendar(rangeStart = s, rangeEnd = e, onRangeChange = { a, b -> s = a; e = b })
        }
        val nights = if (s != null && e != null) (e!!.epochDay - s!!.epochDay).toInt() else null
        TesseraText(if (nights != null) "共 ${nights + 1} 天" else "先选开始日期，再选结束日期", style = Tessera.type.caption, color = Tessera.colors.textSecondary, modifier = Modifier.padding(top = TesseraSpace.s2))
        SheetActions("清除", { s = null; e = null }) { if (s != null && e != null) { onValueChange(s!!, e!!); open = false } }
    }
}

// ------------------------------------------------------------------
// 颜色选择器 ColorPicker：饱和度 / 明度面板 + 色相条 + HEX 输入 + 预设色（TesseraUI 的颜料色）
// ------------------------------------------------------------------

private fun hsvToColor(h: Float, s: Float, v: Float): Color = Color.hsv(h.coerceIn(0f, 360f), s.coerceIn(0f, 1f), v.coerceIn(0f, 1f))
private fun colorToHsv(c: Color): FloatArray { val out = FloatArray(3); android.graphics.Color.colorToHSV(c.toArgb(), out); return out }
fun Color.toHex(): String = "#%06X".format(toArgb() and 0xFFFFFF)
private fun parseHex(s: String): Color? {
    val t = s.trim().removePrefix("#")
    val full = when (t.length) { 3 -> t.map { "$it$it" }.joinToString(""); 6 -> t; else -> return null }
    return full.toLongOrNull(16)?.let { Color(0xFF000000 or it) }
}

/** 颜色选择器。value 为不透明颜色 */
@Composable
fun TesseraColorPicker(
    value: Color,
    onValueChange: (Color) -> Unit,
    modifier: Modifier = Modifier,
    swatches: List<Color> = listOf(
        Tessera.colors.accentSolid, Tessera.colors.pigmentCinnabar, Tessera.colors.pigmentOchre, Tessera.colors.pigmentOlive,
        Tessera.colors.pigmentLapis, Tessera.colors.pigmentAmethyst, Tessera.colors.textPrimary,
    ),
) {
    val c = Tessera.colors
    val hsv = remember(value) { colorToHsv(value) }
    var h by remember { mutableFloatStateOf(hsv[0]) }
    var s by remember { mutableFloatStateOf(hsv[1]) }
    var v by remember { mutableFloatStateOf(hsv[2]) }
    LaunchedEffect(value) { val n = colorToHsv(value); if (hsvToColor(h, s, v).toHex() != value.toHex()) { h = n[0]; s = n[1]; v = n[2] } }
    fun emit() = onValueChange(hsvToColor(h, s, v))
    val panelShape = shapeOf(TesseraRadius.md, 18.dp)
    Column(modifier.width(280.dp), verticalArrangement = Arrangement.spacedBy(TesseraSpace.s3)) {
        // 饱和度（横）/ 明度（纵）面板
        var panel by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1.6f)
                .clip(panelShape)
                .onSizeChanged { panel = it }
                .pointerInput(Unit) {
                    fun at(o: Offset) { s = (o.x / size.width).coerceIn(0f, 1f); v = 1f - (o.y / size.height).coerceIn(0f, 1f); emit() }
                    detectDragGestures(onDragStart = { at(it) }) { ch, _ -> ch.consume(); at(ch.position) }
                }
                .pointerInput(Unit) { detectTapGestures { o -> s = (o.x / size.width).coerceIn(0f, 1f); v = 1f - (o.y / size.height).coerceIn(0f, 1f); emit() } }
                .semantics {
                    contentDescription = "饱和度与明度"
                    stateDescription = "饱和度 ${(s * 100).roundToInt()}%，明度 ${(v * 100).roundToInt()}%"
                    progressBarRangeInfo = ProgressBarRangeInfo(s, 0f..1f)
                    setProgress { t -> s = t.coerceIn(0f, 1f); emit(); true }
                },
        ) {
            Canvas(Modifier.matchParentSize()) {
                drawRect(Brush.horizontalGradient(listOf(Color.White, hsvToColor(h, 1f, 1f))))
                drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
                val p = Offset(s * size.width, (1 - v) * size.height)
                drawCircle(Color.White, 9.dp.toPx(), p, style = Stroke(3.dp.toPx()))
                drawCircle(Color.Black.copy(alpha = 0.25f), 10.5.dp.toPx(), p, style = Stroke(1.dp.toPx()))
            }
        }
        // 色相条
        Box(
            Modifier
                .fillMaxWidth()
                .height(TesseraSize.touchMin)
                .pointerInput(Unit) {
                    fun at(x: Float) { h = (x / size.width).coerceIn(0f, 1f) * 360f; emit() }
                    detectDragGestures(onDragStart = { at(it.x) }) { ch, _ -> ch.consume(); at(ch.position.x) }
                }
                .pointerInput(Unit) { detectTapGestures { o -> h = (o.x / size.width).coerceIn(0f, 1f) * 360f; emit() } }
                .semantics {
                    contentDescription = "色相"
                    stateDescription = "${h.roundToInt()} 度"
                    progressBarRangeInfo = ProgressBarRangeInfo(h, 0f..360f)
                    setProgress { t -> h = t.coerceIn(0f, 360f); emit(); true }
                },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.fillMaxWidth().height(14.dp)) {
                drawRoundRect(Brush.horizontalGradient((0..6).map { hsvToColor(it * 60f, 1f, 1f) }), cornerRadius = androidx.compose.ui.geometry.CornerRadius(7.dp.toPx()))
                val x = h / 360f * size.width
                drawCircle(Color.White, 10.dp.toPx(), Offset(x.coerceIn(10.dp.toPx(), size.width - 10.dp.toPx()), size.height / 2))
                drawCircle(hsvToColor(h, 1f, 1f), 7.dp.toPx(), Offset(x.coerceIn(10.dp.toPx(), size.width - 10.dp.toPx()), size.height / 2))
            }
        }
        // HEX 输入
        var hex by remember { mutableStateOf(value.toHex()) }
        LaunchedEffect(value) { if (parseHex(hex)?.toHex() != value.toHex()) hex = value.toHex() }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(TesseraSpace.s2)) {
            Box(Modifier.size(TesseraSize.md).clip(shapeOf(TesseraRadius.md)).background(value).border(1.dp, c.borderSubtle, shapeOf(TesseraRadius.md)))
            TesseraTextField(
                hex, { hex = it; parseHex(it)?.let(onValueChange) },
                modifier = Modifier.weight(1f),
                placeholder = "#0F6B63",
                error = if (parseHex(hex) == null) "请输入 6 位十六进制颜色，如 #0F6B63" else null,
            )
        }
        // 预设色
        Row(horizontalArrangement = Arrangement.spacedBy(TesseraSpace.s2)) {
            swatches.forEach { sw ->
                val on = sw.toHex() == value.toHex()
                Box(
                    Modifier
                        .size(32.dp)
                        .clip(shapeOf(9.dp))
                        .background(sw)
                        .then(if (on) Modifier.border(2.dp, c.focusRing, shapeOf(9.dp)) else Modifier)
                        .clickable(role = Role.Button) { onValueChange(sw) }
                        .semantics { contentDescription = "颜色 ${sw.toHex()}"; selected = on },
                    contentAlignment = Alignment.Center,
                ) { if (on) TesseraIcon(TesseraIcons.Check, null, size = 16.dp, tint = if (sw.luminance() > 0.5f) Color.Black else Color.White) }
            }
        }
    }
}
