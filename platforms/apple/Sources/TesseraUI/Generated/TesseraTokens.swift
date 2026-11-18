// TesseraUI v0.1.0 — 由 scripts/build.mjs 从 tokens/*.json 生成，请勿手改。
import SwiftUI

// MARK: - 颜色

public enum TesseraColor {
    public static let bgCanvas = Color.tessera(light: 0xF5F5F7FF, dark: 0x111113FF, lightHC: 0xF5F5F7FF, darkHC: 0x111113FF)
    public static let bgSurface = Color.tessera(light: 0xFFFFFFFF, dark: 0x1A1A1DFF, lightHC: 0xFFFFFFFF, darkHC: 0x1A1A1DFF)
    public static let bgElevated = Color.tessera(light: 0xFFFFFFFF, dark: 0x232327FF, lightHC: 0xFFFFFFFF, darkHC: 0x232327FF)
    public static let bgElevated2 = Color.tessera(light: 0xFFFFFFFF, dark: 0x2C2C31FF, lightHC: 0xFFFFFFFF, darkHC: 0x2C2C31FF)
    public static let bgScrim = Color.tessera(light: 0x00000052, dark: 0x0000008F, lightHC: 0x00000052, darkHC: 0x0000008F)
    /// 开关关闭时的轨道
    public static let fillControlOff = Color.tessera(light: 0xC7C7CCFF, dark: 0x48484EFF, lightHC: 0x8E8E93FF, darkHC: 0x8E8E93FF)
    public static let fillDefault = Color.tessera(light: 0x0000000D, dark: 0xFFFFFF14, lightHC: 0x0000000D, darkHC: 0xFFFFFF14)
    public static let textPrimary = Color.tessera(light: 0x1D1D1FFF, dark: 0xEDEDF0FF, lightHC: 0x1D1D1FFF, darkHC: 0xEDEDF0FF)
    public static let textSecondary = Color.tessera(light: 0x68686DFF, dark: 0xA1A1A8FF, lightHC: 0x48484EFF, darkHC: 0xC7C7CCFF)
    public static let textTertiary = Color.tessera(light: 0x8E8E93FF, dark: 0x68686DFF, lightHC: 0x68686DFF, darkHC: 0xA1A1A8FF)
    public static let textOnSolid = Color.tessera(light: 0xFFFFFFFF, dark: 0xFFFFFFFF, lightHC: 0xFFFFFFFF, darkHC: 0xFFFFFFFF)
    public static let borderSubtle = Color.tessera(light: 0x0000000F, dark: 0xFFFFFF14, lightHC: 0x0000003D, darkHC: 0xFFFFFF47)
    public static let borderStrong = Color.tessera(light: 0x0000007A, dark: 0xFFFFFF7A, lightHC: 0x0000007A, darkHC: 0xFFFFFF7A)
    /// 卡片、浮层的外描边：浅色没有，深色用 subtle，增强对比度用 strong
    public static let borderSurface = Color.tessera(light: 0x00000000, dark: 0xFFFFFF14, lightHC: 0x0000007A, darkHC: 0xFFFFFF7A)
    /// 输入框描边：平时没有，增强对比度时补上
    public static let borderField = Color.tessera(light: 0x00000000, dark: 0x00000000, lightHC: 0x0000007A, darkHC: 0xFFFFFF7A)
    public static let accentSolid = Color.tessera(light: 0x5B5BD6FF, dark: 0x5B5BD6FF, lightHC: 0x5B5BD6FF, darkHC: 0x5B5BD6FF)
    public static let accentText = Color.tessera(light: 0x5252CFFF, dark: 0x9C9CF0FF, lightHC: 0x5252CFFF, darkHC: 0x9C9CF0FF)
    /// 非文字的实色控件：开关开启、复选框和单选选中、滑块轨道。深色下调亮，保证对底色 ≥ 3:1
    public static let accentControl = Color.tessera(light: 0x5B5BD6FF, dark: 0x6E6EE3FF, lightHC: 0x5B5BD6FF, darkHC: 0x6E6EE3FF)
    public static let accentSubtle = Color.tessera(light: 0x5B5BD61A, dark: 0x5B5BD633, lightHC: 0x5B5BD61A, darkHC: 0x5B5BD633)
    public static let dangerSolid = Color.tessera(light: 0xC0292EFF, dark: 0xC0292EFF, lightHC: 0xC0292EFF, darkHC: 0xC0292EFF)
    public static let dangerText = Color.tessera(light: 0xC0292EFF, dark: 0xFF6B6BFF, lightHC: 0xC0292EFF, darkHC: 0xFF6B6BFF)
    public static let dangerSubtle = Color.tessera(light: 0xC0292E1A, dark: 0xC0292E33, lightHC: 0xC0292E1A, darkHC: 0xC0292E33)
    public static let successSolid = Color.tessera(light: 0x146B3AFF, dark: 0x146B3AFF, lightHC: 0x146B3AFF, darkHC: 0x146B3AFF)
    public static let successText = Color.tessera(light: 0x146B3AFF, dark: 0x4CC38AFF, lightHC: 0x146B3AFF, darkHC: 0x4CC38AFF)
    public static let successSubtle = Color.tessera(light: 0x146B3A1A, dark: 0x146B3A33, lightHC: 0x146B3A1A, darkHC: 0x146B3A33)
    public static let warningSolid = Color.tessera(light: 0x9A5100FF, dark: 0x9A5100FF, lightHC: 0x9A5100FF, darkHC: 0x9A5100FF)
    public static let warningText = Color.tessera(light: 0x9A5100FF, dark: 0xF5A524FF, lightHC: 0x9A5100FF, darkHC: 0xF5A524FF)
    public static let warningSubtle = Color.tessera(light: 0x9A51001A, dark: 0x9A510033, lightHC: 0x9A51001A, darkHC: 0x9A510033)
    public static let focusGlow = Color.tessera(light: 0x5B5BD640, dark: 0x5B5BD666, lightHC: 0x5B5BD640, darkHC: 0x5B5BD666)
    public static let focusRing = Color.tessera(light: 0x5252CFFF, dark: 0x9C9CF0FF, lightHC: 0x5252CFFF, darkHC: 0x9C9CF0FF)
    public static let selection = Color.tessera(light: 0x5B5BD633, dark: 0x5B5BD633, lightHC: 0x5B5BD633, darkHC: 0x5B5BD633)
    public static let stateHover = Color.tessera(light: 0x0000000F, dark: 0xFFFFFF0F, lightHC: 0x0000000F, darkHC: 0xFFFFFF0F)
    public static let statePressed = Color.tessera(light: 0x0000001A, dark: 0xFFFFFF1A, lightHC: 0x0000001A, darkHC: 0xFFFFFF1A)
    public static let stateSolidHover = Color.tessera(light: 0xFFFFFF14, dark: 0xFFFFFF14, lightHC: 0xFFFFFF14, darkHC: 0xFFFFFF14)
    public static let stateSolidPressed = Color.tessera(light: 0x00000014, dark: 0x00000014, lightHC: 0x00000014, darkHC: 0x00000014)
    public static let materialGlass = Color.tessera(light: 0xFFFFFFB8, dark: 0x1A1A1DB8, lightHC: 0xFFFFFFB8, darkHC: 0x1A1A1DB8)
    public static let shadowColor = Color.tessera(light: 0x000000FF, dark: 0x000000FF, lightHC: 0x000000FF, darkHC: 0x000000FF)
}

// MARK: - 字体

public enum TesseraFontFamily {
    /// 英文和数字。需把字体文件加入 App 并在 Info.plist 注册；未注册时 SwiftUI 自动回退到系统字体。
    public static let latin = "Plus Jakarta Sans"
    /// 中文由系统自动回退到苹方，无需额外处理。
    public static let cjk = "PingFang SC"
}

public struct TesseraTextStyle: Sendable {
    public let size: CGFloat
    public let lineHeight: CGFloat
    public let weight: Font.Weight
    public let relativeTo: Font.TextStyle
}

public extension TesseraTextStyle {
    static let display = TesseraTextStyle(size: 40, lineHeight: 48, weight: .semibold, relativeTo: .largeTitle)
    static let title1 = TesseraTextStyle(size: 32, lineHeight: 40, weight: .semibold, relativeTo: .title)
    static let title2 = TesseraTextStyle(size: 24, lineHeight: 32, weight: .semibold, relativeTo: .title2)
    static let title3 = TesseraTextStyle(size: 20, lineHeight: 28, weight: .semibold, relativeTo: .title3)
    static let body = TesseraTextStyle(size: 16, lineHeight: 26, weight: .regular, relativeTo: .body)
    static let bodySm = TesseraTextStyle(size: 14, lineHeight: 22, weight: .regular, relativeTo: .subheadline)
    static let label = TesseraTextStyle(size: 14, lineHeight: 20, weight: .semibold, relativeTo: .subheadline)
    static let caption = TesseraTextStyle(size: 12, lineHeight: 18, weight: .regular, relativeTo: .caption)
}

// MARK: - 间距、尺寸、圆角

public enum TesseraSpace {
    public static let s0: CGFloat = 0
    public static let s1: CGFloat = 4
    public static let s2: CGFloat = 8
    public static let s3: CGFloat = 12
    public static let s4: CGFloat = 16
    public static let s5: CGFloat = 20
    public static let s6: CGFloat = 24
    public static let s8: CGFloat = 32
    public static let s10: CGFloat = 40
    public static let s12: CGFloat = 48
    public static let s16: CGFloat = 64
}

public enum TesseraSize {
    public static let sm: CGFloat = 32
    public static let md: CGFloat = 40
    public static let lg: CGFloat = 48
    public static let touchMin: CGFloat = 44
}

/// 使用时搭配 `RoundedRectangle(cornerRadius:style: .continuous)`，即连续曲率圆角。
public enum TesseraRadius {
    public static let xs: CGFloat = 6
    public static let sm: CGFloat = 8
    public static let md: CGFloat = 12
    public static let lg: CGFloat = 16
    public static let xl: CGFloat = 20
    public static let s2xl: CGFloat = 28
    public static let full: CGFloat = 9999
}

public enum TesseraFocus {
    public static let ringWidth: CGFloat = 2
    public static let ringOffset: CGFloat = 2
    public static let glowWidth: CGFloat = 3
}

public enum TesseraOpacity {
    public static let disabled: Double = 0.4
}

// MARK: - 阴影

public struct TesseraShadowLayer: Sendable {
    public let y: CGFloat
    public let blur: CGFloat
    public let lightOpacity: Double
    public let darkOpacity: Double
}

public struct TesseraShadow: Sendable {
    public let layers: [TesseraShadowLayer]
    public static let level1 = TesseraShadow(layers: [.init(y: 1, blur: 2, lightOpacity: 0.04, darkOpacity: 0.4), .init(y: 4, blur: 12, lightOpacity: 0.04, darkOpacity: 0.4)])
    public static let level2 = TesseraShadow(layers: [.init(y: 2, blur: 6, lightOpacity: 0.06, darkOpacity: 0.4), .init(y: 12, blur: 32, lightOpacity: 0.08, darkOpacity: 0.4)])
    public static let level3 = TesseraShadow(layers: [.init(y: 4, blur: 12, lightOpacity: 0.08, darkOpacity: 0.4), .init(y: 24, blur: 64, lightOpacity: 0.12, darkOpacity: 0.4)])
}

// MARK: - 动效

public enum TesseraMotion {
    /// 刚度 438.6，阻尼 35.6
    public static let snappy = Animation.spring(duration: 0.3, bounce: 0.15)
    /// 刚度 246.7，阻尼 31.4（默认）
    public static let smooth = Animation.spring(duration: 0.4, bounce: 0)
    /// 刚度 157.9，阻尼 18.8
    public static let bouncy = Animation.spring(duration: 0.5, bounce: 0.25)
    public static let durationInstant: Double = 0.1
    public static let durationFast: Double = 0.15
    public static let durationNormal: Double = 0.25
    public static let durationSlow: Double = 0.35
    public static func easeStandard(_ duration: Double) -> Animation { .timingCurve(0.2, 0, 0, 1, duration: duration) }
    public static func easeEnter(_ duration: Double) -> Animation { .timingCurve(0.05, 0.7, 0.1, 1, duration: duration) }
    public static func easeExit(_ duration: Double) -> Animation { .timingCurve(0.3, 0, 0.8, 0.15, duration: duration) }
    public static let pressScale: CGFloat = 0.97
    public static let flingVelocity: CGFloat = 500
    public static let staggerStep: Double = 0.03
    public static let staggerMaxItems = 6
    public static let toastDuration: Double = 3
    public static let toastDurationWithAction: Double = 5
}

// MARK: - 层级

public enum TesseraZ {
    public static let sticky: Double = 100
    public static let overlay: Double = 200
    public static let modal: Double = 300
    public static let popover: Double = 400
    public static let toast: Double = 500
    public static let tooltip: Double = 600
}
