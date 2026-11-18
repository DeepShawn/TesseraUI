import SwiftUI
#if canImport(UIKit)
import UIKit
#endif

// MARK: - 文本框

/// 回车键类型
public enum TesseraSubmitKey: Sendable {
    case done, search, next, go
    var label: SubmitLabel { switch self { case .done: .done; case .search: .search; case .next: .next; case .go: .go } }
}

/// 文本框：凹槽；聚焦时底色变成砖面色，外圈 3pt 光晕。
///
/// - `onCommittedChange` 只在输入法完成组合（拼音选定汉字）后回调，适合做搜索、校验、字数统计；
///   `text` 绑定则随每次按键更新（包括拼音字母）。
/// - iOS / visionOS 上用 UITextField 实现，以便读取 `markedTextRange` 判断输入法状态。
public struct TesseraTextField: View {
    let label: String?
    @Binding var text: String
    let placeholder: String
    let help: String?
    let error: String?
    let icon: TesseraIconName?
    let isSecure: Bool
    let clearable: Bool
    let submitKey: TesseraSubmitKey
    let onCommittedChange: ((String) -> Void)?
    let onSubmit: (() -> Void)?
    @State private var focused = false
    @FocusState private var swiftUIFocused: Bool
    @Environment(\.tesseraShape) private var tesseraShape

    public init(
        _ label: String? = nil,
        text: Binding<String>,
        placeholder: String = "",
        help: String? = nil,
        error: String? = nil,
        icon: TesseraIconName? = nil,
        isSecure: Bool = false,
        clearable: Bool = false,
        submitKey: TesseraSubmitKey = .done,
        onCommittedChange: ((String) -> Void)? = nil,
        onSubmit: (() -> Void)? = nil
    ) {
        self.label = label
        self._text = text
        self.placeholder = placeholder
        self.help = help
        self.error = error
        self.icon = icon
        self.isSecure = isSecure
        self.clearable = clearable
        self.submitKey = submitKey
        self.onCommittedChange = onCommittedChange
        self.onSubmit = onSubmit
    }

    private var isFocused: Bool { focused || swiftUIFocused }

    public var body: some View {
        // 圆融：胶囊，没有凹槽和描边
        let radius = tesseraShape.radius(tile: TesseraRadius.md)
        let shape = RoundedRectangle(cornerRadius: radius, style: .continuous)
        VStack(alignment: .leading, spacing: 6) {
            if let label { TesseraLabel(label, style: .label) }
            HStack(spacing: TesseraSpace.s2) {
                if let icon { TesseraIcon(icon, size: 18).foregroundStyle(TesseraColor.textSecondary) }
                field
                if clearable && !text.isEmpty {
                    Button { text = ""; onCommittedChange?("") } label: { TesseraIcon(.close, size: 16) }
                        .buttonStyle(.plain)
                        .foregroundStyle(TesseraColor.textSecondary)
                        .frame(width: 28, height: 28)
                        .accessibilityLabel("清除")
                }
            }
            .padding(.horizontal, tesseraShape.isRound ? TesseraSpace.s4 : TesseraSpace.s3)
            .frame(minHeight: TesseraSize.md)
            .tesseraTile(isFocused ? TesseraColor.bgSurface : TesseraColor.fillDefault, style: isFocused ? .plain : .sunken, shape: shape)
            .tesseraFocusGlow(isFocused || error != nil, radius: min(radius, TesseraSize.md / 2), error: error != nil)
            .animation(.easeOut(duration: TesseraMotion.durationFast), value: isFocused)
            if let message = error ?? help {
                TesseraLabel(message, style: .caption, color: error != nil ? TesseraColor.dangerText : TesseraColor.textSecondary)
            }
        }
        .accessibilityElement(children: .contain)
    }

    @ViewBuilder private var field: some View {
        #if canImport(UIKit) && !os(watchOS)
        CommittedTextField(text: $text, focused: $focused, placeholder: placeholder, isSecure: isSecure,
                           returnKey: submitKey.returnKey,
                           onCommitted: onCommittedChange, onSubmit: onSubmit)
            .frame(minHeight: 22)
            .accessibilityLabel(label ?? placeholder)
        #else
        Group {
            if isSecure { SecureField(placeholder, text: $text) } else { TextField(placeholder, text: $text) }
        }
        .textFieldStyle(.plain)
        .tesseraText(.bodySm)
        .foregroundStyle(TesseraColor.textPrimary)
        .focused($swiftUIFocused)
        .submitLabel(submitKey.label)
        .onSubmit { onSubmit?() }
        // macOS 的输入法在选定之前不会更新绑定，这里的变化即为已提交的内容
        .onChange(of: text) { _, new in onCommittedChange?(new) }
        #endif
    }
}

#if canImport(UIKit) && !os(watchOS)
extension TesseraSubmitKey {
    var returnKey: UIReturnKeyType { switch self { case .done: .done; case .search: .search; case .next: .next; case .go: .go } }
}

/// UITextField 包装：把“输入法是否在组合中”暴露出来
struct CommittedTextField: UIViewRepresentable {
    @Binding var text: String
    @Binding var focused: Bool
    var placeholder: String
    var isSecure: Bool
    var returnKey: UIReturnKeyType
    var onCommitted: ((String) -> Void)?
    var onSubmit: (() -> Void)?

    func makeUIView(context: Context) -> UITextField {
        let f = UITextField()
        f.delegate = context.coordinator
        f.addTarget(context.coordinator, action: #selector(Coordinator.changed(_:)), for: .editingChanged)
        f.font = UIFontMetrics(forTextStyle: .subheadline).scaledFont(for: UIFont(name: TesseraFontFamily.text, size: 14) ?? .systemFont(ofSize: 14))
        f.adjustsFontForContentSizeCategory = true
        f.textColor = UIColor(TesseraColor.textPrimary)
        f.tintColor = UIColor(TesseraColor.accentSolid)
        f.setContentHuggingPriority(.defaultLow, for: .horizontal)
        f.setContentCompressionResistancePriority(.defaultLow, for: .horizontal)
        return f
    }

    func updateUIView(_ f: UITextField, context: Context) {
        context.coordinator.parent = self
        // 组合中不要覆盖文字，否则会打断输入法
        if f.markedTextRange == nil && f.text != text { f.text = text }
        f.isSecureTextEntry = isSecure
        f.returnKeyType = returnKey
        f.attributedPlaceholder = NSAttributedString(string: placeholder, attributes: [.foregroundColor: UIColor(TesseraColor.textTertiary)])
    }

    func makeCoordinator() -> Coordinator { Coordinator(self) }

    final class Coordinator: NSObject, UITextFieldDelegate {
        var parent: CommittedTextField
        init(_ parent: CommittedTextField) { self.parent = parent }

        @objc func changed(_ f: UITextField) {
            let value = f.text ?? ""
            parent.text = value
            if f.markedTextRange == nil { parent.onCommitted?(value) }
        }
        func textFieldDidBeginEditing(_ textField: UITextField) { parent.focused = true }
        func textFieldDidEndEditing(_ textField: UITextField) { parent.focused = false }
        func textFieldShouldReturn(_ textField: UITextField) -> Bool {
            // 组合中的回车由输入法用来选字，不提交
            if textField.markedTextRange != nil { return true }
            parent.onSubmit?()
            return true
        }
    }
}
#endif

// MARK: - 搜索框

public struct TesseraSearchField: View {
    @Binding var text: String
    let placeholder: String
    let onSearch: (String) -> Void
    let onCommittedChange: ((String) -> Void)?

    public init(text: Binding<String>, placeholder: String = "搜索", onCommittedChange: ((String) -> Void)? = nil, onSearch: @escaping (String) -> Void) {
        self._text = text
        self.placeholder = placeholder
        self.onSearch = onSearch
        self.onCommittedChange = onCommittedChange
    }

    public var body: some View {
        TesseraTextField(text: $text, placeholder: placeholder, icon: .search, clearable: true, submitKey: .search,
                         onCommittedChange: onCommittedChange, onSubmit: { onSearch(text) })
    }
}

// MARK: - 下拉选择

/// 下拉选择：触发框是 TesseraUI 的凹槽样式，弹出的选项列表使用系统菜单（保证读屏与键盘可用）
public struct TesseraSelect<Value: Hashable>: View {
    let label: String?
    @Binding var selection: Value
    let options: [(value: Value, title: String)]
    @Environment(\.tesseraShape) private var tesseraShape

    public init(_ label: String? = nil, selection: Binding<Value>, options: [(value: Value, title: String)]) {
        self.label = label
        self._selection = selection
        self.options = options
    }

    public var body: some View {
        let shape = RoundedRectangle(cornerRadius: tesseraShape.radius(tile: TesseraRadius.md), style: .continuous)
        let current = options.first { $0.value == selection }?.title ?? "请选择"
        VStack(alignment: .leading, spacing: 6) {
            if let label { TesseraLabel(label, style: .label) }
            Menu {
                Picker(label ?? "", selection: $selection) {
                    ForEach(options.indices, id: \.self) { i in Text(options[i].title).tag(options[i].value) }
                }
                .pickerStyle(.inline)
            } label: {
                HStack {
                    TesseraLabel(current, style: .bodySm)
                    Spacer(minLength: 0)
                    TesseraIcon(.chevronDown, size: 16).foregroundStyle(TesseraColor.textSecondary)
                }
                .padding(.horizontal, tesseraShape.isRound ? TesseraSpace.s4 : TesseraSpace.s3)
                .frame(height: TesseraSize.md)
                .tesseraTile(TesseraColor.fillDefault, style: .sunken, shape: shape)
                .contentShape(shape)
            }
            .buttonStyle(.plain)
            .accessibilityLabel(label ?? "选择")
            .accessibilityValue(current)
        }
    }
}
