package com.tesseraui.components

import com.tesseraui.foundation.flowRipple
import com.tesseraui.TesseraRound
import com.tesseraui.foundation.shapeRadius
import com.tesseraui.foundation.shapeOf
import com.tesseraui.foundation.isRound
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.tesseraui.LocalContentColor
import com.tesseraui.Tessera
import com.tesseraui.TesseraMotion
import com.tesseraui.TesseraOpacity
import com.tesseraui.TesseraRadius
import com.tesseraui.TesseraSize
import com.tesseraui.TesseraSpace
import com.tesseraui.TesseraText
import com.tesseraui.TesseraTile
import com.tesseraui.foundation.focusGlow
import com.tesseraui.foundation.sunken
import com.tesseraui.icons.TesseraIcon
import com.tesseraui.icons.TesseraIcons
import com.tesseraui.motion

// ------------------------------------------------------------------
// 文本框 TextField：凹槽；聚焦时底色变成砖面色，外圈 3dp 光晕
// ------------------------------------------------------------------

/**
 * 文本框。
 * onCommittedValueChange 只在输入法完成组合（拼音选定汉字）后回调，适合用来做搜索、校验、字数统计；
 * onValueChange 则每次按键都会回调（包括拼音字母）。
 */
@Composable
fun TesseraTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    help: String? = null,
    error: String? = null,
    leadingIcon: ImageVector? = null,
    trailing: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    minLines: Int = 1,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    onCommittedValueChange: ((String) -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val c = Tessera.colors
    val focused by interactionSource.collectIsFocusedAsState()
    val glow by animateFloatAsState(if (focused || error != null) 1f else 0f, tween(TesseraMotion.DURATION_FAST), label = "glow")
    val bg by animateColorAsState(if (focused) c.bgSurface else c.fillDefault, tween(TesseraMotion.DURATION_FAST), label = "bg")
    // 圆融：单行是胶囊、多行是 22 圆角，没有凹槽和描边
    val round = isRound
    val radius = if (singleLine) shapeRadius(TesseraRadius.md) else shapeRadius(TesseraRadius.lg, TesseraRound.row)
    val shape = RoundedCornerShape(radius)

    // 内部保留 TextFieldValue，用来判断输入法是否还在组合中
    var state by remember { mutableStateOf(TextFieldValue(value, TextRange(value.length))) }
    val current = if (state.text == value) state else state.copy(text = value, selection = TextRange(value.length))

    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (label != null) TesseraText(label, style = Tessera.type.label)
        BasicTextField(
            value = current,
            onValueChange = { nv ->
                state = nv
                if (nv.text != value) onValueChange(nv.text)
                if (nv.composition == null && nv.text != current.text) onCommittedValueChange?.invoke(nv.text)
                else if (nv.composition == null && current.composition != null) onCommittedValueChange?.invoke(nv.text)
            },
            enabled = enabled,
            singleLine = singleLine,
            minLines = minLines,
            textStyle = Tessera.type.bodySm.copy(color = c.textPrimary),
            cursorBrush = SolidColor(c.accentSolid),
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            visualTransformation = visualTransformation,
            interactionSource = interactionSource,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { if (error != null) error(error) }
                .focusGlow(glow, radius, if (error != null) c.dangerSubtle else c.focusGlow, if (error != null) c.dangerText else c.focusRing)
                .graphicsLayer { alpha = if (enabled) 1f else TesseraOpacity.DISABLED },
            decorationBox = { inner ->
                Row(
                    Modifier
                        .defaultMinSize(minHeight = TesseraSize.md)
                        .clip(shape)
                        .background(bg)
                        .then(if (!focused) Modifier.sunken() else Modifier)
                        .then(if (round) Modifier else Modifier.border(1.dp, c.borderField, shape))
                        .padding(horizontal = if (round) TesseraSpace.s4 else TesseraSpace.s3, vertical = if (singleLine) 0.dp else TesseraSpace.s3),
                    verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(TesseraSpace.s2),
                ) {
                    if (leadingIcon != null) TesseraIcon(leadingIcon, null, tint = c.textSecondary, size = 18.dp)
                    Box(Modifier.weight(1f)) {
                        if (value.isEmpty() && placeholder != null) TesseraText(placeholder, style = Tessera.type.bodySm, color = c.textTertiary, maxLines = 1)
                        inner()
                    }
                    if (trailing != null) CompositionLocalProvider(LocalContentColor provides c.textSecondary) { trailing() }
                }
            },
        )
        val helpText = error ?: help
        if (helpText != null) TesseraText(helpText, style = Tessera.type.caption, color = if (error != null) c.dangerText else c.textSecondary)
    }
}

/** 搜索框：带搜索图标和清除按钮；回车触发 onSearch（输入法组合中的回车由系统用来选字，不会触发） */
@Composable
fun TesseraSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "搜索",
    onCommittedQueryChange: ((String) -> Unit)? = null,
) {
    TesseraTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        placeholder = placeholder,
        leadingIcon = TesseraIcons.Search,
        trailing = if (query.isNotEmpty()) ({
            Box(Modifier.size(28.dp).clip(shapeOf(10.dp)).clickable(role = Role.Button) { onQueryChange(""); onCommittedQueryChange?.invoke("") }
                .semantics { contentDescription = "清除" }, contentAlignment = Alignment.Center) { TesseraIcon(TesseraIcons.Close, null, size = 16.dp) }
        }) else null,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch(query) }),
        onCommittedValueChange = onCommittedQueryChange,
    )
}

// ------------------------------------------------------------------
// 菜单 Menu：从锚点展开（0.96 → 1 + 淡入，smooth），关闭只淡出 150ms；放不下自动翻到上方
// ------------------------------------------------------------------

private class MenuPosition(private val gap: Int) : PopupPositionProvider {
    override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize): IntOffset {
        val below = anchorBounds.bottom + gap
        val y = if (below + popupContentSize.height <= windowSize.height) below else (anchorBounds.top - gap - popupContentSize.height).coerceAtLeast(0)
        val x = anchorBounds.left.coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0))
        return IntOffset(x, y)
    }
}

/**
 * 下拉菜单。放在锚点所在的 Box 里：
 * Box { TesseraButton(…, onClick = { open = true }); TesseraMenu(open, { open = false }) { TesseraMenuItem(…) } }
 */
@Composable
fun TesseraMenu(expanded: Boolean, onDismissRequest: () -> Unit, modifier: Modifier = Modifier, minWidth: Dp = 220.dp, content: @Composable ColumnScope.() -> Unit) {
    val state = remember { MutableTransitionState(false) }
    state.targetState = expanded
    if (!state.currentState && !state.targetState) return
    val density = androidx.compose.ui.platform.LocalDensity.current
    Popup(popupPositionProvider = remember(density) { MenuPosition(with(density) { 6.dp.roundToPx() }) }, onDismissRequest = onDismissRequest, properties = PopupProperties(focusable = true)) {
        val t = rememberTransition(state, label = "menu")
        val enterScale = motion(TesseraMotion.smooth<Float>())
        val alpha by t.animateFloat(
            transitionSpec = { if (targetState) tween<Float>(TesseraMotion.DURATION_NORMAL) else tween<Float>(TesseraMotion.DURATION_FAST) },
            label = "a",
        ) { if (it) 1f else 0f }
        val scale by t.animateFloat(
            transitionSpec = { if (targetState) enterScale else tween<Float>(TesseraMotion.DURATION_FAST) },
            label = "s",
        ) { if (it) 1f else 0.96f }
        TesseraMenuPanel(modifier.graphicsLayer { this.alpha = alpha; scaleX = scale; scaleY = scale; transformOrigin = TransformOrigin(0.1f, 0f) }, minWidth, content)
    }
}

/** 菜单面板本身（不含弹出层），也可用于自定义弹层或静态展示 */
@Composable
fun TesseraMenuPanel(modifier: Modifier = Modifier, minWidth: Dp = 220.dp, content: @Composable ColumnScope.() -> Unit) {
    val c = Tessera.colors
    val round = isRound
    val shape = shapeOf(TesseraRadius.lg, TesseraRound.menu)
    Column(
        modifier
            .widthIn(min = minWidth)
            .shadow(12.dp, shape, ambientColor = c.shadowColor, spotColor = c.shadowColor)
            .clip(shape)
            .background(c.bgElevated)
            .then(if (round) Modifier else Modifier.border(1.dp, c.borderSubtle, shape))
            .padding(if (round) 6.dp else TesseraSpace.s1),
    ) { CompositionLocalProvider(LocalContentColor provides c.textPrimary) { content() } }
}

@Composable
fun TesseraMenuItem(
    text: String,
    onClick: () -> Unit,
    icon: ImageVector? = null,
    danger: Boolean = false,
    shortcut: String? = null,
    selected: Boolean = false,
    enabled: Boolean = true,
) {
    val c = Tessera.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val fg = if (danger) c.dangerText else c.textPrimary
    Row(
        Modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(shapeOf(TesseraRadius.lg - TesseraSpace.s1, TesseraRound.menu - 6.dp))
            .background(if (pressed) c.statePressed else Color.Transparent)
            .flowRipple(interaction, c.textPrimary.copy(alpha = 0.13f))
            .clickable(interaction, null, enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { if (selected) stateDescription = "已选择" }
            .padding(horizontal = TesseraSpace.s3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TesseraSpace.s3),
    ) {
        if (icon != null) TesseraIcon(icon, null, tint = if (danger) c.dangerText else c.textSecondary, size = 18.dp)
        TesseraText(text, Modifier.weight(1f), style = Tessera.type.bodySm, color = fg, maxLines = 1)
        if (shortcut != null) TesseraText(shortcut, style = Tessera.type.caption, color = c.textTertiary)
        if (selected) TesseraIcon(TesseraIcons.Check, null, tint = c.accentText, size = 18.dp)
    }
}

/** 菜单分隔：拼贴是一道砖缝；圆融只留白，不画线 */
@Composable
fun TesseraMenuSeparator() {
    if (isRound) Box(Modifier.fillMaxWidth().height(TesseraRound.gap))
    else Box(Modifier.fillMaxWidth().padding(vertical = TesseraSpace.s1).height(TesseraTile.grout).background(Tessera.colors.bgCanvas))
}

// ------------------------------------------------------------------
// 下拉选择 Select：触发框是凹槽，弹出菜单选择
// ------------------------------------------------------------------

@Composable
fun TesseraSelect(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String = "请选择",
) {
    val c = Tessera.colors
    var open by remember { mutableStateOf(false) }
    val round = isRound
    val shape = shapeOf(TesseraRadius.md)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (label != null) TesseraText(label, style = Tessera.type.label)
        Box {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(TesseraSize.md)
                    .clip(shape)
                    .background(c.fillDefault)
                    .sunken()
                    .then(if (round) Modifier else Modifier.border(1.dp, c.borderField, shape))
                    .clickable(role = Role.DropdownList) { open = true }
                    .semantics { stateDescription = options.getOrNull(selectedIndex) ?: placeholder }
                    .padding(horizontal = if (round) TesseraSpace.s4 else TesseraSpace.s3),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val text = options.getOrNull(selectedIndex)
                TesseraText(text ?: placeholder, Modifier.weight(1f), style = Tessera.type.bodySm, color = if (text == null) c.textTertiary else c.textPrimary, maxLines = 1)
                TesseraIcon(TesseraIcons.ChevronDown, null, tint = c.textSecondary, size = 16.dp)
            }
            TesseraMenu(open, { open = false }) {
                options.forEachIndexed { i, o -> TesseraMenuItem(o, { onSelect(i); open = false }, selected = i == selectedIndex) }
            }
        }
    }
}

