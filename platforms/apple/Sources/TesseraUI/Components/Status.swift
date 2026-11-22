import SwiftUI

// MARK: - 进度条 Progress

/// 线形进度：凹槽里一条釉色砖；value 为 nil 时是不确定进度（一块砖在槽里来回跳）
public struct TesseraProgress: View {
    let value: Double?
    let label: String?
    let valueText: String?
    let tone: TesseraTone
    @Environment(\.tesseraShape) private var tesseraShape
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    public init(value: Double?, label: String? = nil, valueText: String? = nil, tone: TesseraTone = .accent) {
        self.value = value
        self.label = label
        self.valueText = valueText
        self.tone = tone
    }

    private var percent: String { "\(Int(((value ?? 0) * 100).rounded()))%" }

    public var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            if label != nil || valueText != nil {
                HStack {
                    TesseraLabel(label ?? "", style: .caption, color: TesseraColor.textSecondary)
                    Spacer(minLength: 0)
                    if value != nil { TesseraLabel(valueText ?? percent, style: .caption, color: TesseraColor.textSecondary).monospacedDigit() }
                }
            }
            ProgressTrack(value: value, fill: progressFill(tone), round: tesseraShape.isRound, reduceMotion: reduceMotion)
                .frame(height: 8)
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(label ?? "进度")
        .accessibilityValue(value == nil ? "进行中" : (valueText ?? percent))
    }
}

func progressFill(_ tone: TesseraTone) -> Color {
    switch tone {
    case .success: TesseraColor.successSolid
    case .danger: TesseraColor.dangerSolid
    case .warning: TesseraColor.warningSolid
    default: TesseraColor.accentControl
    }
}

private struct ProgressTrack: View {
    let value: Double?
    let fill: Color
    let round: Bool
    let reduceMotion: Bool

    var body: some View {
        let shape = RoundedRectangle(cornerRadius: round ? 4 : 4, style: .continuous)
        GeometryReader { g in
            ZStack(alignment: .leading) {
                shape.fill(TesseraColor.fillControlOff)
                if let value {
                    shape.fill(fill)
                        .overlay(LinearGradient(colors: [.white.opacity(0.18), .clear], startPoint: .top, endPoint: .bottom).clipShape(shape))
                        .frame(width: g.size.width * min(max(value, 0), 1))
                        .animation(.tessera(TesseraMotion.smooth, reduceMotion: reduceMotion), value: value)
                } else {
                    HopBar(width: g.size.width, fill: fill, shape: shape, reduceMotion: reduceMotion)
                }
            }
        }
        .clipShape(shape)
    }
}

/// 不确定进度：28% 宽的一块砖在槽里来回跳（1.3 秒一趟）
private struct HopBar: View {
    let width: CGFloat
    let fill: Color
    let shape: RoundedRectangle
    let reduceMotion: Bool

    var body: some View {
        TimelineView(.animation(paused: reduceMotion)) { ctx in
            let period = 1.3
            let t = ctx.date.timeIntervalSinceReferenceDate.truncatingRemainder(dividingBy: period * 2) / period
            let p = t <= 1 ? t : 2 - t
            let eased = p < 0.5 ? 4 * p * p * p : 1 - pow(-2 * p + 2, 3) / 2
            shape.fill(fill)
                .frame(width: width * 0.28)
                .offset(x: reduceMotion ? 0 : (width * 0.72) * eased)
        }
    }
}

/// 环形进度：大号（96pt）中间用标题字体显示百分比
public struct TesseraProgressRing: View {
    let value: Double
    let size: CGFloat
    let label: String
    let showsValue: Bool
    let tone: TesseraTone
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    public init(value: Double, size: CGFloat = 48, label: String = "进度", showsValue: Bool = true, tone: TesseraTone = .accent) {
        self.value = value
        self.size = size
        self.label = label
        self.showsValue = showsValue
        self.tone = tone
    }

    public var body: some View {
        let v = min(max(value, 0), 1)
        let thick: CGFloat = size >= 80 ? 8 : 5
        let percent = "\(Int((v * 100).rounded()))%"
        ZStack {
            Circle().stroke(TesseraColor.fillControlOff, lineWidth: thick)
            Circle().trim(from: 0, to: v)
                .stroke(progressFill(tone), style: StrokeStyle(lineWidth: thick, lineCap: .round))
                .rotationEffect(.degrees(-90))
                .animation(.tessera(TesseraMotion.smooth, reduceMotion: reduceMotion), value: v)
            if showsValue {
                if size >= 80 {
                    Text(percent).font(.custom(TesseraFontFamily.display, fixedSize: 22).weight(.semibold)).monospacedDigit()
                } else {
                    Text(percent).font(.system(size: 12, weight: .semibold)).monospacedDigit()
                }
            }
        }
        .foregroundStyle(TesseraColor.textPrimary)
        .padding(thick / 2)
        .frame(width: size, height: size)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(label)
        .accessibilityValue(percent)
    }
}

// MARK: - 横幅 Banner

/// 横幅：整块浅色底，图标 + 标题 + 正文 + 操作；可关闭
public struct TesseraBanner<Actions: View>: View {
    let text: String
    let title: String?
    let tone: TesseraTone
    let icon: TesseraIconName?
    let onDismiss: (() -> Void)?
    let actions: Actions
    @Environment(\.tesseraShape) private var tesseraShape

    public init(_ text: String, title: String? = nil, tone: TesseraTone = .accent, icon: TesseraIconName? = nil,
                onDismiss: (() -> Void)? = nil, @ViewBuilder actions: () -> Actions) {
        self.text = text
        self.title = title
        self.tone = tone
        self.icon = icon
        self.onDismiss = onDismiss
        self.actions = actions()
    }

    private var symbol: TesseraIconName {
        if let icon { return icon }
        switch tone {
        case .success: return .success
        case .warning: return .warning
        case .danger: return .error
        default: return .info
        }
    }

    private var background: Color {
        switch tone {
        case .success: TesseraColor.successSubtle
        case .warning: TesseraColor.warningSubtle
        case .danger: TesseraColor.dangerSubtle
        case .neutral: TesseraColor.fillDefault
        default: TesseraColor.accentSubtle
        }
    }

    private var iconColor: Color {
        switch tone {
        case .success: TesseraColor.successText
        case .warning: TesseraColor.warningText
        case .danger: TesseraColor.dangerText
        case .neutral: TesseraColor.textSecondary
        default: TesseraColor.accentText
        }
    }

    public var body: some View {
        let shape = RoundedRectangle(cornerRadius: tesseraShape.radius(tile: TesseraRadius.lg, round: TesseraRound.row), style: .continuous)
        HStack(alignment: .top, spacing: TesseraSpace.s3) {
            TesseraIcon(symbol, size: 20).foregroundStyle(iconColor).padding(.top, 1).accessibilityHidden(true)
            VStack(alignment: .leading, spacing: 2) {
                if let title { TesseraLabel(title, style: .label) }
                TesseraLabel(text, style: .bodySm).fixedSize(horizontal: false, vertical: true)
                actions.padding(.top, Actions.self == EmptyView.self ? 0 : TesseraSpace.s3 - 2)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .accessibilityElement(children: .combine)
            if let onDismiss {
                Button(action: onDismiss) {
                    TesseraIcon(.close, size: 18).foregroundStyle(TesseraColor.textSecondary)
                        .frame(width: TesseraSize.touchMin, height: TesseraSize.touchMin)
                        .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                .accessibilityLabel("关闭提示")
                // 触控范围 44，但版面只占 20（上下各伸出 12），单行横幅不会被撑高
                .padding(.vertical, -12)
                .padding(.trailing, -12)
            }
        }
        .padding(.leading, tesseraShape.isRound ? 18 : 16)
        .padding(.trailing, 16)
        .padding(.vertical, 14)
        .background(background, in: shape)
    }
}

public extension TesseraBanner where Actions == EmptyView {
    init(_ text: String, title: String? = nil, tone: TesseraTone = .accent, icon: TesseraIconName? = nil, onDismiss: (() -> Void)? = nil) {
        self.init(text, title: title, tone: tone, icon: icon, onDismiss: onDismiss, actions: { EmptyView() })
    }
}

// MARK: - 结果页 Result

/// 结果页的状态：成功 / 失败 / 提示 / 页面不存在
public enum TesseraResultStatus: Sendable { case success, error, info, notFound }

/// 结果页：一块大釉砖图标落下时轻轻弹一下
public struct TesseraResult<Actions: View>: View {
    public typealias Status = TesseraResultStatus
    let status: Status
    let title: String
    let text: String?
    let actions: Actions
    @State private var settled = false
    @Environment(\.tesseraShape) private var tesseraShape
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    public init(_ status: Status, title: String, text: String? = nil, @ViewBuilder actions: () -> Actions) {
        self.status = status
        self.title = title
        self.text = text
        self.actions = actions()
    }

    private var fill: Color {
        switch status {
        case .success: TesseraColor.successSolid
        case .error: TesseraColor.dangerSolid
        case .notFound: TesseraColor.textPrimary
        case .info: TesseraColor.accentSolid
        }
    }

    public var body: some View {
        VStack(spacing: TesseraSpace.s3) {
            ResultIcon(status: status, fill: fill, round: tesseraShape.isRound)
                .scaleEffect(settled || reduceMotion ? 1 : 0.6)
                .offset(y: settled || reduceMotion ? 0 : -24)
                .opacity(settled || reduceMotion ? 1 : 0)
                .padding(.bottom, TesseraSpace.s2)
            TesseraLabel(title, style: .title1).multilineTextAlignment(.center).accessibilityAddTraits(.isHeader)
            if let text {
                TesseraLabel(text, style: .body, color: TesseraColor.textSecondary)
                    .multilineTextAlignment(.center).frame(maxWidth: 420).padding(.bottom, TesseraSpace.s4)
            }
            HStack(spacing: TesseraSpace.s2) { actions }
        }
        .frame(maxWidth: .infinity)
        .padding(.horizontal, TesseraSpace.s6)
        .padding(.vertical, TesseraSpace.s12)
        .onAppear { withAnimation(.tessera(TesseraMotion.bouncy, reduceMotion: reduceMotion)) { settled = true } }
    }
}

public extension TesseraResult where Actions == EmptyView {
    init(_ status: Status, title: String, text: String? = nil) {
        self.init(status, title: title, text: text, actions: { EmptyView() })
    }
}

private struct ResultIcon: View {
    let status: TesseraResultStatus
    let fill: Color
    let round: Bool

    var body: some View {
        let shape = RoundedRectangle(cornerRadius: round ? 36 : 26, style: .continuous)
        ZStack {
            Color.clear.tesseraTile(fill, style: .solid, shape: shape)
            content
        }
        .frame(width: 72, height: 72)
        .tesseraShadow(.level2)
        .accessibilityHidden(true)
    }

    @ViewBuilder private var content: some View {
        switch status {
        case .success: TesseraIcon(.check, size: 36).foregroundStyle(.white)
        case .error: TesseraIcon(.close, size: 36).foregroundStyle(.white)
        case .info: TesseraIcon(.info, size: 36).foregroundStyle(.white)
        case .notFound: Text("404").font(.custom(TesseraFontFamily.display, fixedSize: 22).weight(.semibold)).foregroundStyle(TesseraColor.bgSurface)
        }
    }
}

// MARK: - 空状态 EmptyState

/// 空状态：一小片 3×3 马赛克，中间一块釉砖翘起来
public struct TesseraEmptyState<Action: View>: View {
    let title: String
    let text: String?
    let action: Action
    @Environment(\.tesseraShape) private var tesseraShape

    public init(_ title: String, text: String? = nil, @ViewBuilder action: () -> Action) {
        self.title = title
        self.text = text
        self.action = action()
    }

    public var body: some View {
        VStack(spacing: TesseraSpace.s2) {
            EmptyMosaic(round: tesseraShape.isRound).padding(.bottom, TesseraSpace.s3).accessibilityHidden(true)
            TesseraLabel(title, style: .title3).multilineTextAlignment(.center).accessibilityAddTraits(.isHeader)
            if let text {
                TesseraLabel(text, style: .bodySm, color: TesseraColor.textSecondary)
                    .multilineTextAlignment(.center).frame(maxWidth: 360).padding(.bottom, TesseraSpace.s3)
            }
            action
        }
        .frame(maxWidth: .infinity)
        .padding(.horizontal, TesseraSpace.s6)
        .padding(.vertical, TesseraSpace.s12)
    }
}

public extension TesseraEmptyState where Action == EmptyView {
    init(_ title: String, text: String? = nil) { self.init(title, text: text, action: { EmptyView() }) }
}

private struct EmptyMosaic: View {
    let round: Bool

    var body: some View {
        let gap: CGFloat = round ? 6 : 3
        VStack(spacing: gap) {
            ForEach(0..<3, id: \.self) { r in
                HStack(spacing: gap) {
                    ForEach(0..<3, id: \.self) { c in cell(r * 3 + c) }
                }
            }
        }
    }

    @ViewBuilder private func cell(_ i: Int) -> some View {
        let big: CGFloat = round ? 11 : 12
        let small: CGFloat = round ? 11 : 6
        let shape = UnevenRoundedRectangle(cornerRadii: .init(
            topLeading: i == 0 ? big : small, bottomLeading: i == 6 ? big : small,
            bottomTrailing: i == 8 ? big : small, topTrailing: i == 2 ? big : small), style: .continuous)
        if i == 4 {
            Color.clear.tesseraTile(TesseraColor.accentSolid, style: .solid, shape: shape)
                .frame(width: 22, height: 22)
                .rotationEffect(.degrees(round ? 0 : -8))
                .offset(y: -6)
        } else {
            Color.clear.tesseraTile(TesseraColor.fillDefault, style: .sunken, shape: shape).frame(width: 22, height: 22)
        }
    }
}

// MARK: - 按键 Kbd

/// 快捷键键帽；组合键传多个：TesseraKbd("⌘", "K")
public struct TesseraKbd: View {
    let keys: [String]
    @Environment(\.tesseraShape) private var tesseraShape

    public init(_ keys: String...) { self.keys = keys }

    private static func spoken(_ k: String) -> String {
        switch k {
        case "⌘": "Command"
        case "⌥": "Option"
        case "⇧": "Shift"
        case "⌃": "Control"
        case "↵", "⏎": "回车"
        case "⌫": "退格"
        case "↑": "上"
        case "↓": "下"
        case "←": "左"
        case "→": "右"
        default: k
        }
    }

    public var body: some View {
        let shape = RoundedRectangle(cornerRadius: tesseraShape.isRound ? 8 : 6, style: .continuous)
        HStack(spacing: 4) {
            ForEach(Array(keys.enumerated()), id: \.offset) { _, k in
                Text(k)
                    .font(.system(size: 11, weight: .medium, design: .monospaced))
                    .foregroundStyle(TesseraColor.textSecondary)
                    .padding(.horizontal, 6)
                    .padding(.top, 3)
                    .padding(.bottom, 4)
                    .background(TesseraColor.bgSurface, in: shape)
                    .overlay(shape.strokeBorder(TesseraColor.borderSubtle, lineWidth: 1))
            }
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("快捷键 " + keys.map(Self.spoken).joined(separator: " 加 "))
    }
}

// MARK: - 工具提示 Tooltip

/// 提示气泡本身：一块小反色砖（静态展示、截图用）
public struct TesseraTooltipBubble: View {
    let text: String
    @Environment(\.tesseraShape) private var tesseraShape

    public init(_ text: String) { self.text = text }

    public var body: some View {
        // 短提示贴合文字宽度；放不下一行时最宽 260 再换行
        ViewThatFits(in: .horizontal) {
            bubble(Text(text).fixedSize())
            bubble(Text(text).frame(width: 260 - (tesseraShape.isRound ? 24 : 20), alignment: .leading).fixedSize(horizontal: false, vertical: true))
        }
    }

    private func bubble(_ label: some View) -> some View {
        label
            .font(.system(size: 12.5, weight: .medium))
            .foregroundStyle(TesseraColor.textInverse)
            .padding(.horizontal, tesseraShape.isRound ? 12 : 10)
            .padding(.vertical, 6)
            .background(TesseraColor.bgInverse, in: RoundedRectangle(cornerRadius: tesseraShape.isRound ? 14 : TesseraRadius.sm, style: .continuous))
            .tesseraShadow(.level2)
    }
}

public extension View {
    /// 工具提示：触屏上长按出现、松手 1.5 秒后消失；Mac 上同时作为鼠标悬停提示（.help）。
    /// 读屏把提示文字作为补充说明读出（accessibilityHint），不覆盖内容自己的名称。
    func tesseraTooltip(_ text: String) -> some View { modifier(TooltipModifier(text: text)) }
}

private struct TooltipModifier: ViewModifier {
    let text: String
    @State private var visible = false
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    func body(content: Content) -> some View {
        content
            .help(text)
            .accessibilityHint(text)
            .simultaneousGesture(LongPressGesture(minimumDuration: 0.5).onEnded { _ in
                withAnimation(.tessera(TesseraMotion.easeStandard(TesseraMotion.durationFast), reduceMotion: reduceMotion)) { visible = true }
                Task { @MainActor in
                    try? await Task.sleep(nanoseconds: 1_500_000_000)
                    withAnimation(.easeOut(duration: TesseraMotion.durationFast)) { visible = false }
                }
            })
            .overlay(alignment: .top) {
                if visible {
                    TesseraTooltipBubble(text)
                        .fixedSize()
                        .alignmentGuide(.top) { $0[.bottom] + 6 }
                        .transition(.opacity.combined(with: .scale(scale: 0.94, anchor: .bottom)))
                        .allowsHitTesting(false)
                        .accessibilityHidden(true)
                }
            }
    }
}

// MARK: - 气泡卡片 Popover

/// 气泡卡片面板：内容可交互。弹出用 .tesseraPopover(isPresented:)，面板放在里面：
/// Button("详情") { open = true }.tesseraPopover(isPresented: $open) { TesseraPopoverPanel(title: "…") { … } }
public struct TesseraPopoverPanel<Content: View>: View {
    let title: String?
    let content: Content
    @Environment(\.tesseraShape) private var tesseraShape
    @Environment(\.colorSchemeContrast) private var contrast

    public init(title: String? = nil, @ViewBuilder content: () -> Content) {
        self.title = title
        self.content = content()
    }

    public var body: some View {
        let shape = RoundedRectangle(cornerRadius: tesseraShape.radius(tile: TesseraRadius.lg, round: TesseraRound.menu), style: .continuous)
        VStack(alignment: .leading, spacing: 6) {
            if let title { TesseraLabel(title, style: .title3).accessibilityAddTraits(.isHeader) }
            content
        }
        .padding(TesseraSpace.s5)
        .frame(maxWidth: 340, alignment: .leading)
        .background(TesseraColor.bgElevated, in: shape)
        .overlay(shape.strokeBorder(tesseraShape.showsOutline(contrast) ? TesseraColor.borderSubtle : .clear, lineWidth: 1))
        .tesseraShadow(.level2)
    }
}
