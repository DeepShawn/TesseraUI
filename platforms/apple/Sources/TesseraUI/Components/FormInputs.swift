import SwiftUI
#if canImport(UIKit)
import UIKit
#endif

/// 全角数字、全角小数点与负号转为半角（中文输入法下常见）
func tesseraNormalizeDigits(_ s: String) -> String {
    String(s.map { ch -> Character in
        if let v = ch.unicodeScalars.first?.value, (0xFF10...0xFF19).contains(v) { return Character(UnicodeScalar(v - 0xFF10 + 0x30)!) }
        switch ch { case "．", "。": return "."; case "－", "—", "−": return "-"; default: return ch }
    })
}

// MARK: - 多行文本

/// 多行文本框：minLines 行起，随内容长高。maxLength 不为 nil 时右下角显示字数，
/// 字数只在输入法选定后更新（打拼音时不计数）。iOS 上用 UITextView 读取 markedTextRange。
public struct TesseraTextArea: View {
    let label: String?
    @Binding var text: String
    let placeholder: String
    let help: String?
    let error: String?
    let maxLength: Int?
    let minLines: Int
    @State private var committed: Int
    @State private var focused = false
    @State private var height: CGFloat = 0
    @Environment(\.tesseraShape) private var tesseraShape

    public init(_ label: String? = nil, text: Binding<String>, placeholder: String = "", help: String? = nil, error: String? = nil, maxLength: Int? = nil, minLines: Int = 3) {
        self.label = label
        self._text = text
        self.placeholder = placeholder
        self.help = help
        self.error = error
        self.maxLength = maxLength
        self.minLines = minLines
        self._committed = State(initialValue: text.wrappedValue.count)
    }

    public var body: some View {
        let radius = tesseraShape.radius(tile: TesseraRadius.lg, round: TesseraRound.row)
        let shape = RoundedRectangle(cornerRadius: radius, style: .continuous)
        let minH = CGFloat(minLines) * 22 + TesseraSpace.s3 * 2
        VStack(alignment: .leading, spacing: 6) {
            if let label { TesseraLabel(label, style: .label) }
            ZStack(alignment: .topLeading) {
                if text.isEmpty {
                    TesseraLabel(placeholder, style: .bodySm, color: TesseraColor.textTertiary)
                        .padding(.vertical, TesseraSpace.s3)
                        .accessibilityHidden(true)
                }
                editor
                    .frame(minHeight: minH - TesseraSpace.s3 * 2)
                    .padding(.vertical, TesseraSpace.s3)
            }
            .padding(.horizontal, tesseraShape.isRound ? TesseraSpace.s4 : TesseraSpace.s3)
            .frame(minHeight: minH, alignment: .topLeading)
            .tesseraTile(focused ? TesseraColor.bgSurface : TesseraColor.fillDefault, style: focused ? .plain : .sunken, shape: shape)
            .tesseraFocusGlow(focused || error != nil, radius: radius, error: error != nil)
            .animation(.easeOut(duration: TesseraMotion.durationFast), value: focused)
            HStack {
                if let message = error ?? help {
                    TesseraLabel(message, style: .caption, color: error != nil ? TesseraColor.dangerText : TesseraColor.textSecondary)
                }
                Spacer(minLength: 0)
                if let maxLength {
                    TesseraLabel("\(committed) / \(maxLength)", style: .caption, color: committed > maxLength ? TesseraColor.dangerText : TesseraColor.textSecondary)
                        .accessibilityLabel("已输入 \(committed) 字，最多 \(maxLength) 字")
                }
            }
        }
    }

    @ViewBuilder private var editor: some View {
        #if canImport(UIKit) && !os(watchOS)
        CommittedTextView(text: $text, focused: $focused, onCommitted: { committed = $0.count })
            .accessibilityLabel(label ?? placeholder)
        #else
        TextEditor(text: $text)
            .scrollContentBackground(.hidden)
            .tesseraText(.bodySm)
            .foregroundStyle(TesseraColor.textPrimary)
            .onChange(of: text) { _, new in committed = new.count }
        #endif
    }
}

#if canImport(UIKit) && !os(watchOS)
/// UITextView 包装：随内容长高，并暴露“输入法是否在组合中”
struct CommittedTextView: UIViewRepresentable {
    @Binding var text: String
    @Binding var focused: Bool
    var onCommitted: (String) -> Void

    func makeUIView(context: Context) -> UITextView {
        let v = UITextView()
        v.delegate = context.coordinator
        v.backgroundColor = .clear
        v.isScrollEnabled = false
        v.textContainerInset = .zero
        v.textContainer.lineFragmentPadding = 0
        v.font = UIFontMetrics(forTextStyle: .subheadline).scaledFont(for: UIFont(name: TesseraFontFamily.text, size: 14) ?? .systemFont(ofSize: 14))
        v.adjustsFontForContentSizeCategory = true
        v.textColor = UIColor(TesseraColor.textPrimary)
        v.tintColor = UIColor(TesseraColor.accentSolid)
        v.setContentCompressionResistancePriority(.defaultLow, for: .horizontal)
        return v
    }

    func updateUIView(_ v: UITextView, context: Context) {
        context.coordinator.parent = self
        if v.markedTextRange == nil && v.text != text { v.text = text }
    }

    func sizeThatFits(_ proposal: ProposedViewSize, uiView: UITextView, context: Context) -> CGSize? {
        let w = proposal.width ?? 300
        let s = uiView.sizeThatFits(CGSize(width: w, height: .greatestFiniteMagnitude))
        return CGSize(width: w, height: s.height)
    }

    func makeCoordinator() -> Coordinator { Coordinator(self) }

    final class Coordinator: NSObject, UITextViewDelegate {
        var parent: CommittedTextView
        init(_ parent: CommittedTextView) { self.parent = parent }
        func textViewDidChange(_ v: UITextView) {
            parent.text = v.text
            if v.markedTextRange == nil { parent.onCommitted(v.text) }
        }
        func textViewDidBeginEditing(_ v: UITextView) { parent.focused = true }
        func textViewDidEndEditing(_ v: UITextView) { parent.focused = false }
    }
}
#endif

// MARK: - 密码框

/// 密码框：右侧按钮切换明文显示。capsLock 为 true 时显示“大写锁定已打开”（iPad / Mac 外接键盘时由应用传入）
public struct TesseraPasswordField: View {
    let label: String?
    @Binding var text: String
    let placeholder: String
    let help: String?
    let error: String?
    let capsLock: Bool
    let onSubmit: (() -> Void)?
    @State private var visible = false

    public init(_ label: String? = "密码", text: Binding<String>, placeholder: String = "", help: String? = nil, error: String? = nil, capsLock: Bool = false, onSubmit: (() -> Void)? = nil) {
        self.label = label
        self._text = text
        self.placeholder = placeholder
        self.help = help
        self.error = error
        self.capsLock = capsLock
        self.onSubmit = onSubmit
    }

    public var body: some View {
        ZStack(alignment: .trailing) {
            TesseraTextField(label, text: $text, placeholder: placeholder, help: capsLock && error == nil ? "大写锁定已打开" : help, error: error,
                             icon: .lock, isSecure: !visible, submitKey: .go, onSubmit: onSubmit)
            Button { visible.toggle() } label: {
                TesseraIcon(visible ? .eyeOff : .eye, size: 18)
                    .foregroundStyle(TesseraColor.textSecondary)
                    .frame(width: TesseraSize.touchMin, height: TesseraSize.md)
                    .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel(visible ? "隐藏密码" : "显示密码")
            // 与输入框对齐：有标签时下移一行标签的高度
            .padding(.top, label == nil ? 0 : 26)
            .frame(maxHeight: .infinity, alignment: .top)
        }
    }
}

// MARK: - 数字步进

/// 数字步进：− [数值] + 三块砖拼在一起。可直接输入（全角数字自动转半角），提交时按 min / max / step 修正。
/// 按住 − / + 先停 0.4 秒，然后每 0.12 秒调一次，连续 10 次后加速到 0.04 秒。
public struct TesseraNumberInput: View {
    let label: String?
    @Binding var value: Double
    let range: ClosedRange<Double>
    let step: Double
    let decimals: Int
    @State private var text = ""
    @FocusState private var focused: Bool
    @State private var repeatTask: Task<Void, Never>?
    @Environment(\.tesseraShape) private var tesseraShape

    public init(_ label: String? = nil, value: Binding<Double>, range: ClosedRange<Double> = -Double.greatestFiniteMagnitude...Double.greatestFiniteMagnitude, step: Double = 1, decimals: Int = 0) {
        self.label = label
        self._value = value
        self.range = range
        self.step = step
        self.decimals = decimals
    }

    private func fmt(_ v: Double) -> String { decimals == 0 ? String(Int(v.rounded())) : String(format: "%.\(decimals)f", v) }
    private func fix(_ v: Double) -> Double { let s = step > 0 ? (v / step).rounded() * step : v; return min(max(s, range.lowerBound), range.upperBound) }
    @discardableResult private func bump(_ dir: Double) -> Bool {
        let next = fix(value + dir * step)
        guard next != value else { return false }
        value = next; text = fmt(next); return true
    }
    private func commit() {
        let v = Double(tesseraNormalizeDigits(text)) ?? value
        value = fix(v); text = fmt(value)
    }

    public var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            if let label { TesseraLabel(label, style: .label) }
            HStack(spacing: tesseraShape.groupGap) {
                stepButton(.minus, "减少", -1, .first, enabled: value > range.lowerBound)
                TextField("", text: $text)
                    .textFieldStyle(.plain)
                    .multilineTextAlignment(.center)
                    .tesseraText(.label)
                    .foregroundStyle(TesseraColor.textPrimary)
                    .focused($focused)
                    #if os(iOS)
                    .keyboardType(decimals > 0 || range.lowerBound < 0 ? .decimalPad : .numberPad)
                    #endif
                    .onSubmit(commit)
                    .onChange(of: focused) { _, f in if !f { commit() } }
                    .frame(width: 88, height: TesseraSize.md)
                    .tesseraTile(focused ? TesseraColor.bgSurface : TesseraColor.fillDefault, style: focused ? .plain : .sunken,
                                 shape: tesseraShape.groupShape(.middle, outer: TesseraRadius.md))
                    .accessibilityLabel(label ?? "数值")
                    .accessibilityValue(fmt(value))
                    .accessibilityAdjustableAction { d in if d == .increment { bump(1) } else { bump(-1) } }
                stepButton(.plus, "增加", 1, .last, enabled: value < range.upperBound)
            }
        }
        .onAppear { text = fmt(value) }
        .onChange(of: value) { _, v in if !focused { text = fmt(v) } }
        .tesseraHaptic(.selection, trigger: value)
    }

    private func stepButton(_ icon: TesseraIconName, _ name: String, _ dir: Double, _ position: TilePosition, enabled: Bool) -> some View {
        Button { bump(dir) } label: {
            TesseraIcon(icon, size: 18).foregroundStyle(TesseraColor.textPrimary)
                .frame(width: TesseraSize.md, height: TesseraSize.md)
                .tesseraTile(TesseraColor.fillDefault, style: .soft, shape: tesseraShape.groupShape(position, outer: TesseraRadius.md))
                .frame(width: TesseraSize.touchMin, height: TesseraSize.touchMin)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .disabled(!enabled)
        .opacity(enabled ? 1 : TesseraOpacity.disabled)
        .accessibilityLabel(name)
        // 按住连续调整
        .simultaneousGesture(LongPressGesture(minimumDuration: 0.4).onEnded { _ in
            repeatTask?.cancel()
            repeatTask = Task { @MainActor in
                var n = 0
                while !Task.isCancelled, bump(dir) {
                    try? await Task.sleep(nanoseconds: n < 10 ? 120_000_000 : 40_000_000)
                    n += 1
                }
            }
        })
        .simultaneousGesture(DragGesture(minimumDistance: 0).onEnded { _ in repeatTask?.cancel(); repeatTask = nil })
    }
}

// MARK: - 验证码

/// 验证码输入：一排凹槽格子，一个隐藏的输入框接收输入；iOS 上可从短信自动填充（oneTimeCode），也可直接粘贴整段文字
public struct TesseraOtpInput: View {
    @Binding var code: String
    let length: Int
    let error: String?
    let onComplete: ((String) -> Void)?
    @FocusState private var focused: Bool
    @State private var shake: CGFloat = 0
    @Environment(\.tesseraShape) private var tesseraShape
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    public init(code: Binding<String>, length: Int = 6, error: String? = nil, onComplete: ((String) -> Void)? = nil) {
        self._code = code
        self.length = length
        self.error = error
        self.onComplete = onComplete
    }

    public var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            ZStack {
                TextField("", text: Binding(get: { code }, set: { new in
                    let digits = String(tesseraNormalizeDigits(new).filter { $0.isASCII && $0.isNumber }.prefix(length))
                    if digits != code { code = digits; if digits.count == length { onComplete?(digits) } }
                }))
                .focused($focused)
                #if os(iOS)
                .keyboardType(.numberPad)
                .textContentType(.oneTimeCode)
                #endif
                .foregroundStyle(.clear)
                .tint(.clear)
                // 透明的输入框盖住整排格子：点哪里都能聚焦，读屏的范围就是整个验证码区域
                .frame(maxWidth: .infinity, minHeight: 56)
                .opacity(0.02)
                .accessibilityLabel("验证码，共 \(length) 位")
                .accessibilityValue("已输入 \(code.count) 位")
                HStack(spacing: TesseraSpace.s2) {
                    ForEach(0..<length, id: \.self) { i in cell(i) }
                }
                .allowsHitTesting(false)
                .accessibilityHidden(true)
            }
            .fixedSize(horizontal: true, vertical: false)
            .offset(x: shake)
            if let error { TesseraLabel(error, style: .caption, color: TesseraColor.dangerText) }
        }
        .onChange(of: error) { _, e in
            guard e != nil, !reduceMotion else { return }
            withAnimation(.easeInOut(duration: 0.05).repeatCount(5, autoreverses: true)) { shake = 6 }
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) { shake = 0 }
        }
    }

    private func cell(_ i: Int) -> some View {
        let chars = Array(code)
        let ch = i < chars.count ? String(chars[i]) : nil
        let active = focused && (i == chars.count || (i == length - 1 && chars.count == length))
        let radius = tesseraShape.radius(tile: TesseraRadius.md, round: 14)
        let shape = RoundedRectangle(cornerRadius: radius, style: .continuous)
        return ZStack {
            if let ch { Text(ch).tesseraText(TesseraTextStyle(size: 24, lineHeight: 28, weight: .semibold, family: .display, tracking: 0, relativeTo: .title2)).foregroundStyle(TesseraColor.textPrimary).transition(.scale(scale: 0.9).combined(with: .opacity)) }
            else if active { RoundedRectangle(cornerRadius: 1).fill(TesseraColor.accentSolid).frame(width: 2, height: 24) }
        }
        .frame(minWidth: 32, maxWidth: 48, minHeight: 56)
        .frame(width: 44)
        .tesseraTile(active ? TesseraColor.bgSurface : TesseraColor.fillDefault, style: active ? .plain : .sunken, shape: shape)
        .overlay { if error != nil { shape.strokeBorder(TesseraColor.dangerText, lineWidth: 1) } }
        .tesseraFocusGlow(active, radius: radius, error: error != nil)
        .animation(TesseraMotion.snappy, value: ch)
    }
}

// MARK: - 自动补全

/// 自动补全：输入框下方是候选列表；只在输入法选定后筛选（打拼音时候选不变，不会按拼音字母筛出“没有匹配”）
public struct TesseraAutocomplete: View {
    let label: String?
    @Binding var text: String
    let options: [String]
    let placeholder: String
    let icon: TesseraIconName?
    let maxItems: Int
    let onSelect: ((String) -> Void)?
    @State private var query = ""
    @State private var open = false
    @Environment(\.tesseraShape) private var tesseraShape

    public init(_ label: String? = nil, text: Binding<String>, options: [String], placeholder: String = "", icon: TesseraIconName? = nil, maxItems: Int = 6, onSelect: ((String) -> Void)? = nil) {
        self.label = label
        self._text = text
        self.options = options
        self.placeholder = placeholder
        self.icon = icon
        self.maxItems = maxItems
        self.onSelect = onSelect
    }

    private var matches: [String] {
        let q = query.trimmingCharacters(in: .whitespaces)
        return Array((q.isEmpty ? options : options.filter { $0.localizedCaseInsensitiveContains(q) }).prefix(maxItems))
    }

    public var body: some View {
        VStack(alignment: .leading, spacing: TesseraSpace.s1) {
            TesseraTextField(label, text: $text, placeholder: placeholder, icon: icon, clearable: true,
                             onCommittedChange: { query = $0; open = !$0.isEmpty },
                             onSubmit: { if let m = matches.first { pick(m) } })
            if open {
                VStack(alignment: .leading, spacing: 2) {
                    if matches.isEmpty {
                        TesseraLabel("没有匹配“\(query)”的结果", style: .bodySm, color: TesseraColor.textSecondary)
                            .padding(TesseraSpace.s3)
                    } else {
                        ForEach(matches, id: \.self) { m in
                            Button(m) { pick(m) }
                                .accessibilityAddTraits(m == text ? .isSelected : [])
                        }
                    }
                }
                .buttonStyle(TesseraMenuItemStyle())
                .padding(TesseraSpace.s1)
                .background {
                    RoundedRectangle(cornerRadius: tesseraShape.radius(tile: TesseraRadius.lg, round: TesseraRound.menu), style: .continuous)
                        .fill(TesseraColor.bgElevated)
                        .shadow(color: TesseraColor.shadowColor.opacity(0.16), radius: 12, y: 6)
                }
                .transition(.opacity.combined(with: .scale(scale: 0.98, anchor: .top)))
                .accessibilityElement(children: .contain)
                .accessibilityLabel("候选")
            }
        }
        .animation(TesseraMotion.smooth, value: open)
    }

    private func pick(_ m: String) {
        text = m; query = m; open = false
        onSelect?(m)
    }
}

// MARK: - 标签输入

/// 标签输入：已输入的标签是一排小砖。回车、逗号、顿号、分号添加（拼音组合中不会），重复的不添加；点标签上的 × 删除
public struct TesseraTagInput: View {
    let label: String?
    @Binding var tags: [String]
    let placeholder: String
    let maxTags: Int
    @State private var draft = ""
    @Environment(\.tesseraShape) private var tesseraShape

    public init(_ label: String? = nil, tags: Binding<[String]>, placeholder: String = "输入后回车添加", maxTags: Int = .max) {
        self.label = label
        self._tags = tags
        self.placeholder = placeholder
        self.maxTags = maxTags
    }

    private static let separators: Set<Character> = [",", "，", "、", ";", "；"]

    private func add(_ raw: String) {
        let t = raw.trimmingCharacters(in: .whitespaces)
        if !t.isEmpty && !tags.contains(t) && tags.count < maxTags { tags.append(t) }
    }

    public var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            if let label { TesseraLabel(label, style: .label) }
            TesseraFlowLayout(spacing: 6, lineSpacing: 6) {
                ForEach(tags, id: \.self) { t in chip(t) }
            }
            TesseraTextField(text: $draft, placeholder: placeholder,
                             onCommittedChange: { v in
                                 // 拼音选定后才切分：onCommittedChange 只在组合结束时回调
                                 guard v.contains(where: { Self.separators.contains($0) }) else { return }
                                 let parts = v.split(omittingEmptySubsequences: false) { Self.separators.contains($0) }.map(String.init)
                                 parts.dropLast().forEach(add)
                                 draft = parts.last ?? ""
                             },
                             onSubmit: { add(draft); draft = "" })
                .accessibilityLabel(label ?? "标签")
                .accessibilityValue(tags.isEmpty ? "没有标签" : "已有 \(tags.count) 个标签：\(tags.joined(separator: "、"))")
        }
        .animation(TesseraMotion.snappy, value: tags)
        .tesseraHaptic(.light, trigger: tags.count) { old, new in new > old }
    }

    private func chip(_ t: String) -> some View {
        HStack(spacing: 0) {
            TesseraLabel(t, style: TesseraTextStyle(size: 12, lineHeight: 16, weight: .semibold, family: .text, tracking: 0, relativeTo: .caption), color: TesseraColor.accentText)
                .padding(.leading, 10)
            Button { tags.removeAll { $0 == t } } label: {
                // 看得见的 28，可点范围 40×40（负边距伸出去，不改变标签的大小）
                TesseraIcon(.close, size: 14).foregroundStyle(TesseraColor.accentText)
                    .frame(width: 40, height: 40).contentShape(Rectangle())
            }
            .padding(-6)
            .buttonStyle(.plain)
            .accessibilityLabel("删除标签 \(t)")
        }
        .frame(height: 28)
        .background(RoundedRectangle(cornerRadius: tesseraShape.radius(tile: 8), style: .continuous).fill(TesseraColor.accentSubtle))
        .transition(.scale(scale: 0.8).combined(with: .opacity))
    }
}
