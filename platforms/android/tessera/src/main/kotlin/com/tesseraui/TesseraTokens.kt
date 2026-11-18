// TesseraUI v0.1.0 — 由 scripts/build.mjs 从 tokens/*.json 生成，请勿手改。
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
import androidx.compose.ui.unit.sp

@Immutable
data class TesseraColors(
    val bgCanvas: Color,
    val bgSurface: Color,
    val bgElevated: Color,
    val bgElevated2: Color,
    val bgScrim: Color,
    /** 开关关闭时的轨道 */ val fillControlOff: Color,
    val fillDefault: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val textOnSolid: Color,
    val borderSubtle: Color,
    val borderStrong: Color,
    /** 卡片、浮层的外描边：浅色没有，深色用 subtle，增强对比度用 strong */ val borderSurface: Color,
    /** 输入框描边：平时没有，增强对比度时补上 */ val borderField: Color,
    val accentSolid: Color,
    val accentText: Color,
    /** 非文字的实色控件：开关开启、复选框和单选选中、滑块轨道。深色下调亮，保证对底色 ≥ 3:1 */ val accentControl: Color,
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
    val materialGlass: Color,
    val shadowColor: Color,
)

val TesseraLightColors = TesseraColors(
    bgCanvas = Color(0xFFF5F5F7),
    bgSurface = Color(0xFFFFFFFF),
    bgElevated = Color(0xFFFFFFFF),
    bgElevated2 = Color(0xFFFFFFFF),
    bgScrim = Color(0x52000000),
    fillControlOff = Color(0xFFC7C7CC),
    fillDefault = Color(0x0D000000),
    textPrimary = Color(0xFF1D1D1F),
    textSecondary = Color(0xFF68686D),
    textTertiary = Color(0xFF8E8E93),
    textOnSolid = Color(0xFFFFFFFF),
    borderSubtle = Color(0x0F000000),
    borderStrong = Color(0x7A000000),
    borderSurface = Color(0x00000000),
    borderField = Color(0x00000000),
    accentSolid = Color(0xFF5B5BD6),
    accentText = Color(0xFF5252CF),
    accentControl = Color(0xFF5B5BD6),
    accentSubtle = Color(0x1A5B5BD6),
    dangerSolid = Color(0xFFC0292E),
    dangerText = Color(0xFFC0292E),
    dangerSubtle = Color(0x1AC0292E),
    successSolid = Color(0xFF146B3A),
    successText = Color(0xFF146B3A),
    successSubtle = Color(0x1A146B3A),
    warningSolid = Color(0xFF9A5100),
    warningText = Color(0xFF9A5100),
    warningSubtle = Color(0x1A9A5100),
    focusGlow = Color(0x405B5BD6),
    focusRing = Color(0xFF5252CF),
    selection = Color(0x335B5BD6),
    stateHover = Color(0x0F000000),
    statePressed = Color(0x1A000000),
    stateSolidHover = Color(0x14FFFFFF),
    stateSolidPressed = Color(0x14000000),
    materialGlass = Color(0xB8FFFFFF),
    shadowColor = Color(0xFF000000),
)

val TesseraDarkColors = TesseraColors(
    bgCanvas = Color(0xFF111113),
    bgSurface = Color(0xFF1A1A1D),
    bgElevated = Color(0xFF232327),
    bgElevated2 = Color(0xFF2C2C31),
    bgScrim = Color(0x8F000000),
    fillControlOff = Color(0xFF48484E),
    fillDefault = Color(0x14FFFFFF),
    textPrimary = Color(0xFFEDEDF0),
    textSecondary = Color(0xFFA1A1A8),
    textTertiary = Color(0xFF68686D),
    textOnSolid = Color(0xFFFFFFFF),
    borderSubtle = Color(0x14FFFFFF),
    borderStrong = Color(0x7AFFFFFF),
    borderSurface = Color(0x14FFFFFF),
    borderField = Color(0x00000000),
    accentSolid = Color(0xFF5B5BD6),
    accentText = Color(0xFF9C9CF0),
    accentControl = Color(0xFF6E6EE3),
    accentSubtle = Color(0x335B5BD6),
    dangerSolid = Color(0xFFC0292E),
    dangerText = Color(0xFFFF6B6B),
    dangerSubtle = Color(0x33C0292E),
    successSolid = Color(0xFF146B3A),
    successText = Color(0xFF4CC38A),
    successSubtle = Color(0x33146B3A),
    warningSolid = Color(0xFF9A5100),
    warningText = Color(0xFFF5A524),
    warningSubtle = Color(0x339A5100),
    focusGlow = Color(0x665B5BD6),
    focusRing = Color(0xFF9C9CF0),
    selection = Color(0x335B5BD6),
    stateHover = Color(0x0FFFFFFF),
    statePressed = Color(0x1AFFFFFF),
    stateSolidHover = Color(0x14FFFFFF),
    stateSolidPressed = Color(0x14000000),
    materialGlass = Color(0xB81A1A1D),
    shadowColor = Color(0xFF000000),
)

val TesseraLightHighContrastColors = TesseraColors(
    bgCanvas = Color(0xFFF5F5F7),
    bgSurface = Color(0xFFFFFFFF),
    bgElevated = Color(0xFFFFFFFF),
    bgElevated2 = Color(0xFFFFFFFF),
    bgScrim = Color(0x52000000),
    fillControlOff = Color(0xFF8E8E93),
    fillDefault = Color(0x0D000000),
    textPrimary = Color(0xFF1D1D1F),
    textSecondary = Color(0xFF48484E),
    textTertiary = Color(0xFF68686D),
    textOnSolid = Color(0xFFFFFFFF),
    borderSubtle = Color(0x3D000000),
    borderStrong = Color(0x7A000000),
    borderSurface = Color(0x7A000000),
    borderField = Color(0x7A000000),
    accentSolid = Color(0xFF5B5BD6),
    accentText = Color(0xFF5252CF),
    accentControl = Color(0xFF5B5BD6),
    accentSubtle = Color(0x1A5B5BD6),
    dangerSolid = Color(0xFFC0292E),
    dangerText = Color(0xFFC0292E),
    dangerSubtle = Color(0x1AC0292E),
    successSolid = Color(0xFF146B3A),
    successText = Color(0xFF146B3A),
    successSubtle = Color(0x1A146B3A),
    warningSolid = Color(0xFF9A5100),
    warningText = Color(0xFF9A5100),
    warningSubtle = Color(0x1A9A5100),
    focusGlow = Color(0x405B5BD6),
    focusRing = Color(0xFF5252CF),
    selection = Color(0x335B5BD6),
    stateHover = Color(0x0F000000),
    statePressed = Color(0x1A000000),
    stateSolidHover = Color(0x14FFFFFF),
    stateSolidPressed = Color(0x14000000),
    materialGlass = Color(0xB8FFFFFF),
    shadowColor = Color(0xFF000000),
)

val TesseraDarkHighContrastColors = TesseraColors(
    bgCanvas = Color(0xFF111113),
    bgSurface = Color(0xFF1A1A1D),
    bgElevated = Color(0xFF232327),
    bgElevated2 = Color(0xFF2C2C31),
    bgScrim = Color(0x8F000000),
    fillControlOff = Color(0xFF8E8E93),
    fillDefault = Color(0x14FFFFFF),
    textPrimary = Color(0xFFEDEDF0),
    textSecondary = Color(0xFFC7C7CC),
    textTertiary = Color(0xFFA1A1A8),
    textOnSolid = Color(0xFFFFFFFF),
    borderSubtle = Color(0x47FFFFFF),
    borderStrong = Color(0x7AFFFFFF),
    borderSurface = Color(0x7AFFFFFF),
    borderField = Color(0x7AFFFFFF),
    accentSolid = Color(0xFF5B5BD6),
    accentText = Color(0xFF9C9CF0),
    accentControl = Color(0xFF6E6EE3),
    accentSubtle = Color(0x335B5BD6),
    dangerSolid = Color(0xFFC0292E),
    dangerText = Color(0xFFFF6B6B),
    dangerSubtle = Color(0x33C0292E),
    successSolid = Color(0xFF146B3A),
    successText = Color(0xFF4CC38A),
    successSubtle = Color(0x33146B3A),
    warningSolid = Color(0xFF9A5100),
    warningText = Color(0xFFF5A524),
    warningSubtle = Color(0x339A5100),
    focusGlow = Color(0x665B5BD6),
    focusRing = Color(0xFF9C9CF0),
    selection = Color(0x335B5BD6),
    stateHover = Color(0x0FFFFFFF),
    statePressed = Color(0x1AFFFFFF),
    stateSolidHover = Color(0x14FFFFFF),
    stateSolidPressed = Color(0x14000000),
    materialGlass = Color(0xB81A1A1D),
    shadowColor = Color(0xFF000000),
)

object TesseraType {
    val display = TextStyle(fontSize = 40.sp, lineHeight = 48.sp, fontWeight = FontWeight.SemiBold)
    val title1 = TextStyle(fontSize = 32.sp, lineHeight = 40.sp, fontWeight = FontWeight.SemiBold)
    val title2 = TextStyle(fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.SemiBold)
    val title3 = TextStyle(fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold)
    val body = TextStyle(fontSize = 16.sp, lineHeight = 26.sp, fontWeight = FontWeight.Normal)
    val bodySm = TextStyle(fontSize = 14.sp, lineHeight = 22.sp, fontWeight = FontWeight.Normal)
    val label = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold)
    val caption = TextStyle(fontSize = 12.sp, lineHeight = 18.sp, fontWeight = FontWeight.Normal)
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
    val xs = 6.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 20.dp
    val s2xl = 28.dp
    val full = 9999.dp
}

object TesseraFocus {
    val ringWidth = 2.dp
    val ringOffset = 2.dp
    val glowWidth = 3.dp
}

object TesseraElevation {
    /** 阴影层：y 偏移、模糊半径、浅色不透明度、深色不透明度 */
    data class Layer(val y: Float, val blur: Float, val lightAlpha: Float, val darkAlpha: Float)
    val level1 = listOf(Layer(1f, 2f, 0.04f, 0.4f), Layer(4f, 12f, 0.04f, 0.4f))
    val level2 = listOf(Layer(2f, 6f, 0.06f, 0.4f), Layer(12f, 32f, 0.08f, 0.4f))
    val level3 = listOf(Layer(4f, 12f, 0.08f, 0.4f), Layer(24f, 64f, 0.12f, 0.4f))
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
    const val PRESS_SCALE = 0.97f
    const val FLING_VELOCITY_DP = 500f
    const val STAGGER_STEP = 30
    const val STAGGER_MAX_ITEMS = 6
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
