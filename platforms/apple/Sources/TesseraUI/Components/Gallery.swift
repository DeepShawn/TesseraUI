import SwiftUI

/// 组件总览：在 Xcode 预览中查看全部首批组件（浅色 / 深色 / 增强对比度）
public struct TesseraGallery: View {
    @State private var on = true
    @State private var off = false
    @State private var check = true
    @State private var radio = 0
    @State private var segment = 1
    @State private var tab = 0
    @State private var barTab = 0
    @State private var name = ""
    @State private var query = "设计"
    @State private var sort = 0
    @State private var toast: TesseraToastMessage? = nil
    @State private var dialog = false
    @State private var sheet = false

    public init() {}

    private struct Row: Identifiable { let id: Int; let title: String; let subtitle: String; let tone: TesseraTone }
    private let rows = [Row(id: 0, title: "设计评审", subtitle: "今天 14:00 · 3 号会议室", tone: .lapis),
                        Row(id: 1, title: "v0.3 发布检查", subtitle: "三端组件已完成", tone: .olive),
                        Row(id: 2, title: "首批图标", subtitle: "157 / 150", tone: .ochre)]

    public var body: some View {
        VStack(spacing: 0) {
            TesseraNavBar("组件", leading: { TesseraIconButton(.chevronLeft, label: "返回") {} },
                          trailing: { TesseraIconButton(.bell, label: "通知", badge: 3) {} })
            ScrollView {
                VStack(alignment: .leading, spacing: TesseraSpace.s5) {
                    TesseraLabel("早上好", style: .title1)
                    HStack(spacing: TesseraSpace.s2) {
                        Button("主要按钮") { toast = .init("已保存到相册", icon: .successFill) }.buttonStyle(.tessera())
                        Button("次要按钮") { dialog = true }.buttonStyle(.tessera(.secondary))
                        Button("删除") { sheet = true }.buttonStyle(.tessera(.danger))
                    }
                    TesseraTextField("昵称", text: $name, placeholder: "输入昵称", help: "选定汉字之前不计数")
                    TesseraSearchField(text: $query) { _ in }
                    TesseraSelect("排序", selection: $sort, options: [(0, "最近更新"), (1, "名称"), (2, "大小")])
                    Toggle("接收通知", isOn: $check).toggleStyle(.tesseraCheckbox)
                    TesseraRadioGroup(selection: $radio, options: [(0, "标清"), (1, "高清"), (2, "原画")])
                    HStack(spacing: TesseraSpace.s5) {
                        Toggle("Wi-Fi", isOn: $on).toggleStyle(.tesseraSwitch)
                        Toggle("蓝牙", isOn: $off).toggleStyle(.tesseraSwitch(glass: true))
                    }
                    TesseraSegmentedControl(["日", "周", "月", "全年"], selection: $segment)
                    TesseraTabs(["概览", "活动记录", "成员", "设置"], selection: $tab)
                    HStack(spacing: TesseraSpace.s2) {
                        TesseraTag("进行中", tone: .accent); TesseraTag("已完成", tone: .success, icon: .check)
                        TesseraTag("已逾期", tone: .danger); TesseraBadge(count: 8)
                    }
                    HStack(spacing: TesseraSpace.s4) {
                        TesseraAvatar("林", size: .lg); TesseraAvatar("朱", tone: .cinnabar)
                        TesseraAvatarGroup(["陈", "吴", "赵", "周", "王"])
                        TesseraMenu { Button("编辑") {}; Button("删除", role: .destructive) {} }
                    }
                    TesseraList(rows) { r, position in
                        TesseraListItem(r.title, subtitle: r.subtitle, position: position, action: {}) {
                            TesseraAvatar(String(r.title.prefix(1)), tone: r.tone)
                        } trailing: { TesseraIcon(.chevronRight, size: 18) }
                    }
                    TesseraCard {
                        HStack(spacing: TesseraSpace.s3) {
                            TesseraSkeleton(cornerRadius: 14).frame(width: 40, height: 40)
                            VStack(spacing: 8) { TesseraSkeleton().frame(height: 12); TesseraSkeleton().frame(height: 12) }
                        }
                    }
                }
                .padding(TesseraSpace.s5)
            }
            TesseraTabBar([.init("首页", icon: .home, selectedIcon: .homeFill), .init("收藏", icon: .star, selectedIcon: .starFill),
                           .init("消息", icon: .message, selectedIcon: .messageFill, badge: 5), .init("我的", icon: .user, selectedIcon: .userFill)],
                          selection: $barTab)
        }
        .background(TesseraColor.bgCanvas)
        .tesseraToast($toast)
        .tesseraDialog(isPresented: $dialog, title: "发布 v0.3？", message: "发布后会生成 0.3.0 标签和更新日志。", confirmTitle: "发布") {}
        .tesseraSheet(isPresented: $sheet, title: "分享到") { TesseraLabel("复制链接") }
    }
}

#Preview("浅色") { TesseraGallery() }
#Preview("深色") { TesseraGallery().preferredColorScheme(.dark) }
#Preview("圆融 · 浅色") { TesseraGallery().tesseraShape(.round) }
#Preview("圆融 · 深色") { TesseraGallery().tesseraShape(.round).preferredColorScheme(.dark) }
#Preview("流韵 · 浅色") { TesseraGallery().tesseraShape(.flow, requireCapableDevice: false) }
#Preview("流韵 · 深色") { TesseraGallery().tesseraShape(.flow, requireCapableDevice: false).preferredColorScheme(.dark) }
