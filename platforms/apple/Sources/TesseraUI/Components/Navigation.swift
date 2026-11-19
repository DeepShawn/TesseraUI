import SwiftUI

// MARK: - 顶部导航栏

/// 顶部导航栏：不透明的页面底色 + 底部一条细线；标题是软衬线。
/// 自绘导航栏时，请在 NavigationStack 中隐藏系统导航栏，并保留边缘右滑返回（见 README）。
public struct TesseraNavBar<Leading: View, Trailing: View>: View {
    let title: String
    let large: Bool
    let leading: Leading
    let trailing: Trailing
    @Environment(\.tesseraShape) private var tesseraShape

    public init(_ title: String, large: Bool = false, @ViewBuilder leading: () -> Leading, @ViewBuilder trailing: () -> Trailing) {
        self.title = title
        self.large = large
        self.leading = leading()
        self.trailing = trailing()
    }

    public var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            ZStack {
                if !large { TesseraLabel(title, style: .title3).lineLimit(1).accessibilityAddTraits(.isHeader) }
                HStack {
                    leading
                    Spacer(minLength: 0)
                    trailing
                }
            }
            .frame(height: 56)
            .padding(.horizontal, TesseraSpace.s1)
            if large {
                TesseraLabel(title, style: .title1).accessibilityAddTraits(.isHeader)
                    .padding(.horizontal, TesseraSpace.s5).padding(.bottom, TesseraSpace.s3)
            }
        }
        .background(TesseraColor.bgCanvas)
        // 圆融外观没有底部分割线
        .overlay(alignment: .bottom) { if !tesseraShape.isRound { Rectangle().fill(TesseraColor.borderSubtle).frame(height: 1) } }
    }
}

public extension TesseraNavBar where Leading == EmptyView, Trailing == EmptyView {
    init(_ title: String, large: Bool = false) { self.init(title, large: large, leading: { EmptyView() }, trailing: { EmptyView() }) }
}

// MARK: - 标签页

private struct TabFrameKey: PreferenceKey {
    static let defaultValue: [Int: CGRect] = [:]
    static func reduce(value: inout [Int: CGRect], nextValue: () -> [Int: CGRect]) { value.merge(nextValue()) { $1 } }
}

/// 标签页：指示条是一条细砖；切换时前沿 snappy、后沿 smooth
public struct TesseraTabs: View {
    let titles: [String]
    @Binding var selection: Int
    @State private var frames: [Int: CGRect] = [:]
    @State private var left: CGFloat = 0
    @State private var right: CGFloat = 0
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @Environment(\.tesseraShape) private var tesseraShape

    public init(_ titles: [String], selection: Binding<Int>) {
        self.titles = titles
        self._selection = selection
    }

    public var body: some View {
        HStack(spacing: TesseraSpace.s6) {
            ForEach(titles.indices, id: \.self) { i in
                Button { selection = i } label: {
                    // 指示条按文字宽度量；可点按范围至少 44 宽（两个字的标签只有 28 宽）
                    TesseraLabel(titles[i], style: .label, color: i == selection ? TesseraColor.textPrimary : TesseraColor.textSecondary)
                        .background { GeometryReader { g in Color.clear.preference(key: TabFrameKey.self, value: [i: g.frame(in: .named("tabs"))]) } }
                        .padding(.top, 12).padding(.bottom, 14)
                        .frame(minWidth: TesseraSize.touchMin)
                        .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                .accessibilityAddTraits(i == selection ? [.isSelected] : [])
            }
            Spacer(minLength: 0)
        }
        .coordinateSpace(name: "tabs")
        .overlay(alignment: .bottomLeading) {
            // 圆融：没有底线，指示器是一颗圆头短条
            UnevenRoundedRectangle(topLeadingRadius: 2, bottomLeadingRadius: tesseraShape.isRound ? 2 : 0,
                                   bottomTrailingRadius: tesseraShape.isRound ? 2 : 0, topTrailingRadius: 2, style: .continuous)
                .fill(TesseraColor.accentSolid)
                .frame(width: max(0, right - left), height: tesseraShape.isRound ? 4 : 3)
                .offset(x: left)
        }
        .overlay(alignment: .bottom) { if !tesseraShape.isRound { Rectangle().fill(TesseraColor.borderSubtle).frame(height: 1) } }
        .onPreferenceChange(TabFrameKey.self) { f in
            frames = f
            if right == 0, let r = f[selection] { left = r.minX; right = r.maxX }
        }
        .onChange(of: selection) { old, new in
            guard let r = frames[new] else { return }
            let goingRight = new > old
            withAnimation(.tessera(goingRight ? TesseraMotion.smooth : TesseraMotion.snappy, reduceMotion: reduceMotion)) { left = r.minX }
            withAnimation(.tessera(goingRight ? TesseraMotion.snappy : TesseraMotion.smooth, reduceMotion: reduceMotion)) { right = r.maxX }
        }
    }
}

// MARK: - 底部标签栏

public struct TesseraTabBarItem: Identifiable, Sendable {
    public let id = UUID()
    public let title: String
    public let icon: TesseraIconName
    public let selectedIcon: TesseraIconName
    public let badge: Int?

    public init(_ title: String, icon: TesseraIconName, selectedIcon: TesseraIconName? = nil, badge: Int? = nil) {
        self.title = title
        self.icon = icon
        self.selectedIcon = selectedIcon ?? icon
        self.badge = badge
    }
}

/// 底部标签栏：一排砖，选中的那块上釉色，图标从线性换成实心
public struct TesseraTabBar: View {
    let items: [TesseraTabBarItem]
    @Binding var selection: Int
    @Environment(\.tesseraShape) private var tesseraShape

    public init(_ items: [TesseraTabBarItem], selection: Binding<Int>) {
        self.items = items
        self._selection = selection
    }

    public var body: some View {
        let round = tesseraShape.isRound
        let pill = RoundedRectangle(cornerRadius: tesseraShape.roundRadius(TesseraRound.pill), style: .continuous)
        HStack(spacing: tesseraShape.groupGap) {
            ForEach(items.indices, id: \.self) { i in
                let item = items[i], on = i == selection
                Button { selection = i } label: {
                    VStack(spacing: 3) {
                        TesseraIcon(on ? item.selectedIcon : item.icon, size: 22)
                            .tesseraBeadPop(trigger: on, active: tesseraShape == .flow && on)
                            .overlay(alignment: .topTrailing) { if let b = item.badge { TesseraBadge(count: b).offset(x: 12, y: -6) } }
                        Text(item.title).font(.system(size: 10, weight: .semibold))
                    }
                    .foregroundStyle(on ? TesseraColor.accentText : TesseraColor.textSecondary)
                    .frame(maxWidth: .infinity, minHeight: round ? 50 : 52)
                    .background(on ? TesseraColor.accentSubtle : .clear, in: RoundedRectangle(cornerRadius: tesseraShape.radius(tile: TesseraRadius.md), style: .continuous))
                    .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                .accessibilityLabel(item.title)
                .accessibilityAddTraits(on ? [.isSelected] : [])
                .animation(.easeOut(duration: TesseraMotion.durationNormal), value: on)
            }
        }
        // 拼贴：一排砖，顶部一条细线；圆融：浮起的一颗胶囊，没有线
        .padding(.horizontal, round ? 6 : TesseraSpace.s3)
        .padding(.vertical, round ? 6 : 0)
        .padding(.top, round ? 0 : TesseraSpace.s2)
        .background { if round { pill.fill(TesseraColor.bgElevated).tesseraShadow(.level2) } }
        .padding(.horizontal, round ? TesseraSpace.s3 : 0)
        .padding(.bottom, round ? TesseraSpace.s3 : 0)
        .background(TesseraColor.bgCanvas.ignoresSafeArea(edges: .bottom))
        .overlay(alignment: .top) { if !round { Rectangle().fill(TesseraColor.borderSubtle).frame(height: 1) } }
        .tesseraHaptic(.selection, trigger: selection)
    }
}

// MARK: - 菜单

/// 菜单按钮：触发按钮与弹出的面板都是 TesseraUI 样式（自绘弹层）。
/// 用系统 popover 承载（读屏、键盘、点外面关闭都由系统处理），iPhone 上也保持为弹层而不是整屏 sheet。
/// content 里放 Button；点任意一项后菜单自动关闭，`role: .destructive` 的项显示为红色。
/// 之前用系统 Menu 时，iOS 读屏读不出触发按钮的名称（v0.4 的已知问题），现在触发按钮是普通 Button，名称正常。
public struct TesseraMenu<Content: View>: View {
    let icon: TesseraIconName
    let label: String
    let content: Content
    @State private var open = false
    @Environment(\.tesseraShape) private var tesseraShape

    public init(icon: TesseraIconName = .more, label: String = "更多", @ViewBuilder content: () -> Content) {
        self.icon = icon
        self.label = label
        self.content = content()
    }

    public var body: some View {
        Button { open = true } label: {
            TesseraIcon(icon)
                .foregroundStyle(TesseraColor.textPrimary)
                .frame(width: TesseraSize.md, height: TesseraSize.md)
                .tesseraTile(TesseraColor.fillDefault, style: .soft, shape: RoundedRectangle(cornerRadius: tesseraShape.radius(tile: TesseraRadius.md), style: .continuous))
                .frame(width: TesseraSize.touchMin, height: TesseraSize.touchMin)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(label)
        .accessibilityHint("打开菜单")
        .tesseraPopover(isPresented: $open) {
            TesseraMenuPanel { content }
        }
    }
}

/// 菜单面板：一组菜单项（Button），用于 TesseraMenu、TesseraSplitButton 或自定义弹层
public struct TesseraMenuPanel<Content: View>: View {
    let content: Content
    public init(@ViewBuilder content: () -> Content) { self.content = content() }
    public var body: some View {
        VStack(alignment: .leading, spacing: 2) { content }
            .buttonStyle(TesseraMenuItemStyle())
            .padding(TesseraSpace.s1)
            .frame(minWidth: 200)
    }
}

/// 菜单项：点击执行操作并关闭所在的弹层；危险操作（role: .destructive）为红色
public struct TesseraMenuItemStyle: PrimitiveButtonStyle {
    @Environment(\.tesseraDismissPopover) private var dismiss
    @Environment(\.tesseraShape) private var tesseraShape
    public init() {}
    public func makeBody(configuration: Configuration) -> some View {
        Button {
            configuration.trigger()
            dismiss?()
        } label: {
            configuration.label
                .tesseraText(.bodySm)
                .foregroundStyle(configuration.role == .destructive ? TesseraColor.dangerText : TesseraColor.textPrimary)
                .frame(maxWidth: .infinity, minHeight: TesseraSize.touchMin, alignment: .leading)
                .padding(.horizontal, TesseraSpace.s3)
                .contentShape(Rectangle())
        }
        .buttonStyle(MenuRowPress(radius: tesseraShape.radius(tile: TesseraRadius.sm, round: TesseraRound.row)))
    }
}

private struct MenuRowPress: ButtonStyle {
    let radius: CGFloat
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .background { RoundedRectangle(cornerRadius: radius, style: .continuous).fill(configuration.isPressed ? TesseraColor.statePressed : .clear) }
    }
}

private struct DismissPopoverKey: EnvironmentKey { static let defaultValue: (() -> Void)? = nil }
public extension EnvironmentValues {
    /// 关闭当前 TesseraUI 弹层（菜单项点击后调用）
    var tesseraDismissPopover: (() -> Void)? {
        get { self[DismissPopoverKey.self] }
        set { self[DismissPopoverKey.self] = newValue }
    }
}

public extension View {
    /// TesseraUI 弹层：圆角面板、浮起投影；iPhone 上也保持为弹层（不变成整屏 sheet）
    func tesseraPopover<Content: View>(isPresented: Binding<Bool>, arrowEdge: Edge = .bottom, @ViewBuilder content: @escaping () -> Content) -> some View {
        modifier(PopoverModifier(isPresented: isPresented, arrowEdge: arrowEdge, panel: content))
    }
}

struct PopoverModifier<Panel: View>: ViewModifier {
    @Binding var isPresented: Bool
    let arrowEdge: Edge
    let panel: () -> Panel
    @Environment(\.tesseraShape) private var tesseraShape

    func body(content: Content) -> some View {
        content.popover(isPresented: $isPresented, arrowEdge: arrowEdge) {
            panel()
                .environment(\.tesseraShape, tesseraShape)
                .environment(\.tesseraDismissPopover, { isPresented = false })
                .presentationCompactAdaptation(.popover)
                .presentationBackground(TesseraColor.bgElevated)
        }
    }
}
