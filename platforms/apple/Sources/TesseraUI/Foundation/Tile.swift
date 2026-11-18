import SwiftUI

// MARK: - 拼贴

/// 拼贴组里一块砖的位置：决定哪几个角是“外角”（大圆角）
public enum TilePosition: Sendable {
    case single, first, middle, last

    /// 组内第 index 块（共 count 块）的位置
    public static func of(_ index: Int, count: Int) -> TilePosition {
        if count <= 1 { return .single }
        if index == 0 { return .first }
        if index == count - 1 { return .last }
        return .middle
    }
}

/// 拼贴形状：外角用 outer，砖与砖相邻的内角用 inner（默认 6）；连续曲率圆角
public func tileShape(_ position: TilePosition, outer: CGFloat, inner: CGFloat = TesseraRadius.grout, vertical: Bool = false) -> UnevenRoundedRectangle {
    let r: RectangleCornerRadii
    switch (position, vertical) {
    case (.single, _): r = .init(topLeading: outer, bottomLeading: outer, bottomTrailing: outer, topTrailing: outer)
    case (.middle, _): r = .init(topLeading: inner, bottomLeading: inner, bottomTrailing: inner, topTrailing: inner)
    case (.first, false): r = .init(topLeading: outer, bottomLeading: outer, bottomTrailing: inner, topTrailing: inner)
    case (.last, false): r = .init(topLeading: inner, bottomLeading: inner, bottomTrailing: outer, topTrailing: outer)
    case (.first, true): r = .init(topLeading: outer, bottomLeading: inner, bottomTrailing: inner, topTrailing: outer)
    case (.last, true): r = .init(topLeading: inner, bottomLeading: outer, bottomTrailing: outer, topTrailing: inner)
    }
    return UnevenRoundedRectangle(cornerRadii: r, style: .continuous)
}

// MARK: - 釉面砖

/// 砖面的样式：实色砖（釉光 + 深砖厚）、浅色砖（浅砖厚）、凹槽（顶部内阴影）
public enum TesseraSurfaceStyle: Sendable { case solid, soft, sunken, plain }

public extension View {
    /// 给视图铺一块砖面：填充色 + 质感，用 shape 裁切（渐变色带随圆角裁切，边缘平滑）。
    /// 圆融外观下：实色砖改为整面柔光，浅色砖与凹槽是平的，没有砖厚、内阴影和描边。
    func tesseraTile<S: InsettableShape>(_ fill: Color, style: TesseraSurfaceStyle, shape: S, pressed: Bool = false) -> some View {
        modifier(TileSurface(fill: fill, style: style, shape: shape, pressed: pressed))
    }

    /// 聚焦光晕：外侧 3pt 半透明光晕 + 1pt 焦点描边
    func tesseraFocusGlow(_ on: Bool, radius: CGFloat, error: Bool = false) -> some View {
        overlay {
            RoundedRectangle(cornerRadius: radius, style: .continuous)
                .strokeBorder(error ? TesseraColor.dangerText : TesseraColor.focusRing, lineWidth: 1)
                .opacity(on ? 1 : 0)
        }
        .background {
            RoundedRectangle(cornerRadius: radius + TesseraFocus.glowWidth, style: .continuous)
                .fill(error ? TesseraColor.dangerSubtle : TesseraColor.focusGlow)
                .padding(-TesseraFocus.glowWidth)
                .opacity(on ? 1 : 0)
        }
        .animation(.easeOut(duration: TesseraMotion.durationFast), value: on)
    }
}

struct TileSurface<S: InsettableShape>: ViewModifier {
    let fill: Color
    let style: TesseraSurfaceStyle
    let shape: S
    let pressed: Bool
    @Environment(\.tesseraShape) private var tesseraShape

    func body(content: Content) -> some View {
        content.background {
            ZStack {
                shape.fill(fill)
                if tesseraShape == .round {
                    if style == .solid {
                        // 整面柔光：顶部略亮、底部略暗，没有硬边
                        LinearGradient(stops: [.init(color: .white.opacity(0.14), location: 0), .init(color: .clear, location: 0.6)], startPoint: .top, endPoint: .bottom)
                        LinearGradient(stops: [.init(color: .clear, location: 0.45), .init(color: .black.opacity(0.06), location: 1)], startPoint: .top, endPoint: .bottom)
                    }
                } else {
                    switch style {
                    case .solid:
                        VStack(spacing: 0) {
                            LinearGradient(colors: [TesseraColor.sheen, .clear], startPoint: .top, endPoint: .bottom).frame(height: 2)
                            Spacer(minLength: 0)
                            LinearGradient(stops: [.init(color: .clear, location: 0), .init(color: TesseraColor.edgeSolid, location: 0.35), .init(color: TesseraColor.edgeSolid, location: 1)],
                                           startPoint: .top, endPoint: .bottom).frame(height: (pressed ? 1 : TesseraTile.edge) * 1.4)
                        }
                    case .soft:
                        VStack(spacing: 0) {
                            Spacer(minLength: 0)
                            LinearGradient(stops: [.init(color: .clear, location: 0), .init(color: TesseraColor.edgeSoft, location: 0.35), .init(color: TesseraColor.edgeSoft, location: 1)],
                                           startPoint: .top, endPoint: .bottom).frame(height: (pressed ? 1 : TesseraTile.edge) * 1.4)
                        }
                    case .sunken:
                        VStack(spacing: 0) {
                            LinearGradient(colors: [TesseraColor.edgeSoft, .clear], startPoint: .top, endPoint: .bottom).frame(height: 3)
                            Spacer(minLength: 0)
                        }
                    case .plain:
                        EmptyView()
                    }
                }
            }
            .clipShape(shape)
            .overlay { shape.strokeBorder(style == .sunken && tesseraShape == .tile ? TesseraColor.borderField : .clear, lineWidth: 1) }
        }
    }
}

// MARK: - 动效

public extension Animation {
    /// 系统“减弱动态效果”打开时退化为 150ms 淡入淡出
    static func tessera(_ spring: Animation, reduceMotion: Bool) -> Animation {
        reduceMotion ? .easeInOut(duration: TesseraMotion.durationFast) : spring
    }
}

// MARK: - 图标

/// 图标：单色，跟随前景色；label 为 nil 时视为装饰图标，读屏会跳过
public struct TesseraIcon: View {
    let name: TesseraIconName
    let size: CGFloat
    let label: String?

    public init(_ name: TesseraIconName, size: CGFloat = 20, label: String? = nil) {
        self.name = name
        self.size = size
        self.label = label
    }

    public var body: some View {
        Image(name.rawValue, bundle: .module)
            .renderingMode(.template)
            .resizable()
            .interpolation(.high)
            .frame(width: size, height: size)
            .accessibilityLabel(label ?? "")
            .accessibilityHidden(label == nil)
    }
}

public extension Image {
    /// Image(tessera: .homeFill)
    init(tessera name: TesseraIconName) {
        self.init(name.rawValue, bundle: .module)
    }
}

// MARK: - 文字

/// 文字：默认正文样式、主文字颜色
public struct TesseraLabel: View {
    let text: String
    let style: TesseraTextStyle
    let color: Color

    public init(_ text: String, style: TesseraTextStyle = .body, color: Color = TesseraColor.textPrimary) {
        self.text = text
        self.style = style
        self.color = color
    }

    public var body: some View {
        Text(text).tesseraText(style).foregroundStyle(color)
    }
}
