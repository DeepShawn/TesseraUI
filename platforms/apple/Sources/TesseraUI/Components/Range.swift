import SwiftUI
import UniformTypeIdentifiers

// MARK: - 滑块

private let thumbSize: CGFloat = 22
private let trackHeight: CGFloat = 8

/// 滑块：凹槽轨道 + 釉色填充 + 白色小砖滑块；拖动时滑块放大、上方冒出数值气泡。
/// step > 0 时按步长吸附并画刻度（不超过 20 个）。读屏可上下滑动调整。
public struct TesseraSlider: View {
    let label: String?
    @Binding var value: Double
    let range: ClosedRange<Double>
    let step: Double
    let format: (Double) -> String

    public init(_ label: String? = nil, value: Binding<Double>, in range: ClosedRange<Double> = 0...100, step: Double = 0, format: @escaping (Double) -> String = { String(Int($0.rounded())) }) {
        self.label = label
        self._value = value
        self.range = range
        self.step = step
        self.format = format
    }

    public var body: some View {
        SliderCore(label: label, values: Binding(get: { [value] }, set: { value = $0[0] }), range: range, step: step, format: format)
    }
}

/// 范围滑块：两个滑块分别调整起点和终点，不能交叉
public struct TesseraRangeSlider: View {
    let label: String?
    @Binding var value: ClosedRange<Double>
    let range: ClosedRange<Double>
    let step: Double
    let format: (Double) -> String

    public init(_ label: String? = nil, value: Binding<ClosedRange<Double>>, in range: ClosedRange<Double> = 0...100, step: Double = 0, format: @escaping (Double) -> String = { String(Int($0.rounded())) }) {
        self.label = label
        self._value = value
        self.range = range
        self.step = step
        self.format = format
    }

    public var body: some View {
        SliderCore(label: label, values: Binding(get: { [value.lowerBound, value.upperBound] }, set: { value = $0[0]...$0[1] }), range: range, step: step, format: format)
    }
}

struct SliderCore: View {
    let label: String?
    @Binding var values: [Double]
    let range: ClosedRange<Double>
    let step: Double
    let format: (Double) -> String
    @State private var active: Int? = nil
    @Environment(\.tesseraShape) private var tesseraShape
    @Environment(\.isEnabled) private var isEnabled

    private var span: Double { max(range.upperBound - range.lowerBound, 1e-9) }
    private func snap(_ v: Double) -> Double {
        let c = min(max(v, range.lowerBound), range.upperBound)
        guard step > 0 else { return c }
        return min(max(range.lowerBound + ((c - range.lowerBound) / step).rounded() * step, range.lowerBound), range.upperBound)
    }
    private func set(_ i: Int, _ v: Double) {
        var list = values
        var nv = snap(v)
        if list.count == 2 { nv = i == 0 ? min(nv, list[1]) : max(nv, list[0]) }
        guard nv != list[i] else { return }
        list[i] = nv
        values = list
    }
    private func name(_ i: Int) -> String {
        if values.count == 1 { return label ?? "滑块" }
        return "\(label ?? "范围")\(i == 0 ? "起点" : "终点")"
    }

    var body: some View {
        VStack(alignment: .leading, spacing: TesseraSpace.s2) {
            if let label {
                HStack {
                    TesseraLabel(label, style: .label)
                    Spacer()
                    TesseraLabel(values.map(format).joined(separator: " – "), style: .label, color: TesseraColor.textSecondary)
                }
                .accessibilityHidden(true)
            }
            GeometryReader { g in
                let usable = g.size.width - thumbSize
                let fracs = values.map { CGFloat(($0 - range.lowerBound) / span) }
                ZStack(alignment: .leading) {
                    // 轨道与填充
                    Capsule().fill(TesseraColor.fillControlOff)
                        .overlay(alignment: .top) { if !tesseraShape.isRound { LinearGradient(colors: [TesseraColor.edgeSoft, .clear], startPoint: .top, endPoint: .bottom).frame(height: 3).clipShape(Capsule()) } }
                        .frame(height: trackHeight)
                        .padding(.horizontal, thumbSize / 2)
                    let start = fracs.count == 2 ? fracs[0] : 0
                    Capsule().fill(TesseraColor.accentControl)
                        .frame(width: max(0, usable * (fracs.last! - start)), height: trackHeight)
                        .offset(x: thumbSize / 2 + usable * start)
                    if step > 0, Int((span / step).rounded()) <= 20 {
                        let n = Int((span / step).rounded())
                        ForEach(0...n, id: \.self) { k in
                            let f = CGFloat(k) / CGFloat(n)
                            Circle().fill(f >= start && f <= fracs.last! ? Color.white.opacity(0.55) : TesseraColor.textTertiary.opacity(0.6))
                                .frame(width: 2, height: 2)
                                .offset(x: thumbSize / 2 + usable * f - 1)
                        }
                    }
                    ForEach(values.indices, id: \.self) { i in
                        thumb(i)
                            .offset(x: usable * fracs[i])
                    }
                }
                .frame(height: TesseraSize.touchMin)
                .contentShape(Rectangle())
                .gesture(
                    DragGesture(minimumDistance: 0)
                        .onChanged { d in
                            let v = range.lowerBound + Double(min(max((d.location.x - thumbSize / 2) / usable, 0), 1)) * span
                            if active == nil {
                                active = values.count == 1 ? 0 : (abs(values[0] - v) <= abs(values[1] - v) ? 0 : 1)
                            }
                            set(active!, v)
                        }
                        .onEnded { _ in active = nil }
                )
            }
            .frame(height: TesseraSize.touchMin)
        }
        .opacity(isEnabled ? 1 : TesseraOpacity.disabled)
        .tesseraHaptic(.selection, trigger: values) { _, _ in step > 0 }
    }

    private func thumb(_ i: Int) -> some View {
        let shape = tesseraShape.isRound ? AnyShape(Circle()) : AnyShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
        let dragging = active == i
        return shape
            .fill(Color.white)
            .overlay(shape.stroke(TesseraColor.borderField, lineWidth: 1))
            .shadow(color: TesseraColor.shadowColor.opacity(0.2), radius: 3, y: 1)
            .frame(width: thumbSize, height: thumbSize)
            .scaleEffect(dragging ? 1.12 : 1)
            .overlay(alignment: .top) {
                // 数值气泡
                Text(format(values[i]))
                    .tesseraText(TesseraTextStyle(size: 12, lineHeight: 16, weight: .semibold, family: .text, tracking: 0, relativeTo: .caption))
                    .foregroundStyle(TesseraColor.textInverse)
                    .padding(.horizontal, 8).padding(.vertical, 4)
                    .background(RoundedRectangle(cornerRadius: 8, style: .continuous).fill(TesseraColor.bgInverse))
                    .fixedSize()
                    .offset(y: dragging ? -34 : -30)
                    .opacity(dragging ? 1 : 0)
                    .scaleEffect(dragging ? 1 : 0.9)
                    .accessibilityHidden(true)
            }
            .animation(TesseraMotion.snappy, value: dragging)
            .accessibilityElement()
            .accessibilityLabel(name(i))
            .accessibilityValue(format(values[i]))
            .accessibilityAdjustableAction { d in
                let delta = step > 0 ? step : span / 100
                set(i, values[i] + (d == .increment ? delta : -delta))
            }
    }
}

// MARK: - 评分

/// 评分：一排星星，点中的星星弹一下；readOnly 时只显示（读屏读作“4 星，共 5 星”），点同一颗星可清零
public struct TesseraRating: View {
    @Binding var value: Int
    let max: Int
    let readOnly: Bool
    let allowClear: Bool
    let label: String
    @State private var popped: Int? = nil

    public init(value: Binding<Int>, max: Int = 5, readOnly: Bool = false, allowClear: Bool = true, label: String = "评分") {
        self._value = value
        self.max = max
        self.readOnly = readOnly
        self.allowClear = allowClear
        self.label = label
    }

    public var body: some View {
        HStack(spacing: 2) {
            ForEach(1...max, id: \.self) { i in
                let on = i <= value
                let star = TesseraIcon(on ? .starFill : .star, size: 24)
                    .foregroundStyle(on ? TesseraColor.pigmentOchre : TesseraColor.fillControlOff)
                    .scaleEffect(popped == i ? 1.25 : 1)
                if readOnly {
                    star.frame(width: 28, height: 28)
                } else {
                    Button {
                        value = allowClear && value == i ? 0 : i
                        popped = i
                        withAnimation(TesseraMotion.bouncy.delay(0.05)) { popped = nil }
                    } label: { star.frame(width: TesseraSize.touchMin, height: TesseraSize.touchMin).contentShape(Rectangle()) }
                    .buttonStyle(.plain)
                    .accessibilityLabel("\(i) 星")
                    .accessibilityAddTraits(i == value ? .isSelected : [])
                }
            }
        }
        .accessibilityElement(children: readOnly ? .ignore : .contain)
        .accessibilityLabel(readOnly ? "\(label)：\(value) 星，共 \(max) 星" : label)
        .tesseraHaptic(.selection, trigger: value)
    }
}

// MARK: - 文件上传

public struct TesseraUploadFile: Identifiable, Hashable, Sendable {
    public let id: UUID
    public var name: String
    public var size: Int64
    public var progress: Double?
    public var error: String?
    public init(id: UUID = UUID(), name: String, size: Int64, progress: Double? = nil, error: String? = nil) {
        self.id = id; self.name = name; self.size = size; self.progress = progress; self.error = error
    }
}

/// 文件上传：虚线框的凹槽区域（点击打开系统文件选择器）+ 文件列表（每个文件一块砖，带进度和删除）
public struct TesseraFileUpload: View {
    @Binding var files: [TesseraUploadFile]
    let types: [UTType]
    let multiple: Bool
    let title: String
    let hint: String?
    let onPick: ([URL]) -> Void
    @State private var importing = false
    @Environment(\.tesseraShape) private var tesseraShape

    public init(files: Binding<[TesseraUploadFile]>, types: [UTType] = [.item], multiple: Bool = true, title: String = "点击选择文件",
                hint: String? = "支持 PDF、图片，单个不超过 20 MB", onPick: @escaping ([URL]) -> Void) {
        self._files = files
        self.types = types
        self.multiple = multiple
        self.title = title
        self.hint = hint
        self.onPick = onPick
    }

    public var body: some View {
        let radius = tesseraShape.radius(tile: TesseraRadius.lg, round: TesseraRound.row)
        VStack(spacing: TesseraSpace.s3) {
            Button { importing = true } label: {
                VStack(spacing: TesseraSpace.s2) {
                    TesseraIcon(.upload, size: 32).foregroundStyle(TesseraColor.accentText)
                    TesseraLabel(title, style: .label)
                    if let hint { TesseraLabel(hint, style: .caption, color: TesseraColor.textSecondary) }
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical, 32)
                .padding(.horizontal, 24)
                .tesseraTile(TesseraColor.fillDefault, style: .sunken, shape: RoundedRectangle(cornerRadius: radius, style: .continuous))
                .overlay {
                    RoundedRectangle(cornerRadius: max(radius - 8, 4), style: .continuous)
                        .strokeBorder(TesseraColor.borderStrong, style: StrokeStyle(lineWidth: 1.5, dash: [6, 4]))
                        .padding(8)
                }
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel(title)
            .accessibilityHint(hint ?? "")
            .fileImporter(isPresented: $importing, allowedContentTypes: types, allowsMultipleSelection: multiple) { result in
                if case let .success(urls) = result { onPick(urls) }
            }
            if !files.isEmpty {
                TesseraList(files) { f, position in
                    TesseraListItem(f.name, subtitle: status(f), position: position) {
                        TesseraIcon(f.error != nil ? .error : .fileText, size: 22).foregroundStyle(f.error != nil ? TesseraColor.dangerText : TesseraColor.accentText)
                    } trailing: {
                        Button { files.removeAll { $0.id == f.id } } label: {
                            TesseraIcon(.close, size: 18).foregroundStyle(TesseraColor.textSecondary)
                                .frame(width: TesseraSize.touchMin, height: TesseraSize.touchMin).contentShape(Rectangle())
                        }
                        .buttonStyle(.plain)
                        .accessibilityLabel("移除 \(f.name)")
                    }
                }
            }
        }
    }

    private func status(_ f: TesseraUploadFile) -> String {
        if let e = f.error { return e }
        if let p = f.progress { return "上传中 \(Int(p * 100))%" }
        return ByteCountFormatter.string(fromByteCount: f.size, countStyle: .file)
    }
}
