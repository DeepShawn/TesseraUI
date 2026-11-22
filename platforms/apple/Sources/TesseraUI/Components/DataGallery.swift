import SwiftUI

/// 示例数据：订单
public struct TesseraSampleOrder: Identifiable, Sendable {
    public let id: String
    public let customer: String
    public let city: String
    public let amount: String
    public let status: String
}

let sampleOrders: [TesseraSampleOrder] = [
    .init(id: "A-1024", customer: "王小明", city: "杭州", amount: "1,280", status: "已发货"),
    .init(id: "A-1025", customer: "李思", city: "北京", amount: "356", status: "待付款"),
    .init(id: "A-1026", customer: "陈一", city: "广州", amount: "12,040", status: "已完成"),
    .init(id: "A-1027", customer: "张安", city: "成都", amount: "89", status: "已退款"),
    .init(id: "A-1028", customer: "赵宁", city: "上海", amount: "2,400", status: "已发货"),
]

private func statusTone(_ s: String) -> TesseraTone {
    switch s {
    case "已完成": .success
    case "待付款": .warning
    case "已退款": .neutral
    default: .accent
    }
}

let sampleColumns: [TesseraColumn<TesseraSampleOrder>] = [
    TesseraColumn("客户", width: 96, sortable: true, text: { $0.customer }),
    TesseraColumn("金额", width: 100, sortable: true, numeric: true, text: { $0.amount }),
    TesseraColumn("城市", width: 84, sortable: true, text: { $0.city }),
    TesseraColumn("状态", width: 100, text: { $0.status }, cell: { AnyView(TesseraTag($0.status, tone: statusTone($0.status))) }),
    TesseraColumn("订单号", width: 100, text: { $0.id }),
]

let sampleTree: [TesseraTreeNode] = [
    TesseraTreeNode("src", label: "src", icon: .folder, children: [
        TesseraTreeNode("components", label: "components", icon: .folder, children: [
            TesseraTreeNode("button", label: "Button.swift", icon: .fileText),
            TesseraTreeNode("table", label: "Table.swift", icon: .fileText),
        ]),
        TesseraTreeNode("theme", label: "Theme.swift", icon: .fileText),
    ]),
    TesseraTreeNode("docs", label: "docs", icon: .folder, children: [TesseraTreeNode("spec", label: "spec.md", icon: .fileText)]),
    TesseraTreeNode("readme", label: "README.md", icon: .fileText),
]

/// 示例图片：一片釉色马赛克（不依赖图片资源）
func tesseraMosaic(_ base: Color, _ accent: Color) -> Image {
    Image(size: CGSize(width: 400, height: 260)) { ctx in
        let size = CGSize(width: 400, height: 260)
        ctx.fill(Path(CGRect(origin: .zero, size: size)), with: .color(base))
        let cols = 8, rows = 5
        let gap: CGFloat = 4
        let w = (size.width - gap * CGFloat(cols + 1)) / CGFloat(cols)
        let h = (size.height - gap * CGFloat(rows + 1)) / CGFloat(rows)
        for r in 0..<rows {
            for c in 0..<cols {
                let k = (r * 7 + c * 3) % 5
                let color: Color = [accent, accent.opacity(0.7), .white.opacity(0.18), .black.opacity(0.12), .white.opacity(0.06)][k]
                let rect = CGRect(x: gap + CGFloat(c) * (w + gap), y: gap + CGFloat(r) * (h + gap), width: w, height: h)
                ctx.fill(Path(roundedRect: rect, cornerRadius: w / 6, style: .continuous), with: .color(color))
            }
        }
    }
}

/// v0.6 数据展示组件总览：Xcode 预览、截图测试、iOS UI 测试与示例 App 共用
public struct TesseraDataGallery: View {
    private let scrolls: Bool
    @State private var selection: Set<String> = ["A-1025"]
    @State private var cardSelection: Set<String> = ["A-1024"]
    @State private var expanded: Set<String> = ["src", "components"]
    @State private var treeSelection: String? = "table"
    @State private var popover = false
    @State private var viewer = false

    /// - Parameter scrolls: 截图测试传 false（ImageRenderer 画不出系统滚动视图）
    public init(scrolls: Bool = true) { self.scrolls = scrolls }

    public var body: some View {
        Group {
            if scrolls { ScrollView(showsIndicators: false) { content } } else { content }
        }
        .background(TesseraColor.bgCanvas)
        .environment(\.tesseraStaticRender, !scrolls)
    }

    private func section(_ title: String) -> some View {
        TesseraLabel(title, style: .title2).accessibilityAddTraits(.isHeader).padding(.top, TesseraSpace.s4)
    }

    private func caption(_ text: String) -> some View {
        TesseraLabel(text, style: .caption, color: TesseraColor.textSecondary)
    }

    private var content: some View {
        VStack(alignment: .leading, spacing: TesseraSpace.s4) {
            tables
            treeAndDetails
            overlays
            media
            status
        }
        .padding(TesseraSpace.s5)
    }

    @ViewBuilder private var tables: some View {
        section("表格")
        caption("宽屏：固定首列、可排序、多选、分页")
        TesseraTable(sampleOrders, columns: sampleColumns, id: \.id, selection: $selection, pageSize: 4, layout: .grid)
        caption("窄屏：每行一张卡片")
        TesseraTable(Array(sampleOrders.prefix(2)), columns: sampleColumns, id: \.id, selection: $cardSelection, layout: .cards)
        caption("加载中 · 筛选后没有结果")
        TesseraTable(sampleOrders, columns: Array(sampleColumns.prefix(3)), id: \.id, pageSize: 2, loading: true, layout: .grid)
        TesseraTable(sampleOrders, columns: sampleColumns, id: \.id, filter: "深圳", layout: .grid)
    }

    @ViewBuilder private var treeAndDetails: some View {
        section("树 · 折叠面板")
        TesseraTree(sampleTree, expanded: $expanded, selection: $treeSelection)
        TesseraAccordion([
            TesseraAccordionItem(0, title: "如何退换货？") { Text("收货 7 天内可申请无理由退货，商品需保持完好。") },
            TesseraAccordionItem(1, title: "多久发货？") { Text("付款后 48 小时内发货。") },
            TesseraAccordionItem(2, title: "支持哪些付款方式？") { Text("支付宝、微信支付、银行卡。") },
        ], single: true, initiallyExpanded: [0])

        section("描述列表 · 统计数字 · 时间线")
        TesseraDescriptionList([("订单号", "A-1024"), ("下单时间", "2026 年 11 月 19 日 09:30"), ("收货地址", "浙江省杭州市西湖区文三路 90 号"), ("备注", "工作日送货")])
        TesseraStats([
            TesseraStatItem("本月收入", value: "¥86,240", delta: "12.5%", trend: .up),
            TesseraStatItem("新增用户", value: "1,208", unit: "人", delta: "3.1%", trend: .down),
            TesseraStatItem("转化率", value: "4.6", unit: "%"),
            TesseraStatItem("退款率", value: "0.8", unit: "%", delta: "0.2%", trend: .down),
        ])
        TesseraTimeline([
            TesseraTimelineItem("已签收", time: "11 月 19 日 14:02", body: "本人签收，感谢使用", tone: .success),
            TesseraTimelineItem("派送中", time: "11 月 19 日 09:15", body: "快递员：周师傅 138 0000 0000", tone: .accent),
            TesseraTimelineItem("已到达杭州转运中心", time: "11 月 18 日 22:40"),
            TesseraTimelineItem("已发货", time: "11 月 18 日 10:05"),
        ])
        HStack(spacing: TesseraSpace.s3) {
            TesseraLabel("搜索", style: .bodySm)
            TesseraKbd("⌘", "K")
            TesseraLabel("保存", style: .bodySm)
            TesseraKbd("⌘", "S")
        }
    }

    @ViewBuilder private var overlays: some View {
        section("提示 · 气泡卡片 · 代码块")
        HStack(spacing: TesseraSpace.s3) {
            TesseraIconButton(.copy, label: "复制链接") {}.tesseraTooltip("复制链接")
            TesseraTooltipBubble("长按或悬停显示提示")
        }
        Button("配送说明") { popover = true }
            .buttonStyle(.tessera(.secondary, size: .md))
            .tesseraPopover(isPresented: $popover) {
                TesseraPopoverPanel(title: "配送说明") {
                    TesseraLabel("江浙沪次日达，其他地区 2～3 天送达。", style: .bodySm, color: TesseraColor.textSecondary)
                }
            }
        TesseraPopoverPanel(title: "配送说明") {
            TesseraLabel("江浙沪次日达，其他地区 2～3 天送达。", style: .bodySm, color: TesseraColor.textSecondary)
            HStack(spacing: TesseraSpace.s2) {
                Button("知道了") {}.buttonStyle(.tessera(.primary, size: .sm))
                Button("查看详情") {}.buttonStyle(.tessera(.secondary, size: .sm))
            }
            .padding(.top, 6)
        }
        TesseraCodeBlock("TesseraTable(orders,\n    columns: columns,\n    id: \\.id,\n    selection: $selected)", language: "swift", lineNumbers: true)
    }

    @ViewBuilder private var media: some View {
        section("轮播 · 图片预览")
        TesseraCarousel(count: 3, height: 180, peek: true, initialPage: 1) { i in
            ZStack(alignment: .bottomLeading) {
                [tesseraMosaic(TesseraColor.accentSolid, TesseraColor.accentControl),
                 tesseraMosaic(TesseraColor.pigmentLapis, TesseraColor.pigmentOchre),
                 tesseraMosaic(TesseraColor.pigmentOlive, TesseraColor.pigmentCinnabar)][i]
                    .resizable().scaledToFill()
                TesseraLabel(["秋季上新", "会员日", "以旧换新"][i], style: .title3, color: .white).padding(16)
            }
        }
        Button("打开图片预览") { viewer = true }
            .buttonStyle(.tessera(.secondary, size: .md))
            .tesseraImageViewer(isPresented: $viewer, images: [tesseraMosaic(TesseraColor.accentSolid, TesseraColor.accentControl), tesseraMosaic(TesseraColor.pigmentLapis, TesseraColor.pigmentOchre)], descriptions: ["釉色马赛克", "青金马赛克"])
        TesseraImageViewer(images: [tesseraMosaic(TesseraColor.accentSolid, TesseraColor.accentControl), tesseraMosaic(TesseraColor.pigmentLapis, TesseraColor.pigmentOchre)], descriptions: ["釉色马赛克", "青金马赛克"]) {}
            .frame(height: 300)
            .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
    }

    @ViewBuilder private var status: some View {
        section("进度 · 加载 · 横幅")
        TesseraProgress(value: 0.62, label: "上传中")
        TesseraProgress(value: 1, label: "已完成", tone: .success)
        TesseraProgress(value: nil, label: "正在准备…")
        HStack(spacing: TesseraSpace.s5) {
            TesseraProgressRing(value: 0.36, label: "存储空间")
            TesseraProgressRing(value: 0.82, size: 96, label: "本周目标")
            TesseraSpinner()
            TesseraSpinner(size: 32)
        }
        TesseraBanner("新版本已发布，重启后生效。", title: "更新可用", onDismiss: {}) {
            Button("立即重启") {}.buttonStyle(.tessera(.primary, size: .sm))
        }
        TesseraBanner("网络连接不稳定，部分内容可能无法加载。", tone: .warning)
        TesseraBanner("付款失败，请检查银行卡余额。", tone: .danger, onDismiss: {})

        section("结果 · 空状态")
        TesseraResult(.success, title: "付款成功", text: "订单 A-1024 已支付，预计明天送达。") {
            Button("查看订单") {}.buttonStyle(.tessera(.primary))
            Button("继续购物") {}.buttonStyle(.tessera(.secondary))
        }
        TesseraResult(.error, title: "提交失败", text: "网络中断，填写的内容已保存，请稍后重试。") {
            Button("重试") {}.buttonStyle(.tessera(.primary))
        }
        TesseraResult(.notFound, title: "页面不存在", text: "链接可能已失效。")
        TesseraEmptyState("还没有收藏", text: "看到喜欢的商品，点一下心形就能收藏在这里。") {
            Button("去逛逛") {}.buttonStyle(.tessera(.secondary))
        }
    }
}

#Preview("数据 · 拼贴") { TesseraDataGallery() }
#Preview("数据 · 圆融 · 深色") { TesseraDataGallery().tesseraShape(.round).preferredColorScheme(.dark) }
#Preview("数据 · 流韵") { TesseraDataGallery().tesseraShape(.flow, requireCapableDevice: false) }
