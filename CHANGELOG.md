# 更新日志

版本号遵循[语义化版本](https://semver.org/lang/zh-CN/)。

## 0.1.0 —— 预览版（未发布）

### 新增

- 设计规范 `docs/spec.md`，目标平台确定为 Apple、Android、Windows、Web
- token 源文件：原始色板、37 个语义颜色（浅色 / 深色 / 浅色增强对比度 / 深色增强对比度）、字体、间距、尺寸、圆角、阴影、毛玻璃、层级、断点、弹簧与缓动
- 构建脚本：一份 JSON 导出到 CSS、SwiftUI、Jetpack Compose（含 View 系统 XML）、WinUI 3 XAML + C#；纯 CSS 弹簧由数值模拟生成 `linear()` 曲线
- 对比度自动检查（240 项，全部通过 WCAG AA）与图标规范检查
- 44 个图标 + 6 个实心版
- Web：22 个首批组件、弹簧 / 手势运行时、主题交叉淡化、中文输入法保护、原生控件重绘
- Apple / Android / Windows：主题与按钮的参考实现（未编译验证）
- 预览页 `demo/index.html`

### 相对方案稿的调整

- `text.secondary` 浅色 #6E6E73 → #68686D（原值在输入框底上对比度不足）
- 深色 `text.tertiary` #6B6B72 → #68686D（与上一条共用色阶）
- 新增 `accent.control`，深色 #6E6EE3（主色在深色浮层上作为控件色对比度不足 3:1）
- 补齐方案稿中用到但未定义的颜色 token：遮罩、开关轨道、焦点框、选中文字、叠加层、毛玻璃、各类描边
