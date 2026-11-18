// TesseraUI v0.3.0 — 由 scripts/build.mjs 从 tokens/*.json 生成，请勿手改。
package com.tesseraui

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

@Immutable
data class TesseraColors(
    /** 页面底，也是“砖缝”的颜色 */ val bgCanvas: Color,
    /** 卡片、砖面 */ val bgSurface: Color,
    /** 浮层 */ val bgElevated: Color,
    /** 浮层上的浮层 */ val bgElevated2: Color,
    /** 反色砖：提示消息、工具提示 */ val bgInverse: Color,
    val bgScrim: Color,
    /** 输入框、次要控件的底 */ val fillDefault: Color,
    /** 开关关闭时的轨道 */ val fillControlOff: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val textOnSolid: Color,
    val textInverse: Color,
    val borderSubtle: Color,
    val borderStrong: Color,
    /** 卡片、浮层外描边：浅色没有，深色很淡，增强对比度时为实线 */ val borderSurface: Color,
    /** 输入框描边：只在增强对比度时出现 */ val borderField: Color,
    /** 实色砖底部的砖厚 */ val edgeSolid: Color,
    /** 浅色砖底部的砖厚 */ val edgeSoft: Color,
    /** 釉面顶部的一道高光 */ val sheen: Color,
    /** 主按钮底（孔雀釉） */ val accentSolid: Color,
    /** 非文字实色控件：开关开启、复选框和单选选中 */ val accentControl: Color,
    val accentText: Color,
    val accentSubtle: Color,
    val dangerSolid: Color,
    val dangerText: Color,
    val dangerSubtle: Color,
    val successSolid: Color,
    val successText: Color,
    val successSubtle: Color,
    val warningSolid: Color,
    val warningText: Color,
    val warningSubtle: Color,
    val focusGlow: Color,
    val focusRing: Color,
    val selection: Color,
    val stateHover: Color,
    val statePressed: Color,
    val stateSolidHover: Color,
    val stateSolidPressed: Color,
    /** 阴影带石色暖调，不用纯黑 */ val shadowColor: Color,
    /** 矿物颜料：只用于头像、分类、插图，不表示状态 */ val pigmentCinnabar: Color,
    val pigmentCinnabarSubtle: Color,
    val pigmentCinnabarText: Color,
    /** 矿物颜料：只用于头像、分类、插图，不表示状态 */ val pigmentOchre: Color,
    val pigmentOchreSubtle: Color,
    val pigmentOchreText: Color,
    /** 矿物颜料：只用于头像、分类、插图，不表示状态 */ val pigmentLapis: Color,
    val pigmentLapisSubtle: Color,
    val pigmentLapisText: Color,
    /** 矿物颜料：只用于头像、分类、插图，不表示状态 */ val pigmentAmethyst: Color,
    val pigmentAmethystSubtle: Color,
    val pigmentAmethystText: Color,
    /** 矿物颜料：只用于头像、分类、插图，不表示状态 */ val pigmentOlive: Color,
    val pigmentOliveSubtle: Color,
    val pigmentOliveText: Color,
    /** 可选毛玻璃的底色（导航栏、标签栏、菜单、提示消息） */ val materialGlass: Color,
    /** 可选毛玻璃滑块（开关、分段控制） */ val materialGlassThumb: Color,
    /** 毛玻璃边缘的一圈高光 */ val materialGlassRim: Color,
)

val TesseraLightColors = TesseraColors(
    bgCanvas = Color(0xFFEFEBE4),
    bgSurface = Color(0xFFFBF9F5),
    bgElevated = Color(0xFFFFFFFF),
    bgElevated2 = Color(0xFFFFFFFF),
    bgInverse = Color(0xFF1F1B16),
    bgScrim = Color(0x5C1F1B16),
    fillDefault = Color(0x0F1F1B16),
    fillControlOff = Color(0xFFD9D3C8),
    textPrimary = Color(0xFF1F1B16),
    textSecondary = Color(0xFF5E584E),
    textTertiary = Color(0xFF77716A),
    textOnSolid = Color(0xFFFFFFFF),
    textInverse = Color(0xFFF0ECE4),
    borderSubtle = Color(0x141F1B16),
    borderStrong = Color(0x731F1B16),
    borderSurface = Color(0x00000000),
    borderField = Color(0x00000000),
    edgeSolid = Color(0x38000000),
    edgeSoft = Color(0x1A1F1B16),
    sheen = Color(0x29FFFFFF),
    accentSolid = Color(0xFF0F6B63),
    accentControl = Color(0xFF0F6B63),
    accentText = Color(0xFF0F6B63),
    accentSubtle = Color(0x1A0F6B63),
    dangerSolid = Color(0xFFB42318),
    dangerText = Color(0xFFB42318),
    dangerSubtle = Color(0x1AB42318),
    successSolid = Color(0xFF3D6B1C),
    successText = Color(0xFF3D6B1C),
    successSubtle = Color(0x1A3D6B1C),
    warningSolid = Color(0xFF8A5600),
    warningText = Color(0xFF8A5600),
    warningSubtle = Color(0x1A8A5600),
    focusGlow = Color(0x380F6B63),
    focusRing = Color(0xFF0F6B63),
    selection = Color(0x381F8A7E),
    stateHover = Color(0x0D1F1B16),
    statePressed = Color(0x171F1B16),
    stateSolidHover = Color(0x14FFFFFF),
    stateSolidPressed = Color(0x1A000000),
    shadowColor = Color(0xFF1F1B16),
    pigmentCinnabar = Color(0xFFC4472D),
    pigmentCinnabarSubtle = Color(0x24C4472D),
    pigmentCinnabarText = Color(0xFF93301C),
    pigmentOchre = Color(0xFFB7862B),
    pigmentOchreSubtle = Color(0x24B7862B),
    pigmentOchreText = Color(0xFF7A5512),
    pigmentLapis = Color(0xFF2F4E9E),
    pigmentLapisSubtle = Color(0x242F4E9E),
    pigmentLapisText = Color(0xFF243E80),
    pigmentAmethyst = Color(0xFF7B4C9C),
    pigmentAmethystSubtle = Color(0x247B4C9C),
    pigmentAmethystText = Color(0xFF5E3778),
    pigmentOlive = Color(0xFF6A7A2B),
    pigmentOliveSubtle = Color(0x246A7A2B),
    pigmentOliveText = Color(0xFF4D5A1A),
    materialGlass = Color(0xB8FBF9F5),
    materialGlassThumb = Color(0x9EFFFFFF),
    materialGlassRim = Color(0xBFFFFFFF),
)

val TesseraDarkColors = TesseraColors(
    bgCanvas = Color(0xFF151310),
    bgSurface = Color(0xFF1E1C18),
    bgElevated = Color(0xFF282521),
    bgElevated2 = Color(0xFF322F2A),
    bgInverse = Color(0xFFF0ECE4),
    bgScrim = Color(0x99000000),
    fillDefault = Color(0x14F0ECE4),
    fillControlOff = Color(0xFF4A453D),
    textPrimary = Color(0xFFF0ECE4),
    textSecondary = Color(0xFFADA699),
    textTertiary = Color(0xFF7D776B),
    textOnSolid = Color(0xFFFFFFFF),
    textInverse = Color(0xFF1F1B16),
    borderSubtle = Color(0x14F0ECE4),
    borderStrong = Color(0x73F0ECE4),
    borderSurface = Color(0x0FF0ECE4),
    borderField = Color(0x00000000),
    edgeSolid = Color(0x52000000),
    edgeSoft = Color(0x66000000),
    sheen = Color(0x1AFFFFFF),
    accentSolid = Color(0xFF0F6B63),
    accentControl = Color(0xFF1F8A7E),
    accentText = Color(0xFF6CBDB1),
    accentSubtle = Color(0x331F8A7E),
    dangerSolid = Color(0xFFB42318),
    dangerText = Color(0xFFFF8A7A),
    dangerSubtle = Color(0x38B42318),
    successSolid = Color(0xFF3D6B1C),
    successText = Color(0xFF9CCB6B),
    successSubtle = Color(0x383D6B1C),
    warningSolid = Color(0xFF8A5600),
    warningText = Color(0xFFE0A643),
    warningSubtle = Color(0x388A5600),
    focusGlow = Color(0x611F8A7E),
    focusRing = Color(0xFF6CBDB1),
    selection = Color(0x521F8A7E),
    stateHover = Color(0x0FF0ECE4),
    statePressed = Color(0x1AF0ECE4),
    stateSolidHover = Color(0x14FFFFFF),
    stateSolidPressed = Color(0x1A000000),
    shadowColor = Color(0xFF000000),
    pigmentCinnabar = Color(0xFFC4472D),
    pigmentCinnabarSubtle = Color(0x42C4472D),
    pigmentCinnabarText = Color(0xFFF0A08C),
    pigmentOchre = Color(0xFFB7862B),
    pigmentOchreSubtle = Color(0x42B7862B),
    pigmentOchreText = Color(0xFFE3B866),
    pigmentLapis = Color(0xFF2F4E9E),
    pigmentLapisSubtle = Color(0x422F4E9E),
    pigmentLapisText = Color(0xFF9DB2EC),
    pigmentAmethyst = Color(0xFF7B4C9C),
    pigmentAmethystSubtle = Color(0x427B4C9C),
    pigmentAmethystText = Color(0xFFC9A6E2),
    pigmentOlive = Color(0xFF6A7A2B),
    pigmentOliveSubtle = Color(0x426A7A2B),
    pigmentOliveText = Color(0xFFBFCB7A),
    materialGlass = Color(0xB81E1C18),
    materialGlassThumb = Color(0x801E1C18),
    materialGlassRim = Color(0x33FFFFFF),
)

val TesseraLightHighContrastColors = TesseraColors(
    bgCanvas = Color(0xFFEFEBE4),
    bgSurface = Color(0xFFFBF9F5),
    bgElevated = Color(0xFFFFFFFF),
    bgElevated2 = Color(0xFFFFFFFF),
    bgInverse = Color(0xFF1F1B16),
    bgScrim = Color(0x5C1F1B16),
    fillDefault = Color(0x0F1F1B16),
    fillControlOff = Color(0xFF7D776B),
    textPrimary = Color(0xFF1F1B16),
    textSecondary = Color(0xFF4A453D),
    textTertiary = Color(0xFF5E584E),
    textOnSolid = Color(0xFFFFFFFF),
    textInverse = Color(0xFFF0ECE4),
    borderSubtle = Color(0x471F1B16),
    borderStrong = Color(0x731F1B16),
    borderSurface = Color(0x801F1B16),
    borderField = Color(0x801F1B16),
    edgeSolid = Color(0x38000000),
    edgeSoft = Color(0x1A1F1B16),
    sheen = Color(0x29FFFFFF),
    accentSolid = Color(0xFF0F6B63),
    accentControl = Color(0xFF0F6B63),
    accentText = Color(0xFF0F6B63),
    accentSubtle = Color(0x1A0F6B63),
    dangerSolid = Color(0xFFB42318),
    dangerText = Color(0xFFB42318),
    dangerSubtle = Color(0x1AB42318),
    successSolid = Color(0xFF3D6B1C),
    successText = Color(0xFF3D6B1C),
    successSubtle = Color(0x1A3D6B1C),
    warningSolid = Color(0xFF8A5600),
    warningText = Color(0xFF8A5600),
    warningSubtle = Color(0x1A8A5600),
    focusGlow = Color(0x380F6B63),
    focusRing = Color(0xFF0F6B63),
    selection = Color(0x381F8A7E),
    stateHover = Color(0x0D1F1B16),
    statePressed = Color(0x171F1B16),
    stateSolidHover = Color(0x14FFFFFF),
    stateSolidPressed = Color(0x1A000000),
    shadowColor = Color(0xFF1F1B16),
    pigmentCinnabar = Color(0xFFC4472D),
    pigmentCinnabarSubtle = Color(0x24C4472D),
    pigmentCinnabarText = Color(0xFF93301C),
    pigmentOchre = Color(0xFFB7862B),
    pigmentOchreSubtle = Color(0x24B7862B),
    pigmentOchreText = Color(0xFF7A5512),
    pigmentLapis = Color(0xFF2F4E9E),
    pigmentLapisSubtle = Color(0x242F4E9E),
    pigmentLapisText = Color(0xFF243E80),
    pigmentAmethyst = Color(0xFF7B4C9C),
    pigmentAmethystSubtle = Color(0x247B4C9C),
    pigmentAmethystText = Color(0xFF5E3778),
    pigmentOlive = Color(0xFF6A7A2B),
    pigmentOliveSubtle = Color(0x246A7A2B),
    pigmentOliveText = Color(0xFF4D5A1A),
    materialGlass = Color(0xB8FBF9F5),
    materialGlassThumb = Color(0x9EFFFFFF),
    materialGlassRim = Color(0xBFFFFFFF),
)

val TesseraDarkHighContrastColors = TesseraColors(
    bgCanvas = Color(0xFF151310),
    bgSurface = Color(0xFF1E1C18),
    bgElevated = Color(0xFF282521),
    bgElevated2 = Color(0xFF322F2A),
    bgInverse = Color(0xFFF0ECE4),
    bgScrim = Color(0x99000000),
    fillDefault = Color(0x14F0ECE4),
    fillControlOff = Color(0xFF7D776B),
    textPrimary = Color(0xFFF0ECE4),
    textSecondary = Color(0xFFBDB6A9),
    textTertiary = Color(0xFFADA699),
    textOnSolid = Color(0xFFFFFFFF),
    textInverse = Color(0xFF1F1B16),
    borderSubtle = Color(0x4DF0ECE4),
    borderStrong = Color(0x73F0ECE4),
    borderSurface = Color(0x80F0ECE4),
    borderField = Color(0x80F0ECE4),
    edgeSolid = Color(0x52000000),
    edgeSoft = Color(0x66000000),
    sheen = Color(0x1AFFFFFF),
    accentSolid = Color(0xFF0F6B63),
    accentControl = Color(0xFF1F8A7E),
    accentText = Color(0xFF6CBDB1),
    accentSubtle = Color(0x331F8A7E),
    dangerSolid = Color(0xFFB42318),
    dangerText = Color(0xFFFF8A7A),
    dangerSubtle = Color(0x38B42318),
    successSolid = Color(0xFF3D6B1C),
    successText = Color(0xFF9CCB6B),
    successSubtle = Color(0x383D6B1C),
    warningSolid = Color(0xFF8A5600),
    warningText = Color(0xFFE0A643),
    warningSubtle = Color(0x388A5600),
    focusGlow = Color(0x611F8A7E),
    focusRing = Color(0xFF6CBDB1),
    selection = Color(0x521F8A7E),
    stateHover = Color(0x0FF0ECE4),
    statePressed = Color(0x1AF0ECE4),
    stateSolidHover = Color(0x14FFFFFF),
    stateSolidPressed = Color(0x1A000000),
    shadowColor = Color(0xFF000000),
    pigmentCinnabar = Color(0xFFC4472D),
    pigmentCinnabarSubtle = Color(0x42C4472D),
    pigmentCinnabarText = Color(0xFFF0A08C),
    pigmentOchre = Color(0xFFB7862B),
    pigmentOchreSubtle = Color(0x42B7862B),
    pigmentOchreText = Color(0xFFE3B866),
    pigmentLapis = Color(0xFF2F4E9E),
    pigmentLapisSubtle = Color(0x422F4E9E),
    pigmentLapisText = Color(0xFF9DB2EC),
    pigmentAmethyst = Color(0xFF7B4C9C),
    pigmentAmethystSubtle = Color(0x427B4C9C),
    pigmentAmethystText = Color(0xFFC9A6E2),
    pigmentOlive = Color(0xFF6A7A2B),
    pigmentOliveSubtle = Color(0x426A7A2B),
    pigmentOliveText = Color(0xFFBFCB7A),
    materialGlass = Color(0xB81E1C18),
    materialGlassThumb = Color(0x801E1C18),
    materialGlassRim = Color(0x33FFFFFF),
)

object TesseraType {
    /** 标题字体（软衬线） */
    val display = TextStyle(fontSize = 44.sp, lineHeight = 52.sp, fontWeight = FontWeight.SemiBold, letterSpacing = -0.02.em)
    /** 标题字体（软衬线） */
    val title1 = TextStyle(fontSize = 32.sp, lineHeight = 40.sp, fontWeight = FontWeight.SemiBold, letterSpacing = -0.015.em)
    /** 标题字体（软衬线） */
    val title2 = TextStyle(fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.SemiBold, letterSpacing = -0.01.em)
    /** 标题字体（软衬线） */
    val title3 = TextStyle(fontSize = 19.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.em)
    /** 正文字体 */
    val body = TextStyle(fontSize = 16.sp, lineHeight = 26.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.em)
    /** 正文字体 */
    val bodySm = TextStyle(fontSize = 14.sp, lineHeight = 22.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.em)
    /** 正文字体 */
    val label = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.em)
    /** 正文字体 */
    val caption = TextStyle(fontSize = 12.sp, lineHeight = 18.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.em)
    /** 正文字体 */
    val overline = TextStyle(fontSize = 11.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.12.em)
    const val READING_LINE_HEIGHT = 1.75f
}

object TesseraSpace {
    val s0 = 0.dp
    val s1 = 4.dp
    val s2 = 8.dp
    val s3 = 12.dp
    val s4 = 16.dp
    val s5 = 20.dp
    val s6 = 24.dp
    val s8 = 32.dp
    val s10 = 40.dp
    val s12 = 48.dp
    val s16 = 64.dp
}

object TesseraSize {
    val sm = 32.dp
    val md = 40.dp
    val lg = 48.dp
    val touchMin = 44.dp
}

object TesseraRadius {
    val grout = 6.dp
    val sm = 8.dp
    val md = 14.dp
    val lg = 18.dp
    val xl = 24.dp
    val s2xl = 32.dp
    val full = 9999.dp
}

object TesseraOpacity {
    const val DISABLED = 0.4f
}

/** 拼贴：成组的砖之间留缝，实色砖底部有砖厚 */
object TesseraTile {
    val grout = 3.dp
    val edge = 2.dp
}

/** 圆融外观的形状数值（TesseraTheme(shape = TesseraShape.Round) 时使用） */
object TesseraRound {
    val pill = 9999.dp
    val card = 28.dp
    val panel = 36.dp
    val row = 22.dp
    val menu = 24.dp
    val gap = 8.dp
    val groupGap = 0.dp
    const val PRESS_SCALE = 0.97f
}

object TesseraFocus {
    val ringWidth = 2.dp
    val ringOffset = 2.dp
    val glowWidth = 3.dp
}

object TesseraElevation {
    /** 阴影层：y 偏移、模糊半径、浅色不透明度、深色不透明度 */
    data class Layer(val y: Float, val blur: Float, val lightAlpha: Float, val darkAlpha: Float)
    val level1 = listOf(Layer(1f, 2f, 0.05f, 0.4f), Layer(4f, 14f, 0.05f, 0.4f))
    val level2 = listOf(Layer(2f, 6f, 0.07f, 0.4f), Layer(14f, 36f, 0.1f, 0.4f))
    val level3 = listOf(Layer(4f, 12f, 0.09f, 0.4f), Layer(28f, 72f, 0.16f, 0.4f))
}

object TesseraMotion {
    /** 时长 0.3s，回弹 0.15 */
    fun <T> snappy(): SpringSpec<T> = spring(dampingRatio = 0.85f, stiffness = 438.6f)
    /** 时长 0.4s，回弹 0（默认） */
    fun <T> smooth(): SpringSpec<T> = spring(dampingRatio = 1f, stiffness = 246.7f)
    /** 时长 0.5s，回弹 0.25 */
    fun <T> bouncy(): SpringSpec<T> = spring(dampingRatio = 0.75f, stiffness = 157.9f)
    const val DURATION_INSTANT = 100
    const val DURATION_FAST = 150
    const val DURATION_NORMAL = 250
    const val DURATION_SLOW = 350
    val EaseStandard = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val EaseEnter = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    val EaseExit = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
    val EaseLinear = LinearEasing
    /** 按下时砖往下沉的距离（dp） */
    const val PRESS_SINK_DP = 1f
    const val PRESS_DURATION = 80
    const val SETTLE_LIFT_DP = 6f
    const val SETTLE_SCALE = 1.015f
    const val FLING_VELOCITY_DP = 500f
    const val STAGGER_STEP = 30
    const val STAGGER_MAX_ITEMS = 8
    const val TOAST_DURATION = 3000L
    const val TOAST_DURATION_WITH_ACTION = 5000L
}

object TesseraZ {
    const val STICKY = 100f
    const val OVERLAY = 200f
    const val MODAL = 300f
    const val POPOVER = 400f
    const val TOAST = 500f
    const val TOOLTIP = 600f
}
