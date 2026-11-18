package com.tesseraui.components

import com.tesseraui.TesseraMotion
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.animation.core.animateDpAsState
import com.tesseraui.foundation.TesseraHaptic
import com.tesseraui.foundation.rememberFlowHaptic
import com.tesseraui.foundation.flowRipple
import com.tesseraui.foundation.shapeRadius
import com.tesseraui.foundation.isFlow
import com.tesseraui.foundation.isRound
import com.tesseraui.foundation.shapeOf
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tesseraui.LocalContentColor
import com.tesseraui.Tessera
import com.tesseraui.TesseraOpacity
import com.tesseraui.TesseraRadius
import com.tesseraui.TesseraSize
import com.tesseraui.TesseraSpace
import com.tesseraui.TesseraText
import com.tesseraui.foundation.pressSink
import com.tesseraui.foundation.raisedSoft
import com.tesseraui.foundation.raisedSolid
import com.tesseraui.icons.TesseraIcon

enum class TesseraButtonVariant { Primary, Secondary, Text, Danger }

enum class TesseraButtonSize(val height: Dp, val padding: Dp, val radius: Dp) {
    Sm(TesseraSize.sm, TesseraSpace.s3, 11.dp),
    Md(TesseraSize.md, TesseraSpace.s4, TesseraRadius.md),
    Lg(TesseraSize.lg, TesseraSpace.s6, 17.dp),
}

/**
 * 按钮：圆角方砖。实色砖带釉光与砖厚，浅色砖带浅砖厚；按下下沉 1dp、砖厚变薄，松手 snappy 弹回。
 * shape 可传入 tileShape(…) 用于按钮组。热区不小于 44×44。
 */
@Composable
fun TesseraButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: TesseraButtonVariant = TesseraButtonVariant.Primary,
    size: TesseraButtonSize = TesseraButtonSize.Md,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    loading: Boolean = false,
    shape: Shape? = null,
) {
    val c = Tessera.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val hovered by interaction.collectIsHoveredAsState()
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
    val overlay = when {
        pressed && enabled -> if (solid) c.stateSolidPressed else c.statePressed
        hovered && enabled -> if (solid) c.stateSolidHover else c.stateHover
        else -> Color.Transparent
    }
    // 流韵：静止时方圆之间，按下变成胶囊，松手 bouncy 弹回；按下处荡开水波；实色按钮有轻触反馈
    val flow = isFlow
    val baseRadius = shapeRadius(size.radius)
    val radius by animateDpAsState(if (flow && pressed && enabled) size.height / 2 else baseRadius, if (pressed) tween(120) else TesseraMotion.bouncy(), label = "radius")
    val s = shape ?: RoundedCornerShape(radius)
    val haptic = rememberFlowHaptic()
    LaunchedEffect(pressed) { if (pressed && solid && enabled) haptic(if (variant == TesseraButtonVariant.Danger) TesseraHaptic.Warning else TesseraHaptic.Light) }
    Box(
        modifier = modifier
            .defaultMinSize(minWidth = TesseraSize.touchMin, minHeight = TesseraSize.touchMin)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled && !loading, role = Role.Button, onClick = onClick)
            .semantics { if (loading) stateDescription = "加载中" },
        contentAlignment = Alignment.Center,
        propagateMinConstraints = true, // 外部给定宽度（如 weight）时，按钮砖撑满
    ) {
        Row(
            modifier = Modifier
                .pressSink(interaction, enabled)
                .alpha(if (enabled) 1f else TesseraOpacity.DISABLED)
                .height(size.height)
                .clip(s)
                .background(bg)
                .then(
                    when (variant) {
                        TesseraButtonVariant.Text -> Modifier
                        TesseraButtonVariant.Secondary -> Modifier.raisedSoft(pressed)
                        else -> Modifier.raisedSolid(pressed)
                    },
                )
                .background(overlay)
                .flowRipple(interaction, fg.copy(alpha = if (solid) 0.22f else 0.13f))
                .padding(horizontal = if (variant == TesseraButtonVariant.Text) TesseraSpace.s3 else size.padding),
            horizontalArrangement = Arrangement.spacedBy(TesseraSpace.s2, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CompositionLocalProvider(LocalContentColor provides fg) {
                Box(contentAlignment = Alignment.Center) {
                    Row(Modifier.alpha(if (loading) 0f else 1f), horizontalArrangement = Arrangement.spacedBy(TesseraSpace.s2), verticalAlignment = Alignment.CenterVertically) {
                        if (icon != null) TesseraIcon(icon, null, size = 18.dp)
                        TesseraText(text, style = if (size == TesseraButtonSize.Lg) Tessera.type.label.copy(fontSize = Tessera.type.body.fontSize) else Tessera.type.label, maxLines = 1)
                    }
                    if (loading) TesseraSpinner(size = 16.dp)
                }
            }
        }
    }
}

/** 图标按钮：方砖形；filled 时有底色和砖厚 */
@Composable
fun TesseraIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    filled: Boolean = false,
    size: TesseraButtonSize = TesseraButtonSize.Md,
    enabled: Boolean = true,
    badge: (@Composable () -> Unit)? = null,
    shape: Shape? = null,
) {
    val c = Tessera.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val flow = isFlow
    val baseRadius = shapeRadius(size.radius)
    val radius by animateDpAsState(if (flow && pressed && enabled) size.height / 2 else baseRadius, if (pressed) tween(120) else TesseraMotion.bouncy(), label = "radius")
    val s = shape ?: RoundedCornerShape(radius)
    Box(
        modifier = modifier
            .size(maxOf(size.height, TesseraSize.touchMin))
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .pressSink(interaction, enabled)
                .alpha(if (enabled) 1f else TesseraOpacity.DISABLED)
                .size(size.height)
                .clip(s)
                .background(if (filled) c.fillDefault else Color.Transparent)
                .then(if (filled) Modifier.raisedSoft(pressed) else Modifier)
                .background(if (pressed) c.statePressed else Color.Transparent)
                .flowRipple(interaction, c.textPrimary.copy(alpha = 0.13f)),
            contentAlignment = Alignment.Center,
        ) {
            CompositionLocalProvider(LocalContentColor provides c.textPrimary) { TesseraIcon(icon, null) }
        }
        if (badge != null) Box(Modifier.align(Alignment.TopEnd).padding(top = 4.dp, end = 4.dp)) { badge() }
    }
}

/** 加载指示：四块小砖轮流亮起（只用在按钮和小区域里） */
@Composable
fun TesseraSpinner(modifier: Modifier = Modifier, size: Dp = 20.dp, color: Color = Color.Unspecified) {
    val c = if (color != Color.Unspecified) color else LocalContentColor.current.takeIf { it != Color.Unspecified } ?: Tessera.colors.textSecondary
    val t = rememberInfiniteTransition(label = "spinner")
    val step by t.animateFloat(0f, 4f, infiniteRepeatable(tween(1000, easing = StepsEasing(4)), RepeatMode.Restart), label = "step")
    val gap = size / 10
    val tile = (size - gap) / 2
    // 顺时针：左上 → 右上 → 右下 → 左下
    val order = listOf(0, 1, 3, 2)
    // 圆融外观：四颗小圆点
    val dot = if (isRound) RoundedCornerShape(50) else RoundedCornerShape(tile / 3)
    Column(modifier.size(size).semantics { contentDescription = "加载中" }, verticalArrangement = Arrangement.spacedBy(gap)) {
        for (row in 0..1) Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
            for (col in 0..1) {
                val i = order.indexOf(row * 2 + col)
                Box(Modifier.size(tile).clip(dot).background(c.copy(alpha = if (step.toInt() % 4 == i) 1f else 0.25f)))
            }
        }
    }
}

private class StepsEasing(private val steps: Int) : androidx.compose.animation.core.Easing {
    override fun transform(fraction: Float): Float = kotlin.math.floor(fraction * steps) / steps
}
