import SwiftUI

// MARK: - 静态渲染（截图）

private struct StaticRenderKey: EnvironmentKey { static let defaultValue = false }

extension EnvironmentValues {
    /// 截图测试用 ImageRenderer 画不出系统滚动视图：为 true 时，表格、轮播、代码块不用 ScrollView，直接裁切显示
    var tesseraStaticRender: Bool {
        get { self[StaticRenderKey.self] }
        set { self[StaticRenderKey.self] = newValue }
    }
}

// MARK: - 表格 Table

public enum TesseraSortDirection: Sendable { case none, ascending, descending }

/// 表格列。text 用于显示、筛选、排序和读屏；cell 可自定义单元格内容
public struct TesseraColumn<Row> {
    public let title: String
    public let width: CGFloat
    public let sortable: Bool
    public let numeric: Bool
    public let text: (Row) -> String
    public let cell: ((Row) -> AnyView)?

    public init(_ title: String, width: CGFloat = 120, sortable: Bool = false, numeric: Bool = false,
                text: @escaping (Row) -> String, cell: ((Row) -> AnyView)? = nil) {
        self.title = title
        self.width = width
        self.sortable = sortable
        self.numeric = numeric
        self.text = text
        self.cell = cell
    }
}

private func tesseraNumber(_ s: String) -> Double? {
    let cleaned = s.filter { !",，¥$% ".contains($0) }
    return Double(cleaned)
}

/// 按表格的规则排序：数字列按数值，其余按中文拼音
public func tesseraSort<Row>(_ rows: [Row], by column: TesseraColumn<Row>, _ direction: TesseraSortDirection) -> [Row] {
    if direction == .none { return rows }
    let numeric = column.numeric || rows.allSatisfy { tesseraNumber(column.text($0)) != nil }
    let zh = Locale(identifier: "zh_Hans_CN")
    let ascending = rows.sorted { a, b in
        let x = column.text(a), y = column.text(b)
        if numeric { return (tesseraNumber(x) ?? 0) < (tesseraNumber(y) ?? 0) }
        return x.compare(y, locale: zh) == .orderedAscending
    }
    return direction == .ascending ? ascending : ascending.reversed()
}

/// 表格。
/// - 点表头排序：升序 → 降序 → 原顺序；数字按数值、中文按拼音
/// - 多选：selection 不为 nil 时出现选择列和全选（含半选）
/// - 固定首列：横向滚动后首列右侧出现分隔阴影
/// - layout：.automatic 时 iPhone 竖屏（紧凑宽度）每行变成一张卡片
public struct TesseraTable<Row, ID: Hashable>: View {
    public enum Layout: Sendable { case automatic, grid, cards }
    let rows: [Row]
    let columns: [TesseraColumn<Row>]
    let id: (Row) -> ID
    let filter: String
    let selection: Binding<Set<ID>>?
    let stickyFirst: Bool
    let pageSize: Int
    let loading: Bool
    let layout: Layout
    let emptyText: String
    @State private var sortColumn = -1
    @State private var sortDirection: TesseraSortDirection = .none
    @State private var page = 1
    @Environment(\.horizontalSizeClass) private var sizeClass

    public init(_ rows: [Row], columns: [TesseraColumn<Row>], id: @escaping (Row) -> ID, filter: String = "",
                selection: Binding<Set<ID>>? = nil, stickyFirst: Bool = true, pageSize: Int = 0, loading: Bool = false,
                layout: Layout = .automatic, emptyText: String = "没有符合条件的数据") {
        self.rows = rows
        self.columns = columns
        self.id = id
        self.filter = filter
        self.selection = selection
        self.stickyFirst = stickyFirst
        self.pageSize = pageSize
        self.loading = loading
        self.layout = layout
        self.emptyText = emptyText
    }

    private var filtered: [Row] {
        let q = filter.trimmingCharacters(in: .whitespaces).lowercased()
        if q.isEmpty { return rows }
        return rows.filter { r in columns.contains { $0.text(r).lowercased().contains(q) } }
    }

    private var sorted: [Row] {
        sortColumn < 0 ? filtered : tesseraSort(filtered, by: columns[sortColumn], sortDirection)
    }

    private var pages: Int { pageSize > 0 ? max(1, (sorted.count + pageSize - 1) / pageSize) : 1 }
    private var currentPage: Int { min(max(page, 1), pages) }
    private var shown: [Row] {
        guard pageSize > 0 else { return sorted }
        return Array(sorted.dropFirst((currentPage - 1) * pageSize).prefix(pageSize))
    }

    private var useCards: Bool {
        switch layout {
        case .cards: true
        case .grid: false
        case .automatic: sizeClass == .compact
        }
    }

    private func toggleSort(_ i: Int) {
        if sortColumn != i { sortDirection = .ascending } else {
            switch sortDirection {
            case .ascending: sortDirection = .descending
            case .descending: sortDirection = .none
            case .none: sortDirection = .ascending
            }
        }
        sortColumn = sortDirection == .none ? -1 : i
        page = 1
    }

    public var body: some View {
        VStack(spacing: TesseraSpace.s3) {
            if useCards {
                TableCards(rows: shown, columns: columns, id: id, selection: selection, loading: loading, placeholders: pageSize > 0 ? pageSize : 3, emptyText: emptyText)
            } else {
                TableGrid(rows: shown, allRows: sorted, columns: columns, id: id, selection: selection, stickyFirst: stickyFirst,
                          sortColumn: sortColumn, sortDirection: sortDirection, onSort: toggleSort,
                          loading: loading, placeholders: pageSize > 0 ? pageSize : 5, emptyText: emptyText)
            }
            if pageSize > 0 && !loading {
                TableFooter(total: sorted.count, page: currentPage, pages: pages, pageSize: pageSize, shownCount: shown.count) { page = $0 }
            }
        }
        .onChange(of: filter) { page = 1 }
    }
}

private let tableHead: CGFloat = 44
private let tableRow: CGFloat = 48
private let selectWidth: CGFloat = 48

/// 一行在左（leading）或右（trailing）半边的形状：首行、末行外角大，其余是砖缝小角
private func tableRowShape(_ i: Int, _ count: Int, leading: Bool, trailing: Bool, shape: TesseraShape) -> UnevenRoundedRectangle {
    let outer = shape.isRound ? shape.roundRadius(TesseraRound.row) : TesseraRadius.lg
    let inner: CGFloat = shape.isRound ? 0 : TesseraRadius.grout
    let top = i == 0 ? outer : inner
    let bottom = i == count - 1 ? outer : inner
    return UnevenRoundedRectangle(cornerRadii: .init(
        topLeading: leading ? top : 0, bottomLeading: leading ? bottom : 0,
        bottomTrailing: trailing ? bottom : 0, topTrailing: trailing ? top : 0), style: .continuous)
}

private func tableRowFill(_ i: Int, selected: Bool, round: Bool) -> Color {
    if selected { return TesseraColor.accentSolid.opacity(round ? 0.12 : 0.10) }
    if round && i % 2 == 1 { return TesseraColor.textPrimary.opacity(0.025) }
    return .clear
}

private struct ScrollOffsetKey: PreferenceKey {
    static let defaultValue: CGFloat = 0
    static func reduce(value: inout CGFloat, nextValue: () -> CGFloat) { value = nextValue() }
}

private struct TableGrid<Row, ID: Hashable>: View {
    let rows: [Row]
    let allRows: [Row]
    let columns: [TesseraColumn<Row>]
    let id: (Row) -> ID
    let selection: Binding<Set<ID>>?
    let stickyFirst: Bool
    let sortColumn: Int
    let sortDirection: TesseraSortDirection
    let onSort: (Int) -> Void
    let loading: Bool
    let placeholders: Int
    let emptyText: String
    @State private var offset: CGFloat = 0
    @Environment(\.tesseraShape) private var tesseraShape
    @Environment(\.tesseraStaticRender) private var staticRender

    private var fixedCount: Int { stickyFirst ? 1 : 0 }
    private var count: Int { loading ? placeholders : rows.count }
    private var gap: CGFloat { tesseraShape.isRound ? 0 : TesseraTile.grout }

    var body: some View {
        if !loading && rows.isEmpty {
            VStack(spacing: gap) {
                header(range: 0..<columns.count, withSelect: selection != nil)
                TesseraLabel(emptyText, style: .bodySm, color: TesseraColor.textTertiary)
                    .frame(maxWidth: .infinity).padding(TesseraSpace.s10)
                    .background(TesseraColor.bgSurface, in: tableRowShape(0, 1, leading: true, trailing: true, shape: tesseraShape))
            }
        } else {
            HStack(alignment: .top, spacing: 0) {
                if selection != nil || fixedCount > 0 {
                    fixedPart
                        .shadow(color: .black.opacity(offset < -1 ? 0.14 : 0), radius: 5, x: 4)
                        .zIndex(1)
                }
                scrollPart
            }
            .coordinateSpace(name: "tessera-table")
        }
    }

    private var fixedPart: some View {
        VStack(spacing: gap) {
            HStack(spacing: 0) {
                if selection != nil { selectAll.frame(width: selectWidth, height: tableHead) }
                if fixedCount > 0 { headCell(0) }
            }
            ForEach(0..<count, id: \.self) { r in
                let row = loading ? nil : rows[r]
                let sel = row.map { selection?.wrappedValue.contains(id($0)) ?? false } ?? false
                HStack(spacing: 0) {
                    if let selection {
                        Group {
                            if let row { rowCheckbox(row, selection: selection) }
                        }
                        .frame(width: selectWidth, height: tableRow)
                    }
                    if fixedCount > 0 { bodyCell(0, row) }
                }
                .background(TesseraColor.bgSurface)
                .background(alignment: .leading) {
                    if sel && !tesseraShape.isRound { TesseraColor.accentSolid.frame(width: 3) }
                }
                .overlay(tableRowFill(r, selected: sel, round: tesseraShape.isRound).allowsHitTesting(false))
                .clipShape(tableRowShape(r, count, leading: true, trailing: false, shape: tesseraShape))
            }
        }
    }

    @ViewBuilder private var scrollPart: some View {
        let content = VStack(alignment: .leading, spacing: gap) {
            header(range: fixedCount..<columns.count, withSelect: false)
            ForEach(0..<count, id: \.self) { r in
                let row = loading ? nil : rows[r]
                let sel = row.map { selection?.wrappedValue.contains(id($0)) ?? false } ?? false
                HStack(spacing: 0) {
                    ForEach(fixedCount..<columns.count, id: \.self) { i in bodyCell(i, row) }
                    Spacer(minLength: 0)
                }
                .background(TesseraColor.bgSurface)
                .overlay(tableRowFill(r, selected: sel, round: tesseraShape.isRound).allowsHitTesting(false))
                .clipShape(tableRowShape(r, count, leading: selection == nil && fixedCount == 0, trailing: true, shape: tesseraShape))
            }
        }
        if staticRender {
            content.frame(maxWidth: .infinity, alignment: .leading).clipped()
        } else {
            ScrollView(.horizontal, showsIndicators: false) {
                content
                    .containerRelativeFrame(.horizontal) { w, _ in max(w, scrollWidth) }
                    .background(GeometryReader { g in
                        Color.clear.preference(key: ScrollOffsetKey.self, value: g.frame(in: .named("tessera-table")).minX - fixedWidth)
                    })
            }
            .onPreferenceChange(ScrollOffsetKey.self) { offset = $0 }
        }
    }

    private var fixedWidth: CGFloat { (selection != nil ? selectWidth : 0) + (fixedCount > 0 ? columns[0].width : 0) }
    private var scrollWidth: CGFloat { columns.dropFirst(fixedCount).reduce(0) { $0 + $1.width } }

    private func header(range: Range<Int>, withSelect: Bool) -> some View {
        HStack(spacing: 0) {
            if withSelect { Color.clear.frame(width: selectWidth, height: tableHead) }
            ForEach(range, id: \.self) { i in headCell(i) }
        }
    }

    @ViewBuilder private func headCell(_ i: Int) -> some View {
        let col = columns[i]
        let dir: TesseraSortDirection = sortColumn == i ? sortDirection : .none
        Group {
            if col.sortable {
                SortHeader(title: col.title, numeric: col.numeric, direction: dir) { onSort(i) }
                    .padding(.horizontal, 10)
            } else {
                Text(col.title).font(.system(size: 13, weight: .semibold)).foregroundStyle(TesseraColor.textSecondary)
                    .padding(.horizontal, 16)
            }
        }
        .frame(width: col.width, height: tableHead, alignment: col.numeric ? .trailing : .leading)
    }

    @ViewBuilder private func bodyCell(_ i: Int, _ row: Row?) -> some View {
        let col = columns[i]
        Group {
            if let row {
                if let cell = col.cell { cell(row) } else {
                    TesseraLabel(col.text(row), style: .bodySm).monospacedDigit().lineLimit(1)
                }
            } else {
                TesseraSkeleton().frame(width: col.width * (0.3 + Double((i * 23) % 40) / 100), height: 12)
            }
        }
        .padding(.horizontal, 16)
        .frame(width: col.width, height: tableRow, alignment: col.numeric ? .trailing : .leading)
    }

    private var selectAll: some View {
        let keys = allRows.map(id)
        let picked = keys.filter { selection?.wrappedValue.contains($0) ?? false }.count
        let state: Bool? = picked == 0 ? false : (picked == keys.count ? true : nil)
        return TableCheck(state: state, label: "全选") {
            guard let selection else { return }
            if state == true { selection.wrappedValue.subtract(keys) } else { selection.wrappedValue.formUnion(keys) }
        }
    }

    private func rowCheckbox(_ row: Row, selection: Binding<Set<ID>>) -> some View {
        let key = id(row)
        let on = selection.wrappedValue.contains(key)
        let name = columns.first.map { $0.text(row) } ?? ""
        return TableCheck(state: on, label: "选择 \(name)") {
            if on { selection.wrappedValue.remove(key) } else { selection.wrappedValue.insert(key) }
        }
    }
}

/// 表格里的复选框（支持半选）：触控范围 44
private struct TableCheck: View {
    let state: Bool?
    let label: String
    let action: () -> Void

    var body: some View {
        let on = state != false
        let shape = RoundedRectangle(cornerRadius: 7, style: .continuous)
        Button(action: action) {
            ZStack {
                if on {
                    Color.clear.tesseraTile(TesseraColor.accentControl, style: .solid, shape: shape)
                } else {
                    shape.fill(TesseraColor.fillDefault)
                    shape.strokeBorder(TesseraColor.borderStrong, lineWidth: 1.5)
                }
                if state == true { TesseraIcon(.check, size: 14).foregroundStyle(TesseraColor.textOnSolid) }
                if state == nil { Capsule().fill(TesseraColor.textOnSolid).frame(width: 10, height: 2) }
            }
            .frame(width: 20, height: 20)
            .frame(width: TesseraSize.touchMin, height: TesseraSize.touchMin)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(label)
        .accessibilityValue(state == true ? "已选中" : (state == nil ? "部分选中" : "未选中"))
        .accessibilityAddTraits(state == true ? .isSelected : [])
    }
}

private struct SortHeader: View {
    let title: String
    let numeric: Bool
    let direction: TesseraSortDirection
    let action: () -> Void

    var body: some View {
        let active = direction != .none
        let icon: TesseraIconName = direction == .ascending ? .arrowUp : (direction == .descending ? .arrowDown : .chevronsUpDown)
        let fg = active ? TesseraColor.accentText : TesseraColor.textSecondary
        Button(action: action) {
            HStack(spacing: 4) {
                if numeric { TesseraIcon(icon, size: 14).opacity(active ? 1 : 0.4) }
                Text(title).font(.system(size: 13, weight: .semibold)).lineLimit(1)
                if !numeric { TesseraIcon(icon, size: 14).opacity(active ? 1 : 0.4) }
            }
            .foregroundStyle(fg)
            .padding(.horizontal, 6)
            .frame(minWidth: TesseraSize.touchMin, minHeight: TesseraSize.touchMin)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel("\(title)，排序")
        .accessibilityValue(direction == .ascending ? "升序" : (direction == .descending ? "降序" : "未排序"))
    }
}

private struct TableCards<Row, ID: Hashable>: View {
    let rows: [Row]
    let columns: [TesseraColumn<Row>]
    let id: (Row) -> ID
    let selection: Binding<Set<ID>>?
    let loading: Bool
    let placeholders: Int
    let emptyText: String
    @Environment(\.tesseraShape) private var tesseraShape

    var body: some View {
        let round = tesseraShape.isRound
        if !loading && rows.isEmpty {
            TesseraLabel(emptyText, style: .bodySm, color: TesseraColor.textTertiary)
                .frame(maxWidth: .infinity).padding(TesseraSpace.s10)
                .background(TesseraColor.bgSurface, in: tableRowShape(0, 1, leading: true, trailing: true, shape: tesseraShape))
        } else {
            VStack(spacing: round ? TesseraRound.gap : TesseraTile.grout) {
                ForEach(0..<(loading ? placeholders : rows.count), id: \.self) { r in
                    let shape = round ? tableRowShape(0, 1, leading: true, trailing: true, shape: tesseraShape)
                        : tableRowShape(r, loading ? placeholders : rows.count, leading: true, trailing: true, shape: tesseraShape)
                    Group {
                        if loading { placeholder } else { card(rows[r]) }
                    }
                    .background(TesseraColor.bgSurface, in: shape)
                }
            }
        }
    }

    private var placeholder: some View {
        VStack(alignment: .leading, spacing: 10) {
            TesseraSkeleton().frame(width: 140, height: 14)
            TesseraSkeleton().frame(width: 220, height: 12)
        }
        .padding(16).frame(maxWidth: .infinity, alignment: .leading)
    }

    private func card(_ row: Row) -> some View {
        let key = id(row)
        let on = selection?.wrappedValue.contains(key) ?? false
        return VStack(spacing: 0) {
            HStack(spacing: 6) {
                if let selection {
                    TableCheck(state: on, label: "选择 \(columns[0].text(row))") {
                        if on { selection.wrappedValue.remove(key) } else { selection.wrappedValue.insert(key) }
                    }
                    .padding(.leading, -12)
                }
                if let cell = columns[0].cell { cell(row) } else {
                    Text(columns[0].text(row)).font(.system(size: 15, weight: .semibold)).foregroundStyle(TesseraColor.textPrimary)
                }
                Spacer(minLength: 0)
            }
            .frame(minHeight: 44)
            ForEach(1..<columns.count, id: \.self) { i in
                HStack {
                    TesseraLabel(columns[i].title, style: .caption, color: TesseraColor.textSecondary)
                    Spacer(minLength: TesseraSpace.s3)
                    if let cell = columns[i].cell { cell(row) } else {
                        TesseraLabel(columns[i].text(row), style: .bodySm).monospacedDigit().multilineTextAlignment(.trailing)
                    }
                }
                .padding(.vertical, 6)
                .accessibilityElement(children: .combine)
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 6)
        .background(on ? TesseraColor.accentSolid.opacity(tesseraShape.isRound ? 0.12 : 0.10) : .clear)
    }
}

private struct TableFooter: View {
    let total: Int
    let page: Int
    let pages: Int
    let pageSize: Int
    let shownCount: Int
    let go: (Int) -> Void

    var body: some View {
        let from = total == 0 ? 0 : (page - 1) * pageSize + 1
        HStack(spacing: 0) {
            TesseraLabel(total == 0 ? "共 0 条" : "共 \(total) 条，第 \(from)–\(from + shownCount - 1) 条", style: .caption, color: TesseraColor.textSecondary)
            Spacer(minLength: TesseraSpace.s3)
            TesseraIconButton(.chevronLeft, label: "上一页") { go(page - 1) }.disabled(page <= 1).opacity(page <= 1 ? 0.4 : 1)
            TesseraLabel("\(page) / \(pages)", style: .caption, color: TesseraColor.textSecondary).monospacedDigit()
                .accessibilityLabel("第 \(page) 页，共 \(pages) 页")
            TesseraIconButton(.chevronRight, label: "下一页") { go(page + 1) }.disabled(page >= pages).opacity(page >= pages ? 0.4 : 1)
        }
    }
}

// MARK: - 树 Tree

public struct TesseraTreeNode: Identifiable, Hashable, Sendable {
    public let id: String
    public let label: String
    public let icon: TesseraIconName?
    public let children: [TesseraTreeNode]

    public init(_ id: String, label: String, icon: TesseraIconName? = nil, children: [TesseraTreeNode] = []) {
        self.id = id
        self.label = label
        self.icon = icon
        self.children = children
    }
}

/// 树：可展开的层级列表；展开箭头带弹簧转动，子级淡入
public struct TesseraTree: View {
    let nodes: [TesseraTreeNode]
    @Binding var expanded: Set<String>
    @Binding var selection: String?
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    public init(_ nodes: [TesseraTreeNode], expanded: Binding<Set<String>>, selection: Binding<String?>) {
        self.nodes = nodes
        self._expanded = expanded
        self._selection = selection
    }

    private struct VisibleNode: Identifiable {
        let node: TesseraTreeNode
        let depth: Int
        var id: String { node.id }
    }

    /// 当前可见的行（展开的节点才列出子级），用一维列表渲染，避免视图递归
    private var visible: [VisibleNode] {
        var out: [VisibleNode] = []
        func walk(_ list: [TesseraTreeNode], _ depth: Int) {
            for n in list {
                out.append(VisibleNode(node: n, depth: depth))
                if expanded.contains(n.id) { walk(n.children, depth + 1) }
            }
        }
        walk(nodes, 0)
        return out
    }

    public var body: some View {
        VStack(spacing: 0) {
            ForEach(visible) { item in
                TreeRow(node: item.node, depth: item.depth, open: expanded.contains(item.node.id), selected: selection == item.node.id) {
                    withAnimation(.tessera(TesseraMotion.smooth, reduceMotion: reduceMotion)) {
                        if !item.node.children.isEmpty {
                            if expanded.contains(item.node.id) { expanded.remove(item.node.id) } else { expanded.insert(item.node.id) }
                        }
                        selection = item.node.id
                    }
                }
                .transition(.opacity.combined(with: .offset(y: -4)))
            }
        }
    }
}

private struct TreeRow: View {
    let node: TesseraTreeNode
    let depth: Int
    let open: Bool
    let selected: Bool
    let action: () -> Void
    @Environment(\.tesseraShape) private var tesseraShape
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        let round = tesseraShape.isRound
        let shape = RoundedRectangle(cornerRadius: round ? 22 : 10, style: .continuous)
        Button(action: action) {
            HStack(spacing: 6) {
                TesseraIcon(.chevronRight, size: 14)
                    .foregroundStyle(TesseraColor.textTertiary)
                    .rotationEffect(.degrees(open ? 90 : 0))
                    .animation(.tessera(TesseraMotion.snappy, reduceMotion: reduceMotion), value: open)
                    .frame(width: 18, height: 18)
                    .opacity(node.children.isEmpty ? 0 : 1)
                if let icon = node.icon { TesseraIcon(icon, size: 18).foregroundStyle(selected ? TesseraColor.accentText : TesseraColor.textSecondary) }
                Text(node.label)
                    .font(.system(size: 14, weight: selected ? .semibold : .regular))
                    .foregroundStyle(selected ? TesseraColor.accentText : TesseraColor.textPrimary)
                    .lineLimit(1)
                Spacer(minLength: 0)
            }
            .padding(.leading, 4)
            .padding(.trailing, 8)
            .frame(minHeight: TesseraSize.touchMin)
            .background(selected ? TesseraColor.accentSubtle : .clear, in: shape)
            .contentShape(shape)
        }
        .buttonStyle(TreePress(shape: shape))
        .padding(.leading, CGFloat(depth) * 20)
        .background(alignment: .leading) { guides(round: round) }
        .accessibilityLabel(node.label)
        .accessibilityValue(node.children.isEmpty ? "" : (open ? "已展开" : "已收起"))
        .accessibilityAddTraits(selected ? .isSelected : [])
    }

    /// 拼贴：每一层子级左侧一道细竖线；圆融不画线
    @ViewBuilder private func guides(round: Bool) -> some View {
        if !round && depth > 0 {
            HStack(spacing: 0) {
                ForEach(0..<depth, id: \.self) { _ in
                    Rectangle().fill(TesseraColor.borderSubtle).frame(width: 1.5).padding(.leading, 11).padding(.trailing, 7.5)
                }
            }
        }
    }

    private struct TreePress<S: Shape>: ButtonStyle {
        let shape: S
        @Environment(\.tesseraShape) private var tesseraShape
        func makeBody(configuration: Configuration) -> some View {
            configuration.label
                .overlay { if configuration.isPressed { shape.fill(TesseraColor.statePressed) } }
                .tesseraFlowRipple(pressed: configuration.isPressed, active: tesseraShape == .flow, color: TesseraColor.textPrimary.opacity(0.13), in: shape)
        }
    }
}

// MARK: - 折叠面板 Accordion

public struct TesseraAccordionItem: Identifiable {
    public let id: Int
    public let title: String
    public let content: AnyView

    public init<Content: View>(_ id: Int, title: String, @ViewBuilder content: () -> Content) {
        self.id = id
        self.title = title
        self.content = AnyView(content())
    }
}

/// 折叠面板：拼贴是一列砖；圆融是一整块面板，展开的那一行底色加深。single 时同时只开一个
public struct TesseraAccordion: View {
    let items: [TesseraAccordionItem]
    let single: Bool
    @State private var open: Set<Int>
    @Environment(\.tesseraShape) private var tesseraShape
    @Environment(\.colorSchemeContrast) private var contrast
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    public init(_ items: [TesseraAccordionItem], single: Bool = false, initiallyExpanded: Set<Int> = []) {
        self.items = items
        self.single = single
        self._open = State(initialValue: initiallyExpanded)
    }

    public var body: some View {
        let round = tesseraShape.isRound
        let panel = RoundedRectangle(cornerRadius: tesseraShape.roundRadius(TesseraRound.card), style: .continuous)
        VStack(spacing: round ? 0 : TesseraTile.grout) {
            ForEach(Array(items.enumerated()), id: \.element.id) { i, item in
                section(item, position: .of(i, count: items.count))
            }
        }
        .padding(round ? 6 : 0)
        .background(round ? TesseraColor.bgSurface : .clear, in: panel)
        .tesseraShadow(round ? .level1 : TesseraShadow(layers: []))
    }

    private func section(_ item: TesseraAccordionItem, position: TilePosition) -> some View {
        let round = tesseraShape.isRound
        let isOpen = open.contains(item.id)
        let shape = round ? tileShape(.single, outer: tesseraShape.roundRadius(TesseraRound.row)) : tileShape(position, outer: TesseraRadius.lg, vertical: true)
        return VStack(alignment: .leading, spacing: 0) {
            Button {
                withAnimation(.tessera(TesseraMotion.smooth, reduceMotion: reduceMotion)) {
                    if isOpen { open.remove(item.id) } else if single { open = [item.id] } else { open.insert(item.id) }
                }
            } label: {
                HStack {
                    Text(item.title).font(.system(size: 15, weight: .semibold)).foregroundStyle(TesseraColor.textPrimary)
                    Spacer(minLength: TesseraSpace.s3)
                    TesseraIcon(.chevronDown, size: 16).foregroundStyle(TesseraColor.textTertiary)
                        .rotationEffect(.degrees(isOpen ? 180 : 0))
                }
                .padding(.horizontal, round ? TesseraSpace.s4 : TesseraSpace.s5)
                .frame(minHeight: 56)
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .accessibilityAddTraits(.isHeader)
            .accessibilityValue(isOpen ? "已展开" : "已收起")
            if isOpen {
                item.content
                    .font(.system(size: 14))
                    .foregroundStyle(TesseraColor.textSecondary)
                    .padding(.horizontal, round ? 16 : 20)
                    .padding(.bottom, round ? 16 : 20)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .transition(.opacity.combined(with: .move(edge: .top)))
            }
        }
        .background(round ? (isOpen ? TesseraColor.fillDefault : .clear) : TesseraColor.bgSurface, in: shape)
        .overlay(shape.strokeBorder(!round && tesseraShape.showsOutline(contrast) ? TesseraColor.borderSurface : .clear, lineWidth: 1))
        .clipShape(shape)
    }
}

// MARK: - 描述列表 DescriptionList

/// 描述列表：每一项是左右两块砖（标签 / 值）；stacked 时上下排（默认：紧凑宽度上下排）
public struct TesseraDescriptionList: View {
    let items: [(String, String)]
    let labelWidth: CGFloat
    let stacked: Bool?
    @Environment(\.tesseraShape) private var tesseraShape
    @Environment(\.horizontalSizeClass) private var sizeClass

    public init(_ items: [(String, String)], labelWidth: CGFloat = 96, stacked: Bool? = nil) {
        self.items = items
        self.labelWidth = labelWidth
        self.stacked = stacked
    }

    public var body: some View {
        let round = tesseraShape.isRound
        let isStacked = stacked ?? (sizeClass == .compact)
        VStack(spacing: round ? 0 : TesseraTile.grout) {
            ForEach(Array(items.enumerated()), id: \.offset) { i, pair in
                DescriptionRow(label: pair.0, value: pair.1, index: i, count: items.count, stacked: isStacked, labelWidth: labelWidth)
            }
        }
        .padding(.vertical, round ? 6 : 0)
        .background(round ? TesseraColor.bgSurface : .clear, in: RoundedRectangle(cornerRadius: tesseraShape.roundRadius(TesseraRound.row), style: .continuous))
    }
}

private struct DescriptionRow: View {
    let label: String
    let value: String
    let index: Int
    let count: Int
    let stacked: Bool
    let labelWidth: CGFloat
    @Environment(\.tesseraShape) private var tesseraShape

    var body: some View {
        let round = tesseraShape.isRound
        let fill = round ? Color.clear : TesseraColor.bgSurface
        let top: CGFloat = index == 0 ? TesseraRadius.lg : TesseraRadius.grout
        let bottom: CGFloat = index == count - 1 ? TesseraRadius.lg : TesseraRadius.grout
        let g = TesseraRadius.grout
        Group {
            if stacked {
                VStack(alignment: .leading, spacing: 2) {
                    TesseraLabel(label, style: .bodySm, color: TesseraColor.textSecondary)
                    Text(value).font(.system(size: 14, weight: .medium)).foregroundStyle(TesseraColor.textPrimary)
                }
                .padding(.horizontal, 16).padding(.vertical, 10)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(fill, in: UnevenRoundedRectangle(cornerRadii: .init(topLeading: top, bottomLeading: bottom, bottomTrailing: bottom, topTrailing: top), style: .continuous))
            } else {
                HStack(alignment: .top, spacing: round ? 0 : TesseraTile.grout) {
                    TesseraLabel(label, style: .bodySm, color: TesseraColor.textSecondary)
                        .padding(.horizontal, 16).padding(.vertical, 12)
                        .frame(width: labelWidth, alignment: .leading)
                        .frame(maxHeight: .infinity, alignment: .top)
                        .background(fill, in: UnevenRoundedRectangle(cornerRadii: .init(topLeading: top, bottomLeading: bottom, bottomTrailing: g, topTrailing: g), style: .continuous))
                    Text(value).font(.system(size: 14, weight: .medium)).foregroundStyle(TesseraColor.textPrimary)
                        .padding(.horizontal, 16).padding(.vertical, 12)
                        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
                        .background(fill, in: UnevenRoundedRectangle(cornerRadii: .init(topLeading: g, bottomLeading: g, bottomTrailing: bottom, topTrailing: top), style: .continuous))
                }
                .fixedSize(horizontal: false, vertical: true)
            }
        }
        .accessibilityElement(children: .combine)
    }
}

// MARK: - 时间线 Timeline

public struct TesseraTimelineItem: Identifiable, Sendable {
    public let id = UUID()
    public let title: String
    public let time: String?
    public let body: String?
    public let tone: TesseraTone

    public init(_ title: String, time: String? = nil, body: String? = nil, tone: TesseraTone = .neutral) {
        self.title = title
        self.time = time
        self.body = body
        self.tone = tone
    }
}

/// 时间线：小砖节点 + 竖向砖缝连线
public struct TesseraTimeline: View {
    let items: [TesseraTimelineItem]
    public init(_ items: [TesseraTimelineItem]) { self.items = items }

    public var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            ForEach(Array(items.enumerated()), id: \.element.id) { i, item in
                TimelineRow(item: item, last: i == items.count - 1)
            }
        }
    }
}

private struct TimelineRow: View {
    let item: TesseraTimelineItem
    let last: Bool
    @Environment(\.tesseraShape) private var tesseraShape

    private var dot: Color {
        switch item.tone {
        case .accent: TesseraColor.accentControl
        case .success: TesseraColor.successSolid
        case .warning: TesseraColor.warningSolid
        case .danger: TesseraColor.dangerSolid
        default: TesseraColor.fillControlOff
        }
    }

    var body: some View {
        let round = tesseraShape.isRound
        HStack(alignment: .top, spacing: TesseraSpace.s3) {
            ZStack(alignment: .top) {
                if !last {
                    RoundedRectangle(cornerRadius: round ? 1.5 : 1, style: .continuous)
                        .fill(TesseraColor.fillDefault)
                        .frame(width: 3)
                        .padding(.top, 22)
                }
                RoundedRectangle(cornerRadius: round ? 7 : 5, style: .continuous)
                    .fill(dot)
                    .frame(width: 14, height: 14)
                    .padding(.top, 4)
            }
            .frame(width: 28)
            .frame(maxHeight: .infinity, alignment: .top)
            VStack(alignment: .leading, spacing: 0) {
                if let time = item.time { TesseraLabel(time, style: .caption, color: TesseraColor.textTertiary) }
                TesseraLabel(item.title, style: .label)
                if let body = item.body { TesseraLabel(body, style: .bodySm, color: TesseraColor.textSecondary).padding(.top, 2) }
            }
            .padding(.top, 1)
            .padding(.bottom, last ? 0 : TesseraSpace.s5)
            Spacer(minLength: 0)
        }
        .fixedSize(horizontal: false, vertical: true)
        .accessibilityElement(children: .combine)
    }
}

// MARK: - 统计数字 Stat

public enum TesseraTrend: Sendable { case up, down }

public struct TesseraStatItem: Identifiable, Sendable {
    public let id = UUID()
    public let label: String
    public let value: String
    public let unit: String?
    public let delta: String?
    public let trend: TesseraTrend?

    public init(_ label: String, value: String, unit: String? = nil, delta: String? = nil, trend: TesseraTrend? = nil) {
        self.label = label
        self.value = value
        self.unit = unit
        self.delta = delta
        self.trend = trend
    }
}

/// 统计数字：一组大数字砖，外角大；圆融是一整块面板
public struct TesseraStats: View {
    let items: [TesseraStatItem]
    let columns: Int
    @Environment(\.tesseraShape) private var tesseraShape

    public init(_ items: [TesseraStatItem], columns: Int = 2) {
        self.items = items
        self.columns = max(1, columns)
    }

    public var body: some View {
        let round = tesseraShape.isRound
        let gap = round ? 0 : TesseraTile.grout
        let rowCount = (items.count + columns - 1) / columns
        VStack(spacing: gap) {
            ForEach(0..<rowCount, id: \.self) { r in
                HStack(spacing: gap) {
                    ForEach(0..<columns, id: \.self) { c in
                        let i = r * columns + c
                        if i < items.count {
                            StatTile(item: items[i], shape: cellShape(r: r, c: c, rows: rowCount, lastInRow: min(columns, items.count - r * columns) - 1))
                        } else {
                            Color.clear.frame(maxWidth: .infinity)
                        }
                    }
                }
                .fixedSize(horizontal: false, vertical: true)
            }
        }
        .background(round ? TesseraColor.bgSurface : .clear, in: RoundedRectangle(cornerRadius: tesseraShape.roundRadius(TesseraRound.card), style: .continuous))
        .tesseraShadow(round ? .level1 : TesseraShadow(layers: []))
    }

    private func cellShape(r: Int, c: Int, rows: Int, lastInRow: Int) -> UnevenRoundedRectangle {
        let xl = TesseraRadius.xl, g = TesseraRadius.grout
        return UnevenRoundedRectangle(cornerRadii: .init(
            topLeading: r == 0 && c == 0 ? xl : g,
            bottomLeading: r == rows - 1 && c == 0 ? xl : g,
            bottomTrailing: r == rows - 1 && c == lastInRow ? xl : g,
            topTrailing: r == 0 && (c == lastInRow || c == columns - 1) ? xl : g), style: .continuous)
    }
}

private struct StatTile: View {
    let item: TesseraStatItem
    let shape: UnevenRoundedRectangle
    @Environment(\.tesseraShape) private var tesseraShape
    @Environment(\.colorSchemeContrast) private var contrast

    private var spoken: String {
        var s = "\(item.label) \(item.value)\(item.unit ?? "")"
        if let d = item.delta { s += "，较上期\(item.trend == .up ? "上升" : (item.trend == .down ? "下降" : "")) \(d)" }
        return s
    }

    var body: some View {
        let round = tesseraShape.isRound
        let fg: Color = item.trend == .up ? TesseraColor.successText : (item.trend == .down ? TesseraColor.dangerText : TesseraColor.textSecondary)
        VStack(alignment: .leading, spacing: 4) {
            TesseraLabel(item.label, style: .caption, color: TesseraColor.textSecondary)
            HStack(alignment: .lastTextBaseline, spacing: 4) {
                Text(item.value).font(.custom(TesseraFontFamily.display, fixedSize: 32).weight(.semibold)).monospacedDigit()
                    .foregroundStyle(TesseraColor.textPrimary).lineLimit(1).minimumScaleFactor(0.6)
                if let unit = item.unit { Text(unit).font(.system(size: 14, weight: .medium)).foregroundStyle(TesseraColor.textSecondary) }
            }
            if let delta = item.delta {
                HStack(spacing: 2) {
                    if let t = item.trend { TesseraIcon(t == .up ? .arrowUp : .arrowDown, size: 14) }
                    Text(delta).font(.system(size: 12, weight: .semibold))
                }
                .foregroundStyle(fg)
            }
        }
        .padding(TesseraSpace.s5)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
        .background(round ? .clear : TesseraColor.bgSurface, in: shape)
        .overlay(shape.strokeBorder(!round && tesseraShape.showsOutline(contrast) ? TesseraColor.borderSurface : .clear, lineWidth: 1))
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(spoken)
    }
}
