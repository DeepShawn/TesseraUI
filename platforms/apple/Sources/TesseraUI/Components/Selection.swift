import SwiftUI

// MARK: - 复选框

/// 复选框：150ms 填色，勾号从左往右描出来。用法：Toggle("接收通知", isOn: $on).toggleStyle(.tesseraCheckbox)
public struct TesseraCheckboxStyle: ToggleStyle {
    @Environment(\.isEnabled) private var isEnabled

    public func makeBody(configuration: Configuration) -> some View {
        let on = configuration.isOn
        let shape = RoundedRectangle(cornerRadius: 7, style: .continuous)
        Button { configuration.isOn.toggle() } label: {
            HStack(spacing: TesseraSpace.s3) {
                ZStack {
                    if on {
                        Color.clear.tesseraTile(TesseraColor.accentControl, style: .solid, shape: shape)
                    } else {
                        shape.fill(TesseraColor.fillDefault).overlay { shape.strokeBorder(TesseraColor.borderStrong, lineWidth: 1.5) }
                    }
                    CheckMark()
                        .trim(from: 0, to: on ? 1 : 0)
                        .stroke(TesseraColor.textOnSolid, style: StrokeStyle(lineWidth: 2, lineCap: .round, lineJoin: .round))
                        .padding(5)
                }
                .frame(width: 20, height: 20)
                .animation(.easeOut(duration: TesseraMotion.durationFast), value: on)
                configuration.label.tesseraText(.bodySm).foregroundStyle(TesseraColor.textPrimary)
            }
            .frame(minHeight: TesseraSize.touchMin)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .opacity(isEnabled ? 1 : TesseraOpacity.disabled)
        .accessibilityAddTraits(on ? [.isSelected] : [])
        .accessibilityValue(on ? "已勾选" : "未勾选")
    }

    struct CheckMark: Shape {
        func path(in r: CGRect) -> Path {
            var p = Path()
            p.move(to: CGPoint(x: r.minX + r.width * 0.05, y: r.minY + r.height * 0.55))
            p.addLine(to: CGPoint(x: r.minX + r.width * 0.38, y: r.minY + r.height * 0.85))
            p.addLine(to: CGPoint(x: r.maxX - r.width * 0.02, y: r.minY + r.height * 0.18))
            return p
        }
    }
}

public extension ToggleStyle where Self == TesseraCheckboxStyle {
    static var tesseraCheckbox: TesseraCheckboxStyle { TesseraCheckboxStyle() }
}

// MARK: - 单选

/// 单选组：选中时中间的小方砖从 −45° 转正并放大（snappy）
public struct TesseraRadioGroup<Value: Hashable>: View {
    @Binding var selection: Value
    let options: [(value: Value, title: String)]
    let axis: Axis
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    public init(selection: Binding<Value>, options: [(value: Value, title: String)], axis: Axis = .horizontal) {
        self._selection = selection
        self.options = options
        self.axis = axis
    }

    public var body: some View {
        let layout = axis == .horizontal ? AnyLayout(HStackLayout(spacing: TesseraSpace.s5)) : AnyLayout(VStackLayout(alignment: .leading, spacing: 0))
        layout {
            ForEach(options.indices, id: \.self) { i in
                let on = options[i].value == selection
                Button { selection = options[i].value } label: {
                    HStack(spacing: TesseraSpace.s3) {
                        ZStack {
                            if on { Color.clear.tesseraTile(TesseraColor.accentControl, style: .solid, shape: Circle()) }
                            else { Circle().fill(TesseraColor.fillDefault).overlay { Circle().strokeBorder(TesseraColor.borderStrong, lineWidth: 1.5) } }
                            RoundedRectangle(cornerRadius: 3, style: .continuous)
                                .fill(TesseraColor.textOnSolid)
                                .frame(width: 8, height: 8)
                                .scaleEffect(on ? 1 : 0)
                                .rotationEffect(.degrees(on ? 0 : -45))
                        }
                        .frame(width: 20, height: 20)
                        .animation(.tessera(TesseraMotion.snappy, reduceMotion: reduceMotion), value: on)
                        TesseraLabel(options[i].title, style: .bodySm)
                    }
                    .frame(minHeight: TesseraSize.touchMin)
                    .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                .accessibilityAddTraits(on ? [.isSelected] : [])
            }
        }
    }
}

// MARK: - 开关

/// 开关：可点击，也可按住滑块左右拖动（拖出两端有阻力，松手按位置或速度决定）。
/// glass = true 时滑块使用系统毛玻璃材质。用法：Toggle("Wi-Fi", isOn: $on).toggleStyle(.tesseraSwitch)
public struct TesseraSwitchStyle: ToggleStyle {
    var glass = false
    @Environment(\.isEnabled) private var isEnabled

    public func makeBody(configuration: Configuration) -> some View {
        HStack(spacing: TesseraSpace.s3) {
            SwitchTrack(isOn: configuration.$isOn, glass: glass)
            configuration.label.tesseraText(.bodySm).foregroundStyle(TesseraColor.textPrimary)
        }
        .frame(minHeight: TesseraSize.touchMin)
        .opacity(isEnabled ? 1 : TesseraOpacity.disabled)
        .accessibilityElement(children: .combine)
        .accessibilityAddTraits(.isButton)
        .accessibilityValue(configuration.isOn ? "已开启" : "已关闭")
        .accessibilityAction { configuration.isOn.toggle() }
    }

    struct SwitchTrack: View {
        @Binding var isOn: Bool
        let glass: Bool
        @State private var dragPos: CGFloat? = nil
        @GestureState private var pressing = false
        @Environment(\.accessibilityReduceMotion) private var reduceMotion

        private let w: CGFloat = 52, h: CGFloat = 30, pad: CGFloat = 3, thumb: CGFloat = 24, stretchMax: CGFloat = 6

        var body: some View {
            let stretch = (pressing || dragPos != nil) ? stretchMax : 0
            let travel = w - pad * 2 - thumb - stretchMax
            let pos = dragPos ?? (isOn ? 1 : 0)
            let preview = dragPos.map { $0 > 0.5 } ?? isOn
            let track = RoundedRectangle(cornerRadius: 13, style: .continuous)
            let thumbShape = RoundedRectangle(cornerRadius: 10, style: .continuous)
            ZStack(alignment: .leading) {
                Color.clear.tesseraTile(preview ? TesseraColor.accentControl : TesseraColor.fillControlOff, style: .sunken, shape: track)
                // 开 / 关标记：开启时左侧竖线，关闭时右侧小圆圈
                if dragPos == nil {
                    if isOn { Capsule().fill(TesseraColor.textOnSolid.opacity(0.7)).frame(width: 1.5, height: 9).offset(x: 15) }
                    else { Circle().strokeBorder(TesseraColor.textTertiary, lineWidth: 1.2).frame(width: 8, height: 8).offset(x: w - 19) }
                }
                Group {
                    if glass { thumbShape.fill(.ultraThinMaterial).overlay { thumbShape.strokeBorder(TesseraColor.materialGlassRim, lineWidth: 1) } }
                    else { thumbShape.fill(.white).overlay { thumbShape.fill(LinearGradient(colors: [.clear, Color.black.opacity(0.06)], startPoint: .center, endPoint: .bottom)) } }
                }
                .frame(width: thumb + stretch, height: thumb)
                .shadow(color: TesseraColor.shadowColor.opacity(0.22), radius: 1.5, y: 0.5)
                .shadow(color: TesseraColor.shadowColor.opacity(0.12), radius: 4, y: 2)
                .offset(x: pad + pos * (travel + stretchMax - stretch))
            }
            .frame(width: w, height: h)
            .animation(.tessera(TesseraMotion.snappy, reduceMotion: reduceMotion), value: isOn)
            .animation(.tessera(TesseraMotion.snappy, reduceMotion: reduceMotion), value: stretch)
            .animation(.easeOut(duration: TesseraMotion.durationNormal), value: preview)
            .contentShape(Rectangle())
            .onTapGesture { isOn.toggle() }
            .gesture(
                DragGesture(minimumDistance: 3)
                    .updating($pressing) { _, state, _ in state = true }
                    .onChanged { v in
                        let start: CGFloat = isOn ? 1 : 0
                        var p = start + v.translation.width / travel
                        if p < 0 { p *= 0.3 } else if p > 1 { p = 1 + (p - 1) * 0.3 } // 拖出两端有阻力
                        dragPos = p
                    }
                    .onEnded { v in
                        let next = abs(v.velocity.width) > TesseraMotion.flingVelocity ? v.velocity.width > 0 : (dragPos ?? 0) > 0.5
                        withAnimation(.tessera(TesseraMotion.snappy, reduceMotion: reduceMotion)) {
                            dragPos = nil
                            isOn = next
                        }
                    }
            )
        }
    }
}

public extension ToggleStyle where Self == TesseraSwitchStyle {
    static var tesseraSwitch: TesseraSwitchStyle { TesseraSwitchStyle() }
    static func tesseraSwitch(glass: Bool) -> TesseraSwitchStyle { TesseraSwitchStyle(glass: glass) }
}

// MARK: - 分段控制

/// 分段控制：一排砖 + 滑动的墨色选中砖。切换时前沿 snappy、后沿 smooth（移动中自然拉长）；
/// 可以按住左右拖动选择，松手落到最近的一块，甩得够快就朝甩的方向走一块。
public struct TesseraSegmentedControl: View {
    let options: [String]
    @Binding var selection: Int
    let small: Bool
    @State private var left: CGFloat = 0
    @State private var right: CGFloat = 0
    @State private var dragging = false
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    public init(_ options: [String], selection: Binding<Int>, small: Bool = false) {
        self.options = options
        self._selection = selection
        self.small = small
    }

    public var body: some View {
        let height = small ? TesseraSize.sm : TesseraSize.md
        let outer: CGFloat = small ? 11 : TesseraRadius.md
        let gap = TesseraTile.grout
        GeometryReader { geo in
            let n = CGFloat(options.count)
            let item = (geo.size.width - gap * (n - 1)) / n
            let leftOf = { (i: Int) in CGFloat(i) * (item + gap) }
            let current = Int(((left + item / 2) / (item + gap)).rounded(.down)).clamped(0, options.count - 1)
            ZStack(alignment: .topLeading) {
                HStack(spacing: gap) {
                    ForEach(options.indices, id: \.self) { i in
                        Color.clear.tesseraTile(TesseraColor.fillDefault, style: .sunken, shape: tileShape(.of(i, count: options.count), outer: outer))
                    }
                }
                Color.clear
                    .tesseraTile(TesseraColor.textPrimary, style: .solid, shape: tileShape(.of(current, count: options.count), outer: outer))
                    .frame(width: max(0, right - left), height: height)
                    .offset(x: left)
                    .shadow(color: TesseraColor.shadowColor.opacity(0.1), radius: 3, y: 1)
                HStack(spacing: gap) {
                    ForEach(options.indices, id: \.self) { i in
                        Button { selection = i } label: {
                            TesseraLabel(options[i], style: .label, color: i == selection ? TesseraColor.bgSurface : TesseraColor.textSecondary)
                                .lineLimit(1)
                                .frame(maxWidth: .infinity, maxHeight: .infinity)
                                .contentShape(Rectangle())
                        }
                        .buttonStyle(.plain)
                        .accessibilityAddTraits(i == selection ? [.isSelected] : [])
                    }
                }
            }
            .frame(height: height)
            .onAppear { left = leftOf(selection); right = left + item }
            .onChange(of: geo.size.width) { _, _ in left = leftOf(selection); right = left + item }
            .onChange(of: selection) { old, new in
                guard !dragging else { return }
                let l = leftOf(new), goingRight = new > old
                withAnimation(.tessera(goingRight ? TesseraMotion.smooth : TesseraMotion.snappy, reduceMotion: reduceMotion)) { left = l }
                withAnimation(.tessera(goingRight ? TesseraMotion.snappy : TesseraMotion.smooth, reduceMotion: reduceMotion)) { right = l + item }
            }
            .gesture(
                DragGesture(minimumDistance: 4)
                    .onChanged { v in
                        dragging = true
                        // 选中砖的中心追到手指下面
                        let l = (v.location.x - item / 2).clamped(-item * 0.15, leftOf(options.count - 1) + item * 0.15)
                        withAnimation(.tessera(TesseraMotion.snappy, reduceMotion: reduceMotion)) { left = l; right = l + item }
                    }
                    .onEnded { v in
                        var i = Int(((v.location.x) / (item + gap)).rounded(.down)).clamped(0, options.count - 1)
                        if abs(v.velocity.width) > TesseraMotion.flingVelocity && i == selection { i = (i + (v.velocity.width > 0 ? 1 : -1)).clamped(0, options.count - 1) }
                        withAnimation(.tessera(TesseraMotion.snappy, reduceMotion: reduceMotion)) { left = leftOf(i); right = leftOf(i) + item }
                        dragging = false
                        selection = i
                    }
            )
        }
        .frame(height: height)
        .accessibilityElement(children: .contain)
    }
}

extension Comparable {
    func clamped(_ lo: Self, _ hi: Self) -> Self { min(max(self, lo), hi) }
}
