import SwiftUI

/// v0.5 表单与输入组件总览：Xcode 预览、截图测试、iOS UI 测试与示例 App 共用
public struct TesseraFormsGallery: View {
    private let scrolls: Bool
    @State private var view: Set<Int> = [0]
    @State private var fmt: Set<Int> = [0, 2]
    @State private var note = "一块块砖，拼出整面墙。"
    @State private var pwd = "tessera2026"
    @State private var qty: Double = 3
    @State private var otp = "2026"
    @State private var city = "杭州"
    @State private var tags = ["设计", "动效"]
    @State private var vol: Double = 60
    @State private var price: ClosedRange<Double> = 200...800
    @State private var stars = 4
    @State private var readOnlyStars = 3
    @State private var files = [TesseraUploadFile(name: "设计稿.pdf", size: 2_400_000), TesseraUploadFile(name: "封面.png", size: 820_000, progress: 0.62)]
    @State private var day: Date? = tesseraCalendar.date(from: DateComponents(year: 2026, month: 11, day: 20))
    @State private var range: ClosedRange<Date>? = tesseraCalendar.date(from: DateComponents(year: 2026, month: 11, day: 24))!...tesseraCalendar.date(from: DateComponents(year: 2026, month: 11, day: 27))!
    @State private var hour: Int? = 9
    @State private var minute: Int? = 30
    @State private var wheelH = 9
    @State private var wheelM = 30
    @State private var color = "#0F6B63"
    @State private var name = ""
    @State private var phone = "138 0013 8000"
    @State private var mail = "hello@"
    @State private var agree = false
    @State private var satisfaction = 0
    @StateObject private var form = TesseraFormState()
    private let today = tesseraCalendar.date(from: DateComponents(year: 2026, month: 11, day: 19))!

    /// - Parameter scrolls: 截图测试传 false（ImageRenderer 画不出系统滚动视图）
    public init(scrolls: Bool = true) { self.scrolls = scrolls }

    public var body: some View {
        Group {
            if scrolls { ScrollView(showsIndicators: false) { content } } else { content }
        }
        .background(TesseraColor.bgCanvas)
    }

    private func section(_ title: String) -> some View {
        TesseraLabel(title, style: .title2).accessibilityAddTraits(.isHeader).padding(.top, TesseraSpace.s4)
    }

    private var content: some View {
        VStack(alignment: .leading, spacing: TesseraSpace.s4) {
            section("按钮组 · 分割按钮 · 悬浮按钮 · 链接")
            TesseraButtonGroup(["日", "周", "月"], selection: $view)
            TesseraButtonGroup(["加粗", "斜体", "下划线"], selection: $fmt, multiple: true, size: .sm)
            HStack(spacing: TesseraSpace.s3) {
                TesseraSplitButton("保存", icon: .download, action: {}) {
                    Button("另存为…") {}
                    Button("导出 PDF") {}
                }
                TesseraSplitButton("合并", variant: .secondary, action: {}) { Button("变基合并") {} }
            }
            HStack(spacing: TesseraSpace.s4) {
                TesseraFab(.plus, label: "新建") {}
                TesseraFab(.edit, label: "写评论", text: "写评论") {}
            }
            HStack(spacing: TesseraSpace.s4) {
                TesseraLink("查看使用条款") {}
                TesseraLink("TesseraUI 文档", external: true) {}
            }

            section("多行文本 · 密码 · 数字 · 验证码 · 自动补全 · 标签")
            TesseraTextArea("留言", text: $note, help: "选定汉字后计数", maxLength: 100)
            TesseraPasswordField(text: $pwd, help: "至少 8 位，包含字母和数字")
            TesseraNumberInput("数量", value: $qty, range: 1...99)
            TesseraOtpInput(code: $otp)
            TesseraAutocomplete("城市", text: $city, options: ["杭州", "广州", "苏州", "福州", "郑州"], icon: .location)
            TesseraTagInput("标签", tags: $tags)

            section("滑块 · 评分 · 上传")
            TesseraSlider("音量", value: $vol)
            TesseraRangeSlider("价格", value: $price, in: 0...1000, step: 100, format: { "¥\(Int($0))" })
            HStack(spacing: TesseraSpace.s4) {
                TesseraRating(value: $stars)
                TesseraRating(value: $readOnlyStars, readOnly: true)
            }
            TesseraFileUpload(files: $files) { _ in }

            section("日历 · 滚轮 · 日期时间 · 颜色")
            TesseraCalendar(selection: $day, today: today)
            TesseraCalendar(range: $range, today: today)
            TesseraWheelPicker {
                TesseraWheel((0..<24).map { String(format: "%02d", $0) }, selection: $wheelH, label: "时")
                TesseraLabel(":", style: .title3, color: TesseraColor.textTertiary)
                TesseraWheel((0..<60).map { String(format: "%02d", $0) }, selection: $wheelM, label: "分")
            }
            .frame(width: 220)
            TesseraDatePicker("出发日期", date: $day)
            TesseraTimePicker("提醒时间", hour: $hour, minute: $minute, step: 5)
            TesseraDateRangePicker("入住与离店", range: $range)
            TesseraColorPicker(hex: $color)

            section("表单")
            TesseraForm {
                TesseraFieldGroup("联系方式", description: "我们只在订单有变化时联系你") {
                    TesseraTextField("姓名", text: $name, error: form.error(for: "name", value: name, rules: [TesseraRules.required("姓名")]))
                    TesseraTextField("手机号", text: $phone, error: form.error(for: "phone", value: phone, rules: [TesseraRules.required("手机号"), TesseraRules.phone]))
                    TesseraTextField("邮箱", text: $mail, error: form.error(for: "mail", value: mail, rules: [TesseraRules.email]))
                }
                TesseraFormField("满意度", required: true, help: "点同一颗星可清除") { TesseraRating(value: $satisfaction) }
                Toggle("我已阅读并同意服务条款", isOn: $agree).toggleStyle(.tesseraCheckbox)
                Button("提交") { form.submit() }.buttonStyle(.tessera(fullWidth: true))
            }
        }
        .padding(TesseraSpace.s5)
        // 截图里展示提交后的状态（显示中文校验提示）
        .onAppear { form.submit() }
    }
}

#Preview("表单 · 拼贴") { TesseraFormsGallery() }
#Preview("表单 · 圆融 · 深色") { TesseraFormsGallery().tesseraShape(.round).preferredColorScheme(.dark) }
#Preview("表单 · 流韵") { TesseraFormsGallery().tesseraShape(.flow, requireCapableDevice: false) }
