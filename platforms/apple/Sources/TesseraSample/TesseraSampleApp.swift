import SwiftUI
import TesseraUI

/// macOS 示例 App：swift run TesseraSample。可切换外观与浅色 / 深色，查看首批组件。
/// 增强对比度跟随系统设置（系统设置 → 辅助功能 → 显示 → 增强对比度）。
@main
struct TesseraSampleApp: App {
    @State private var shape: TesseraShape = .tile
    @State private var scheme = 0
    @State private var gallery = 0

    var body: some Scene {
        WindowGroup("TesseraUI 示例") {
            VStack(spacing: 0) {
                HStack(spacing: TesseraSpace.s4) {
                    TesseraSegmentedControl(["拼贴", "圆融", "流韵"], selection: Binding(
                        get: { shape == .tile ? 0 : shape == .round ? 1 : 2 },
                        set: { shape = [TesseraShape.tile, .round, .flow][$0] }))
                    TesseraSegmentedControl(["跟随系统", "浅色", "深色"], selection: $scheme)
                    TesseraSegmentedControl(["首批组件", "表单与输入", "数据展示"], selection: $gallery)
                }
                .padding(TesseraSpace.s4)
                if gallery == 0 { TesseraGallery() } else if gallery == 1 { TesseraFormsGallery() } else { TesseraDataGallery() }
            }
            .frame(minWidth: 640, minHeight: 760)
            .background(TesseraColor.bgCanvas)
            .tesseraShape(shape)
            .preferredColorScheme(scheme == 1 ? .light : scheme == 2 ? .dark : nil)
        }
    }
}
