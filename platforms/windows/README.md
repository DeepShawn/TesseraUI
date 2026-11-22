# TesseraUI · Windows（WinUI 3）

Windows App SDK 1.6，.NET 8，最低 Windows 10 1809。

## 引入

把 `platforms/windows/TesseraUI` 作为项目引用加入应用，然后在 `App.xaml` 中合并资源字典（放在 `XamlControlsResources` 之后）：

```xml
<Application.Resources>
    <ResourceDictionary>
        <ResourceDictionary.MergedDictionaries>
            <XamlControlsResources xmlns="using:Microsoft.UI.Xaml.Controls" />
            <ResourceDictionary Source="ms-appx:///TesseraUI/Themes/TesseraTokens.xaml" />
            <ResourceDictionary Source="ms-appx:///TesseraUI/Themes/TesseraTheme.xaml" />
            <ResourceDictionary Source="ms-appx:///TesseraUI/Themes/TesseraButton.xaml" />
            <ResourceDictionary Source="ms-appx:///TesseraUI/Themes/TesseraControls.xaml" />
            <!-- 可选：圆融外观（胶囊与大圆角，成组控件合成一整块，没有砖缝和分割线）。不合并即为拼贴 -->
            <ResourceDictionary Source="ms-appx:///TesseraUI/Themes/TesseraShapeRound.xaml" />
            <!-- 或者：流韵外观（方圆之间的圆角，按下变成胶囊）。与圆融二选一；建议先用 TesseraResources.FlowCapable 判断设备 -->
            <!-- <ResourceDictionary Source="ms-appx:///TesseraUI/Themes/TesseraShapeFlow.xaml" /> -->
        </ResourceDictionary.MergedDictionaries>
    </ResourceDictionary>
</Application.Resources>
```

两种外观共用同一套样式和控件：样式里随外观变化的圆角、描边、砖厚、按压都用 `ThemeResource` 引用，`TesseraShapeRound.xaml` 放在最后合并即可整体覆盖；C# 控件通过 `TesseraResources.Shape`、`IsRound`、`IsFlow` 判断外观；`TesseraResources.FlowCapable` 判断设备是否适合流韵（系统“动画效果”打开、不少于 6 核、不少于 4GB 内存）。

| 文件 | 内容 |
|---|---|
| `Themes/TesseraTokens.xaml` | 颜色（浅色 / 深色 / 对比度主题）、圆角、间距、尺寸、字体、文字样式。生成文件 |
| `Themes/TesseraTheme.xaml` | 系统控件的轻量样式：把强调色、控件圆角、输入框 / 下拉 / 菜单 / 对话框 / 列表 / 导航等的画刷换成 TesseraUI。合并后，未指定样式的系统控件也会接近 TesseraUI 外观。生成文件 |
| `Themes/TesseraButton.xaml` | 按钮：主要、次要、文字、危险 |
| `Themes/TesseraControls.xaml` | 其余控件的样式与模板 |
| `Themes/TesseraShapeRound.xaml` | 圆融外观：覆盖圆角、砖缝、砖厚、按压等数值。可选，生成文件 |
| `Themes/TesseraShapeFlow.xaml` | 流韵外观：以圆融为基础，圆角介于方砖与胶囊之间。可选，生成文件。水波、开关涟漪与滑块水滴形变、水珠弹跳、按压变胶囊、菜单浮现由 `TesseraFlow.Ripple` / `Splash` / `PressMorph` / `Surface` 附加属性实现（TesseraUI 样式已默认挂上）；浮现不做模糊（WinUI 的元素模糊需要 Win2D，为了不增加依赖改为由淡到实、轻微放大上浮）；WinUI 没有通用触感接口 |
| `Controls/*.cs` | 系统中没有对应控件、或系统控件无法改成 TesseraUI 交互的自定义控件 |
| `Generated/*.g.cs` | 动效常量、层级、图标路径数据。生成文件 |

## 首批 22 个组件

| 组件 | 用法 |
|---|---|
| Button | `<Button Style="{StaticResource TesseraPrimaryButtonStyle}" />`，另有 `Secondary`、`Text`、`Danger` |
| IconButton | `<Button Style="{StaticResource TesseraIconButtonStyle}"><tessera:TesseraIcon Icon="more" Size="20" /></Button>`，另有 `TesseraFilledIconButtonStyle` |
| TextField | `<TextBox Style="{StaticResource TesseraTextBoxStyle}" Header="邮箱" />`；密码用 `PasswordBox` + `TesseraPasswordBoxStyle` |
| SearchField | `<AutoSuggestBox Style="{StaticResource TesseraSearchBoxStyle}" QueryIcon="Find" />` |
| Select | `<ComboBox Style="{StaticResource TesseraComboBoxStyle}" />` |
| Checkbox | `<CheckBox Style="{StaticResource TesseraCheckBoxStyle}" />` |
| Radio | `<RadioButtons>` 内放 `<RadioButton Style="{StaticResource TesseraRadioButtonStyle}" />` |
| Switch | `<ToggleSwitch Style="{StaticResource TesseraToggleSwitchStyle}" />`：凹槽轨道 + 方砖滑块，按住拉宽，可拖动 |
| SegmentedControl | `<tessera:TesseraSegmentedControl Items="…" SelectedIndex="{x:Bind …, Mode=TwoWay}" Glass="True" />` |
| NavBar | `<Grid Style="{StaticResource TesseraNavBarStyle}">`，标题用 `TesseraTitle3TextStyle` |
| TabBar / Tabs | 使用系统 `NavigationView`（底部 / 侧边）与 `SelectorBar`（标签页）；颜色与圆角由 `TesseraTheme.xaml` 提供 |
| Menu | `<MenuFlyout MenuFlyoutPresenterStyle="{StaticResource TesseraMenuFlyoutPresenterStyle}">`，菜单项用 `TesseraMenuFlyoutItemStyle` |
| Card | `<Border Style="{StaticResource TesseraCardStyle}">` |
| List | `<Border Style="{StaticResource TesseraListContainerStyle}"><ListView ItemContainerStyle="{StaticResource TesseraListViewItemStyle}" /></Border>`：拼贴每项一块砖、砖缝 3；圆融整个列表是一整块面 |
| Avatar | `<Border Style="{StaticResource TesseraAvatarStyle}"><TextBlock Style="{StaticResource TesseraAvatarTextStyle}" Text="林" /></Border>` |
| Badge | `<Border Style="{StaticResource TesseraBadgeStyle}"><TextBlock Style="{StaticResource TesseraBadgeTextStyle}" Text="3" /></Border>` |
| Tag | `<Border Style="{StaticResource TesseraAccentTagStyle}"><TextBlock Style="{StaticResource TesseraTagTextStyle}" Text="新" /></Border>`，另有 `Success`、`Warning`、`Danger` 及中性 `TesseraTagStyle` |
| Toast | 页面根 Grid 最上层放 `<tessera:TesseraToastHost x:Name="Toasts" />`，代码中 `Toasts.Show("已保存", icon: "check")` |
| Dialog | `ContentDialog` + `TesseraContentDialogStyle`：按钮是一组拼贴砖 |
| BottomSheet | 页面根 Grid 最上层放 `<tessera:TesseraSheet Title="筛选" IsOpen="…">`，内容放在 `SheetContent` |
| Skeleton | `<Border Style="{StaticResource TesseraSkeletonStyle}" Width="160" />` |
| Icon | `<tessera:TesseraIcon Icon="search" Size="20" Foreground="…" />`，157 个图标，名称与其他三端一致 |

命名空间：`xmlns:tessera="using:TesseraUI"`。

## 组件（v0.5：表单与输入）

在 `App.xaml` 里多合并一个字典（放在 `TesseraControls.xaml` 之后、外观字典之前）：`<ResourceDictionary Source="ms-appx:///TesseraUI/Themes/TesseraForms.xaml" />`。

| 组件 | 用法 |
|---|---|
| ButtonGroup | `<tessera:TesseraButtonGroup Items="…" Multiple="True" />`，`Selected` 为选中下标，`SelectionChanged` 事件 |
| SplitButton | `<tessera:TesseraSplitButton Text="保存" Click="…"><tessera:TesseraSplitButton.Flyout><MenuFlyout>…</MenuFlyout></…>` |
| FAB | `<Button Style="{StaticResource TesseraFabButtonStyle}">` |
| Link | `<HyperlinkButton Style="{StaticResource TesseraLinkButtonStyle}" />` |
| TextArea | `<TextBox Style="{StaticResource TesseraTextAreaStyle}" />`；字数统计在 `TextCompositionEnded` 后更新 |
| PasswordField | `PasswordBox` + `TesseraPasswordBoxStyle`，`PasswordRevealMode="Peek"`；`tessera:TesseraPassword.CapsLockWarning="True"` 在大写锁定打开时提示 |
| NumberInput | `<tessera:TesseraNumberInput Header="数量" Minimum="1" Maximum="99" />`：按住 − / + 连续调整（RepeatButton），全角数字自动转半角，上下方向键调整 |
| OTPInput | `<tessera:TesseraOtpInput Length="6" Completed="…" />`：整段粘贴自动取出数字 |
| Autocomplete | `<tessera:TesseraAutocomplete />`，`Options` 为候选：拼音组合中不筛选，选定汉字后才筛选 |
| TagInput | `<tessera:TesseraTagInput />`，`Tags` 为标签集合：回车、逗号、顿号、分号添加，空框退格删除 |
| Slider / RangeSlider | `Slider` + `TesseraSliderStyle`；范围用 `<tessera:TesseraRangeSlider Lower="200" Upper="800" Step="100" />`（两个滑块不能交叉，方向键与读屏可调） |
| Rating | 系统 `RatingControl`，颜色由 `TesseraTheme.xaml` 提供（建议 `Height="44"` 保证触摸范围） |
| FileUpload | `<tessera:TesseraFileUpload PickRequested="…" Dropped="…" />`：点击时由应用打开 `FileOpenPicker`，也可拖入文件 |
| Calendar / DateRange | `CalendarView` + `TesseraCalendarViewStyle`；范围用 `<tessera:TesseraDateRangeCalendar />` |
| DatePicker / TimePicker | `CalendarDatePicker` + `TesseraCalendarDatePickerStyle`、`TimePicker` + `TesseraTimePickerStyle`（24 小时制）；滚轮使用系统控件 |
| ColorPicker | 系统 `ColorPicker` + `TesseraColorPickerStyle` |
| Form | `<tessera:TesseraFormField Label="姓名" Required="True" Error="…">`，`TesseraRules.Check(text, TesseraRules.Required("姓名"), TesseraRules.Phone)` 返回中文提示；出错时通过 UI 自动化通知朗读 |

WheelPicker 在 Windows 上没有单独的控件：日期、时间的滚轮使用系统 `DatePicker` / `TimePicker` 的弹出层。

## 组件（v0.6：数据展示）

在 `App.xaml` 里再合并一个字典（放在 `TesseraForms.xaml` 之后、外观字典之前）：`<ResourceDictionary Source="ms-appx:///TesseraUI/Themes/TesseraData.xaml" />`。

| 组件 | 用法 |
|---|---|
| Table | `<tessera:TesseraTable />`，代码里添加 `Columns`（`TesseraTableColumn`：`Header`、`Width`、`Sortable`、`Numeric`、`Text`、`Cell`）并设置 `Rows`、`Key`；`Selectable`、`PageSize`、`Filter`、`Loading`、`Layout`（`Automatic` 时宽度小于 560 每行一张卡片）。排序：数字按数值、中文按拼音（`TesseraTable.Sort` 可单独使用） |
| Tree | 系统 `TreeView` + `ItemContainerStyle="{StaticResource TesseraTreeViewItemStyle}"`（每行至少 44 高） |
| Accordion | `<tessera:TesseraAccordion Single="True" />`，`Add(标题, 内容, expanded)`；每项是系统 `Expander` |
| Tooltip | `ToolTipService.ToolTip` + `TesseraToolTipStyle`（反色小砖）；静态展示用 `TesseraTooltipBubble` |
| Popover | `Flyout` + `FlyoutPresenterStyle="{StaticResource TesseraFlyoutPresenterStyle}"`；静态面板 `TesseraPopoverPanel` |
| DescriptionList / Timeline / Stats | `TesseraDescriptionList.Add(标签, 值)`、`TesseraTimeline.Add(标题, 时间, 正文, tone)`、`TesseraStats.Add(标签, 值, 单位, 涨跌, trend)` |
| Kbd / CodeBlock | `<tessera:TesseraKbd Keys="Ctrl+K" />`、`<tessera:TesseraCodeBlock Language="csharp" LineNumbers="True" />`（复制到剪贴板，图标变勾） |
| Carousel | `<tessera:TesseraCarousel />`，`Add(内容)`；系统 `FlipView` 加上前后按钮和圆点，`AutoplaySeconds` 自动播放（悬停、聚焦时暂停） |
| ImageViewer | `TesseraImageViewer.Show(XamlRoot, 图片, 说明)` 全屏打开：滚轮 / 双指 / 双击缩放，放大后拖动平移，未放大时下滑关闭、左右滑切换；Esc 关闭，← → 切换，+ − 缩放 |
| Progress / Spinner | `TesseraProgress`（`Value = null` 为不确定进度）、`TesseraProgressRing`、`TesseraSpinner`（四块小砖轮流亮） |
| Banner / Result / EmptyState | `TesseraBanner`（`Tone`、`Dismissible`、`Actions`）、`TesseraResult`（`Status`：success / error / info / notfound，图标弹簧落下）、`TesseraEmptyState` |

轮播在 Windows 上不支持露出两侧（`FlipView` 每次只显示一页）。

## 交互与动效

- **分段控件**：点选或从任意一格按下后左右拖动，滑块跟手，松手吸附到最近的一格（snappy 弹簧）；左右方向键、Home、End 切换。读屏报告为“分段控件”，值为当前选中项。
- **开关**：沿用 `ToggleSwitch` 自带的拖动与键盘；Storyboard 没有弹簧，松手动画用 `BackEase` 近似 snappy。
- **提示消息**：从顶部落下（bouncy），停留 3 秒（带操作 5 秒），可向上滑走；出现时通过 UI 自动化通知朗读。
- **底部抽屉**：从底部升起（smooth），拖动顶部把手在半高 / 全高两档之间吸附，向下甩或拖过半高的一半即关闭；Esc 关闭，Tab 焦点留在面板内。宽窗口上限宽 560 居中。
- **中文输入法**：`TextBox` 的 `TextChanged` 在拼音组合中也会触发。搜索、校验等应在组合结束后再做：

  ```csharp
  var composing = false;
  box.TextCompositionStarted += (_, _) => composing = true;
  box.TextCompositionEnded += (_, _) => { composing = false; Search(box.Text); };
  box.TextChanged += (_, _) => { if (!composing) Search(box.Text); };
  ```
- 弹簧使用 Composition API 的 `SpringVector3NaturalMotionAnimation`，参数取 `TesseraMotion.Snappy / Smooth / Bouncy`。
- 系统“动画效果”关闭时（设置 → 辅助功能 → 视觉效果），所有弹簧改为瞬时。

## 平台差异

- **对比度主题**：Windows 的对比度主题由用户自选配色，所以 `HighContrast` 字典里全部映射到系统色（`SystemColorWindowColor` 等）。这是 Windows 的要求，和其他三端不同。
- **圆角**：WinUI 的 `CornerRadius` 没有连续曲率，使用普通圆角。
- **材质**：毛玻璃在 Windows 上对应 Acrylic（浮层）和 Mica（窗口底）；分段控件的 `Glass` 使用令牌中的半透明色与亮边。
- **字体**：标题需随应用打包 Fraunces 和思源宋体（`TesseraDisplayFontFamily`），不打包时回退微软雅黑；正文用 Instrument Sans + 微软雅黑。
- **代码中取颜色**：自定义控件通过 `TesseraResources.Brush(key)` 读取应用级资源，跟随应用的 `RequestedTheme`；资源字典未合并时返回透明画刷，不会抛异常。

## 验证

持续集成在 `windows-latest` 上用 Visual Studio 的 MSBuild 执行 `msbuild TesseraUI.csproj -restore -p:Configuration=Release -p:Platform=x64`（`dotnet build` 缺少生成 PRI 资源索引的任务，无法编译含 XAML 的 WinUI 库）。

## 示例 App 与截图

`platforms/windows/TesseraSample` 是一个不打包的 WinUI 3 应用（Windows App SDK 随应用自带，解压即可运行）：可切换三种外观和浅色 / 深色，查看首批组件、表单与输入、数据展示三页。流韵在设备不满足要求时显示为圆融。

```powershell
msbuild TesseraSample.csproj -restore -p:Configuration=Release -p:Platform=x64 -p:RuntimeIdentifier=win-x64
TesseraSample.exe --snapshot snapshots   # 截图与无障碍检查，完成后自动退出
```

`--snapshot` 依次切换首批组件、表单、数据展示三页 × 三种外观 × 浅色 / 深色，每种截一张（共 18 张），并检查每个可聚焦元素都有读屏名称、尺寸不小于 40×40（Windows 触控目标的最小建议值），结果写到 `a11y.txt`，有问题时退出码为 1。持续集成每次都会运行，截图上传为 `windows-snapshots`，示例 App 上传为 `tessera-sample-windows`。高对比度主题只能由系统设置打开，应用无法强制切换，所以不截图，需在真机上检查。
