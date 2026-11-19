# 更新日志

本项目的所有重要变更均记录于此文件。格式参考 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/)，版本号遵循[语义化版本](https://semver.org/lang/zh-CN/)。

## [未发布]

### 修复

- 滚轮选择器（以及手机上的日期、时间选择）滑动生硬、没有弹簧：原来用浏览器的滚动吸附，松手后是硬停，鼠标滚轮一跳一跳。现在改为自绘：拖动 1:1 跟手、拉出两端有橡皮筋阻力；松手带着手指的速度进入弹簧，落定前轻轻回弹；鼠标滚轮一格一行、触控板跟手后吸附；选项沿圆柱排开，越远越倾斜、越淡；流韵外观下每经过一行轻触一下。月份变了只替换“日”这一列，不再整体重建。点按时不再出现键盘焦点框。
- 日历换选日期时没有动效：原来每点一次整个日历重建、选中色原地瞬间切换。现在不再重建，选中的深色砖用弹簧从旧日期滑到新日期，落下时轻轻压实一下；切换月份时旧月份整块滑出、新月份从另一侧推入（原来只是新月份淡入，旧的直接消失）。日期范围选择也改为原地更新。
- 分页翻页没有动效且有点卡：原来每次翻页把所有按钮删掉重建，按下的动效被打断、焦点跳回当前页。现在按钮复用，当前页底下的深色砖用弹簧滑到新页码；省略号位置变化时，页码平滑滑到新位置，新出现的淡入；焦点留在被点的按钮上。翻页结果与原来完全相同。
- 目录锚点点击后的页面滚动改用弹簧（原来是浏览器固定曲线），滚动中碰一下滚轮或屏幕立即停下。

- Web 流韵外观的圆角一直显示为圆融的胶囊（v0.3.1 起）：流韵的形状数值写在样式层内，被 `tokens.css` 中层外的同名变量压过。现在流韵按设计显示“方圆之间”的圆角（按钮 / 输入框 17、卡片 26、列表行 18）。由新增的视觉回归测试发现。
- 三级文字色（`text.tertiary`）在页面底色上对比度只有 4.06（浅色）/ 3.0（深色），用于时间线时间、选项说明等正文时不达 WCAG AA。现调深为浅色 `#6B655C`、深色 `#9E978B`，并纳入正文对比度检查（共 300 项，全部通过）。
- 增强对比度下，Android、Apple、Windows 的圆融 / 流韵外观没有加回描边（Web 端已正确处理）：输入框、开关轨道、分段控制、卡片、对话框在增强对比度下边界不清。现在四端一致加回 1 像素描边。
- 无障碍：
  - 只读评分不再嵌套按钮，可交互评分的每颗星都有读屏名称。
  - 日历日期改为一组切换按钮（`aria-pressed`），今天标 `aria-current="date"`。
  - 代码块、下拉刷新等可滚动区域可以用键盘聚焦后滚动。
  - Apple（由新增的 iOS UI 测试发现）：标签页两个字的项可点范围只有 28 宽，改为至少 44；搜索框清除按钮只有 16×16，改为 40×40；开关在读屏中改为系统开关语义，范围是整行 44 高；输入框的读屏范围撑满整个框。
  - Android 文本框、搜索框没有读屏名称（标签显示在框外，读屏只会说“编辑框”）：现在以标签（没有标签时用占位文字）作为读屏名称。

### 新增

- Windows 流韵补齐三项：开关滑块水滴形变（`TesseraFlow.Splash`）、按下时圆角变成胶囊并 bouncy 弹回（`TesseraFlow.PressMorph`，成组控件不变形）、菜单像从水下浮上来（`TesseraFlow.Surface`）。浮现不做模糊：WinUI 的元素模糊需要 Win2D，为了不增加依赖，改为由淡到实、轻微放大上浮。

### 新增（测试）

- `npm run test:a11y`：axe-core 无障碍扫描，首页与组件总览 × 三种外观 × 浅色 / 深色 / 增强对比度 × 桌面 / 手机（36 个组合），严重与重要问题必须为 0，每个组合截图。
- `npm run test:visual`：视觉回归，三种外观 × 浅色 / 深色下 39 个组件的计算样式（圆角、颜色、阴影、尺寸、内边距、字号等）与基准比对；不比对像素，不受字体差异影响。
- Android 截图测试增加增强对比度，共 54 张（三种外观 × 浅色 / 深色 / 增强对比度 × 6 个分区）。
- Android 无障碍检查（`gradle :sample:testDebugUnitTest`，Robolectric）：三种外观 × 6 个分区共 174 个可交互元素，检查读屏名称和不小于 44dp 的触摸区域。
- Android 示例 App（`platforms/android/sample`）：可切换外观、主题、对比度，展示首批 22 个组件；持续集成每次编译并上传 `tessera-sample-apk`，可直接安装到手机上评审。
- iOS 示例 App 与 UI 测试（`platforms/apple/Examples`，XcodeGen 生成工程）：XCUITest 在三种外观下检查读屏名称与尺寸，每种截图，CI 上传 `ios-snapshots`。已知问题：下拉选择与更多菜单在 iOS 上没有读屏名称，v0.5 改为自绘弹层。
- Apple 截图测试（`swift test`，ImageRenderer）：三种外观 × 浅色 / 深色 / 增强对比度配色共 9 张，CI 上传 `apple-snapshots`；macOS 示例 App（`swift run TesseraSample`）。
- Windows 示例 App（`platforms/windows/TesseraSample`）：可切换外观与浅色 / 深色；`--snapshot` 模式截图三种外观 × 浅色 / 深色共 6 张，并做无障碍检查（读屏名称、不小于 40×40，78 个可聚焦元素全部通过）。CI 上传 `windows-snapshots` 和可直接运行的 `tessera-sample-windows`。
- 中文输入法测试清单 `docs/ime-checklist.md`：15 项检查、各平台必测输入法与记录表；Web 端新增“组合中的回车、Safari 补发的回车被拦下”自动化测试。
- Android 截图对比失败时，CI 上传差异图 `android-snapshot-diffs`。
- 中文输入法：用 CDP 模拟拼音组合输入，验证组合中不触发自动补全筛选与字数统计、选定汉字后才触发。

## [0.3.2] - 2026-11-18

补齐 v0.3.1 的短板：Windows 端的流韵动效、四端统一的触感分档、流韵的性能保护，以及输入框与滚动条的逐项检查与修复。

### 新增

- Windows 流韵：按下处的水波、开关切换时的涟漪、复选框与单选的水珠弹跳，使用 Composition 动画实现；新增附加属性 `TesseraFlow.Ripple`、`TesseraFlow.Splash`，并已挂在 TesseraUI 的按钮、列表项、开关、复选框、单选样式上。
- 触感分五档，四端统一命名：选择（selection）、轻触（light）、中等（medium）、确认（success）、警告（warning）。
  - Web：`Tessera.haptic(kind)` 返回是否触发，不支持时静默降级。
  - Android：`TesseraHaptic` 新增 `Selection`、`Success`、`Warning`；Android 11 以下用相近的触感代替。
  - Apple：新增 `TesseraHaptic` 与 `.tesseraHaptic(_:trigger:)`。
  - 选择类操作（复选框、单选、分段控制、标签栏、分页）使用 selection，危险按钮使用 warning。
- 流韵性能保护：
  - Web：持续掉帧（连续 2 秒低于 45 帧）、电量不高于 20% 且未充电时，自动退回圆融，并派发 `tessera:flow-degraded` 事件；页面切到后台时暂停帧率监测与指针釉光。
  - Android：省电模式打开时退回圆融，使用中切换会实时生效。
  - Apple：低电量模式切换时实时更新外观。

### 修复

- 多行文本框聚焦时出现双重框：外层光晕之外，内部 `<textarea>` 还画了一个方形焦点框。
- 圆融 / 流韵：数字步进器聚焦时只有中间一段变白；“搜索 + 排序”等合成一整块的组合输入，聚焦光晕只包住其中一段、另一侧是直角。现在整块一起变色，光晕包住整块。
- 下拉列表在上下空间都不够时会盖住输入框（手机上常见）；现在放到空间较大的一侧并压缩高度，内部滚动。
- 下拉列表、选择器面板、气泡卡片、菜单在页面滚动或手机软键盘弹出时不跟随锚点；新增 `follow()`，在滚动、窗口变化与 visualViewport 变化时重新定位。
- 输入框放进弹性布局时，被原生 `<input>` 的固有宽度撑开，窄屏上把容器撑出屏幕。
- 验证码输入在 320px 宽的屏幕上放不下；分页在很窄的屏幕上撑宽页面。
- 预览页在手机上横向溢出。
- 字母索引栏旁边多出一条竖栏：那是列表自身的滚动条。有字母索引的列表现在隐藏滚动条（仍可滑动）。

### 变更

- 隐藏全部滚动条（Web 全局、Apple 预览、Windows 底部抽屉），滚轮、触控板、手指与键盘滚动不受影响；可滚动区域用边缘渐隐、吸附与内容露出一截来提示。

### 新增（测试）

- 测试：交互测试的每一项都自动截图（三种外观各一组），CI 上传为 `e2e-screenshots`；新增输入框聚焦、下拉列表遮挡、手机横向溢出的检查。
- 测试：`npm run test:inputs`，输入框逐项截图审查（14 个输入框 × 三种外观 × 浅色 / 深色 × 桌面 / 手机 × 未聚焦 / 聚焦，共 336 张），并生成对照表。
- 测试：流韵掉帧时自动退回圆融、触感接口静默降级。
- 后续计划写入 `plan/` 目录。

### 已知限制

- 流韵的动效、触感与设备门槛尚未在真机上验证（见 `plan/v0.3.2.md` 第 3 节）。
- Windows 端流韵暂不支持按下时圆角变成胶囊与浮层由模糊到清晰，WinUI 也没有通用的触感接口。

## [0.3.1] - 2026-11-18

新增两种外观：“圆融”与“流韵”，与 v0.3.0 的全部内容对齐。三种外观（拼贴、圆融、流韵）共用同一套组件代码，今后的每次更新同时适用于三种外观。

### 新增

- 圆融外观：控件是胶囊与大圆角，成组控件合成一整块，没有砖缝和任何分割线；去掉砖厚和凹槽，改为整面柔光；按下是轻轻一捏（缩小到 97%）；增强对比度下自动加回描边。
  - **Web**：全部 84 个组件。在 `<html>` 上写 `data-shape="round"`，或调用 `Tessera.setShape('round')`。
  - **Android**：首批 22 个组件。`TesseraTheme(shape = TesseraShape.Round)`。
  - **Apple**：首批 22 个组件。`.tesseraShape(.round)`。
  - **Windows**：首批 22 个组件。在 `App.xaml` 中最后合并 `TesseraShapeRound.xaml`。
- 流韵外观：以圆融为基础（成组控件合成一整块、没有砖缝和分割线），圆角介于方砖与胶囊之间，按下时变成胶囊、松手带回弹地恢复；按下处荡开两圈清透的水波；开关拨动时溅起涟漪、滑块像水滴一样晃，轨道颜色从滑块一侧灌满；复选框、单选像水珠一样弹；分段控制的选中块有液面张力；开关、复选框、单选、分段控制、标签栏与主要按钮有触感反馈。流韵只在性能较好的设备上启用，打开“减弱动态效果”或设备不满足要求时自动改用圆融。
  - **Web**：全部 84 个组件使用流韵的形状，首批 22 个组件及列表、菜单、分页、树、命令面板等常用点按元素有流体动效；浮层由模糊到清晰浮现，鼠标移过控件时釉光跟随指针。`Tessera.setShape('flow')`，或在 `<html>` 上写 `data-shape="flow"`。新增 `Tessera.flowCapable()` 与触感接口 `Tessera.haptic()`（Android 浏览器使用 Vibration API，iOS Safari 18+ 借用系统开关控件的触感）。
  - **Android**：首批 22 个组件。`TesseraTheme(shape = TesseraShape.Flow)`；新增 `requireCapableDevice` 参数。
  - **Apple**：首批 22 个组件。`.tesseraShape(.flow)`；动效使用 `keyframeAnimator`，触感使用 `sensoryFeedback`。
  - **Windows**：首批 22 个组件的形状与按压，以及分段控制的液面张力。在 `App.xaml` 中最后合并 `TesseraShapeFlow.xaml`；`TesseraResources.FlowCapable` 用于判断设备。
- 设计令牌 `shape`：圆融外观的胶囊、卡片 28、面板 36、行 22、菜单 24、间距 8、按压缩放 0.97；流韵外观的控件 17、卡片 26、面板 34、行 18、菜单 22、按压缩放 0.96、水波 760ms、涟漪 640ms，以及启用条件（6 核、4GB 内存），导出到四端。
- 预览页与组件总览加入外观切换（拼贴 / 圆融 / 流韵），支持地址栏参数 `?shape=round`、`?shape=flow`。
- 测试：Web 交互测试在三种外观下各跑一遍（拼贴 49 项、圆融 51 项、流韵 53 项）；Android 截图基准图增至 36 张（拼贴、圆融、流韵各 12 张）。

### 变更

- Windows 按钮与控件模板中随外观变化的圆角、描边、砖厚和按压数值改用 `ThemeResource` 引用。

### 修复

- Web 表格的勾选列实际宽度只有 36px，与固定的第二列之间露出一道缝。

### 已知限制

- 流韵的动效、触感尚未在真机上逐项验证；iOS Safari 的触感依赖系统开关控件，可能随系统版本变化。
- Windows 端的流韵目前只有形状、按压与分段控制的液面张力，水波与开关涟漪尚未实现；WinUI 也没有通用的触感接口。
- Apple 与 Windows 端尚无截图对比测试。

## [0.3.0] - 2026-11-18

Apple、Android、Windows 三端提供首批 22 个原生组件，并在持续集成中编译验证；Web 端表格重做。

### 新增

- 原生三端首批 22 个组件：Button、IconButton、TextField、SearchField、Select、Checkbox、Radio、Switch、SegmentedControl、NavBar、TabBar、Tabs、Menu、Card、List、Avatar、Badge、Tag、Toast、Dialog、BottomSheet、Skeleton。
  - **Android**（Jetpack Compose，只依赖 Foundation）：拼贴形状、釉面砖质感、按下下沉与聚焦光晕的基础修饰符；开关与分段控件可拖动；文本框区分输入法组合中与已提交的内容；跟随系统深色、对比度与“移除动画”设置。
  - **Apple**（SwiftUI，iOS 17 / macOS 14 / visionOS 1）：连续曲率拼贴形状；开关与分段控件可拖动，开关可选系统毛玻璃滑块；iOS 文本框基于 UITextField 判断输入法组合状态；菜单、下拉与底部抽屉使用系统交互，外观换成 TesseraUI。
  - **Windows**（WinUI 3）：系统控件的轻量样式（`TesseraTheme.xaml`，由构建脚本生成）、控件样式与开关模板（`TesseraControls.xaml`），以及 `TesseraIcon`、`TesseraSegmentedControl`、`TesseraToastHost`、`TesseraSheet` 四个自定义控件；弹簧使用 Composition API。
- 原生三端的图标：157 个图标分别导出为 Android ImageVector、Apple Asset Catalog（模板渲染）与 Windows 路径数据，名称与 Web 端一致。
- Android 截图对比测试（Paparazzi），浅色与深色共 12 张基准图。
- 持续集成新增 Android（编译、截图对比）、Apple（macOS 与 iOS 模拟器编译）、Windows（x64 编译）三个任务。
- Web 表格：多选（含 Shift 连选）与批量操作栏、分页、加载状态、横向与纵向滚动时的固定列阴影，窄容器下自动切换为卡片布局。

### 变更

- Web 表格工具栏：搜索框与操作按钮按容器宽度排布；选中行后批量操作栏覆盖在工具栏上方，不再挤压搜索框；窄宽度下按钮只显示图标。
- 构建脚本为 Kotlin 新增 `TesseraOpacity`，为 Windows 生成系统控件轻量样式。

### 已知限制

- 原生三端目前只有首批 22 个组件，其余组件请参考 Web 端或使用系统控件。
- Apple 与 Windows 端尚无截图对比测试，外观需在 Xcode 预览或 Visual Studio 中查看。
- Windows 端 TabBar 与 Tabs 使用系统 `NavigationView` 与 `SelectorBar`，仅调整颜色与圆角。
- 尚无文档站。

## [0.2.0] - 2026-11-18

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

### 已知限制

- Apple、Android、Windows 仍只提供设计令牌与按钮组件，代码尚未经编译验证；原生组件计划在 0.3.0 提供。
- 尚无截图对比测试与文档站。
- 端到端测试已加入持续集成，但尚未在 GitHub Actions 上实际运行。

## [0.1.0] - 2026-11-18

首个公开版本，以 MIT 许可协议发布。由DeepShawn审核通过

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
