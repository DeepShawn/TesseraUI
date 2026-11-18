# TesseraUI · Windows（WinUI 3）

Windows App SDK 1.6，.NET 8。

```xml
<!-- App.xaml -->
<Application.Resources>
    <ResourceDictionary>
        <ResourceDictionary.MergedDictionaries>
            <XamlControlsResources xmlns="using:Microsoft.UI.Xaml.Controls" />
            <ResourceDictionary Source="ms-appx:///TesseraUI/Themes/TesseraTokens.xaml" />
            <ResourceDictionary Source="ms-appx:///TesseraUI/Themes/TesseraButton.xaml" />
        </ResourceDictionary.MergedDictionaries>
    </ResourceDictionary>
</Application.Resources>

<Button Content="保存" Style="{StaticResource TesseraPrimaryButtonStyle}" />
<TextBlock Text="区块标题" Style="{StaticResource TesseraTitle2TextStyle}"
           Foreground="{ThemeResource TesseraTextPrimaryBrush}" />
```

- `Themes/TesseraTokens.xaml` 和 `Generated/TesseraTokens.g.cs` 由 `npm run build` 生成，不要手改。
- 浅色 / 深色跟随 `RequestedTheme`。Windows 的“对比度主题”由用户自选配色，所以 `HighContrast` 字典里全部映射到系统色（`SystemColorWindowColor` 等），这是 Windows 的硬性要求，和其他三端不同。
- 弹簧：Composition API 的 `SpringVector3NaturalMotionAnimation` 用 `DampingRatio` + `Period`，直接取 `TesseraMotion.Snappy` 等常量；XAML Storyboard 没有弹簧，用 `BackEase` 近似。
- 材质：毛玻璃在 Windows 上对应 Acrylic（浮层）和 Mica（窗口底）。
- 圆角：WinUI 的 `CornerRadius` 没有连续曲率，v0.1 先用普通圆角。
- v0.1 状态：只含 token 和按钮样式的参考实现，**尚未在 Visual Studio 中编译验证**。
