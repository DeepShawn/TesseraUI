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
import androidx.compose.ui.unit.Dp
import com.tesseraui.Tessera
import com.tesseraui.TesseraMotion
import com.tesseraui.TesseraSize
import com.tesseraui.TesseraSpace
import com.tesseraui.TesseraType

enum class TesseraButtonVariant { Primary, Secondary, Text, Danger }
enum class TesseraButtonSize(val height: Dp, val padding: Dp) {
    Sm(TesseraSize.sm, TesseraSpace.s3), Md(TesseraSize.md, TesseraSpace.s4), Lg(TesseraSize.lg, TesseraSpace.s6)
}

/** 按钮：胶囊形；按下 100ms 缩到 0.97，松手用 snappy 弹回。 */
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
    val scale by animateFloatAsState(
        targetValue = if (pressed) TesseraMotion.PRESS_SCALE else 1f,
        animationSpec = if (pressed) tween(TesseraMotion.DURATION_INSTANT) else TesseraMotion.snappy(),
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
    // 注：Compose 没有连续曲率圆角，胶囊形用 50% 圆角；squircle Shape 计划在 v0.2 提供。
    val shape = RoundedCornerShape(percent = 50)

    Box(
        modifier = modifier
            .defaultMinSize(minWidth = TesseraSize.touchMin, minHeight = TesseraSize.touchMin)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .alpha(if (enabled) 1f else 0.4f)
                .height(size.height)
                .clip(shape)
                .background(bg)
                .background(overlay)
                .padding(horizontal = size.padding),
            contentAlignment = Alignment.Center,
        ) {
            BasicText(text, style = TesseraType.label.copy(color = fg))
        }
    }
}
