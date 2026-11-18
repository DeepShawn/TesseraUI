package com.tesseraui.samples

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tesseraui.Tessera
import com.tesseraui.TesseraText
import com.tesseraui.components.*
import com.tesseraui.foundation.tilePosition
import com.tesseraui.icons.TesseraIcons

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun GalleryMore(section: GallerySection) {
    when (section) {
        GallerySection.Inputs -> Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            TesseraTextField("", {}, label = "昵称", placeholder = "输入昵称", help = "用拼音输入法打字时，选定汉字之前不计数")
            TesseraTextField("hello@", {}, label = "邮箱", leadingIcon = TesseraIcons.Mail, error = "邮箱格式不正确")
            TesseraSearchField("设计", {}, {})
            TesseraSelect(listOf("最近更新", "名称", "大小"), 0, {}, label = "排序")
            TesseraTextField("一块块砖，拼出整面墙。", {}, label = "留言", singleLine = false, minLines = 3)
        }
        GallerySection.Selection -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                TesseraCheckbox(true, {}, label = "接收通知")
                TesseraCheckbox(false, {}, label = "自动更新")
                TesseraCheckbox(false, {}, label = "部分选中", indeterminate = true)
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                TesseraRadio(true, {}, label = "标清"); TesseraRadio(false, {}, label = "高清"); TesseraRadio(false, {}, label = "原画", enabled = false)
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                TesseraSwitch(true, {}, label = "Wi-Fi"); TesseraSwitch(false, {}, label = "蓝牙"); TesseraSwitch(false, {}, label = "飞行模式", enabled = false)
            }
            Box(Modifier.padding(top = 8.dp)) { TesseraSegmentedControl(listOf("日", "周", "月", "全年"), 1, {}) }
            Box(Modifier.padding(top = 8.dp)) { TesseraSegmentedControl(listOf("列表", "网格", "看板"), 0, {}, small = true) }
        }
        GallerySection.Navigation -> Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            TesseraNavBar("设置", navigationIcon = { TesseraIconButton(TesseraIcons.ChevronLeft, "返回", {}) },
                actions = { TesseraIconButton(TesseraIcons.Bell, "通知", {}, badge = { TesseraBadge(3) }) }, applyStatusBarInsets = false)
            TesseraNavBar("早上好", largeTitle = true, applyStatusBarInsets = false, actions = { TesseraIconButton(TesseraIcons.Search, "搜索", {}) })
            TesseraTabs(listOf("概览", "活动记录", "成员", "设置"), 0, {})
            TesseraTabBar(
                listOf(
                    TesseraTabBarItem("首页", TesseraIcons.Home, TesseraIcons.HomeFill),
                    TesseraTabBarItem("收藏", TesseraIcons.Star, TesseraIcons.StarFill),
                    TesseraTabBarItem("消息", TesseraIcons.Message, TesseraIcons.MessageFill, badge = 5),
                    TesseraTabBarItem("我的", TesseraIcons.User, TesseraIcons.UserFill),
                ), 0, {}, applyNavigationBarInsets = false,
            )
            TesseraMenuPanel {
                TesseraMenuItem("编辑", {}, icon = TesseraIcons.Edit, shortcut = "⌘E")
                TesseraMenuItem("复制", {}, icon = TesseraIcons.Copy, shortcut = "⌘C")
                TesseraMenuItem("分享", {}, icon = TesseraIcons.Share, selected = true)
                TesseraMenuSeparator()
                TesseraMenuItem("删除", {}, icon = TesseraIcons.Trash, danger = true)
            }
        }
        GallerySection.Feedback -> Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            TesseraToast("已保存到相册", TesseraIcons.SuccessFill)
            TesseraToast("已删除 3 个文件", TesseraIcons.Trash, actionLabel = "撤销")
            TesseraDialogPanel("发布 v0.3？", "发布", {}, text = "发布后会生成 0.3.0 标签和更新日志，四端产物同步打包。")
            TesseraDialogPanel("删除项目？", "删除", {}, text = "删除后无法恢复。", danger = true)
            TesseraSheetPanel("分享到") {
                val rows = listOf("复制链接" to TesseraTone.Neutral, "通过邮件发送" to TesseraTone.Cinnabar, "发消息" to TesseraTone.Lapis)
                TesseraList {
                    rows.forEachIndexed { i, (t, tone) -> TesseraListItem(t, tilePosition(i, rows.size), leading = { TesseraAvatar(t.take(1), tone = tone, size = TesseraAvatarSize.Sm) }) }
                }
            }
        }
        else -> TesseraText("—", style = Tessera.type.body)
    }
}
