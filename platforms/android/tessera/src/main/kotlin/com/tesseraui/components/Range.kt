package com.tesseraui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.tesseraui.LocalReduceMotion
import com.tesseraui.Tessera
import com.tesseraui.TesseraMotion
import com.tesseraui.TesseraOpacity
import com.tesseraui.TesseraRadius
import com.tesseraui.TesseraSize
import com.tesseraui.TesseraSpace
import com.tesseraui.TesseraText
import com.tesseraui.foundation.TesseraHaptic
import com.tesseraui.foundation.isRound
import com.tesseraui.foundation.rememberFlowHaptic
import com.tesseraui.foundation.shapeOf
import com.tesseraui.foundation.shapeRadius
import com.tesseraui.foundation.showOutline
import com.tesseraui.foundation.sunken
import com.tesseraui.icons.TesseraIcon
import com.tesseraui.icons.TesseraIcons
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// ------------------------------------------------------------------
// 滑块 Slider：凹槽轨道 + 釉色填充 + 白色小砖滑块；拖动时滑块放大、上方冒出数值气泡
// 单值用 TesseraSlider，范围用 TesseraRangeSlider（两个滑块不能交叉）
// ------------------------------------------------------------------

private val THUMB = 22.dp
private val TRACK = 8.dp

/**
 * 滑块。step > 0 时按步长吸附，并在轨道上画刻度（不超过 20 个）。
 * 读屏可上下滑动调整（每次一个步长，或 1% 的范围）。
 */
@Composable
fun TesseraSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    range: ClosedFloatingPointRange<Float> = 0f..100f,
    step: Float = 0f,
    label: String? = null,
    enabled: Boolean = true,
    format: (Float) -> String = { it.roundToInt().toString() },
) {
    SliderCore(listOf(value), { onValueChange(it[0]) }, modifier, range, step, label, enabled, format)
}

/** 范围滑块：两个滑块分别调整起点和终点，不能交叉 */
@Composable
fun TesseraRangeSlider(
    value: ClosedFloatingPointRange<Float>,
    onValueChange: (ClosedFloatingPointRange<Float>) -> Unit,
    modifier: Modifier = Modifier,
    range: ClosedFloatingPointRange<Float> = 0f..100f,
    step: Float = 0f,
    label: String? = null,
    enabled: Boolean = true,
    format: (Float) -> String = { it.roundToInt().toString() },
) {
    SliderCore(listOf(value.start, value.endInclusive), { onValueChange(it[0]..it[1]) }, modifier, range, step, label, enabled, format)
}

@Composable
private fun SliderCore(
    values: List<Float>,
    onChange: (List<Float>) -> Unit,
    modifier: Modifier,
    range: ClosedFloatingPointRange<Float>,
    step: Float,
    label: String?,
    enabled: Boolean,
    format: (Float) -> String,
) {
    val c = Tessera.colors
    val haptic = rememberFlowHaptic()
    val latest by rememberUpdatedState(values)
    val span = (range.endInclusive - range.start).coerceAtLeast(1e-6f)
    fun snap(v: Float): Float {
        val clamped = v.coerceIn(range.start, range.endInclusive)
        return if (step > 0f) (range.start + ((clamped - range.start) / step).roundToInt() * step).coerceIn(range.start, range.endInclusive) else clamped
    }
    fun set(i: Int, v: Float) {
        val list = latest.toMutableList()
        var nv = snap(v)
        if (list.size == 2) nv = if (i == 0) nv.coerceAtMost(list[1]) else nv.coerceAtLeast(list[0])
        if (nv != list[i]) {
            if (step > 0f) haptic(TesseraHaptic.Selection)
            list[i] = nv; onChange(list)
        }
    }
    var active by remember { mutableIntStateOf(-1) }
    val density = LocalDensity.current
    val thumbPx = with(density) { THUMB.toPx() }
    val thumbShape = if (isRound) RoundedCornerShape(50) else RoundedCornerShape(8.dp)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(TesseraSpace.s2)) {
        if (label != null) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TesseraText(label, style = Tessera.type.label)
            TesseraText(values.joinToString(" – ") { format(it) }, style = Tessera.type.label, color = c.textSecondary)
        }
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .height(TesseraSize.touchMin)
                .alpha(if (enabled) 1f else TesseraOpacity.DISABLED)
                .pointerInput(enabled, range, step) {
                    if (!enabled) return@pointerInput
                    val usable = size.width - thumbPx
                    fun valueAt(x: Float) = range.start + ((x - thumbPx / 2) / usable).coerceIn(0f, 1f) * span
                    fun nearest(x: Float): Int {
                        val v = valueAt(x)
                        return if (latest.size == 1) 0 else if (kotlin.math.abs(latest[0] - v) <= kotlin.math.abs(latest[1] - v)) 0 else 1
                    }
                    detectDragGestures(
                        onDragStart = { o -> active = nearest(o.x); set(active, valueAt(o.x)) },
                        onDragEnd = { active = -1 },
                        onDragCancel = { active = -1 },
                    ) { change, _ -> change.consume(); if (active >= 0) set(active, valueAt(change.position.x)) }
                }
                .pointerInput(enabled, range, step) {
                    if (!enabled) return@pointerInput
                    val usable = size.width - thumbPx
                    detectTapGestures { o ->
                        val v = range.start + ((o.x - thumbPx / 2) / usable).coerceIn(0f, 1f) * span
                        val i = if (latest.size == 1) 0 else if (kotlin.math.abs(latest[0] - v) <= kotlin.math.abs(latest[1] - v)) 0 else 1
                        set(i, v)
                    }
                },
            contentAlignment = Alignment.CenterStart,
        ) {
            val width = maxWidth
            val usable = width - THUMB
            val fracs = values.map { ((it - range.start) / span).coerceIn(0f, 1f) }
            // 轨道：凹槽；填充：釉色
            Box(
                Modifier
                    .padding(horizontal = THUMB / 2)
                    .fillMaxWidth()
                    .height(TRACK)
                    .clip(RoundedCornerShape(4.dp))
                    .background(c.fillControlOff)
                    .sunken()
                    .drawBehind {
                        val r = CornerRadius(4.dp.toPx())
                        val start = if (fracs.size == 2) fracs[0] else 0f
                        val end = fracs.last()
                        drawRoundRect(c.accentControl, Offset(size.width * start, 0f), Size(size.width * (end - start), size.height), r)
                        if (step > 0f) {
                            val n = (span / step).roundToInt()
                            if (n in 2..20) for (k in 0..n) {
                                val x = size.width * k / n
                                val f = k.toFloat() / n
                                // 填充段上的刻度用半透明白色，其余用三级文字色
                                val onFill = f >= start && f <= end
                                drawCircle(if (onFill) Color.White.copy(alpha = 0.55f) else c.textTertiary.copy(alpha = 0.6f), 1.dp.toPx(), Offset(x.coerceIn(1.dp.toPx(), size.width - 1.dp.toPx()), size.height / 2))
                            }
                        }
                    },
            )
            values.forEachIndexed { i, v ->
                val frac = fracs[i]
                val scale by animateFloatAsState(if (active == i) 1.12f else 1f, TesseraMotion.snappy(), label = "thumb")
                val bubble by animateFloatAsState(if (active == i) 1f else 0f, TesseraMotion.snappy(), label = "bubble")
                val name = when { values.size == 1 -> label ?: "滑块"; i == 0 -> "${label ?: "范围"}起点"; else -> "${label ?: "范围"}终点" }
                val lo = if (values.size == 2 && i == 1) values[0] else range.start
                val hi = if (values.size == 2 && i == 0) values[1] else range.endInclusive
                Box(
                    Modifier
                        .offset { IntOffset((usable.toPx() * frac).roundToInt(), 0) }
                        .size(THUMB)
                        .semantics {
                            contentDescription = name
                            stateDescription = format(v)
                            progressBarRangeInfo = ProgressBarRangeInfo(v, range, if (step > 0f) ((span / step).roundToInt() - 1).coerceAtLeast(0) else 0)
                            if (enabled) setProgress { target -> set(i, target.coerceIn(lo, hi)); true }
                        },
                ) {
                    // 数值气泡：拖动时出现在滑块上方
                    if (bubble > 0.01f) Box(
                        Modifier
                            .align(Alignment.TopCenter)
                            .offset(y = (-34).dp)
                            .graphicsLayer { alpha = bubble; scaleX = 0.9f + 0.1f * bubble; scaleY = 0.9f + 0.1f * bubble; translationY = (1 - bubble) * 4 * density.density }
                            .clip(RoundedCornerShape(8.dp))
                            .background(c.bgInverse)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) { TesseraText(format(v), style = Tessera.type.caption, color = c.textInverse, maxLines = 1) }
                    Box(
                        Modifier
                            .fillMaxWidth().height(THUMB)
                            .graphicsLayer { scaleX = scale; scaleY = scale }
                            .shadow(3.dp, thumbShape, ambientColor = c.shadowColor, spotColor = c.shadowColor)
                            .clip(thumbShape)
                            .background(Color.White)
                            .border(1.dp, c.borderField, thumbShape),
                    )
                }
            }
        }
    }
}

// ------------------------------------------------------------------
// 评分 Rating：一排星星；点中的星星弹一下；只读时只显示
// ------------------------------------------------------------------

/**
 * 评分。readOnly = true 时只显示（读屏读作“4 星，共 5 星”）；
 * 否则每颗星是一个按钮，点同一颗星可清零（allowClear）。
 */
@Composable
fun TesseraRating(
    value: Int,
    onValueChange: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
    max: Int = 5,
    readOnly: Boolean = false,
    allowClear: Boolean = true,
    label: String = "评分",
) {
    val c = Tessera.colors
    val haptic = rememberFlowHaptic()
    val reduce = LocalReduceMotion.current
    var popped by remember { mutableIntStateOf(-1) }
    val scope = rememberCoroutineScope()
    val pops = remember(max) { List(max) { Animatable(1f) } }
    Row(
        modifier.then(if (readOnly) Modifier.semantics(mergeDescendants = true) { contentDescription = "$label：$value 星，共 $max 星" } else Modifier),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        for (i in 1..max) {
            val on = i <= value
            // 看得见的星 36dp（与 Web 的 32 接近），触摸范围由 Compose 自动扩展到 48dp
            val star = Modifier.size(if (readOnly) 28.dp else 36.dp)
            Box(
                if (readOnly) star else star
                    .clip(shapeOf(10.dp))
                    .clickable(role = Role.Button) {
                        val next = if (allowClear && value == i) 0 else i
                        onValueChange(next)
                        haptic(TesseraHaptic.Selection)
                        if (!reduce) scope.launch { pops[i - 1].snapTo(1.25f); pops[i - 1].animateTo(1f, TesseraMotion.bouncy()) }
                        popped = i
                    }
                    .semantics { contentDescription = "$i 星"; selected = i == value },
                contentAlignment = Alignment.Center,
            ) {
                TesseraIcon(
                    if (on) TesseraIcons.StarFill else TesseraIcons.Star, null, size = 24.dp,
                    tint = if (on) c.pigmentOchre else c.fillControlOff,
                    modifier = Modifier.graphicsLayer { scaleX = pops[i - 1].value; scaleY = pops[i - 1].value },
                )
            }
        }
    }
}

// ------------------------------------------------------------------
// 文件上传 FileUpload：虚线框的凹槽区域 + 文件列表（每个文件一块砖，带进度和删除）
// 选择文件由调用方负责（例如 ActivityResultContracts.GetMultipleContents），本组件只负责展示
// ------------------------------------------------------------------

data class TesseraUploadFile(val name: String, val size: Long, val progress: Float? = null, val error: String? = null)

/**
 * 文件上传。点击区域调用 onPick（由应用打开系统文件选择器）；files 为已选文件，progress 为 0–1（null 表示已完成）。
 */
@Composable
fun TesseraFileUpload(
    files: List<TesseraUploadFile>,
    onPick: () -> Unit,
    onRemove: (TesseraUploadFile) -> Unit,
    modifier: Modifier = Modifier,
    title: String = "点击选择文件",
    hint: String? = "支持 PDF、图片，单个不超过 20 MB",
) {
    val c = Tessera.colors
    val radius = shapeRadius(TesseraRadius.lg, 22.dp)
    val shape = RoundedCornerShape(radius)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(TesseraSpace.s3)) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(c.fillDefault)
                .sunken()
                .drawBehind {
                    val inset = 8.dp.toPx()
                    drawRoundRect(
                        c.borderStrong, Offset(inset, inset), Size(size.width - inset * 2, size.height - inset * 2),
                        CornerRadius((radius.toPx() - inset).coerceAtLeast(4.dp.toPx())),
                        style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))),
                    )
                }
                .clickable(role = Role.Button, onClick = onPick)
                .padding(vertical = 32.dp, horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(TesseraSpace.s2),
        ) {
            TesseraIcon(TesseraIcons.Upload, null, size = 32.dp, tint = c.accentText)
            TesseraText(title, style = Tessera.type.label)
            if (hint != null) TesseraText(hint, style = Tessera.type.caption, color = c.textSecondary)
        }
        if (files.isNotEmpty()) TesseraList {
            files.forEachIndexed { i, f ->
                UploadRow(f, com.tesseraui.foundation.tilePosition(i, files.size)) { onRemove(f) }
            }
        }
    }
}

@Composable
private fun UploadRow(f: TesseraUploadFile, position: com.tesseraui.foundation.TilePosition, onRemove: () -> Unit) {
    val c = Tessera.colors
    val sizeText = when {
        f.size >= 1 shl 20 -> "%.1f MB".format(f.size / 1048576.0)
        f.size >= 1024 -> "${f.size / 1024} KB"
        else -> "${f.size} B"
    }
    val status = when { f.error != null -> f.error; f.progress != null -> "上传中 ${(f.progress * 100).roundToInt()}%"; else -> sizeText }
    TesseraListItem(
        headline = f.name,
        position = position,
        supporting = status,
        leading = { TesseraIcon(if (f.error != null) TesseraIcons.Error else TesseraIcons.FileText, null, size = 22.dp, tint = if (f.error != null) c.dangerText else c.accentText) },
        trailing = {
            Box(
                Modifier.size(TesseraSize.touchMin).clip(shapeOf(10.dp)).clickable(role = Role.Button, onClick = onRemove)
                    .semantics { contentDescription = "移除 ${f.name}" },
                contentAlignment = Alignment.Center,
            ) { TesseraIcon(TesseraIcons.Close, null, size = 18.dp, tint = c.textSecondary) }
        },
        below = if (f.progress != null) ({ UploadProgress(f.progress) }) else null,
    )
}

@Composable
private fun UploadProgress(p: Float) {
    val c = Tessera.colors
    val anim by animateFloatAsState(p, tween(TesseraMotion.DURATION_NORMAL), label = "progress")
    Box(Modifier.fillMaxWidth().padding(top = 6.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(c.fillControlOff)) {
        Box(Modifier.fillMaxWidth(anim).height(4.dp).clip(RoundedCornerShape(2.dp)).background(c.accentControl))
    }
}
