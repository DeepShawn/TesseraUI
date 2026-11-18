# TesseraUI · Apple（SwiftUI）

支持 iOS 17+、macOS 14+、visionOS 1+。通过 Swift Package Manager 引入本目录。

```swift
import TesseraUI

Button("保存") { }
    .buttonStyle(.tessera(.primary))

Text("区块标题")
    .tesseraText(.title2)
    .foregroundStyle(TesseraColor.textPrimary)

RoundedRectangle(cornerRadius: TesseraRadius.xl, style: .continuous) // 连续曲率圆角
    .fill(TesseraColor.bgSurface)
    .tesseraShadow(.level1)

withAnimation(TesseraMotion.smooth) { isOpen.toggle() }
```

- `Sources/TesseraUI/Generated/` 由 `npm run build` 生成，不要手改。
- 颜色自动跟随浅色 / 深色 / 增强对比度（系统“设置 › 辅助功能 › 增强对比度”）。
- 字体：把 Fraunces（标题，可变字体）和 Instrument Sans（正文）加入 App 并在 Info.plist 的 `UIAppFonts` 注册；标题中文回退到宋体，正文中文回退到苹方。
- v0.1 状态：只含 token 与按钮样式的参考实现，**尚未在 Xcode 中编译验证**。
