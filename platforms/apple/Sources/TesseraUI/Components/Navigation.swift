import SwiftUI

// MARK: - 顶部导航栏

/// 顶部导航栏：不透明的页面底色 + 底部一条细线；标题是软衬线。
/// 自绘导航栏时，请在 NavigationStack 中隐藏系统导航栏，并保留边缘右滑返回（见 README）。
public struct TesseraNavBar<Leading: View, Trailing: View>: View {
    let title: String
    let large: Bool
    let leading: Leading
    let trailing: Trailing

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
        .overlay(alignment: .bottom) { Rectangle().fill(TesseraColor.borderSubtle).frame(height: 1) }
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

    public init(_ titles: [String], selection: Binding<Int>) {
        self.titles = titles
        self._selection = selection
    }

    public var body: some View {
        HStack(spacing: TesseraSpace.s6) {
            ForEach(titles.indices, id: \.self) { i in
                Button { selection = i } label: {
                    TesseraLabel(titles[i], style: .label, color: i == selection ? TesseraColor.textPrimary : TesseraColor.textSecondary)
                        .padding(.top, 12).padding(.bottom, 14)
                        .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                .accessibilityAddTraits(i == selection ? [.isSelected] : [])
                .background { GeometryReader { g in Color.clear.preference(key: TabFrameKey.self, value: [i: g.frame(in: .named("tabs"))]) } }
            }
            Spacer(minLength: 0)
        }
        .coordinateSpace(name: "tabs")
        .overlay(alignment: .bottomLeading) {
            UnevenRoundedRectangle(topLeadingRadius: 2, topTrailingRadius: 2, style: .continuous)
                .fill(TesseraColor.accentSolid)
                .frame(width: max(0, right - left), height: 3)
                .offset(x: left)
        }
        .overlay(alignment: .bottom) { Rectangle().fill(TesseraColor.borderSubtle).frame(height: 1) }
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

    public init(_ items: [TesseraTabBarItem], selection: Binding<Int>) {
        self.items = items
        self._selection = selection
    }

    public var body: some View {
        HStack(spacing: TesseraTile.grout) {
            ForEach(items.indices, id: \.self) { i in
                let item = items[i], on = i == selection
                Button { selection = i } label: {
                    VStack(spacing: 3) {
                        TesseraIcon(on ? item.selectedIcon : item.icon, size: 22)
                            .overlay(alignment: .topTrailing) { if let b = item.badge { TesseraBadge(count: b).offset(x: 12, y: -6) } }
                        Text(item.title).font(.system(size: 10, weight: .semibold))
                    }
                    .foregroundStyle(on ? TesseraColor.accentText : TesseraColor.textSecondary)
                    .frame(maxWidth: .infinity, minHeight: 52)
                    .background(on ? TesseraColor.accentSubtle : .clear, in: RoundedRectangle(cornerRadius: TesseraRadius.md, style: .continuous))
                    .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                .accessibilityLabel(item.title)
                .accessibilityAddTraits(on ? [.isSelected] : [])
                .animation(.easeOut(duration: TesseraMotion.durationNormal), value: on)
            }
        }
        .padding(.horizontal, TesseraSpace.s3)
        .padding(.top, TesseraSpace.s2)
        .background(TesseraColor.bgCanvas.ignoresSafeArea(edges: .bottom))
        .overlay(alignment: .top) { Rectangle().fill(TesseraColor.borderSubtle).frame(height: 1) }
    }
}

// MARK: - 菜单

/// 菜单按钮：触发按钮用 TesseraUI 样式，弹出的菜单使用系统菜单（保证读屏、键盘与指针交互）
public struct TesseraMenu<Content: View>: View {
    let icon: TesseraIconName
    let label: String
    let content: Content

    public init(icon: TesseraIconName = .more, label: String = "更多", @ViewBuilder content: () -> Content) {
        self.icon = icon
        self.label = label
        self.content = content()
    }

    public var body: some View {
        Menu { content } label: {
            TesseraIcon(icon)
                .foregroundStyle(TesseraColor.textPrimary)
                .frame(width: TesseraSize.md, height: TesseraSize.md)
                .tesseraTile(TesseraColor.fillDefault, style: .soft, shape: RoundedRectangle(cornerRadius: TesseraRadius.md, style: .continuous))
                .frame(width: TesseraSize.touchMin, height: TesseraSize.touchMin)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(label)
    }
}
