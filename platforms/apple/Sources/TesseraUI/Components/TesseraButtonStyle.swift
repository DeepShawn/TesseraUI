import SwiftUI

/// 按钮：圆角方砖（连续曲率）。实色砖带釉光与砖厚，浅色砖带浅砖厚；
/// 按下下沉 1pt、砖厚变薄，松手 snappy 弹回。热区不小于 44×44。
public struct TesseraButtonStyle: ButtonStyle {
    public enum Variant: Sendable { case primary, secondary, text, danger }
    public enum Size: Sendable {
        case sm, md, lg
        var height: CGFloat { switch self { case .sm: TesseraSize.sm; case .md: TesseraSize.md; case .lg: TesseraSize.lg } }
        var padding: CGFloat { switch self { case .sm: TesseraSpace.s3; case .md: TesseraSpace.s4; case .lg: TesseraSpace.s6 } }
        var radius: CGFloat { switch self { case .sm: 11; case .md: TesseraRadius.md; case .lg: 17 } }
    }

    let variant: Variant
    let size: Size
    let position: TilePosition?
    let fullWidth: Bool
    @Environment(\.isEnabled) private var isEnabled
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @Environment(\.tesseraShape) private var tesseraShape

    public init(_ variant: Variant = .primary, size: Size = .md, position: TilePosition? = nil, fullWidth: Bool = false) {
        self.variant = variant
        self.size = size
        self.position = position
        self.fullWidth = fullWidth
    }

    private var fill: Color {
        switch variant {
        case .primary: TesseraColor.accentSolid
        case .danger: TesseraColor.dangerSolid
        case .secondary: TesseraColor.fillDefault
        case .text: .clear
        }
    }
    private var foreground: Color {
        switch variant {
        case .primary, .danger: TesseraColor.textOnSolid
        case .secondary: TesseraColor.textPrimary
        case .text: TesseraColor.accentText
        }
    }
    private var isSolid: Bool { variant == .primary || variant == .danger }

    public func makeBody(configuration: Configuration) -> some View {
        let pressed = configuration.isPressed
        // 拼贴：圆角方砖，成组时外角大、内角 6；圆融：胶囊，成组时合成一整条
        // 流韵：静止时方圆之间，按下变成胶囊，松手 bouncy 弹回
        let flow = tesseraShape == .flow
        let morph = flow && pressed && position == nil
        let shape = tesseraShape.groupShape(position ?? .single, outer: position == nil ? size.radius : TesseraRadius.lg,
                                            roundOuter: morph ? size.height / 2 : TesseraRound.pill)
        return configuration.label
            .tesseraText(size == .lg ? TesseraTextStyle(size: 16, lineHeight: 20, weight: .semibold, family: .text, tracking: 0, relativeTo: .body) : .label)
            .lineLimit(1)
            .foregroundStyle(foreground)
            .padding(.horizontal, variant == .text ? TesseraSpace.s3 : size.padding + (tesseraShape.isRound ? TesseraSpace.s1 : 0))
            .padding(.bottom, variant == .text || tesseraShape.isRound ? 0 : 1)
            .frame(maxWidth: fullWidth ? .infinity : nil, minHeight: size.height)
            .tesseraTile(fill, style: variant == .text ? .plain : (isSolid ? .solid : .soft), shape: shape, pressed: pressed)
            .overlay { if pressed { shape.fill(isSolid ? TesseraColor.stateSolidPressed : TesseraColor.statePressed) } }
            .tesseraFlowRipple(pressed: pressed, active: flow && variant != .text, color: foreground.opacity(isSolid ? 0.22 : 0.13), in: shape)
            .contentShape(Rectangle())
            .frame(minWidth: TesseraSize.touchMin, minHeight: TesseraSize.touchMin)
            .tesseraPress(pressed, shape: tesseraShape, reduceMotion: reduceMotion)
            .animation(pressed ? .easeOut(duration: TesseraMotion.pressDuration) : .tessera(flow ? TesseraMotion.bouncy : TesseraMotion.snappy, reduceMotion: reduceMotion), value: pressed)
            .sensoryFeedback(.impact(flexibility: .soft, intensity: 0.6), trigger: pressed) { _, now in now && flow && isSolid }
            .opacity(isEnabled ? 1 : TesseraOpacity.disabled)
    }
}

public extension ButtonStyle where Self == TesseraButtonStyle {
    static func tessera(_ variant: TesseraButtonStyle.Variant = .primary, size: TesseraButtonStyle.Size = .md, position: TilePosition? = nil, fullWidth: Bool = false) -> TesseraButtonStyle {
        TesseraButtonStyle(variant, size: size, position: position, fullWidth: fullWidth)
    }
}

/// 图标按钮：方砖形；filled 时有底色和砖厚
public struct TesseraIconButton: View {
    let icon: TesseraIconName
    let label: String
    let filled: Bool
    let badge: Int?
    let action: () -> Void

    public init(_ icon: TesseraIconName, label: String, filled: Bool = false, badge: Int? = nil, action: @escaping () -> Void) {
        self.icon = icon
        self.label = label
        self.filled = filled
        self.badge = badge
        self.action = action
    }

    public var body: some View {
        Button(action: action) { TesseraIcon(icon) }
            .buttonStyle(IconStyle(filled: filled))
            .overlay(alignment: .topTrailing) {
                if let badge { TesseraBadge(count: badge).offset(x: -2, y: 2).allowsHitTesting(false) }
            }
            .accessibilityLabel(label)
    }

    private struct IconStyle: ButtonStyle {
        let filled: Bool
        @Environment(\.accessibilityReduceMotion) private var reduceMotion
        @Environment(\.tesseraShape) private var tesseraShape
        func makeBody(configuration: Configuration) -> some View {
            let flow = tesseraShape == .flow
            let shape = RoundedRectangle(cornerRadius: flow && configuration.isPressed ? TesseraSize.md / 2 : tesseraShape.radius(tile: TesseraRadius.md), style: .continuous)
            configuration.label
                .foregroundStyle(TesseraColor.textPrimary)
                .frame(width: TesseraSize.md, height: TesseraSize.md)
                .tesseraTile(filled ? TesseraColor.fillDefault : .clear, style: filled ? .soft : .plain, shape: shape, pressed: configuration.isPressed)
                .overlay { if configuration.isPressed { shape.fill(TesseraColor.statePressed) } }
                .tesseraFlowRipple(pressed: configuration.isPressed, active: flow, color: TesseraColor.textPrimary.opacity(0.13), in: shape)
                .frame(width: TesseraSize.touchMin, height: TesseraSize.touchMin)
                .contentShape(Rectangle())
                .tesseraPress(configuration.isPressed, shape: tesseraShape, reduceMotion: reduceMotion)
                .animation(.tessera(flow ? TesseraMotion.bouncy : TesseraMotion.snappy, reduceMotion: reduceMotion), value: configuration.isPressed)
        }
    }
}

/// 加载指示：四块小砖轮流亮起（只用在按钮和小区域里）
public struct TesseraSpinner: View {
    let size: CGFloat
    @State private var step = 0
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @Environment(\.tesseraShape) private var tesseraShape

    public init(size: CGFloat = 20) { self.size = size }

    public var body: some View {
        let gap = size / 10, tile = (size - gap) / 2
        let order = [0, 1, 3, 2] // 顺时针：左上 → 右上 → 右下 → 左下
        TimelineView(.periodic(from: .now, by: 0.25)) { context in
            let active = reduceMotion ? 0 : Int(context.date.timeIntervalSinceReferenceDate * 4) % 4
            VStack(spacing: gap) {
                ForEach(0..<2, id: \.self) { row in
                    HStack(spacing: gap) {
                        ForEach(0..<2, id: \.self) { col in
                            // 圆融外观：四颗小圆点
                            RoundedRectangle(cornerRadius: tesseraShape.isRound ? tile / 2 : tile / 3, style: .continuous)
                                .frame(width: tile, height: tile)
                                .opacity(order.firstIndex(of: row * 2 + col) == active ? 1 : 0.25)
                        }
                    }
                }
            }
        }
        .frame(width: size, height: size)
        .accessibilityLabel("加载中")
    }
}
