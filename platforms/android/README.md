# TesseraUI · Android（Jetpack Compose）

minSdk 24，compileSdk 35，Compose BOM 2024.12.01。`tessera/` 是一个 Android Library 模块，只依赖 Compose Foundation，不依赖 Material。

## 使用

```kotlin
TesseraTheme(fonts = TesseraFonts(display = fraunces, text = instrumentSans)) {
    var on by remember { mutableStateOf(true) }
    TesseraButton("保存", onClick = { })
    TesseraSwitch(on, { on = it }, label = "Wi-Fi")
    TesseraText("区块标题", style = Tessera.type.title2)
}
```

- 外观：`TesseraTheme(shape = TesseraShape.Flow)` 切换为“流韵”——在圆融基础上，圆角介于方砖与胶囊之间，按下变成胶囊并荡开水波，开关溅起涟漪、滑块像水滴一样晃，开关、复选框、分段控制、标签栏和主要按钮有触感反馈；只在性能较好的设备上启用（不是低内存设备、不少于 6 核、不少于 4GB 内存，且没有打开“移除动画”），否则自动改用圆融，`requireCapableDevice = false` 可跳过设备检测。`TesseraShape.Round` 切换为“圆融”——胶囊与大圆角，成组控件合成一整块，没有砖缝和分割线，按下是轻轻一捏；默认 `TesseraShape.Tile`（拼贴）。两种外观共用同一套组件，自定义组件可通过 `isRound`、`shapeOf()`、`groupShape()`、`groupGap` 跟随外观。
- 主题默认跟随系统的深色模式、Android 14+ 的“对比度”设置和“移除动画”设置。
- 动效统一通过 `motion(TesseraMotion.snappy())` 取得；系统“移除动画”时自动改为 150ms 过渡。
- 字体：把 Fraunces（标题）和 Instrument Sans（正文）放进 `res/font/`，通过 `TesseraFonts` 传入；未传入时标题回退系统衬线体（中文为 Noto Serif CJK），正文回退系统无衬线体。

## 组件（v0.3：首批 22 个）

| 类别 | 组件 |
|---|---|
| 操作 | `TesseraButton`、`TesseraIconButton` |
| 输入 | `TesseraTextField`、`TesseraSearchField`、`TesseraSelect`、`TesseraCheckbox`、`TesseraRadio`、`TesseraSwitch`、`TesseraSegmentedControl` |
| 导航 | `TesseraNavBar`、`TesseraTabBar`、`TesseraTabs`、`TesseraMenu` |
| 数据展示 | `TesseraCard`、`TesseraList` / `TesseraListItem`、`TesseraAvatar` / `TesseraAvatarGroup`、`TesseraBadge`、`TesseraTag` |
| 反馈 | `TesseraToastHost` / `TesseraToastHostState`、`TesseraDialog`、`TesseraBottomSheet`、`TesseraSkeleton` |

设计细节与 Web 端一致：
- 拼贴：`tileShape(position, outer)` 与 `tilePosition(index, count)` 生成外角大、内角小的形状。
- 釉面砖：`Modifier.raisedSolid()`、`raisedSoft()`、`sunken()`、`pressSink()`。
- 开关和分段控制都可以拖动选择；底部抽屉可以拖动、甩动，停在多个档位。
- 输入框的 `onCommittedValueChange` 只在输入法完成组合后回调，用于搜索、校验、字数统计。

图标：`TesseraIcon(TesseraIcons.Home, contentDescription = "首页")`，共 157 个，由 `npm run build` 从统一的图标源生成。

## 构建与测试

```bash
echo "sdk.dir=$ANDROID_HOME" > local.properties
gradle :tessera:compileDebugKotlin          # 编译
gradle :tessera:recordPaparazziDebug        # 录制截图基准图（tessera/src/test/snapshots）
gradle :tessera:verifyPaparazziDebug        # 截图对比
```

截图测试使用 [Paparazzi](https://github.com/cashapp/paparazzi)，在 JVM 上渲染，不需要模拟器；每个分区截拼贴 / 圆融 / 流韵 × 浅色 / 深色共 6 张，共 36 张基准图。组件总览 `samples/TesseraGallery` 同时用于开发预览和截图测试。

- `TesseraTokens.kt`、`icons/TesseraIcons.kt`、`res/values*/tessera_*.xml` 由 `npm run build` 生成，请勿手动修改。
- 已知限制：Compose 没有连续曲率圆角，目前使用普通圆角；毛玻璃滑块在 Android 上为半透明效果，不做背景模糊。
