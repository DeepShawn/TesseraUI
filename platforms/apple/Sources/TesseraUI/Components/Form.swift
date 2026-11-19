import SwiftUI

// MARK: - 校验

/// 校验规则：返回 nil 表示通过，否则返回中文提示（与 Web、Android 一致）
public typealias TesseraRule = (String) -> String?

public enum TesseraRules {
    public static func required(_ name: String) -> TesseraRule { { $0.trimmingCharacters(in: .whitespaces).isEmpty ? "请填写\(name)" : nil } }
    public static func minLength(_ n: Int) -> TesseraRule { { !$0.isEmpty && $0.count < n ? "至少 \(n) 个字" : nil } }
    public static func maxLength(_ n: Int) -> TesseraRule { { $0.count > n ? "最多 \(n) 个字，已超出 \($0.count - n) 个" : nil } }
    public static let email: TesseraRule = { v in
        !v.isEmpty && v.range(of: #"^[^@\s]+@[^@\s]+\.[^@\s]+$"#, options: .regularExpression) == nil ? "邮箱格式不正确" : nil
    }
    /// 中国大陆手机号：1 开头 11 位，允许空格和连字符分隔、全角数字
    public static let phone: TesseraRule = { v in
        let d = tesseraNormalizeDigits(v).filter { $0.isASCII && $0.isNumber }
        return !v.isEmpty && d.range(of: #"^1[3-9]\d{9}$"#, options: .regularExpression) == nil ? "请输入 11 位手机号" : nil
    }
}

/// 表单状态：每个字段用 error(for:value:rules:) 取要显示的错误；submit() 校验全部字段并显示错误。
/// 用户还没提交、也没离开过某个字段时，不提前显示错误。
@MainActor
public final class TesseraFormState: ObservableObject {
    @Published private var touched: Set<String> = []
    @Published private(set) var submitted = false
    private var rules: [String: (value: String, rules: [TesseraRule])] = [:]

    public init() {}

    public func error(for name: String, value: String, rules list: [TesseraRule]) -> String? {
        rules[name] = (value, list)
        guard submitted || touched.contains(name) else { return nil }
        return list.lazy.compactMap { $0(value) }.first
    }

    /// 字段失焦时调用：之后开始显示这个字段的错误
    public func touch(_ name: String) { touched.insert(name) }

    /// 校验全部字段；返回第一个出错字段的名字，全部通过返回 nil
    @discardableResult public func submit() -> String? {
        submitted = true
        return rules.keys.sorted().first { key in rules[key]!.rules.contains { $0(rules[key]!.value) != nil } }
    }
}

// MARK: - 表单与字段

/// 表单：字段之间 20pt 间距
public struct TesseraForm<Content: View>: View {
    let content: Content
    public init(@ViewBuilder content: () -> Content) { self.content = content() }
    public var body: some View { VStack(alignment: .leading, spacing: TesseraSpace.s5) { content } }
}

/// 表单字段：标签（必填时后面有一个“*”，读屏读作“必填”）+ 内容 + 帮助或错误提示
public struct TesseraFormField<Content: View>: View {
    let label: String
    let required: Bool
    let help: String?
    let error: String?
    let content: Content

    public init(_ label: String, required: Bool = false, help: String? = nil, error: String? = nil, @ViewBuilder content: () -> Content) {
        self.label = label
        self.required = required
        self.help = help
        self.error = error
        self.content = content()
    }

    public var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack(spacing: 0) {
                TesseraLabel(label, style: .label)
                if required { TesseraLabel(" *", style: .label, color: TesseraColor.dangerText).accessibilityLabel("必填") }
            }
            content
            if let msg = error ?? help {
                TesseraLabel(msg, style: .caption, color: error != nil ? TesseraColor.dangerText : TesseraColor.textSecondary)
            }
        }
        .accessibilityElement(children: .contain)
    }
}

/// 字段分组：一块带标题的面板（拼贴是带描边的砖面，圆融 / 流韵是大圆角无描边）
public struct TesseraFieldGroup<Content: View>: View {
    let title: String
    let description: String?
    let content: Content
    @Environment(\.tesseraShape) private var tesseraShape
    @Environment(\.colorSchemeContrast) private var contrast

    public init(_ title: String, description: String? = nil, @ViewBuilder content: () -> Content) {
        self.title = title
        self.description = description
        self.content = content()
    }

    public var body: some View {
        let shape = RoundedRectangle(cornerRadius: tesseraShape.isRound ? tesseraShape.roundRadius(TesseraRound.card) : TesseraRadius.lg, style: .continuous)
        VStack(alignment: .leading, spacing: TesseraSpace.s4) {
            VStack(alignment: .leading, spacing: TesseraSpace.s1) {
                TesseraLabel(title, style: .title3).accessibilityAddTraits(.isHeader)
                if let description { TesseraLabel(description, style: .caption, color: TesseraColor.textSecondary) }
            }
            content
        }
        .padding(TesseraSpace.s5)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(shape.fill(TesseraColor.bgSurface))
        .overlay { if tesseraShape.showsOutline(contrast) { shape.strokeBorder(TesseraColor.borderSubtle, lineWidth: 1) } }
    }
}
