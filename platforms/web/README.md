# TesseraUI · Web

TesseraUI 的 Web 实现，由样式表与原生 ES 模块组成，无运行时依赖。

## 文件

| 文件 | 说明 |
|---|---|
| `dist/tessera.css` | 设计令牌（CSS 自定义属性）与全部组件样式 |
| `dist/tokens.css` | 仅包含设计令牌 |
| `dist/tessera.js` | 运行时：弹簧动画、手势、浮层、主题切换、输入法保护 |
| `dist/tokens.mjs`、`dist/tokens.json` | 已解析的设计令牌，供脚本与工具读取 |
| `dist/icons.mjs` | 图标 SVG 字符串与分类表 |
| `src/` | 组件样式与运行时脚本的源文件 |

`dist/` 由 `npm run build` 生成，请勿手动修改。

## 使用

```html
<link rel="stylesheet" href="dist/tessera.css">
<script type="module">
  import Tessera from './dist/tessera.js';
  Tessera.init();
</script>
```

`Tessera.init()` 会为页面上的 `.ts-switch`、`.ts-segmented`、`.ts-tabs` 绑定交互，把 `<i data-icon="名称">` 替换为图标，并启用中文输入法的回车保护。

## 运行时接口

| 接口 | 说明 |
|---|---|
| `init(root?)` | 初始化指定范围内的组件 |
| `setTheme(theme)` | 切换主题：`'light'`、`'dark'`、`'system'` |
| `setContrast(enabled)` | 启用或关闭增强对比度 |
| `setMaterial(enabled)` | 启用或关闭毛玻璃效果 |
| `toast(message, options?)` | 显示提示消息 |
| `openDialog(dialog)` / `closeDialog(dialog)` | 打开或关闭 `<dialog class="ts-dialog">` |
| `menu(trigger, menuElement)` | 绑定下拉菜单 |
| `bottomSheet(element, options?)` | 绑定底部抽屉，返回 `{ open, close }` |
| `onCommittedInput(input, callback)` | 仅在输入法完成组合后回调 |
| `Spring` | 可中断、可继承速度的弹簧动画类 |
| `icon(name, options?)` | 创建图标元素 |

## 浏览器支持

支持 Chrome、Edge、Safari、Firefox 的最新两个版本。`linear()` 缓动、`backdrop-filter` 与 View Transitions 在不支持的浏览器中会自动降级。
