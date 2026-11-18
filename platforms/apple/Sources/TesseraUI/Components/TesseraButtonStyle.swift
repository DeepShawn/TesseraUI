import SwiftUI

/// 按钮：胶囊形；按下 100ms 缩到 0.97，松手用 snappy 弹回。
public struct TesseraButtonStyle: ButtonStyle {
    public enum Variant { case primary, secondary, text, danger }
    public enum Size { case sm, md, lg }

    let variant: Variant
    let size: Size
    @Environment(\.isEnabled) private var isEnabled

    public init(_ variant: Variant = .primary, size: Size = .md) {
        self.variant = variant
        self.size = size
    }

    private var height: CGFloat {
        switch size { case .sm: TesseraSize.sm; case .md: TesseraSize.md; case .lg: TesseraSize.lg }
    }
    private var padding: CGFloat {
        switch size { case .sm: TesseraSpace.s3; case .md: TesseraSpace.s4; case .lg: TesseraSpace.s6 }
    }
    private var background: Color {
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
        configuration.label
            .tesseraText(.label)
            .foregroundStyle(foreground)
            .padding(.horizontal, padding)
            .frame(minHeight: height)
            .background(background, in: Capsule())
            .overlay {
                // 按下叠加层：实色按钮叠黑 8%，其他叠 state.pressed
                if configuration.isPressed {
                    Capsule().fill(isSolid ? TesseraColor.stateSolidPressed : TesseraColor.statePressed)
                }
            }
            .frame(minWidth: TesseraSize.touchMin, minHeight: TesseraSize.touchMin) // 热区 ≥ 44
            .contentShape(Rectangle())
            .scaleEffect(configuration.isPressed ? TesseraMotion.pressScale : 1)
            .animation(
                configuration.isPressed ? .easeOut(duration: TesseraMotion.durationInstant) : TesseraMotion.snappy,
                value: configuration.isPressed
            )
            .opacity(isEnabled ? 1 : TesseraOpacity.disabled)
    }
}

public extension ButtonStyle where Self == TesseraButtonStyle {
    static func tessera(_ variant: TesseraButtonStyle.Variant = .primary, size: TesseraButtonStyle.Size = .md) -> TesseraButtonStyle {
        TesseraButtonStyle(variant, size: size)
    }
}
