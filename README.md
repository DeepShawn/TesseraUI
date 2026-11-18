
<h1 align="center">TesseraUI</h1>

<p align="center">面向 Apple、Android、Windows 与 Web 的跨平台 UI 设计标准库</p>

<p align="center">
  <a href="LICENSE"><img alt="License: MIT" src="https://img.shields.io/badge/license-MIT-0F6B63.svg"></a>
  <img alt="Version" src="https://img.shields.io/badge/version-0.2.0-1F1B16.svg">
  <img alt="WCAG 2.1 AA" src="https://img.shields.io/badge/WCAG%202.1-AA-3D6B1C.svg">
  <img alt="Platforms" src="https://img.shields.io/badge/platforms-Apple%20%7C%20Android%20%7C%20Windows%20%7C%20Web-B7862B.svg">
</p>

---

## 简介

TesseraUI 是一套跨平台 UI 设计标准库，包含设计规范、设计令牌（Design Tokens）、图标库与组件实现。全部视觉参数集中定义在一组 JSON 源文件中，由构建工具导出为 SwiftUI、Jetpack Compose、WinUI 3 与 CSS 的原生格式，从而保证四个平台的界面在颜色、字体、间距、圆角、阴影与动效上保持一致。

库名取自 *tessera*，即马赛克镶嵌所用的小块石片。TesseraUI 以此确立其设计语言：

| 识别要素 | 说明 |
|---|---|
| **拼贴** | 成组的控件由独立的“砖”构成，砖与砖之间以 3px 缝隙分隔，不使用分割线；整组外角大、内角小。 |
| **釉面砖** | 可操作的控件呈凸起状，带有顶部釉光与底部砖厚，按下时下沉；输入类控件呈凹槽状。 |
| **软衬线** | 标题使用圆角衬线字体 Fraunces 与思源宋体，正文使用 Instrument Sans 与各平台系统黑体。 |
| **矿物色** | 以暖调石色为底，孔雀釉色 `#0F6B63` 为唯一操作色，另有五种矿物颜料用于装饰。 |

TesseraUI 提供三种外观，共用全部组件代码，每次更新同时生效，详见[设计规范 0.4 节](docs/spec.md#04-三种外观拼贴圆融与流韵)：

| 外观 | 特点 |
|---|---|
| **拼贴**（默认） | 圆角方砖，成组时留 3px 砖缝；砖有釉光与砖厚，按下时下沉。 |
| **圆融** | 胶囊与大圆角，成组控件合成一整块，没有砖缝和任何分割线；按下是轻轻一捏。 |
| **流韵** | 以圆融为基础，圆角介于方砖与胶囊之间，按下时变成胶囊；水波、涟漪、液面张力等流体动效与触感反馈。只在性能较好的设备上启用，否则自动改用圆融。 |

完整说明见[设计规范](docs/spec.md)。

## 特性

- **单一数据源**：颜色、字体、间距、圆角、阴影、层级、断点与动效均定义于 `tokens/*.json`，一次修改即同步至四个平台。
- **多外观支持**：每个语义颜色均提供浅色、深色以及两套增强对比度取值，默认跟随系统设置；形状可在“拼贴”“圆融”“流韵”三种外观之间切换。
- **无障碍**：284 项对比度检查全部符合 WCAG 2.1 AA；支持键盘操作、读屏软件、减弱动态效果与降低透明度。
- **物理化动效**：位移与缩放统一使用弹簧动画，可随时中断并继承手势速度；纯 CSS 场景提供由数值模拟生成的 `linear()` 弹簧曲线。
- **中文输入法适配**：输入组件在拼音组合输入期间不触发搜索、校验与字数统计，回车亦不提交表单。
- **图标库**：首批 157 个图标（141 个线性、16 个实心），统一 24×24 网格与 1.5px 线宽。
- **零运行时依赖**：构建与测试仅需 Node.js 18 及以上版本。

## 平台支持

| 平台 | 技术栈 | 最低版本 | 当前提供 | 状态 |
|---|---|---|---|---|
| Web | CSS + ES Modules | 主流浏览器最新两个版本 | 设计令牌、84 个组件、运行时脚本、图标 | 可用 |
| Apple | SwiftUI | iOS 17、macOS 14、visionOS 1 | 设计令牌、首批 22 个组件、图标 | 预览，已在持续集成中编译 |
| Android | Jetpack Compose | API 24 | 设计令牌、主题、首批 22 个组件、图标，及 View 系统 XML 资源 | 预览，已编译并通过截图对比 |
| Windows | WinUI 3（Windows App SDK 1.6） | Windows 10 1809 | 设计令牌、系统控件样式、首批 22 个组件、图标 | 预览，已在持续集成中编译 |

## 快速开始

### Web

在页面中引入样式表与运行时脚本：

```html
<link rel="stylesheet" href="platforms/web/dist/tessera.css">

<button class="ts-button ts-primary ts-pressable ts-state">保存</button>

<label class="ts-check">
  <input type="checkbox" role="switch" class="ts-switch">
  Wi-Fi
</label>

<script type="module">
  import Tessera from './platforms/web/dist/tessera.js';

  Tessera.init();                                // 初始化页面上的全部组件与输入法保护
  Tessera.toast('已保存', { type: 'success' });  // 显示提示消息
  Tessera.setTheme('dark');                      // 'light' | 'dark' | 'system'

  const ok = await Tessera.confirm({ title: '删除文件？', danger: true });  // 替代 window.confirm()
</script>
```

外观默认跟随系统，也可通过根元素属性手动指定：

| 属性 | 取值 | 作用 |
|---|---|---|
| `data-theme` | `light`、`dark` | 指定浅色或深色主题 |
| `data-contrast` | `more` | 启用增强对比度 |
| `data-material` | `glass` | 启用可选的毛玻璃效果 |
| `data-shape` | `round`、`flow` | 启用圆融外观（胶囊与大圆角，没有砖缝和分割线）或流韵外观（流体动效与触感，建议通过 `Tessera.setShape('flow')` 打开，会先检测设备性能） |
| `data-motion` | `reduce` | 减弱动态效果 |

### Apple

通过 Swift Package Manager 添加 `platforms/apple` 目录：

```swift
import TesseraUI

Button("保存") { }
    .buttonStyle(.tessera(.primary))

Toggle("Wi-Fi", isOn: $wifi)
    .toggleStyle(.tesseraSwitch(glass: true))

TesseraSegmentedControl(["列表", "网格", "看板"], selection: $view)

Text("区块标题")
    .tesseraText(.title2)
    .foregroundStyle(TesseraColor.textPrimary)
```

### Android

将 `platforms/android/tessera` 作为 Android Library 模块引入：

```kotlin
TesseraTheme {
    TesseraButton("保存", onClick = { })
    TesseraSwitch(checked = wifi, onCheckedChange = { wifi = it }, label = "Wi-Fi")
    TesseraSegmentedControl(listOf("列表", "网格", "看板"), selectedIndex = view, onSelect = { view = it })
}
```

### Windows

在 `App.xaml` 中合并资源字典：

```xml
<ResourceDictionary.MergedDictionaries>
    <XamlControlsResources xmlns="using:Microsoft.UI.Xaml.Controls" />
    <ResourceDictionary Source="ms-appx:///TesseraUI/Themes/TesseraTokens.xaml" />
    <ResourceDictionary Source="ms-appx:///TesseraUI/Themes/TesseraTheme.xaml" />
    <ResourceDictionary Source="ms-appx:///TesseraUI/Themes/TesseraButton.xaml" />
    <ResourceDictionary Source="ms-appx:///TesseraUI/Themes/TesseraControls.xaml" />
</ResourceDictionary.MergedDictionaries>
```

```xml
<Button Content="保存" Style="{StaticResource TesseraPrimaryButtonStyle}" />
<ToggleSwitch Header="Wi-Fi" Style="{StaticResource TesseraToggleSwitchStyle}" />
<tessera:TesseraSegmentedControl Items="{x:Bind ViewModes}" Glass="True" />
```

各平台的字体配置与详细用法见对应目录下的 README：[Web](platforms/web/README.md)、[Apple](platforms/apple/README.md)、[Android](platforms/android/README.md)、[Windows](platforms/windows/README.md)。

## 组件

Web 端提供设计规范所列的全部 84 个组件：

| 类别 | 组件 |
|---|---|
| 操作 | Button、IconButton、ButtonGroup、SplitButton、FAB、Link |
| 输入 | TextField、TextArea、PasswordField、SearchField、NumberInput、OTPInput、Select、Autocomplete、TagInput、Checkbox、Radio、Switch、Slider、SegmentedControl、Rating、FileUpload |
| 选择器 | DatePicker、TimePicker、DateRangePicker、WheelPicker、ColorPicker、Calendar |
| 表单 | Form、FormField、FieldGroup |
| 导航 | NavBar、TabBar、Tabs、Sidebar、Breadcrumb、Pagination、Steps、Menu、ContextMenu、CommandPalette、Anchor |
| 数据展示 | Card、List、Table、Tree、Accordion、Avatar、AvatarGroup、Badge、Tag、Tooltip、Popover、DescriptionList、Timeline、Stat、Carousel、ImageViewer、EmptyState、CodeBlock、Kbd |
| 反馈 | Toast、Banner、Dialog、ConfirmDialog、BottomSheet、Drawer、ActionSheet、Progress、Spinner、Skeleton、Result |
| 布局 | Container、Grid、Stack、Divider、ScrollArea、SplitPane、AspectRatio、SafeArea |
| 移动端手势 | SwipeAction、PullToRefresh、InfiniteScroll、IndexBar |

全部组件的示例见 `demo/components.html`，接口说明见 [Web 端文档](platforms/web/README.md)。

Apple、Android、Windows 三端提供首批 22 个组件：Button、IconButton、TextField、SearchField、Select、Checkbox、Radio、Switch、SegmentedControl、NavBar、TabBar、Tabs、Menu、Card、List、Avatar、Badge、Tag、Toast、Dialog、BottomSheet、Skeleton。各端的接口与平台差异见对应目录下的 README，其余组件的实施计划见[设计规范第 8 节](docs/spec.md#8-实施计划)。

## 目录结构

```text
TesseraUI/
├── tokens/                 设计令牌源文件（唯一需要手动编辑的数值来源）
├── icons/                  图标源文件与导出的 SVG
├── platforms/
│   ├── web/                样式（src/css）、运行时核心与组件模块（src/modules）、构建产物（dist）
│   ├── apple/              Swift Package
│   ├── android/            Android Library 模块
│   └── windows/            WinUI 3 资源字典、控件样式与自定义控件
├── scripts/                构建、对比度检查与图标检查脚本
├── tests/                  单元测试与端到端测试（tests/e2e）
├── demo/                   预览页（index.html）与组件总览（components.html）
└── docs/                   设计规范与对比度报告
```

`platforms/*` 中标注“由 scripts/build.mjs 生成”的文件为构建产物，请勿手动修改。

## 开发

环境要求：Node.js 18 或以上版本。

| 命令 | 说明 |
|---|---|
| `npm run build` | 由设计令牌生成四个平台的产物与图标 |
| `npm run check:contrast` | 执行对比度检查，并生成 `docs/contrast-report.md` |
| `npm run check:icons` | 检查图标的画布、线宽、颜色与命名 |
| `npm test` | 依次执行构建、全部检查与单元测试 |
| `npm run test:e2e` | 端到端交互测试（需要 Playwright 与 Chromium，并先启动 `npm run demo`） |
| `npm run demo` | 构建并在 `http://localhost:5173/demo/` 启动预览页 |

修改设计令牌的流程：

1. 编辑 `tokens/*.json`；
2. 运行 `npm test`，确认构建与全部检查通过；
3. 将令牌源文件与生成的产物一并提交。持续集成会校验两者是否一致。

## 文档

- [设计规范](docs/spec.md)：设计语言、令牌定义、动效、深色模式、图标、组件、原生控件重绘、无障碍与实施计划
- [对比度报告](docs/contrast-report.md)：全部 284 项对比度检查的明细
- [更新日志](CHANGELOG.md)

## 版本策略

本项目遵循[语义化版本 2.0.0](https://semver.org/lang/zh-CN/)。在 1.0.0 之前，次版本号的升级可能包含不兼容的变更，相关变更将在更新日志中注明。

## 参与贡献

欢迎通过 Issue 报告问题或提出建议，也欢迎提交 Pull Request。提交前请确认：

- `npm test` 全部通过；
- 新增或修改的组件符合[设计规范](docs/spec.md)中的“组件完成标准”；
- 涉及视觉参数的修改通过设计令牌完成，而非在组件中直接写入数值。

## 第三方字体

TesseraUI 推荐使用以下字体，它们均以 SIL Open Font License 1.1 授权，须由使用方另行获取并遵守其授权条款，本仓库不包含字体文件：

- [Fraunces](https://github.com/undercasetype/Fraunces)
- [Instrument Sans](https://github.com/Instrument/instrument-sans)
- [思源宋体 / Noto Serif CJK](https://github.com/notofonts/noto-cjk)
- [JetBrains Mono](https://github.com/JetBrains/JetBrainsMono)

## 许可协议

本项目以 [MIT 许可协议](LICENSE)发布。

Copyright © 2026 DeepShawn
