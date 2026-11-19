package com.tesseraui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.tesseraui.LocalContentColor
import com.tesseraui.LocalReduceMotion
import com.tesseraui.LocalTesseraFonts
import com.tesseraui.Tessera
import com.tesseraui.TesseraMotion
import com.tesseraui.TesseraRadius
import com.tesseraui.TesseraRound
import com.tesseraui.TesseraSpace
import com.tesseraui.TesseraText
import com.tesseraui.foundation.glaze
import com.tesseraui.foundation.isRound
import com.tesseraui.foundation.roundRadius
import com.tesseraui.foundation.shapeOf
import com.tesseraui.foundation.showOutline
import com.tesseraui.foundation.sunken
import com.tesseraui.icons.TesseraIcon
import com.tesseraui.icons.TesseraIcons
import com.tesseraui.motion
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.roundToInt

/** 预览与截图（只渲染一帧）时不播放入场动效，直接显示落定后的样子 */
@Composable
internal fun isStaticPreview(): Boolean = LocalInspectionMode.current || LocalView.current.isInEditMode

// ------------------------------------------------------------------
// 进度条 Progress：凹槽里一条釉色砖；不确定进度时一块砖在槽里来回跳
// ------------------------------------------------------------------

/**
 * 线形进度。
 * @param value 0～1；传 null 表示不确定进度
 * @param label 进度条上方左侧的说明，右侧自动显示百分比（传 valueText 可自定义）
 */
@Composable
fun TesseraProgress(
    value: Float?,
    modifier: Modifier = Modifier,
    label: String? = null,
    valueText: String? = null,
    tone: TesseraTone = TesseraTone.Accent,
) {
    val c = Tessera.colors
    val fill = when (tone) { TesseraTone.Success -> c.successSolid; TesseraTone.Danger -> c.dangerSolid; TesseraTone.Warning -> c.warningSolid; else -> c.accentControl }
    val shape = if (isRound) RoundedCornerShape(50) else RoundedCornerShape(4.dp)
    val shown by animateFloatAsState((value ?: 0f).coerceIn(0f, 1f), motion(TesseraMotion.smooth()), label = "progress")
    val reduce = LocalReduceMotion.current
    val hop = if (value == null && !reduce) {
        val t = rememberInfiniteTransition(label = "hop")
        t.animateFloat(0f, 1f, infiniteRepeatable(tween(1300, easing = CubicBezierEasing(0.65f, 0f, 0.35f, 1f)), RepeatMode.Reverse), label = "x").value
    } else 0f
    val percent = "${((value ?: 0f) * 100).roundToInt()}%"
    Column(
        modifier.semantics(mergeDescendants = true) {
            if (label != null) contentDescription = label
            progressBarRangeInfo = if (value == null) ProgressBarRangeInfo.Indeterminate else ProgressBarRangeInfo(value.coerceIn(0f, 1f), 0f..1f)
            if (value != null) stateDescription = valueText ?: percent
        },
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (label != null || valueText != null) Row(Modifier.fillMaxWidth()) {
            TesseraText(label ?: "", Modifier.weight(1f), style = Tessera.type.caption, color = c.textSecondary)
            if (value != null) TesseraText(valueText ?: percent, style = Tessera.type.caption, color = c.textSecondary)
        }
        BoxWithConstraints(Modifier.fillMaxWidth().height(8.dp).clip(shape).background(c.fillControlOff).sunken()) {
            if (value == null) {
                val w = maxWidth * 0.28f
                Box(Modifier.offset(x = (maxWidth - w) * hop).width(w).height(8.dp).clip(shape).background(fill).glaze(Color.Transparent, c.sheen))
            } else {
                Box(Modifier.width(maxWidth * shown).height(8.dp).clip(shape).background(fill).glaze(Color.Transparent, c.sheen))
            }
        }
    }
}

/** 环形进度：大号（96dp）中间用标题字体显示百分比 */
@Composable
fun TesseraProgressRing(
    value: Float,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    thickness: Dp = if (size >= 80.dp) 8.dp else 5.dp,
    label: String = "进度",
    showValue: Boolean = true,
    tone: TesseraTone = TesseraTone.Accent,
) {
    val c = Tessera.colors
    val fill = when (tone) { TesseraTone.Success -> c.successSolid; TesseraTone.Danger -> c.dangerSolid; TesseraTone.Warning -> c.warningSolid; else -> c.accentControl }
    val v = value.coerceIn(0f, 1f)
    val shown by animateFloatAsState(v, motion(TesseraMotion.smooth()), label = "ring")
    val track = c.fillControlOff
    val percent = "${(v * 100).roundToInt()}%"
    Box(
        modifier.size(size).semantics(mergeDescendants = true) {
            contentDescription = label
            progressBarRangeInfo = ProgressBarRangeInfo(v, 0f..1f)
            stateDescription = percent
        },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(size)) {
            val t = thickness.toPx()
            val inset = t / 2
            val arc = Size(this.size.width - t, this.size.height - t)
            drawArc(track, 0f, 360f, false, Offset(inset, inset), arc, style = Stroke(t))
            if (shown > 0f) drawArc(fill, -90f, 360f * shown, false, Offset(inset, inset), arc, style = Stroke(t, cap = StrokeCap.Round))
        }
        if (showValue) {
            val big = size >= 80.dp
            TesseraText(
                percent,
                style = if (big) Tessera.type.title2.copy(fontSize = 22.sp, lineHeight = 22.sp) else Tessera.type.caption.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
                color = c.textPrimary,
            )
        }
    }
}

// ------------------------------------------------------------------
// 横幅 Banner：整块浅色底，图标 + 标题 + 正文 + 操作；可关闭
// ------------------------------------------------------------------

@Composable
fun TesseraBanner(
    text: String,
    modifier: Modifier = Modifier,
    title: String? = null,
    tone: TesseraTone = TesseraTone.Accent,
    icon: ImageVector? = null,
    onDismiss: (() -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    val c = Tessera.colors
    val (bg, fg) = when (tone) {
        TesseraTone.Success -> c.successSubtle to c.successText
        TesseraTone.Warning -> c.warningSubtle to c.warningText
        TesseraTone.Danger -> c.dangerSubtle to c.dangerText
        TesseraTone.Neutral -> c.fillDefault to c.textSecondary
        else -> c.accentSubtle to c.accentText
    }
    val ic = icon ?: when (tone) {
        TesseraTone.Success -> TesseraIcons.Success
        TesseraTone.Warning -> TesseraIcons.Warning
        TesseraTone.Danger -> TesseraIcons.Error
        else -> TesseraIcons.Info
    }
    val shape = shapeOf(TesseraRadius.lg, TesseraRound.row)
    Row(
        modifier.fillMaxWidth().clip(shape).background(bg)
            .semantics { if (tone == TesseraTone.Danger || tone == TesseraTone.Warning) liveRegion = LiveRegionMode.Polite }
            .padding(start = if (isRound) 18.dp else 16.dp, end = if (onDismiss != null) 4.dp else 16.dp, top = 14.dp, bottom = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(TesseraSpace.s3),
    ) {
        TesseraIcon(ic, null, Modifier.padding(top = 1.dp), tint = fg, size = 20.dp)
        Column(Modifier.weight(1f).semantics(mergeDescendants = true) {}) {
            if (title != null) TesseraText(title, Modifier.padding(bottom = 2.dp), style = Tessera.type.label, color = c.textPrimary)
            TesseraText(text, style = Tessera.type.bodySm, color = c.textPrimary)
            if (actions != null) Row(Modifier.padding(top = TesseraSpace.s3), horizontalArrangement = Arrangement.spacedBy(TesseraSpace.s2), content = actions)
        }
        // 关闭按钮触控范围 44，但只占 20 的版面高度（上下各伸出 12），单行横幅不会被撑高
        if (onDismiss != null) TesseraIconButton(
            TesseraIcons.Close, "关闭提示", onDismiss,
            Modifier.layout { m, cons ->
                val p = m.measure(cons.copy(minHeight = 0))
                val trim = 12.dp.roundToPx()
                layout(p.width, (p.height - trim * 2).coerceAtLeast(0)) { p.place(0, -trim) }
            },
            size = TesseraButtonSize.Sm,
        )
    }
}

// ------------------------------------------------------------------
// 结果页 Result：一块大釉砖图标落下时轻轻弹一下
// ------------------------------------------------------------------

enum class TesseraResultStatus { Success, Error, Info, NotFound }

@Composable
fun TesseraResult(
    status: TesseraResultStatus,
    title: String,
    modifier: Modifier = Modifier,
    text: String? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    val c = Tessera.colors
    val bg = when (status) { TesseraResultStatus.Success -> c.successSolid; TesseraResultStatus.Error -> c.dangerSolid; TesseraResultStatus.NotFound -> c.textPrimary; else -> c.accentSolid }
    val fg = if (status == TesseraResultStatus.NotFound) c.bgSurface else Color.White
    val shape = if (isRound) CircleShape else RoundedCornerShape(26.dp)
    // 预览与截图（只渲染一帧）时直接显示落定后的样子
    val reduce = LocalReduceMotion.current || isStaticPreview()
    val settle = remember { Animatable(if (reduce) 1f else 0f) }
    val spec = motion(TesseraMotion.bouncy<Float>())
    LaunchedEffect(Unit) { settle.animateTo(1f, spec) }
    Column(
        modifier.fillMaxWidth().padding(horizontal = TesseraSpace.s6, vertical = TesseraSpace.s12),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(TesseraSpace.s3),
    ) {
        Box(
            Modifier.padding(bottom = TesseraSpace.s2)
                .graphicsLayer { val s = 0.6f + 0.4f * settle.value; scaleX = s; scaleY = s; alpha = settle.value.coerceIn(0f, 1f); translationY = (1 - settle.value) * -24.dp.toPx() }
                .shadow(10.dp, shape, ambientColor = c.shadowColor, spotColor = c.shadowColor)
                .size(72.dp).clip(shape).background(bg).glaze(if (isRound) Color.Transparent else c.edgeSolid, c.sheen),
            contentAlignment = Alignment.Center,
        ) {
            when (status) {
                TesseraResultStatus.NotFound -> TesseraText("404", style = Tessera.type.title2.copy(fontSize = 22.sp), color = fg)
                else -> TesseraIcon(
                    when (status) { TesseraResultStatus.Success -> TesseraIcons.Check; TesseraResultStatus.Error -> TesseraIcons.Close; else -> TesseraIcons.Info },
                    null, tint = fg, size = 36.dp,
                )
            }
        }
        TesseraText(title, Modifier.semantics { heading() }, style = Tessera.type.title1, color = c.textPrimary, textAlign = TextAlign.Center)
        if (text != null) TesseraText(text, Modifier.widthIn(max = 420.dp).padding(bottom = TesseraSpace.s4), style = Tessera.type.body, color = c.textSecondary, textAlign = TextAlign.Center)
        if (actions != null) Row(horizontalArrangement = Arrangement.spacedBy(TesseraSpace.s2), content = actions)
    }
}

// ------------------------------------------------------------------
// 空状态 EmptyState：一小片 3×3 马赛克，中间一块釉砖翘起来
// ------------------------------------------------------------------

@Composable
fun TesseraEmptyState(
    title: String,
    modifier: Modifier = Modifier,
    text: String? = null,
    action: (@Composable () -> Unit)? = null,
) {
    val c = Tessera.colors
    val round = isRound
    Column(
        modifier.fillMaxWidth().padding(horizontal = TesseraSpace.s6, vertical = TesseraSpace.s12),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(TesseraSpace.s2),
    ) {
        val gap = if (round) 6.dp else 3.dp
        Column(Modifier.padding(bottom = TesseraSpace.s3), verticalArrangement = Arrangement.spacedBy(gap)) {
            for (r in 0..2) Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                for (col in 0..2) {
                    val i = r * 3 + col
                    val big = 12.dp; val small = 6.dp
                    val shape = if (round) CircleShape else RoundedCornerShape(
                        topStart = if (i == 0) big else small, topEnd = if (i == 2) big else small,
                        bottomStart = if (i == 6) big else small, bottomEnd = if (i == 8) big else small,
                    )
                    if (i == 4) Box(
                        Modifier.offset(y = (-6).dp).then(if (round) Modifier else Modifier.rotate(-8f))
                            .size(22.dp).clip(shape).background(c.accentSolid).glaze(if (round) Color.Transparent else c.edgeSolid, c.sheen),
                    ) else Box(Modifier.size(22.dp).clip(shape).background(c.fillDefault).sunken())
                }
            }
        }
        TesseraText(title, Modifier.semantics { heading() }, style = Tessera.type.title3, color = c.textPrimary, textAlign = TextAlign.Center)
        if (text != null) TesseraText(text, Modifier.widthIn(max = 360.dp).padding(bottom = TesseraSpace.s3), style = Tessera.type.bodySm, color = c.textSecondary, textAlign = TextAlign.Center)
        action?.invoke()
    }
}

// ------------------------------------------------------------------
// 按键 Kbd：等宽小字，细描边的小键帽
// ------------------------------------------------------------------

/** 快捷键键帽；组合键传多个：TesseraKbd("Ctrl", "K") */
@Composable
fun TesseraKbd(vararg keys: String, modifier: Modifier = Modifier) {
    val c = Tessera.colors
    val mono = LocalTesseraFonts.current.mono
    val shape = RoundedCornerShape(if (isRound) 8.dp else 6.dp)
    val spoken = keys.joinToString(" 加 ") { spokenKey(it) }
    Row(modifier.semantics(mergeDescendants = true) { contentDescription = "快捷键 $spoken" }, horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        keys.forEach { k ->
            Box(
                Modifier.clip(shape).background(c.bgSurface).border(1.dp, c.borderSubtle, shape).glaze(c.edgeSoft).padding(start = 6.dp, end = 6.dp, top = 3.dp, bottom = 4.dp),
            ) { TesseraText(k, style = Tessera.type.caption.copy(fontFamily = mono, fontSize = 11.sp, lineHeight = 13.sp, fontWeight = FontWeight.Medium), color = c.textSecondary) }
        }
    }
}

private fun spokenKey(k: String): String = when (k) {
    "⌘" -> "Command"; "⌥" -> "Option"; "⇧" -> "Shift"; "⌃" -> "Control"; "↵", "⏎" -> "回车"; "⌫" -> "退格"; "Esc" -> "Esc"
    "↑" -> "上"; "↓" -> "下"; "←" -> "左"; "→" -> "右"
    else -> k
}

// ------------------------------------------------------------------
// 工具提示 Tooltip：长按（触屏）或悬停 500ms（鼠标）出现；一块小反色砖
// ------------------------------------------------------------------

/**
 * 给任意内容加工具提示。触屏上长按出现，松手 1.5 秒后消失；鼠标悬停 500ms 出现。
 * 读屏：提示文字作为长按操作的说明读出，不覆盖内容自己的名称。
 */
@Composable
fun TesseraTooltip(text: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    var hovering by remember { mutableStateOf(false) }
    LaunchedEffect(hovering) {
        if (hovering) { delay(500); visible = true } else visible = false
    }
    Box(
        modifier
            .semantics { onLongClick(label = text) { visible = true; true } }
            .pointerInput(text) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    if (down.type == androidx.compose.ui.input.pointer.PointerType.Mouse) return@awaitEachGesture
                    val up = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) { waitForUpOrCancellation(PointerEventPass.Initial) }
                    if (up == null) {
                        visible = true
                        waitForUpOrCancellation(PointerEventPass.Initial)
                    }
                }
            }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val e = awaitPointerEvent(PointerEventPass.Initial)
                        when (e.type) { PointerEventType.Enter -> hovering = true; PointerEventType.Exit -> hovering = false; else -> Unit }
                    }
                }
            },
    ) {
        content()
        if (visible) {
            LaunchedEffect(Unit) { if (!hovering) { delay(1500); visible = false } }
            val density = LocalDensity.current
            Popup(popupPositionProvider = remember(density) { AbovePosition(with(density) { 6.dp.roundToPx() }) }, onDismissRequest = { visible = false }, properties = PopupProperties(focusable = false)) {
                TesseraTooltipBubble(text)
            }
        }
    }
}

/** 提示气泡本身（静态展示、截图用） */
@Composable
fun TesseraTooltipBubble(text: String, modifier: Modifier = Modifier) {
    val c = Tessera.colors
    val shape = RoundedCornerShape(if (isRound) 14.dp else TesseraRadius.sm)
    val reduce = LocalReduceMotion.current || isStaticPreview()
    val enter = remember { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(Unit) { enter.animateTo(1f, tween(TesseraMotion.DURATION_FAST, easing = TesseraMotion.EaseStandard)) }
    Box(
        modifier.graphicsLayer { alpha = enter.value; val s = 0.94f + 0.06f * enter.value; scaleX = s; scaleY = s; transformOrigin = TransformOrigin(0.5f, 1f) }
            .widthIn(max = 260.dp).shadow(8.dp, shape, ambientColor = c.shadowColor, spotColor = c.shadowColor)
            .clip(shape).background(c.bgInverse).padding(horizontal = if (isRound) 12.dp else 10.dp, vertical = 6.dp),
    ) { TesseraText(text, style = Tessera.type.caption.copy(fontSize = 12.5.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium), color = c.textInverse) }
}

/** 放在锚点上方、水平居中；上方放不下时放到下方 */
private class AbovePosition(private val gap: Int) : PopupPositionProvider {
    override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize): IntOffset {
        val x = (anchorBounds.center.x - popupContentSize.width / 2).coerceIn(8, (windowSize.width - popupContentSize.width - 8).coerceAtLeast(8))
        val above = anchorBounds.top - gap - popupContentSize.height
        val y = if (above >= 0) above else anchorBounds.bottom + gap
        return IntOffset(x, y)
    }
}

// ------------------------------------------------------------------
// 气泡卡片 Popover：点按触发，内容可交互；点外部或返回键关闭
// ------------------------------------------------------------------

/**
 * 气泡卡片。和菜单一样放在锚点所在的 Box 里：
 * Box { TesseraButton("详情", { open = true }); TesseraPopover(open, { open = false }, title = "…") { … } }
 */
@Composable
fun TesseraPopover(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val state = remember { MutableTransitionState(false) }
    state.targetState = expanded
    if (!state.currentState && !state.targetState) return
    val density = LocalDensity.current
    Popup(popupPositionProvider = remember(density) { BelowPosition(with(density) { 10.dp.roundToPx() }) }, onDismissRequest = onDismissRequest, properties = PopupProperties(focusable = true)) {
        val t = rememberTransition(state, label = "popover")
        val enterSpec = motion(TesseraMotion.smooth<Float>())
        val p by t.animateFloat(transitionSpec = { if (targetState) enterSpec else tween(TesseraMotion.DURATION_FAST) }, label = "p") { if (it) 1f else 0f }
        TesseraPopoverPanel(modifier.graphicsLayer { alpha = p.coerceIn(0f, 1f); val s = 0.96f + 0.04f * p; scaleX = s; scaleY = s; transformOrigin = TransformOrigin(0.5f, 0f) }, title, content)
    }
}

/** 气泡卡片面板（不含弹出层）：静态展示、截图用 */
@Composable
fun TesseraPopoverPanel(modifier: Modifier = Modifier, title: String? = null, content: @Composable ColumnScope.() -> Unit) {
    val c = Tessera.colors
    val shape = shapeOf(TesseraRadius.lg, TesseraRound.menu)
    Column(
        modifier.widthIn(max = 340.dp)
            .shadow(12.dp, shape, ambientColor = c.shadowColor, spotColor = c.shadowColor)
            .clip(shape).background(c.bgElevated)
            .then(if (showOutline) Modifier.border(1.dp, c.borderSubtle, shape) else Modifier)
            .semantics { if (title != null) paneTitle = title }
            .padding(TesseraSpace.s5),
    ) {
        CompositionLocalProvider(LocalContentColor provides c.textPrimary) {
            if (title != null) TesseraText(title, Modifier.padding(bottom = 6.dp).semantics { heading() }, style = Tessera.type.title3, color = c.textPrimary)
            content()
        }
    }
}

private class BelowPosition(private val gap: Int) : PopupPositionProvider {
    override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize): IntOffset {
        val x = (anchorBounds.center.x - popupContentSize.width / 2).coerceIn(8, (windowSize.width - popupContentSize.width - 8).coerceAtLeast(8))
        val below = anchorBounds.bottom + gap
        val y = if (below + popupContentSize.height <= windowSize.height) below else (anchorBounds.top - gap - popupContentSize.height).coerceAtLeast(0)
        return IntOffset(x, y)
    }
}
