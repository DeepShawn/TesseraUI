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
| `Themes/TesseraShapeFlow.xaml` | 流韵外观：以圆融为基础，圆角介于方砖与胶囊之间。可选，生成文件。水波、开关涟漪、水珠弹跳由 `TesseraFlow.Ripple` / `Splash` 附加属性实现（TesseraUI 样式已默认挂上）；按压变胶囊与浮层模糊浮现尚未实现；WinUI 没有通用触感接口 |
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

持续集成在 `windows-latest` 上用 Visual Studio 的 MSBuild 执行 `msbuild TesseraUI.csproj -restore -p:Configuration=Release -p:Platform=x64`（`dotnet build` 缺少生成 PRI 资源索引的任务，无法编译含 XAML 的 WinUI 库）。目前没有截图测试，外观请在 Visual Studio 中运行验证。
