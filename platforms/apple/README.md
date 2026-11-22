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
// 只在性能较好的设备上启用（不在低电量模式、不少于 6 核、不少于 4GB 内存、没有打开“减弱动态效果”），否则自动改用圆融；
// 低电量模式在使用中切换会实时生效。触感：.tesseraHaptic(.selection | .light | .medium | .success | .warning, trigger:)
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

## 组件（v0.5：表单与输入）

| 类别 | 组件 |
|---|---|
| 操作 | `TesseraButtonGroup`、`TesseraSplitButton`、`TesseraFab`、`TesseraLink` |
| 文本 | `TesseraTextArea`（iOS 上用 UITextView 判断输入法组合，字数只在选定后计）、`TesseraPasswordField`、`TesseraAutocomplete`、`TesseraTagInput`、`TesseraFlowLayout` |
| 数字 | `TesseraNumberInput`（按住连续调整、全角数字、读屏可上下调整）、`TesseraOtpInput`（iOS 短信验证码自动填充 `oneTimeCode`、整段粘贴） |
| 范围 | `TesseraSlider`、`TesseraRangeSlider`、`TesseraRating`、`TesseraFileUpload`（`.fileImporter` 打开系统文件选择器） |
| 日期 | `TesseraCalendar`（单选 / 范围，`matchedGeometryEffect` 让选中砖滑动）、`TesseraWheel` / `TesseraWheelPicker`、`TesseraDatePicker`、`TesseraTimePicker`、`TesseraDateRangePicker` |
| 颜色 | `TesseraColorPicker`（值是 HEX 字符串） |
| 表单 | `TesseraForm`、`TesseraFormField`、`TesseraFieldGroup`、`TesseraFormState`、`TesseraRules` |
| 弹层 | `.tesseraPopover`、`TesseraMenuPanel`、`TesseraMenuItemStyle`：自绘的菜单面板，iPhone 上也保持为弹层 |

v0.5 起 `TesseraMenu` 与 `TesseraSelect` 改为自绘弹层（系统 popover 承载，内容是 TesseraUI 样式），触发按钮是普通按钮，修复了 v0.4 的已知问题（iOS 读屏读不出名称）。组件总览 `TesseraFormsGallery` 展示全部 v0.5 组件。

## 组件（v0.6：数据展示）

| 类别 | 组件 |
|---|---|
| 表格 | `TesseraTable` + `TesseraColumn`：排序（数字按数值、中文按拼音，`tesseraSort` 可单独使用）、多选含半选全选、固定首列、分页、加载骨架；`layout: .automatic` 时紧凑宽度（iPhone 竖屏）每行一张卡片 |
| 层级 | `TesseraTree`（`TesseraTreeNode`）、`TesseraAccordion`（`TesseraAccordionItem`，`single` 时只开一个） |
| 提示 | `.tesseraTooltip(_:)`（触屏长按出现，Mac 上同时是悬停提示 `.help`，读屏作为补充说明）、`TesseraTooltipBubble`；气泡卡片用 `.tesseraPopover` + `TesseraPopoverPanel` |
| 展示 | `TesseraDescriptionList`、`TesseraTimeline`、`TesseraStats`、`TesseraKbd("⌘", "K")`（读屏读作“Command 加 K”）、`TesseraCodeBlock` |
| 媒体 | `TesseraCarousel`（系统分页滚动，可露出两侧、自动播放）、`.tesseraImageViewer(isPresented:images:)` / `TesseraImageViewer`（双指 / 双击缩放、拖动平移、下滑关闭、左右滑切换） |
| 状态 | `TesseraProgress`（`value: nil` 为不确定进度）、`TesseraProgressRing`、`TesseraSpinner`、`TesseraBanner`、`TesseraResult`（`TesseraResultStatus`）、`TesseraEmptyState` |

组件总览 `TesseraDataGallery` 展示全部 v0.6 组件。

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
swift test                                                             # 截图：三种外观 × 浅色 / 深色 / 增强对比度配色，写到 .build/snapshots
swift run TesseraSample                                                # macOS 示例 App：切换外观与浅色 / 深色
cd Examples && xcodegen generate && open TesseraSampleiOS.xcodeproj    # iOS 示例 App 与 UI 测试（需 brew install xcodegen）
```

**iOS 无障碍检查**（`Examples/UITests`，XCUITest，读取的就是 VoiceOver 看到的辅助功能树）：三种外观下每个按钮、开关、文本框、分段控制都要有读屏名称、尺寸不小于 40×40，每种外观截一张图。持续集成在 iOS 模拟器上运行，截图上传为 `ios-snapshots`。这项检查发现并修复了：标签页两个字的项只有 28 宽、搜索框清除按钮只有 16×16、开关整行只有 30 高、圆融 / 流韵输入框读屏范围不足 40。

v0.4 的已知问题（下拉选择与更多菜单没有读屏名称）已在 v0.5 改为自绘弹层后修复，测试里不再有例外。UI 测试覆盖首批组件、表单与输入、数据展示三个总览。

持续集成在 macOS 机器上执行以上步骤，截图上传为 `apple-snapshots`。截图用 ImageRenderer 渲染，有两个限制：系统原生控件（文本输入框、系统菜单按钮）画不出来，会是空白；增强对比度只切换了配色，描边由系统设置决定，需在真机或 Xcode 预览里检查。iOS 示例请在 Xcode 中打开本包，使用 `Gallery.swift` 的预览或把 `TesseraGallery()` 放进自己的 App。`Generated/` 与 `Resources/TesseraIcons.xcassets` 由 `npm run build` 生成，请勿手动修改。
