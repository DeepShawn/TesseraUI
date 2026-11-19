package com.tesseraui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.tesseraui.LocalReduceMotion
import com.tesseraui.LocalTesseraFonts
import com.tesseraui.Tessera
import com.tesseraui.TesseraMotion
import com.tesseraui.TesseraRadius
import com.tesseraui.TesseraRound
import com.tesseraui.TesseraSize
import com.tesseraui.TesseraSpace
import com.tesseraui.TesseraText
import com.tesseraui.TesseraTile
import com.tesseraui.foundation.isRound
import com.tesseraui.foundation.roundRadius
import com.tesseraui.foundation.shapeOf
import com.tesseraui.foundation.sunken
import com.tesseraui.icons.TesseraIcon
import com.tesseraui.icons.TesseraIcons
import com.tesseraui.motion
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max

// ------------------------------------------------------------------
// 轮播 Carousel：系统翻页手势（跟手 + 惯性吸附）；圆点变长表示当前页，前后按钮可点
// ------------------------------------------------------------------

/**
 * 轮播。
 * @param peek 露出左右两张的一部分
 * @param autoplayMillis 大于 0 时自动播放；用户拖动后暂停 5 秒；系统开启“减弱动态效果”时不自动播放
 */
@Composable
fun TesseraCarousel(
    count: Int,
    modifier: Modifier = Modifier,
    height: Dp = 200.dp,
    peek: Boolean = false,
    autoplayMillis: Long = 0,
    initialPage: Int = 0,
    page: @Composable (Int) -> Unit,
) {
    val c = Tessera.colors
    val state = rememberPagerState(initialPage) { count }
    val scope = rememberCoroutineScope()
    val reduce = LocalReduceMotion.current
    val dragged by state.interactionSource.collectIsDraggedAsState()
    var lastTouch by remember { mutableLongStateOf(0L) }
    LaunchedEffect(dragged) { if (dragged) lastTouch = System.currentTimeMillis() }
    if (autoplayMillis > 0 && !reduce) LaunchedEffect(state, autoplayMillis) {
        while (true) {
            delay(autoplayMillis)
            if (dragged || System.currentTimeMillis() - lastTouch < 5000) continue
            state.animateScrollToPage(if (state.currentPage == count - 1) 0 else state.currentPage + 1)
        }
    }
    val go: (Int) -> Unit = { i ->
        lastTouch = System.currentTimeMillis()
        scope.launch { if (reduce) state.scrollToPage(i.coerceIn(0, count - 1)) else state.animateScrollToPage(i.coerceIn(0, count - 1)) }
    }
    val shape = RoundedCornerShape(if (isRound) roundRadius(TesseraRound.card) else TesseraRadius.xl)
    Column(modifier.fillMaxWidth().semantics { contentDescription = "轮播" }) {
        Box(Modifier.fillMaxWidth().height(height)) {
            HorizontalPager(
                state,
                Modifier.fillMaxSize(),
                contentPadding = if (peek) PaddingValues(horizontal = 32.dp) else PaddingValues(0.dp),
                pageSpacing = TesseraSpace.s3,
            ) { i ->
                Box(Modifier.fillMaxSize().clip(shape).semantics(mergeDescendants = true) { contentDescription = "第 ${i + 1} 张，共 $count 张" }) { page(i) }
            }
            CarouselNav(TesseraIcons.ChevronLeft, "上一张", Modifier.align(Alignment.CenterStart).padding(start = 12.dp), state.currentPage > 0) { go(state.currentPage - 1) }
            CarouselNav(TesseraIcons.ChevronRight, "下一张", Modifier.align(Alignment.CenterEnd).padding(end = 12.dp), state.currentPage < count - 1) { go(state.currentPage + 1) }
        }
        // 圆点只是视觉指示（读屏通过页面名称与前后按钮操作），点按也能跳页
        Row(
            Modifier.fillMaxWidth().padding(top = TesseraSpace.s1).clearAndSetSemantics {},
            horizontalArrangement = Arrangement.Center,
        ) {
            repeat(count) { i ->
                val on = i == state.currentPage
                val w by animateDpAsState(if (on) 22.dp else 8.dp, motion(TesseraMotion.snappy()), label = "dot")
                Box(Modifier.height(24.dp).clickable(indication = null, interactionSource = null) { go(i) }.padding(horizontal = 3.dp), contentAlignment = Alignment.Center) {
                    Box(Modifier.width(w).height(8.dp).clip(if (isRound) RoundedCornerShape(50) else RoundedCornerShape(3.dp)).background(if (on) c.textPrimary else c.fillControlOff))
                }
            }
        }
    }
}

@Composable
private fun CarouselNav(icon: ImageVector, label: String, modifier: Modifier, enabled: Boolean, onClick: () -> Unit) {
    val c = Tessera.colors
    val shape = shapeOf(TesseraRadius.md)
    val a by animateFloatAsState(if (enabled) 1f else 0f, label = "nav")
    if (a == 0f && !enabled) return
    Box(
        modifier.graphicsLayer { alpha = a }.size(TesseraSize.touchMin)
            .shadow(4.dp, shape, ambientColor = c.shadowColor, spotColor = c.shadowColor)
            .clip(shape).background(c.bgElevated.copy(alpha = 0.88f))
            .clickable(enabled = enabled, role = Role.Button, onClickLabel = label, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) { TesseraIcon(icon, null, tint = c.textPrimary, size = 20.dp) }
}

// ------------------------------------------------------------------
// 图片预览 ImageViewer：全屏查看；双指 / 双击缩放，放大后拖动平移；
// 未放大时下滑关闭（背景随之变淡）、左右滑切换
// ------------------------------------------------------------------

/** 全屏图片预览（弹出窗口） */
@Composable
fun TesseraImageViewer(
    images: List<Painter>,
    onDismissRequest: () -> Unit,
    startIndex: Int = 0,
    descriptions: List<String> = emptyList(),
) {
    Dialog(onDismissRequest, DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        TesseraImageViewerContent(images, onDismissRequest, startIndex, descriptions)
    }
}

/** 图片预览的内容（不含窗口），截图测试也用它 */
@Composable
fun TesseraImageViewerContent(
    images: List<Painter>,
    onDismissRequest: () -> Unit,
    startIndex: Int = 0,
    descriptions: List<String> = emptyList(),
    modifier: Modifier = Modifier,
) {
    var index by remember { mutableIntStateOf(startIndex.coerceIn(0, images.size - 1)) }
    val scope = rememberCoroutineScope()
    val reduce = LocalReduceMotion.current
    val s = remember { Animatable(1f) }
    val x = remember { Animatable(0f) }
    val y = remember { Animatable(0f) }
    var box by remember { mutableStateOf(IntSize.Zero) }
    val spec = motion(TesseraMotion.smooth<Float>())
    fun limits(scale: Float): Pair<Float, Float> = Pair(max(0f, box.width * (scale - 1) / 2), max(0f, box.height * (scale - 1) / 2))
    fun zoomTo(target: Float, cx: Float = 0f, cy: Float = 0f) {
        val ns = target.coerceIn(1f, 5f)
        var nx = cx - (cx - x.value) * (ns / s.value)
        var ny = cy - (cy - y.value) * (ns / s.value)
        if (ns == 1f) { nx = 0f; ny = 0f }
        val (lx, ly) = limits(ns)
        nx = nx.coerceIn(-lx, lx); ny = ny.coerceIn(-ly, ly)
        scope.launch { if (reduce) s.snapTo(ns) else s.animateTo(ns, spec) }
        scope.launch { if (reduce) x.snapTo(nx) else x.animateTo(nx, spec) }
        scope.launch { if (reduce) y.snapTo(ny) else y.animateTo(ny, spec) }
    }
    fun show(i: Int) {
        index = (i + images.size) % images.size
        scope.launch { s.snapTo(1f); x.snapTo(0f); y.snapTo(0f) }
    }
    val fade = (1f - abs(y.value) / 1200f).coerceIn(0.3f, 1f) * (if (s.value < 1f) s.value else 1f)
    val label = descriptions.getOrNull(index)
    Box(
        modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.92f * fade)).semantics { paneTitle = "图片预览" }
            .onSizeChanged { box = it },
        contentAlignment = Alignment.Center,
    ) {
        Image(
            images[index], label,
            Modifier.fillMaxWidth(0.92f).fillMaxSize(0.86f)
                .graphicsLayer { scaleX = s.value; scaleY = s.value; translationX = x.value; translationY = y.value }
                .clip(RoundedCornerShape(if (isRound) roundRadius(TesseraRound.row) else TesseraRadius.md))
                .pointerInput(index) {
                    detectTapGestures(onDoubleTap = { p ->
                        // 双击：以点按处为中心放大到 2.5 倍；再双击还原
                        zoomTo(if (s.value > 1.01f) 1f else 2.5f, p.x - size.width / 2f, p.y - size.height / 2f)
                    })
                }
                .pointerInput(index) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        var dx = 0f; var dy = 0f; var multi = false
                        do {
                            val e = awaitPointerEvent()
                            val zoom = e.calculateZoom()
                            val pan = e.calculatePan()
                            if (e.changes.size > 1) multi = true
                            if (multi || s.value > 1.01f) {
                                val ns = (s.value * zoom).coerceIn(0.6f, 6f)
                                val c0 = e.calculateCentroid() - Offset(size.width / 2f, size.height / 2f)
                                val nx = c0.x - (c0.x - x.value) * (ns / s.value) + pan.x
                                val ny = c0.y - (c0.y - y.value) * (ns / s.value) + pan.y
                                scope.launch { s.snapTo(ns); x.snapTo(nx); y.snapTo(ny) }
                            } else {
                                dx += pan.x; dy += pan.y
                                // 未放大：竖向跟手（用于关闭），横向带阻尼（用于切换）
                                scope.launch { x.snapTo(dx * 0.4f); y.snapTo(dy) }
                            }
                            e.changes.forEach { if (it.positionChanged()) it.consume() }
                        } while (e.changes.any { it.pressed })
                        // 松手后：放大状态下缩放收回到 1～5 倍、位置收回到图片边界内；未放大时判断关闭或切换
                        if (multi || s.value > 1.01f) zoomTo(s.value, 0f, 0f) else when {
                            abs(dy) > 120.dp.toPx() -> onDismissRequest()
                            abs(dx) > 80.dp.toPx() && images.size > 1 -> show(if (dx < 0) index + 1 else index - 1)
                            else -> { scope.launch { x.animateTo(0f, spec) }; scope.launch { y.animateTo(0f, spec) } }
                        }
                    }
                },
            contentScale = ContentScale.Fit,
        )
        // 顶栏：计数 + 关闭
        Row(Modifier.align(Alignment.TopCenter).fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            TesseraText(
                if (images.size > 1) "${index + 1} / ${images.size}" else label ?: "",
                Modifier.weight(1f).semantics { liveRegion = LiveRegionMode.Polite },
                style = Tessera.type.label, color = Color.White,
            )
            ViewerButton(TesseraIcons.Close, "关闭", shapeOf(TesseraRadius.md)) { onDismissRequest() }
        }
        // 底部工具：上一张 / 缩小 / 放大 / 下一张，一组连在一起的砖
        val tools = buildList<Triple<ImageVector, String, () -> Unit>> {
            if (images.size > 1) add(Triple(TesseraIcons.ChevronLeft, "上一张") { show(index - 1) })
            add(Triple(TesseraIcons.ZoomOut, "缩小") { zoomTo(s.value / 1.5f) })
            add(Triple(TesseraIcons.ZoomIn, "放大") { zoomTo(s.value * 1.5f) })
            if (images.size > 1) add(Triple(TesseraIcons.ChevronRight, "下一张") { show(index + 1) })
        }
        val round = isRound
        Row(
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 16.dp)
                .then(if (round) Modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.12f)).padding(4.dp) else Modifier),
            horizontalArrangement = Arrangement.spacedBy(if (round) 0.dp else TesseraTile.grout),
        ) {
            tools.forEachIndexed { i, (ic, name, fn) ->
                val shape: Shape = when {
                    round -> CircleShape
                    i == 0 -> RoundedCornerShape(14.dp, 6.dp, 6.dp, 14.dp)
                    i == tools.size - 1 -> RoundedCornerShape(6.dp, 14.dp, 14.dp, 6.dp)
                    else -> RoundedCornerShape(6.dp)
                }
                ViewerButton(ic, name, shape, filled = !round, onClick = fn)
            }
        }
    }
}

@Composable
private fun ViewerButton(icon: ImageVector, label: String, shape: Shape, filled: Boolean = true, onClick: () -> Unit) {
    Box(
        Modifier.size(TesseraSize.touchMin).clip(shape).background(if (filled) Color.White.copy(alpha = 0.12f) else Color.Transparent)
            .clickable(role = Role.Button, onClick = onClick).semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) { TesseraIcon(icon, null, tint = Color.White, size = 20.dp) }
}

// ------------------------------------------------------------------
// 代码块 CodeBlock：凹槽底色，等宽字体，长行在块内横向滚动；复制后图标变成勾
// ------------------------------------------------------------------

@Composable
fun TesseraCodeBlock(
    code: String,
    modifier: Modifier = Modifier,
    language: String? = null,
    lineNumbers: Boolean = false,
) {
    val c = Tessera.colors
    val round = isRound
    val mono = LocalTesseraFonts.current.mono
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) { if (copied) { delay(1600); copied = false } }
    val shape = RoundedCornerShape(if (round) roundRadius(TesseraRound.row) else TesseraRadius.md)
    val lines = code.trimEnd('\n').split('\n')
    val codeStyle = Tessera.type.bodySm.copy(fontFamily = mono, fontSize = 13.sp, lineHeight = 21.sp, fontWeight = FontWeight.Normal)
    val subtle = c.borderSubtle
    Column(modifier.fillMaxWidth().clip(shape).background(c.fillDefault).sunken()) {
        Row(
            Modifier.fillMaxWidth().drawBehind {
                if (!round) drawRect(subtle, topLeft = Offset(0f, size.height - 1.dp.toPx()), size = androidx.compose.ui.geometry.Size(size.width, 1.dp.toPx()))
            }.padding(start = if (round) 18.dp else 16.dp, end = 0.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TesseraText(language ?: "", Modifier.weight(1f), style = codeStyle.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium), color = c.textSecondary)
            Box(
                Modifier.size(TesseraSize.touchMin).clickable(role = Role.Button) { clipboard.setText(AnnotatedString(lines.joinToString("\n"))); copied = true }
                    .semantics { contentDescription = if (copied) "已复制" else "复制代码"; liveRegion = LiveRegionMode.Polite },
                contentAlignment = Alignment.Center,
            ) { TesseraIcon(if (copied) TesseraIcons.Check else TesseraIcons.Copy, null, tint = if (copied) c.successText else c.textSecondary, size = 18.dp) }
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = if (round) 18.dp else 16.dp, vertical = 14.dp)) {
            // 行号与代码用同一种文字排版（一整段），行高才完全一致
            if (lineNumbers) TesseraText(
                lines.indices.joinToString("\n") { "${it + 1}" },
                Modifier.padding(end = 12.dp).width(26.dp).clearAndSetSemantics {},
                style = codeStyle, color = c.textTertiary, textAlign = TextAlign.End, maxLines = Int.MAX_VALUE,
            )
            TesseraText(lines.joinToString("\n"), style = codeStyle, color = c.textPrimary, maxLines = Int.MAX_VALUE)
        }
    }
}
