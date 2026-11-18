# TesseraUI 设计规范

> **Tessera**：拼成马赛克的一块块小石片。每一块都被打磨得圆润顺手，拼在一起才是完整的画面——就像一套 token 拼出 Apple、Android、Windows、Web 四端一致的界面。目标：干净、圆润、流畅、好看。

- 版本：v0.1.0
- 日期：2026-11-18
- 目标平台：Apple（iOS / iPadOS / macOS / visionOS）、Android、Windows、Web
- 本文是规范的唯一来源；token 的具体数值以 `tokens/*.json` 为准，本文表格与之保持一致。

---

## 0. 设计原则

1. **干净**：只用一个主色，其余交给中性色；层次靠底色明暗区分，几乎不用分割线；只用两种字重。
2. **圆润**：单行控件全部做成胶囊形，容器用大圆角，圆角采用连续曲率。
3. **流畅**：位移和缩放都用弹簧动画；拖动跟手，动画可以随时打断，松手时继承手指的速度。
4. **一致**：组件只引用语义 token，不写死色值；浅色、深色、增强对比度都靠切换 token 实现。

---

## 1. 基础 Token

Token 分三层：

| 层 | 说明 | 例子 |
|---|---|---|
| 原始色板 | 灰阶和主色的完整色阶，只给语义 token 引用 | `gray-50` … `gray-950` |
| 语义 token | 按用途命名，组件只用这一层 | `bg.surface`、`text.primary` |
| 组件 token | 个别组件的专属值，默认指向语义 token | `button.height.md` |

源文件：`tokens/color.primitives.json`（原始色板）、`tokens/color.semantic.json`（语义颜色）、`tokens/foundation.json`（字体、间距、圆角、阴影、层级、断点）、`tokens/motion.json`（动效）。`npm run build` 导出到四端，见第 8 节。

### 1.1 颜色

表中的百分比都指不透明度，例如“黑 5%”表示 5% 不透明度的黑色；“主色”指 `accent.solid`。

| Token | 用途 | 浅色 | 深色 |
|---|---|---|---|
| `bg.canvas` | 页面底 | #F5F5F7 | #111113 |
| `bg.surface` | 卡片 | #FFFFFF | #1A1A1D |
| `bg.elevated` | 浮层（菜单、弹窗、抽屉） | #FFFFFF | #232327 |
| `bg.elevated-2` | 浮层上的浮层 | #FFFFFF | #2C2C31 |
| `bg.scrim` | 弹窗、抽屉的遮罩 | 黑 32% | 黑 56% |
| `fill.default` | 输入框、次要按钮的底 | 黑 5% | 白 8% |
| `fill.control-off` | 开关关闭时的轨道 | #C7C7CC | #48484E |
| `text.primary` | 主文字 | #1D1D1F | #EDEDF0 |
| `text.secondary` | 次要文字 | #68686D | #A1A1A8 |
| `text.tertiary` | 占位文字、禁用 | #8E8E93 | #68686D |
| `text.on-solid` | 实色底上的文字 | #FFFFFF | #FFFFFF |
| `border.subtle` | 描边、分割线 | 黑 6% | 白 8% |
| `border.strong` | 复选框、单选的外框 | 黑 48% | 白 48% |
| `border.surface` | 卡片、浮层的外描边 | 无 | 白 8% |
| `border.field` | 输入框描边（只在增强对比度下出现） | 无 | 无 |
| `accent.solid` | 主按钮底 | #5B5BD6 | #5B5BD6 |
| `accent.control` | 非文字的实色控件：开关开启、复选框和单选选中 | #5B5BD6 | #6E6EE3 |
| `accent.text` | 链接、选中文字 | #5252CF | #9C9CF0 |
| `focus.ring` | 键盘焦点框 | #5252CF | #9C9CF0 |
| `accent.subtle` | 选中项底色 | 主色 10% | 主色 20% |
| `danger.solid` | 危险按钮底 | #C0292E | #C0292E |
| `danger.text` | 错误文字和图标 | #C0292E | #FF6B6B |
| `success.solid` | 成功实色底 | #146B3A | #146B3A |
| `success.text` | 成功文字和图标 | #146B3A | #4CC38A |
| `warning.solid` | 警告实色底 | #9A5100 | #9A5100 |
| `warning.text` | 警告文字和图标 | #9A5100 | #F5A524 |
| `danger/success/warning.subtle` | 各状态的浅底色 | 对应 solid 10% | 对应 solid 20% |
| `focus.glow` | 输入框聚焦光晕 | 主色 25% | 主色 40% |
| `selection` | 选中文字的底色 | 主色 20% | 主色 20% |
| `state.hover` / `state.pressed` | 一般元素的悬停 / 按下叠加层 | 黑 6% / 黑 10% | 白 6% / 白 10% |
| `state.solid-hover` / `state.solid-pressed` | 实色按钮的悬停 / 按下叠加层 | 白 8% / 黑 8% | 白 8% / 黑 8% |
| `material.glass` | 毛玻璃底色 | 白 72% | #1A1A1D 72% |

**相对方案稿的调整**（由对比度自动检查发现）

- `text.secondary` 浅色由 #6E6E73 改为 #68686D：原值放在页面底上的输入框里只有 4.17:1，未达 4.5:1。深色 `text.tertiary` 随之统一为同一色阶（#68686D，原 #6B6B72，占位色不受对比度约束）。
- 新增 `accent.control`：主色 #5B5BD6 在深色浮层（#232327）上只有 2.92:1，低于非文字控件的 3:1。按钮仍用 `accent.solid`（保证白字 ≥ 4.5:1），开关、复选框、单选改用深色下调亮的 `accent.control`。
- 补齐了方案稿里用到、但没有定义成 token 的颜色：遮罩、开关轨道、焦点框、选中文字、叠加层、毛玻璃、各种描边。

**用色规则**

- 红、绿、橙只用来表示状态，不做装饰。
- 实色底（`*.solid`）在两种模式下用同一个值，保证白字对比度达标；深色模式下调亮的版本只用于文字和图标（`*.text`）。
- 对比度由 `npm run check:contrast` 自动检查，共 240 项全部通过，报告见 [contrast-report.md](contrast-report.md)：所有文字色放在页面底、卡片、浮层、输入框底上都达到 WCAG AA 正文标准（4.5:1）；各状态文字放在自己的浅底色上也达标；实色按钮上的白字在悬停、按下时仍达标；焦点框、实色控件、增强对比度下的描边 ≥ 3:1。占位和禁用色按 WCAG 规定除外。

**增强对比度**

在浅色、深色之外各有一套增强对比度色值（`light-hc`、`dark-hc`），只覆盖需要变化的 token：次要和占位文字各加深一档，`border.subtle` 加强，`border.surface` 和 `border.field` 出现实线描边，开关轨道加深。Windows 例外，见 6.2。

**交互状态（叠加层）**

| 情况 | 悬停 | 按下 |
|---|---|---|
| 一般元素（浅色模式） | 叠黑 6% | 叠黑 10% |
| 一般元素（深色模式） | 叠白 6% | 叠白 10% |
| 实色按钮（两种模式） | 叠白 8% | 叠黑 8% |

禁用状态：整体 40% 不透明度，不响应悬停和按下。

### 1.2 字体

- 英文和数字：Plus Jakarta Sans（SIL OFL 开源授权，可随应用分发）
- 中文：各平台的系统黑体，不打包中文字体

| 平台 | 中文 | 英文回退 |
|---|---|---|
| Apple | 苹方 PingFang SC | 系统字体 SF Pro |
| Android | 思源黑体（系统内置为 Noto Sans CJK） | Roboto |
| Windows | 微软雅黑 Microsoft YaHei UI | Segoe UI Variable |
| Web | 按上面顺序依次回退 | system-ui |

- 只用两种字重：常规 400、半粗 600
- 字号随系统的“文字大小”设置缩放（iOS 动态字体、Android sp、Windows 文本缩放、浏览器缩放）

| Token | 字号 | 行高 | 字重 | 用途 |
|---|---|---|---|---|
| `font.display` | 40 | 48 | 600 | 大标题 |
| `font.title-1` | 32 | 40 | 600 | 页面标题 |
| `font.title-2` | 24 | 32 | 600 | 区块标题 |
| `font.title-3` | 20 | 28 | 600 | 卡片标题 |
| `font.body` | 16 | 26 | 400 | 正文 |
| `font.body-sm` | 14 | 22 | 400 | 次要正文、表单 |
| `font.label` | 14 | 20 | 600 | 按钮、标签页 |
| `font.caption` | 12 | 18 | 400 | 说明文字、标签 |

单位都是 px，各平台 1:1 换算为 pt（Apple）、dp/sp（Android）、有效像素 epx（Windows）。长篇阅读的正文，行高可以放宽到 1.75 倍。

### 1.3 间距与尺寸

- 基础单位 4px：`space.1` = 4、`space.2` = 8、`space.3` = 12、`space.4` = 16、`space.5` = 20、`space.6` = 24、`space.8` = 32、`space.10` = 40、`space.12` = 48、`space.16` = 64
- 常用值：卡片内边距 20–24，手机页边距 16，区块之间 32–48
- 控件高度：`size.sm` 32、`size.md` 40（默认）、`size.lg` 48
- 移动端可点击区域不小于 44×44（看起来可以小，热区要够）

### 1.4 圆角与形状

| Token | 值 | 用在 |
|---|---|---|
| `radius.xs` | 6 | 复选框 |
| `radius.sm` | 8 | 工具提示、小缩略图 |
| `radius.md` | 12 | 图片、代码块 |
| `radius.lg` | 16 | 菜单、气泡卡片、多行文本框 |
| `radius.xl` | 20 | 卡片 |
| `radius.2xl` | 28 | 弹窗、底部抽屉 |
| `radius.full` | 9999 | 按钮、输入框、搜索框、标签、开关、分段控制、头像、提示消息 |

- 单行控件一律做成胶囊形。
- 圆角采用连续曲率（squircle）：直边转进弯角的地方没有折点，看起来更柔和。各平台实现：
  - Apple：原生支持，`RoundedRectangle(cornerRadius:style: .continuous)`
  - Web：支持 `corner-shape: squircle` 的浏览器自动启用（半径放大 1.5 倍以保持观感），其他浏览器回退为普通圆角
  - Android、Windows：v0.1 先用普通圆角；自定义 squircle Shape 计划在 v0.2 提供
- 嵌套规则：里层圆角 = 外层圆角 − 内边距，这样里外两条弧线才平行。

### 1.5 阴影、材质与层级

**阴影（浅色模式）**

| Token | 值 | 用在 |
|---|---|---|
| `shadow.1` | `0 1px 2px rgba(0,0,0,.04), 0 4px 12px rgba(0,0,0,.04)` | 卡片 |
| `shadow.2` | `0 2px 6px rgba(0,0,0,.06), 0 12px 32px rgba(0,0,0,.08)` | 菜单、气泡卡片 |
| `shadow.3` | `0 4px 12px rgba(0,0,0,.08), 0 24px 64px rgba(0,0,0,.12)` | 弹窗、底部抽屉 |

深色模式下，各级阴影的不透明度统一改为 40%，同时给卡片和浮层加 1px `border.subtle` 描边。

**毛玻璃**（导航栏、弹出菜单）

- 浅色：`rgba(255,255,255,.72)`；深色：`rgba(26,26,29,.72)`
- 背景模糊 20px，饱和度 180%
- 各平台对应：Apple 用系统材质（`.regularMaterial`）；Windows 用 Acrylic（浮层）和 Mica（窗口底）；Android 12+ 用 `RenderEffect` 模糊，更低版本退回不透明的 `bg.elevated`；Web 用 `backdrop-filter`

**层级**（从下往上）

| Token | 值 | 用在 |
|---|---|---|
| `z.sticky` | 100 | 吸顶栏 |
| `z.overlay` | 200 | 遮罩 |
| `z.modal` | 300 | 弹窗、抽屉 |
| `z.popover` | 400 | 下拉菜单、气泡卡片 |
| `z.toast` | 500 | 提示消息 |
| `z.tooltip` | 600 | 工具提示 |

下拉菜单放在弹窗之上，弹窗里打开的下拉菜单才不会被盖住。

### 1.6 栅格与断点

| 断点 | 宽度 | 列数 | 列间距 | 页边距 |
|---|---|---|---|---|
| 手机 | < 640px | 4 | 16 | 16 |
| 平板 | 640–1024px | 8 | 24 | 32 |
| 桌面 | > 1024px | 12 | 24 | 内容最宽 1200，居中 |

---

## 2. 动效

### 2.1 规则

- 位置、大小、缩放用弹簧动画；颜色、透明度用“时长 + 曲线”。
- 元素越大，回弹越小；整页级别的动画不回弹。
- 退出比进入快，时长约为进入的 70%。
- 同一时间只让一个主要元素在动，其他元素保持静止或只做淡入。
- 只对 transform 和 opacity 做动画，避免页面重新排版。

### 2.2 弹簧 token（质量都取 1）

| Token | 时长 | 回弹 | 刚度 | 阻尼 | 用在 |
|---|---|---|---|---|---|
| `spring.snappy` | 0.3s | 0.15 | 439 | 36 | 按钮回弹、开关、复选框、指示器前沿、动态图标 |
| `spring.smooth`（默认） | 0.4s | 0 | 247 | 31 | 弹窗、抽屉、菜单、卡片展开、页面转场 |
| `spring.bouncy` | 0.5s | 0.25 | 158 | 19 | 提示消息、拖拽松手归位 |

- 换算公式：刚度 = (2π ÷ 时长)²，阻尼 = 4π × (1 − 回弹) ÷ 时长
- 公式由构建脚本计算，表中数值和导出到各平台的数值一致
- SwiftUI：填“时长 + 回弹”，`Animation.spring(duration:bounce:)`
- Jetpack Compose：填“阻尼比（= 1 − 回弹）+ 刚度”，`spring(dampingRatio, stiffness)`
- Windows：Composition 的 `SpringNaturalMotionAnimation` 填“阻尼比 + 周期（= 时长）”；XAML Storyboard 没有弹簧，用 `BackEase` 近似
- Web（JS）：库自带的 `Spring` 类，填“刚度 + 阻尼”，支持打断和初速度
- Web（纯 CSS）：用构建脚本模拟出的 `linear()` 曲线，`--ts-spring-*`；CSS 曲线的时长是弹簧完全停稳的时间（snappy 460ms、smooth 616ms、bouncy 704ms），比“感知时长”长

### 2.3 时长与曲线 token

| Token | 值 | 用在 |
|---|---|---|
| `duration.instant` | 100ms | 悬停、按下时的变色 |
| `duration.fast` | 150ms | 复选框、输入框聚焦、关闭类动画 |
| `duration.normal` | 250ms | 遮罩淡入、内容切换、主题切换 |
| `duration.slow` | 350ms | 不支持弹簧时，给大元素位移用 |
| `ease.standard` | `cubic-bezier(0.2, 0, 0, 1)` | 元素在屏幕内变化 |
| `ease.enter` | `cubic-bezier(0.05, 0.7, 0.1, 1)` | 进场，先快后慢 |
| `ease.exit` | `cubic-bezier(0.3, 0, 0.8, 0.15)` | 离场，先慢后快 |
| `ease.linear` | `linear` | 只用于进度条和骨架屏 |

### 2.4 组件动效

| 组件 | 动效 |
|---|---|
| 按钮及所有可点击元素 | 按下时 100ms 缩到 0.97，松手用 snappy 弹回 |
| 开关 | 滑块用 snappy 滑动；手指按住时滑块先横向拉宽 20% |
| 复选框 | 150ms 填色，勾号从左往右描出来 |
| 输入框 | 聚焦时 150ms 内底色变成 `bg.surface`，`focus.glow` 光晕从 0 扩到 3px |
| 下拉菜单 | 从触发位置展开（从 0.96 缩放到 1 并淡入），smooth；关闭时只淡出，150ms |
| 弹窗 | 遮罩 250ms 淡入；面板从 0.96 缩放到 1 并淡入，smooth；关闭 150ms，缩到 0.98 并淡出 |
| 底部抽屉 | smooth 滑入；拖动跟手，松手按 2.5 的规则停到档位 |
| 选中指示器（标签栏、分段控制） | 朝移动方向的那条边用 snappy，另一条边用 smooth；两边速度不同，移动时自然拉长，到位后收拢。细条指示器（标签页）用 scaleX 拉长；胶囊指示器（分段控制）改宽度，因为 scaleX 会把两端圆头压扁——它是绝对定位元素，改宽度不会引起页面重排 |
| 提示消息 | 从顶部滑入，同时从 0.9 放大到 1，bouncy；停留 3 秒（带操作按钮的 5 秒）；可以上滑关掉；离场 150ms |
| 卡片展开 | 位置、尺寸、圆角（20→0）一起过渡，smooth；详情内容晚 50ms 淡入；可以下拉手势返回 |
| 列表进场 | 每一项错开 30ms 出现，只错开前 6 项 |
| 骨架屏 | 光带 1.5 秒扫一次，linear 循环 |
| 动态图标 | 菜单↔关闭、播放↔暂停、勾选、收藏做变形动画，snappy |

### 2.5 手势

- 拖动时元素和手指 1:1 同步移动，不加任何缓动。
- 松手时把手指当时的速度交给弹簧作为初速度。“流畅感”主要来自这一步。
- 松手速度超过 500px/s，就朝滑动方向切到下一档；没超过就停到最近的档位。
- 拖出边界后阻力越来越大，像拉橡皮筋。

---

## 3. 深色模式

色值见 1.1，本节是规则。

- 深色模式不是把颜色反过来，而是“越靠前的层越亮”：页面底 → 卡片 → 浮层 → 浮层上的浮层，依次变亮，用亮度代替阴影表现层级。
- 背景不用纯黑，文字不用纯白：看着不刺眼，OLED 屏滚动时也不容易拖影。
- 卡片和浮层加 1px `border.subtle` 描边，把边界勾出来；阴影规则见 1.5。
- 主题三选一：浅色、深色、跟随系统，默认跟随系统。
- 切换主题时整屏做一次 250ms 交叉淡化，不让每个组件各自过渡颜色。
- 图标颜色跟随文字色（currentColor）；插图和品牌图形另外准备深色版本。
- 状态栏文字颜色跟随当前主题；Windows 标题栏随主题切换深浅色。

---

## 4. 图标（全部自己画）

### 4.1 绘制规格

- 画布 24×24，四周各留 2px，图形画在中间 20×20 的区域里
- 线条 1.5px，线头和转角都是圆的；图标里的方形带 2px 小圆角，和组件的圆润感一致
- 基准形状：圆形直径 20，正方形 18×18，竖长方形 16×20，横长方形 20×16。按这些尺寸画，不同形状的图标看起来才一样大
- 尺寸：16、20、24 三档。16px 单独画一版，线条不变细，同时删减细节，免得糊成一团
- 样式：线性（默认）和实心（选中状态）。例如底部标签栏里，选中的图标从线性切换成实心
- 颜色：单色，跟随文字颜色（currentColor）
- 同一类图标的细节量保持一致，例如所有文件类图标的折角大小相同

### 4.2 首批范围（约 150 个）

| 分类 | 示例 |
|---|---|
| 箭头方向 | 上下左右箭头、折角箭头、展开、收起、返回、外部链接 |
| 通用操作 | 添加、删除、编辑、复制、粘贴、分享、下载、上传、刷新、搜索、筛选、排序、收藏、更多 |
| 导航 | 首页、菜单、设置、关闭、全屏 |
| 状态 | 成功、警告、错误、信息、帮助 |
| 文件 | 文件、文件夹、图片、视频、文档、附件 |
| 通讯 | 消息、电话、邮件、通知、评论 |
| 媒体 | 播放、暂停、上一个、下一个、音量、静音、相机、麦克风 |
| 用户 | 用户、用户组、添加用户、登录、退出、锁 |
| 商务 | 购物车、钱包、银行卡、优惠券、订单 |

### 4.3 命名与交付

- 命名：小写英文加短横线，实心版加后缀 `-fill`，例如 `heart` 和 `heart-fill`
- 源文件：SVG
- 导出：Web 用 SVG 组件，Apple 用矢量资源（Asset Catalog，保留矢量数据），Android 用 VectorDrawable，Windows 用 SVG（WinUI 的 `SvgImageSource`）或 PathIcon
- 规范检查：`npm run check:icons` 自动检查画布、线宽、圆头圆角、单色、命名

---

## 5. 组件

### 5.1 通用规范

- 尺寸：sm 32、md 40（默认）、lg 48，适用于按钮、输入框、分段控制等
- 状态：每个组件都要有默认、悬停、按下、聚焦、禁用；需要时再加加载、错误、选中
- 外观：浅色、深色两套都要做
- 输入类控件：`fill.default` 底色、没有边框；聚焦时底色变成 `bg.surface`，外圈出现 3px 的 `focus.glow` 光晕
- 键盘聚焦：2px 实色 `accent.text` 描边，和组件之间留 2px 间隙
- 移动端可点击区域不小于 44×44

### 5.2 组件清单

共约 80 个，标 ★ 的 22 个为首批。

**操作**
按钮 Button ★（主要/次要/文字/危险）、图标按钮 IconButton ★、按钮组 ButtonGroup、分割按钮 SplitButton、悬浮按钮 FAB、链接 Link

**输入**
文本框 TextField ★、多行文本 TextArea、密码框 PasswordField、搜索框 SearchField ★、数字步进器 NumberInput、验证码输入 OTPInput、下拉选择 Select ★、自动补全 Autocomplete、标签输入 TagInput、复选框 Checkbox ★、单选 Radio ★、开关 Switch ★、滑块 Slider（单值/范围）、分段控制 SegmentedControl ★、评分 Rating、文件上传 FileUpload

**选择器**
日期 DatePicker、时间 TimePicker、日期范围 DateRangePicker、滚轮选择器 WheelPicker、颜色选择器 ColorPicker、日历 Calendar

**表单**
表单 Form、表单项 FormField（含标签、帮助文字、错误提示、校验状态）、表单分组 FieldGroup

**导航**
顶部导航栏 NavBar ★、底部标签栏 TabBar ★、标签页 Tabs ★、侧边栏 Sidebar、面包屑 Breadcrumb、分页 Pagination、步骤条 Steps、菜单 Menu ★、右键菜单 ContextMenu、命令面板 CommandPalette、目录锚点 Anchor

**数据展示**
卡片 Card ★、列表 List ★、表格 Table（排序、筛选、固定列）、树 Tree、折叠面板 Accordion、头像 Avatar ★、头像组 AvatarGroup、徽标 Badge ★、标签 Tag ★、工具提示 Tooltip、气泡卡片 Popover、描述列表 DescriptionList、时间线 Timeline、统计数字 Stat、轮播 Carousel、图片预览 ImageViewer、空状态 EmptyState、代码块 CodeBlock、快捷键提示 Kbd

**反馈**
提示消息 Toast ★、横幅警告 Banner、对话框 Dialog ★、确认框 ConfirmDialog、底部抽屉 BottomSheet ★、侧边抽屉 Drawer、操作面板 ActionSheet、进度条 Progress（线形/环形）、加载指示 Spinner（只用在按钮和小区域里）、骨架屏 Skeleton ★、结果页 Result（成功/失败/404）

**布局**
容器 Container、栅格 Grid、堆叠 Stack、分割线 Divider、滚动区域 ScrollArea、可拖拽分栏 SplitPane、宽高比容器 AspectRatio、安全区 SafeArea

**移动端手势**
左滑操作 SwipeAction、下拉刷新 PullToRefresh、上拉加载 InfiniteScroll、字母索引栏 IndexBar

---

## 6. 原生控件重绘

**原则**：外观完全自己画，底层尽量沿用原生元素，保留键盘操作、读屏软件和输入法支持。

### 6.1 Web 端

| 原生控件 | 处理方式 |
|---|---|
| 滚动条 | 6px 宽的细胶囊，平时隐藏，滚动时出现，鼠标移上去变宽到 10px |
| 下拉选择 `<select>` | 触发框用胶囊输入框样式，弹出列表用库里的菜单；手机上改成从底部抽屉里选 |
| 日期、时间输入 | 桌面上弹出日历；手机上在底部抽屉里放滚轮选择器 |
| 复选框、单选、滑块、进度条、文件选择按钮 | 换成库里的组件 |
| 数字输入框的上下小箭头、搜索框自带的 × | 去掉，换成自己的按钮 |
| 选中文字、输入光标 | 选中底色用主色 20%，光标用主色 |
| 自动填充底色 | 浏览器自动填充时出现的黄色或蓝色底，改回输入框底色 |
| 悬停提示（title 属性） | 换成库里的工具提示 |
| `alert()`、`confirm()` | 一律不用，改用库里的对话框 |

### 6.2 App 端

**Apple、Android 通用**

- 开关、分段控制、滚轮选择器、日期和时间选择器
- 警告框、底部操作面板、长按菜单
- 导航栏和底部标签栏。自己画导航栏时，返回手势要一起重新实现：iOS 的边缘右滑返回；Android 13+ 的预见式返回（Predictive Back）
- 下拉刷新的指示器
- 键盘上方的工具条（上一项 / 下一项 / 完成）

**Windows（WinUI 3）**

- 按钮、文本框、下拉框、复选框、开关等：通过重写控件模板（ControlTemplate）换外观，保留 WinUI 控件本身的键盘、UI 自动化（读屏）和输入法支持
- 焦点框：用 WinUI 自带的焦点视觉（FocusVisual），把颜色、粗细、间隙改成 5.1 的规格
- 右键菜单（ContextFlyout / MenuFlyout）、工具提示：改圆角、材质和动效
- 标题栏：扩展内容到标题栏（`ExtendsContentIntoTitleBar`），保留系统的最小化、最大化、关闭按钮
- 滚动条：WinUI 默认已是细条、悬停变宽，只改颜色
- **对比度主题**：Windows 的高对比度由用户自选整套配色，应用必须改用系统色（`SystemColorWindowColor` 等），不能用自己的色值。TesseraUI 的 `HighContrast` 主题字典已把每个语义 token 映射到对应的系统色；此时叠加层、阴影、毛玻璃全部关闭

### 6.3 改不了、也不建议改的

系统键盘、权限申请弹窗、系统分享面板、相册和文件选择器、支付弹窗、系统通知的样式；Windows 上还有用户账户控制（UAC）弹窗、系统文件对话框、通知中心的通知样式、窗口外框。这些由操作系统控制，只需要让自己的界面和它们衔接自然。

### 6.4 中文输入法

自己画的输入框必须正确处理 composition 事件：打拼音、还没选定汉字的这段时间里，不能触发搜索、校验或字数统计，按回车也不能提交表单。否则拼音字母会被当成真正的输入内容。

- Web：库提供 `onCommittedInput()` 和 `guardEnterWhileComposing()`；Safari 在选字结束后会补发一次 keyCode 229 的回车，也要拦掉
- Apple：`UITextInput.markedTextRange` 不为空时视为正在输入
- Android：`TextFieldValue.composition` 不为空时视为正在输入
- Windows：`TextBox.TextCompositionStarted` / `TextCompositionEnded` 事件

---

## 7. 无障碍

- 对比度：正文 ≥ 4.5:1，大字和图标 ≥ 3:1（1.1 的色值已满足）
- 键盘：所有操作都能只用键盘完成，焦点顺序和视觉顺序一致，聚焦样式见 5.1
- 读屏：自己画的控件要补齐角色、名称和状态，例如开关要读出“已开启”或“已关闭”。对应接口：VoiceOver（Apple）、TalkBack（Android）、讲述人 / UI Automation（Windows）、ARIA（Web）
- 减弱动态效果：系统开启后，去掉位移、缩放和视差，统一改成 150ms 淡入淡出，骨架屏停止扫光。对应系统设置：Apple“减弱动态效果”、Android“移除动画”、Windows“动画效果”、Web `prefers-reduced-motion`
- 增强对比度：系统开启后，输入框和卡片补上 1px 描边，次要文字的对比度提高一档
- 触控：可点击区域不小于 44×44

---

## 8. 实施计划

| 阶段 | 内容 | 完成标志 |
|---|---|---|
| 一、基础 ✅ v0.1 | 原始色板、全部语义 token、字体、间距、圆角、阴影、层级、断点、动效 token；写成 JSON 源文件，再导出到四端 | 切换浅色/深色只改 token；对比度自动检查全部通过 |
| 二、首批 | 首批约 150 个图标 + 标 ★ 的 22 个组件 | 每个组件都达到下面的“组件完成标准” |
| 三、原生重绘 | 第 6 节列出的全部原生控件 | 主流浏览器和系统上外观一致；中文输入法测试通过 |
| 四、补全 | 其余约 60 个组件 | 达到“组件完成标准” |
| 五、文档 | 文档站（每个组件的用法、参数、示例、注意事项）+ 设计工具里的组件库 | 新人只看文档就能搭出一个完整页面 |

### v0.1 实际完成情况

- **阶段一全部完成**：token 源文件、四端导出、对比度自动检查（240 项）
- **提前做了一部分阶段二、三（只在 Web 端）**：44 个图标（另有 6 个实心版）；按钮、图标按钮、文本框、搜索框、下拉选择、复选框、单选、开关、分段控制、标签页、卡片、列表、头像、徽标、标签、骨架屏、菜单、对话框、底部抽屉、提示消息、顶部导航栏、底部标签栏，正好是方案里标 ★ 的 22 个首批组件；滚动条、选中文字、自动填充底色等原生控件重绘
- **原生三端只有参考实现**：token、主题和一个按钮组件，用来验证 token 在各平台用得顺手；尚未在 Xcode、Android Studio、Visual Studio 中编译验证
- 这 22 个 Web 组件还没有逐项达到下面的“组件完成标准”（例如截图对比测试还没做），所以阶段二不算完成

### 组件完成标准

- [ ] sm / md / lg 三种尺寸（适用时）
- [ ] 默认、悬停、按下、聚焦、禁用等全部状态
- [ ] 浅色、深色、增强对比度三种外观
- [ ] 动效符合第 2 节，并支持减弱动态效果
- [ ] 能用键盘操作，读屏软件能正确读出
- [ ] 输入类组件通过中文输入法测试
- [ ] 有文档和示例
- [ ] 有自动化测试和截图对比测试

### 版本管理

使用语义化版本号（主版本.次版本.修订号），每次发布都附更新日志。

---

## 9. 待定事项

- [x] 目标平台：Apple、Android、Windows、Web 四端（Flutter 不做）
- [ ] 四端组件的先后顺序（建议：Web 先补齐完成标准，再 Apple、Android、Windows）
- [ ] 主色 #5B5BD6 是否最终确定
- [ ] 首批 150 个图标的具体清单
- [ ] 三处色值调整（见 1.1）是否认可
