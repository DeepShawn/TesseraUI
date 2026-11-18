import SwiftUI

// MARK: - 流韵外观的动效
// 水波、涟漪、水滴形变、水珠弹跳、液面张力。只在 `.tesseraShape(.flow)` 下生效；“减弱动态效果”时外观自动改为圆融，这些动效都不出现。

struct WobbleValue {
    var x: CGFloat = 1
    var y: CGFloat = 1
}

public extension View {
    /// 水滴形变：先被拉扁、再回弹、最后轻晃两下（开关滑块）
    func tesseraDropWobble<T: Equatable>(trigger: T, active: Bool) -> some View {
        keyframeAnimator(initialValue: WobbleValue(), trigger: active ? AnyHashableBox(trigger) : AnyHashableBox(0 as Int)) { view, v in
            view.scaleEffect(x: v.x, y: v.y)
        } keyframes: { _ in
            KeyframeTrack(\.x) {
                CubicKeyframe(1.22, duration: 0.11); CubicKeyframe(0.9, duration: 0.14)
                CubicKeyframe(1.05, duration: 0.135); CubicKeyframe(0.98, duration: 0.125); CubicKeyframe(1, duration: 0.11)
            }
            KeyframeTrack(\.y) {
                CubicKeyframe(0.82, duration: 0.11); CubicKeyframe(1.1, duration: 0.14)
                CubicKeyframe(0.96, duration: 0.135); CubicKeyframe(1.02, duration: 0.125); CubicKeyframe(1, duration: 0.11)
            }
        }
    }

    /// 水珠弹跳：0.82 → 1.12 → 0.97 → 1（复选框、单选、标签栏图标）
    func tesseraBeadPop<T: Equatable>(trigger: T, active: Bool) -> some View {
        keyframeAnimator(initialValue: CGFloat(1), trigger: active ? AnyHashableBox(trigger) : AnyHashableBox(0 as Int)) { view, s in
            view.scaleEffect(s)
        } keyframes: { _ in
            KeyframeTrack {
                CubicKeyframe(0.82, duration: 0.12); CubicKeyframe(1.12, duration: 0.144)
                CubicKeyframe(0.97, duration: 0.12); CubicKeyframe(1, duration: 0.096)
            }
        }
    }

    /// 液面张力：选中块移动时被拉扁，停下时回弹（分段控制）
    func tesseraTension<T: Equatable>(trigger: T, active: Bool) -> some View {
        keyframeAnimator(initialValue: CGFloat(1), trigger: active ? AnyHashableBox(trigger) : AnyHashableBox(0 as Int)) { view, s in
            view.scaleEffect(x: 1, y: s)
        } keyframes: { _ in
            KeyframeTrack {
                CubicKeyframe(0.84, duration: 0.156); CubicKeyframe(1.06, duration: 0.166)
                CubicKeyframe(0.98, duration: 0.104); CubicKeyframe(1, duration: 0.094)
            }
        }
    }

    /// 涟漪：以 anchor 为中心向外扩散两圈细环，不裁切（开关、复选框、单选）
    func tesseraSplash<T: Equatable>(trigger: T, active: Bool, color: Color, diameter: CGFloat, anchor: UnitPoint = .center) -> some View {
        overlay(alignment: .topLeading) {
            GeometryReader { geo in
                SplashRings(color: color, diameter: diameter)
                    .keyframeAnimator(initialValue: CGFloat(1), trigger: active ? AnyHashableBox(trigger) : AnyHashableBox(0 as Int)) { rings, p in
                        rings.environment(\.splashProgress, p)
                    } keyframes: { _ in
                        KeyframeTrack {
                            LinearKeyframe(0, duration: 0.001)
                            CubicKeyframe(1, duration: TesseraFlow.splashDuration)
                        }
                    }
                    .position(x: geo.size.width * anchor.x, y: geo.size.height * anchor.y)
            }
            .allowsHitTesting(false)
            .accessibilityHidden(true)
        }
    }

    /// 水波：按下时从中心荡开两圈清透的涟漪（一圈亮边、中间透明），被 shape 裁切
    func tesseraFlowRipple<S: Shape>(pressed: Bool, active: Bool, color: Color, in shape: S) -> some View {
        modifier(FlowRipple(pressed: pressed, active: active, color: color, shape: shape))
    }
}

/// 让关键帧动画的 trigger 可以在“有效值”和“占位值”之间切换
struct AnyHashableBox: Equatable {
    let value: AnyHashable
    init<T: Equatable>(_ v: T) { value = AnyHashable(String(describing: v) + "\(T.self)") }
}

private struct SplashProgressKey: EnvironmentKey { static let defaultValue: CGFloat = 1 }
extension EnvironmentValues {
    var splashProgress: CGFloat {
        get { self[SplashProgressKey.self] }
        set { self[SplashProgressKey.self] = newValue }
    }
}

struct SplashRings: View {
    let color: Color
    let diameter: CGFloat
    @Environment(\.splashProgress) private var p

    var body: some View {
        ZStack {
            ForEach(0..<2, id: \.self) { ring in
                let t = min(max((p - CGFloat(ring) * 0.17) / (1 - CGFloat(ring) * 0.17), 0), 1)
                Circle()
                    .strokeBorder(color, lineWidth: ring == 0 ? 1.5 : 1)
                    .frame(width: diameter, height: diameter)
                    .scaleEffect(0.3 + 0.7 * t)
                    .opacity(p >= 1 || t <= 0 ? 0 : 0.9 * (1 - t))
            }
        }
    }
}

struct FlowRipple<S: Shape>: ViewModifier {
    let pressed: Bool
    let active: Bool
    let color: Color
    let shape: S
    @State private var count = 0
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    func body(content: Content) -> some View {
        content
            .overlay {
                if active && !reduceMotion {
                    GeometryReader { geo in
                        let d = 2 * hypot(geo.size.width / 2, geo.size.height / 2)
                        ZStack {
                            ForEach(0..<2, id: \.self) { ring in
                                Circle()
                                    .fill(RadialGradient(stops: [
                                        .init(color: .clear, location: ring == 0 ? 0.42 : 0.5),
                                        .init(color: color, location: ring == 0 ? 0.6 : 0.64),
                                        .init(color: .clear, location: 0.71),
                                    ], center: .center, startRadius: 0, endRadius: d / 2))
                                    .frame(width: d, height: d)
                                    .keyframeAnimator(initialValue: CGFloat(1), trigger: count) { wave, p in
                                        wave.scaleEffect(p).opacity(p >= 1 ? 0 : (p < 0.6 ? 1 : (1 - p) / 0.4))
                                    } keyframes: { _ in
                                        KeyframeTrack {
                                            LinearKeyframe(0, duration: 0.001)
                                            LinearKeyframe(0, duration: ring == 0 ? 0.001 : 0.09)
                                            CubicKeyframe(1, duration: TesseraFlow.rippleDuration)
                                        }
                                    }
                            }
                        }
                        .position(x: geo.size.width / 2, y: geo.size.height / 2)
                    }
                    .clipShape(shape)
                    .allowsHitTesting(false)
                    .accessibilityHidden(true)
                }
            }
            .onChange(of: pressed) { _, now in if now { count += 1 } }
    }
}
