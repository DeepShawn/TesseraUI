# 更新日志

本项目的所有重要变更均记录于此文件。格式参考 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/)，版本号遵循[语义化版本](https://semver.org/lang/zh-CN/)。

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

[0.1.0]: https://github.com/DeepShawn/TesseraUI/releases/tag/v0.1.0
