package com.tesseraui.samples

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tesseraui.Tessera
import com.tesseraui.TesseraText
import com.tesseraui.components.*
import com.tesseraui.icons.TesseraIcons

/** v0.5 表单与输入组件的总览：开发预览、截图测试、无障碍检查与示例 App 共用 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun GalleryForms(section: GallerySection) {
    when (section) {
        GallerySection.MoreActions -> Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            var view by remember { mutableStateOf(setOf(0)) }
            TesseraButtonGroup(listOf("日", "周", "月"), view, { view = it })
            var fmt by remember { mutableStateOf(setOf(0, 2)) }
            TesseraButtonGroup(listOf("加粗", "斜体", "下划线"), fmt, { fmt = it }, multiple = true, size = TesseraButtonSize.Sm)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                TesseraSplitButton("保存", {}, icon = TesseraIcons.Download) { close ->
                    TesseraMenuItem("另存为…", { close() }, icon = TesseraIcons.Copy)
                    TesseraMenuItem("导出 PDF", { close() }, icon = TesseraIcons.FileText)
                }
                TesseraSplitButton("合并", {}, variant = TesseraButtonVariant.Secondary) { close -> TesseraMenuItem("变基合并", { close() }) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                TesseraFab(TesseraIcons.Plus, "新建", {})
                TesseraFab(TesseraIcons.Edit, "写评论", {}, text = "写评论")
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                TesseraLink("查看使用条款", {})
                TesseraLink("TesseraUI 文档", {}, external = true)
            }
        }
        GallerySection.MoreInputs -> Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            var note by remember { mutableStateOf("一块块砖，拼出整面墙。") }
            TesseraTextArea(note, { note = it }, label = "留言", maxLength = 100, help = "选定汉字后计数")
            var pwd by remember { mutableStateOf("tessera2026") }
            TesseraPasswordField(pwd, { pwd = it }, help = "至少 8 位，包含字母和数字")
            var qty by remember { mutableDoubleStateOf(3.0) }
            TesseraNumberInput(qty, { qty = it }, label = "数量", min = 1.0, max = 99.0)
            var otp by remember { mutableStateOf("2026") }
            TesseraOtpInput(otp, { otp = it })
            var city by remember { mutableStateOf("杭州") }
            TesseraAutocomplete(city, { city = it }, listOf("杭州", "广州", "苏州", "福州", "郑州"), label = "城市", leadingIcon = TesseraIcons.Location)
            var tags by remember { mutableStateOf(listOf("设计", "动效")) }
            TesseraTagInput(tags, { tags = it }, label = "标签")
        }
        GallerySection.Range -> Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            var vol by remember { mutableFloatStateOf(60f) }
            TesseraSlider(vol, { vol = it }, label = "音量")
            var price by remember { mutableStateOf(200f..800f) }
            TesseraRangeSlider(price, { price = it }, range = 0f..1000f, step = 100f, label = "价格", format = { "¥${it.toInt()}" })
            var stars by remember { mutableIntStateOf(4) }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                TesseraRating(stars, { stars = it })
                TesseraRating(3, readOnly = true)
            }
            var files by remember { mutableStateOf(listOf(TesseraUploadFile("设计稿.pdf", 2_400_000), TesseraUploadFile("封面.png", 820_000, progress = 0.62f))) }
            TesseraFileUpload(files, onPick = {}, onRemove = { f -> files = files - f })
        }
        GallerySection.Pickers -> Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            var day by remember { mutableStateOf<TesseraDate?>(TesseraDate(2026, 11, 20)) }
            // 截图里“今天”固定为 11 月 19 日，基准图不随运行日期变化
            TesseraCalendar(selected = day, onSelect = { day = it }, initialMonth = TesseraDate(2026, 11, 1), today = TesseraDate(2026, 11, 19))
            var s by remember { mutableStateOf<TesseraDate?>(TesseraDate(2026, 11, 24)) }
            var e by remember { mutableStateOf<TesseraDate?>(TesseraDate(2026, 11, 27)) }
            TesseraCalendar(rangeStart = s, rangeEnd = e, onRangeChange = { a, b -> s = a; e = b }, initialMonth = TesseraDate(2026, 11, 1), today = TesseraDate(2026, 11, 19))
        }
        GallerySection.PickerFields -> Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            var day by remember { mutableStateOf<TesseraDate?>(TesseraDate(2026, 11, 20)) }
            var h by remember { mutableIntStateOf(9) }
            var m by remember { mutableIntStateOf(30) }
            TesseraWheelPicker(Modifier.width(220.dp)) {
                TesseraWheel((0..23).map { "%02d".format(it) }, h, { h = it }, Modifier.weight(1f), label = "时")
                TesseraText(":", style = Tessera.type.title3, color = Tessera.colors.textTertiary)
                TesseraWheel((0..59).map { "%02d".format(it) }, m, { m = it }, Modifier.weight(1f), label = "分")
            }
            TesseraDatePicker(day, { day = it }, label = "出发日期")
            TesseraTimePicker(h, m, { a, b -> h = a; m = b }, label = "提醒时间", step = 5)
            TesseraDateRangePicker(TesseraDate(2026, 11, 20), TesseraDate(2026, 11, 23), { _, _ -> }, label = "入住与离店")
        }
        GallerySection.Color -> Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            var color by remember { mutableStateOf(Color(0xFF0F6B63)) }
            TesseraColorPicker(color, { color = it })
        }
        GallerySection.Forms -> {
            val form = rememberTesseraFormState()
            var name by remember { mutableStateOf("") }
            var phone by remember { mutableStateOf("138 0013 8000") }
            var mail by remember { mutableStateOf("hello@") }
            var agree by remember { mutableStateOf(false) }
            // 截图里展示提交后的状态（显示中文校验提示）
            remember { form.field("name", "", TesseraRules.required("姓名")); form.field("mail", "hello@", TesseraRules.email); form.submit() }
            TesseraForm {
                TesseraFieldGroup("联系方式", description = "我们只在订单有变化时联系你") {
                    TesseraTextField(name, { name = it }, label = "姓名", error = form.field("name", name, TesseraRules.required("姓名")))
                    TesseraTextField(phone, { phone = it }, label = "手机号", error = form.field("phone", phone, TesseraRules.required("手机号"), TesseraRules.phone))
                    TesseraTextField(mail, { mail = it }, label = "邮箱", error = form.field("mail", mail, TesseraRules.email))
                }
                TesseraFormField("满意度", required = true, help = "点同一颗星可清除") { var s by remember { mutableIntStateOf(0) }; TesseraRating(s, { s = it }) }
                TesseraCheckbox(agree, { agree = it }, label = "我已阅读并同意服务条款")
                TesseraButton("提交", { form.submit() }, modifier = Modifier.fillMaxWidth())
            }
        }
        else -> {}
    }
}
