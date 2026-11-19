package com.tesseraui.samples

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tesseraui.Tessera
import com.tesseraui.TesseraText
import com.tesseraui.components.*
import com.tesseraui.foundation.tilePosition
import com.tesseraui.icons.TesseraIcon
import com.tesseraui.icons.TesseraIcons

enum class GallerySection { Actions, Inputs, Selection, Navigation, Display, Feedback, MoreActions, MoreInputs, Range, Pickers, PickerFields, Color, Forms }

/** 组件总览：开发时预览、截图测试共用 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TesseraGallery(section: GallerySection) {
    Column(
        Modifier.width(412.dp).background(Tessera.colors.bgCanvas).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TesseraText(section.title, style = Tessera.type.title2)
        when (section) {
            GallerySection.Actions -> {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TesseraButton("主要按钮", {})
                    TesseraButton("次要按钮", {}, variant = TesseraButtonVariant.Secondary)
                    TesseraButton("文字按钮", {}, variant = TesseraButtonVariant.Text)
                    TesseraButton("删除", {}, variant = TesseraButtonVariant.Danger)
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TesseraButton("小 32", {}, size = TesseraButtonSize.Sm)
                    TesseraButton("中 40", {}, icon = TesseraIcons.Plus)
                    TesseraButton("大 48", {}, size = TesseraButtonSize.Lg)
                    TesseraButton("加载中", {}, loading = true)
                    TesseraButton("禁用", {}, enabled = false)
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TesseraIconButton(TesseraIcons.Search, "搜索", {})
                    TesseraIconButton(TesseraIcons.Share, "分享", {}, filled = true)
                    TesseraIconButton(TesseraIcons.Bell, "通知", {}, filled = true, badge = { TesseraBadge(null) })
                    TesseraIconButton(TesseraIcons.More, "更多", {}, filled = true)
                }
            }
            GallerySection.Display -> {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    TesseraTag("默认"); TesseraTag("进行中", tone = TesseraTone.Accent)
                    TesseraTag("已完成", tone = TesseraTone.Success, icon = TesseraIcons.Check)
                    TesseraTag("即将到期", tone = TesseraTone.Warning); TesseraTag("已逾期", tone = TesseraTone.Danger)
                    TesseraTag("设计", tone = TesseraTone.Lapis); TesseraBadge(8); TesseraBadge(120)
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    TesseraAvatar("林", size = TesseraAvatarSize.Lg, badge = { TesseraBadge(null, color = Tessera.colors.successSolid) })
                    TesseraAvatar("朱", tone = TesseraTone.Cinnabar)
                    TesseraAvatar("赭", tone = TesseraTone.Ochre, size = TesseraAvatarSize.Sm)
                    TesseraAvatarGroup(listOf("陈", "吴", "赵", "周", "王", "孙"))
                }
                val rows = listOf(Triple("设计评审", "今天 14:00 · 3 号会议室", TesseraTone.Lapis), Triple("v0.3 发布检查", "Android 截图测试通过", TesseraTone.Olive), Triple("首批图标", "157 / 150", TesseraTone.Ochre))
                TesseraList {
                    rows.forEachIndexed { i, (t, s, tone) ->
                        TesseraListItem(t, tilePosition(i, rows.size), supporting = s, leading = { TesseraAvatar(t.take(1), tone = tone) },
                            trailing = { TesseraIcon(TesseraIcons.ChevronRight, null, size = 18.dp) }, onClick = {})
                    }
                }
                TesseraCard(Modifier.fillMaxWidth()) { TesseraSkeletonRow() }
            }
            GallerySection.MoreActions, GallerySection.MoreInputs, GallerySection.Range, GallerySection.Pickers, GallerySection.PickerFields, GallerySection.Color, GallerySection.Forms -> GalleryForms(section)
            else -> GalleryMore(section)
        }
    }
}

private val GallerySection.title get() = when (this) {
    GallerySection.Actions -> "按钮"
    GallerySection.Inputs -> "输入"
    GallerySection.Selection -> "选择"
    GallerySection.Navigation -> "导航"
    GallerySection.Display -> "数据展示"
    GallerySection.Feedback -> "反馈"
    GallerySection.MoreActions -> "按钮组 · 分割按钮 · 悬浮按钮 · 链接"
    GallerySection.MoreInputs -> "多行文本 · 密码 · 数字 · 验证码 · 自动补全 · 标签"
    GallerySection.Range -> "滑块 · 评分 · 上传"
    GallerySection.Pickers -> "日历"
    GallerySection.PickerFields -> "滚轮 · 日期时间"
    GallerySection.Color -> "颜色"
    GallerySection.Forms -> "表单"
}
