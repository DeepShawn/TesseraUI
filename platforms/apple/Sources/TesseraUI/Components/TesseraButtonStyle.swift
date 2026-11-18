import SwiftUI

/// 按钮：圆角方砖（连续曲率）。实色砖顶部一道釉光、底部 2pt 砖厚；按下时下沉 1pt、砖厚变薄，松手用 snappy 弹回。
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
    private var radius: CGFloat {
        switch size { case .sm: 11; case .md: TesseraRadius.md; case .lg: 17 }
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
        let pressed = configuration.isPressed
        let shape = RoundedRectangle(cornerRadius: radius, style: .continuous)
        let edge = pressed ? 1 : TesseraTile.edge
        return configuration.label
            .tesseraText(.label)
            .foregroundStyle(foreground)
            .padding(.horizontal, padding)
            .padding(.bottom, variant == .text ? 0 : 1)
            .frame(minHeight: height)
            .background {
                if variant != .text {
                    ZStack(alignment: .bottom) {
                        shape.fill(background)
                        // 砖厚：底部一条深色带，被形状裁切
                        Rectangle()
                            .fill(isSolid ? TesseraColor.edgeSolid : TesseraColor.edgeSoft)
                            .frame(height: edge)
                        if isSolid {
                            // 釉光：顶部 1pt 高光
                            VStack { Rectangle().fill(TesseraColor.sheen).frame(height: 1); Spacer() }
                        }
                        if pressed {
                            shape.fill(isSolid ? TesseraColor.stateSolidPressed : TesseraColor.statePressed)
                        }
                    }
                    .clipShape(shape)
                }
            }
            .frame(minWidth: TesseraSize.touchMin, minHeight: TesseraSize.touchMin) // 热区 ≥ 44
            .contentShape(Rectangle())
            .offset(y: pressed ? TesseraMotion.pressSink : 0)
            .animation(
                pressed ? .easeOut(duration: TesseraMotion.pressDuration) : TesseraMotion.snappy,
                value: pressed
            )
            .opacity(isEnabled ? 1 : TesseraOpacity.disabled)
    }
}

public extension ButtonStyle where Self == TesseraButtonStyle {
    static func tessera(_ variant: TesseraButtonStyle.Variant = .primary, size: TesseraButtonStyle.Size = .md) -> TesseraButtonStyle {
        TesseraButtonStyle(variant, size: size)
    }
}
