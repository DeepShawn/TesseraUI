import SwiftUI
import TesseraUI

/// iOS 示例 App：切换外观与浅色 / 深色，查看首批组件。
/// UI 测试用启动参数 -tesseraShape tile|round|flow 指定外观（流韵不检测设备）。
@main
struct TesseraSampleApp: App {
    var body: some Scene { WindowGroup { SampleRoot() } }
}

struct SampleRoot: View {
    private static let shapes: [TesseraShape] = [.tile, .round, .flow]
    @State private var shapeIndex: Int
    @State private var scheme = 0
    @State private var gallery = 0
    private let fromTest: Bool

    init() {
        let args = ProcessInfo.processInfo.arguments
        let i = args.firstIndex(of: "-tesseraShape").map { args.index(after: $0) }
        let name = i.flatMap { $0 < args.count ? args[$0] : nil }
        fromTest = name != nil
        _shapeIndex = State(initialValue: ["tile", "round", "flow"].firstIndex(of: name ?? "tile") ?? 0)
        let g = args.firstIndex(of: "-tesseraGallery").map { args.index(after: $0) }.flatMap { $0 < args.count ? args[$0] : nil }
        _gallery = State(initialValue: g == "forms" ? 1 : (g == "data" ? 2 : 0))
    }

    var body: some View {
        VStack(spacing: 0) {
            VStack(spacing: TesseraSpace.s2) {
                TesseraSegmentedControl(["拼贴", "圆融", "流韵"], selection: $shapeIndex)
                TesseraSegmentedControl(["跟随系统", "浅色", "深色"], selection: $scheme)
                TesseraSegmentedControl(["首批组件", "表单与输入", "数据展示"], selection: $gallery)
            }
            .padding(.horizontal, TesseraSpace.s4)
            .padding(.vertical, TesseraSpace.s2)
            if gallery == 0 { TesseraGallery() } else if gallery == 1 { TesseraFormsGallery() } else { TesseraDataGallery() }
        }
        .background(TesseraColor.bgCanvas)
        .tesseraShape(Self.shapes[shapeIndex], requireCapableDevice: !fromTest)
        .preferredColorScheme(scheme == 1 ? .light : scheme == 2 ? .dark : nil)
    }
}
