# TesseraUI · Android（Jetpack Compose）

minSdk 24，Compose BOM 2024.12。`tessera/` 是一个 Android library 模块。

```kotlin
TesseraTheme {
    TesseraButton("保存", onClick = { })
    BasicText("区块标题", style = TesseraType.title2.copy(color = Tessera.colors.textPrimary))
}

val offset by animateFloatAsState(target, animationSpec = TesseraMotion.smooth())
```

- `TesseraTokens.kt` 和 `res/values*/tessera_*.xml` 由 `npm run build` 生成，不要手改；XML 资源给传统 View 系统使用。
- 深色模式跟随系统；Android 14+ 的“对比度”设置为中或高时使用增强对比度色板。
- 字体：Plus Jakarta Sans 放进 `res/font/`，中文由系统回退到 Noto Sans CJK（即思源黑体）。
- 返回手势：自绘导航栏时需接入 Android 13+ 的预见式返回（`PredictiveBackHandler`）。
- v0.1 状态：只含 token、主题和按钮的参考实现，**尚未在 Android Studio 中编译验证**。
