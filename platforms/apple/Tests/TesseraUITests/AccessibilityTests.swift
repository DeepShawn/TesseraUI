#if os(macOS)
import AppKit
import SwiftUI
import XCTest
@testable import TesseraUI

/// 无障碍检查：把组件总览放进窗口，遍历 macOS 辅助功能树（与 VoiceOver 读到的一致），
/// 三种外观下每个可交互元素（按钮、复选框、单选、开关、文本框、下拉、分段控制）都要有读屏名称，尺寸不小于 40×40。
/// 40 是首批控件的标准高度（TesseraSize.md）；iOS 人机界面指南建议 44，组件高度不足 44 时由使用方留出间距。
@MainActor
final class AccessibilityTests: XCTestCase {
    private let interactiveRoles: Set<NSAccessibility.Role> = [
        .button, .checkBox, .radioButton, .popUpButton, .menuButton, .textField, .comboBox, .slider, .incrementor,
    ]

    func testInteractiveElementsHaveLabelsAndSize() throws {
        var problems: [String] = []
        var checked = 0
        for (name, shape) in [("拼贴", TesseraShape.tile), ("圆融", .round), ("流韵", .flow)] {
            let host = NSHostingView(rootView: TesseraGallery(scrolls: false).tesseraShape(shape, requireCapableDevice: false))
            let window = NSWindow(contentRect: NSRect(x: 0, y: 0, width: 420, height: 2000), styleMask: [.titled], backing: .buffered, defer: false)
            window.contentView = host
            window.orderFront(nil)
            host.layoutSubtreeIfNeeded()
            RunLoop.main.run(until: Date().addingTimeInterval(0.5))

            var seen = Set<ObjectIdentifier>()
            // 用属性接口读取（accessibilityAttributeValue）：所有辅助功能对象都支持，与 VoiceOver 读取的方式相同
            func attr(_ el: NSObject, _ name: NSAccessibility.Attribute) -> Any? { el.accessibilityAttributeValue(name) }
            func visit(_ element: Any, depth: Int) {
                guard depth < 60, let el = element as? NSObject, seen.insert(ObjectIdentifier(el)).inserted else { return }
                let hidden = (attr(el, NSAccessibility.Attribute(rawValue: "AXHidden")) as? Bool) ?? false
                if let roleName = attr(el, .role) as? String, interactiveRoles.contains(NSAccessibility.Role(rawValue: roleName)), !hidden {
                    checked += 1
                    let candidates = [attr(el, .description), attr(el, .title), attr(el, .placeholderValue)].compactMap { $0 as? String }
                    let label = candidates.first { !$0.trimmingCharacters(in: CharacterSet.whitespaces).isEmpty }
                    let size = (attr(el, .size) as? NSValue)?.sizeValue ?? .zero
                    if label == nil { problems.append("\(name)：\(roleName) 没有读屏名称") }
                    if size.width < 40 || size.height < 40 {
                        problems.append("\(name)：\(roleName)“\(label ?? "?")”尺寸只有 \(Int(size.width))×\(Int(size.height))")
                    }
                }
                for child in (attr(el, .children) as? [Any]) ?? [] { visit(child, depth: depth + 1) }
            }
            visit(host, depth: 0)
            window.orderOut(nil)
        }
        print("无障碍检查：共检查 \(checked) 个可交互元素，问题 \(problems.count) 个")
        problems.forEach { print("  ✖ \($0)") }
        XCTAssertGreaterThan(checked, 30, "找到的可交互元素太少，辅助功能树可能没有建立，检查本身失效")
        XCTAssertTrue(problems.isEmpty, problems.joined(separator: "\n"))
    }
}
#endif
