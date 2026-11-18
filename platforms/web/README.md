# TesseraUI · Web

TesseraUI 的 Web 实现，由样式表与原生 ES 模块组成，无运行时依赖。提供设计规范所列的全部 84 个组件。

## 文件

| 文件 | 说明 |
|---|---|
| `dist/tessera.css` | 设计令牌（CSS 自定义属性）与全部组件样式 |
| `dist/tokens.css` | 仅包含设计令牌 |
| `dist/tessera.js` | 入口：汇总核心与全部组件模块，提供 `init()` |
| `dist/core.js` | 核心：弹簧动画、手势工具、浮层定位、主题切换、输入法保护，以及 v0.1 的组件交互 |
| `dist/modules/*.js` | 组件模块：`forms`、`pickers`、`navigation`、`data`、`feedback`、`layout`、`gestures` |
| `dist/tokens.mjs`、`dist/tokens.json` | 已解析的设计令牌，供脚本与工具读取 |
| `dist/icons.mjs` | 图标 SVG 字符串与分类表 |
| `src/css/*.css` | 组件样式源文件，构建时按文件名顺序合并 |
| `src/*.js`、`src/modules/*.js` | 运行时源文件 |

`dist/` 由 `npm run build` 生成，请勿手动修改。

## 使用

```html
<link rel="stylesheet" href="dist/tessera.css">
<script type="module">
  import Tessera from './dist/tessera.js';
  Tessera.init();
</script>
```

`Tessera.init(root?)` 按类名初始化 `root`（默认为整个文档）范围内的全部组件；对同一元素重复调用是安全的，动态插入内容后可再次调用。

只需要部分组件时，可以单独引入模块：

```js
import { slider, datePicker } from './dist/modules/forms.js';
```

## 声明式组件

在 HTML 上写好类名与 `data-*` 属性，`init()` 会自动绑定交互。

| 组件 | 写法 |
|---|---|
| Switch | `<input type="checkbox" role="switch" class="ts-switch">`，加 `.ts-glass` 启用毛玻璃滑块 |
| SegmentedControl | `.ts-segmented` 内放 `<label><input type="radio"><span>…</span></label>` |
| Tabs | `.ts-tabs` 内放 `role="tab"` 的按钮 |
| TextArea | `.ts-input.ts-textarea[data-autosize]` 内放 `<textarea>` |
| PasswordField | `.ts-input.ts-password` 内放 `<input type="password">` |
| NumberInput | `.ts-number` 内放 `<input min max step value>` |
| OTPInput | `.ts-otp[data-length="6"]`，输满时派发 `complete` 事件 |
| Autocomplete | `.ts-autocomplete[data-options='[...]']` 内放 `.ts-input > input` |
| TagInput | `.ts-input.ts-taginput[data-value="a,b"]` 内放 `<input>` |
| Slider | `.ts-slider[data-min][data-max][data-step][data-value]`；范围滑块的 `data-value` 写成 `"20,80"` |
| Rating | `.ts-rating[data-value][data-max]`，只读加 `data-readonly` |
| FileUpload | `.ts-upload[data-multiple][data-accept]`，派发 `files` 事件 |
| Form | `form.ts-form`，提交时用中文提示标出不合规的表单项 |
| DatePicker / DateRangePicker / TimePicker | `.ts-input.ts-datepicker`、`.ts-daterange`、`.ts-timepicker`，内放 `<input>` |
| ColorPicker | 输入框 `.ts-input.ts-colorfield`，内嵌面板 `.ts-colorpicker-inline` |
| Calendar | `.ts-calendar-inline[data-mode][data-months][data-value]` |
| Sidebar | `.ts-sidebar`，折叠按钮加 `data-sidebar-toggle` |
| Pagination | `.ts-pagination[data-total][data-page]` |
| ContextMenu | 目标元素加 `data-context-menu="菜单 id"` |
| Menu | 触发按钮加 `data-menu="菜单 id"` |
| Anchor | `nav.ts-anchor` 内放 `<a href="#区块 id">` |
| Table | `table.ts-table`，可排序的表头加 `data-sortable`，筛选框 id 写在 `data-filter` |
| Tree | `ul.ts-tree`，每项 `<li><div class="ts-tree-row">…</div><ul>…</ul></li>` |
| Accordion | `.ts-accordion` 内放 `<details>`，加 `data-single` 时同时只展开一项 |
| Tooltip | 任意元素加 `data-tooltip="文字"`；页面上的 `title` 属性会被自动替换 |
| Popover | 触发按钮加 `data-popover="面板 id"` |
| Carousel | `.ts-carousel > .ts-carousel-track > 幻灯片`，`data-peek` 露出两侧，`data-autoplay="毫秒"` 自动播放 |
| ImageViewer | `<img data-viewer="组名">` |
| CodeBlock | `.ts-code[data-lang][data-lines] > pre` |
| Banner | `.ts-banner[data-tone][data-dismissible]` |
| Drawer | `aside.ts-drawer`，触发按钮加 `data-drawer="抽屉 id"`，关闭按钮加 `data-close` |
| Progress | `.ts-progress[data-value]`、`.ts-progress[data-indeterminate]`、`.ts-ring[data-value]` |
| Spinner | `<span class="ts-spinner"></span>` |
| SplitPane | `.ts-split[data-position][data-persist]`，内放两个子元素 |
| ScrollArea | `.ts-scroll-area` |
| SwipeAction | `.ts-swipe > .ts-swipe-content + .ts-swipe-actions` |
| PullToRefresh | `.ts-ptr > .ts-ptr-content`，派发 `refresh` 事件，处理完调用 `event.detail.done()` |
| IndexBar | 滚动容器加 `data-indexbar`，分组标题加 `data-index="A"` |

## 命令式接口

| 接口 | 说明 |
|---|---|
| `setTheme(theme)` / `setContrast(on)` / `setMaterial(on)` | 切换主题、增强对比度、毛玻璃 |
| `setShape(shape, { force })` | 切换外观：`'tile'`（拼贴，默认）、`'round'`（圆融）或 `'flow'`（流韵），等同于在 `<html>` 上写 `data-shape`。设备不适合流韵时自动改用圆融（`force: true` 可强制），返回实际使用的外观 |
| `flowCapable()` | 设备是否适合流韵：没有打开“减弱动态效果”、没有开省流量、CPU 不少于 6 核、内存不少于 4GB |
| `haptic(kind)` | 触感反馈：`'light'`、`'medium'`、`'success'`。Android 浏览器使用 Vibration API，iOS Safari 18+ 借用系统开关控件的触感，桌面浏览器无效果。须在用户操作的回调中调用 |
| `toast(message, options?)` | 提示消息 |
| `openDialog(dialog)` / `closeDialog(dialog)` | 打开或关闭 `<dialog class="ts-dialog">` |
| `confirm(options)` | 确认框，返回 `Promise<boolean>` |
| `actionSheet(options)` | 操作面板，返回被选中项的索引，取消为 `-1` |
| `bottomSheet(element, options?)` / `drawer(element)` | 底部抽屉、侧边抽屉，返回 `{ open, close }` |
| `commandPalette(options)` | 命令面板，默认绑定 ⌘K / Ctrl+K |
| `wheel(element, { columns, onChange })` | 滚轮选择器 |
| `calendar(element, options)` / `colorPicker(element, options)` | 内嵌日历、颜色选择器 |
| `popover(trigger, panel)` / `contextMenu(target, menu)` / `menu(trigger, menu)` | 浮层绑定 |
| `pagination(element, { total, page, onChange })` | 分页 |
| `infiniteScroll(list, { onLoad })` | 上拉加载，`onLoad` 返回 `false` 表示没有更多 |
| `setProgress(element, value)` | 设置进度，`null` 为不确定进度 |
| `onCommittedInput(input, callback)` | 仅在输入法完成组合后回调 |
| `Spring` | 可中断、可继承速度的弹簧动画类 |
| `place(anchor, panel, options)` | 把浮层放到锚点旁，放不下时自动翻转 |
| `icon(name, options?)` | 创建图标元素 |

组件派发的事件都会冒泡，取值在 `event.detail` 中，例如滑块的 `input` / `change` 事件携带 `detail.value`。

## 浏览器支持

支持 Chrome、Edge、Safari、Firefox 的最新两个版本。`linear()` 缓动、`backdrop-filter`、`scrollend` 事件与 View Transitions 在不支持的浏览器中会自动降级。
