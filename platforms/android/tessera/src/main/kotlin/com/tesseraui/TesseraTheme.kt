package com.tesseraui

import android.app.UiModeManager
import android.content.Context
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle

val LocalTesseraColors = staticCompositionLocalOf { TesseraLightColors }

/**
 * TesseraUI 主题。默认跟随系统的深色模式；Android 14+ 跟随系统“对比度”设置（≥ 中等即使用增强对比度色板）。
 */
@Composable
fun TesseraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    highContrast: Boolean = isSystemHighContrast(),
    content: @Composable () -> Unit,
) {
    val colors = when {
        darkTheme && highContrast -> TesseraDarkHighContrastColors
        darkTheme -> TesseraDarkColors
        highContrast -> TesseraLightHighContrastColors
        else -> TesseraLightColors
    }
    CompositionLocalProvider(LocalTesseraColors provides colors, content = content)
}

object Tessera {
    val colors: TesseraColors
        @Composable @ReadOnlyComposable get() = LocalTesseraColors.current
}

@Composable
fun isSystemHighContrast(): Boolean {
    val context = LocalContext.current
    if (Build.VERSION.SDK_INT < 34) return false
    val ui = context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
    return ui.contrast >= 0.5f
}

/** 给文字样式补上字体家族（在 App 里用 FontFamily(Font(R.font.plus_jakarta_sans_…)) 创建后传入） */
fun TextStyle.withFamily(family: androidx.compose.ui.text.font.FontFamily) = copy(fontFamily = family)
