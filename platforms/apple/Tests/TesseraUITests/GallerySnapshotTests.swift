#if os(macOS)
import AppKit
import SwiftUI
import XCTest
@testable import TesseraUI

/// 组件总览截图：三种外观 × 浅色 / 深色 / 增强对比度配色，共 9 张。
/// 用 ImageRenderer 在 macOS 上渲染，不需要模拟器。图片写到 .build/snapshots（或环境变量 TESSERA_SNAPSHOT_DIR），
/// CI 上传为 apple-snapshots 供人工复查。
/// 限制：ImageRenderer 画不出系统原生控件（文本输入框、系统菜单按钮），这些位置会是空白；
/// 增强对比度只能切换配色，描边由系统的 colorSchemeContrast 决定，这里无法模拟。
@MainActor
final class GallerySnapshotTests: XCTestCase {
    private var outDir: URL {
        let env = ProcessInfo.processInfo.environment["TESSERA_SNAPSHOT_DIR"]
        let url = env.map { URL(fileURLWithPath: $0) }
            ?? URL(fileURLWithPath: #filePath).deletingLastPathComponent().appendingPathComponent("../../.build/snapshots").standardized
        try? FileManager.default.createDirectory(at: url, withIntermediateDirectories: true)
        return url
    }

    func testGallery() throws {
        try shoot("gallery") { AnyView(TesseraGallery(scrolls: false)) }
    }

    /// v0.5 表单与输入组件
    func testFormsGallery() throws {
        try shoot("forms") { AnyView(TesseraFormsGallery(scrolls: false)) }
    }

    private func shoot(_ name: String, _ make: () -> AnyView) throws {
        let shapes: [(String, TesseraShape)] = [("tile", .tile), ("round", .round), ("flow", .flow)]
        let themes: [(String, NSAppearance.Name, ColorScheme)] = [
            ("light", .aqua, .light), ("dark", .darkAqua, .dark), ("light-hc", .accessibilityHighContrastAqua, .light),
        ]
        var written = 0
        for (shapeName, shape) in shapes {
            for (themeName, appearanceName, scheme) in themes {
                let view = make()
                    .tesseraShape(shape, requireCapableDevice: false)
                    .environment(\.colorScheme, scheme)
                    .frame(width: 420)
                let renderer = ImageRenderer(content: view)
                renderer.scale = 2
                var image: CGImage?
                NSAppearance(named: appearanceName)!.performAsCurrentDrawingAppearance { image = renderer.cgImage }
                let cg = try XCTUnwrap(image, "\(name) \(shapeName)-\(themeName) 渲染失败")
                XCTAssertGreaterThan(cg.height, 1200, "\(name) \(shapeName)-\(themeName) 高度不对，内容可能没有排开")
                let rep = NSBitmapImageRep(cgImage: cg)
                let data = try XCTUnwrap(rep.representation(using: .png, properties: [:]))
                let prefix = name == "gallery" ? "" : "\(name)-"
                try data.write(to: outDir.appendingPathComponent("\(prefix)\(shapeName)-\(themeName).png"))
                written += 1
            }
        }
        XCTAssertEqual(written, 9)
        print("截图 \(name) \(written) 张：\(outDir.path)")
    }
}
#endif
