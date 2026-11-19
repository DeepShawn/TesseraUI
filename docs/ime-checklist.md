# 中文输入法测试清单

对应设计规范第 6.4 节。用真机逐项测试，把结果填进最后的记录表。每次发布前，至少把“必测”一列在每个平台上做一遍。

## 测试对象

| 组件 | Web | Android | Apple | Windows |
|---|---|---|---|---|
| 文本框（单行） | `.ts-input > input` | `TesseraTextField` | `TesseraTextField` | `TextBox` + `TesseraTextBoxStyle` |
| 文本域（多行） | `.ts-input.ts-textarea > textarea` | `TesseraTextField(singleLine = false)` | —（v0.5） | `TextBox` + `AcceptsReturn="True"` |
| 搜索框 | 带搜索图标的 `.ts-input` | `TesseraSearchField` | `TesseraSearchField` | `AutoSuggestBox` + `TesseraSearchBoxStyle` |
| 自动补全 | `Tessera.autocomplete()`（示例：组件总览的城市输入框） | —（v0.5） | —（v0.5） | `AutoSuggestBox` |
| 字数统计、校验 | `onCommittedInput()`（示例：首页的昵称输入框） | `onCommittedValueChange` | `onCommittedChange` | `TextCompositionStarted` / `TextCompositionEnded`（见 Windows README） |
| 验证码格 | `Tessera.otpInput()` | —（v0.5） | —（v0.5） | —（v0.5） |

示例页：Web 用 `demo/index.html` 与 `demo/components.html`，Android 用示例 App（`tessera-sample-apk`），Apple 用 `swift run TesseraSample` 或 Xcode 预览，Windows 用示例 App（`tessera-sample-windows`）。

## 输入法

| 平台 | 必测 | 选测 |
|---|---|---|
| Android | 系统默认输入法（Gboard 拼音）、搜狗输入法：全拼、九宫格 | 讯飞输入法手写、双拼、五笔 |
| iOS / iPadOS | 系统简体拼音：全键盘、九宫格 | 系统手写、五笔、搜狗输入法 |
| macOS | 系统简体拼音 | 双拼、五笔、搜狗输入法 |
| Windows | 微软拼音：全拼 | 微软拼音双拼、微软五笔、搜狗输入法 |
| Web | 以上各系统的输入法 × Chrome、Safari、Edge、Firefox | 安卓 WebView、微信内置浏览器 |

## 检查项

每一项都在“拼音还没选定汉字”的状态下检查，然后选定汉字再检查一次。

| 编号 | 检查项 | 期望 |
|---|---|---|
| I1 | 打拼音时，搜索框 / 自动补全 | 不触发搜索，不弹出、不筛选候选列表；选定汉字后才触发 |
| I2 | 打拼音时，字数统计 | 计数不变；选定汉字后按汉字数更新 |
| I3 | 打拼音时，格式校验（邮箱、必填等） | 不显示错误；选定汉字后（或失焦后）才校验 |
| I4 | 打拼音时按回车 | 输入法把拼音上屏为英文字母或确认候选，**不提交表单、不触发搜索** |
| I5 | 选定汉字后按回车 | 提交 / 搜索（单行）；多行换行 |
| I6 | Safari：选字结束后的补发回车（keyCode 229） | 不提交 |
| I7 | 打拼音时点击清除按钮 | 清空内容，拼音组合同时结束，不残留字母 |
| I8 | 打拼音时 Tab / 点击别处失焦 | 组合按输入法规则结束，失焦后的校验按最终内容进行 |
| I9 | 焦点光晕 | 打拼音、弹出候选窗时光晕保持，不闪烁；候选窗不遮住输入框（手机上页面会上推） |
| I10 | 光标与选区 | 选定汉字后光标在新文字后面；中间插入、删除不跳到末尾 |
| I11 | 手写、语音输入 | 内容直接上屏，按“已选定”处理，触发搜索 / 计数 |
| I12 | 粘贴 | 按“已选定”处理 |
| I13 | 密码框 | 系统按规定切换到英文键盘或禁用输入法，不出现异常 |
| I14 | 三种外观 × 浅色 / 深色 | 以上行为一致，候选窗位置正确（流韵外观按下变形时不影响候选窗定位） |
| I15 | 读屏（TalkBack / VoiceOver / 讲述人）打开时 | 能逐字朗读已选定内容，组合中不重复朗读 |

自动化已覆盖的部分：Web 交互测试用 CDP 模拟拼音组合输入，覆盖 I1（自动补全）、I2（字数统计）（`tests/e2e/components.e2e.cjs`）。其余各项以及原生三端需要真机测试。

## 记录表

复制下表，每个平台 × 输入法一份。结果填“通过”“不通过（说明）”或“不适用”。

| 项目 | 内容 |
|---|---|
| 平台与系统版本 | 例如 Android 15 / 小米 14 |
| 输入法与版本 | 例如 搜狗输入法 12.3，九宫格 |
| TesseraUI 版本 / 提交 | 例如 v0.4.0 / 6f33ca5 |
| 测试人、日期 | |

| 编号 | 文本框 | 文本域 | 搜索框 | 自动补全 | 备注 |
|---|---|---|---|---|---|
| I1 | | | | | |
| I2 | | | | | |
| I3 | | | | | |
| I4 | | | | | |
| I5 | | | | | |
| I6 | | | | | |
| I7 | | | | | |
| I8 | | | | | |
| I9 | | | | | |
| I10 | | | | | |
| I11 | | | | | |
| I12 | | | | | |
| I13 | | | | | |
| I14 | | | | | |
| I15 | | | | | |
