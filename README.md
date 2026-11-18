# TesseraUI

一套有自己面貌的 UI 设计标准库，一份 token 同时用于 **Apple、Android、Windows、Web** 四端。

> Tessera：马赛克镶嵌用的小石片。每个控件都是一块打磨圆润的釉面砖，成组时留出砖缝，按下去会微微下沉。

四个识别要素：**拼贴**（成组的砖之间留 3px 缝，不用分割线）、**釉面砖**（凸起的按、凹下的填，按下会沉）、**软衬线**（Fraunces + 思源宋体做标题）、**矿物色**（石灰白底、孔雀釉主色、五种颜料）。详见 [设计规范第 0 节](docs/spec.md#0-设计语言)。

**状态：v0.1.0 预览版，尚未发布。**

## 有什么

| 目录 | 内容 |
|---|---|
| [`docs/spec.md`](docs/spec.md) | 设计规范：颜色、字体、间距、圆角、阴影、动效、深色模式、图标、组件、原生控件重绘、无障碍、实施计划 |
| [`tokens/`](tokens) | token 源文件（JSON）——**唯一需要手改的地方** |
| [`icons/`](icons) | 图标源（`source.mjs`）和导出的 SVG |
| [`platforms/web`](platforms/web) | `tessera.css`、`tessera.js`、`tokens.json/.mjs`、`icons.mjs` |
| [`platforms/apple`](platforms/apple) | Swift Package：SwiftUI token + 按钮样式 |
| [`platforms/android`](platforms/android) | Android library：Compose token、主题、按钮，以及 View 系统的 XML 资源 |
| [`platforms/windows`](platforms/windows) | WinUI 3：XAML 资源字典、按钮样式、C# 动效常量 |
| [`demo/`](demo) | 预览页，用 Web 组件搭成 |

## 常用命令

```bash
npm run build           # tokens/*.json → 四端产物 + 图标
npm run check:contrast  # 对比度检查（WCAG AA，240 项），报告写到 docs/contrast-report.md
npm run check:icons     # 图标规范检查
npm test                # 以上全部 + 单元测试
npm run demo            # 构建并在 http://localhost:5173/demo/ 打开预览页
```

只需要 Node 18+，没有任何第三方依赖。

## Web 快速上手

```html
<link rel="stylesheet" href="platforms/web/dist/tessera.css">
<button class="ts-button ts-primary ts-pressable ts-state">保存</button>
<ul class="ts-list"><li><div class="ts-list-item">一块砖</div></li><li><div class="ts-list-item">又一块砖</div></li></ul>
<label class="ts-check"><input type="checkbox" role="switch" class="ts-switch">Wi-Fi</label>

<script type="module">
  import Tessera from './platforms/web/dist/tessera.js';
  Tessera.init();                 // 图标、分段控制、标签页、输入法保护
  Tessera.toast('已保存', { type: 'success' });
  Tessera.setTheme('dark');       // 'light' | 'dark' | 'system'
</script>
```

浅色、深色、增强对比度默认跟随系统；也可以在 `<html>` 上写 `data-theme="dark"`、`data-contrast="more"` 手动指定。

各原生平台的用法见各自目录下的 README。

## 改 token 的流程

1. 改 `tokens/*.json`
2. `npm test`（会重新生成四端产物，并跑对比度检查）
3. 把 `tokens/` 和生成的产物一起提交——CI 会检查两者是否一致

生成的文件开头都有“请勿手改”的标记。
