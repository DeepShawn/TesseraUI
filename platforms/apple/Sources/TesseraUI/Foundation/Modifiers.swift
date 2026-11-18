import SwiftUI

public extension View {
    /// 套用一档文字样式：字号随“动态字体”缩放，行高按 token 设定。
    func tesseraText(_ style: TesseraTextStyle) -> some View {
        modifier(TesseraTextModifier(style: style))
    }

    /// 套用一级阴影；深色模式自动改用 40% 不透明度并补 1px 描边（由调用方的形状决定）。
    func tesseraShadow(_ shadow: TesseraShadow) -> some View {
        modifier(TesseraShadowModifier(shadow: shadow))
    }
}

private struct TesseraTextModifier: ViewModifier {
    let style: TesseraTextStyle
    @ScaledMetric private var scale: CGFloat = 1

    init(style: TesseraTextStyle) {
        self.style = style
        _scale = ScaledMetric(wrappedValue: 1, relativeTo: style.relativeTo)
    }

    func body(content: Content) -> some View {
        let size = style.size * scale
        // SwiftUI 没有“行高”，用 lineSpacing 近似：行高 − 字体默认行高（约 1.2 倍字号）。
        content
            .font(.custom(style.family == .display ? TesseraFontFamily.display : TesseraFontFamily.text, fixedSize: size).weight(style.weight))
            .tracking(style.tracking * size)
            .lineSpacing(max(0, (style.lineHeight - style.size * 1.2) * scale))
    }
}

private struct TesseraShadowModifier: ViewModifier {
    let shadow: TesseraShadow
    @Environment(\.colorScheme) private var scheme

    func body(content: Content) -> some View {
        shadow.layers.reduce(AnyView(content)) { view, layer in
            AnyView(view.shadow(
                // 阴影带石色暖调（浅色用墨色 #1F1B16，深色用黑）
                color: TesseraColor.shadowColor.opacity(scheme == .dark ? layer.darkOpacity : layer.lightOpacity),
                radius: layer.blur / 2, // CSS 的 blur 约等于 SwiftUI radius 的 2 倍
                x: 0, y: layer.y
            ))
        }
    }
}
