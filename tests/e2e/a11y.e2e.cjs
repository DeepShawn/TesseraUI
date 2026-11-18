// 无障碍扫描：axe-core 检查首页与组件总览 × 三种外观 × 浅色 / 深色 / 增强对比度，桌面与手机。
// 严重（critical）与重要（serious）问题必须为 0；每种组合截一张图。
// 依赖：axe-core（CI 中 npm install --no-save axe-core），用法：npm run test:a11y
const { chromium, devices } = require('playwright');
const fs = require('fs');
const path = require('path');
const BASE = process.env.TESSERA_BASE || 'http://localhost:5173';
const OUT = path.join(__dirname, 'screenshots', 'a11y');
fs.rmSync(OUT, { recursive: true, force: true });
fs.mkdirSync(OUT, { recursive: true });
const axePath = require.resolve('axe-core/axe.min.js', { paths: [process.cwd(), process.env.AXE_DIR || ''].filter(Boolean) });

(async () => {
  const b = await chromium.launch();
  let bad = 0, total = 0;
  const seen = new Map();
  for (const mobile of [false, true]) for (const page of ['index', 'components']) for (const shape of ['tile', 'round', 'flow']) for (const theme of ['light', 'dark', 'contrast']) {
    const ctx = await b.newContext({ ...(mobile ? devices['iPhone 13'] : { viewport: { width: 1280, height: 900 } }), colorScheme: theme === 'dark' ? 'dark' : 'light', reducedMotion: 'reduce' });
    const p = await ctx.newPage();
    const q = [shape === 'tile' ? '' : `shape=${shape}&force`].filter(Boolean).join('&');
    await p.goto(`${BASE}/demo/${page}.html${q ? '?' + q : ''}`, { waitUntil: 'networkidle' });
    if (theme === 'contrast') await p.evaluate(() => { document.documentElement.dataset.contrast = 'more'; });
    await p.waitForTimeout(500);
    await p.addScriptTag({ path: axePath });
    const res = await p.evaluate(async () => {
      const r = await axe.run(document, { resultTypes: ['violations'], runOnly: { type: 'tag', values: ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'best-practice'] } });
      return r.violations.filter((v) => v.impact === 'critical' || v.impact === 'serious').map((v) => ({ id: v.id, impact: v.impact, help: v.help, nodes: v.nodes.slice(0, 40).map((n) => n.target.join(' ') + ' :: ' + (n.failureSummary || '').split('\n')[1]) }));
    });
    const tag = `${mobile ? '手机' : '桌面'}-${page}-${shape}-${theme}`;
    await p.screenshot({ path: path.join(OUT, `${res.length ? '失败-' : ''}${tag}.png`) });
    total++;
    if (res.length) {
      bad++;
      for (const v of res) {
        const key = v.id;
        if (!seen.has(key)) seen.set(key, { ...v, where: [] });
        seen.get(key).where.push(tag);
      }
    }
    await ctx.close();
  }
  await b.close();
  for (const v of seen.values()) {
    console.log(`✖ [${v.impact}] ${v.id}：${v.help}（${v.where.length} 个组合，如 ${v.where[0]}）`);
    for (const n of v.nodes) console.log('    ' + n);
  }
  console.log(bad ? `\n${bad} / ${total} 个组合有严重或重要问题` : `✔ ${total} 个组合全部通过（截图 ${total} 张）`);
  process.exit(bad ? 1 : 0);
})();
