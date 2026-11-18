// 输入框逐项截图审查：14 个输入框 × 三种外观 × 浅色 / 深色 × 桌面 / 手机 × 未聚焦 / 聚焦（共 336 张），
// 并自动检查：聚焦时内部原生控件不画焦点框（不出现双重框）、下拉列表不盖住输入框（滚动后也不盖住）。
// 截图与对照表输出到 tests/e2e/screenshots/inputs/，对照表为 sheet-<桌面 d / 手机 m>-<外观>.png。
// 用法：先启动静态服务器（python3 -m http.server 5173），再运行 npm run test:inputs
const { chromium, devices } = require('playwright');
const fs = require('fs');
const path = require('path');
const OUT = process.argv[2] || path.join(__dirname, 'screenshots', 'inputs');
const BASE = process.env.TESSERA_BASE || 'http://localhost:5173';
fs.rmSync(OUT, { recursive: true, force: true });
fs.mkdirSync(OUT, { recursive: true });
const FIELDS = [
  // [页面, 名称, 要聚焦的元素, 截图范围]
  ['components', '多行文本', '#ta', '#ta'],
  ['components', '密码', '#pw', '#pw'],
  ['components', '数字步进', '.ts-number input', '.ts-number'],
  ['components', '验证码', '#otp input', '#otp'],
  ['components', '自动补全', '#city', '#city'],
  ['components', '标签输入', '#tags', '#tags'],
  ['components', '表单-昵称', '#f1', '#f1'],
  ['components', '日期', '.ts-datepicker[data-value] input:not([type=hidden])', '.ts-datepicker[data-value] input:not([type=hidden])'],
  ['components', '时间', '.ts-timepicker input:not([type=hidden])', '.ts-timepicker input:not([type=hidden])'],
  ['components', '颜色', '.ts-colorfield input:not([type=hidden])', '.ts-colorfield input:not([type=hidden])'],
  ['components', '表格筛选', '#tbl-filter', '#tbl-filter'],
  ['index', '昵称', '#f-name', '#f-name'],
  ['index', '邮箱(错误)', '#f-mail', '#f-mail'],
  ['index', '搜索+排序', 'input[type=search]', 'input[type=search]'],
];

// 把截图拼成对照表：每种外观一张，行 = 输入框，列 = 浅色 / 深色 × 未聚焦 / 聚焦
async function makeSheets(DIR) {
  const files = fs.readdirSync(DIR).filter((f) => f.endsWith('.png') && !f.startsWith('sheet'));

  const b = await chromium.launch();
  const p = await b.newPage({ viewport: { width: 1500, height: 800 } });
  for (const vp of ['d', 'm']) for (const shape of ['tile', 'round', 'flow']) {
    const names = [...new Set(files.filter((f) => f.startsWith(`${vp}-${shape}-light-`) && f.endsWith('-0.png')).map((f) => f.replace(`${vp}-${shape}-light-`, '').replace('-0.png', '')))];
    const cell = (theme, st, n) => { const f = `${vp}-${shape}-${theme}-${n}-${st}.png`; return fs.existsSync(path.join(DIR, f)) ? `<td><img src="${f}"></td>` : '<td>—</td>'; };
    const rows = names.map((n) => `<tr><th>${n}</th>${cell('light', 0, n)}${cell('light', 1, n)}${cell('dark', 0, n)}${cell('dark', 1, n)}</tr>`).join('');
    fs.writeFileSync(path.join(DIR, 'sheet.html'), `<style>body{font:13px sans-serif;margin:8px;background:#888}table{border-collapse:collapse}td,th{border:1px solid #666;padding:4px;vertical-align:top;background:#ccc}th{width:110px;text-align:left}img{max-width:${vp === 'd' ? 330 : 330}px;max-height:150px;display:block}</style>
      <h3>${vp === 'd' ? '桌面' : '手机'} · ${shape}</h3><table><tr><th></th><th>浅色</th><th>浅色·聚焦</th><th>深色</th><th>深色·聚焦</th></tr>${rows}</table>`);
    await p.goto('file://' + path.join(DIR, 'sheet.html'));
    await p.waitForTimeout(300);
    await p.screenshot({ path: path.join(DIR, `sheet-${vp}-${shape}.png`), fullPage: true });
  }
  await b.close();
}

(async () => {
  const b = await chromium.launch();
  const results = [];
  for (const mobile of [false, true]) for (const shape of ['tile', 'round', 'flow']) for (const dark of [false, true]) {
    const ctx = await b.newContext(mobile ? { ...devices['iPhone 13'], colorScheme: dark ? 'dark' : 'light' } : { viewport: { width: 1280, height: 900 }, deviceScaleFactor: 2, colorScheme: dark ? 'dark' : 'light' });
    const p = await ctx.newPage();
    for (const page of ['components', 'index']) {
      await p.goto(`${BASE}/demo/${page}.html${shape === 'tile' ? '' : `?shape=${shape}&force`}`, { waitUntil: 'networkidle' });
      await p.waitForTimeout(700);
      for (const [pg, name, focusSel, shotSel] of FIELDS) {
        if (pg !== page) continue;
        const target = p.locator(shotSel).first();
        // 截图范围：整个输入框（.ts-input / .ts-number / .ts-otp），外扩 10px 以便看到光晕
        const box = await target.evaluate((el) => {
          const host = el.closest('.ts-field') || el.closest('.ts-input, .ts-number, .ts-otp') || el;
          host.scrollIntoView({ block: 'center', behavior: 'instant' });
          const r = (el.closest('.ts-tiles') || el.closest('.ts-input, .ts-number, .ts-otp') || el).getBoundingClientRect();
          return { x: r.x, y: r.y, w: r.width, h: r.height };
        });
        await p.waitForTimeout(150);
        const r = await target.evaluate((el) => { const r = (el.closest('.ts-tiles') || el.closest('.ts-input, .ts-number, .ts-otp') || el).getBoundingClientRect(); return { x: r.x, y: r.y, w: r.width, h: r.height }; });
        const clip = { x: Math.max(0, r.x - 12), y: Math.max(0, r.y - 12), width: Math.min(r.w + 24, mobile ? 390 : 1280), height: r.h + 24 };
        const tag = `${mobile ? 'm' : 'd'}-${shape}-${dark ? 'dark' : 'light'}-${pg}-${name}`;
        await p.waitForTimeout(350); // 等上一个浮层的关闭动画结束
        const r0 = await target.evaluate((el) => { const r = (el.closest('.ts-tiles') || el.closest('.ts-input, .ts-number, .ts-otp') || el).getBoundingClientRect(); return { x: r.x, y: r.y, w: r.width, h: r.height }; });
        await p.screenshot({ path: `${OUT}/${tag}-0.png`, clip: { x: Math.max(0, r0.x - 12), y: Math.max(0, r0.y - 12), width: Math.min(r0.w + 24, mobile ? 390 : 1280), height: r0.h + 24 } });
        await p.locator(focusSel).first().focus();
        await p.waitForTimeout(250);
        const r2 = await target.evaluate((el) => { const r = (el.closest('.ts-tiles') || el.closest('.ts-input, .ts-number, .ts-otp') || el).getBoundingClientRect(); return { x: r.x, y: r.y, w: r.width, h: r.height }; });
        const clip2 = { x: Math.max(0, r2.x - 12), y: Math.max(0, r2.y - 12), width: Math.min(r2.w + 24, mobile ? 390 : 1280), height: r2.h + 24 };
        await p.screenshot({ path: `${OUT}/${tag}-1.png`, clip: clip2 });
        // 聚焦状态下：内部元素不能有自己的 outline（否则出现双重框）
        const inner = await p.locator(focusSel).first().evaluate((el) => { const s = getComputedStyle(el); return s.outlineStyle !== 'none' && parseFloat(s.outlineWidth) > 0 && !!el.closest('.ts-input, .ts-number, .ts-otp'); });
        // 下拉列表（自动补全等）不能盖住输入框；页面滚动后仍然贴着输入框
        const overlap = async () => p.evaluate((sel) => {
          const input = document.querySelector(sel); const box = input.closest('.ts-input') || input;
          const id = input.getAttribute('aria-controls'); const list = id && document.getElementById(id);
          if (!list || list.hidden) return null;
          const a = box.getBoundingClientRect(), b = list.getBoundingClientRect();
          const covers = b.top < a.bottom - 1 && b.bottom > a.top + 1;
          const gap = b.top >= a.bottom ? b.top - a.bottom : a.top - b.bottom;
          return { covers, gap: Math.round(gap) };
        }, focusSel);
        let list = await overlap();
        let listAfterScroll = null;
        if (list) { await p.mouse.wheel(0, 120); await p.waitForTimeout(300); listAfterScroll = await overlap(); await p.mouse.wheel(0, -120); await p.waitForTimeout(200); }
        results.push({ tag, innerOutline: inner, list, listAfterScroll });
        await p.keyboard.press('Escape');
        await p.locator(focusSel).first().evaluate((el) => el.blur());
        await p.waitForTimeout(300);
      }
    }
    await ctx.close();
  }
  fs.writeFileSync(`${OUT}/results.json`, JSON.stringify(results, null, 1));
  console.log('cases', results.length);
  console.log('内部焦点框：', results.filter((r) => r.innerOutline).map((r) => r.tag));
  console.log('下拉列表盖住输入框：', results.filter((r) => (r.list && r.list.covers) || (r.listAfterScroll && r.listAfterScroll.covers)).map((r) => r.tag));
  console.log('下拉列表检查：', results.filter((r) => r.list).map((r) => r.tag + ' ' + JSON.stringify([r.list, r.listAfterScroll])).slice(0, 6));
  await b.close();
  await makeSheets(OUT);
  const bad = results.filter((r) => r.innerOutline || (r.list && r.list.covers) || (r.listAfterScroll && r.listAfterScroll.covers));
  console.log(bad.length ? `✖ ${bad.length} 项有问题` : `✔ ${results.length} 项全部通过，截图 ${results.length * 2} 张`);
  process.exit(bad.length ? 1 : 0);
})();
