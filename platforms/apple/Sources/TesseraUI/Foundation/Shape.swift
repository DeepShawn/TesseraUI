import SwiftUI
import Combine

// MARK: - 外观

/// 外观。
/// - `tile`（拼贴，默认）：圆角方砖，成组时外角大、内角小，中间留砖缝；砖有釉光和砖厚，输入框是凹槽。
/// - `round`（圆融）：胶囊与大圆角，成组控件合成一整块，没有砖缝和分割线；去掉砖厚和凹槽，按下是轻轻一捏。
/// - `flow`（流韵）：以圆融为基础，圆角介于方砖与胶囊之间（按下时变成胶囊），按下处荡开水波，
///   开关溅起涟漪、滑块像水滴一样晃，并有触感反馈。只在性能较好的设备上启用，否则自动改用 `round`。
public enum TesseraShape: Sendable {
    case tile, round, flow

    /// 圆融或流韵：成组控件合成一整块，没有砖缝、砖厚和分割线
    public var isRound: Bool { self != .tile }

    /// 圆融的圆角在流韵下换成“方圆之间”的值：胶囊 → 17、卡片 28 → 26、面板 36 → 34、行 22 → 18、菜单 24 → 22
    public func roundRadius(_ round: CGFloat) -> CGFloat {
        guard self == .flow else { return round }
        switch round {
        case TesseraRound.pill: return TesseraFlow.control
        case TesseraRound.card: return TesseraFlow.card
        case TesseraRound.panel: return TesseraFlow.panel
        case TesseraRound.row: return TesseraFlow.row
        case TesseraRound.menu: return TesseraFlow.menu
        default: return round
        }
    }

    /// 单个控件的圆角：拼贴用 tile，圆融用 round（默认胶囊），流韵用方圆之间的值
    public func radius(tile: CGFloat, round: CGFloat = TesseraRound.pill) -> CGFloat {
        isRound ? roundRadius(round) : tile
    }

    /// 成组控件之间的缝：拼贴 3pt，圆融 / 流韵 0（合成一整块）
    public var groupGap: CGFloat { isRound ? TesseraRound.groupGap : TesseraTile.grout }

    /// 独立的一排胶囊之间的间距（对话框按钮等）：拼贴仍是砖缝，圆融 / 流韵 8pt
    public var pillGap: CGFloat { isRound ? TesseraRound.gap : TesseraTile.grout }

    /// 成组控件里一块的形状：拼贴是外角 outer、内角 6；圆融是外角 roundOuter（默认胶囊）、内角 0
    public func groupShape(_ position: TilePosition, outer: CGFloat, vertical: Bool = false, roundOuter: CGFloat = TesseraRound.pill) -> UnevenRoundedRectangle {
        isRound ? tileShape(position, outer: roundRadius(roundOuter), inner: 0, vertical: vertical) : tileShape(position, outer: outer, vertical: vertical)
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
    /// 选择外观：`.tesseraShape(.round)` 切换为圆融，`.tesseraShape(.flow)` 切换为流韵；默认拼贴。对其中所有 TesseraUI 组件生效。
    /// 流韵只在性能较好的设备上启用：打开“减弱动态效果”、低电量模式，或处理器少于 6 核、内存少于 4GB 时自动改用圆融；
    /// requireCapableDevice = false 跳过设备检测（“减弱动态效果”仍然生效）。
    func tesseraShape(_ shape: TesseraShape, requireCapableDevice: Bool = true) -> some View {
        modifier(ShapeResolver(shape: shape, requireCapableDevice: requireCapableDevice))
    }

    /// 按下反馈：拼贴下沉 1pt，圆融缩小到 97%；“减弱动态效果”时不位移、不缩放
    func tesseraPress(_ pressed: Bool, shape: TesseraShape, reduceMotion: Bool) -> some View {
        let active = pressed && !reduceMotion
        return self
            .offset(y: active && shape == .tile ? TesseraMotion.pressSink : 0)
            .scaleEffect(active && shape.isRound ? (shape == .flow ? TesseraFlow.pressScale : TesseraRound.pressScale) : 1)
    }
}

/// 设备是否适合流韵的持续动效
public func tesseraFlowCapable() -> Bool {
    let info = ProcessInfo.processInfo
    if info.isLowPowerModeEnabled { return false }
    if info.activeProcessorCount < TesseraFlow.minCores { return false }
    return Double(info.physicalMemory) >= Double(TesseraFlow.minMemoryGB) * 0.9 * 1_073_741_824
}

struct ShapeResolver: ViewModifier {
    let shape: TesseraShape
    let requireCapableDevice: Bool
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    /// 低电量模式在使用中打开或关闭时，外观实时在流韵与圆融之间切换
    @State private var capable = tesseraFlowCapable()

    func body(content: Content) -> some View {
        let resolved: TesseraShape = shape == .flow && (reduceMotion || (requireCapableDevice && !capable)) ? .round : shape
        content
            .environment(\.tesseraShape, resolved)
            .onReceive(NotificationCenter.default.publisher(for: Notification.Name.NSProcessInfoPowerStateDidChange)) { _ in
                capable = tesseraFlowCapable()
            }
    }
}

/// 四端统一的触感种类：选择（切换选项）、轻触、中等（开关打开）、确认（成功）、警告（危险操作、失败）
public enum TesseraHaptic: Sendable {
    case selection, light, medium, success, warning

    var feedback: SensoryFeedback {
        switch self {
        case .selection: .selection
        case .light: .impact(flexibility: .soft, intensity: 0.6)
        case .medium: .impact(weight: .medium, intensity: 0.7)
        case .success: .success
        case .warning: .warning
        }
    }
}

public extension View {
    /// 流韵外观下的触感反馈：trigger 变化且 condition 为真时触发；其他外观不触发（macOS 上无效果）
    func tesseraHaptic<T: Equatable>(_ kind: TesseraHaptic, trigger: T, when condition: @escaping (T, T) -> Bool = { _, _ in true }) -> some View {
        modifier(FlowHaptic(kind: kind, trigger: trigger, condition: condition))
    }
}

struct FlowHaptic<T: Equatable>: ViewModifier {
    let kind: TesseraHaptic
    let trigger: T
    let condition: (T, T) -> Bool
    @Environment(\.tesseraShape) private var tesseraShape

    func body(content: Content) -> some View {
        content.sensoryFeedback(kind.feedback, trigger: trigger) { old, new in tesseraShape == .flow && condition(old, new) }
    }
}
