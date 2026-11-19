package com.tesseraui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tesseraui.LocalReduceMotion
import com.tesseraui.Tessera
import com.tesseraui.TesseraMotion
import com.tesseraui.TesseraRadius
import com.tesseraui.TesseraSize
import com.tesseraui.TesseraSpace
import com.tesseraui.TesseraText
import com.tesseraui.foundation.TesseraHaptic
import com.tesseraui.foundation.focusGlow
import com.tesseraui.foundation.groupGap
import com.tesseraui.foundation.groupShape
import com.tesseraui.foundation.isRound
import com.tesseraui.foundation.rememberFlowHaptic
import com.tesseraui.foundation.shapeOf
import com.tesseraui.foundation.shapeRadius
import com.tesseraui.foundation.showOutline
import com.tesseraui.foundation.sunken
import com.tesseraui.foundation.TilePosition
import com.tesseraui.icons.TesseraIcon
import com.tesseraui.icons.TesseraIcons
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** 全角数字、全角小数点与负号转为半角（中文输入法下常见） */
internal fun normalizeDigits(s: String): String = buildString {
    for (ch in s) append(
        when (ch) {
            in '０'..'９' -> '0' + (ch - '０')
            '．', '。' -> '.'
            '－', '—', '−' -> '-'
            else -> ch
        },
    )
}

// ------------------------------------------------------------------
// 多行文本 TextArea：随内容自动长高；可显示字数（只统计已选定的汉字）
// ------------------------------------------------------------------

/**
 * 多行文本框。minLines 行起，随内容长高，最多 maxLines 行后在框内滚动。
 * maxLength 不为 null 时右下角显示字数；字数只在输入法选定后更新（打拼音时不计数）。
 */
@Composable
fun TesseraTextArea(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    help: String? = null,
    error: String? = null,
    maxLength: Int? = null,
    minLines: Int = 3,
    enabled: Boolean = true,
) {
    var committed by remember { mutableIntStateOf(value.length) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        TesseraTextField(
            value = value,
            onValueChange = { v -> onValueChange(if (maxLength != null) v.take(maxLength * 2) else v) },
            modifier = Modifier.fillMaxWidth(),
            label = label,
            placeholder = placeholder,
            error = error,
            enabled = enabled,
            singleLine = false,
            minLines = minLines,
            onCommittedValueChange = { committed = it.length },
        )
        val helpText = if (error == null) help else null
        if (helpText != null || maxLength != null) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TesseraText(helpText ?: "", style = Tessera.type.caption, color = Tessera.colors.textSecondary)
                if (maxLength != null) {
                    val over = committed > maxLength
                    TesseraText(
                        "$committed / $maxLength",
                        style = Tessera.type.caption,
                        color = if (over) Tessera.colors.dangerText else Tessera.colors.textSecondary,
                        modifier = Modifier.semantics { contentDescription = "已输入 $committed 字，最多 $maxLength 字" },
                    )
                }
            }
        }
    }
}

// ------------------------------------------------------------------
// 密码框 PasswordField：右侧“显示 / 隐藏”切换
// ------------------------------------------------------------------

/**
 * 密码框。右侧按钮切换明文显示。
 * 大写锁定提示：安卓软键盘没有大写锁定状态可读；连接实体键盘时可传入 capsLock 显示提示。
 */
@Composable
fun TesseraPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = "密码",
    placeholder: String? = null,
    help: String? = null,
    error: String? = null,
    capsLock: Boolean = false,
    enabled: Boolean = true,
    imeAction: ImeAction = ImeAction.Done,
    onSubmit: (() -> Unit)? = null,
) {
    var visible by remember { mutableStateOf(false) }
    TesseraTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = label,
        placeholder = placeholder,
        help = if (capsLock && error == null) "大写锁定已打开" else help,
        error = error,
        leadingIcon = TesseraIcons.Lock,
        enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = imeAction, autoCorrectEnabled = false),
        keyboardActions = KeyboardActions(onDone = { onSubmit?.invoke() }, onGo = { onSubmit?.invoke() }),
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation('•'),
        trailing = {
            Box(
                Modifier
                    .size(TesseraSize.md)
                    .padding(end = 0.dp)
                    .clip(shapeOf(10.dp))
                    .clickable(role = Role.Button) { visible = !visible }
                    .semantics { contentDescription = if (visible) "隐藏密码" else "显示密码" },
                contentAlignment = Alignment.Center,
            ) { TesseraIcon(if (visible) TesseraIcons.EyeOff else TesseraIcons.Eye, null, size = 18.dp) }
        },
    )
}

// ------------------------------------------------------------------
// 数字步进 NumberInput：− [数值] + 三块砖拼在一起；按住 − / + 连续调整并逐渐加速
// ------------------------------------------------------------------

/**
 * 数字步进。可直接输入（全角数字自动转半角），失焦或回车时按 min / max / step 修正。
 * 按住 − / + 先停 400ms，然后每 120ms 调一次，连续调 10 次后加速到 40ms。
 */
@Composable
fun TesseraNumberInput(
    value: Double,
    onValueChange: (Double) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    min: Double = Double.NEGATIVE_INFINITY,
    max: Double = Double.POSITIVE_INFINITY,
    step: Double = 1.0,
    decimals: Int = 0,
    enabled: Boolean = true,
) {
    val c = Tessera.colors
    val haptic = rememberFlowHaptic()
    val current by rememberUpdatedState(value)
    fun fmt(v: Double) = if (decimals == 0) v.toLong().toString() else "%.${decimals}f".format(v)
    fun clampStep(v: Double): Double {
        val stepped = if (step > 0) Math.round(v / step) * step else v
        return stepped.coerceIn(min, max)
    }
    var text by remember { mutableStateOf(TextFieldValue(fmt(value))) }
    LaunchedEffect(value) { if (normalizeDigits(text.text).toDoubleOrNull() != value) text = TextFieldValue(fmt(value), TextRange(fmt(value).length)) }
    val bump: (Int) -> Boolean = { dir ->
        val next = clampStep(current + dir * step)
        if (next != current) { onValueChange(next); haptic(TesseraHaptic.Selection); true } else false
    }
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val glow by animateFloatAsState(if (focused) 1f else 0f, tween(TesseraMotion.DURATION_FAST), label = "glow")
    val commit = {
        val v = normalizeDigits(text.text).toDoubleOrNull()
        val fixed = if (v == null) current else clampStep(v)
        if (fixed != current) onValueChange(fixed)
        text = TextFieldValue(fmt(fixed), TextRange(fmt(fixed).length))
    }
    LaunchedEffect(focused) { if (!focused) commit() }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (label != null) TesseraText(label, style = Tessera.type.label)
        Row(
            Modifier.semantics(mergeDescendants = false) { stateDescription = fmt(value) },
            horizontalArrangement = Arrangement.spacedBy(groupGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StepButton(TesseraIcons.Minus, "减少", enabled && value > min, TilePosition.First) { bump(-1) }
            val shape = groupShape(TilePosition.Middle, TesseraRadius.md)
            BasicTextField(
                value = text,
                onValueChange = { nv -> text = nv.copy(text = normalizeDigits(nv.text).filter { it.isDigit() || it == '.' || it == '-' }) },
                enabled = enabled,
                singleLine = true,
                textStyle = Tessera.type.label.copy(color = c.textPrimary, textAlign = TextAlign.Center, fontWeight = FontWeight.SemiBold),
                cursorBrush = SolidColor(c.accentSolid),
                keyboardOptions = KeyboardOptions(keyboardType = if (decimals > 0 || min < 0) KeyboardType.Decimal else KeyboardType.Number, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { commit() }),
                interactionSource = interaction,
                modifier = Modifier
                    .width(88.dp)
                    .height(TesseraSize.md)
                    .focusGlow(glow, 6.dp, c.focusGlow, c.focusRing)
                    .semantics { contentDescription = label ?: "数值" },
                decorationBox = { inner ->
                    Box(
                        Modifier.fillMaxWidth().height(TesseraSize.md).clip(shape).background(if (focused) c.bgSurface else c.fillDefault)
                            .then(if (!focused) Modifier.sunken() else Modifier)
                            .then(if (showOutline && !isRound) Modifier.border(1.dp, c.borderField, shape) else Modifier)
                            .padding(horizontal = TesseraSpace.s2),
                        contentAlignment = Alignment.Center,
                    ) { inner() }
                },
            )
            StepButton(TesseraIcons.Plus, "增加", enabled && value < max, TilePosition.Last) { bump(1) }
        }
    }
}

/** − / + 按钮：点一下调一次；按住连续调整并加速 */
@Composable
private fun StepButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, enabled: Boolean, position: TilePosition, onStep: () -> Boolean) {
    val c = Tessera.colors
    val shape = groupShape(position, TesseraRadius.md)
    val step by rememberUpdatedState(onStep)
    var pressed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    Box(
        Modifier
            .size(TesseraSize.touchMin)
            // 读屏双击、外接键盘回车：走语义里的点击动作调一次；手指按下由下面的手势处理（含按住连续调整）
            .semantics {
                contentDescription = label
                role = Role.Button
                if (enabled) onClick { step(); true } else disabled()
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                awaitEachGesture {
                    awaitFirstDown()
                    pressed = true
                    step()
                    val job = scope.launch {
                        delay(400)
                        var n = 0
                        while (step()) { delay(if (n++ < 10) 120 else 40) }
                    }
                    waitForUpOrCancellation()
                    job.cancel()
                    pressed = false
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(TesseraSize.md).alpha(if (enabled) 1f else 0.4f).clip(shape).background(c.fillDefault)
                .background(if (pressed) c.statePressed else Color.Transparent),
            contentAlignment = Alignment.Center,
        ) { TesseraIcon(icon, null, size = 18.dp, tint = c.textPrimary) }
    }
}

// ------------------------------------------------------------------
// 验证码 OTPInput：一排凹槽格子，一个隐藏的输入框接收输入；可直接粘贴整段验证码
// ------------------------------------------------------------------

/**
 * 验证码输入。length 位数字；粘贴或短信验证码建议自动填入时会一次填满并回调 onComplete。
 * error 不为 null 时格子描红并轻轻晃一下。
 */
@Composable
fun TesseraOtpInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    length: Int = 6,
    error: String? = null,
    onComplete: ((String) -> Unit)? = null,
) {
    val c = Tessera.colors
    val focus = remember { FocusRequester() }
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val shake = remember { Animatable(0f) }
    val reduce = LocalReduceMotion.current
    LaunchedEffect(error) {
        if (error != null && !reduce) for (x in listOf(-6f, 6f, -4f, 4f, 0f)) shake.animateTo(x, tween(50))
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.graphicsLayer { translationX = shake.value * density }) {
            BasicTextField(
                value = TextFieldValue(value, TextRange(value.length)),
                onValueChange = { nv ->
                    val digits = normalizeDigits(nv.text).filter { it.isDigit() }.take(length)
                    if (digits != value) {
                        onValueChange(digits)
                        if (digits.length == length) onComplete?.invoke(digits)
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
                interactionSource = interaction,
                cursorBrush = SolidColor(Color.Transparent),
                textStyle = Tessera.type.body.copy(color = Color.Transparent),
                modifier = Modifier
                    .focusRequester(focus)
                    .semantics {
                        contentDescription = "验证码，共 $length 位"
                        stateDescription = "已输入 ${value.length} 位"
                        if (error != null) error(error)
                    },
                decorationBox = { _ ->
                    Row(horizontalArrangement = Arrangement.spacedBy(TesseraSpace.s2)) {
                        for (i in 0 until length) {
                            val ch = value.getOrNull(i)
                            val active = focused && (i == value.length || (i == length - 1 && value.length == length))
                            val shape = shapeOf(TesseraRadius.md, 14.dp)
                            val pop by animateFloatAsState(if (ch != null) 1f else 0.9f, TesseraMotion.snappy(), label = "pop")
                            Box(
                                Modifier
                                    .widthIn(min = 32.dp, max = 48.dp).width(44.dp).height(56.dp)
                                    .focusGlow(if (active) 1f else 0f, shapeRadius(TesseraRadius.md, 14.dp), if (error != null) c.dangerSubtle else c.focusGlow, if (error != null) c.dangerText else c.focusRing)
                                    .clip(shape)
                                    .background(if (active) c.bgSurface else c.fillDefault)
                                    .then(if (!active) Modifier.sunken() else Modifier)
                                    .then(
                                        when {
                                            error != null -> Modifier.border(1.dp, c.dangerText, shape)
                                            showOutline && !active -> Modifier.border(1.dp, c.borderField, shape)
                                            else -> Modifier
                                        },
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (ch != null) TesseraText(ch.toString(), style = Tessera.type.title2.copy(fontSize = 24.sp), modifier = Modifier.graphicsLayer { scaleX = pop; scaleY = pop })
                                else if (active) Box(Modifier.width(2.dp).height(24.dp).background(c.accentSolid, RoundedCornerShape(1.dp)))
                            }
                        }
                    }
                },
            )
        }
        if (error != null) TesseraText(error, style = Tessera.type.caption, color = c.dangerText)
    }
}

// ------------------------------------------------------------------
// 自动补全 Autocomplete：输入框 + 候选列表；拼音组合中不筛选，选定汉字后才筛选
// ------------------------------------------------------------------

/**
 * 自动补全。候选按包含关系筛选（不区分大小写），匹配部分加粗。
 * 只在输入法选定后筛选：打拼音时候选列表保持不变，不会按拼音字母筛出“没有匹配”。
 */
@Composable
fun TesseraAutocomplete(
    value: String,
    onValueChange: (String) -> Unit,
    options: List<String>,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onSelect: (String) -> Unit = { onValueChange(it) },
    maxItems: Int = 6,
) {
    var query by remember { mutableStateOf(value) }
    var open by remember { mutableStateOf(false) }
    val matches = remember(query, options) {
        if (query.isBlank()) options.take(maxItems) else options.filter { it.contains(query, ignoreCase = true) }.take(maxItems)
    }
    Box(modifier) {
        TesseraTextField(
            value = value,
            onValueChange = { onValueChange(it); open = true },
            modifier = Modifier.fillMaxWidth(),
            label = label,
            placeholder = placeholder,
            leadingIcon = leadingIcon,
            onCommittedValueChange = { query = it; open = true },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { matches.firstOrNull()?.let { onSelect(it); query = it }; open = false }),
        )
        TesseraMenu(expanded = open && value.isNotEmpty(), onDismissRequest = { open = false }) {
            if (matches.isEmpty()) {
                Box(Modifier.padding(horizontal = TesseraSpace.s3, vertical = TesseraSpace.s3)) {
                    TesseraText("没有匹配“$query”的结果", style = Tessera.type.bodySm, color = Tessera.colors.textSecondary)
                }
            } else {
                matches.forEach { m ->
                    TesseraMenuItem(m, onClick = { onSelect(m); query = m; open = false }, selected = m == value)
                }
            }
        }
    }
}

// ------------------------------------------------------------------
// 标签输入 TagInput：已输入的标签是一排小砖，回车、逗号、顿号添加，空框退格删除最后一个
// ------------------------------------------------------------------

/**
 * 标签输入。回车、英文 / 中文逗号、顿号、分号都会把当前文字变成标签（拼音组合中不会）；
 * 输入框为空时按退格，先选中最后一个标签，再按一次删除。重复的标签不会添加。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TesseraTagInput(
    tags: List<String>,
    onTagsChange: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String = "输入后回车添加",
    maxTags: Int = Int.MAX_VALUE,
) {
    val c = Tessera.colors
    var field by remember { mutableStateOf(TextFieldValue("")) }
    var armed by remember { mutableStateOf(false) } // 退格已选中最后一个标签
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val glow by animateFloatAsState(if (focused) 1f else 0f, tween(TesseraMotion.DURATION_FAST), label = "glow")
    val haptic = rememberFlowHaptic()
    val radius = shapeRadius(TesseraRadius.md, TesseraRadius.lg)
    val shape = RoundedCornerShape(radius)
    fun add(raw: String) {
        val t = raw.trim()
        if (t.isNotEmpty() && t !in tags && tags.size < maxTags) { onTagsChange(tags + t); haptic(TesseraHaptic.Light) }
    }
    val separators = charArrayOf(',', '，', '、', ';', '；')
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (label != null) TesseraText(label, style = Tessera.type.label)
        FlowRow(
            Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = TesseraSize.md)
                .focusGlow(glow, radius, c.focusGlow, c.focusRing)
                .clip(shape)
                .background(if (focused) c.bgSurface else c.fillDefault)
                .then(if (!focused) Modifier.sunken() else Modifier)
                .then(if (showOutline) Modifier.border(1.dp, c.borderField, shape) else Modifier)
                .padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            tags.forEachIndexed { i, t ->
                val last = i == tags.lastIndex
                Row(
                    Modifier
                        .height(28.dp)
                        .clip(shapeOf(8.dp))
                        .background(if (armed && last) c.accentSolid else c.accentSubtle)
                        .padding(start = 10.dp, end = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TesseraText(t, style = Tessera.type.caption.copy(fontWeight = FontWeight.SemiBold), color = if (armed && last) c.textOnSolid else c.accentText)
                    Box(
                        Modifier
                            .size(28.dp)
                            .clip(shapeOf(6.dp))
                            .clickable(role = Role.Button) { onTagsChange(tags - t); armed = false }
                            .semantics { contentDescription = "删除标签 $t" },
                        contentAlignment = Alignment.Center,
                    ) { TesseraIcon(TesseraIcons.Close, null, size = 14.dp, tint = if (armed && last) c.textOnSolid else c.accentText) }
                }
            }
            Box(Modifier.defaultMinSize(minWidth = 96.dp).height(28.dp), contentAlignment = Alignment.CenterStart) {
                if (field.text.isEmpty() && tags.isEmpty()) TesseraText(placeholder, style = Tessera.type.bodySm, color = c.textTertiary, maxLines = 1)
                BasicTextField(
                    value = field,
                    onValueChange = { nv ->
                        armed = false
                        // 输入法组合中（拼音还没选定）不切分
                        if (nv.composition == null && nv.text.any { it in separators }) {
                            val parts = nv.text.split(*separators)
                            parts.dropLast(1).forEach { add(it) }
                            field = TextFieldValue(parts.last(), TextRange(parts.last().length))
                        } else field = nv
                    },
                    singleLine = true,
                    textStyle = Tessera.type.bodySm.copy(color = c.textPrimary),
                    cursorBrush = SolidColor(c.accentSolid),
                    interactionSource = interaction,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { if (field.composition == null) { add(field.text); field = TextFieldValue("") } }),
                    modifier = Modifier
                        .widthIn(min = 96.dp)
                        .semantics { contentDescription = label ?: "标签"; stateDescription = if (tags.isEmpty()) "没有标签" else "已有 ${tags.size} 个标签：${tags.joinToString("、")}" }
                        .onPreviewKeyEvent { e ->
                            if (e.type == KeyEventType.KeyDown && e.key == Key.Backspace && field.text.isEmpty() && tags.isNotEmpty()) {
                                if (armed) { onTagsChange(tags.dropLast(1)); armed = false } else armed = true
                                true
                            } else false
                        },
                )
            }
        }
    }
}
