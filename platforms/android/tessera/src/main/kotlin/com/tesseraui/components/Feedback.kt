package com.tesseraui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.dismiss
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.tesseraui.LocalContentColor
import com.tesseraui.LocalReduceMotion
import com.tesseraui.Tessera
import com.tesseraui.TesseraMotion
import com.tesseraui.TesseraRadius
import com.tesseraui.TesseraSpace
import com.tesseraui.TesseraText
import com.tesseraui.TesseraTile
import com.tesseraui.foundation.TilePosition
import com.tesseraui.foundation.glaze
import com.tesseraui.foundation.tileShape
import com.tesseraui.icons.TesseraIcon
import com.tesseraui.icons.TesseraIcons
import com.tesseraui.motion
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.abs
import kotlin.math.roundToInt

// ------------------------------------------------------------------
// 提示消息 Toast：一块反色砖，从顶部落下（bouncy）；停留 3 秒（带操作 5 秒）；可上滑关掉
// ------------------------------------------------------------------

enum class TesseraToastResult { Dismissed, ActionPerformed }

@Stable
class TesseraToastData internal constructor(
    val message: String,
    val icon: ImageVector?,
    val actionLabel: String?,
    internal val result: CompletableDeferred<TesseraToastResult>,
)

/** 提示消息队列：同一时间只显示一条，后面的排队 */
@Stable
class TesseraToastHostState {
    private val mutex = Mutex()
    var current by mutableStateOf<TesseraToastData?>(null)
        private set

    suspend fun show(message: String, icon: ImageVector? = null, actionLabel: String? = null): TesseraToastResult = mutex.withLock {
        val data = TesseraToastData(message, icon, actionLabel, CompletableDeferred())
        current = data
        try {
            withTimeoutOrNull(if (actionLabel != null) TesseraMotion.TOAST_DURATION_WITH_ACTION else TesseraMotion.TOAST_DURATION) { data.result.await() }
                ?: TesseraToastResult.Dismissed
        } finally {
            current = null
            delay(TesseraMotion.DURATION_FAST.toLong()) // 等离场动画结束再显示下一条
        }
    }
}

/** 放在页面最上层，例如 Box(Modifier.fillMaxSize()) { 内容; TesseraToastHost(state) } */
@Composable
fun TesseraToastHost(state: TesseraToastHostState, modifier: Modifier = Modifier) {
    val data = state.current
    var last by remember { mutableStateOf<TesseraToastData?>(null) }
    if (data != null) last = data
    val reduce = LocalReduceMotion.current
    Box(modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars).padding(top = TesseraSpace.s4), contentAlignment = Alignment.TopCenter) {
        AnimatedVisibility(
            visible = data != null,
            enter = if (reduce) fadeIn(tween(TesseraMotion.DURATION_FAST)) else
                slideInVertically(TesseraMotion.bouncy()) { -it } + scaleIn(TesseraMotion.bouncy(), initialScale = 0.9f) + fadeIn(tween(TesseraMotion.DURATION_NORMAL)),
            exit = fadeOut(tween(TesseraMotion.DURATION_FAST)) + slideOutVertically(tween(TesseraMotion.DURATION_FAST)) { -it / 3 },
        ) {
            last?.let { d -> TesseraToast(d.message, d.icon, d.actionLabel, onAction = { d.result.complete(TesseraToastResult.ActionPerformed) }, onDismiss = { d.result.complete(TesseraToastResult.Dismissed) }) }
        }
    }
}

/** 提示消息的砖块本身（也可以单独用来做静态展示） */
@Composable
fun TesseraToast(message: String, icon: ImageVector? = null, actionLabel: String? = null, onAction: () -> Unit = {}, onDismiss: () -> Unit = {}) {
    val c = Tessera.colors
    val density = LocalDensity.current
    val y = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val shape = RoundedCornerShape(TesseraRadius.lg)
    val drag = rememberDraggableState { d -> scope.launch { y.snapTo(if (y.value + d > 0) (y.value + d) * 0.3f else y.value + d) } }
    Row(
        Modifier
            .offset { IntOffset(0, y.value.roundToInt()) }
            .draggable(drag, Orientation.Vertical, onDragStopped = { v ->
                val fling = with(density) { TesseraMotion.FLING_VELOCITY_DP.dp.toPx() }
                if (y.value < -with(density) { 24.dp.toPx() } || v < -fling) onDismiss() else y.animateTo(0f, TesseraMotion.bouncy(), initialVelocity = v)
            })
            .widthIn(max = 480.dp)
            .defaultMinSize(minHeight = 48.dp)
            .shadow(12.dp, shape, ambientColor = c.shadowColor, spotColor = c.shadowColor)
            .clip(shape)
            .background(c.bgInverse)
            .glaze(c.edgeSolid, c.sheen)
            .semantics { liveRegion = LiveRegionMode.Polite; dismiss { onDismiss(); true } }
            .padding(start = TesseraSpace.s4, end = if (actionLabel != null) TesseraSpace.s2 else TesseraSpace.s5),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TesseraSpace.s3),
    ) {
        CompositionLocalProvider(LocalContentColor provides c.textInverse) {
            if (icon != null) TesseraIcon(icon, null)
            TesseraText(message, style = Tessera.type.bodySm, maxLines = 2)
            if (actionLabel != null) {
                Box(Modifier.heightIn(min = 44.dp).clickable(role = Role.Button, onClick = onAction).padding(horizontal = TesseraSpace.s3), contentAlignment = Alignment.Center) {
                    TesseraText(actionLabel, style = Tessera.type.label.copy(textDecoration = TextDecoration.Underline))
                }
            }
        }
    }
}

// ------------------------------------------------------------------
// 对话框 Dialog：面板从 0.96 缩放进场（smooth）；按钮是一组拼贴砖
// ------------------------------------------------------------------

@Composable
fun TesseraDialog(
    onDismissRequest: () -> Unit,
    title: String,
    confirmText: String,
    onConfirm: () -> Unit,
    dismissText: String? = "取消",
    text: String? = null,
    danger: Boolean = false,
) {
    Dialog(onDismissRequest = onDismissRequest, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val enter = remember { Animatable(0f) }
        val spec = motion(TesseraMotion.smooth<Float>())
        LaunchedEffect(Unit) { enter.animateTo(1f, spec) }
        Box(Modifier.graphicsLayer { alpha = enter.value.coerceIn(0f, 1f); val s = 0.96f + 0.04f * enter.value; scaleX = s; scaleY = s }) {
            TesseraDialogPanel(title, confirmText, onConfirm, dismissText, onDismissRequest, text, danger)
        }
    }
}

/** 对话框面板（不含窗口）：可用于自定义弹层或静态展示 */
@Composable
fun TesseraDialogPanel(
    title: String,
    confirmText: String,
    onConfirm: () -> Unit,
    dismissText: String? = "取消",
    onDismiss: () -> Unit = {},
    text: String? = null,
    danger: Boolean = false,
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val c = Tessera.colors
    val shape = RoundedCornerShape(TesseraRadius.s2xl)
    Column(
        Modifier
            .padding(16.dp)
            .widthIn(max = 440.dp)
            .fillMaxWidth()
            .shadow(24.dp, shape, ambientColor = c.shadowColor, spotColor = c.shadowColor)
            .clip(shape)
            .background(c.bgElevated)
            .border(1.dp, c.borderSurface, shape)
            .semantics { paneTitle = title }
            .padding(TesseraSpace.s6),
    ) {
        TesseraText(title, style = Tessera.type.title2)
        if (text != null) TesseraText(text, Modifier.padding(top = TesseraSpace.s2), style = Tessera.type.bodySm, color = c.textSecondary)
        content?.invoke(this)
        Row(Modifier.fillMaxWidth().padding(top = TesseraSpace.s6), horizontalArrangement = Arrangement.spacedBy(TesseraTile.grout)) {
            if (dismissText != null) TesseraButton(dismissText, onDismiss, Modifier.weight(1f), variant = TesseraButtonVariant.Secondary, size = TesseraButtonSize.Lg, shape = tileShape(TilePosition.First, TesseraRadius.lg))
            TesseraButton(confirmText, onConfirm, Modifier.weight(1f), variant = if (danger) TesseraButtonVariant.Danger else TesseraButtonVariant.Primary, size = TesseraButtonSize.Lg,
                shape = tileShape(if (dismissText != null) TilePosition.Last else TilePosition.Single, TesseraRadius.lg))
        }
    }
}

// ------------------------------------------------------------------
// 底部抽屉 BottomSheet：smooth 滑入；拖动 1:1 跟手；松手继承速度，
// 超过 500dp/s 朝甩动方向切到下一档，否则停到最近的档位；拖出上限有橡皮筋阻力
// ------------------------------------------------------------------

@Composable
fun TesseraBottomSheet(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    title: String? = null,
    detents: List<Float> = listOf(0.5f, 0.92f),
    content: @Composable ColumnScope.() -> Unit,
) {
    var showing by remember { mutableStateOf(visible) }
    if (visible) showing = true
    if (!showing) return
    Dialog(onDismissRequest = onDismissRequest, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val c = Tessera.colors
            val density = LocalDensity.current
            val full = constraints.maxHeight.toFloat()
            val stops = listOf(0f) + detents.map { it * full }
            // offset = 抽屉顶端到屏幕底部之外的距离；0 表示完全展开到最高档
            val visibleH = remember { Animatable(0f) }
            val smooth = motion(TesseraMotion.smooth<Float>())
            val scope = rememberCoroutineScope()
            LaunchedEffect(visible) {
                if (visible) visibleH.animateTo(stops[1], smooth)
                else { visibleH.animateTo(0f, smooth); showing = false }
            }
            val drag = rememberDraggableState { d ->
                scope.launch {
                    val max = stops.last()
                    var v = visibleH.value - d
                    if (v > max) v = max + (v - max) * 0.3f // 橡皮筋
                    visibleH.snapTo(v.coerceAtLeast(0f))
                }
            }
            // 遮罩
            Box(
                Modifier.fillMaxSize().graphicsLayer { alpha = (visibleH.value / stops[1]).coerceIn(0f, 1f) }.background(c.bgScrim)
                    .clickable(remember { MutableInteractionSource() }, null, onClickLabel = "关闭") { onDismissRequest() },
            )
            val shape = RoundedCornerShape(topStart = TesseraRadius.s2xl, topEnd = TesseraRadius.s2xl)
            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .offset { IntOffset(0, (stops.last() - visibleH.value).roundToInt()) }
                    .widthIn(max = 640.dp)
                    .fillMaxWidth()
                    .height(with(density) { stops.last().toDp() })
                    .shadow(24.dp, shape, ambientColor = c.shadowColor, spotColor = c.shadowColor)
                    .clip(shape)
                    .background(c.bgElevated)
                    .semantics { paneTitle = title ?: "底部抽屉" }
                    .windowInsetsPadding(WindowInsets.navigationBars),
            ) {
                // 拖动把手
                Box(
                    Modifier.fillMaxWidth().height(28.dp)
                        .draggable(drag, Orientation.Vertical, onDragStopped = { velocity ->
                            val v = -velocity // 向上为正
                            val fling = with(density) { TesseraMotion.FLING_VELOCITY_DP.dp.toPx() }
                            val cur = visibleH.value
                            val target = if (abs(v) > fling) {
                                if (v > 0) stops.firstOrNull { it > cur + 1 } ?: stops.last() else stops.lastOrNull { it < cur - 1 } ?: 0f
                            } else stops.minBy { abs(it - cur) }
                            visibleH.animateTo(target, smooth, initialVelocity = v)
                            if (target == 0f) onDismissRequest()
                        })
                        .semantics { contentDescription = "拖动调整高度" },
                    contentAlignment = Alignment.Center,
                ) { Box(Modifier.size(36.dp, 5.dp).clip(RoundedCornerShape(3.dp)).background(c.borderStrong)) }
                Column(Modifier.padding(horizontal = TesseraSpace.s5)) {
                    if (title != null) TesseraText(title, Modifier.padding(bottom = TesseraSpace.s4), style = Tessera.type.title2)
                    CompositionLocalProvider(LocalContentColor provides c.textPrimary) { content() }
                }
            }
        }
    }
}

/** 静态展示用的抽屉面板（截图、预览） */
@Composable
fun TesseraSheetPanel(title: String?, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val c = Tessera.colors
    val shape = RoundedCornerShape(topStart = TesseraRadius.s2xl, topEnd = TesseraRadius.s2xl)
    Column(modifier.fillMaxWidth().shadow(24.dp, shape, ambientColor = c.shadowColor, spotColor = c.shadowColor).clip(shape).background(c.bgElevated).padding(horizontal = TesseraSpace.s5)) {
        Box(Modifier.fillMaxWidth().height(28.dp), contentAlignment = Alignment.Center) { Box(Modifier.size(36.dp, 5.dp).clip(RoundedCornerShape(3.dp)).background(c.borderStrong)) }
        if (title != null) TesseraText(title, Modifier.padding(bottom = TesseraSpace.s4), style = Tessera.type.title2)
        content()
        Box(Modifier.height(TesseraSpace.s6))
    }
}

internal val UnusedIcon = TesseraIcons.Close
