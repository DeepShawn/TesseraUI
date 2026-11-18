# 更新日志

本项目的所有重要变更均记录于此文件。格式参考 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/)，版本号遵循[语义化版本](https://semver.org/lang/zh-CN/)。

## [0.2.0] - 未发布

Web 端补齐设计规范所列的全部组件，共 84 个。

### 新增

- 62 个 Web 组件：
  - 操作：ButtonGroup、SplitButton、FAB、Link
  - 输入：TextArea（自动高度）、PasswordField（显示切换、大写锁定提示）、NumberInput（长按连续调整、全角数字转换）、OTPInput（支持粘贴与短信自动填充）、Autocomplete、TagInput、Slider（单值与范围）、Rating、FileUpload（拖放与进度）
  - 选择器：DatePicker、TimePicker、DateRangePicker、WheelPicker、ColorPicker、Calendar；手机上日期与时间改为底部抽屉中的滚轮
  - 表单：Form、FormField、FieldGroup，校验提示为中文
  - 导航：Sidebar、Breadcrumb、Pagination、Steps、ContextMenu、CommandPalette、Anchor
  - 数据展示：Table（排序、筛选、固定首列）、Tree、Accordion、AvatarGroup、Tooltip、Popover、DescriptionList、Timeline、Stat、Carousel、ImageViewer（缩放、平移、下滑关闭）、EmptyState、CodeBlock、Kbd
  - 反馈：Banner、ConfirmDialog、Drawer、ActionSheet、Progress（线形与环形）、Spinner、Result
  - 布局：Container、Grid、Stack、Divider、ScrollArea、SplitPane、AspectRatio、SafeArea
  - 移动端手势：SwipeAction、PullToRefresh、InfiniteScroll、IndexBar
- 组件总览页 `demo/components.html`。
- 端到端交互测试 `tests/e2e/components.e2e.cjs`（42 项），持续集成中运行。
- 核心工具：浮层定位 `place()`、外部点击检测、按元素去重的初始化，以及弹簧的停止精度设置。

### 变更

- Web 端代码结构：运行时拆分为核心 `core.js` 与 7 个组件模块，样式按类别拆为 8 个源文件，构建时合并；入口 `tessera.js` 的接口保持兼容。
- `Tessera.init()` 现在初始化全部组件，并可对同一范围安全地重复调用。

### 修复

- 组件自身设置了 `display` 时，`hidden` 属性不生效的问题。
- 长代码行撑开外层布局，导致手机页面被缩放的问题。

## [0.1.0] - 2026-11-18

首个公开版本，以 MIT 许可协议发布。

### 新增

**设计规范**

- 发布设计规范 `docs/spec.md`，覆盖设计语言、设计令牌、动效、深色模式、图标、组件、原生控件重绘、无障碍与实施计划。
- 确立四项设计识别要素：拼贴、釉面砖、软衬线、矿物色。

**设计令牌**

- 原始色板：暖调石色、孔雀釉主色、五种矿物颜料与三种状态色。
- 59 个语义颜色，每个均提供浅色、深色、浅色增强对比度、深色增强对比度四套取值。
- 字体、间距、尺寸、圆角、拼贴、阴影、可选毛玻璃、层级、断点与动效令牌。
- 三档弹簧参数（snappy、smooth、bouncy），刚度与阻尼由时长和回弹量自动换算。

**构建与检查**

- 构建脚本：将设计令牌导出为 CSS 自定义属性、SwiftUI、Jetpack Compose（含 View 系统 XML 资源）、WinUI 3 XAML 与 C# 常量；为纯 CSS 场景数值模拟生成 `linear()` 弹簧曲线。
- 对比度检查：284 项，全部符合 WCAG 2.1 AA，并自动生成报告。
- 图标规范检查与单元测试；持续集成校验令牌源文件与构建产物的一致性。

**图标**

- 首批 157 个图标（141 个线性、16 个实心），分为 10 个类别。

**Web**

- 22 个组件：Button、IconButton、TextField、SearchField、Select、Checkbox、Radio、Switch、SegmentedControl、NavBar、TabBar、Tabs、Menu、Card、List、Avatar、Badge、Tag、Toast、Dialog、BottomSheet、Skeleton。
- 运行时：可中断并继承速度的弹簧动画、开关与分段控制的拖动选择、底部抽屉手势与档位、主题交叉淡化、中文输入法保护。
- 可选毛玻璃效果，并遵循系统的“降低透明度”设置。
- 原生控件重绘：滚动条、文字选中色、输入光标、浏览器自动填充底色、数字与搜索输入框的内置按钮。
- 组件预览页 `demo/index.html`。

**Apple / Android / Windows**

- 设计令牌导出、主题与按钮组件的参考实现。

### 已知限制

- Apple、Android、Windows 的代码尚未在 Xcode、Android Studio 与 Visual Studio 中编译验证。
- 原生平台目前仅提供按钮组件。
- Web 端暂未启用连续曲率圆角（`corner-shape`），原因是 Chromium 当前实现的边缘抗锯齿不足。
- 图标暂无 16px 精简版本。

[0.2.0]: https://github.com/DeepShawn/TesseraUI/compare/v0.1.0...v0.2.0
[0.1.0]: https://github.com/DeepShawn/TesseraUI/releases/tag/v0.1.0
