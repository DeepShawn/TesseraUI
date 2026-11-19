import SwiftUI

// MARK: - 按钮组

/// 按钮组：一组拼贴砖，选中的一块是深色实砖；圆融 / 流韵合成一整块胶囊。
/// multiple = false 时单选，true 时可多选；selection 是选中项的下标集合。
public struct TesseraButtonGroup: View {
    let options: [String]
    @Binding var selection: Set<Int>
    let multiple: Bool
    let icons: [TesseraIconName?]?
    let size: TesseraButtonStyle.Size
    @Environment(\.tesseraShape) private var tesseraShape

    public init(_ options: [String], selection: Binding<Set<Int>>, multiple: Bool = false, icons: [TesseraIconName?]? = nil, size: TesseraButtonStyle.Size = .md) {
        self.options = options
        self._selection = selection
        self.multiple = multiple
        self.icons = icons
        self.size = size
    }

    public var body: some View {
        HStack(spacing: tesseraShape.groupGap) {
            ForEach(options.indices, id: \.self) { i in
                let on = selection.contains(i)
                Button {
                    if multiple { if on { selection.remove(i) } else { selection.insert(i) } } else { selection = [i] }
                } label: {
                    HStack(spacing: TesseraSpace.s2) {
                        if let icon = icons?[safe: i] ?? nil { TesseraIcon(icon, size: 18) }
                        Text(options[i])
                    }
                }
                .buttonStyle(GroupTileStyle(on: on, size: size, position: .of(i, count: options.count)))
                .accessibilityAddTraits(on ? .isSelected : [])
                .accessibilityValue(multiple ? (on ? "已选中" : "未选中") : "")
            }
        }
        .tesseraHaptic(.selection, trigger: selection)
    }
}

private struct GroupTileStyle: ButtonStyle {
    let on: Bool
    let size: TesseraButtonStyle.Size
    let position: TilePosition
    @Environment(\.tesseraShape) private var tesseraShape
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    func makeBody(configuration: Configuration) -> some View {
        let shape = tesseraShape.groupShape(position, outer: size.radius)
        let fg = on ? TesseraColor.bgSurface : TesseraColor.textPrimary
        return configuration.label
            .tesseraText(.label)
            .lineLimit(1)
            .foregroundStyle(fg)
            .padding(.horizontal, size.padding)
            .frame(minHeight: size.height)
            .tesseraTile(on ? TesseraColor.textPrimary : TesseraColor.fillDefault, style: on ? .solid : .soft, shape: shape, pressed: configuration.isPressed)
            .overlay { if configuration.isPressed { shape.fill(TesseraColor.statePressed) } }
            .frame(minHeight: TesseraSize.touchMin)
            // 命中与读屏范围要包括撑到 44 的外框（contentShape 放在外框之后）
            .contentShape(Rectangle())
            .tesseraPress(configuration.isPressed, shape: tesseraShape, reduceMotion: reduceMotion)
            .animation(.tessera(TesseraMotion.snappy, reduceMotion: reduceMotion), value: configuration.isPressed)
    }
}

extension Array {
    subscript(safe i: Int) -> Element? { indices.contains(i) ? self[i] : nil }
}

// MARK: - 分割按钮

/// 分割按钮：左边执行默认操作，右边打开更多选项（TesseraUI 弹层，菜单项是 Button）
public struct TesseraSplitButton<MenuContent: View>: View {
    let title: String
    let icon: TesseraIconName?
    let variant: TesseraButtonStyle.Variant
    let menuLabel: String
    let action: () -> Void
    let menu: MenuContent
    @State private var open = false
    @Environment(\.tesseraShape) private var tesseraShape

    public init(_ title: String, icon: TesseraIconName? = nil, variant: TesseraButtonStyle.Variant = .primary, menuLabel: String = "更多选项",
                action: @escaping () -> Void, @ViewBuilder menu: () -> MenuContent) {
        self.title = title
        self.icon = icon
        self.variant = variant
        self.menuLabel = menuLabel
        self.action = action
        self.menu = menu()
    }

    public var body: some View {
        HStack(spacing: tesseraShape.groupGap) {
            Button(action: action) {
                HStack(spacing: TesseraSpace.s2) {
                    if let icon { TesseraIcon(icon, size: 18) }
                    Text(title)
                }
            }
            .buttonStyle(.tessera(variant, position: .first))
            Button { open = true } label: {
                TesseraIcon(.chevronDown, size: 18)
                    .rotationEffect(.degrees(open ? 180 : 0))
                    .animation(TesseraMotion.snappy, value: open)
                    .frame(width: TesseraSize.md - TesseraSpace.s4 * 2)
            }
            .buttonStyle(.tessera(variant, position: .last))
            .accessibilityLabel(menuLabel)
            .tesseraPopover(isPresented: $open) { TesseraMenuPanel { menu } }
        }
    }
}

// MARK: - 悬浮按钮

/// 悬浮按钮：一块 56 高的大釉面砖，带投影；text 不为 nil 时是扩展形态，expanded = false 时收起文字
public struct TesseraFab: View {
    let icon: TesseraIconName
    let label: String
    let text: String?
    let expanded: Bool
    let action: () -> Void

    public init(_ icon: TesseraIconName, label: String, text: String? = nil, expanded: Bool = true, action: @escaping () -> Void) {
        self.icon = icon
        self.label = label
        self.text = text
        self.expanded = expanded
        self.action = action
    }

    public var body: some View {
        let showText = text != nil && expanded
        Button(action: action) {
            HStack(spacing: TesseraSpace.s2) {
                TesseraIcon(icon, size: 24)
                if showText, let text { Text(text).transition(.opacity.combined(with: .scale(scale: 0.9, anchor: .leading))) }
            }
        }
        .buttonStyle(FabStyle(showText: showText))
        .accessibilityLabel(showText ? (text ?? label) : label)
        .animation(TesseraMotion.smooth, value: showText)
    }
}

private struct FabStyle: ButtonStyle {
    let showText: Bool
    @Environment(\.tesseraShape) private var tesseraShape
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    func makeBody(configuration: Configuration) -> some View {
        let pressed = configuration.isPressed
        // 拼贴 20 圆角的方砖；圆融是胶囊；流韵静止 20、按下变胶囊
        let radius: CGFloat = tesseraShape == .round || (tesseraShape == .flow && pressed) ? 28 : 20
        let shape = RoundedRectangle(cornerRadius: radius, style: .continuous)
        return configuration.label
            .tesseraText(TesseraTextStyle(size: 15, lineHeight: 20, weight: .semibold, family: .text, tracking: 0, relativeTo: .body))
            .foregroundStyle(TesseraColor.textOnSolid)
            .padding(.horizontal, showText ? 20 : 16)
            .frame(minWidth: 56, minHeight: 56)
            .tesseraTile(TesseraColor.accentSolid, style: .solid, shape: shape, pressed: pressed)
            .overlay { if pressed { shape.fill(TesseraColor.stateSolidPressed) } }
            .shadow(color: TesseraColor.shadowColor.opacity(0.22), radius: pressed ? 4 : 10, y: pressed ? 2 : 6)
            .tesseraPress(pressed, shape: tesseraShape, reduceMotion: reduceMotion)
            .animation(pressed ? .easeOut(duration: TesseraMotion.pressDuration) : .tessera(TesseraMotion.bouncy, reduceMotion: reduceMotion), value: pressed)
            .tesseraHaptic(.light, trigger: pressed) { _, now in now }
    }
}

// MARK: - 链接

/// 链接：釉色文字 + 40% 透明度的下划线，按下时下划线变实；external = true 时末尾加外链箭头，读屏会说“在新窗口打开”
public struct TesseraLink: View {
    let title: String
    let external: Bool
    let action: () -> Void

    public init(_ title: String, external: Bool = false, action: @escaping () -> Void) {
        self.title = title
        self.external = external
        self.action = action
    }

    public var body: some View {
        Button(action: action) {
            HStack(spacing: 2) {
                Text(title)
                if external { TesseraIcon(.arrowUpRight, size: 14) }
            }
        }
        .buttonStyle(LinkStyle())
        .accessibilityLabel(external ? "\(title)，在新窗口打开" : title)
        .accessibilityAddTraits(.isLink)
    }
}

private struct LinkStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .tesseraText(.bodySm)
            .foregroundStyle(TesseraColor.accentText)
            .padding(.bottom, 2)
            .overlay(alignment: .bottom) {
                Rectangle().fill(TesseraColor.accentText.opacity(configuration.isPressed ? 1 : 0.4)).frame(height: 1)
            }
            .frame(minHeight: TesseraSize.touchMin)
            .contentShape(Rectangle())
    }
}

// MARK: - 自动换行布局（标签输入等）

/// 从左到右排列，放不下时换行
public struct TesseraFlowLayout: Layout {
    let spacing: CGFloat
    let lineSpacing: CGFloat
    public init(spacing: CGFloat = 6, lineSpacing: CGFloat = 6) { self.spacing = spacing; self.lineSpacing = lineSpacing }

    public func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let width = proposal.width ?? .infinity
        var x: CGFloat = 0, y: CGFloat = 0, lineH: CGFloat = 0, maxX: CGFloat = 0
        for v in subviews {
            let s = v.sizeThatFits(.unspecified)
            if x > 0 && x + s.width > width { x = 0; y += lineH + lineSpacing; lineH = 0 }
            x += s.width + spacing
            maxX = max(maxX, x - spacing)
            lineH = max(lineH, s.height)
        }
        return CGSize(width: proposal.width ?? maxX, height: y + lineH)
    }

    public func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        var x = bounds.minX, y = bounds.minY, lineH: CGFloat = 0
        for v in subviews {
            let s = v.sizeThatFits(.unspecified)
            if x > bounds.minX && x + s.width > bounds.maxX { x = bounds.minX; y += lineH + lineSpacing; lineH = 0 }
            v.place(at: CGPoint(x: x, y: y + (s.height < lineH ? 0 : 0)), proposal: ProposedViewSize(s))
            x += s.width + spacing
            lineH = max(lineH, s.height)
        }
    }
}
