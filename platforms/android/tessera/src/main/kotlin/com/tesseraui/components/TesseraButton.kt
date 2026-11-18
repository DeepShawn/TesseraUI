package com.tesseraui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tesseraui.TesseraRadius
import com.tesseraui.TesseraTile
import com.tesseraui.Tessera
import com.tesseraui.TesseraMotion
import com.tesseraui.TesseraSize
import com.tesseraui.TesseraSpace
import com.tesseraui.TesseraType

enum class TesseraButtonVariant { Primary, Secondary, Text, Danger }
enum class TesseraButtonSize(val height: Dp, val padding: Dp, val radius: Dp) {
    Sm(TesseraSize.sm, TesseraSpace.s3, 8.dp), Md(TesseraSize.md, TesseraSpace.s4, TesseraRadius.md), Lg(TesseraSize.lg, TesseraSpace.s6, 12.dp)
}

/** 按钮：圆角方砖。实色砖顶部一道釉光、底部 2dp 砖厚；按下时下沉 1dp、砖厚变薄，松手用 snappy 弹回。 */
@Composable
fun TesseraButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: TesseraButtonVariant = TesseraButtonVariant.Primary,
    size: TesseraButtonSize = TesseraButtonSize.Md,
    enabled: Boolean = true,
) {
    val c = Tessera.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val sink by animateFloatAsState(
        targetValue = if (pressed) TesseraMotion.PRESS_SINK_DP else 0f,
        animationSpec = if (pressed) tween(TesseraMotion.PRESS_DURATION) else TesseraMotion.snappy(),
        label = "press",
    )
    val solid = variant == TesseraButtonVariant.Primary || variant == TesseraButtonVariant.Danger
    val bg = when (variant) {
        TesseraButtonVariant.Primary -> c.accentSolid
        TesseraButtonVariant.Danger -> c.dangerSolid
        TesseraButtonVariant.Secondary -> c.fillDefault
        TesseraButtonVariant.Text -> Color.Transparent
    }
    val fg = when (variant) {
        TesseraButtonVariant.Primary, TesseraButtonVariant.Danger -> c.textOnSolid
        TesseraButtonVariant.Secondary -> c.textPrimary
        TesseraButtonVariant.Text -> c.accentText
    }
    val overlay = if (pressed) (if (solid) c.stateSolidPressed else c.statePressed) else Color.Transparent
    val edgeColor = if (solid) c.edgeSolid else c.edgeSoft
    val edge = if (pressed) 1.dp else TesseraTile.edge
    // 注：Compose 没有连续曲率圆角，v0.1 先用普通圆角；squircle Shape 计划在 v0.2 提供。
    val shape = RoundedCornerShape(size.radius)

    Box(
        modifier = modifier
            .defaultMinSize(minWidth = TesseraSize.touchMin, minHeight = TesseraSize.touchMin)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .graphicsLayer { translationY = sink * density }
                .alpha(if (enabled) 1f else 0.4f)
                .height(size.height)
                .clip(shape)
                .background(bg)
                .drawBehind {
                    if (variant != TesseraButtonVariant.Text) {
                        // 砖厚：底部深色带
                        drawRect(edgeColor, topLeft = Offset(0f, this.size.height - edge.toPx()), size = Size(this.size.width, edge.toPx()))
                        // 釉光：顶部 1dp 高光
                        if (solid) drawRect(c.sheen, size = Size(this.size.width, 1.dp.toPx()))
                    }
                }
                .background(overlay)
                .padding(horizontal = size.padding)
                .padding(bottom = if (variant == TesseraButtonVariant.Text) 0.dp else 1.dp),
            contentAlignment = Alignment.Center,
        ) {
            BasicText(text, style = TesseraType.label.copy(color = fg))
        }
    }
}
