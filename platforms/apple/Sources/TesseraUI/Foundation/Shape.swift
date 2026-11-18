import SwiftUI

// MARK: - 外观

/// 外观。
/// - `tile`（拼贴，默认）：圆角方砖，成组时外角大、内角小，中间留砖缝；砖有釉光和砖厚，输入框是凹槽。
/// - `round`（圆融）：胶囊与大圆角，成组控件合成一整块，没有砖缝和分割线；去掉砖厚和凹槽，按下是轻轻一捏。
public enum TesseraShape: Sendable {
    case tile, round

    /// 单个控件的圆角：拼贴用 tile，圆融用 round（默认胶囊）
    public func radius(tile: CGFloat, round: CGFloat = TesseraRound.pill) -> CGFloat {
        self == .round ? round : tile
    }

    /// 成组控件之间的缝：拼贴 3pt，圆融 0（合成一整块）
    public var groupGap: CGFloat { self == .round ? TesseraRound.groupGap : TesseraTile.grout }

    /// 独立的一排胶囊之间的间距（对话框按钮等）：拼贴仍是砖缝，圆融 8pt
    public var pillGap: CGFloat { self == .round ? TesseraRound.gap : TesseraTile.grout }

    /// 成组控件里一块的形状：拼贴是外角 outer、内角 6；圆融是外角 roundOuter（默认胶囊）、内角 0
    public func groupShape(_ position: TilePosition, outer: CGFloat, vertical: Bool = false, roundOuter: CGFloat = TesseraRound.pill) -> UnevenRoundedRectangle {
        self == .round ? tileShape(position, outer: roundOuter, inner: 0, vertical: vertical) : tileShape(position, outer: outer, vertical: vertical)
    }
}

private struct TesseraShapeKey: EnvironmentKey {
    static let defaultValue: TesseraShape = .tile
}

public extension EnvironmentValues {
    /// 当前外观
    var tesseraShape: TesseraShape {
        get { self[TesseraShapeKey.self] }
        set { self[TesseraShapeKey.self] = newValue }
    }
}

public extension View {
    /// 选择外观：`.tesseraShape(.round)` 切换为圆融；默认拼贴。对其中所有 TesseraUI 组件生效。
    func tesseraShape(_ shape: TesseraShape) -> some View {
        environment(\.tesseraShape, shape)
    }

    /// 按下反馈：拼贴下沉 1pt，圆融缩小到 97%；“减弱动态效果”时不位移、不缩放
    func tesseraPress(_ pressed: Bool, shape: TesseraShape, reduceMotion: Bool) -> some View {
        let active = pressed && !reduceMotion
        return self
            .offset(y: active && shape == .tile ? TesseraMotion.pressSink : 0)
            .scaleEffect(active && shape == .round ? TesseraRound.pressScale : 1)
    }
}
