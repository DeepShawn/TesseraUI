package com.tesseraui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.tesseraui.LocalContentColor
import com.tesseraui.Tessera
import com.tesseraui.TesseraMotion
import com.tesseraui.TesseraOpacity
import com.tesseraui.TesseraRadius
import com.tesseraui.TesseraSize
import com.tesseraui.TesseraSpace
import com.tesseraui.TesseraText
import com.tesseraui.foundation.TesseraHaptic
import com.tesseraui.foundation.flowRipple
import com.tesseraui.foundation.groupGap
import com.tesseraui.foundation.groupShape
import com.tesseraui.foundation.isFlow
import com.tesseraui.foundation.isRound
import com.tesseraui.foundation.pressSink
import com.tesseraui.foundation.raisedSoft
import com.tesseraui.foundation.raisedSolid
import com.tesseraui.foundation.rememberFlowHaptic
import com.tesseraui.foundation.tilePosition
import com.tesseraui.icons.TesseraIcon
import com.tesseraui.icons.TesseraIcons

// ------------------------------------------------------------------
// 按钮组 ButtonGroup：一组拼贴砖，选中的一块是深色实砖（与 Web .ts-button-group 一致）
// 拼贴外观砖与砖之间留 3dp 砖缝、外角大内角小；圆融 / 流韵合成一整块胶囊
// ------------------------------------------------------------------

/**
 * 按钮组。multiple = false 时是单选（类似分段控制，但每项是独立按钮），true 时可多选。
 * selected 是选中项的下标集合。
 */
@Composable
fun TesseraButtonGroup(
    options: List<String>,
    selected: Set<Int>,
    onSelectedChange: (Set<Int>) -> Unit,
    modifier: Modifier = Modifier,
    multiple: Boolean = false,
    icons: List<ImageVector?>? = null,
    size: TesseraButtonSize = TesseraButtonSize.Md,
    enabled: Boolean = true,
) {
    val c = Tessera.colors
    val haptic = rememberFlowHaptic()
    Row(
        modifier.then(if (multiple) Modifier else Modifier.selectableGroup()),
        horizontalArrangement = Arrangement.spacedBy(groupGap),
    ) {
        options.forEachIndexed { i, label ->
            val on = i in selected
            val interaction = remember { MutableInteractionSource() }
            val pressed by interaction.collectIsPressedAsState()
            val shape = groupShape(tilePosition(i, options.size), size.radius)
            val toggle = {
                haptic(TesseraHaptic.Selection)
                onSelectedChange(
                    if (multiple) (if (on) selected - i else selected + i)
                    else setOf(i),
                )
            }
            val selection = if (multiple) {
                Modifier.toggleable(on, interaction, null, enabled, Role.Checkbox) { toggle() }
            } else {
                Modifier.selectable(on, interaction, null, enabled, Role.RadioButton) { toggle() }
            }
            Box(
                Modifier.defaultMinSize(minHeight = TesseraSize.touchMin).then(selection),
                contentAlignment = Alignment.Center,
            ) {
                val fg = if (on) c.bgSurface else c.textPrimary
                Row(
                    Modifier
                        .pressSink(interaction, enabled)
                        .alpha(if (enabled) 1f else TesseraOpacity.DISABLED)
                        .height(size.height)
                        .clip(shape)
                        .background(if (on) c.textPrimary else c.fillDefault)
                        .then(if (on) Modifier.raisedSolid(pressed) else Modifier.raisedSoft(pressed))
                        .background(if (pressed) c.statePressed else Color.Transparent)
                        .flowRipple(interaction, fg.copy(alpha = 0.16f))
                        .padding(horizontal = size.padding),
                    horizontalArrangement = Arrangement.spacedBy(TesseraSpace.s2),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CompositionLocalProvider(LocalContentColor provides fg) {
                        icons?.getOrNull(i)?.let { TesseraIcon(it, null, size = 18.dp) }
                        TesseraText(label, style = Tessera.type.label, maxLines = 1)
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------
// 分割按钮 SplitButton：主操作 + 右侧下拉箭头，两块砖拼在一起
// ------------------------------------------------------------------

/**
 * 分割按钮：左边执行默认操作，右边打开更多选项菜单。
 * menu 里放 TesseraMenuItem（点击后菜单自动关闭）。
 */
@Composable
fun TesseraSplitButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: TesseraButtonVariant = TesseraButtonVariant.Primary,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    menuLabel: String = "更多选项",
    menu: @Composable (close: () -> Unit) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(groupGap)) {
        TesseraButton(
            text, onClick, variant = variant, icon = icon, enabled = enabled,
            shape = groupShape(tilePosition(0, 2), TesseraRadius.md),
        )
        Box {
            SplitArrow(variant, enabled, menuLabel, open) { open = !open }
            TesseraMenu(open, onDismissRequest = { open = false }) { menu { open = false } }
        }
    }
}

@Composable
private fun SplitArrow(variant: TesseraButtonVariant, enabled: Boolean, label: String, open: Boolean, onClick: () -> Unit) {
    val c = Tessera.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val solid = variant == TesseraButtonVariant.Primary || variant == TesseraButtonVariant.Danger
    val bg = when (variant) {
        TesseraButtonVariant.Primary -> c.accentSolid
        TesseraButtonVariant.Danger -> c.dangerSolid
        else -> c.fillDefault
    }
    val fg = if (solid) c.textOnSolid else c.textPrimary
    val shape = groupShape(tilePosition(1, 2), TesseraRadius.md)
    // 箭头随菜单打开翻转
    val rotation by animateDpAsState(if (open) 180.dp else 0.dp, TesseraMotion.snappy(), label = "arrow")
    Box(
        Modifier
            .defaultMinSize(minWidth = TesseraSize.touchMin, minHeight = TesseraSize.touchMin)
            .clickable(interaction, null, enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label },
        // 看得见的砖贴着主按钮（左对齐），多出来的触摸区域在右侧，不在两块砖之间留缝
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .pressSink(interaction, enabled)
                .alpha(if (enabled) 1f else TesseraOpacity.DISABLED)
                .height(TesseraSize.md)
                .width(TesseraSize.md)
                .clip(shape)
                .background(bg)
                .then(if (solid) Modifier.raisedSolid(pressed) else Modifier.raisedSoft(pressed))
                .background(if (pressed) (if (solid) c.stateSolidPressed else c.statePressed) else Color.Transparent)
                .flowRipple(interaction, fg.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center,
        ) {
            CompositionLocalProvider(LocalContentColor provides fg) {
                TesseraIcon(TesseraIcons.ChevronDown, null, size = 18.dp, modifier = Modifier.graphicsRotation(rotation.value))
            }
        }
    }
}

private fun Modifier.graphicsRotation(deg: Float) = graphicsLayer { rotationZ = deg }

// ------------------------------------------------------------------
// 悬浮按钮 FAB：一块 56 高的大釉面砖，带投影；可带文字（扩展形态）
// ------------------------------------------------------------------

/**
 * 悬浮按钮。放在页面右下角（由调用方定位，例如 Box 里 align(BottomEnd)）。
 * text 为 null 时是正方形图标砖，否则是带文字的扩展形态；expanded = false 时收起文字（例如列表向下滚动时）。
 */
@Composable
fun TesseraFab(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String? = null,
    expanded: Boolean = true,
) {
    val c = Tessera.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val haptic = rememberFlowHaptic()
    LaunchedEffect(pressed) { if (pressed) haptic(TesseraHaptic.Light) }
    // 拼贴 20 圆角的方砖；圆融是胶囊；流韵静止 20、按下变胶囊
    val round = isRound
    val flow = isFlow
    val base = if (round && !flow) 28.dp else 20.dp
    val radius by animateDpAsState(if (flow && pressed) 28.dp else base, if (pressed) tween(120) else TesseraMotion.bouncy(), label = "fabRadius")
    val shape = RoundedCornerShape(radius)
    val showText = text != null && expanded
    Box(
        modifier
            .clickable(interaction, null, role = Role.Button, onClick = onClick)
            .semantics { this.contentDescription = if (showText) text!! else contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Row(
            Modifier
                .pressSink(interaction)
                .shadow(if (pressed) 4.dp else 10.dp, shape, ambientColor = c.shadowColor, spotColor = c.shadowColor)
                .clip(shape)
                .background(c.accentSolid)
                .raisedSolid(pressed)
                .background(if (pressed) c.stateSolidPressed else Color.Transparent)
                .flowRipple(interaction, c.textOnSolid.copy(alpha = 0.22f))
                .defaultMinSize(minWidth = 56.dp)
                .height(56.dp)
                .padding(horizontal = if (showText) 20.dp else 16.dp),
            horizontalArrangement = Arrangement.spacedBy(TesseraSpace.s2, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CompositionLocalProvider(LocalContentColor provides c.textOnSolid) {
                TesseraIcon(icon, null, size = 24.dp)
                androidx.compose.animation.AnimatedVisibility(showText) {
                    TesseraText(text ?: "", style = Tessera.type.label.copy(fontSize = Tessera.type.body.fontSize), maxLines = 1)
                }
            }
        }
    }
}

// ------------------------------------------------------------------
// 链接 Link：釉色文字 + 40% 透明度的下划线，按下时下划线变实
// ------------------------------------------------------------------

/** 链接。external = true 时在末尾加一个外链箭头，读屏会说“在新窗口打开” */
@Composable
fun TesseraLink(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    external: Boolean = false,
    style: androidx.compose.ui.text.TextStyle = Tessera.type.bodySm,
) {
    val c = Tessera.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Row(
        modifier
            .defaultMinSize(minHeight = TesseraSize.touchMin)
            .clip(RoundedCornerShape(4.dp))
            .clickable(interaction, null, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = if (external) "$text，在新窗口打开" else text },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val line = c.accentText.copy(alpha = if (pressed) 1f else 0.4f)
        // Compose 的下划线颜色只能跟随文字颜色：改为在底部画一条 40% 透明的线，按下时变实
        Box(Modifier.padding(bottom = 1.dp)) {
            TesseraText(text, style = style, color = c.accentText, modifier = Modifier.underline(line))
        }
        if (external) TesseraIcon(TesseraIcons.ArrowUpRight, null, size = 14.dp, tint = c.accentText, modifier = Modifier.padding(start = 2.dp))
    }
}

private fun Modifier.underline(color: Color) = this.then(
    Modifier.drawBehind {
        val y = size.height - 1.dp.toPx()
        drawLine(color, androidx.compose.ui.geometry.Offset(0f, y), androidx.compose.ui.geometry.Offset(size.width, y), strokeWidth = 1.dp.toPx())
    },
)
