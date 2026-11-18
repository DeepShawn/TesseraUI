import SwiftUI

// MARK: - 色调

/// 状态色只表示状态；颜料色只用于头像、分类，不表示状态
public enum TesseraTone: Sendable {
    case neutral, accent, success, warning, danger, cinnabar, ochre, lapis, amethyst, olive

    var colors: (background: Color, foreground: Color) {
        switch self {
        case .neutral: (TesseraColor.fillDefault, TesseraColor.textSecondary)
        case .accent: (TesseraColor.accentSubtle, TesseraColor.accentText)
        case .success: (TesseraColor.successSubtle, TesseraColor.successText)
        case .warning: (TesseraColor.warningSubtle, TesseraColor.warningText)
        case .danger: (TesseraColor.dangerSubtle, TesseraColor.dangerText)
        case .cinnabar: (TesseraColor.pigmentCinnabarSubtle, TesseraColor.pigmentCinnabarText)
        case .ochre: (TesseraColor.pigmentOchreSubtle, TesseraColor.pigmentOchreText)
        case .lapis: (TesseraColor.pigmentLapisSubtle, TesseraColor.pigmentLapisText)
        case .amethyst: (TesseraColor.pigmentAmethystSubtle, TesseraColor.pigmentAmethystText)
        case .olive: (TesseraColor.pigmentOliveSubtle, TesseraColor.pigmentOliveText)
        }
    }
}

// MARK: - 标签、徽标

public struct TesseraTag: View {
    let text: String
    let tone: TesseraTone
    let icon: TesseraIconName?
    @Environment(\.tesseraShape) private var tesseraShape

    public init(_ text: String, tone: TesseraTone = .neutral, icon: TesseraIconName? = nil) {
        self.text = text
        self.tone = tone
        self.icon = icon
    }

    public var body: some View {
        HStack(spacing: 4) {
            if let icon { TesseraIcon(icon, size: 14) }
            Text(text).font(.system(size: 12, weight: .semibold))
        }
        .foregroundStyle(tone.colors.foreground)
        .padding(.horizontal, tesseraShape.isRound ? 10 : 8)
        .frame(height: 24)
        .background(tone.colors.background, in: RoundedRectangle(cornerRadius: tesseraShape.radius(tile: TesseraRadius.sm), style: .continuous))
    }
}

/// 徽标：数字小砖（count）或小方点（count = nil）
public struct TesseraBadge: View {
    let count: Int?
    let color: Color
    let max: Int
    @Environment(\.tesseraShape) private var tesseraShape

    public init(count: Int?, color: Color = TesseraColor.dangerSolid, max: Int = 99) {
        self.count = count
        self.color = color
        self.max = max
    }

    public var body: some View {
        Group {
            if let count {
                Text(count > max ? "\(max)+" : "\(count)")
                    .font(.system(size: 11, weight: .semibold).monospacedDigit())
                    .foregroundStyle(TesseraColor.textOnSolid)
                    .padding(.horizontal, 5)
                    .frame(minWidth: 18, minHeight: 18)
                    .background(color, in: RoundedRectangle(cornerRadius: tesseraShape.radius(tile: 7), style: .continuous))
                    .accessibilityLabel("\(count) 条未读")
            } else {
                RoundedRectangle(cornerRadius: tesseraShape.radius(tile: 3), style: .continuous).fill(color).frame(width: 8, height: 8)
                    .accessibilityLabel("有新消息")
            }
        }
        .padding(2)
        .background(TesseraColor.bgSurface, in: RoundedRectangle(cornerRadius: tesseraShape.radius(tile: 9), style: .continuous))
    }
}

// MARK: - 头像

/// 头像：36% 圆角的方砖，文字用软衬线
public struct TesseraAvatar: View {
    public enum Size: Sendable {
        case sm, md, lg
        var side: CGFloat { switch self { case .sm: 32; case .md: 40; case .lg: 56 } }
        var font: CGFloat { switch self { case .sm: 13; case .md: 16; case .lg: 22 } }
    }
    let text: String
    let tone: TesseraTone
    let size: Size
    @Environment(\.tesseraShape) private var tesseraShape

    public init(_ text: String, tone: TesseraTone = .accent, size: Size = .md) {
        self.text = text
        self.tone = tone
        self.size = size
    }

    public var body: some View {
        // 拼贴：36% 圆角的方砖；圆融：圆
        let shape = RoundedRectangle(cornerRadius: tesseraShape == .flow ? size.side * 0.38 : tesseraShape.isRound ? size.side / 2 : size.side * 0.36, style: .continuous)
        Text(String(text.prefix(2)))
            .font(.custom(TesseraFontFamily.display, fixedSize: size.font).weight(.semibold))
            .foregroundStyle(tone.colors.foreground)
            .frame(width: size.side, height: size.side)
            .background { shape.fill(TesseraColor.bgSurface); shape.fill(tone.colors.background) }
            .accessibilityLabel(text)
    }
}

/// 头像组：后一个叠在前一个上，之间留一道砖缝
public struct TesseraAvatarGroup: View {
    let names: [String]
    let max: Int
    @Environment(\.tesseraShape) private var tesseraShape

    public init(_ names: [String], max: Int = 4) {
        self.names = names
        self.max = max
    }

    public var body: some View {
        let tones: [TesseraTone] = [.lapis, .olive, .amethyst, .ochre, .cinnabar]
        // 圆融：描边细一些（2pt），叠得更紧
        let ring: CGFloat = tesseraShape.isRound ? 2 : TesseraTile.grout
        let ringShape = RoundedRectangle(cornerRadius: tesseraShape.isRound ? 20 : 14, style: .continuous)
        HStack(spacing: tesseraShape.isRound ? -8 : -6) {
            ForEach(Array(names.prefix(max).enumerated()), id: \.offset) { i, n in
                TesseraAvatar(n, tone: tones[i % tones.count], size: .sm).padding(ring)
                    .background(TesseraColor.bgSurface, in: ringShape)
            }
            if names.count > max {
                TesseraAvatar("+\(names.count - max)", tone: .neutral, size: .sm).padding(ring)
                    .background(TesseraColor.bgSurface, in: ringShape)
            }
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(names.joined(separator: "、"))
    }
}

// MARK: - 卡片

public struct TesseraCard<Content: View>: View {
    let elevated: Bool
    let padding: CGFloat
    let content: Content
    @Environment(\.tesseraShape) private var tesseraShape
    @Environment(\.colorSchemeContrast) private var contrast

    public init(elevated: Bool = false, padding: CGFloat = TesseraSpace.s6, @ViewBuilder content: () -> Content) {
        self.elevated = elevated
        self.padding = padding
        self.content = content()
    }

    public var body: some View {
        let shape = RoundedRectangle(cornerRadius: tesseraShape.radius(tile: TesseraRadius.xl, round: TesseraRound.card), style: .continuous)
        VStack(alignment: .leading, spacing: TesseraSpace.s4) { content }
            .padding(padding)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(elevated ? TesseraColor.bgElevated : TesseraColor.bgSurface, in: shape)
            // 圆融外观没有描边，只靠柔和的投影
            .overlay { shape.strokeBorder(tesseraShape.showsOutline(contrast) ? TesseraColor.borderSurface : .clear, lineWidth: 1) }
            .tesseraShadow(elevated ? .level2 : .level1)
    }
}

// MARK: - 列表

/// 列表。拼贴：每行一块砖，砖缝分隔，首尾外角大；圆融：整个列表是一整块圆角面，行与行之间没有缝和线
public struct TesseraList<Data: RandomAccessCollection, Row: View>: View where Data.Element: Identifiable {
    let data: Data
    let row: (Data.Element, TilePosition) -> Row
    @Environment(\.tesseraShape) private var tesseraShape

    public init(_ data: Data, @ViewBuilder row: @escaping (Data.Element, TilePosition) -> Row) {
        self.data = data
        self.row = row
    }

    public var body: some View {
        if tesseraShape.isRound {
            let shape = RoundedRectangle(cornerRadius: tesseraShape.roundRadius(TesseraRound.card), style: .continuous)
            VStack(spacing: 0) {
                ForEach(Array(data.enumerated()), id: \.element.id) { i, item in row(item, .of(i, count: data.count)) }
            }
            .padding(6)
            .background(TesseraColor.bgSurface, in: shape)
            .tesseraShadow(.level1)
        } else {
            VStack(spacing: TesseraTile.grout) {
                ForEach(Array(data.enumerated()), id: \.element.id) { i, item in row(item, .of(i, count: data.count)) }
            }
        }
    }
}

public struct TesseraListItem<Leading: View, Trailing: View>: View {
    let title: String
    let subtitle: String?
    let position: TilePosition
    let leading: Leading
    let trailing: Trailing
    let action: (() -> Void)?
    @Environment(\.tesseraShape) private var tesseraShape

    public init(_ title: String, subtitle: String? = nil, position: TilePosition, action: (() -> Void)? = nil,
                @ViewBuilder leading: () -> Leading, @ViewBuilder trailing: () -> Trailing) {
        self.title = title
        self.subtitle = subtitle
        self.position = position
        self.action = action
        self.leading = leading()
        self.trailing = trailing()
    }

    public var body: some View {
        let round = tesseraShape.isRound
        let shape = round ? tileShape(.single, outer: tesseraShape.roundRadius(TesseraRound.row)) : tileShape(position, outer: TesseraRadius.lg, vertical: true)
        let row = HStack(spacing: TesseraSpace.s3) {
            leading
            VStack(alignment: .leading, spacing: 1) {
                TesseraLabel(title, style: .label).lineLimit(1)
                if let subtitle { TesseraLabel(subtitle, style: .caption, color: TesseraColor.textSecondary).lineLimit(2) }
            }
            Spacer(minLength: 0)
            trailing.foregroundStyle(TesseraColor.textTertiary)
        }
        .padding(.horizontal, round ? TesseraSpace.s3 : TesseraSpace.s4)
        .padding(.vertical, TesseraSpace.s2)
        .frame(minHeight: 60)
        .background(round ? Color.clear : TesseraColor.bgSurface, in: shape)
        .contentShape(shape)
        if let action {
            Button(action: action) { row }.buttonStyle(RowPress(shape: shape))
        } else { row }
    }

    private struct RowPress<S: Shape>: ButtonStyle {
        let shape: S
        @Environment(\.tesseraShape) private var tesseraShape
        func makeBody(configuration: Configuration) -> some View {
            configuration.label.overlay { if configuration.isPressed { shape.fill(TesseraColor.statePressed) } }
                .tesseraFlowRipple(pressed: configuration.isPressed, active: tesseraShape == .flow, color: TesseraColor.textPrimary.opacity(0.13), in: shape)
        }
    }
}

public extension TesseraListItem where Leading == EmptyView, Trailing == EmptyView {
    init(_ title: String, subtitle: String? = nil, position: TilePosition, action: (() -> Void)? = nil) {
        self.init(title, subtitle: subtitle, position: position, action: action, leading: { EmptyView() }, trailing: { EmptyView() })
    }
}

// MARK: - 骨架屏

/// 骨架屏：光带 1.5 秒扫一次；“减弱动态效果”时静止
public struct TesseraSkeleton: View {
    let cornerRadius: CGFloat?
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @Environment(\.tesseraShape) private var tesseraShape

    /// cornerRadius 为 nil 时：拼贴 6，圆融为胶囊
    public init(cornerRadius: CGFloat? = nil) { self.cornerRadius = cornerRadius }

    public var body: some View {
        let shape = RoundedRectangle(cornerRadius: cornerRadius ?? tesseraShape.radius(tile: TesseraRadius.grout), style: .continuous)
        TimelineView(.animation(paused: reduceMotion)) { ctx in
            let t = ctx.date.timeIntervalSinceReferenceDate.truncatingRemainder(dividingBy: 1.5) / 1.5
            GeometryReader { g in
                shape.fill(TesseraColor.fillDefault)
                    .overlay {
                        if !reduceMotion {
                            LinearGradient(colors: [.clear, TesseraColor.bgSurface.opacity(0.7), .clear], startPoint: .leading, endPoint: .trailing)
                                .frame(width: g.size.width * 0.5)
                                .offset(x: (t * 3 - 1) * g.size.width)
                        }
                    }
                    .clipShape(shape)
            }
        }
        .accessibilityLabel("加载中")
    }
}
