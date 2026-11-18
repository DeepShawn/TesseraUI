// TesseraUI v0.2.0 — 由 scripts/build.mjs 从 tokens/*.json 生成，请勿手改。
import SwiftUI

// MARK: - 颜色

public enum TesseraColor {
    /// 页面底，也是“砖缝”的颜色
    public static let bgCanvas = Color.tessera(light: 0xEFEBE4FF, dark: 0x151310FF, lightHC: 0xEFEBE4FF, darkHC: 0x151310FF)
    /// 卡片、砖面
    public static let bgSurface = Color.tessera(light: 0xFBF9F5FF, dark: 0x1E1C18FF, lightHC: 0xFBF9F5FF, darkHC: 0x1E1C18FF)
    /// 浮层
    public static let bgElevated = Color.tessera(light: 0xFFFFFFFF, dark: 0x282521FF, lightHC: 0xFFFFFFFF, darkHC: 0x282521FF)
    /// 浮层上的浮层
    public static let bgElevated2 = Color.tessera(light: 0xFFFFFFFF, dark: 0x322F2AFF, lightHC: 0xFFFFFFFF, darkHC: 0x322F2AFF)
    /// 反色砖：提示消息、工具提示
    public static let bgInverse = Color.tessera(light: 0x1F1B16FF, dark: 0xF0ECE4FF, lightHC: 0x1F1B16FF, darkHC: 0xF0ECE4FF)
    public static let bgScrim = Color.tessera(light: 0x1F1B165C, dark: 0x00000099, lightHC: 0x1F1B165C, darkHC: 0x00000099)
    /// 输入框、次要控件的底
    public static let fillDefault = Color.tessera(light: 0x1F1B160F, dark: 0xF0ECE414, lightHC: 0x1F1B160F, darkHC: 0xF0ECE414)
    /// 开关关闭时的轨道
    public static let fillControlOff = Color.tessera(light: 0xD9D3C8FF, dark: 0x4A453DFF, lightHC: 0x7D776BFF, darkHC: 0x7D776BFF)
    public static let textPrimary = Color.tessera(light: 0x1F1B16FF, dark: 0xF0ECE4FF, lightHC: 0x1F1B16FF, darkHC: 0xF0ECE4FF)
    public static let textSecondary = Color.tessera(light: 0x5E584EFF, dark: 0xADA699FF, lightHC: 0x4A453DFF, darkHC: 0xBDB6A9FF)
    public static let textTertiary = Color.tessera(light: 0x77716AFF, dark: 0x7D776BFF, lightHC: 0x5E584EFF, darkHC: 0xADA699FF)
    public static let textOnSolid = Color.tessera(light: 0xFFFFFFFF, dark: 0xFFFFFFFF, lightHC: 0xFFFFFFFF, darkHC: 0xFFFFFFFF)
    public static let textInverse = Color.tessera(light: 0xF0ECE4FF, dark: 0x1F1B16FF, lightHC: 0xF0ECE4FF, darkHC: 0x1F1B16FF)
    public static let borderSubtle = Color.tessera(light: 0x1F1B1614, dark: 0xF0ECE414, lightHC: 0x1F1B1647, darkHC: 0xF0ECE44D)
    public static let borderStrong = Color.tessera(light: 0x1F1B1673, dark: 0xF0ECE473, lightHC: 0x1F1B1673, darkHC: 0xF0ECE473)
    /// 卡片、浮层外描边：浅色没有，深色很淡，增强对比度时为实线
    public static let borderSurface = Color.tessera(light: 0x00000000, dark: 0xF0ECE40F, lightHC: 0x1F1B1680, darkHC: 0xF0ECE480)
    /// 输入框描边：只在增强对比度时出现
    public static let borderField = Color.tessera(light: 0x00000000, dark: 0x00000000, lightHC: 0x1F1B1680, darkHC: 0xF0ECE480)
    /// 实色砖底部的砖厚
    public static let edgeSolid = Color.tessera(light: 0x00000038, dark: 0x00000052, lightHC: 0x00000038, darkHC: 0x00000052)
    /// 浅色砖底部的砖厚
    public static let edgeSoft = Color.tessera(light: 0x1F1B161A, dark: 0x00000066, lightHC: 0x1F1B161A, darkHC: 0x00000066)
    /// 釉面顶部的一道高光
    public static let sheen = Color.tessera(light: 0xFFFFFF29, dark: 0xFFFFFF1A, lightHC: 0xFFFFFF29, darkHC: 0xFFFFFF1A)
    /// 主按钮底（孔雀釉）
    public static let accentSolid = Color.tessera(light: 0x0F6B63FF, dark: 0x0F6B63FF, lightHC: 0x0F6B63FF, darkHC: 0x0F6B63FF)
    /// 非文字实色控件：开关开启、复选框和单选选中
    public static let accentControl = Color.tessera(light: 0x0F6B63FF, dark: 0x1F8A7EFF, lightHC: 0x0F6B63FF, darkHC: 0x1F8A7EFF)
    public static let accentText = Color.tessera(light: 0x0F6B63FF, dark: 0x6CBDB1FF, lightHC: 0x0F6B63FF, darkHC: 0x6CBDB1FF)
    public static let accentSubtle = Color.tessera(light: 0x0F6B631A, dark: 0x1F8A7E33, lightHC: 0x0F6B631A, darkHC: 0x1F8A7E33)
    public static let dangerSolid = Color.tessera(light: 0xB42318FF, dark: 0xB42318FF, lightHC: 0xB42318FF, darkHC: 0xB42318FF)
    public static let dangerText = Color.tessera(light: 0xB42318FF, dark: 0xFF8A7AFF, lightHC: 0xB42318FF, darkHC: 0xFF8A7AFF)
    public static let dangerSubtle = Color.tessera(light: 0xB423181A, dark: 0xB4231838, lightHC: 0xB423181A, darkHC: 0xB4231838)
    public static let successSolid = Color.tessera(light: 0x3D6B1CFF, dark: 0x3D6B1CFF, lightHC: 0x3D6B1CFF, darkHC: 0x3D6B1CFF)
    public static let successText = Color.tessera(light: 0x3D6B1CFF, dark: 0x9CCB6BFF, lightHC: 0x3D6B1CFF, darkHC: 0x9CCB6BFF)
    public static let successSubtle = Color.tessera(light: 0x3D6B1C1A, dark: 0x3D6B1C38, lightHC: 0x3D6B1C1A, darkHC: 0x3D6B1C38)
    public static let warningSolid = Color.tessera(light: 0x8A5600FF, dark: 0x8A5600FF, lightHC: 0x8A5600FF, darkHC: 0x8A5600FF)
    public static let warningText = Color.tessera(light: 0x8A5600FF, dark: 0xE0A643FF, lightHC: 0x8A5600FF, darkHC: 0xE0A643FF)
    public static let warningSubtle = Color.tessera(light: 0x8A56001A, dark: 0x8A560038, lightHC: 0x8A56001A, darkHC: 0x8A560038)
    public static let focusGlow = Color.tessera(light: 0x0F6B6338, dark: 0x1F8A7E61, lightHC: 0x0F6B6338, darkHC: 0x1F8A7E61)
    public static let focusRing = Color.tessera(light: 0x0F6B63FF, dark: 0x6CBDB1FF, lightHC: 0x0F6B63FF, darkHC: 0x6CBDB1FF)
    public static let selection = Color.tessera(light: 0x1F8A7E38, dark: 0x1F8A7E52, lightHC: 0x1F8A7E38, darkHC: 0x1F8A7E52)
    public static let stateHover = Color.tessera(light: 0x1F1B160D, dark: 0xF0ECE40F, lightHC: 0x1F1B160D, darkHC: 0xF0ECE40F)
    public static let statePressed = Color.tessera(light: 0x1F1B1617, dark: 0xF0ECE41A, lightHC: 0x1F1B1617, darkHC: 0xF0ECE41A)
    public static let stateSolidHover = Color.tessera(light: 0xFFFFFF14, dark: 0xFFFFFF14, lightHC: 0xFFFFFF14, darkHC: 0xFFFFFF14)
    public static let stateSolidPressed = Color.tessera(light: 0x0000001A, dark: 0x0000001A, lightHC: 0x0000001A, darkHC: 0x0000001A)
    /// 阴影带石色暖调，不用纯黑
    public static let shadowColor = Color.tessera(light: 0x1F1B16FF, dark: 0x000000FF, lightHC: 0x1F1B16FF, darkHC: 0x000000FF)
    /// 矿物颜料：只用于头像、分类、插图，不表示状态
    public static let pigmentCinnabar = Color.tessera(light: 0xC4472DFF, dark: 0xC4472DFF, lightHC: 0xC4472DFF, darkHC: 0xC4472DFF)
    public static let pigmentCinnabarSubtle = Color.tessera(light: 0xC4472D24, dark: 0xC4472D42, lightHC: 0xC4472D24, darkHC: 0xC4472D42)
    public static let pigmentCinnabarText = Color.tessera(light: 0x93301CFF, dark: 0xF0A08CFF, lightHC: 0x93301CFF, darkHC: 0xF0A08CFF)
    /// 矿物颜料：只用于头像、分类、插图，不表示状态
    public static let pigmentOchre = Color.tessera(light: 0xB7862BFF, dark: 0xB7862BFF, lightHC: 0xB7862BFF, darkHC: 0xB7862BFF)
    public static let pigmentOchreSubtle = Color.tessera(light: 0xB7862B24, dark: 0xB7862B42, lightHC: 0xB7862B24, darkHC: 0xB7862B42)
    public static let pigmentOchreText = Color.tessera(light: 0x7A5512FF, dark: 0xE3B866FF, lightHC: 0x7A5512FF, darkHC: 0xE3B866FF)
    /// 矿物颜料：只用于头像、分类、插图，不表示状态
    public static let pigmentLapis = Color.tessera(light: 0x2F4E9EFF, dark: 0x2F4E9EFF, lightHC: 0x2F4E9EFF, darkHC: 0x2F4E9EFF)
    public static let pigmentLapisSubtle = Color.tessera(light: 0x2F4E9E24, dark: 0x2F4E9E42, lightHC: 0x2F4E9E24, darkHC: 0x2F4E9E42)
    public static let pigmentLapisText = Color.tessera(light: 0x243E80FF, dark: 0x9DB2ECFF, lightHC: 0x243E80FF, darkHC: 0x9DB2ECFF)
    /// 矿物颜料：只用于头像、分类、插图，不表示状态
    public static let pigmentAmethyst = Color.tessera(light: 0x7B4C9CFF, dark: 0x7B4C9CFF, lightHC: 0x7B4C9CFF, darkHC: 0x7B4C9CFF)
    public static let pigmentAmethystSubtle = Color.tessera(light: 0x7B4C9C24, dark: 0x7B4C9C42, lightHC: 0x7B4C9C24, darkHC: 0x7B4C9C42)
    public static let pigmentAmethystText = Color.tessera(light: 0x5E3778FF, dark: 0xC9A6E2FF, lightHC: 0x5E3778FF, darkHC: 0xC9A6E2FF)
    /// 矿物颜料：只用于头像、分类、插图，不表示状态
    public static let pigmentOlive = Color.tessera(light: 0x6A7A2BFF, dark: 0x6A7A2BFF, lightHC: 0x6A7A2BFF, darkHC: 0x6A7A2BFF)
    public static let pigmentOliveSubtle = Color.tessera(light: 0x6A7A2B24, dark: 0x6A7A2B42, lightHC: 0x6A7A2B24, darkHC: 0x6A7A2B42)
    public static let pigmentOliveText = Color.tessera(light: 0x4D5A1AFF, dark: 0xBFCB7AFF, lightHC: 0x4D5A1AFF, darkHC: 0xBFCB7AFF)
    /// 可选毛玻璃的底色（导航栏、标签栏、菜单、提示消息）
    public static let materialGlass = Color.tessera(light: 0xFBF9F5B8, dark: 0x1E1C18B8, lightHC: 0xFBF9F5B8, darkHC: 0x1E1C18B8)
    /// 可选毛玻璃滑块（开关、分段控制）
    public static let materialGlassThumb = Color.tessera(light: 0xFFFFFF9E, dark: 0x1E1C1880, lightHC: 0xFFFFFF9E, darkHC: 0x1E1C1880)
    /// 毛玻璃边缘的一圈高光
    public static let materialGlassRim = Color.tessera(light: 0xFFFFFFBF, dark: 0xFFFFFF33, lightHC: 0xFFFFFFBF, darkHC: 0xFFFFFF33)
}

// MARK: - 字体

public enum TesseraFontFamily {
    /// 标题：软衬线 Fraunces（可变字体，SOFT=100）。需加入 App 并在 Info.plist 注册。
    public static let display = "Fraunces"
    /// 正文：Instrument Sans。需加入 App 并在 Info.plist 注册；未注册时回退到系统字体。
    public static let text = "Instrument Sans"
    /// 标题里的中文：系统自带的宋体。
    public static let cjkSerif = "Songti SC"
    /// 正文里的中文：系统自动回退到苹方。
    public static let cjk = "PingFang SC"
}

public struct TesseraTextStyle: Sendable {
    public enum Family: Sendable { case display, text }
    public let size: CGFloat
    public let lineHeight: CGFloat
    public let weight: Font.Weight
    public let family: Family
    /// 字距，单位 em
    public let tracking: CGFloat
    public let relativeTo: Font.TextStyle
}

public extension TesseraTextStyle {
    static let display = TesseraTextStyle(size: 44, lineHeight: 52, weight: .semibold, family: .display, tracking: -0.02, relativeTo: .largeTitle)
    static let title1 = TesseraTextStyle(size: 32, lineHeight: 40, weight: .semibold, family: .display, tracking: -0.015, relativeTo: .title)
    static let title2 = TesseraTextStyle(size: 24, lineHeight: 32, weight: .semibold, family: .display, tracking: -0.01, relativeTo: .title2)
    static let title3 = TesseraTextStyle(size: 19, lineHeight: 26, weight: .semibold, family: .display, tracking: 0, relativeTo: .title3)
    static let body = TesseraTextStyle(size: 16, lineHeight: 26, weight: .regular, family: .text, tracking: 0, relativeTo: .body)
    static let bodySm = TesseraTextStyle(size: 14, lineHeight: 22, weight: .regular, family: .text, tracking: 0, relativeTo: .subheadline)
    static let label = TesseraTextStyle(size: 14, lineHeight: 20, weight: .semibold, family: .text, tracking: 0, relativeTo: .subheadline)
    static let caption = TesseraTextStyle(size: 12, lineHeight: 18, weight: .regular, family: .text, tracking: 0, relativeTo: .caption)
    static let overline = TesseraTextStyle(size: 11, lineHeight: 16, weight: .semibold, family: .text, tracking: 0.12, relativeTo: .caption2)
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
    public static let grout: CGFloat = 6
    public static let sm: CGFloat = 8
    public static let md: CGFloat = 14
    public static let lg: CGFloat = 18
    public static let xl: CGFloat = 24
    public static let s2xl: CGFloat = 32
    public static let full: CGFloat = 9999
}

/// 拼贴：成组的砖之间留缝，实色砖底部有砖厚
public enum TesseraTile {
    public static let grout: CGFloat = 3
    public static let edge: CGFloat = 2
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
    public static let level1 = TesseraShadow(layers: [.init(y: 1, blur: 2, lightOpacity: 0.05, darkOpacity: 0.4), .init(y: 4, blur: 14, lightOpacity: 0.05, darkOpacity: 0.4)])
    public static let level2 = TesseraShadow(layers: [.init(y: 2, blur: 6, lightOpacity: 0.07, darkOpacity: 0.4), .init(y: 14, blur: 36, lightOpacity: 0.1, darkOpacity: 0.4)])
    public static let level3 = TesseraShadow(layers: [.init(y: 4, blur: 12, lightOpacity: 0.09, darkOpacity: 0.4), .init(y: 28, blur: 72, lightOpacity: 0.16, darkOpacity: 0.4)])
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
    /// 按下时砖往下沉的距离
    public static let pressSink: CGFloat = 1
    public static let pressDuration: Double = 0.08
    public static let settleLift: CGFloat = 6
    public static let settleScale: CGFloat = 1.015
    public static let flingVelocity: CGFloat = 500
    public static let staggerStep: Double = 0.03
    public static let staggerMaxItems = 8
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
