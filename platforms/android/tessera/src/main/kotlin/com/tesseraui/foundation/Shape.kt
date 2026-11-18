package com.tesseraui.foundation

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tesseraui.TesseraFlow
import com.tesseraui.TesseraRound
import com.tesseraui.TesseraTile

/**
 * 外观。
 * - Tile（拼贴，默认）：圆角方砖，成组时外角大、内角小，中间留砖缝；砖有釉光和砖厚，输入框是凹槽。
 * - Round（圆融）：胶囊与大圆角，成组控件合成一整块，没有砖缝和分割线；去掉砖厚和凹槽，按下是轻轻一捏。
 * - Flow（流韵）：以圆融为基础，圆角介于方砖与胶囊之间（按下时变成胶囊），按下处荡开水波，开关溅起涟漪，
 *   并有触感反馈。只在性能较好的设备上启用：TesseraTheme 会检测设备，不满足时自动改用 Round。
 */
enum class TesseraShape { Tile, Round, Flow }

val LocalTesseraShape = staticCompositionLocalOf { TesseraShape.Tile }

/** 当前是否为圆融或流韵外观（成组控件合成一整块，没有砖缝、砖厚和分割线） */
val isRound: Boolean
    @Composable @ReadOnlyComposable get() = LocalTesseraShape.current != TesseraShape.Tile

/**
 * 是否画描边：拼贴外观一直画；圆融、流韵平时不画，增强对比度下加回 1dp 描边，保证边界可辨。
 */
val showOutline: Boolean
    @Composable @ReadOnlyComposable get() = !isRound || com.tesseraui.LocalTesseraHighContrast.current

/** 当前是否为流韵外观 */
val isFlow: Boolean
    @Composable @ReadOnlyComposable get() = LocalTesseraShape.current == TesseraShape.Flow

/** 圆融的圆角在流韵外观下换成“方圆之间”的值：胶囊 → 17、卡片 28 → 26、面板 36 → 34、行 22 → 18、菜单 24 → 22 */
@Composable
@ReadOnlyComposable
fun roundRadius(round: Dp): Dp = if (!isFlow) round else when (round) {
    TesseraRound.pill -> TesseraFlow.control
    TesseraRound.card -> TesseraFlow.card
    TesseraRound.panel -> TesseraFlow.panel
    TesseraRound.row -> TesseraFlow.row
    TesseraRound.menu -> TesseraFlow.menu
    else -> round
}

/** 单个控件的圆角：拼贴用 tile，圆融用 round（默认胶囊） */
@Composable
@ReadOnlyComposable
fun shapeRadius(tile: Dp, round: Dp = TesseraRound.pill): Dp = if (isRound) roundRadius(round) else tile

/** 单个控件的形状 */
@Composable
@ReadOnlyComposable
fun shapeOf(tile: Dp, round: Dp = TesseraRound.pill): Shape = RoundedCornerShape(shapeRadius(tile, round))

/** 头像等：拼贴是 36% 圆角的方砖，圆融是圆 */
@Composable
@ReadOnlyComposable
fun avatarShape(): Shape = if (isFlow) RoundedCornerShape(38) else if (isRound) CircleShape else RoundedCornerShape(36)

/** 成组控件之间的缝：拼贴 3dp，圆融 0（合成一整块） */
val groupGap: Dp
    @Composable @ReadOnlyComposable get() = if (isRound) TesseraRound.groupGap else TesseraTile.grout

/** 独立的一排胶囊之间的间距（对话框按钮等）：拼贴仍是砖缝，圆融 8dp */
val pillGap: Dp
    @Composable @ReadOnlyComposable get() = if (isRound) TesseraRound.gap else TesseraTile.grout

/**
 * 成组控件里一块的形状：拼贴是外角 outer、内角 6；圆融是外角 roundOuter（默认胶囊）、内角 0，合成一整块。
 */
@Composable
@ReadOnlyComposable
fun groupShape(position: TilePosition, outer: Dp, vertical: Boolean = false, roundOuter: Dp = TesseraRound.pill): Shape =
    if (isRound) tileShape(position, roundRadius(roundOuter), 0.dp, vertical) else tileShape(position, outer, vertical = vertical)
