package com.tesseraui.foundation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tesseraui.LocalReduceMotion
import com.tesseraui.Tessera
import com.tesseraui.TesseraMotion
import com.tesseraui.TesseraRadius
import com.tesseraui.TesseraFlow
import com.tesseraui.TesseraRound
import com.tesseraui.TesseraTile

/** 拼贴组里一块砖的位置：决定哪几个角是“外角”（大圆角） */
enum class TilePosition { Single, First, Middle, Last }

/** 组内第 index 块（共 count 块）的位置 */
fun tilePosition(index: Int, count: Int): TilePosition = when {
    count <= 1 -> TilePosition.Single
    index == 0 -> TilePosition.First
    index == count - 1 -> TilePosition.Last
    else -> TilePosition.Middle
}

/**
 * 拼贴形状：外角用 outer，砖与砖相邻的内角用 inner（默认砖缝圆角 6）。
 * vertical = true 时首块是上方的砖，否则是左侧的砖。
 */
fun tileShape(position: TilePosition, outer: Dp, inner: Dp = TesseraRadius.grout, vertical: Boolean = false): RoundedCornerShape =
    when (position) {
        TilePosition.Single -> RoundedCornerShape(outer)
        TilePosition.Middle -> RoundedCornerShape(inner)
        TilePosition.First -> if (vertical) RoundedCornerShape(outer, outer, inner, inner) else RoundedCornerShape(outer, inner, inner, outer)
        TilePosition.Last -> if (vertical) RoundedCornerShape(inner, inner, outer, outer) else RoundedCornerShape(inner, outer, outer, inner)
    }

/**
 * 釉面砖质感：顶部 1dp 釉光、底部砖厚（默认 2dp，按下时 1dp）。
 * 用渐变色带绘制，并依赖外层 clip(shape) 裁切，圆角处平滑、没有锯齿。
 * 须放在 clip 与 background 之后。
 */
fun Modifier.glaze(edgeColor: Color, sheenColor: Color = Color.Transparent, edge: Dp = TesseraTile.edge): Modifier = drawBehind {
    val e = edge.toPx()
    drawRect(
        brush = Brush.verticalGradient(0f to Color.Transparent, 0.35f to edgeColor, 1f to edgeColor, startY = size.height - e * 1.4f, endY = size.height),
        topLeft = Offset(0f, size.height - e * 1.4f),
        size = Size(size.width, e * 1.4f),
    )
    if (sheenColor.alpha > 0f) {
        val s = 2.dp.toPx()
        drawRect(brush = Brush.verticalGradient(listOf(sheenColor, Color.Transparent), startY = 0f, endY = s), size = Size(size.width, s))
    }
}

/** 实色砖：釉光 + 深色砖厚。圆融外观下改为整面柔光：顶部略亮、底部略暗，没有硬边 */
@Composable
fun Modifier.raisedSolid(pressed: Boolean = false): Modifier {
    val c = Tessera.colors
    if (isRound) return softSheen()
    return glaze(c.edgeSolid, c.sheen, if (pressed) 1.dp else TesseraTile.edge)
}

/** 浅色砖：只有浅砖厚。圆融外观下没有砖厚 */
@Composable
fun Modifier.raisedSoft(pressed: Boolean = false): Modifier =
    if (isRound) this else glaze(Tessera.colors.edgeSoft, edge = if (pressed) 1.dp else TesseraTile.edge)

/** 圆融外观的整面柔光 */
fun Modifier.softSheen(): Modifier = drawBehind {
    drawRect(Brush.verticalGradient(0f to Color.White.copy(alpha = 0.14f), 0.6f to Color.Transparent))
    drawRect(Brush.verticalGradient(0.45f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.06f)))
}

/** 凹槽：顶部 3dp 渐隐的内阴影（相当于 Web 的 inset 0 1px 2px）。圆融外观下是平的浅底，没有凹槽 */
@Composable
fun Modifier.sunken(): Modifier {
    if (isRound) return this
    val c = Tessera.colors.edgeSoft
    return drawBehind {
        val h = 3.dp.toPx()
        drawRect(brush = Brush.verticalGradient(listOf(c, Color.Transparent), startY = 0f, endY = h), size = Size(size.width, h))
    }
}

/**
 * 按下反馈：拼贴外观下沉 1dp，圆融外观轻轻一捏（缩小到 97%）；按下 80ms，松手 snappy 弹回。
 * 系统“移除动画”时不位移、不缩放。
 */
fun Modifier.pressSink(interactionSource: InteractionSource, enabled: Boolean = true): Modifier = composed {
    val pressed by interactionSource.collectIsPressedAsState()
    val reduce = LocalReduceMotion.current
    val shape = LocalTesseraShape.current
    val round = shape != TesseraShape.Tile
    val pressScale = if (shape == TesseraShape.Flow) TesseraFlow.PRESS_SCALE else TesseraRound.PRESS_SCALE
    val t by animateFloatAsState(
        targetValue = if (pressed && enabled && !reduce) 1f else 0f,
        // 流韵：松手用 bouncy，回弹更明显
        animationSpec = if (pressed) tween(TesseraMotion.PRESS_DURATION) else if (shape == TesseraShape.Flow) TesseraMotion.bouncy() else TesseraMotion.snappy(),
        label = "pressSink",
    )
    graphicsLayer {
        if (round) {
            val s = 1f - (1f - pressScale) * t
            scaleX = s; scaleY = s
        } else {
            translationY = TesseraMotion.PRESS_SINK_DP * t * density
        }
    }
}

/**
 * 聚焦光晕：在控件外侧画一圈 3dp 的半透明光晕，再加 1dp 焦点描边（与 Web 输入框一致）。
 * progress 为 0–1，用于动画。
 */
fun Modifier.focusGlow(progress: Float, radius: Dp, glow: Color, ring: Color): Modifier = drawBehind {
    if (progress <= 0f) return@drawBehind
    val w = 3.dp.toPx() * progress
    val r = radius.toPx()
    drawRoundRect(color = glow, topLeft = Offset(-w, -w), size = Size(size.width + 2 * w, size.height + 2 * w), cornerRadius = CornerRadius(r + w))
    drawRoundRect(color = ring.copy(alpha = ring.alpha * progress), style = Stroke(1.dp.toPx()), cornerRadius = CornerRadius(r))
}
