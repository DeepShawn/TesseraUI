import SwiftUI
#if canImport(UIKit)
import UIKit
#elseif canImport(AppKit)
import AppKit
#endif

// MARK: - 语法高亮（与 Web、Android 同一套规则）

public enum TesseraTokenKind: Sendable { case keyword, string, number, function, comment }

private let tesseraKeywords: [String: Set<String>] = {
    let js = "await async break case catch class const continue default delete do else export extends false finally for from function if import in instanceof let new null of return static super switch this throw true try typeof undefined var void while yield"
    let raw: [String: String] = [
        "js": js,
        "ts": js + " interface type enum implements private public readonly declare keyof as",
        "kotlin": "as break class continue do else false for fun if in interface is null object package return super this throw true try typealias typeof val var when while data sealed private public internal override suspend import",
        "swift": "as break case catch class continue default defer do else enum extension false for func guard if import in init let nil private protocol public return self static struct switch throw throws true try var while some any async await",
        "csharp": "abstract as async await base bool break case catch class const continue default do double else enum false finally for foreach if in int interface internal is namespace new null object out override private protected public readonly return sealed static string struct switch this throw true try using var void while",
        "python": "and as assert async await break class continue def del elif else except False finally for from global if import in is lambda None nonlocal not or pass raise return True try while with yield",
        "json": "true false null",
    ]
    return raw.mapValues { Set($0.split(separator: " ").map(String.init)) }
}()
private let tesseraLangAliases = ["javascript": "js", "typescript": "ts", "kt": "kotlin", "cs": "csharp", "c#": "csharp", "py": "python"]
private let cLikePattern = #"(//[^\n]*|/\*[\s\S]*?(?:\*/|$))|("(?:\\.|[^"\\\n])*"|'(?:\\.|[^'\\\n])*'|`(?:\\.|[^`\\])*`)|(\b\d[\d_]*(?:\.\d+)?(?:[eE][+-]?\d+)?[fFLu]?\b|\b0x[\da-fA-F]+\b)|([A-Za-z_$][\w$]*)(?=\s*\()|([A-Za-z_$][\w$]*)"#
private let hashPattern = #"(#[^\n]*)|("""[\s\S]*?"""|'''[\s\S]*?'''|"(?:\\.|[^"\\\n])*"|'(?:\\.|[^'\\\n])*')|(\b\d[\d_]*(?:\.\d+)?(?:[eE][+-]?\d+)?\b|\b0x[\da-fA-F]+\b)|([A-Za-z_$][\w$]*)(?=\s*\()|([A-Za-z_$][\w$]*)"#
private let cLikeRegex = try! NSRegularExpression(pattern: cLikePattern)
private let hashRegex = try! NSRegularExpression(pattern: hashPattern)

/// 按语言分词，返回需要着色的范围（NSRange，UTF-16）；不认识的语言返回空
public func tesseraTokens(_ code: String, language: String) -> [(NSRange, TesseraTokenKind)] {
    let key = tesseraLangAliases[language.lowercased()] ?? language.lowercased()
    guard let words = tesseraKeywords[key] else { return [] }
    let re = key == "python" ? hashRegex : cLikeRegex
    let ns = code as NSString
    var out: [(NSRange, TesseraTokenKind)] = []
    for m in re.matches(in: code, range: NSRange(location: 0, length: ns.length)) {
        if m.range(at: 1).location != NSNotFound { out.append((m.range, .comment)) }
        else if m.range(at: 2).location != NSNotFound { out.append((m.range, .string)) }
        else if m.range(at: 3).location != NSNotFound { out.append((m.range, .number)) }
        else if m.range(at: 4).location != NSNotFound { out.append((m.range, words.contains(ns.substring(with: m.range)) ? .keyword : .function)) }
        else if m.range(at: 5).location != NSNotFound, words.contains(ns.substring(with: m.range)) { out.append((m.range, .keyword)) }
    }
    return out
}

func tokenColor(_ k: TesseraTokenKind) -> Color {
    switch k {
    case .keyword: TesseraColor.pigmentLapisText
    case .string: TesseraColor.pigmentOliveText
    case .number: TesseraColor.pigmentOchreText
    case .function: TesseraColor.pigmentAmethystText
    case .comment: TesseraColor.textSecondary
    }
}

// MARK: - 编辑操作（纯函数，便于测试）

public enum TesseraCodeEdit {
    private static func lineStart(_ s: NSString, _ i: Int) -> Int {
        i <= 0 ? 0 : s.range(of: "\n", options: .backwards, range: NSRange(location: 0, length: i)).location.map0 + 1
    }

    /// Tab：插入一级缩进；选中多行时整块缩进。返回新文本与新选区
    public static func indent(_ text: String, _ sel: NSRange, unit: String) -> (String, NSRange) {
        let s = text as NSString
        let picked = s.substring(with: sel)
        if !picked.contains("\n") {
            return (s.replacingCharacters(in: sel, with: unit), NSRange(location: sel.location + (unit as NSString).length, length: 0))
        }
        let from = lineStart(s, sel.location)
        var to = NSMaxRange(sel)
        if sel.length > 0 && s.character(at: to - 1) == 10 { to -= 1 }
        let block = s.substring(with: NSRange(location: from, length: to - from)).components(separatedBy: "\n").map { unit + $0 }.joined(separator: "\n")
        return (s.replacingCharacters(in: NSRange(location: from, length: to - from), with: block),
                NSRange(location: sel.location + (unit as NSString).length, length: from + (block as NSString).length - sel.location - (unit as NSString).length))
    }

    /// Shift+Tab：当前行（或选中的每一行）去掉一级缩进
    public static func outdent(_ text: String, _ sel: NSRange, unit: String) -> (String, NSRange) {
        let s = text as NSString
        let from = lineStart(s, sel.location)
        var to = NSMaxRange(sel)
        if sel.length > 0 && s.character(at: to - 1) == 10 { to -= 1 }
        if sel.length == 0 {
            let lineEnd = s.range(of: "\n", range: NSRange(location: from, length: s.length - from)).location
            to = lineEnd == NSNotFound ? s.length : lineEnd
        }
        let lines = s.substring(with: NSRange(location: from, length: to - from)).components(separatedBy: "\n")
        let n = (unit as NSString).length
        let stripped = lines.map { line -> String in
            if line.hasPrefix("\t") { return String(line.dropFirst()) }
            let spaces = line.prefix(while: { $0 == " " }).count
            return String(line.dropFirst(min(spaces, n)))
        }
        let block = stripped.joined(separator: "\n")
        let firstDelta = (lines[0] as NSString).length - (stripped[0] as NSString).length
        let start = max(from, sel.location - firstDelta)
        let newSel = sel.length > 0 ? NSRange(location: start, length: from + (block as NSString).length - start) : NSRange(location: start, length: 0)
        return (s.replacingCharacters(in: NSRange(location: from, length: to - from), with: block), newSel)
    }

    /// 回车：保持上一行缩进；以 { [ ( : 结尾时多缩一级；光标在一对括号中间时把右括号放到下一行
    public static func newline(_ text: String, _ sel: NSRange, unit: String) -> (String, NSRange) {
        let s = text as NSString
        let from = lineStart(s, sel.location)
        let line = s.substring(with: NSRange(location: from, length: sel.location - from))
        let base = String(line.prefix(while: { $0 == " " || $0 == "\t" }))
        let trimmed = line.trimmingCharacters(in: .whitespaces)
        let opens = trimmed.last.map { "{[(:".contains($0) } ?? false
        let lead = opens ? base + unit : base
        let after = NSMaxRange(sel) < s.length ? Character(UnicodeScalar(s.character(at: NSMaxRange(sel)))!) : nil
        if opens, trimmed.last != ":", let a = after, "}])".contains(a) {
            let insert = "\n" + lead + "\n" + base
            return (s.replacingCharacters(in: sel, with: insert), NSRange(location: sel.location + 1 + (lead as NSString).length, length: 0))
        }
        let insert = "\n" + lead
        return (s.replacingCharacters(in: sel, with: insert), NSRange(location: sel.location + (insert as NSString).length, length: 0))
    }
}

private extension Int {
    /// NSNotFound 当作 -1（找不到换行时从 0 开始）
    var map0: Int { self == NSNotFound ? -1 : self }
}

// MARK: - 代码编辑器 CodeEditor

public struct TesseraDiagnostic: Identifiable, Hashable, Sendable {
    public var id: Int { line }
    public let line: Int
    public let message: String
    public init(line: Int, message: String) { self.line = line; self.message = message }
}

/// 代码编辑器：凹槽底色，左侧行号栏；语法高亮、当前行底色、第几行第几列。
/// - 实体键盘：Tab / Shift+Tab 缩进（多行整块），按 Esc 之后再按 Tab 离开编辑器
/// - 回车保持缩进，{ [ ( : 之后多缩一级；输入法组合（marked text）中不做任何处理
/// - diagnostics：出错的行号变红、整行淡红底，提示列在下方并朗读
public struct TesseraCodeEditor: View {
    @Binding var text: String
    let language: String
    let label: String
    let readOnly: Bool
    let diagnostics: [TesseraDiagnostic]
    let indent: Int
    let maxHeight: CGFloat
    @State private var caret = (line: 1, column: 1)
    @Environment(\.tesseraShape) private var tesseraShape
    @Environment(\.tesseraStaticRender) private var staticRender
    @Environment(\.colorSchemeContrast) private var contrast

    public init(text: Binding<String>, language: String = "", label: String = "代码", readOnly: Bool = false,
                diagnostics: [TesseraDiagnostic] = [], indent: Int = 2, maxHeight: CGFloat = 360) {
        self._text = text
        self.language = language
        self.label = label
        self.readOnly = readOnly
        self.diagnostics = diagnostics
        self.indent = indent
        self.maxHeight = maxHeight
    }

    public var body: some View {
        let round = tesseraShape.isRound
        let shape = RoundedRectangle(cornerRadius: tesseraShape.radius(tile: TesseraRadius.md, round: TesseraRound.row), style: .continuous)
        VStack(alignment: .leading, spacing: 0) {
            HStack {
                Text(language).font(.system(size: 12, weight: .medium, design: .monospaced)).foregroundStyle(TesseraColor.textSecondary)
                Spacer()
                Text("第 \(caret.line) 行，第 \(caret.column) 列").font(.system(size: 12, design: .monospaced)).foregroundStyle(TesseraColor.textSecondary)
                    .accessibilityHidden(true)
            }
            .padding(.horizontal, 16).padding(.vertical, 8)
            .overlay(alignment: .bottom) { if !round { TesseraColor.borderSubtle.frame(height: 1) } }
            editor.frame(minHeight: 120, maxHeight: maxHeight)
            if !readOnly {
                Text("实体键盘：Tab 缩进；按 Esc 后再按 Tab 可离开编辑器")
                    .font(.system(size: 12)).foregroundStyle(TesseraColor.textSecondary)
                    .padding(.horizontal, 16).padding(.vertical, 6)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .overlay(alignment: .top) { if !round { TesseraColor.borderSubtle.frame(height: 1) } }
            }
            if !diagnostics.isEmpty {
                VStack(alignment: .leading, spacing: 0) {
                    ForEach(diagnostics) { d in
                        HStack(alignment: .firstTextBaseline, spacing: 8) {
                            Text("第 \(d.line) 行").font(.system(size: 14, weight: .semibold))
                            Text(d.message).font(.system(size: 14))
                        }
                        .foregroundStyle(TesseraColor.textPrimary)
                        .padding(.horizontal, 16).padding(.vertical, 6)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .accessibilityElement(children: .combine)
                    }
                }
                .background(TesseraColor.dangerSubtle)
            }
        }
        .background { Color.clear.tesseraTile(TesseraColor.fillDefault, style: .sunken, shape: shape) }
        .clipShape(shape)
        .overlay(shape.strokeBorder(!round || contrast == .increased ? TesseraColor.borderField : .clear, lineWidth: 1))
    }

    @ViewBuilder private var editor: some View {
        if staticRender {
            StaticCode(text: text, language: language, errors: Set(diagnostics.map(\.line)))
        } else {
            NativeCodeView(text: $text, language: language, label: label, readOnly: readOnly,
                           errors: Set(diagnostics.map(\.line)),
                           errorText: diagnostics.map { "第 \($0.line) 行：\($0.message)" }.joined(separator: "；"),
                           unit: String(repeating: " ", count: indent)) { caret = $0 }
        }
    }
}

/// 截图用：与原生编辑器同样的排版，只读（ImageRenderer 画不出系统文本视图）
private struct StaticCode: View {
    let text: String
    let language: String
    let errors: Set<Int>

    var body: some View {
        let lines = text.components(separatedBy: "\n")
        HStack(alignment: .top, spacing: 0) {
            Text(lines.indices.map { "\($0 + 1)" }.joined(separator: "\n"))
                .multilineTextAlignment(.trailing)
                .foregroundStyle(TesseraColor.textSecondary)
                .frame(minWidth: 26, alignment: .trailing)
                .padding(.leading, 12).padding(.trailing, 10)
            LeadingClipLayout { Text(highlighted).fixedSize() }.clipped().padding(.leading, 4)
        }
        .font(.system(size: 13, design: .monospaced))
        .lineSpacing(5)
        .padding(.vertical, 12)
        .background(alignment: .topLeading) {
            VStack(spacing: 0) {
                ForEach(lines.indices, id: \.self) { i in
                    (errors.contains(i + 1) ? TesseraColor.dangerSolid.opacity(0.12) : Color.clear).frame(height: 13 * 1.2 + 5)
                }
            }
            .padding(.top, 12 - 2.5)
        }
    }

    private var highlighted: AttributedString {
        var a = AttributedString(text)
        for (r, kind) in tesseraTokens(text, language: language) {
            guard let range = Range(r, in: text), let lo = AttributedString.Index(range.lowerBound, within: a), let hi = AttributedString.Index(range.upperBound, within: a) else { continue }
            a[lo..<hi].foregroundColor = tokenColor(kind)
            if kind == .keyword { a[lo..<hi].font = .system(size: 13, weight: .semibold, design: .monospaced) }
            if kind == .comment { a[lo..<hi].font = .system(size: 13, design: .monospaced).italic() }
        }
        return a
    }
}

// MARK: 原生文本视图（iOS：UITextView；macOS：NSTextView）

#if canImport(UIKit)
struct NativeCodeView: UIViewRepresentable {
    @Binding var text: String
    let language: String
    let label: String
    let readOnly: Bool
    let errors: Set<Int>
    let errorText: String
    let unit: String
    let onCaret: ((line: Int, column: Int)) -> Void

    func makeCoordinator() -> Coordinator { Coordinator(self) }

    func makeUIView(context: Context) -> CodeTextView {
        let v = CodeTextView()
        v.delegate = context.coordinator
        v.coordinator = context.coordinator
        v.backgroundColor = .clear
        v.autocapitalizationType = .none
        v.autocorrectionType = .no
        v.smartQuotesType = .no
        v.smartDashesType = .no
        v.spellCheckingType = .no
        v.keyboardType = .asciiCapable
        v.textContainer.lineFragmentPadding = 0
        v.textContainer.widthTracksTextView = false
        v.textContainer.size = CGSize(width: 100_000, height: CGFloat.greatestFiniteMagnitude)
        v.alwaysBounceHorizontal = false
        v.textContainerInset = UIEdgeInsets(top: 12, left: 52, bottom: 12, right: 14)
        v.text = text
        context.coordinator.restyle(v)
        return v
    }

    func updateUIView(_ v: CodeTextView, context: Context) {
        context.coordinator.parent = self
        v.isEditable = !readOnly
        v.accessibilityLabel = label
        v.accessibilityValue = errorText.isEmpty ? nil : "有错误：\(errorText)"
        if v.text != text && v.markedTextRange == nil {
            let sel = v.selectedRange
            v.text = text
            v.selectedRange = NSRange(location: min(sel.location, (text as NSString).length), length: 0)
        }
        v.errors = errors
        context.coordinator.restyle(v)
    }

    final class Coordinator: NSObject, UITextViewDelegate {
        var parent: NativeCodeView
        private var styledKey = ""
        private var lastCaret = (0, 0)
        init(_ p: NativeCodeView) { parent = p }

        /// 着色：直接改文本存储的属性（不替换整段文字，撤销记录和选区都保留）；内容没变时跳过
        func restyle(_ v: CodeTextView, force: Bool = false) {
            // 输入法组合中不改属性（否则会打断候选）
            guard v.markedTextRange == nil else { return }
            let key = parent.language + "\u{1}" + v.text + "\u{1}" + parent.errors.sorted().map(String.init).joined(separator: ",")
            if key != styledKey || force {
                styledKey = key
                let storage = v.textStorage
                let full = NSRange(location: 0, length: storage.length)
                storage.beginEditing()
                storage.setAttributes(CodeTextView.baseAttributes, range: full)
                for (r, kind) in tesseraTokens(v.text, language: parent.language) where NSMaxRange(r) <= storage.length {
                    storage.addAttribute(.foregroundColor, value: UIColor(tokenColor(kind)), range: r)
                    if kind == .keyword { storage.addAttribute(.font, value: UIFont.monospacedSystemFont(ofSize: 13, weight: .semibold), range: r) }
                }
                storage.endEditing()
                v.typingAttributes = CodeTextView.baseAttributes
            }
            v.refreshDecorations()
            report(v)
        }

        /// 光标位置报告给 SwiftUI：异步、只在变化时报告（避免在视图更新过程中改状态）
        func report(_ v: UITextView) {
            let ns = v.text as NSString
            let upto = ns.substring(to: min(v.selectedRange.location, ns.length))
            let parts = upto.components(separatedBy: "\n")
            let now = (parts.count, ((parts.last ?? "") as NSString).length + 1)
            guard now != lastCaret else { return }
            lastCaret = now
            let cb = parent.onCaret
            DispatchQueue.main.async { cb(now) }
        }

        func textViewDidChange(_ v: UITextView) {
            guard let cv = v as? CodeTextView else { return }
            if v.markedTextRange == nil { parent.text = v.text; restyle(cv) }
        }

        func textViewDidChangeSelection(_ v: UITextView) {
            (v as? CodeTextView)?.refreshDecorations()
            report(v)
        }

        func textView(_ v: UITextView, shouldChangeTextIn range: NSRange, replacementText t: String) -> Bool {
            guard t == "\n", v.markedTextRange == nil, let cv = v as? CodeTextView else { return true }
            let (new, sel) = TesseraCodeEdit.newline(v.text, range, unit: parent.unit)
            cv.replaceAll(new, selection: sel)
            return false
        }
    }
}

final class CodeTextView: UITextView {
    static let baseAttributes: [NSAttributedString.Key: Any] = {
        let p = NSMutableParagraphStyle()
        p.minimumLineHeight = 21
        p.maximumLineHeight = 21
        return [.font: UIFont.monospacedSystemFont(ofSize: 13, weight: .regular), .foregroundColor: UIColor(TesseraColor.textPrimary), .paragraphStyle: p]
    }()
    weak var coordinator: NativeCodeView.Coordinator?
    var errors: Set<Int> = []
    private var exitOnTab = false
    private let gutter = UILabel()
    private let currentBar = UIView()
    private var errorBars: [UIView] = []

    override init(frame: CGRect, textContainer: NSTextContainer?) {
        super.init(frame: frame, textContainer: textContainer)
        gutter.numberOfLines = 0
        gutter.textAlignment = .right
        gutter.isAccessibilityElement = false
        currentBar.isUserInteractionEnabled = false
        addSubview(gutter)
        insertSubview(currentBar, at: 0)
    }

    required init?(coder: NSCoder) { fatalError() }

    /// 整段替换（保留撤销）：用于缩进、自动缩进
    func replaceAll(_ new: String, selection: NSRange) {
        guard let all = textRange(from: beginningOfDocument, to: endOfDocument) else { return }
        replace(all, withText: new)
        selectedRange = selection
        coordinator?.parent.text = new
        coordinator?.restyle(self)
    }

    override var keyCommands: [UIKeyCommand]? {
        let tab = UIKeyCommand(input: "\t", modifierFlags: [], action: #selector(onTab))
        tab.wantsPriorityOverSystemBehavior = true
        let back = UIKeyCommand(input: "\t", modifierFlags: .shift, action: #selector(onBackTab))
        back.wantsPriorityOverSystemBehavior = true
        return [UIKeyCommand(input: UIKeyCommand.inputEscape, modifierFlags: [], action: #selector(onEscape)), tab, back]
    }

    @objc private func onEscape() { exitOnTab = true }
    /// Esc 之后的 Tab：结束编辑、交出键盘焦点（文本视图自己会把 Tab 当字符插入，所以这里接管）
    @objc private func onTab() { if exitOnTab { exitOnTab = false; _ = resignFirstResponder() } else { edit(TesseraCodeEdit.indent) } }
    @objc private func onBackTab() { if exitOnTab { exitOnTab = false; _ = resignFirstResponder() } else { edit(TesseraCodeEdit.outdent) } }

    private func edit(_ f: (String, NSRange, String) -> (String, NSRange)) {
        guard markedTextRange == nil, isEditable, let unit = coordinator?.parent.unit else { return }
        let (new, sel) = f(text, selectedRange, unit)
        replaceAll(new, selection: sel)
    }

    override func resignFirstResponder() -> Bool { exitOnTab = false; return super.resignFirstResponder() }
    override func insertText(_ t: String) { exitOnTab = false; super.insertText(t) }
    override func deleteBackward() { exitOnTab = false; super.deleteBackward() }

    func refreshDecorations() {
        let lines = text.components(separatedBy: "\n")
        let ns = text as NSString
        let current = ns.substring(to: min(selectedRange.location, ns.length)).components(separatedBy: "\n").count
        let g = NSMutableAttributedString()
        for i in 1...max(lines.count, 1) {
            var attrs = Self.baseAttributes
            attrs[.foregroundColor] = UIColor(errors.contains(i) ? TesseraColor.dangerText : (i == current && isFirstResponder ? TesseraColor.textPrimary : TesseraColor.textSecondary))
            if errors.contains(i) || (i == current && isFirstResponder) { attrs[.font] = UIFont.monospacedSystemFont(ofSize: 13, weight: .semibold) }
            g.append(NSAttributedString(string: i == 1 ? "\(i)" : "\n\(i)", attributes: attrs))
        }
        gutter.attributedText = g
        gutter.frame = CGRect(x: 8, y: textContainerInset.top, width: 34, height: CGFloat(lines.count) * 21)
        let width = max(contentSize.width, bounds.width)
        currentBar.backgroundColor = UIColor(TesseraColor.textPrimary.opacity(0.05))
        currentBar.isHidden = !isFirstResponder
        currentBar.frame = CGRect(x: 0, y: textContainerInset.top + CGFloat(current - 1) * 21, width: width, height: 21)
        errorBars.forEach { $0.removeFromSuperview() }
        errorBars = errors.sorted().map { line in
            let bar = UIView(frame: CGRect(x: 0, y: textContainerInset.top + CGFloat(line - 1) * 21, width: width, height: 21))
            bar.backgroundColor = UIColor(TesseraColor.dangerSolid.opacity(0.12))
            bar.isUserInteractionEnabled = false
            insertSubview(bar, at: 0)
            return bar
        }
    }

    override func layoutSubviews() {
        super.layoutSubviews()
        // 行号栏固定在左侧：横向滚动时跟着平移
        gutter.frame.origin.x = contentOffset.x + 8
    }

    override func becomeFirstResponder() -> Bool { defer { refreshDecorations() }; return super.becomeFirstResponder() }
}
#elseif canImport(AppKit)
struct NativeCodeView: NSViewRepresentable {
    @Binding var text: String
    let language: String
    let label: String
    let readOnly: Bool
    let errors: Set<Int>
    let errorText: String
    let unit: String
    let onCaret: ((line: Int, column: Int)) -> Void

    func makeCoordinator() -> Coordinator { Coordinator(self) }

    func makeNSView(context: Context) -> NSScrollView {
        let scroll = NSScrollView()
        scroll.drawsBackground = false
        scroll.hasVerticalScroller = true
        scroll.hasHorizontalScroller = true
        scroll.autohidesScrollers = true
        let v = MacCodeTextView(frame: NSRect(x: 0, y: 0, width: 400, height: 200))
        v.minSize = NSSize(width: 0, height: 0)
        v.maxSize = NSSize(width: CGFloat.greatestFiniteMagnitude, height: CGFloat.greatestFiniteMagnitude)
        v.isVerticallyResizable = true
        v.autoresizingMask = [.width]
        scroll.documentView = v
        v.delegate = context.coordinator
        v.coordinator = context.coordinator
        v.drawsBackground = false
        v.isRichText = false
        v.isAutomaticQuoteSubstitutionEnabled = false
        v.isAutomaticDashSubstitutionEnabled = false
        v.isAutomaticSpellingCorrectionEnabled = false
        v.isContinuousSpellCheckingEnabled = false
        v.allowsUndo = true
        v.isHorizontallyResizable = true
        v.textContainer?.widthTracksTextView = false
        v.textContainer?.containerSize = NSSize(width: 100_000, height: CGFloat.greatestFiniteMagnitude)
        v.textContainer?.lineFragmentPadding = 0
        v.textContainerInset = NSSize(width: 52, height: 12)
        v.string = text
        context.coordinator.restyle(v)
        return scroll
    }

    func updateNSView(_ scroll: NSScrollView, context: Context) {
        context.coordinator.parent = self
        guard let v = scroll.documentView as? MacCodeTextView else { return }
        v.isEditable = !readOnly
        v.setAccessibilityLabel(label)
        v.errors = errors
        if v.string != text && !v.hasMarkedText() { v.string = text }
        context.coordinator.restyle(v)
    }

    final class Coordinator: NSObject, NSTextViewDelegate {
        var parent: NativeCodeView
        init(_ p: NativeCodeView) { parent = p }

        func restyle(_ v: MacCodeTextView) {
            guard !v.hasMarkedText(), let storage = v.textStorage else { return }
            let full = NSRange(location: 0, length: storage.length)
            storage.beginEditing()
            storage.setAttributes(MacCodeTextView.baseAttributes, range: full)
            for (r, kind) in tesseraTokens(v.string, language: parent.language) where NSMaxRange(r) <= storage.length {
                storage.addAttribute(.foregroundColor, value: NSColor(tokenColor(kind)), range: r)
                if kind == .keyword { storage.addAttribute(.font, value: NSFont.monospacedSystemFont(ofSize: 13, weight: .semibold), range: r) }
            }
            storage.endEditing()
            v.typingAttributes = MacCodeTextView.baseAttributes
            v.needsDisplay = true
            report(v)
        }

        private var lastCaret = (0, 0)

        /// 光标位置报告给 SwiftUI：异步、只在变化时报告（避免在视图更新过程中改状态）
        func report(_ v: NSTextView) {
            let ns = v.string as NSString
            let upto = ns.substring(to: min(v.selectedRange().location, ns.length))
            let parts = upto.components(separatedBy: "\n")
            let now = (parts.count, ((parts.last ?? "") as NSString).length + 1)
            guard now != lastCaret else { return }
            lastCaret = now
            let cb = parent.onCaret
            DispatchQueue.main.async { cb(now) }
        }

        func textDidChange(_ n: Notification) {
            guard let v = n.object as? MacCodeTextView, !v.hasMarkedText() else { return }
            parent.text = v.string
            restyle(v)
        }

        func textViewDidChangeSelection(_ n: Notification) {
            guard let v = n.object as? NSTextView else { return }
            v.needsDisplay = true
            report(v)
        }

        func textView(_ v: NSTextView, doCommandBy selector: Selector) -> Bool {
            guard let cv = v as? MacCodeTextView, !v.hasMarkedText(), v.isEditable else { return false }
            switch selector {
            case #selector(NSResponder.cancelOperation(_:)):
                cv.exitOnTab = true
                return true
            case #selector(NSResponder.insertTab(_:)):
                if cv.exitOnTab { cv.exitOnTab = false; v.window?.selectNextKeyView(nil); return true }
                cv.apply(TesseraCodeEdit.indent(v.string, v.selectedRange(), unit: parent.unit))
                return true
            case #selector(NSResponder.insertBacktab(_:)):
                if cv.exitOnTab { cv.exitOnTab = false; v.window?.selectPreviousKeyView(nil); return true }
                cv.apply(TesseraCodeEdit.outdent(v.string, v.selectedRange(), unit: parent.unit))
                return true
            case #selector(NSResponder.insertNewline(_:)):
                cv.exitOnTab = false
                cv.apply(TesseraCodeEdit.newline(v.string, v.selectedRange(), unit: parent.unit))
                return true
            default:
                cv.exitOnTab = false
                return false
            }
        }
    }
}

final class MacCodeTextView: NSTextView {
    static let baseAttributes: [NSAttributedString.Key: Any] = {
        let p = NSMutableParagraphStyle()
        p.minimumLineHeight = 21
        p.maximumLineHeight = 21
        return [.font: NSFont.monospacedSystemFont(ofSize: 13, weight: .regular), .foregroundColor: NSColor(TesseraColor.textPrimary), .paragraphStyle: p]
    }()
    weak var coordinator: NativeCodeView.Coordinator?
    var errors: Set<Int> = []
    var exitOnTab = false

    /// 整段替换（保留撤销）
    func apply(_ r: (String, NSRange)) {
        let full = NSRange(location: 0, length: (string as NSString).length)
        if shouldChangeText(in: full, replacementString: r.0) {
            replaceCharacters(in: full, with: r.0)
            didChangeText()
        }
        setSelectedRange(r.1)
    }

    override func drawBackground(in rect: NSRect) {
        super.drawBackground(in: rect)
        let lines = string.components(separatedBy: "\n")
        let ns = string as NSString
        let current = ns.substring(to: min(selectedRange().location, ns.length)).components(separatedBy: "\n").count
        let top = textContainerInset.height
        let focused = window?.firstResponder === self
        for line in errors {
            NSColor(TesseraColor.dangerSolid.opacity(0.12)).setFill()
            NSRect(x: 0, y: top + CGFloat(line - 1) * 21, width: bounds.width, height: 21).fill()
        }
        if focused {
            NSColor(TesseraColor.textPrimary.opacity(0.05)).setFill()
            NSRect(x: 0, y: top + CGFloat(current - 1) * 21, width: bounds.width, height: 21).fill()
        }
        // 行号栏：固定在可见区域左侧
        let left = enclosingScrollView?.contentView.bounds.minX ?? 0
        for i in 1...max(lines.count, 1) {
            let err = errors.contains(i), cur = i == current && focused
            let attrs: [NSAttributedString.Key: Any] = [
                .font: NSFont.monospacedSystemFont(ofSize: 13, weight: err || cur ? .semibold : .regular),
                .foregroundColor: NSColor(err ? TesseraColor.dangerText : (cur ? TesseraColor.textPrimary : TesseraColor.textSecondary)),
            ]
            let s = NSAttributedString(string: "\(i)", attributes: attrs)
            let w = s.size().width
            s.draw(at: NSPoint(x: left + 42 - w, y: top + CGFloat(i - 1) * 21 + 2))
        }
    }
}
#endif

// MARK: - 十六进制查看 HexView

/// 一段着色的字节：[from, to)
public struct TesseraHexRange: Hashable, Sendable {
    public let from: Int
    public let to: Int
    public let label: String
    public let tone: TesseraTone
    public init(_ from: Int, _ to: Int, label: String, tone: TesseraTone = .accent) {
        self.from = from; self.to = to; self.label = label; self.tone = tone
    }
    public static func == (a: Self, b: Self) -> Bool { a.from == b.from && a.to == b.to && a.label == b.label }
    public func hash(into h: inout Hasher) { h.combine(from); h.combine(to); h.combine(label) }
}

private func hex2(_ b: UInt8) -> String { String(format: "%02X", b) }
private func printable(_ b: UInt8) -> String { (0x20..<0x7F).contains(b) ? String(UnicodeScalar(b)) : "·" }

/// 状态栏的说明文字（与 Web、Android 一致，也供测试使用）
public func tesseraHexDescribe(_ data: [UInt8], from: Int, to: Int, focus: Int, ranges: [TesseraHexRange] = []) -> String {
    guard !data.isEmpty else { return "没有数据" }
    let b = data[focus]
    let ch = (0x20..<0x7F).contains(b) ? "，字符“\(String(UnicodeScalar(b)))”" : ""
    let r = ranges.first { focus >= $0.from && focus < $0.to }.map { "，\($0.label)" } ?? ""
    let sel = to > from ? "，已选 \(to - from + 1) 个字节（\(from)–\(to)）" : ""
    let bin = String(b, radix: 2)
    return "偏移 0x\(String(focus, radix: 16, uppercase: true))（\(focus)）：0x\(hex2(b)) = \(b) = 0b\(String(repeating: "0", count: 8 - bin.count))\(bin)\(ch)\(r)\(sel)"
}

/// 十六进制查看：偏移 / 16 个字节（紧凑宽度 8 个）/ ASCII 三栏，按行懒加载。
/// 点按选中一个字节（两栏同步），长按扩展选区；实体键盘方向键移动、Shift 扩展；“复制”把选中的字节复制为十六进制。
/// 读屏按行朗读（偏移、字节、文本），状态栏实时播报当前字节。
public struct TesseraHexView: View {
    let data: [UInt8]
    let highlights: [TesseraHexRange]
    let columns: Int?
    let height: CGFloat
    let label: String
    @State private var anchor: Int
    @State private var focus: Int
    @State private var copied = false
    @FocusState private var focused: Bool
    @Environment(\.tesseraShape) private var tesseraShape
    @Environment(\.horizontalSizeClass) private var sizeClass
    @Environment(\.tesseraStaticRender) private var staticRender

    public init(_ data: [UInt8], highlights: [TesseraHexRange] = [], columns: Int? = nil, height: CGFloat = 312,
                label: String = "十六进制数据", selection: ClosedRange<Int>? = nil) {
        self.data = data
        self.highlights = highlights
        self.columns = columns
        self.height = height
        self.label = label
        self._anchor = State(initialValue: selection?.lowerBound ?? 0)
        self._focus = State(initialValue: selection?.upperBound ?? 0)
    }

    private var cols: Int { columns ?? (sizeClass == .compact ? 8 : 16) }
    private var lo: Int { min(anchor, focus) }
    private var hi: Int { max(anchor, focus) }

    private func select(_ i: Int, extend: Bool) {
        guard !data.isEmpty else { return }
        focus = min(max(i, 0), data.count - 1)
        if !extend { anchor = focus }
        copied = false
    }

    private func copy() {
        let s = data[lo...hi].map(hex2).joined(separator: " ")
        #if canImport(UIKit)
        UIPasteboard.general.string = s
        #elseif canImport(AppKit)
        NSPasteboard.general.clearContents()
        NSPasteboard.general.setString(s, forType: .string)
        #endif
        copied = true
    }

    public var body: some View {
        let round = tesseraShape.isRound
        let shape = RoundedRectangle(cornerRadius: tesseraShape.radius(tile: TesseraRadius.md, round: TesseraRound.row), style: .continuous)
        VStack(alignment: .leading, spacing: 0) {
            HexHeader(cols: cols)
                .overlay(alignment: .bottom) { if !round { TesseraColor.borderSubtle.frame(height: 1) } }
            rows.frame(height: height)
            HStack {
                Text(copied ? "已复制 \(hi - lo + 1) 个字节" : tesseraHexDescribe(data, from: lo, to: hi, focus: focus, ranges: highlights))
                    .font(.system(size: 12, design: .monospaced)).foregroundStyle(TesseraColor.textSecondary)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .accessibilityAddTraits(.updatesFrequently)
                TesseraIconButton(.copy, label: "复制选中的字节") { copy() }
            }
            .padding(.leading, 14)
            .overlay(alignment: .top) { if !round { TesseraColor.borderSubtle.frame(height: 1) } }
            if !highlights.isEmpty { legend.padding(.horizontal, 14).padding(.bottom, 10) }
        }
        .background { Color.clear.tesseraTile(TesseraColor.fillDefault, style: .sunken, shape: shape) }
        .clipShape(shape)
    }

    @ViewBuilder private var rows: some View {
        let count = (data.count + cols - 1) / cols
        let content = LazyVStack(alignment: .leading, spacing: 0) {
            ForEach(0..<count, id: \.self) { r in
                HexRow(data: data, row: r, cols: cols, lo: lo, hi: hi, focus: focus, highlights: highlights) { i, ext in select(i, extend: ext) }
                    .id(r)
            }
        }
        if staticRender {
            LeadingClipLayout { content.fixedSize() }.clipped()
        } else {
            ScrollViewReader { proxy in
                ScrollView([.vertical, .horizontal], showsIndicators: false) { content }
                    .focusable()
                    .focused($focused)
                    .accessibilityLabel(label)
                    .onKeyPress(phases: .down) { press in
                        let ext = press.modifiers.contains(.shift)
                        let page = cols * 10
                        let target: Int?
                        switch press.key {
                        case .rightArrow: target = focus + 1
                        case .leftArrow: target = focus - 1
                        case .downArrow: target = focus + cols
                        case .upArrow: target = focus - cols
                        case .pageDown: target = focus + page
                        case .pageUp: target = focus - page
                        case .home: target = press.modifiers.contains(.command) ? 0 : focus - focus % cols
                        case .end: target = press.modifiers.contains(.command) ? data.count - 1 : min(data.count - 1, focus - focus % cols + cols - 1)
                        default: target = nil
                        }
                        guard let t = target else { return .ignored }
                        select(t, extend: ext)
                        proxy.scrollTo(focus / cols)
                        return .handled
                    }
            }
        }
    }

    private var legend: some View {
        TesseraFlowLayout(spacing: 16) {
            ForEach(highlights, id: \.self) { h in
                HStack(spacing: 6) {
                    RoundedRectangle(cornerRadius: 3).fill(h.tone.colors.foreground).frame(width: 10, height: 10)
                    Text("\(h.label) \(h.from)–\(h.to - 1)").font(.system(size: 12)).foregroundStyle(TesseraColor.textSecondary)
                }
            }
        }
    }
}

private struct HexHeader: View {
    let cols: Int
    var body: some View {
        HStack(spacing: 0) {
            Text("偏移").frame(width: 72, alignment: .leading)
            ForEach(0..<cols, id: \.self) { k in
                if k == cols / 2 { Color.clear.frame(width: 6) }
                Text(String(format: "%02X", k)).frame(width: 22)
            }
            Text("ASCII").padding(.leading, 12)
        }
        .font(.system(size: 12, design: .monospaced))
        .foregroundStyle(TesseraColor.textSecondary)
        .padding(.horizontal, 14).padding(.vertical, 6)
        .fixedSize()
        .accessibilityHidden(true)
    }
}

private struct HexRow: View {
    let data: [UInt8]
    let row: Int
    let cols: Int
    let lo: Int
    let hi: Int
    let focus: Int
    let highlights: [TesseraHexRange]
    let onTap: (Int, Bool) -> Void
    @Environment(\.tesseraShape) private var tesseraShape

    private var spoken: String {
        let off = row * cols, end = min(data.count, off + cols)
        return "偏移 " + String(format: "%08X", off) + "：" + data[off..<end].map(hex2).joined(separator: " ") + "；文本 " + data[off..<end].map(printable).joined()
    }

    private func tone(_ i: Int) -> Color? {
        highlights.first { i >= $0.from && i < $0.to }?.tone.colors.background
    }

    var body: some View {
        let off = row * cols
        HStack(spacing: 0) {
            Text(String(format: "%08X", off)).foregroundStyle(TesseraColor.textSecondary).frame(width: 72, alignment: .leading)
            ForEach(0..<cols, id: \.self) { k in
                if k == cols / 2 { Color.clear.frame(width: 6) }
                cell(off + k, text: off + k < data.count ? hex2(data[off + k]) : "", width: 22)
            }
            Color.clear.frame(width: 12)
            ForEach(0..<cols, id: \.self) { k in
                if off + k < data.count { cell(off + k, text: printable(data[off + k]), width: 10, secondary: true) }
            }
        }
        .font(.system(size: 13, design: .monospaced))
        .padding(.horizontal, 14)
        .frame(height: 26)
        .fixedSize()
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(spoken)
    }

    @ViewBuilder private func cell(_ i: Int, text: String, width: CGFloat, secondary: Bool = false) -> some View {
        let isFocus = i == focus, picked = i >= lo && i <= hi
        let bg: Color = isFocus ? TesseraColor.accentSolid : (picked ? TesseraColor.textPrimary : (tone(i) ?? .clear))
        let fg: Color = isFocus ? TesseraColor.textOnSolid : (picked ? TesseraColor.bgSurface : (secondary ? TesseraColor.textSecondary : TesseraColor.textPrimary))
        Text(text)
            .foregroundStyle(fg)
            .frame(width: width, height: 22)
            .background(bg, in: RoundedRectangle(cornerRadius: tesseraShape.isRound ? 6 : 4, style: .continuous))
            .contentShape(Rectangle())
            .onTapGesture { if i < data.count { onTap(i, false) } }
            .onLongPressGesture { if i < data.count { onTap(i, true) } }
    }
}
