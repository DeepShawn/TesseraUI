// 视觉回归：记录各组件的计算样式（圆角、颜色、阴影、尺寸、内边距、字号……），与基准文件 visual-baseline.json 比对。
// 覆盖三种外观 × 浅色 / 深色；不比对像素，所以不受各机器中文字体差异影响。
// 外观有意改动后，运行 npm run test:visual -- --update 更新基准，并在提交里说明改了什么。
const { chromium } = require('playwright');
const fs = require('fs');
const path = require('path');
const BASE = process.env.TESSERA_BASE || 'http://localhost:5173';
const FILE = path.join(__dirname, 'visual-baseline.json');
const UPDATE = process.argv.includes('--update');
const OUT = path.join(__dirname, 'screenshots', 'visual');
fs.rmSync(OUT, { recursive: true, force: true });
fs.mkdirSync(OUT, { recursive: true });

// [页面, 名称, 选择器]：每个取第一个匹配的元素
const TARGETS = [
  ['index', '主要按钮', '#components .ts-button.ts-primary'],
  ['index', '次要按钮', '#components .ts-button:not(.ts-primary):not(.ts-text):not(.ts-danger)'],
  ['index', '图标按钮', '#components .ts-icon-button.ts-filled'],
  ['index', '按钮组', '#components .ts-tiles'],
  ['index', '输入框', '#components .ts-input'],
  ['index', '复选框（选中）', '#components .ts-checkbox:checked'],
  ['index', '单选（选中）', '#components .ts-radio:checked'],
  ['index', '开关（开）', '#components .ts-switch:checked'],
  ['index', '开关（关）', '#components .ts-switch:not(:checked)'],
  ['index', '分段控制', '#components .ts-segmented:not(.ts-glass)'],
  ['index', '分段选中块', '#components .ts-segmented:not(.ts-glass) .ts-segmented-indicator'],
  ['index', '标签页指示条', '#components .ts-tabs-indicator'],
  ['index', '卡片', '#components .ts-card'],
  ['index', '列表', '#components .ts-list'],
  ['index', '列表项', '#components .ts-list-item'],
  ['index', '头像', '#components .ts-avatar'],
  ['index', '标签', '#components .ts-tag.ts-accent'],
  ['index', '徽标', '#components .ts-badge:not(.ts-dot)'],
  ['index', '骨架屏', '#components .ts-skeleton:not(.ts-tile)'],
  ['index', '顶部导航栏', 'header.ts-navbar'],
  ['index', '底部标签栏', '.ts-tabbar'],
  ['index', '标签栏选中项', '.ts-tabbar-item[aria-selected="true"]'],
  ['components', '文本域', '.ts-textarea'],
  ['components', '数字步进', '.ts-number'],
  ['components', '验证码格', '.ts-otp-cell'],
  ['components', '滑块轨道', '.ts-slider-track'],
  ['components', '滑块手柄', '.ts-slider-thumb'],
  ['components', '分页', '.ts-pages'],
  ['components', '当前页', '.ts-page[aria-current="page"]'],
  ['components', '步骤', '.ts-step'],
  ['components', '表格外框', '.ts-table-wrap'],
  ['components', '表格单元格', '.ts-table tbody td'],
  ['components', '折叠面板', '.ts-accordion'],
  ['components', '描述列表', '.ts-dl'],
  ['components', '统计', '.ts-stats'],
  ['components', '横幅', '.ts-banner'],
  ['components', '进度条', '.ts-progress'],
  ['components', '代码块', '.ts-code'],
  ['components', '侧边栏项', '.ts-sidebar-item'],
];
const PROPS = ['borderTopLeftRadius', 'borderBottomRightRadius', 'backgroundColor', 'backgroundImage', 'color', 'boxShadow', 'height', 'paddingLeft', 'paddingTop', 'columnGap', 'rowGap', 'fontSize', 'fontWeight', 'borderTopWidth'];

(async () => {
  const b = await chromium.launch();
  const current = {};
  for (const shape of ['tile', 'round', 'flow']) for (const theme of ['light', 'dark']) {
    const ctx = await b.newContext({ viewport: { width: 1280, height: 900 }, colorScheme: theme, reducedMotion: 'reduce' });
    const p = await ctx.newPage();
    // 不加载网络字体，样式与字体无关
    await p.route(/fonts\.(googleapis|gstatic)\.com/, (r) => r.abort());
    for (const page of ['index', 'components']) {
      await p.goto(`${BASE}/demo/${page}.html${shape === 'tile' ? '' : `?shape=${shape}&force`}`, { waitUntil: 'networkidle' });
      await p.waitForTimeout(600);
      for (const [pg, name, sel] of TARGETS) {
        if (pg !== page) continue;
        const style = await p.evaluate(([sel, props]) => {
          const el = document.querySelector(sel);
          if (!el) return null;
          const s = getComputedStyle(el);
          const o = {};
          // 高度四舍五入；颜色、阴影原样记录
          for (const k of props) o[k] = k === 'height' ? `${Math.round(el.getBoundingClientRect().height)}px` : s[k];
          return o;
        }, [sel, PROPS]);
        current[`${shape}/${theme}/${name}`] = style;
      }
      await p.screenshot({ path: path.join(OUT, `${shape}-${theme}-${page}.png`), fullPage: true });
    }
    await ctx.close();
  }
  await b.close();

  if (UPDATE || !fs.existsSync(FILE)) {
    fs.writeFileSync(FILE, JSON.stringify(current, null, 1) + '\n');
    console.log(`已写入基准：${Object.keys(current).length} 项`);
    return;
  }
  const base = JSON.parse(fs.readFileSync(FILE, 'utf8'));
  const diffs = [];
  for (const [key, style] of Object.entries(current)) {
    const was = base[key];
    if (was === undefined) { diffs.push(`${key}：基准里没有（新增的检查项？运行 --update）`); continue; }
    if (style === null || was === null) { if (style !== was) diffs.push(`${key}：元素${style === null ? '找不到了' : '新出现'}`); continue; }
    // 高度随系统字体的行高略有不同（不同机器上的中文字体），允许 ±2px
    const same = (k) => style[k] === was[k] || (k === 'height' && Math.abs(parseFloat(style[k]) - parseFloat(was[k])) <= 2);
    for (const k of PROPS) if (!same(k)) diffs.push(`${key} · ${k}：${was[k]} → ${style[k]}`);
  }
  for (const d of diffs.slice(0, 60)) console.log('  ✖ ' + d);
  console.log(diffs.length ? `\n${diffs.length} 处样式与基准不同。若是有意修改，运行 npm run test:visual -- --update` : `✔ ${Object.keys(current).length} 项样式与基准一致（截图 ${6 * 2} 张）`);
  process.exit(diffs.length ? 1 : 0);
})();
