package com.tesseraui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.selection.triStateToggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.tesseraui.Tessera
import com.tesseraui.TesseraMotion
import com.tesseraui.TesseraOpacity
import com.tesseraui.TesseraRadius
import com.tesseraui.TesseraSize
import com.tesseraui.TesseraSpace
import com.tesseraui.TesseraText
import com.tesseraui.TesseraTile
import com.tesseraui.foundation.TilePosition
import com.tesseraui.foundation.glaze
import com.tesseraui.foundation.pressSink
import com.tesseraui.foundation.sunken
import com.tesseraui.foundation.tilePosition
import com.tesseraui.foundation.tileShape
import com.tesseraui.motion
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/** 选择控件右侧的文字标签（点击文字也能切换） */
@Composable
private fun Labeled(label: String?, content: @Composable () -> Unit) {
    if (label == null) { content(); return }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(TesseraSpace.s3)) {
        content()
        TesseraText(label, style = Tessera.type.bodySm)
    }
}

// ------------------------------------------------------------------
// 复选框 Checkbox：150ms 填色，勾号从左往右描出来；支持半选
// ------------------------------------------------------------------

@Composable
fun TesseraCheckbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    label: String? = null,
    indeterminate: Boolean = false,
    enabled: Boolean = true,
) {
    val c = Tessera.colors
    val on = checked || indeterminate
    val fill by animateColorAsState(if (on) c.accentControl else c.fillDefault, tween(TesseraMotion.DURATION_FAST), label = "fill")
    val draw by animateFloatAsState(if (checked) 1f else 0f, tween(TesseraMotion.DURATION_FAST, delayMillis = if (checked) 40 else 0), label = "draw")
    val interaction = remember { MutableInteractionSource() }
    val state = when { indeterminate -> ToggleableState.Indeterminate; checked -> ToggleableState.On; else -> ToggleableState.Off }
    Box(
        modifier
            .defaultMinSize(minHeight = TesseraSize.touchMin)
            .triStateToggleable(state, interaction, null, enabled = enabled && onCheckedChange != null, role = Role.Checkbox) { onCheckedChange?.invoke(!checked) }
            .alpha(if (enabled) 1f else TesseraOpacity.DISABLED),
        contentAlignment = Alignment.CenterStart,
    ) {
        Labeled(label) {
            val shape = RoundedCornerShape(7.dp)
            Box(
                Modifier.pressSink(interaction, enabled).size(20.dp).clip(shape).background(fill)
                    .then(if (on) Modifier.glaze(c.edgeSolid, c.sheen, 1.5.dp) else Modifier.border(1.5.dp, c.borderStrong, shape)),
            ) {
                val mark = c.textOnSolid
                Canvas(Modifier.size(20.dp)) {
                    if (indeterminate) {
                        drawRoundRect(mark, topLeft = Offset(size.width * 0.25f, size.height / 2 - 1.dp.toPx()), size = Size(size.width * 0.5f, 2.dp.toPx()), cornerRadius = CornerRadius(1.dp.toPx()))
                        return@Canvas
                    }
                    // 勾号从左往右露出
                    clipRect(right = size.width * (0.2f + 0.65f * draw)) {
                        val p = Path().apply {
                            moveTo(size.width * 0.27f, size.height * 0.52f)
                            lineTo(size.width * 0.44f, size.height * 0.69f)
                            lineTo(size.width * 0.75f, size.height * 0.33f)
                        }
                        if (draw > 0f) drawPath(p, mark, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------
// 单选 Radio：选中时中间的小方砖从 −45° 转正并放大（snappy）
// ------------------------------------------------------------------

@Composable
fun TesseraRadio(selected: Boolean, onClick: (() -> Unit)?, modifier: Modifier = Modifier, label: String? = null, enabled: Boolean = true) {
    val c = Tessera.colors
    val p by animateFloatAsState(if (selected) 1f else 0f, motion(TesseraMotion.snappy()), label = "dot")
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier
            .defaultMinSize(minHeight = TesseraSize.touchMin)
            .selectable(selected, interaction, null, enabled = enabled && onClick != null, role = Role.RadioButton) { onClick?.invoke() }
            .alpha(if (enabled) 1f else TesseraOpacity.DISABLED),
        contentAlignment = Alignment.CenterStart,
    ) {
        Labeled(label) {
            val fill = if (selected) c.accentControl else c.fillDefault
            Box(
                Modifier.pressSink(interaction, enabled).size(20.dp).clip(CircleShape).background(fill)
                    .then(if (selected) Modifier.glaze(c.edgeSolid, c.sheen, 1.5.dp) else Modifier.border(1.5.dp, c.borderStrong, CircleShape)),
            ) {
                val dot = c.textOnSolid
                Canvas(Modifier.size(20.dp)) {
                    if (p <= 0.01f) return@Canvas
                    rotate(-45f * (1 - p)) {
                        scale(p) {
                            val s = 8.dp.toPx()
                            drawRoundRect(dot, topLeft = Offset((size.width - s) / 2, (size.height - s) / 2), size = Size(s, s), cornerRadius = CornerRadius(3.dp.toPx()))
                        }
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------
// 开关 Switch：可点击，也可按住滑块左右拖动（拖出两端有阻力，松手按位置或速度决定）
// ------------------------------------------------------------------

@Composable
fun TesseraSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    label: String? = null,
    enabled: Boolean = true,
    glass: Boolean = false,
) {
    val c = Tessera.colors
    val density = LocalDensity.current
    val trackW = 52.dp; val trackH = 30.dp; val pad = 3.dp; val thumb = 24.dp
    val stretchMax = 6.dp
    val pos = remember { Animatable(if (checked) 1f else 0f) }
    var dragging by remember { mutableStateOf(false) }
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val stretch by animateDpAsState(if (pressed || dragging) stretchMax else 0.dp, motion(TesseraMotion.snappy()), label = "stretch")
    val scope = rememberCoroutineScope()
    val snappy = motion(TesseraMotion.snappy<Float>())
    val latestChecked by rememberUpdatedState(checked)
    LaunchedEffect(checked) { if (!dragging) pos.animateTo(if (checked) 1f else 0f, snappy) }
    val travelPx = with(density) { (trackW - pad * 2 - thumb - stretchMax).toPx() }
    val preview = if (dragging) pos.value > 0.5f else checked
    val track by animateColorAsState(if (preview) c.accentControl else c.fillControlOff, tween(TesseraMotion.DURATION_NORMAL), label = "track")
    val dragState = rememberDraggableState { delta ->
        scope.launch {
            val raw = pos.value + delta / travelPx
            // 超出两端时阻力越来越大
            pos.snapTo(if (raw < 0f) raw * 0.3f else if (raw > 1f) 1f + (raw - 1f) * 0.3f else raw)
        }
    }
    Box(
        modifier
            .defaultMinSize(minHeight = TesseraSize.touchMin)
            .toggleable(checked, interaction, null, enabled = enabled && onCheckedChange != null, role = Role.Switch) { onCheckedChange?.invoke(it) }
            .alpha(if (enabled) 1f else TesseraOpacity.DISABLED),
        contentAlignment = Alignment.CenterStart,
    ) {
        Labeled(label) {
            val shape = RoundedCornerShape(13.dp)
            Box(
                Modifier
                    .size(trackW, trackH)
                    .clip(shape)
                    .background(track)
                    .sunken()
                    .border(1.dp, c.borderField, shape)
                    .draggable(
                        dragState, Orientation.Horizontal, enabled = enabled && onCheckedChange != null,
                        onDragStarted = { dragging = true },
                        onDragStopped = { v ->
                            val fling = with(density) { TesseraMotion.FLING_VELOCITY_DP.dp.toPx() }
                            val next = if (abs(v) > fling) v > 0 else pos.value > 0.5f
                            dragging = false
                            pos.animateTo(if (next) 1f else 0f, snappy)
                            if (next != latestChecked) onCheckedChange?.invoke(next)
                        },
                    ),
            ) {
                // 开 / 关标记：开启时左侧竖线，关闭时右侧小圆圈
                val on = c.textOnSolid.copy(alpha = 0.7f); val off = c.textTertiary
                Canvas(Modifier.size(trackW, trackH)) {
                    if (dragging) return@Canvas
                    if (checked) drawRect(on, topLeft = Offset(15.dp.toPx(), size.height / 2 - 4.5.dp.toPx()), size = Size(1.5.dp.toPx(), 9.dp.toPx()))
                    else drawCircle(off, radius = 3.8.dp.toPx(), center = Offset(size.width - 15.dp.toPx(), size.height / 2), style = Stroke(1.2.dp.toPx()))
                }
                val x = with(density) { (pad.toPx() + pos.value * (travelPx + (stretchMax - stretch).toPx()) - (if (pos.value > 0.5f) 0f else 0f)).roundToInt() }
                val thumbShape = RoundedCornerShape(10.dp)
                Box(
                    Modifier
                        .offset { IntOffset(x, with(density) { pad.roundToPx() }) }
                        .size(thumb + stretch, thumb)
                        .shadow(3.dp, thumbShape, ambientColor = c.shadowColor, spotColor = c.shadowColor)
                        .clip(thumbShape)
                        .then(
                            if (glass) Modifier.background(c.materialGlassThumb).background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.55f), Color.Transparent))).border(1.dp, c.materialGlassRim, thumbShape)
                            else Modifier.background(Color.White).background(Brush.verticalGradient(0.55f to Color.Transparent, 1f to Color(0x141F1B16))),
                        ),
                )
            }
        }
    }
}

// ------------------------------------------------------------------
// 分段控制 SegmentedControl：一排砖 + 滑动的墨色选中砖
// 切换时前沿 snappy、后沿 smooth（移动中自然拉长）；可以按住左右拖动选择
// ------------------------------------------------------------------

@Composable
fun TesseraSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    small: Boolean = false,
) {
    val c = Tessera.colors
    val density = LocalDensity.current
    val height = if (small) TesseraSize.sm else TesseraSize.md
    val outer = if (small) 11.dp else TesseraRadius.md
    BoxWithConstraints(modifier.fillMaxWidth().height(height).selectableGroup()) {
        val n = options.size
        val gapPx = with(density) { TesseraTile.grout.toPx() }
        val itemPx = (constraints.maxWidth - gapPx * (n - 1)) / n
        fun leftOf(i: Int) = i * (itemPx + gapPx)
        val left = remember { Animatable(leftOf(selectedIndex)) }
        val right = remember { Animatable(leftOf(selectedIndex) + itemPx) }
        val snappy = motion(TesseraMotion.snappy<Float>()); val smooth = motion(TesseraMotion.smooth<Float>())
        var dragging by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()
        LaunchedEffect(selectedIndex, itemPx) {
            if (dragging) return@LaunchedEffect
            val l = leftOf(selectedIndex); val r = l + itemPx
            val goingRight = l > left.value
            launch { left.animateTo(l, if (goingRight) smooth else snappy) }
            launch { right.animateTo(r, if (goingRight) snappy else smooth) }
        }
        val latest by rememberUpdatedState(selectedIndex)
        val drag = rememberDraggableState { d -> scope.launch { val max = leftOf(n - 1); val nl = (left.value + d).coerceIn(-itemPx * 0.15f, max + itemPx * 0.15f); left.snapTo(nl); right.snapTo(nl + itemPx) } }
        // 底砖
        Row(Modifier.fillMaxWidth().fillMaxHeight(), horizontalArrangement = Arrangement.spacedBy(TesseraTile.grout)) {
            options.indices.forEach { i ->
                Box(Modifier.weight(1f).fillMaxHeight().clip(tileShape(tilePosition(i, n), outer)).background(c.fillDefault).sunken())
            }
        }
        // 选中砖（圆角跟随落脚的那块砖）
        val idxNow = ((left.value + itemPx / 2) / (itemPx + gapPx)).toInt().coerceIn(0, n - 1)
        val indicatorShape = tileShape(tilePosition(idxNow, n), outer)
        Box(
            Modifier
                .offset { IntOffset(left.value.roundToInt(), 0) }
                .width(with(density) { (right.value - left.value).coerceAtLeast(0f).toDp() })
                .fillMaxHeight()
                .shadow(2.dp, indicatorShape, ambientColor = c.shadowColor, spotColor = c.shadowColor)
                .clip(indicatorShape)
                .background(c.textPrimary)
                .glaze(c.edgeSolid, c.sheen),
        )
        // 文字与点击区域
        Row(
            Modifier.fillMaxWidth().fillMaxHeight().draggable(drag, Orientation.Horizontal,
                onDragStarted = { dragging = true },
                onDragStopped = { v ->
                    dragging = false
                    var i = ((left.value + itemPx / 2) / (itemPx + gapPx)).roundToInt().coerceIn(0, n - 1)
                    val fling = with(density) { TesseraMotion.FLING_VELOCITY_DP.dp.toPx() }
                    if (abs(v) > fling && i == latest) i = (i + if (v > 0) 1 else -1).coerceIn(0, n - 1)
                    val l = leftOf(i)
                    launch { left.animateTo(l, snappy, initialVelocity = v) }
                    launch { right.animateTo(l + itemPx, snappy, initialVelocity = v) }
                    if (i != latest) onSelect(i)
                }),
            horizontalArrangement = Arrangement.spacedBy(TesseraTile.grout),
        ) {
            options.forEachIndexed { i, o ->
                val selected = i == selectedIndex
                val fg by animateColorAsState(if (selected) c.bgSurface else c.textSecondary, tween(TesseraMotion.DURATION_NORMAL), label = "fg")
                Box(
                    Modifier.weight(1f).fillMaxHeight().selectable(selected, role = Role.Tab) { onSelect(i) },
                    contentAlignment = Alignment.Center,
                ) { TesseraText(o, style = Tessera.type.label, color = fg, maxLines = 1) }
            }
        }
    }
}

