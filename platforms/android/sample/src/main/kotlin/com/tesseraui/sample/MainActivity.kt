package com.tesseraui.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tesseraui.Tessera
import com.tesseraui.TesseraTheme
import com.tesseraui.TesseraText
import com.tesseraui.isSystemHighContrast
import com.tesseraui.components.TesseraSegmentedControl
import com.tesseraui.foundation.TesseraShape
import com.tesseraui.samples.GallerySection
import com.tesseraui.samples.TesseraGallery

/** 示例 App：在真机上切换外观与主题，逐个查看首批 22 个组件 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { SampleApp() }
    }
}

@Composable
fun SampleApp() {
    var shape by rememberSaveable { mutableIntStateOf(0) }
    var theme by rememberSaveable { mutableIntStateOf(0) }
    var contrast by rememberSaveable { mutableIntStateOf(0) }
    val systemContrast = isSystemHighContrast()
    val system = isSystemInDarkTheme()
    TesseraTheme(
        darkTheme = when (theme) { 1 -> false; 2 -> true; else -> system },
        highContrast = when (contrast) { 1 -> false; 2 -> true; else -> systemContrast },
        shape = TesseraShape.entries[shape],
    ) {
        LazyColumn(
            Modifier.fillMaxSize().background(Tessera.colors.bgCanvas).windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            item {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    TesseraText("TesseraUI 示例", style = Tessera.type.title1)
                    TesseraText("外观", style = Tessera.type.label)
                    TesseraSegmentedControl(listOf("拼贴", "圆融", "流韵"), selectedIndex = shape, onSelect = { shape = it })
                    TesseraText("主题", style = Tessera.type.label)
                    TesseraSegmentedControl(listOf("跟随系统", "浅色", "深色"), selectedIndex = theme, onSelect = { theme = it })
                    TesseraText("对比度", style = Tessera.type.label)
                    TesseraSegmentedControl(listOf("跟随系统", "标准", "增强"), selectedIndex = contrast, onSelect = { contrast = it })
                    if (shape == 2) TesseraText("流韵只在性能较好的设备上启用；设备不满足要求或打开了省电模式、移除动画时，会自动显示为圆融。", style = Tessera.type.caption, color = Tessera.colors.textSecondary)
                }
            }
            items(GallerySection.entries) { section -> TesseraGallery(section) }
        }
    }
}
