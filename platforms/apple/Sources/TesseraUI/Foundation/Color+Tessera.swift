import SwiftUI
#if canImport(UIKit)
import UIKit
#elseif canImport(AppKit)
import AppKit
#endif

extension Color {
    /// 创建一个随浅色 / 深色 / 增强对比度自动切换的颜色。参数是 0xRRGGBBAA。
    static func tessera(light: UInt32, dark: UInt32, lightHC: UInt32, darkHC: UInt32) -> Color {
        #if canImport(UIKit)
        return Color(uiColor: UIColor { traits in
            let isDark = traits.userInterfaceStyle == .dark
            let isHC = traits.accessibilityContrast == .high
            return UIColor(rgba: isDark ? (isHC ? darkHC : dark) : (isHC ? lightHC : light))
        })
        #elseif canImport(AppKit)
        return Color(nsColor: NSColor(name: nil) { appearance in
            let match = appearance.bestMatch(from: [
                .aqua, .darkAqua, .accessibilityHighContrastAqua, .accessibilityHighContrastDarkAqua,
            ])
            switch match {
            case .darkAqua: return NSColor(rgba: dark)
            case .accessibilityHighContrastAqua: return NSColor(rgba: lightHC)
            case .accessibilityHighContrastDarkAqua: return NSColor(rgba: darkHC)
            default: return NSColor(rgba: light)
            }
        })
        #endif
    }
}

#if canImport(UIKit)
private extension UIColor {
    convenience init(rgba: UInt32) {
        self.init(
            red: CGFloat((rgba >> 24) & 0xFF) / 255,
            green: CGFloat((rgba >> 16) & 0xFF) / 255,
            blue: CGFloat((rgba >> 8) & 0xFF) / 255,
            alpha: CGFloat(rgba & 0xFF) / 255
        )
    }
}
#elseif canImport(AppKit)
private extension NSColor {
    convenience init(rgba: UInt32) {
        self.init(
            srgbRed: CGFloat((rgba >> 24) & 0xFF) / 255,
            green: CGFloat((rgba >> 16) & 0xFF) / 255,
            blue: CGFloat((rgba >> 8) & 0xFF) / 255,
            alpha: CGFloat(rgba & 0xFF) / 255
        )
    }
}
#endif
