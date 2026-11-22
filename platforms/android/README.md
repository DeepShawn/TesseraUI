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

- 外观：`TesseraTheme(shape = TesseraShape.Flow)` 切换为“流韵”——在圆融基础上，圆角介于方砖与胶囊之间，按下变成胶囊并荡开水波，开关溅起涟漪、滑块像水滴一样晃，开关、复选框、分段控制、标签栏和主要按钮有触感反馈；只在性能较好的设备上启用（不是低内存设备、不少于 6 核、不少于 4GB 内存，且没有打开“移除动画”），否则自动改用圆融，`requireCapableDevice = false` 可跳过设备检测；省电模式在使用中打开时会实时退回圆融。触感用 `rememberFlowHaptic()`，五档 `TesseraHaptic.Selection / Light / Medium / Success / Warning`（Android 11 以下用相近触感代替）。`TesseraShape.Round` 切换为“圆融”——胶囊与大圆角，成组控件合成一整块，没有砖缝和分割线，按下是轻轻一捏；默认 `TesseraShape.Tile`（拼贴）。两种外观共用同一套组件，自定义组件可通过 `isRound`、`shapeOf()`、`groupShape()`、`groupGap` 跟随外观。
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

## 组件（v0.5：表单与输入）

| 类别 | 组件 |
|---|---|
| 操作 | `TesseraButtonGroup`（单选 / 多选）、`TesseraSplitButton`、`TesseraFab`（可带文字、可收起）、`TesseraLink` |
| 文本 | `TesseraTextArea`（自动长高、字数只在选定汉字后计）、`TesseraPasswordField`（显示切换）、`TesseraAutocomplete`（拼音组合中不筛选）、`TesseraTagInput`（回车 / 逗号 / 顿号添加，空框退格删除） |
| 数字 | `TesseraNumberInput`（按住连续调整并加速、全角数字）、`TesseraOtpInput`（整段粘贴自动取数字） |
| 范围 | `TesseraSlider`、`TesseraRangeSlider`（刻度、拖动时数值气泡、读屏可调）、`TesseraRating`、`TesseraFileUpload` |
| 日期 | `TesseraCalendar`（单选 / 范围，选中砖弹簧滑动、换月推入）、`TesseraWheel` / `TesseraWheelPicker`（与 Web 同一套滚轮手感）、`TesseraDatePicker`、`TesseraTimePicker`、`TesseraDateRangePicker`、`TesseraDate`（不依赖 java.time） |
| 颜色 | `TesseraColorPicker`（饱和度 / 明度面板、色相条、HEX、预设颜料色） |
| 表单 | `TesseraForm`、`TesseraFormField`、`TesseraFieldGroup`、`rememberTesseraFormState()`、`TesseraRules`（中文校验提示：必填、手机号、邮箱、长度） |

## 组件（v0.6：数据展示）

| 类别 | 组件 |
|---|---|
| 表格 | `TesseraTable` + `TesseraColumn`：点表头排序（升序 → 降序 → 原顺序；数字按数值、中文按拼音，`tesseraSort` 可单独使用），多选含半选全选，固定首列（横向滚动后出现分隔阴影），分页，`loading` 骨架行；宽度小于 `cardBreakpoint`（默认 560dp）时每行变成一张卡片 |
| 层级 | `TesseraTree`（`TesseraTreeNode`，展开箭头弹簧转动，读屏报“已展开 / 已收起”）、`TesseraAccordion`（`single` 时只开一个） |
| 提示 | `TesseraTooltip`（触屏长按、鼠标悬停 500ms 出现）、`TesseraTooltipBubble`；`TesseraPopover`（放在锚点所在的 Box 里，与菜单相同）、`TesseraPopoverPanel` |
| 展示 | `TesseraDescriptionList`（窄于 360dp 上下排）、`TesseraTimeline`、`TesseraStats` + `TesseraStatItem`（涨跌箭头与读屏说明）、`TesseraKbd("Ctrl", "K")`、`TesseraCodeBlock`（复制后图标变勾、可显示行号） |
| 媒体 | `TesseraCarousel`（系统翻页手势，可露出两侧、自动播放）、`TesseraImageViewer` / `TesseraImageViewerContent`（双指 / 双击缩放，放大后平移，未放大时下滑关闭、左右滑切换） |
| 状态 | `TesseraProgress`（`value = null` 为不确定进度）、`TesseraProgressRing`、`TesseraSpinner`、`TesseraBanner`、`TesseraResult`（成功 / 失败 / 提示 / 404）、`TesseraEmptyState` |

## 组件（v0.6.1：开发者）

| 组件 | 用法 |
|---|---|
| CodeEditor | `TesseraCodeEditor(value, onValueChange, language = "kotlin", diagnostics = listOf(TesseraDiagnostic(8, "…")))`：语法高亮、行号、当前行；实体键盘 Tab / Shift+Tab 缩进，Esc 后 Tab 移出焦点；回车自动缩进（`tesseraIndent` / `tesseraOutdent` / `tesseraAutoIndent` 可单独使用）；输入法组合中不处理 |
| HexView | `TesseraHexView(bytes, highlights = listOf(TesseraHexRange(0, 8, "PNG 签名")), onSelect = { … })`：按行懒加载；点按选中、长按扩展，实体键盘方向键 / Shift；复制按钮；读屏按行朗读 |

文件上传只负责展示：选择文件由应用调用系统选择器（例如 `ActivityResultContracts.GetMultipleContents`），把结果放进 `files`。安卓软键盘读不到大写锁定状态，`TesseraPasswordField(capsLock = …)` 由应用在接实体键盘时传入。

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
gradle :sample:testDebugUnitTest            # 无障碍检查
gradle :sample:assembleDebug                # 示例 App（sample/build/outputs/apk/debug/sample-debug.apk）
```

截图测试使用 [Paparazzi](https://github.com/cashapp/paparazzi)，在 JVM 上渲染，不需要模拟器；每个分区截拼贴 / 圆融 / 流韵 × 浅色 / 深色 / 浅色增强对比度共 9 张，24 个分区共 216 张基准图。组件总览 `samples/TesseraGallery` 同时用于开发预览、截图测试、无障碍检查和示例 App。

- **无障碍检查**（`sample/src/test/.../AccessibilityTest.kt`）：用 Robolectric 在 JVM 上运行，三种外观 × 24 个分区，检查每个可点击、可切换的元素都有读屏名称，触摸区域不小于 44dp（按 Compose 自动扩展后的触摸范围计算）。放在 `sample` 模块是因为 Robolectric 与 Paparazzi 不能在同一模块里运行。
- **交互测试**（`sample/src/test/.../FormInteractionTest.kt`）：数字步进的点按、按住连续调整、上限禁用与全角数字；标签输入的逗号 / 顿号切分与去重；验证码整段粘贴；评分清零；按钮组多选；滑块与滚轮的读屏调整；日历范围选择（先晚后早自动调换）；表单提交后的中文校验提示。
- **交互测试**（`sample/src/test/.../DataInteractionTest.kt`）：表格按数值与拼音排序、第三次点击回到原顺序；全选的半选状态；筛选与分页（筛选后回到第 1 页）；窄屏卡片；树的展开与选中；单开折叠面板；复制代码（不带行号）；轮播前后按钮与两端隐藏；进度、按键、统计数字、横幅的读屏信息。
- **示例 App**（`sample`）：可在真机上切换三种外观和浅色 / 深色，逐个查看首批 22 个组件。持续集成每次都会编译并上传为 `tessera-sample-apk`，下载后直接安装即可做真机验证。

- `TesseraTokens.kt`、`icons/TesseraIcons.kt`、`res/values*/tessera_*.xml` 由 `npm run build` 生成，请勿手动修改。
- 已知限制：Compose 没有连续曲率圆角，目前使用普通圆角；毛玻璃滑块在 Android 上为半透明效果，不做背景模糊。
