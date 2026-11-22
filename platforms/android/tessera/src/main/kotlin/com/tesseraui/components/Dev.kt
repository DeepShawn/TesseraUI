package com.tesseraui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tesseraui.LocalTesseraFonts
import com.tesseraui.Tessera
import com.tesseraui.TesseraColors
import com.tesseraui.TesseraRadius
import com.tesseraui.TesseraRound
import com.tesseraui.TesseraText
import com.tesseraui.foundation.isRound
import com.tesseraui.foundation.roundRadius
import com.tesseraui.foundation.sunken
import com.tesseraui.icons.TesseraIcons
import kotlinx.coroutines.launch

// ------------------------------------------------------------------
// 语法高亮：轻量分词（注释、字符串、数字、关键字、函数名），与 Web 端规则一致
// ------------------------------------------------------------------

private val keywordSets: Map<String, Set<String>> = run {
    val js = "await async break case catch class const continue default delete do else export extends false finally for from function if import in instanceof let new null of return static super switch this throw true try typeof undefined var void while yield"
    mapOf(
        "js" to js,
        "ts" to "$js interface type enum implements private public readonly declare keyof as",
        "kotlin" to "as break class continue do else false for fun if in interface is null object package return super this throw true try typealias typeof val var when while data sealed private public internal override suspend import",
        "swift" to "as break case catch class continue default defer do else enum extension false for func guard if import in init let nil private protocol public return self static struct switch throw throws true try var while some any async await",
        "csharp" to "abstract as async await base bool break case catch class const continue default do double else enum false finally for foreach if in int interface internal is namespace new null object out override private protected public readonly return sealed static string struct switch this throw true try using var void while",
        "python" to "and as assert async await break class continue def del elif else except False finally for from global if import in is lambda None nonlocal not or pass raise return True try while with yield",
        "json" to "true false null",
    ).mapValues { it.value.split(' ').toSet() }
}
private val langAliases = mapOf("javascript" to "js", "typescript" to "ts", "kt" to "kotlin", "cs" to "csharp", "c#" to "csharp", "py" to "python")
private val cLikeTokens = Regex("""(//[^\n]*|/\*[\s\S]*?(?:\*/|$))|("(?:\\.|[^"\\\n])*"|'(?:\\.|[^'\\\n])*'|`(?:\\.|[^`\\])*`)|(\b\d[\d_]*(?:\.\d+)?(?:[eE][+-]?\d+)?[fFLu]?\b|\b0x[\da-fA-F]+\b)|([A-Za-z_$][\w$]*)(?=\s*\()|([A-Za-z_$][\w$]*)""")
private val hashTokens = Regex("""(#[^\n]*)|(""${'"'}[\s\S]*?""${'"'}|'''[\s\S]*?'''|"(?:\\.|[^"\\\n])*"|'(?:\\.|[^'\\\n])*')|(\b\d[\d_]*(?:\.\d+)?(?:[eE][+-]?\d+)?\b|\b0x[\da-fA-F]+\b)|([A-Za-z_$][\w$]*)(?=\s*\()|([A-Za-z_$][\w$]*)""")

/** 按语言给代码着色（不改变任何字符）；不认识的语言原样返回 */
fun tesseraHighlight(code: String, language: String, colors: TesseraColors): AnnotatedString {
    val lang = langAliases[language.lowercase()] ?: language.lowercase()
    val words = keywordSets[lang] ?: return AnnotatedString(code)
    val re = if (lang == "python") hashTokens else cLikeTokens
    return buildAnnotatedString {
        append(code)
        re.findAll(code).forEach { m ->
            val style = when {
                m.groups[1] != null -> SpanStyle(color = colors.textSecondary, fontStyle = FontStyle.Italic)
                m.groups[2] != null -> SpanStyle(color = colors.pigmentOliveText)
                m.groups[3] != null -> SpanStyle(color = colors.pigmentOchreText)
                m.groups[4] != null -> if (m.value in words) SpanStyle(color = colors.pigmentLapisText, fontWeight = FontWeight.SemiBold) else SpanStyle(color = colors.pigmentAmethystText)
                m.groups[5] != null && m.value in words -> SpanStyle(color = colors.pigmentLapisText, fontWeight = FontWeight.SemiBold)
                else -> null
            }
            if (style != null) addStyle(style, m.range.first, m.range.last + 1)
        }
    }
}

// ------------------------------------------------------------------
// 编辑操作（纯函数，便于测试）：缩进、反缩进、回车自动缩进
// ------------------------------------------------------------------

private fun lineStart(text: String, i: Int): Int = if (i <= 0) 0 else text.lastIndexOf('\n', i - 1) + 1

/** Tab：光标处插入一级缩进；选中多行时整块缩进 */
fun tesseraIndent(v: TextFieldValue, unit: String): TextFieldValue {
    val s = v.selection.min; val e = v.selection.max
    val multi = v.text.substring(s, e).contains('\n')
    if (!multi) {
        val t = v.text.replaceRange(s, e, unit)
        return v.copy(text = t, selection = TextRange(s + unit.length))
    }
    val from = lineStart(v.text, s)
    val to = if (e > s && v.text[e - 1] == '\n') e - 1 else e
    val lines = v.text.substring(from, to).split('\n')
    val block = lines.joinToString("\n") { unit + it }
    return v.copy(text = v.text.replaceRange(from, to, block), selection = TextRange(s + unit.length, from + block.length))
}

/** Shift+Tab：当前行（或选中的每一行）去掉一级缩进 */
fun tesseraOutdent(v: TextFieldValue, unit: String): TextFieldValue {
    val s = v.selection.min; val e = v.selection.max
    val from = lineStart(v.text, s)
    val to = if (e > s && v.text[e - 1] == '\n') e - 1 else e
    val lines = v.text.substring(from, maxOf(to, from)).let { if (to <= from) listOf(v.text.substring(from).substringBefore('\n')) else it.split('\n') }
    val strip = Regex("^ {1,${unit.length}}|^\t")
    val out = lines.map { it.replaceFirst(strip, "") }
    val end = if (to <= from) from + lines[0].length else to
    val block = out.joinToString("\n")
    val firstDelta = lines[0].length - out[0].length
    val ns = (s - firstDelta).coerceAtLeast(from)
    return v.copy(text = v.text.replaceRange(from, end, block), selection = if (e > s) TextRange(ns, from + block.length) else TextRange(ns))
}

/**
 * 回车之后补上缩进：保持上一行的缩进；上一行以 { [ ( : 结尾时多缩一级；
 * 光标正好在一对括号中间时，把右括号放到下一行、与上一行对齐。
 * old 是回车之前的值，new 是输入法 / 键盘给出的新值；不是“插入了一个换行”的情况原样返回。
 */
fun tesseraAutoIndent(old: TextFieldValue, new: TextFieldValue, unit: String): TextFieldValue {
    if (new.composition != null) return new
    val c = new.selection.start
    if (!new.selection.collapsed || new.text.length != old.text.length - (old.selection.max - old.selection.min) + 1) return new
    if (c == 0 || new.text[c - 1] != '\n' || c - 1 != old.selection.min) return new
    val prev = new.text.substring(lineStart(new.text, c - 1), c - 1)
    val base = prev.takeWhile { it == ' ' || it == '\t' }
    val opens = prev.trimEnd().lastOrNull()?.let { it in "{[(:" } == true
    val lead = if (opens) base + unit else base
    val after = new.text.getOrNull(c)
    return if (opens && prev.trimEnd().last() != ':' && after != null && after in "}])") {
        val insert = "$lead\n$base"
        new.copy(text = new.text.replaceRange(c, c, insert), selection = TextRange(c + lead.length))
    } else new.copy(text = new.text.replaceRange(c, c, lead), selection = TextRange(c + lead.length))
}

// ------------------------------------------------------------------
// 代码编辑器 CodeEditor
// ------------------------------------------------------------------

data class TesseraDiagnostic(val line: Int, val message: String)

/**
 * 代码编辑器：凹槽底色的输入框，左侧行号栏；语法高亮、当前行底色、第几行第几列。
 * - 实体键盘：Tab / Shift+Tab 缩进（多行整块），按 Esc 之后再按 Tab 离开编辑器
 * - 回车保持缩进，{ [ ( : 之后多缩一级；输入法组合中不做任何处理
 * - diagnostics：出错的行号变红、整行淡红底，提示列在下方并朗读
 */
@Composable
fun TesseraCodeEditor(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    language: String = "",
    label: String = "代码",
    readOnly: Boolean = false,
    diagnostics: List<TesseraDiagnostic> = emptyList(),
    indent: Int = 2,
    maxHeight: Dp = 360.dp,
) {
    val c = Tessera.colors
    val round = isRound
    val mono = LocalTesseraFonts.current.mono
    val unit = " ".repeat(indent)
    val style = Tessera.type.bodySm.copy(fontFamily = mono, fontSize = 13.sp, lineHeight = 21.sp, fontWeight = FontWeight.Normal, color = c.textPrimary)
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    var exitOnTab by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val errors = diagnostics.associate { it.line to it.message }
    val lineCount = value.text.count { it == '\n' } + 1
    val caretLine = value.text.substring(0, value.selection.start.coerceIn(0, value.text.length)).count { it == '\n' }
    val caretCol = value.selection.start - (value.text.lastIndexOf('\n', (value.selection.start - 1).coerceAtLeast(0)).let { if (value.selection.start == 0) -1 else it })
    val shape = RoundedCornerShape(if (round) roundRadius(TesseraRound.row) else TesseraRadius.md)
    val highlight = remember(language, c) { VisualTransformation { t -> TransformedText(tesseraHighlight(t.text, language, c), OffsetMapping.Identity) } }
    val lineBg = c.textPrimary.copy(alpha = 0.05f)
    val errBg = c.dangerSolid.copy(alpha = 0.12f)
    val subtle = c.borderSubtle
    Column(
        modifier.fillMaxWidth().clip(shape).background(c.fillDefault)
            .then(if (round) Modifier else Modifier.sunken())
            .border(if (focused) 1.5.dp else 1.dp, if (focused) c.focusRing else if (round) Color.Transparent else c.borderField, shape),
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            TesseraText(language, Modifier.weight(1f), style = style.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium), color = c.textSecondary)
            TesseraText("第 ${caretLine + 1} 行，第 $caretCol 列", Modifier.clearAndSetSemantics {}, style = style.copy(fontSize = 12.sp), color = c.textSecondary)
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(if (round) Color.Transparent else subtle))
        Row(Modifier.fillMaxWidth().heightIn(max = maxHeight).verticalScroll(rememberScrollState())) {
            // 行号：一整段文字，行高与代码完全一致
            androidx.compose.foundation.text.BasicText(
                buildAnnotatedString {
                    for (i in 1..lineCount) {
                        val start = length
                        append(i.toString())
                        when {
                            errors.containsKey(i) -> addStyle(SpanStyle(color = c.dangerText, fontWeight = FontWeight.SemiBold), start, length)
                            i == caretLine + 1 && focused -> addStyle(SpanStyle(color = c.textPrimary, fontWeight = FontWeight.SemiBold), start, length)
                        }
                        if (i < lineCount) append('\n')
                    }
                },
                Modifier.clearAndSetSemantics {}.padding(start = 12.dp, end = 10.dp, top = 12.dp, bottom = 12.dp).width(26.dp),
                style = style.copy(color = c.textSecondary, textAlign = TextAlign.End),
            )
            BoxWithConstraints(Modifier.weight(1f)) {
                val viewport = maxWidth
                Box(Modifier.horizontalScroll(rememberScrollState())) {
                    BasicTextField(
                        value = value,
                        onValueChange = { nv -> onValueChange(tesseraAutoIndent(value, nv, unit)); exitOnTab = false },
                        modifier = Modifier
                            .defaultMinSize(minWidth = viewport)
                            .drawBehind {
                                val l = layout ?: return@drawBehind
                                val pad = 12.dp.toPx()
                                fun bar(line: Int, color: Color) {
                                    if (line < 0 || line >= l.lineCount) return
                                    val top = l.getLineTop(line) + pad
                                    drawRect(color, Offset(0f, top), Size(size.width, l.getLineBottom(line) - l.getLineTop(line)))
                                }
                                errors.keys.forEach { bar(it - 1, errBg) }
                                if (focused) bar(caretLine, lineBg)
                            }
                            .onPreviewKeyEvent { e ->
                                if (readOnly || e.type != KeyEventType.KeyDown || value.composition != null) return@onPreviewKeyEvent false
                                when (e.key) {
                                    Key.Escape -> { exitOnTab = true; false }
                                    Key.Tab -> {
                                        // Esc 之后：文本框自己会把 Tab 当字符插入，所以这里主动把焦点移到下一个（Shift 时上一个）控件
                                        if (exitOnTab) { exitOnTab = false; focusManager.moveFocus(if (e.isShiftPressed) FocusDirection.Previous else FocusDirection.Next); true } else {
                                            onValueChange(if (e.isShiftPressed) tesseraOutdent(value, unit) else tesseraIndent(value, unit)); true
                                        }
                                    }
                                    else -> false
                                }
                            }
                            .semantics {
                                contentDescription = label
                                if (diagnostics.isNotEmpty()) error(diagnostics.joinToString("；") { "第 ${it.line} 行：${it.message}" })
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        readOnly = readOnly,
                        textStyle = style,
                        visualTransformation = highlight,
                        onTextLayout = { layout = it },
                        interactionSource = interaction,
                        cursorBrush = SolidColor(c.textPrimary),
                    )
                }
            }
        }
        if (!readOnly) {
            Box(Modifier.fillMaxWidth().height(1.dp).background(if (round) Color.Transparent else subtle))
            TesseraText("实体键盘：Tab 缩进；按 Esc 后再按 Tab 可离开编辑器", Modifier.padding(horizontal = 16.dp, vertical = 6.dp), style = Tessera.type.caption, color = c.textSecondary)
        }
        if (diagnostics.isNotEmpty()) Column(Modifier.fillMaxWidth().background(c.dangerSubtle).semantics { liveRegion = LiveRegionMode.Polite }) {
            diagnostics.forEach { d ->
                Row(Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}.padding(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TesseraText("第 ${d.line} 行", style = Tessera.type.bodySm.copy(fontWeight = FontWeight.SemiBold), color = c.textPrimary)
                    TesseraText(d.message, Modifier.weight(1f), style = Tessera.type.bodySm, color = c.textPrimary)
                }
            }
        }
    }
}

// ------------------------------------------------------------------
// 十六进制查看 HexView
// ------------------------------------------------------------------

/** 一段着色的字节：[from, to)；tone 用颜料色区分 */
data class TesseraHexRange(val from: Int, val to: Int, val label: String, val tone: TesseraTone = TesseraTone.Accent)

private fun hex2(b: Int) = "%02X".format(b and 0xFF)
private fun printable(b: Int) = if (b in 0x20..0x7E) b.toChar().toString() else "·"

/** 状态栏的说明文字（也供测试与读屏使用） */
fun tesseraHexDescribe(data: ByteArray, from: Int, to: Int, focus: Int, ranges: List<TesseraHexRange> = emptyList()): String {
    if (data.isEmpty()) return "没有数据"
    val b = data[focus].toInt() and 0xFF
    val ch = if (b in 0x20..0x7E) "，字符“${b.toChar()}”" else ""
    val r = ranges.firstOrNull { focus >= it.from && focus < it.to }?.let { "，${it.label}" } ?: ""
    val sel = if (to > from) "，已选 ${to - from + 1} 个字节（$from–$to）" else ""
    return "偏移 0x${focus.toString(16).uppercase()}（$focus）：0x${hex2(b)} = $b = 0b${b.toString(2).padStart(8, '0')}$ch$r$sel"
}

/**
 * 十六进制查看：偏移 / 16 个字节（窄于 560dp 时 8 个）/ ASCII 三栏，按行懒加载。
 * - 点按选中一个字节（十六进制与 ASCII 两栏同步），长按另一个字节扩展选区；实体键盘方向键移动、Shift 扩展
 * - 状态栏读出偏移、十六进制 / 十进制 / 二进制值和字符；“复制”按钮把选中的字节复制为十六进制
 * - 读屏按行朗读：每一行是一项（偏移、字节、文本）
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun TesseraHexView(
    data: ByteArray,
    modifier: Modifier = Modifier,
    highlights: List<TesseraHexRange> = emptyList(),
    columns: Int? = null,
    height: Dp = 312.dp,
    label: String = "十六进制数据",
    initialSelection: IntRange? = null,
    onSelect: (IntRange) -> Unit = {},
) {
    val c = Tessera.colors
    val round = isRound
    val mono = LocalTesseraFonts.current.mono
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    var anchor by remember { mutableIntStateOf(initialSelection?.first ?: 0) }
    var focus by remember { mutableIntStateOf(initialSelection?.last ?: 0) }
    var copied by remember { mutableStateOf(false) }
    val list = rememberLazyListState()
    val shape = RoundedCornerShape(if (round) roundRadius(TesseraRound.row) else TesseraRadius.md)
    val cell = Tessera.type.bodySm.copy(fontFamily = mono, fontSize = 13.sp, lineHeight = 26.sp, fontWeight = FontWeight.Normal)
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val cols = columns ?: if (maxWidth < 560.dp) 8 else 16
        val rows = (data.size + cols - 1) / cols
        val lo = minOf(anchor, focus); val hi = maxOf(anchor, focus)
        fun select(i: Int, extend: Boolean) {
            if (data.isEmpty()) return
            focus = i.coerceIn(0, data.size - 1)
            if (!extend) anchor = focus
            copied = false
            scope.launch {
                val row = focus / cols
                val visible = list.layoutInfo.visibleItemsInfo
                if (visible.none { it.index == row } || visible.first().index == row && visible.first().offset < 0 || visible.last().index == row && visible.last().offset + visible.last().size > list.layoutInfo.viewportEndOffset) list.scrollToItem(row)
            }
            onSelect(minOf(anchor, focus)..maxOf(anchor, focus))
        }
        val toneBg = highlights.map { toneColors(it.tone).first }
        fun toneOf(i: Int): Color? = highlights.indexOfFirst { i >= it.from && i < it.to }.takeIf { it >= 0 }?.let { toneBg[it] }
        Column(Modifier.fillMaxWidth().clip(shape).background(c.fillDefault).then(if (round) Modifier else Modifier.sunken())) {
            val hScroll = rememberScrollState()
            Column(Modifier.fillMaxWidth().horizontalScroll(hScroll)) {
                // 表头
                Row(Modifier.padding(horizontal = 12.dp, vertical = 4.dp).clearAndSetSemantics {}, verticalAlignment = Alignment.CenterVertically) {
                    TesseraText("偏移", Modifier.width(64.dp), style = cell.copy(fontSize = 12.sp), color = c.textSecondary)
                    for (k in 0 until cols) {
                        if (k == cols / 2) Box(Modifier.width(6.dp))
                        TesseraText(hex2(k), Modifier.width(21.dp), style = cell.copy(fontSize = 12.sp), color = c.textSecondary, textAlign = TextAlign.Center)
                    }
                    TesseraText("ASCII", Modifier.padding(start = 10.dp), style = cell.copy(fontSize = 12.sp), color = c.textSecondary)
                }
                if (!round) Box(Modifier.width(64.dp + 21.dp * cols + 6.dp + 10.dp + 9.dp * cols + 40.dp).height(1.dp).background(c.borderSubtle))
                LazyColumn(
                    Modifier.height(height).width(64.dp + 21.dp * cols + 6.dp + 10.dp + 9.dp * cols + 40.dp)
                        .semantics { contentDescription = label }
                        .onKeyEvent { e ->
                            if (e.type != KeyEventType.KeyDown) return@onKeyEvent false
                            val page = cols * 10
                            val target = when (e.key) {
                                Key.DirectionRight -> focus + 1
                                Key.DirectionLeft -> focus - 1
                                Key.DirectionDown -> focus + cols
                                Key.DirectionUp -> focus - cols
                                Key.PageDown -> focus + page
                                Key.PageUp -> focus - page
                                Key.MoveHome -> if (e.isCtrlPressed || e.isMetaPressed) 0 else focus - focus % cols
                                Key.MoveEnd -> if (e.isCtrlPressed || e.isMetaPressed) data.size - 1 else minOf(data.size - 1, focus - focus % cols + cols - 1)
                                else -> null
                            } ?: return@onKeyEvent false
                            select(target, e.isShiftPressed); true
                        }
                        .focusable(),
                    state = list,
                ) {
                    items(rows) { r ->
                        val off = r * cols
                        val end = minOf(data.size, off + cols)
                        val spoken = "偏移 ${"%08X".format(off)}：" + (off until end).joinToString(" ") { hex2(data[it].toInt()) } +
                            "；文本 " + (off until end).joinToString("") { printable(data[it].toInt()) }
                        Row(
                            Modifier.height(26.dp).padding(horizontal = 12.dp).clearAndSetSemantics { contentDescription = spoken },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            TesseraText("%08X".format(off), Modifier.width(64.dp), style = cell, color = c.textSecondary)
                            for (k in 0 until cols) {
                                val i = off + k
                                if (k == cols / 2) Box(Modifier.width(6.dp))
                                if (i >= data.size) { Box(Modifier.width(21.dp)); continue }
                                ByteCell(hex2(data[i].toInt()), i, i == focus, i in lo..hi, toneOf(i), Modifier.width(21.dp), cell) { ext -> select(i, ext) }
                            }
                            Box(Modifier.width(10.dp))
                            for (k in 0 until cols) {
                                val i = off + k
                                if (i >= data.size) break
                                ByteCell(printable(data[i].toInt()), i, i == focus, i in lo..hi, toneOf(i), Modifier.width(9.dp), cell.copy(color = c.textSecondary, fontSize = 11.sp)) { ext -> select(i, ext) }
                            }
                        }
                    }
                }
            }
            if (!round) Box(Modifier.fillMaxWidth().height(1.dp).background(c.borderSubtle))
            Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                TesseraText(
                    if (copied) "已复制 ${hi - lo + 1} 个字节" else tesseraHexDescribe(data, lo, hi, focus, highlights),
                    Modifier.weight(1f).padding(vertical = 6.dp).semantics { liveRegion = LiveRegionMode.Polite },
                    style = Tessera.type.caption.copy(fontFamily = mono), color = c.textSecondary,
                )
                TesseraIconButton(TesseraIcons.Copy, "复制选中的字节", {
                    if (data.isNotEmpty()) {
                        clipboard.setText(AnnotatedString((lo..hi).joinToString(" ") { hex2(data[it].toInt()) }))
                        copied = true
                    }
                }, size = TesseraButtonSize.Sm)
            }
            if (highlights.isNotEmpty()) androidx.compose.foundation.layout.FlowRow(
                Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                highlights.forEach { h ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(toneColors(h.tone).second))
                        TesseraText("${h.label} ${h.from}–${h.to - 1}", style = Tessera.type.caption, color = c.textSecondary)
                    }
                }
            }
        }
        LaunchedEffect(cols) { list.scrollToItem((focus / cols).coerceAtLeast(0)) }
    }
}

@Composable
private fun ByteCell(text: String, index: Int, isFocus: Boolean, selected: Boolean, tone: Color?, modifier: Modifier, style: androidx.compose.ui.text.TextStyle, onTap: (Boolean) -> Unit) {
    val c = Tessera.colors
    val bg = when { isFocus -> c.accentSolid; selected -> c.textPrimary; tone != null -> tone; else -> Color.Transparent }
    val fg = when { isFocus -> c.textOnSolid; selected -> c.bgSurface; else -> style.color.takeIf { it != Color.Unspecified } ?: c.textPrimary }
    Box(
        modifier.height(22.dp).clip(RoundedCornerShape(if (isRound) 6.dp else 4.dp)).background(bg)
            // 只响应手势，不作为读屏的独立按钮（读屏按行朗读，避免几千个小目标）
            .pointerInput(index) { detectTapGestures(onTap = { onTap(false) }, onLongPress = { onTap(true) }) },
        contentAlignment = Alignment.Center,
    ) { TesseraText(text, style = style, color = fg, maxLines = 1) }
}
