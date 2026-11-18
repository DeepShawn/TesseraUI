package com.tesseraui

import android.app.UiModeManager
import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow

/**
 * 字体。标题用软衬线（Fraunces + 思源宋体），正文用 Instrument Sans + 系统黑体。
 * 在 App 里用 FontFamily(Font(R.font.fraunces_semibold, FontWeight.SemiBold), …) 创建后传入 TesseraTheme。
 * 未传入时，标题回退到系统衬线体（中文为 Noto Serif CJK），正文回退到系统无衬线体。
 */
@Immutable
data class TesseraFonts(
    val display: FontFamily = FontFamily.Serif,
    val text: FontFamily = FontFamily.Default,
    val mono: FontFamily = FontFamily.Monospace,
)

/** 套上字体后的文字样式 */
@Immutable
data class TesseraTypography(
    val display: TextStyle, val title1: TextStyle, val title2: TextStyle, val title3: TextStyle,
    val body: TextStyle, val bodySm: TextStyle, val label: TextStyle, val caption: TextStyle, val overline: TextStyle,
) {
    companion object {
        fun from(f: TesseraFonts) = TesseraTypography(
            display = TesseraType.display.copy(fontFamily = f.display),
            title1 = TesseraType.title1.copy(fontFamily = f.display),
            title2 = TesseraType.title2.copy(fontFamily = f.display),
            title3 = TesseraType.title3.copy(fontFamily = f.display),
            body = TesseraType.body.copy(fontFamily = f.text),
            bodySm = TesseraType.bodySm.copy(fontFamily = f.text),
            label = TesseraType.label.copy(fontFamily = f.text),
            caption = TesseraType.caption.copy(fontFamily = f.text),
            overline = TesseraType.overline.copy(fontFamily = f.text),
        )
    }
}

val LocalTesseraColors = staticCompositionLocalOf { TesseraLightColors }
val LocalTesseraTypography = staticCompositionLocalOf { TesseraTypography.from(TesseraFonts()) }
val LocalTesseraFonts = staticCompositionLocalOf { TesseraFonts() }
val LocalTesseraDark = staticCompositionLocalOf { false }
/** 系统“移除动画”打开时为 true：去掉位移和缩放，统一改为 150ms 淡入淡出 */
val LocalReduceMotion = staticCompositionLocalOf { false }
/** 当前内容颜色（文字、图标默认使用） */
val LocalContentColor = compositionLocalOf { Color.Unspecified }

/**
 * TesseraUI 主题。默认跟随系统深色模式；Android 14+ 跟随系统“对比度”设置（≥ 中等即使用增强对比度色板）；
 * 跟随系统“移除动画”设置。
 */
@Composable
fun TesseraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    highContrast: Boolean = isSystemHighContrast(),
    fonts: TesseraFonts = TesseraFonts(),
    reduceMotion: Boolean = isSystemReduceMotion(),
    content: @Composable () -> Unit,
) {
    val colors = when {
        darkTheme && highContrast -> TesseraDarkHighContrastColors
        darkTheme -> TesseraDarkColors
        highContrast -> TesseraLightHighContrastColors
        else -> TesseraLightColors
    }
    val typography = remember(fonts) { TesseraTypography.from(fonts) }
    CompositionLocalProvider(
        LocalTesseraColors provides colors,
        LocalTesseraTypography provides typography,
        LocalTesseraFonts provides fonts,
        LocalTesseraDark provides darkTheme,
        LocalReduceMotion provides reduceMotion,
        LocalContentColor provides colors.textPrimary,
        content = content,
    )
}

object Tessera {
    val colors: TesseraColors
        @Composable @ReadOnlyComposable get() = LocalTesseraColors.current
    val type: TesseraTypography
        @Composable @ReadOnlyComposable get() = LocalTesseraTypography.current
    val isDark: Boolean
        @Composable @ReadOnlyComposable get() = LocalTesseraDark.current
    val reduceMotion: Boolean
        @Composable @ReadOnlyComposable get() = LocalReduceMotion.current
}

/** 弹簧动画；系统“移除动画”时退化为 150ms 线性过渡 */
@Composable
@ReadOnlyComposable
fun <T> motion(spring: SpringSpec<T>): FiniteAnimationSpec<T> =
    if (LocalReduceMotion.current) tween(TesseraMotion.DURATION_FAST) else spring

@Composable
fun isSystemHighContrast(): Boolean {
    if (LocalInspectionMode.current || Build.VERSION.SDK_INT < 34) return false
    val ui = LocalContext.current.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager ?: return false
    return ui.contrast >= 0.5f
}

@Composable
fun isSystemReduceMotion(): Boolean {
    if (LocalInspectionMode.current) return false
    val resolver = LocalContext.current.contentResolver
    return Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
}

/** 文字：默认使用正文样式和当前内容颜色 */
@Composable
fun TesseraText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = Tessera.type.body,
    color: Color = Color.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Ellipsis,
    textAlign: TextAlign? = null,
) {
    val c = color.takeOrElse { LocalContentColor.current.takeOrElse { Tessera.colors.textPrimary } }
    BasicText(
        text = text,
        modifier = modifier,
        style = style.copy(color = c, textAlign = textAlign ?: style.textAlign),
        maxLines = maxLines,
        overflow = overflow,
    )
}

private inline fun Color.takeOrElse(block: () -> Color): Color = if (this != Color.Unspecified) this else block()

/** 给文字样式补上字体家族 */
fun TextStyle.withFamily(family: FontFamily) = copy(fontFamily = family)
