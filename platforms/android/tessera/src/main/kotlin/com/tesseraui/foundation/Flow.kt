package com.tesseraui.foundation

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.PowerManager
import android.content.Context
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tesseraui.LocalReduceMotion
import com.tesseraui.TesseraFlow
import kotlinx.coroutines.launch
import kotlin.math.hypot
import kotlin.math.max

// 流韵外观的动效：水波、涟漪、水滴形变、触感。只在 TesseraShape.Flow 下生效，“移除动画”时只保留触感。

/**
 * 设备是否适合流韵的持续动效：不是低内存设备，处理器不少于 6 核，内存不少于 4GB，且没有打开省电模式。
 * 省电模式在使用中打开或关闭时会实时更新（外观随之在流韵与圆融之间切换）。
 */
@Composable
fun isFlowCapable(): Boolean {
    if (LocalInspectionMode.current) return true
    val context = LocalContext.current
    val hardware = remember {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val mem = ActivityManager.MemoryInfo().also { am?.getMemoryInfo(it) }
        val lowRam = am?.isLowRamDevice == true
        val cores = Runtime.getRuntime().availableProcessors()
        !lowRam && cores >= TesseraFlow.MIN_CORES && mem.totalMem >= TesseraFlow.MIN_MEMORY_GB * 0.9 * 1024 * 1024 * 1024
    }
    val power = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    var saving by remember { mutableStateOf(power?.isPowerSaveMode == true) }
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, i: Intent?) { saving = power?.isPowerSaveMode == true }
        }
        ContextCompatRegister.register(context, receiver, IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED))
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }
    return hardware && !saving
}

private object ContextCompatRegister {
    fun register(context: Context, receiver: BroadcastReceiver, filter: IntentFilter) {
        if (Build.VERSION.SDK_INT >= 33) context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        else @Suppress("UnspecifiedRegisterReceiverFlag") context.registerReceiver(receiver, filter)
    }
}

/** 四端统一的触感种类：选择（切换选项）、轻触、中等（开关打开）、确认（成功）、警告（危险操作、失败） */
enum class TesseraHaptic { Selection, Light, Medium, Success, Warning }

/** 流韵外观下的触感反馈；其他外观不触发。Android 11 以下没有“确认 / 警告”专用触感，用相近的代替 */
@Composable
fun rememberFlowHaptic(): (TesseraHaptic) -> Unit {
    val view: View = LocalView.current
    val flow = isFlow
    return remember(view, flow) {
        { kind ->
            if (flow) view.performHapticFeedback(
                when (kind) {
                    TesseraHaptic.Selection -> HapticFeedbackConstants.CLOCK_TICK
                    TesseraHaptic.Light -> HapticFeedbackConstants.KEYBOARD_TAP
                    TesseraHaptic.Medium -> HapticFeedbackConstants.VIRTUAL_KEY
                    TesseraHaptic.Success -> if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.VIRTUAL_KEY
                    TesseraHaptic.Warning -> if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.REJECT else HapticFeedbackConstants.LONG_PRESS
                },
            )
        }
    }
}

private class Wave(val center: Offset, val progress: Animatable<Float, *>)

private val WaveEasing = CubicBezierEasing(0.2f, 0.7f, 0.2f, 1f)

/**
 * 水波：按下处荡开两圈清透的涟漪（一圈亮边、中间透明），不是实心墨点。
 * 放在 clip(shape) 之后，涟漪会被控件的形状裁切。只在流韵外观下生效。
 */
fun Modifier.flowRipple(interactionSource: InteractionSource, color: Color, origin: Offset = Offset.Zero): Modifier = composed {
    if (LocalTesseraShape.current != TesseraShape.Flow || LocalReduceMotion.current) return@composed this
    val waves = remember { mutableStateListOf<Wave>() }
    val scope = rememberCoroutineScope()
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { i ->
            if (i is PressInteraction.Press) {
                for (ring in 0..1) {
                    val w = Wave(i.pressPosition - origin, Animatable(0f))
                    waves.add(w)
                    scope.launch {
                        if (ring == 1) kotlinx.coroutines.delay(90)
                        w.progress.animateTo(1f, tween(TesseraFlow.RIPPLE_DURATION, easing = WaveEasing))
                        waves.remove(w)
                    }
                }
            }
        }
    }
    drawWithContent {
        drawContent()
        for ((index, w) in waves.withIndex()) {
            val p = w.progress.value
            if (p <= 0f) continue
            val far = 2 * hypot(max(w.center.x, size.width - w.center.x), max(w.center.y, size.height - w.center.y))
            val r = far / 2 * p
            val alpha = (if (p < 0.6f) 1f else (1f - p) / 0.4f) * color.alpha
            val inner = if (index % 2 == 0) 0.42f else 0.5f
            drawCircle(
                Brush.radialGradient(
                    0f to Color.Transparent, inner to Color.Transparent,
                    (inner + 0.18f) to color.copy(alpha = alpha), 0.71f to Color.Transparent, 1f to Color.Transparent,
                    center = w.center, radius = r.coerceAtLeast(1f),
                ),
                radius = r, center = w.center,
            )
        }
    }
}

/**
 * 涟漪：从 center 处向外扩散两圈细环（开关拨动、复选框勾选时使用）。不裁切，可以溢出控件。
 * trigger 每变化一次扩散一次；只在流韵外观下生效。
 */
fun Modifier.flowSplash(trigger: Any, center: (androidx.compose.ui.geometry.Size) -> Offset, maxRadius: Dp, color: Color): Modifier = composed {
    if (LocalTesseraShape.current != TesseraShape.Flow || LocalReduceMotion.current) return@composed this
    val p = remember { Animatable(1f) }
    val first = remember { booleanArrayOf(true) }
    LaunchedEffect(trigger) {
        if (first[0]) { first[0] = false; return@LaunchedEffect }
        p.snapTo(0f)
        p.animateTo(1f, tween(TesseraFlow.SPLASH_DURATION, easing = CubicBezierEasing(0.15f, 0.8f, 0.25f, 1f)))
    }
    drawWithContent {
        val v = p.value
        if (v < 1f) {
            val c = center(size)
            val rMax = maxRadius.toPx()
            for (ring in 0..1) {
                val t = ((v - ring * 0.17f) / (1f - ring * 0.17f)).coerceIn(0f, 1f)
                if (t <= 0f) continue
                drawCircle(color.copy(alpha = color.alpha * 0.9f * (1f - t)), radius = rMax * (0.3f + 0.7f * t), center = c, style = Stroke(if (ring == 0) 1.5.dp.toPx() else 1.dp.toPx()))
            }
        }
        drawContent()
    }
}

/** 水滴形变：先被拉扁、再回弹、最后轻晃两下（开关滑块）。返回 (scaleX, scaleY) 动画值的读取函数 */
@Composable
fun rememberDropWobble(trigger: Any): () -> Pair<Float, Float> {
    val flow = isFlow && !LocalReduceMotion.current
    val x = remember { Animatable(1f) }
    val y = remember { Animatable(1f) }
    val first = remember { booleanArrayOf(true) }
    LaunchedEffect(trigger) {
        if (first[0] || !flow) { first[0] = false; return@LaunchedEffect }
        launch { x.animateTo(1f, keyframes { durationMillis = 620; 1.22f at 110; 0.9f at 250; 1.05f at 385; 0.98f at 510 }) }
        launch { y.animateTo(1f, keyframes { durationMillis = 620; 0.82f at 110; 1.1f at 250; 0.96f at 385; 1.02f at 510 }) }
    }
    return { x.value to y.value }
}

/** 水珠弹跳：0.82 → 1.12 → 0.97 → 1（复选框、单选、标签栏图标） */
@Composable
fun rememberBeadPop(trigger: Any): () -> Float {
    val flow = isFlow && !LocalReduceMotion.current
    val s = remember { Animatable(1f) }
    val first = remember { booleanArrayOf(true) }
    LaunchedEffect(trigger) {
        if (first[0] || !flow) { first[0] = false; return@LaunchedEffect }
        s.animateTo(1f, keyframes { durationMillis = 480; 0.82f at 120; 1.12f at 264; 0.97f at 384 })
    }
    return { s.value }
}

/** 液面张力：选中块移动时被拉扁，停下时回弹（分段控制）。返回 scaleY */
@Composable
fun rememberTension(trigger: Any): () -> Float {
    val flow = isFlow && !LocalReduceMotion.current
    val s = remember { Animatable(1f) }
    val first = remember { booleanArrayOf(true) }
    LaunchedEffect(trigger) {
        if (first[0] || !flow) { first[0] = false; return@LaunchedEffect }
        s.animateTo(1f, keyframes { durationMillis = 520; 0.84f at 156; 1.06f at 322; 0.98f at 426 })
    }
    return { s.value }
}
