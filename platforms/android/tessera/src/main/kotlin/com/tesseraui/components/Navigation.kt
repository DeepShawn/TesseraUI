package com.tesseraui.components

import androidx.compose.ui.graphics.graphicsLayer
import com.tesseraui.foundation.rememberBeadPop
import com.tesseraui.foundation.TesseraHaptic
import com.tesseraui.foundation.rememberFlowHaptic
import com.tesseraui.foundation.roundRadius
import androidx.compose.ui.draw.shadow
import com.tesseraui.TesseraRound
import com.tesseraui.foundation.groupGap
import com.tesseraui.foundation.isRound
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.vector.ImageVector
import com.tesseraui.LocalContentColor
import com.tesseraui.Tessera
import com.tesseraui.TesseraMotion
import com.tesseraui.TesseraRadius
import com.tesseraui.TesseraSpace
import com.tesseraui.TesseraText
import com.tesseraui.TesseraTile
import com.tesseraui.icons.TesseraIcon
import com.tesseraui.motion
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// ------------------------------------------------------------------
// 顶部导航栏 NavBar：不透明的页面底色 + 底部一条细线；标题是软衬线
// ------------------------------------------------------------------

@Composable
fun TesseraNavBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
    largeTitle: Boolean = false,
    applyStatusBarInsets: Boolean = true,
) {
    val c = Tessera.colors
    val line = c.borderSubtle
    val round = isRound
    Column(
        modifier
            .fillMaxWidth()
            .background(c.bgCanvas)
            .then(if (applyStatusBarInsets) Modifier.windowInsetsPadding(WindowInsets.statusBars) else Modifier)
            // 圆融外观没有底部分割线
            .drawBehind { if (!round) drawLine(line, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx()) },
    ) {
        Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(56.dp)) { navigationIcon?.invoke() }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                if (!largeTitle) TesseraText(title, Modifier.semantics { heading() }, style = Tessera.type.title3, maxLines = 1)
            }
            Row(Modifier.width(56.dp * 2), horizontalArrangement = Arrangement.End) { actions?.invoke(this) }
        }
        if (largeTitle) TesseraText(title, Modifier.padding(start = 20.dp, bottom = 12.dp).semantics { heading() }, style = Tessera.type.title1)
    }
}

// ------------------------------------------------------------------
// 标签页 Tabs：指示条是一条细砖；切换时前沿 snappy、后沿 smooth
// ------------------------------------------------------------------

@Composable
fun TesseraTabs(titles: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val c = Tessera.colors
    val density = LocalDensity.current
    val xs = remember(titles.size) { mutableStateListOf(*Array(titles.size) { 0f }) }
    val ws = remember(titles.size) { mutableStateListOf(*Array(titles.size) { 0f }) }
    val left = remember { Animatable(0f) }
    val right = remember { Animatable(0f) }
    val snappy = motion(TesseraMotion.snappy<Float>()); val smooth = motion(TesseraMotion.smooth<Float>())
    val measured = ws.getOrNull(selectedIndex)?.let { it > 0f } == true
    LaunchedEffect(selectedIndex, measured) {
        if (!measured) return@LaunchedEffect
        val l = xs[selectedIndex]; val r = l + ws[selectedIndex]
        if (right.value == 0f) { left.snapTo(l); right.snapTo(r); return@LaunchedEffect }
        val goingRight = l > left.value
        launch { left.animateTo(l, if (goingRight) smooth else snappy) }
        launch { right.animateTo(r, if (goingRight) snappy else smooth) }
    }
    val line = c.borderSubtle
    val round = isRound
    // 圆融外观：没有底线，指示器是一颗圆头短条
    Box(modifier.fillMaxWidth().drawBehind { if (!round) drawLine(line, Offset(0f, size.height - 0.5f), Offset(size.width, size.height - 0.5f), 1.dp.toPx()) }) {
        Row(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(TesseraSpace.s6)) {
            titles.forEachIndexed { i, t ->
                val selected = i == selectedIndex
                val fg by animateColorAsState(if (selected) c.textPrimary else c.textSecondary, tween(TesseraMotion.DURATION_NORMAL), label = "tab")
                Box(
                    Modifier
                        .onGloballyPositioned { xs[i] = it.positionInParent().x; ws[i] = it.size.width.toFloat() }
                        .selectable(selected, role = Role.Tab) { onSelect(i) }
                        .padding(top = 12.dp, bottom = 14.dp),
                ) { TesseraText(t, style = Tessera.type.label, color = fg, maxLines = 1) }
            }
        }
        // 首帧还没开始动画时，直接用测量到的位置
        val first = right.value == 0f && measured
        val l = if (first) xs[selectedIndex] else left.value
        val r = if (first) xs[selectedIndex] + ws[selectedIndex] else right.value
        val w = with(density) { (r - l).coerceAtLeast(0f).toDp() }
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .offset { IntOffset(l.roundToInt(), 0) }
                .width(w)
                .height(if (round) 4.dp else 3.dp)
                .clip(if (round) RoundedCornerShape(TesseraRound.pill) else RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                .background(c.accentSolid),
        )
    }
}

// ------------------------------------------------------------------
// 底部标签栏 TabBar：一排砖，选中的那块上釉色，图标从线性换成实心
// ------------------------------------------------------------------

@Immutable
data class TesseraTabBarItem(val label: String, val icon: ImageVector, val selectedIcon: ImageVector = icon, val badge: Int? = null)

@Composable
fun TesseraTabBar(items: List<TesseraTabBarItem>, selectedIndex: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier, applyNavigationBarInsets: Boolean = true) {
    val c = Tessera.colors
    val line = c.borderSubtle
    val round = isRound
    val pill = RoundedCornerShape(roundRadius(TesseraRound.pill))
    val haptic = rememberFlowHaptic()
    // 拼贴：一排砖，顶部一条细线；圆融：浮起的一颗胶囊，没有线
    val bar = if (round) {
        modifier
            .fillMaxWidth()
            .background(c.bgCanvas)
            .then(if (applyNavigationBarInsets) Modifier.windowInsetsPadding(WindowInsets.navigationBars) else Modifier)
            .padding(start = TesseraSpace.s3, end = TesseraSpace.s3, bottom = TesseraSpace.s3)
            .shadow(10.dp, pill, ambientColor = c.shadowColor, spotColor = c.shadowColor)
            .clip(pill)
            .background(c.bgElevated)
            .padding(6.dp)
            .selectableGroup()
    } else {
        modifier
            .fillMaxWidth()
            .background(c.bgCanvas)
            .drawBehind { drawLine(line, Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx()) }
            .then(if (applyNavigationBarInsets) Modifier.windowInsetsPadding(WindowInsets.navigationBars) else Modifier)
            .padding(horizontal = TesseraSpace.s3, vertical = TesseraSpace.s2)
            .selectableGroup()
    }
    Row(
        bar,
        horizontalArrangement = Arrangement.spacedBy(groupGap),
    ) {
        items.forEachIndexed { i, item ->
            val selected = i == selectedIndex
            val bg by animateColorAsState(if (selected) c.accentSubtle else c.bgCanvas.copy(alpha = 0f), tween(TesseraMotion.DURATION_NORMAL), label = "bg")
            val fg by animateColorAsState(if (selected) c.accentText else c.textSecondary, tween(TesseraMotion.DURATION_NORMAL), label = "fg")
            Column(
                Modifier
                    .weight(1f)
                    .height(if (round) 50.dp else 52.dp)
                    .clip(if (round) pill else RoundedCornerShape(TesseraRadius.md))
                    .background(bg)
                    .selectable(selected, role = Role.Tab) { if (!selected) haptic(TesseraHaptic.Selection); onSelect(i) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CompositionLocalProvider(LocalContentColor provides fg) {
                    // 流韵：选中时图标像水珠一样弹一下
                    val pop = rememberBeadPop(selected)
                    Box(Modifier.graphicsLayer { val p = if (selected) pop() else 1f; scaleX = p; scaleY = p }) {
                        TesseraIcon(if (selected) item.selectedIcon else item.icon, null, size = 22.dp)
                        if (item.badge != null) Box(Modifier.align(Alignment.TopEnd).offset(10.dp, (-4).dp)) { TesseraBadge(item.badge) }
                    }
                    TesseraText(item.label, style = Tessera.type.caption.copy(fontSize = 10.sp, lineHeight = 14.sp, fontWeight = FontWeight.SemiBold), maxLines = 1)
                }
            }
        }
    }
}
