import SwiftUI

// MARK: - 日期工具

/// 公历、周一开始（中国习惯）
let tesseraCalendar: Calendar = {
    var c = Calendar(identifier: .gregorian)
    c.firstWeekday = 2
    c.locale = Locale(identifier: "zh_CN")
    return c
}()

private let weekTitles = ["一", "二", "三", "四", "五", "六", "日"]

extension Date {
    /// “2026 年 11 月 19 日”
    var tesseraTitle: String {
        let c = tesseraCalendar.dateComponents([.year, .month, .day], from: self)
        return "\(c.year!) 年 \(c.month!) 月 \(c.day!) 日"
    }
    var tesseraStartOfMonth: Date { tesseraCalendar.date(from: tesseraCalendar.dateComponents([.year, .month], from: self))! }
}

// MARK: - 日历

/// 日历：日期是一块块小砖；单选时选中砖用弹簧滑到新日期，换月时整月推入推出。
/// 单选传 selection；范围选择传 range（第一次点起点，第二次点终点，先晚后早会自动调换）。
public struct TesseraCalendar: View {
    @Binding var selection: Date?
    @Binding var range: ClosedRange<Date>?
    let isRange: Bool
    let minDate: Date?
    let maxDate: Date?
    let today: Date
    @State private var month: Date
    @State private var forward = true
    @State private var pendingStart: Date? = nil
    @Namespace private var ns
    @Environment(\.tesseraShape) private var tesseraShape
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    /// 单选
    public init(selection: Binding<Date?>, min: Date? = nil, max: Date? = nil, today: Date = Date()) {
        self._selection = selection
        self._range = .constant(nil)
        self.isRange = false
        self.minDate = min
        self.maxDate = max
        self.today = today
        self._month = State(initialValue: (selection.wrappedValue ?? today).tesseraStartOfMonth)
    }

    /// 范围选择
    public init(range: Binding<ClosedRange<Date>?>, min: Date? = nil, max: Date? = nil, today: Date = Date()) {
        self._selection = .constant(nil)
        self._range = range
        self.isRange = true
        self.minDate = min
        self.maxDate = max
        self.today = today
        self._month = State(initialValue: (range.wrappedValue?.lowerBound ?? today).tesseraStartOfMonth)
    }

    private func go(_ delta: Int) {
        forward = delta > 0
        withAnimation(.tessera(TesseraMotion.smooth, reduceMotion: reduceMotion)) {
            month = tesseraCalendar.date(byAdding: .month, value: delta, to: month)!
        }
    }

    public var body: some View {
        let c = tesseraCalendar.dateComponents([.year, .month], from: month)
        VStack(spacing: TesseraSpace.s2) {
            HStack {
                TesseraIconButton(.chevronLeft, label: "上个月") { go(-1) }
                TesseraLabel("\(c.year!) 年 \(c.month!) 月", style: .title3)
                    .padding(.leading, TesseraSpace.s2)
                    .accessibilityAddTraits(.isHeader)
                Spacer()
                TesseraIconButton(.chevronRight, label: "下个月") { go(1) }
            }
            HStack(spacing: 3) {
                ForEach(weekTitles, id: \.self) { w in
                    TesseraLabel(w, style: .caption, color: TesseraColor.textTertiary).frame(maxWidth: .infinity, minHeight: 28)
                }
            }
            .accessibilityHidden(true)
            grid(for: month)
                .id(month)
                .transition(.push(from: forward ? .trailing : .leading))
        }
        .frame(width: 308)
        .clipped()
    }

    private func grid(for month: Date) -> some View {
        let weekday = tesseraCalendar.component(.weekday, from: month) // 1 = 周日
        let offset = (weekday + 5) % 7
        let origin = tesseraCalendar.date(byAdding: .day, value: -offset, to: month)!
        let days = (0..<42).map { tesseraCalendar.date(byAdding: .day, value: $0, to: origin)! }
        return LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 3), count: 7), spacing: 3) {
            ForEach(days, id: \.self) { d in dayCell(d, inMonth: tesseraCalendar.isDate(d, equalTo: month, toGranularity: .month)) }
        }
    }

    private func same(_ a: Date?, _ b: Date) -> Bool { a.map { tesseraCalendar.isDate($0, inSameDayAs: b) } ?? false }

    private func dayCell(_ d: Date, inMonth: Bool) -> some View {
        let round = tesseraShape.isRound
        let lo = range?.lowerBound ?? pendingStart
        let hi = range?.upperBound
        let isSel = (!isRange && same(selection, d)) || same(lo, d) || same(hi, d)
        let inRange = isRange && lo != nil && hi != nil && d > tesseraCalendar.startOfDay(for: lo!) && !same(lo, d) && d < hi! && !same(hi, d)
        let disabled = (minDate.map { d < tesseraCalendar.startOfDay(for: $0) } ?? false) || (maxDate.map { d > $0 } ?? false)
        let weekend = [1, 7].contains(tesseraCalendar.component(.weekday, from: d))
        let isToday = tesseraCalendar.isDate(d, inSameDayAs: today)
        let fg: Color = isSel ? TesseraColor.bgSurface : inRange ? TesseraColor.accentText : !inMonth ? TesseraColor.textTertiary : weekend ? TesseraColor.textSecondary : TesseraColor.textPrimary
        let cellShape = round ? AnyShape(Circle()) : AnyShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
        return Button {
            pick(d, inMonth: inMonth)
        } label: {
            ZStack {
                if inRange { Rectangle().fill(TesseraColor.accentSubtle).padding(.horizontal, round ? -1.5 : 0).clipShape(round ? AnyShape(Rectangle()) : AnyShape(RoundedRectangle(cornerRadius: 6, style: .continuous))) }
                if isSel && inMonth {
                    // 选中砖：单选时用 matchedGeometryEffect 从旧日期滑过来
                    cellShape.fill(TesseraColor.textPrimary)
                        .matchedGeometryEffect(id: isRange ? "sel-\(same(lo, d) ? "a" : "b")" : "sel", in: ns)
                }
                Text("\(tesseraCalendar.component(.day, from: d))")
                    .tesseraText(isSel ? TesseraTextStyle(size: 14, lineHeight: 20, weight: .semibold, family: .text, tracking: 0, relativeTo: .subheadline) : .bodySm)
                    .foregroundStyle(disabled ? fg.opacity(0.3) : fg)
                if isToday {
                    Circle().fill(isSel ? TesseraColor.bgSurface : TesseraColor.accentText).frame(width: 4, height: 4)
                        .frame(maxHeight: .infinity, alignment: .bottom).padding(.bottom, 5)
                }
            }
            .aspectRatio(1, contentMode: .fit)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .disabled(disabled)
        .accessibilityLabel("\(d.tesseraTitle)，星期\(weekTitles[(tesseraCalendar.component(.weekday, from: d) + 5) % 7])\(isToday ? "，今天" : "")")
        .accessibilityAddTraits(isSel ? .isSelected : [])
    }

    private func pick(_ d: Date, inMonth: Bool) {
        withAnimation(.tessera(.spring(duration: 0.35, bounce: 0.2), reduceMotion: reduceMotion)) {
            if isRange {
                if let s = pendingStart {
                    range = d < s ? d...s : s...d
                    pendingStart = nil
                } else {
                    pendingStart = d
                    range = nil
                }
            } else {
                selection = d
            }
        }
        if !inMonth { go(d > month ? 1 : -1) }
    }
}

// MARK: - 滚轮

/// 滚轮的一列：与 Web、Android 同一套手感。
/// 拖动 1:1 跟手、拉出两端有橡皮筋阻力；松手按速度投射到最近一行，带着手指的速度进入略欠阻尼的弹簧，落定前轻轻回弹。
/// 选项沿圆柱排开：每行 18°，越远越倾斜、越淡。
public struct TesseraWheel: View {
    let items: [String]
    @Binding var selection: Int
    let label: String
    @State private var pos: CGFloat? = nil
    @State private var dragStart: CGFloat? = nil
    @Environment(\.tesseraShape) private var tesseraShape
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    private let row: CGFloat = 36

    public init(_ items: [String], selection: Binding<Int>, label: String = "滚轮") {
        self.items = items
        self._selection = selection
        self.label = label
    }

    private var maxPos: CGFloat { CGFloat(max(items.count - 1, 0)) * row }
    /// 上下边缘淡出
    private static let fade = LinearGradient(stops: [.init(color: .clear, location: 0), .init(color: .black, location: 0.28), .init(color: .black, location: 0.72), .init(color: .clear, location: 1)], startPoint: .top, endPoint: .bottom)

    public var body: some View {
        let p = pos ?? CGFloat(selection) * row
        let center = p / row
        ZStack {
            ForEach(visible(center), id: \.self) { i in
                WheelRow(text: items[i], offset: Double(i) - Double(center), selected: i == Int(center.rounded()), row: row)
            }
        }
        .frame(maxWidth: .infinity)
        .frame(height: row * 5)
        .clipped()
        .mask(Self.fade)
        .contentShape(Rectangle())
        .gesture(
            DragGesture(minimumDistance: 2)
                .onChanged { g in
                    let start = dragStart ?? (pos ?? CGFloat(selection) * row)
                    if dragStart == nil { dragStart = start }
                    let raw = start - g.translation.height
                    // 橡皮筋
                    let h = row * 5
                    func rubber(_ over: CGFloat) -> CGFloat { (1 - 1 / (abs(over) * 0.55 / h + 1)) * h * (over < 0 ? -1 : 1) }
                    pos = raw < 0 ? rubber(raw) : raw > maxPos ? maxPos + rubber(raw - maxPos) : raw
                }
                .onEnded { g in
                    let start = dragStart ?? 0
                    dragStart = nil
                    // 系统给出的预测落点已经包含惯性
                    let projected = start - g.predictedEndTranslation.height
                    settle(Int((projected / row).rounded()))
                }
        )
        .onTapGesture { location in settle(Int(((p + location.y - row * 2.5) / row).rounded())) }
        .accessibilityElement()
        .accessibilityLabel(label)
        .accessibilityValue(items.indices.contains(selection) ? items[selection] : "")
        .accessibilityAdjustableAction { d in settle(selection + (d == .increment ? 1 : -1)) }
        .tesseraHaptic(.selection, trigger: Int(center.rounded()))
    }

    private func visible(_ center: CGFloat) -> [Int] {
        guard !items.isEmpty else { return [] }
        let from = max(0, Int((center - 4.5).rounded(.down))), to = min(items.count - 1, Int((center + 4.5).rounded(.up)))
        return from <= to ? Array(from...to) : []
    }

    private func settle(_ index: Int) {
        let i = min(max(index, 0), items.count - 1)
        withAnimation(reduceMotion ? .easeOut(duration: TesseraMotion.durationFast) : .spring(response: 0.48, dampingFraction: 0.7)) {
            pos = CGFloat(i) * row
        }
        selection = i
    }
}

/// 滚轮里的一行：沿圆柱排开（每行 18°），越远越倾斜、越淡
private struct WheelRow: View {
    let text: String
    let offset: Double
    let selected: Bool
    let row: CGFloat

    var body: some View {
        let step: Double = 18.0 * Double.pi / 180.0
        let a: Double = min(max(offset * step, -Double.pi / 2), Double.pi / 2)
        let radius: Double = Double(row) / step
        let y: CGFloat = CGFloat(radius * sin(a))
        let alpha: Double = pow(max(cos(a), 0), 1.6)
        let style: TesseraTextStyle = selected ? TesseraTextStyle(size: 16, lineHeight: 22, weight: .semibold, family: .text, tracking: 0, relativeTo: .body) : .body
        return Text(text)
            .tesseraText(style)
            .foregroundStyle(selected ? TesseraColor.textPrimary : TesseraColor.textSecondary)
            .lineLimit(1)
            .rotation3DEffect(.radians(-a), axis: (x: 1, y: 0, z: 0), perspective: 0.5)
            .offset(y: y)
            .opacity(alpha)
    }
}

/// 多列滚轮：中间一条凹槽选中带，列之间可放分隔符（例如时间的冒号）
public struct TesseraWheelPicker<Content: View>: View {
    let content: Content
    @Environment(\.tesseraShape) private var tesseraShape
    public init(@ViewBuilder content: () -> Content) { self.content = content() }
    public var body: some View {
        ZStack {
            RoundedRectangle(cornerRadius: tesseraShape.radius(tile: 12), style: .continuous)
                .fill(TesseraColor.fillDefault)
                .frame(height: 36)
                .tesseraTile(TesseraColor.fillDefault, style: .sunken, shape: RoundedRectangle(cornerRadius: tesseraShape.radius(tile: 12), style: .continuous))
            HStack(spacing: 4) { content }
        }
        .frame(height: 36 * 5)
    }
}

// MARK: - 日期 / 时间 / 日期范围选择器

private struct PickerField: View {
    let label: String?
    let text: String
    let placeholder: String
    let icon: TesseraIconName
    let action: () -> Void
    @Environment(\.tesseraShape) private var tesseraShape

    var body: some View {
        let shape = RoundedRectangle(cornerRadius: tesseraShape.radius(tile: TesseraRadius.md), style: .continuous)
        VStack(alignment: .leading, spacing: 6) {
            if let label { TesseraLabel(label, style: .label) }
            Button(action: action) {
                HStack(spacing: TesseraSpace.s2) {
                    TesseraIcon(icon, size: 18).foregroundStyle(TesseraColor.textSecondary)
                    TesseraLabel(text.isEmpty ? placeholder : text, style: .bodySm, color: text.isEmpty ? TesseraColor.textTertiary : TesseraColor.textPrimary).lineLimit(1)
                    Spacer(minLength: 0)
                }
                .padding(.horizontal, tesseraShape.isRound ? TesseraSpace.s4 : TesseraSpace.s3)
                .frame(height: TesseraSize.md)
                .tesseraTile(TesseraColor.fillDefault, style: .sunken, shape: shape)
                .contentShape(shape)
            }
            .buttonStyle(.plain)
            .accessibilityLabel(label ?? placeholder)
            .accessibilityValue(text.isEmpty ? "未选择" : text)
        }
    }
}

private struct SheetActions: View {
    let left: String
    let onLeft: () -> Void
    let onConfirm: () -> Void
    var body: some View {
        HStack {
            Button(left, action: onLeft).buttonStyle(.tessera(.secondary))
            Spacer()
            Button("确定", action: onConfirm).buttonStyle(.tessera())
        }
        .padding(.top, TesseraSpace.s4)
    }
}

/// 日期选择器：点开是底部抽屉里的年 / 月 / 日三列滚轮（换月份时“日”那一列跟着变）
public struct TesseraDatePicker: View {
    let label: String?
    @Binding var date: Date?
    let placeholder: String
    @State private var open = false
    @State private var y = 2026
    @State private var m = 1
    @State private var d = 1

    public init(_ label: String? = nil, date: Binding<Date?>, placeholder: String = "选择日期") {
        self.label = label
        self._date = date
        self.placeholder = placeholder
    }

    public var body: some View {
        PickerField(label: label, text: date?.tesseraTitle ?? "", placeholder: placeholder, icon: .calendar) {
            let c = tesseraCalendar.dateComponents([.year, .month, .day], from: date ?? Date())
            y = c.year!; m = c.month!; d = c.day!
            open = true
        }
        .tesseraSheet(isPresented: $open, title: "选择日期", detents: [.medium]) {
            let thisYear = tesseraCalendar.component(.year, from: Date())
            let years = Array((thisYear - 50)...(thisYear + 50))
            let days = tesseraCalendar.range(of: .day, in: .month, for: tesseraCalendar.date(from: DateComponents(year: y, month: m))!)!.count
            TesseraWheelPicker {
                TesseraWheel(years.map { "\($0) 年" }, selection: Binding(get: { years.firstIndex(of: y) ?? 0 }, set: { y = years[$0] }), label: "年")
                TesseraWheel((1...12).map { "\($0) 月" }, selection: Binding(get: { m - 1 }, set: { m = $0 + 1 }), label: "月")
                TesseraWheel((1...days).map { "\($0) 日" }, selection: Binding(get: { min(d, days) - 1 }, set: { d = $0 + 1 }), label: "日")
                    .id("days-\(days)")
            }
            SheetActions(left: "取消", onLeft: { open = false }) {
                date = tesseraCalendar.date(from: DateComponents(year: y, month: m, day: min(d, days)))
                open = false
            }
        }
    }
}

/// 时间选择器：时 / 分两列滚轮，分钟按 step 取整
public struct TesseraTimePicker: View {
    let label: String?
    @Binding var hour: Int?
    @Binding var minute: Int?
    let step: Int
    let placeholder: String
    @State private var open = false
    @State private var h = 9
    @State private var mi = 0

    public init(_ label: String? = nil, hour: Binding<Int?>, minute: Binding<Int?>, step: Int = 1, placeholder: String = "选择时间") {
        self.label = label
        self._hour = hour
        self._minute = minute
        self.step = step
        self.placeholder = placeholder
    }

    public var body: some View {
        let text = hour != nil && minute != nil ? String(format: "%02d:%02d", hour!, minute!) : ""
        PickerField(label: label, text: text, placeholder: placeholder, icon: .clock) {
            h = hour ?? 9; mi = ((minute ?? 0) / step) * step
            open = true
        }
        .tesseraSheet(isPresented: $open, title: "选择时间", detents: [.medium]) {
            let minutes = Array(stride(from: 0, to: 60, by: step))
            TesseraWheelPicker {
                TesseraWheel((0..<24).map { String(format: "%02d", $0) }, selection: $h, label: "时")
                TesseraLabel(":", style: .title3, color: TesseraColor.textTertiary)
                TesseraWheel(minutes.map { String(format: "%02d", $0) }, selection: Binding(get: { minutes.firstIndex(of: mi) ?? 0 }, set: { mi = minutes[$0] }), label: "分")
            }
            .padding(.horizontal, TesseraSpace.s8)
            SheetActions(left: "现在", onLeft: {
                let c = tesseraCalendar.dateComponents([.hour, .minute], from: Date())
                hour = c.hour; minute = (c.minute! / step) * step; open = false
            }) { hour = h; minute = mi; open = false }
        }
    }
}

/// 日期范围选择器：抽屉里是日历，第一次点起点、第二次点终点
public struct TesseraDateRangePicker: View {
    let label: String?
    @Binding var range: ClosedRange<Date>?
    let placeholder: String
    @State private var open = false
    @State private var draft: ClosedRange<Date>? = nil

    public init(_ label: String? = nil, range: Binding<ClosedRange<Date>?>, placeholder: String = "选择日期范围") {
        self.label = label
        self._range = range
        self.placeholder = placeholder
    }

    private func short(_ d: Date) -> String {
        let c = tesseraCalendar.dateComponents([.month, .day], from: d)
        return "\(c.month!) 月 \(c.day!) 日"
    }

    public var body: some View {
        let text = range.map { "\(short($0.lowerBound)) – \(short($0.upperBound))" } ?? ""
        PickerField(label: label, text: text, placeholder: placeholder, icon: .calendar) { draft = range; open = true }
            .tesseraSheet(isPresented: $open, title: "选择日期范围", detents: [.large]) {
                TesseraCalendar(range: $draft)
                let days = draft.map { (tesseraCalendar.dateComponents([.day], from: $0.lowerBound, to: $0.upperBound).day ?? 0) + 1 }
                TesseraLabel(days.map { "共 \($0) 天" } ?? "先选开始日期，再选结束日期", style: .caption, color: TesseraColor.textSecondary)
                    .padding(.top, TesseraSpace.s2)
                SheetActions(left: "清除", onLeft: { draft = nil }) { if let draft { range = draft; open = false } }
            }
    }
}

// MARK: - 颜色选择器

/// 颜色选择器：饱和度 / 明度面板 + 色相条 + HEX 输入 + 预设色（TesseraUI 的颜料色）
public struct TesseraColorPicker: View {
    @Binding var hex: String
    let swatches: [String]
    @State private var h: Double = 0
    @State private var s: Double = 1
    @State private var v: Double = 1
    @State private var field = ""
    @Environment(\.tesseraShape) private var tesseraShape

    public init(hex: Binding<String>, swatches: [String] = ["#0F6B63", "#C4472C", "#B88A2E", "#687A2E", "#2F4E9E", "#7A4E9C", "#1F1B16"]) {
        self._hex = hex
        self.swatches = swatches
    }

    public var body: some View {
        let panelShape = RoundedRectangle(cornerRadius: tesseraShape.radius(tile: TesseraRadius.md, round: 18), style: .continuous)
        VStack(alignment: .leading, spacing: TesseraSpace.s3) {
            GeometryReader { g in
                ZStack {
                    LinearGradient(colors: [.white, Color(hue: h / 360, saturation: 1, brightness: 1)], startPoint: .leading, endPoint: .trailing)
                    LinearGradient(colors: [.clear, .black], startPoint: .top, endPoint: .bottom)
                    Circle().strokeBorder(.white, lineWidth: 3).frame(width: 18, height: 18)
                        .position(x: s * g.size.width, y: (1 - v) * g.size.height)
                }
                .clipShape(panelShape)
                .contentShape(panelShape)
                .gesture(DragGesture(minimumDistance: 0).onChanged { d in
                    s = min(max(d.location.x / g.size.width, 0), 1)
                    v = 1 - min(max(d.location.y / g.size.height, 0), 1)
                    emit()
                })
            }
            .aspectRatio(1.6, contentMode: .fit)
            .accessibilityElement()
            .accessibilityLabel("饱和度与明度")
            .accessibilityValue("饱和度 \(Int(s * 100))%，明度 \(Int(v * 100))%")
            .accessibilityAdjustableAction { d in s = min(max(s + (d == .increment ? 0.05 : -0.05), 0), 1); emit() }
            GeometryReader { g in
                ZStack(alignment: .leading) {
                    Capsule().fill(LinearGradient(colors: (0...6).map { Color(hue: Double($0) / 6, saturation: 1, brightness: 1) }, startPoint: .leading, endPoint: .trailing))
                        .frame(height: 14)
                    Circle().fill(Color(hue: h / 360, saturation: 1, brightness: 1)).overlay(Circle().strokeBorder(.white, lineWidth: 3))
                        .frame(width: 20, height: 20)
                        .offset(x: min(max(h / 360 * g.size.width - 10, 0), g.size.width - 20))
                }
                .frame(maxHeight: .infinity)
                .contentShape(Rectangle())
                .gesture(DragGesture(minimumDistance: 0).onChanged { d in h = min(max(d.location.x / g.size.width, 0), 1) * 360; emit() })
            }
            .frame(height: TesseraSize.touchMin)
            .accessibilityElement()
            .accessibilityLabel("色相")
            .accessibilityValue("\(Int(h)) 度")
            .accessibilityAdjustableAction { d in h = min(max(h + (d == .increment ? 10 : -10), 0), 360); emit() }
            HStack(spacing: TesseraSpace.s2) {
                RoundedRectangle(cornerRadius: tesseraShape.radius(tile: TesseraRadius.md), style: .continuous)
                    .fill(Self.color(hex) ?? .clear)
                    .frame(width: TesseraSize.md, height: TesseraSize.md)
                    .accessibilityHidden(true)
                TesseraTextField(text: $field, placeholder: "#0F6B63", error: Self.color(field) == nil ? "请输入 6 位十六进制颜色，如 #0F6B63" : nil,
                                 onCommittedChange: { f in if Self.color(f) != nil { hex = Self.normalize(f); load() } })
            }
            HStack(spacing: TesseraSpace.s2) {
                ForEach(swatches, id: \.self) { sw in
                    let on = Self.normalize(sw) == Self.normalize(hex)
                    Button { hex = Self.normalize(sw); load() } label: {
                        RoundedRectangle(cornerRadius: tesseraShape.radius(tile: 9), style: .continuous)
                            .fill(Self.color(sw) ?? .clear)
                            .overlay { if on { RoundedRectangle(cornerRadius: tesseraShape.radius(tile: 9), style: .continuous).strokeBorder(TesseraColor.focusRing, lineWidth: 2) } }
                            .overlay { if on { TesseraIcon(.check, size: 16).foregroundStyle(.white) } }
                            .frame(width: 32, height: 32)
                            .frame(width: TesseraSize.touchMin, height: TesseraSize.touchMin)
                            .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel("颜色 \(Self.normalize(sw))")
                    .accessibilityAddTraits(on ? .isSelected : [])
                }
            }
        }
        .frame(width: 300)
        .onAppear(perform: load)
    }

    private func load() {
        field = Self.normalize(hex)
        guard let rgb = Self.rgb(hex) else { return }
        let mx = max(rgb.0, rgb.1, rgb.2), mn = min(rgb.0, rgb.1, rgb.2), d = mx - mn
        var hue = 0.0
        if d > 0 {
            if mx == rgb.0 { hue = ((rgb.1 - rgb.2) / d).truncatingRemainder(dividingBy: 6) }
            else if mx == rgb.1 { hue = (rgb.2 - rgb.0) / d + 2 }
            else { hue = (rgb.0 - rgb.1) / d + 4 }
        }
        h = (hue * 60 + 360).truncatingRemainder(dividingBy: 360)
        s = mx > 0 ? d / mx : 0
        v = mx
    }

    private func emit() {
        let f = { (n: Double) -> Double in let k = (n + h / 60).truncatingRemainder(dividingBy: 6); return v - v * s * max(0, min(k, 4 - k, 1)) }
        let rgb = [f(5), f(3), f(1)].map { Int(($0 * 255).rounded()) }
        hex = String(format: "#%02X%02X%02X", rgb[0], rgb[1], rgb[2])
        field = hex
    }

    static func normalize(_ s: String) -> String {
        var t = s.trimmingCharacters(in: .whitespaces).uppercased()
        if t.hasPrefix("#") { t.removeFirst() }
        if t.count == 3 { t = t.map { "\($0)\($0)" }.joined() }
        return "#" + t
    }
    static func rgb(_ s: String) -> (Double, Double, Double)? {
        let t = normalize(s).dropFirst()
        guard t.count == 6, let n = UInt32(t, radix: 16) else { return nil }
        return (Double((n >> 16) & 0xFF) / 255, Double((n >> 8) & 0xFF) / 255, Double(n & 0xFF) / 255)
    }
    static func color(_ s: String) -> Color? { rgb(s).map { Color(red: $0.0, green: $0.1, blue: $0.2) } }
}
