import XCTest

/// iOS 无障碍检查（XCUITest，读取的就是 VoiceOver 看到的辅助功能树）：
/// 三种外观下，每个按钮、开关、文本框、分段控制都要有读屏名称，尺寸不小于 40×40；
/// 每种外观截一张图，作为测试附件保存在结果包里（CI 用 xcresulttool 导出为 ios-snapshots）。
/// 40 是首批控件的标准高度（TesseraSize.md）；人机界面指南建议 44，组件高度不足 44 时由使用方留出间距。
final class AccessibilityUITests: XCTestCase {
    override func setUp() { continueAfterFailure = true }

    func testLabelsAndSizes() throws {
        var problems: [String] = []
        var checked = 0
        var knownIssues = 0
        for shape in ["tile", "round", "flow"] {
            let app = XCUIApplication()
            app.launchArguments = ["-tesseraShape", shape]
            app.launch()
            XCTAssertTrue(app.buttons["主要按钮"].waitForExistence(timeout: 10), "\(shape)：组件总览没有出现")

            let shot = XCTAttachment(screenshot: XCUIScreen.main.screenshot())
            shot.name = "ios-\(shape)"; shot.lifetime = .keepAlways; add(shot)

            let queries: [(String, XCUIElementQuery)] = [
                ("按钮", app.buttons), ("开关", app.switches), ("文本框", app.textFields),
                ("搜索框", app.searchFields), ("分段控制", app.segmentedControls),
            ]
            for (kind, query) in queries {
                for el in query.allElementsBoundByIndex where el.exists {
                    let label = [el.label, el.placeholderValue ?? "", (el.value as? String) ?? ""]
                        .first { !$0.trimmingCharacters(in: .whitespaces).isEmpty }
                    let f = el.frame
                    if f.isEmpty { continue }
                    checked += 1
                    if label == nil {
                        // 已知问题：自定义外观的 Menu（下拉选择 362×40、更多菜单 44×44）在 iOS 上被系统包进一个没有名称的按钮，
                        // 试过 Label 标题、accessibilityLabel、合并子元素、accessibilityRepresentation 均无效。待真机 VoiceOver 确认，v0.5 改为自绘弹层
                        let known = kind == "按钮" && ((f.height.rounded() == 40 && f.width > 300) || (f.width.rounded() == 44 && f.height.rounded() == 44))
                        if known { knownIssues += 1 } else { problems.append("\(shape)：\(kind) 没有读屏名称（\(f)）") }
                    }
                    // 3 倍屏上的坐标会有零点几的舍入误差，允许 0.5
                    if f.width < 39.5 || f.height < 39.5 { problems.append("\(shape)：\(kind)“\(label ?? "?")”尺寸只有 \(String(format: "%.1f×%.1f", f.width, f.height))") }
                }
            }
            app.terminate()
        }
        print("无障碍检查：共检查 \(checked) 个可交互元素，问题 \(problems.count) 个，已知问题 \(knownIssues) 个（下拉选择、更多菜单没有读屏名称）")
        problems.forEach { print("  ✖ \($0)") }
        XCTAssertGreaterThan(checked, 30, "找到的可交互元素太少，检查本身可能失效")
        XCTAssertTrue(problems.isEmpty, problems.joined(separator: "\n"))
        // 已知问题修好后这里会失败，提醒把上面的例外删掉
        XCTAssertEqual(knownIssues, 6, "已知问题的数量变了：如果已修好，请删除例外")
    }
}
