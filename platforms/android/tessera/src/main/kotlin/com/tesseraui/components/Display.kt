package com.tesseraui.components

import com.tesseraui.foundation.flowRipple
import com.tesseraui.foundation.roundRadius
import com.tesseraui.TesseraRound
import com.tesseraui.foundation.avatarShape
import com.tesseraui.foundation.isRound
import com.tesseraui.foundation.shapeOf
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tesseraui.LocalContentColor
import com.tesseraui.LocalReduceMotion
import com.tesseraui.Tessera
import com.tesseraui.TesseraRadius
import com.tesseraui.TesseraSpace
import com.tesseraui.TesseraText
import com.tesseraui.TesseraTile
import com.tesseraui.foundation.TilePosition
import com.tesseraui.foundation.tileShape
import com.tesseraui.icons.TesseraIcon

// ------------------------------------------------------------------
// 标签 Tag / 徽标 Badge
// ------------------------------------------------------------------

enum class TesseraTone { Neutral, Accent, Success, Warning, Danger, Cinnabar, Ochre, Lapis, Amethyst, Olive }

@Composable
internal fun toneColors(tone: TesseraTone): Pair<Color, Color> {
    val c = Tessera.colors
    return when (tone) {
        TesseraTone.Neutral -> c.fillDefault to c.textSecondary
        TesseraTone.Accent -> c.accentSubtle to c.accentText
        TesseraTone.Success -> c.successSubtle to c.successText
        TesseraTone.Warning -> c.warningSubtle to c.warningText
        TesseraTone.Danger -> c.dangerSubtle to c.dangerText
        TesseraTone.Cinnabar -> c.pigmentCinnabarSubtle to c.pigmentCinnabarText
        TesseraTone.Ochre -> c.pigmentOchreSubtle to c.pigmentOchreText
        TesseraTone.Lapis -> c.pigmentLapisSubtle to c.pigmentLapisText
        TesseraTone.Amethyst -> c.pigmentAmethystSubtle to c.pigmentAmethystText
        TesseraTone.Olive -> c.pigmentOliveSubtle to c.pigmentOliveText
    }
}

/** 标签：圆角 8 的小砖；状态色只表示状态，颜料色只用于分类 */
@Composable
fun TesseraTag(text: String, modifier: Modifier = Modifier, tone: TesseraTone = TesseraTone.Neutral, icon: ImageVector? = null) {
    val (bg, fg) = toneColors(tone)
    Row(
        modifier.height(24.dp).clip(shapeOf(TesseraRadius.sm)).background(bg).padding(horizontal = if (isRound) 10.dp else 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        CompositionLocalProvider(LocalContentColor provides fg) {
            if (icon != null) TesseraIcon(icon, null, size = 14.dp)
            TesseraText(text, style = Tessera.type.caption.copy(fontWeight = FontWeight.SemiBold), maxLines = 1)
        }
    }
}

/** 徽标：数字小砖（count）或小圆点（count = null） */
@Composable
fun TesseraBadge(count: Int?, modifier: Modifier = Modifier, color: Color = Tessera.colors.dangerSolid, max: Int = 99) {
    val ring = Tessera.colors.bgSurface
    if (count == null) {
        Box(modifier.size(8.dp).clip(shapeOf(3.dp)).background(color).semantics { contentDescription = "有新消息" })
        return
    }
    val label = if (count > max) "$max+" else "$count"
    Box(
        modifier
            .defaultMinSize(minWidth = 18.dp)
            .height(18.dp)
            .border(2.dp, ring, shapeOf(8.dp))
            .padding(1.dp)
            .clip(shapeOf(7.dp))
            .background(color)
            .padding(horizontal = 5.dp)
            .clearAndSetSemantics { contentDescription = "$count 条未读" },
        contentAlignment = Alignment.Center,
    ) {
        TesseraText(label, style = Tessera.type.caption.copy(fontSize = 11.sp, lineHeight = 11.sp, fontWeight = FontWeight.SemiBold), color = Tessera.colors.textOnSolid)
    }
}

// ------------------------------------------------------------------
// 头像 Avatar：36% 圆角的方砖，文字用软衬线
// ------------------------------------------------------------------

enum class TesseraAvatarSize(val dp: Dp, val font: Int) { Sm(32.dp, 13), Md(40.dp, 16), Lg(56.dp, 22) }

@Composable
fun TesseraAvatar(
    text: String,
    modifier: Modifier = Modifier,
    tone: TesseraTone = TesseraTone.Accent,
    size: TesseraAvatarSize = TesseraAvatarSize.Md,
    badge: (@Composable () -> Unit)? = null,
) {
    val (bg, fg) = toneColors(tone)
    Box(modifier.size(size.dp)) {
        Box(
            Modifier.size(size.dp).clip(avatarShape()).background(Tessera.colors.bgSurface).background(bg)
                .semantics { contentDescription = text },
            contentAlignment = Alignment.Center,
        ) {
            TesseraText(text.take(2), style = Tessera.type.title3.copy(fontSize = size.font.sp, lineHeight = size.font.sp), color = fg)
        }
        if (badge != null) Box(Modifier.align(Alignment.TopEnd).offset(3.dp, (-3).dp)) { badge() }
    }
}

/** 头像组：后一个叠在前一个上，之间留一道砖缝 */
@Composable
fun TesseraAvatarGroup(names: List<String>, modifier: Modifier = Modifier, max: Int = 4, size: TesseraAvatarSize = TesseraAvatarSize.Sm) {
    val tones = listOf(TesseraTone.Lapis, TesseraTone.Olive, TesseraTone.Amethyst, TesseraTone.Ochre, TesseraTone.Cinnabar)
    Row(modifier.semantics(mergeDescendants = true) { contentDescription = names.joinToString("、") }) {
        // 圆融外观：头像之间的描边细一些（2dp），叠得更紧
        val ring = if (isRound) 2.dp else TesseraTile.grout
        val overlap = if (isRound) 8 else 6
        names.take(max).forEachIndexed { i, n ->
            Box(Modifier.offset(x = (-overlap * i).dp).border(ring, Tessera.colors.bgSurface, avatarShape()).padding(ring)) {
                TesseraAvatar(n, tone = tones[i % tones.size], size = size)
            }
        }
        if (names.size > max) Box(Modifier.offset(x = (-overlap * max).dp).border(ring, Tessera.colors.bgSurface, avatarShape()).padding(ring)) {
            TesseraAvatar("+${names.size - max}", tone = TesseraTone.Neutral, size = size)
        }
    }
}

// ------------------------------------------------------------------
// 卡片 Card
// ------------------------------------------------------------------

@Composable
fun TesseraCard(
    modifier: Modifier = Modifier,
    elevated: Boolean = false,
    onClick: (() -> Unit)? = null,
    padding: Dp = TesseraSpace.s6,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = Tessera.colors
    val shape = shapeOf(TesseraRadius.xl, TesseraRound.card)
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier
            .shadow(if (elevated) 10.dp else 3.dp, shape, ambientColor = c.shadowColor, spotColor = c.shadowColor)
            .clip(shape)
            .background(if (elevated) c.bgElevated else c.bgSurface)
            // 圆融外观没有描边，只靠柔和的投影
            .then(if (isRound) Modifier else Modifier.border(1.dp, c.borderSurface, shape))
            .then(if (onClick != null) Modifier.clickable(interaction, null, role = Role.Button, onClick = onClick) else Modifier)
            .padding(padding),
    ) { CompositionLocalProvider(LocalContentColor provides c.textPrimary) { content() } }
}

// ------------------------------------------------------------------
// 列表 List：每行一块砖，砖缝分隔，首尾外角大
// ------------------------------------------------------------------

/**
 * 列表。拼贴：每行一块砖，砖缝 3dp；圆融：整个列表是一整块圆角面，行与行之间没有缝、没有线，按下时行内浮出一颗胶囊。
 */
@Composable
fun TesseraList(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    if (isRound) {
        val c = Tessera.colors
        val shape = RoundedCornerShape(roundRadius(TesseraRound.card))
        Column(
            modifier.shadow(3.dp, shape, ambientColor = c.shadowColor, spotColor = c.shadowColor).clip(shape).background(c.bgSurface).padding(6.dp),
            content = content,
        )
    } else {
        Column(modifier, verticalArrangement = Arrangement.spacedBy(TesseraTile.grout), content = content)
    }
}

@Composable
fun TesseraListItem(
    headline: String,
    position: TilePosition,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val c = Tessera.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Row(
        modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 60.dp)
            .clip(if (isRound) RoundedCornerShape(roundRadius(TesseraRound.row)) else tileShape(position, TesseraRadius.lg, vertical = true))
            .background(if (isRound) Color.Transparent else c.bgSurface)
            .background(if (pressed) c.statePressed else Color.Transparent)
            .flowRipple(interaction, c.textPrimary.copy(alpha = 0.13f))
            .then(if (onClick != null) Modifier.clickable(interaction, null, role = Role.Button, onClick = onClick) else Modifier)
            .padding(horizontal = if (isRound) TesseraSpace.s3 else TesseraSpace.s4, vertical = TesseraSpace.s2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TesseraSpace.s3),
    ) {
        if (leading != null) leading()
        Column(Modifier.weight(1f)) {
            TesseraText(headline, style = Tessera.type.label, maxLines = 1)
            if (supporting != null) TesseraText(supporting, style = Tessera.type.caption, color = c.textSecondary, maxLines = 2)
        }
        if (trailing != null) CompositionLocalProvider(LocalContentColor provides c.textTertiary) { trailing() }
    }
}

// ------------------------------------------------------------------
// 骨架屏 Skeleton：光带 1.5 秒扫一次；“移除动画”时静止
// ------------------------------------------------------------------

@Composable
fun TesseraSkeleton(modifier: Modifier = Modifier, shape: Shape = shapeOf(TesseraRadius.grout)) {
    val c = Tessera.colors
    val reduce = LocalReduceMotion.current
    val x = if (reduce) -1f else {
        val t = rememberInfiniteTransition(label = "skeleton")
        t.animateFloat(-1f, 2f, infiniteRepeatable(tween(1500, easing = LinearEasing), RepeatMode.Restart), label = "shimmer").value
    }
    Box(
        modifier.clip(shape).background(c.fillDefault).drawBehind {
            if (reduce) return@drawBehind
            val w = size.width * 0.5f
            val start = x * size.width
            drawRect(Brush.horizontalGradient(listOf(Color.Transparent, c.bgSurface.copy(alpha = 0.7f), Color.Transparent), startX = start, endX = start + w))
        }.semantics { contentDescription = "加载中" },
    )
}

/** 常用骨架：头像 + 三行文字 */
@Composable
fun TesseraSkeletonRow(modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(TesseraSpace.s3)) {
        TesseraSkeleton(Modifier.size(40.dp), avatarShape())
        Column(Modifier.weight(1f).padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TesseraSkeleton(Modifier.fillMaxWidth(0.4f).height(12.dp))
            TesseraSkeleton(Modifier.fillMaxWidth(0.9f).height(12.dp))
            TesseraSkeleton(Modifier.fillMaxWidth(0.7f).height(12.dp))
        }
    }
}

