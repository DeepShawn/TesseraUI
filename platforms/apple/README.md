# TesseraUI · Apple（SwiftUI）

支持 iOS 17+、macOS 14+、visionOS 1+。通过 Swift Package Manager 引入本目录。

## 使用

```swift
import TesseraUI

Button("保存") { }
    .buttonStyle(.tessera(.primary))

Toggle("Wi-Fi", isOn: $wifi).toggleStyle(.tesseraSwitch)          // 可拖动的开关
Toggle("接收通知", isOn: $notify).toggleStyle(.tesseraCheckbox)    // 复选框

TesseraSegmentedControl(["日", "周", "月"], selection: $range)    // 可拖动选择
TesseraTextField("昵称", text: $name, onCommittedChange: { count = $0.count })  // 拼音选定后才回调

content
    .tesseraToast($toast)
    .tesseraDialog(isPresented: $confirm, title: "删除文件？", confirmTitle: "删除", destructive: true) { delete() }
    .tesseraSheet(isPresented: $share, title: "分享到") { ShareOptions() }

// 外观：圆融（胶囊与大圆角，成组控件合成一整块，没有砖缝和分割线）；不写即为拼贴
ContentView()
    .tesseraShape(.round)

// 流韵：在圆融基础上，圆角介于方砖与胶囊之间，按下变成胶囊并荡开水波，开关溅起涟漪，有触感反馈。
// 只在性能较好的设备上启用（不在低电量模式、不少于 6 核、不少于 4GB 内存、没有打开“减弱动态效果”），否则自动改用圆融
ContentView()
    .tesseraShape(.flow)
```

两种外观共用同一套组件。自定义组件可读取 `@Environment(\.tesseraShape)`，用 `isRound`、`radius(tile:round:)`、`groupShape(_:outer:)`、`groupGap`、`pillGap` 跟随外观；流韵的动效可用 `.tesseraFlowRipple`、`.tesseraSplash`、`.tesseraDropWobble`、`.tesseraBeadPop`、`.tesseraTension`；`.tesseraTile(...)` 在圆融外观下自动去掉砖厚和凹槽。

## 组件（v0.3：首批 22 个）

| 类别 | 组件 |
|---|---|
| 操作 | `TesseraButtonStyle`（`.buttonStyle(.tessera())`）、`TesseraIconButton` |
| 输入 | `TesseraTextField`、`TesseraSearchField`、`TesseraSelect`、`TesseraCheckboxStyle`、`TesseraRadioGroup`、`TesseraSwitchStyle`、`TesseraSegmentedControl` |
| 导航 | `TesseraNavBar`、`TesseraTabBar`、`TesseraTabs`、`TesseraMenu` |
| 数据展示 | `TesseraCard`、`TesseraList` / `TesseraListItem`、`TesseraAvatar` / `TesseraAvatarGroup`、`TesseraBadge`、`TesseraTag` |
| 反馈 | `.tesseraToast`、`.tesseraDialog`、`.tesseraSheet`、`TesseraSkeleton` |

在 Xcode 中打开 `Components/Gallery.swift` 的预览，可以查看全部组件。

## 与其他平台的差异

- **圆角**：全部使用系统的连续曲率圆角（`.continuous`、`UnevenRoundedRectangle`）。
- **底部抽屉**：基于系统 `.sheet` 与 `presentationDetents`，拖动、甩动与档位使用系统手势，外观换成 TesseraUI 样式。
- **菜单与下拉选择**：触发按钮使用 TesseraUI 样式，弹出的列表使用系统菜单，以保证读屏、键盘与指针交互。
- **毛玻璃开关**：`.tesseraSwitch(glass: true)` 的滑块使用系统 `.ultraThinMaterial`。
- **中文输入法**：iOS / visionOS 上 `TesseraTextField` 由 UITextField 实现，通过 `markedTextRange` 判断拼音是否在组合中；macOS 上输入法在选定前不更新绑定。
- **自绘导航栏**：使用 `TesseraNavBar` 时请隐藏系统导航栏（`.toolbar(.hidden, for: .navigationBar)`），并保留边缘右滑返回。

## 资源与字体

- 图标：`TesseraIcon(.home)` 或 `Image(tessera: .homeFill)`，共 157 个，位于 `Resources/TesseraIcons.xcassets`（SVG，保留矢量数据，模板渲染）。
- 字体：把 Fraunces（标题，可变字体）和 Instrument Sans（正文）加入 App 并在 Info.plist 的 `UIAppFonts` 注册；标题中文回退到宋体，正文中文回退到苹方。

## 构建

```bash
swift build                                                            # macOS
xcodebuild -scheme TesseraUI -destination 'generic/platform=iOS Simulator' build
```

持续集成在 macOS 机器上执行以上两步。`Generated/` 与 `Resources/TesseraIcons.xcassets` 由 `npm run build` 生成，请勿手动修改。
