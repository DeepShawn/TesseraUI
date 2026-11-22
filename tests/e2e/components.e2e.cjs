// 组件交互端到端测试（需要 Playwright 与 Chromium）。
// 用法：先在仓库根目录启动静态服务器（npx http-server -p 5173 .），再运行 node tests/e2e/components.e2e.cjs
const { chromium, devices } = require('playwright');
const assert = require('node:assert/strict');

const BASE = process.env.TESSERA_BASE || 'http://localhost:5173';
// TESSERA_SHAPE=round / flow：在圆融、流韵外观下跑同一套测试（流韵加 force，跳过设备性能检测）
const SHAPE = ['round', 'flow'].includes(process.env.TESSERA_SHAPE) ? process.env.TESSERA_SHAPE : 'tile';
const PAGE = `${BASE}/demo/components.html${SHAPE === 'tile' ? '' : `?shape=${SHAPE}&force`}`;
const SHAPE_NAME = { tile: '拼贴', round: '圆融', flow: '流韵' }[SHAPE];
// 每一项测试结束后都截一张图，存到 TESSERA_SHOTS（默认 tests/e2e/screenshots/<外观>/），CI 会上传
const path = require('node:path');
const fs = require('node:fs');
const SHOTS = process.env.TESSERA_SHOTS || path.join(__dirname, 'screenshots', SHAPE);
fs.rmSync(SHOTS, { recursive: true, force: true });
fs.mkdirSync(SHOTS, { recursive: true });
let passed = 0, failed = 0, shotPage = null, shotNo = 0;
async function check(name, fn) {
  let ok = true;
  try { await fn(); passed++; console.log(`  ✔ ${name}`); }
  catch (e) { ok = false; failed++; console.log(`  ✖ ${name}\n    ${e.message.split('\n')[0]}`); }
  if (shotPage) {
    const file = `${String(++shotNo).padStart(2, '0')}-${ok ? '' : '失败-'}${name.replace(/[\\/:*?"<>|\s]+/g, '_')}.png`;
    await shotPage.screenshot({ path: path.join(SHOTS, file) }).catch(() => {});
  }
}

(async () => {
  const browser = await chromium.launch();
  const ctx = await browser.newContext({ viewport: { width: 1400, height: 900 }, permissions: ['clipboard-read', 'clipboard-write'] });
  const p = await ctx.newPage();
  const errors = [];
  p.on('pageerror', (e) => errors.push(e.message));
  p.on('console', (m) => { if (m.type() === 'error') errors.push(m.text()); });
  await p.goto(PAGE);
  shotPage = p;
  console.log(`外观：${SHAPE_NAME}`);
  await p.waitForTimeout(1500);
  const esc = () => p.keyboard.press('Escape');

  await check('外观与地址栏参数一致，且可以切换', async () => {
    assert.equal(await p.evaluate(() => document.documentElement.dataset.shape || 'tile'), SHAPE);
    const other = SHAPE === 'tile' ? 'round' : 'tile';
    await p.locator(`#shape input[value="${other}"]`).evaluate((i) => i.closest('label').click());
    await p.waitForTimeout(400);
    assert.equal(await p.evaluate(() => document.documentElement.dataset.shape || 'tile'), other);
    await p.locator(`#shape input[value="${SHAPE}"]`).evaluate((i) => i.closest('label').click());
    await p.waitForTimeout(400);
    assert.equal(await p.evaluate(() => document.documentElement.dataset.shape || 'tile'), SHAPE);
  });
  if (SHAPE !== 'tile') {
    await check(`${SHAPE_NAME}：按钮组合成一整块，中间没有缝`, async () => {
      const gaps = await p.evaluate(() => [...document.querySelectorAll(':is(.ts-tiles, .ts-split-button, .ts-number):not(.ts-drawer-foot, .ts-dialog-actions)')].map((g) => getComputedStyle(g).columnGap));
      assert.ok(gaps.length > 0 && gaps.every((g) => g === '0px' || g === 'normal'), gaps.join(','));
    });
    await check(`${SHAPE_NAME}：列表行之间没有缝和分割线`, async () => {
      const r = await p.evaluate(() => { const l = document.querySelector('.ts-list'); const i = l.querySelector('.ts-list-item'); return [getComputedStyle(l).rowGap, getComputedStyle(i).boxShadow]; });
      assert.equal(r[0], '0px'); assert.equal(r[1], 'none');
    });
  }
  if (SHAPE === 'flow') {
    await check('流韵：按下时荡开水波，松手后自动清除', async () => {
      const b = p.locator('#forms .ts-button.ts-primary').first();
      await b.scrollIntoViewIfNeeded();
      const box = await b.boundingBox();
      await p.mouse.move(box.x + 10, box.y + box.height / 2); await p.mouse.down();
      assert.equal(await b.locator('.ts-flow-ink').count(), 1);
      await p.mouse.up(); await p.waitForTimeout(1000);
      assert.equal(await b.locator('.ts-flow-ink').count(), 0);
    });
    await check('流韵：勾选时溅起涟漪', async () => {
      const sw = p.locator('.ts-checkbox:not(:disabled):visible').first();
      await sw.scrollIntoViewIfNeeded();
      await sw.click();
      assert.ok((await p.locator('.ts-flow-splash').count()) >= 1);
      await p.waitForTimeout(900);
      assert.equal(await p.locator('.ts-flow-splash').count(), 0);
      await sw.click();
    });
    await check('流韵：触感接口在不支持的环境里静默降级，返回布尔值', async () => {
      const r = await p.evaluate(async () => { const T = (await import('/platforms/web/dist/tessera.js')).default; return ['selection', 'light', 'medium', 'success', 'warning', 'unknown'].map((k) => typeof T.haptic(k)); });
      assert.deepEqual([...new Set(r)], ['boolean']);
    });
  }

  console.log('操作与输入');
  await check('按钮组单选', async () => {
    await p.click('.ts-button-group button:nth-child(2)');
    assert.equal(await p.getAttribute('.ts-button-group button:nth-child(2)', 'aria-pressed'), 'true');
    assert.equal(await p.getAttribute('.ts-button-group button:nth-child(1)', 'aria-pressed'), 'false');
  });
  await check('分割按钮打开菜单', async () => {
    await p.click('[data-menu="save-menu"]');
    await p.waitForTimeout(300);
    assert.ok(await p.isVisible('#save-menu'));
    await esc(); await p.waitForTimeout(300);
  });
  await check('密码显示 / 隐藏', async () => {
    await p.click('.ts-password .ts-reveal');
    assert.equal(await p.getAttribute('#pw', 'type'), 'text');
    await p.click('.ts-password .ts-reveal');
    assert.equal(await p.getAttribute('#pw', 'type'), 'password');
  });
  await check('数字步进器：加减、上限、全角数字', async () => {
    const input = p.locator('.ts-number input');
    await p.click('.ts-number button[aria-label="增加"]');
    assert.equal(await input.inputValue(), '3');
    await input.fill('１２０'); await input.blur();
    assert.equal(await input.inputValue(), '99');
    await input.focus(); await p.keyboard.press('ArrowDown');
    assert.equal(await input.inputValue(), '98');
  });
  await check('验证码：只收数字、输满触发 complete、错误态', async () => {
    const input = p.locator('#otp input');
    await input.focus();
    await p.keyboard.type('12a34５6');
    assert.equal(await input.inputValue(), '123456');
    assert.match(await p.textContent('#otp-msg'), /验证通过/);
    await input.fill(''); await input.focus(); await p.keyboard.type('000000');
    assert.ok(await p.getAttribute('#otp', 'data-invalid') !== null);
  });
  await check('自动补全：过滤、键盘选择', async () => {
    await p.fill('#city', '州');
    await p.waitForTimeout(300);
    const opts = await p.locator('.ts-listbox:visible .ts-option').allTextContents();
    assert.ok(opts.some((o) => o.startsWith('广州')) && opts.some((o) => o.startsWith('杭州')), opts.join());
    await p.keyboard.press('ArrowDown'); await p.keyboard.press('Enter');
    assert.equal(await p.inputValue('#city'), '杭州');
  });
  await check('自动补全：输入法组合中不过滤', async () => {
    await p.fill('#city', '');
    const before = await p.locator('.ts-listbox .ts-option').count();
    await p.evaluate(() => { const i = document.getElementById('city'); i.dispatchEvent(new CompositionEvent('compositionstart')); i.value = 'shen'; i.dispatchEvent(new InputEvent('input', { isComposing: true })); });
    await p.waitForTimeout(200);
    assert.equal(await p.locator('.ts-listbox .ts-option').count(), before);
    await p.evaluate(() => { const i = document.getElementById('city'); i.value = '深'; i.dispatchEvent(new CompositionEvent('compositionend')); });
    await p.waitForTimeout(200);
    assert.deepEqual(await p.locator('.ts-listbox:visible .ts-option').allTextContents(), ['深圳粤']);
    await esc();
  });
  await check('标签输入：回车、顿号添加，退格两次删除', async () => {
    await p.click('#tags');
    await p.keyboard.type('动效'); await p.keyboard.press('Enter');
    await p.keyboard.type('图标、');
    let tags = await p.locator('.ts-taginput .ts-tag > span:first-child').allTextContents();
    assert.deepEqual(tags, ['设计', '前端', '动效', '图标']);
    await p.keyboard.press('Backspace'); await p.keyboard.press('Backspace');
    tags = await p.locator('.ts-taginput .ts-tag > span:first-child').allTextContents();
    assert.deepEqual(tags, ['设计', '前端', '动效']);
  });
  await check('滑块：键盘与点击轨道', async () => {
    const thumb = p.locator('#vol .ts-slider-thumb');
    await thumb.focus(); await p.keyboard.press('ArrowRight');
    assert.equal(await thumb.getAttribute('aria-valuenow'), '61');
    await p.keyboard.press('End');
    assert.equal(await thumb.getAttribute('aria-valuenow'), '100');
    const box = await p.locator('#vol .ts-slider-track').boundingBox();
    await p.mouse.click(box.x + box.width * 0.25, box.y + box.height / 2);
    assert.equal(await thumb.getAttribute('aria-valuenow'), '25');
  });
  await check('范围滑块：两个滑块不能交叉', async () => {
    const [a, b] = [p.locator('#price .ts-slider-thumb').nth(0), p.locator('#price .ts-slider-thumb').nth(1)];
    await a.focus(); await p.keyboard.press('End');
    assert.equal(await a.getAttribute('aria-valuenow'), await b.getAttribute('aria-valuenow'));
  });
  await check('评分：点击与键盘', async () => {
    const r = p.locator('.ts-rating').first();
    await r.locator('button').nth(1).click();
    assert.equal(await r.locator('button[data-on]').count(), 2);
    await p.keyboard.press('ArrowRight');
    assert.equal(await r.locator('button[data-on]').count(), 3);
  });
  await check('文件上传：选择文件后出现在列表并完成', async () => {
    await p.setInputFiles('#upload input[type=file]', [{ name: '设计稿.png', mimeType: 'image/png', buffer: Buffer.from('x') }]);
    await p.waitForTimeout(3500);
    const items = await p.locator('.ts-upload-item').allTextContents();
    assert.ok(items.some((t) => t.includes('设计稿.png') && t.includes('已上传')), items.join());
  });
  await check('表单校验：中文提示，修正后消失', async () => {
    await p.click('#signup button:not([type])');
    assert.equal(await p.textContent('#f1 >> xpath=../../div[contains(@class,"ts-field-help")]'), '此项为必填');
    await p.fill('#f2', 'abc');
    await p.click('#signup button:not([type])');
    assert.equal(await p.textContent('#f2 >> xpath=../../div[contains(@class,"ts-field-help")]'), '邮箱格式不正确');
    await p.fill('#f1', '林'); await p.fill('#f1', '林间');
    assert.equal(await p.textContent('#f1 >> xpath=../../div[contains(@class,"ts-field-help")]'), '2–12 个字符');
  });

  console.log('选择器');
  await check('日期选择器：打开日历、选中日期', async () => {
    await p.locator('.ts-datepicker').nth(1).click();
    await p.waitForTimeout(300);
    await p.click('.ts-picker-panel:visible .ts-day[data-key="2026-11-27"]');
    await p.waitForTimeout(300);
    assert.equal(await p.locator('.ts-datepicker').nth(1).locator('input:not([type=hidden])').inputValue(), '2026 年 11 月 27 日');
  });
  await check('日期选择器：键盘移动与翻月', async () => {
    await p.locator('.ts-datepicker').nth(1).click();
    await p.waitForTimeout(300);
    await p.keyboard.press('ArrowRight'); await p.keyboard.press('PageDown'); await p.keyboard.press('Enter');
    await p.waitForTimeout(300);
    assert.equal(await p.locator('.ts-datepicker').nth(1).locator('input:not([type=hidden])').inputValue(), '2026 年 12 月 28 日');
  });
  await check('日期范围：两次点击选出范围', async () => {
    await p.click('.ts-daterange');
    await p.waitForTimeout(300);
    const panel = p.locator('.ts-picker-panel:visible');
    await panel.locator('.ts-calendar').first().locator('.ts-day:not([data-outside])').nth(9).click();
    await panel.locator('.ts-calendar').first().locator('.ts-day:not([data-outside])').nth(14).click();
    await p.waitForTimeout(500);
    assert.match(await p.locator('.ts-daterange input').inputValue(), /10 日 – .*15 日/);
  });
  await check('日历：换选日期时选中砖滑过去；切换月份时整月推入', async () => {
    const cal = p.locator('.ts-calendar').first();
    await cal.evaluate((e) => e.scrollIntoView({ block: 'center', behavior: 'instant' }));
    const days = cal.locator('.ts-day:not([data-outside])');
    await days.nth(3).click(); await p.waitForTimeout(600);
    await p.evaluate(() => { window.__dx = []; const loop = () => { const i = document.querySelector('.ts-calendar .ts-day-indicator'); window.__dx.push(i.style.transform); if (window.__dx.length < 40) requestAnimationFrame(loop); }; requestAnimationFrame(loop); });
    await days.nth(20).click(); await p.waitForTimeout(800);
    const tr = await p.evaluate(() => window.__dx);
    assert.ok(new Set(tr).size > 6, '选中砖没有滑动');
    const ind = await cal.locator('.ts-day-indicator').boundingBox(), d = await days.nth(20).boundingBox();
    assert.ok(Math.abs(ind.x - d.x) < 1 && Math.abs(ind.y - d.y) < 1, '选中砖没有落在选中的日期上');
    assert.equal(await days.nth(20).getAttribute('aria-pressed'), 'true');
    const title = await cal.locator('.ts-calendar-title').textContent();
    await cal.locator('[aria-label="下个月"]').click();
    await p.waitForTimeout(100);
    assert.equal(await p.locator('.ts-calendar').first().locator('.ts-calendar-grid').count(), 2, '换月时旧月份没有滑出');
    await p.waitForTimeout(900);
    const c2 = p.locator('.ts-calendar').first();
    assert.equal(await c2.locator('.ts-calendar-grid').count(), 1);
    assert.notEqual(await c2.locator('.ts-calendar-title').textContent(), title);
    await c2.locator('[aria-label="上个月"]').click(); await p.waitForTimeout(900);
  });
  await check('时间选择器：滚轮选择后确定', async () => {
    await p.click('.ts-timepicker');
    await p.waitForTimeout(400);
    const col = p.locator('.ts-picker-panel:visible .ts-wheel-col').first();
    await col.focus(); await p.keyboard.press('ArrowDown'); await p.keyboard.press('ArrowDown');
    await p.waitForTimeout(700);
    await p.click('.ts-picker-panel:visible .ts-button.ts-primary');
    await p.waitForTimeout(300);
    assert.equal(await p.locator('.ts-timepicker input').inputValue(), '11:30');
  });
  await check('滚轮选择器：拖动跟手，松手带惯性继续走，弹簧落定在整行', async () => {
    const col = p.locator('#wheel .ts-wheel-col').first();
    await col.evaluate((e) => e.scrollIntoView({ block: 'center', behavior: 'instant' }));
    await col.focus(); await p.keyboard.press('Home'); await p.waitForTimeout(700);
    const box = await col.boundingBox();
    const x = box.x + box.width / 2, y = box.y + box.height / 2;
    // 逐帧记录位置（单位：行），由选中项的圆柱位移反推
    await p.evaluate(() => {
      window.__wheel = []; const c = document.querySelector('#wheel .ts-wheel-col'); const items = [...c.querySelectorAll('.ts-wheel-item')];
      const row = items[0].offsetHeight, STEP = 18 * Math.PI / 180, R = row / STEP;
      const loop = () => { const i = items.findIndex((n) => n.getAttribute('aria-selected') === 'true'); const ty = parseFloat(items[i].style.transform.match(/translateY\(([-\d.]+)px/)[1]);
        window.__wheel.push(i - Math.asin(Math.max(-1, Math.min(1, ty / R))) / STEP); if (window.__wheel.length < 200) requestAnimationFrame(loop); };
      requestAnimationFrame(loop);
    });
    await p.mouse.move(x, y); await p.mouse.down();
    for (let i = 1; i <= 5; i++) { await p.mouse.move(x, y - i * 18); await p.waitForTimeout(16); }
    const atRelease = await p.evaluate(() => window.__wheel[window.__wheel.length - 1]);
    await p.mouse.up();
    await p.waitForTimeout(1500);
    const t = await p.evaluate(() => window.__wheel);
    const end = t[t.length - 1];
    assert.ok(Math.abs(atRelease - 2.5) < 0.3, `拖动没有跟手：松手时在第 ${atRelease.toFixed(2)} 行，应为 2.5`);
    assert.ok(Math.abs(end - Math.round(end)) < 0.01, `没有落在整行上：${end}`);
    assert.ok(end >= 3.5, `松手后没有惯性：只走到第 ${end.toFixed(2)} 行`);
    // 落定前有一点回弹（略过目标再回来），而不是硬停
    assert.ok(Math.max(...t) > end + 0.005, '落定时没有弹簧回弹');
    // 松手后每帧的位移不能突变（硬停或跳跃）：相邻两帧差不超过 0.6 行
    const after = t.slice(t.findIndex((v) => v > atRelease + 0.01));
    for (let i = 1; i < after.length; i++) assert.ok(Math.abs(after[i] - after[i - 1]) < 0.6, `第 ${i} 帧跳了 ${(after[i] - after[i - 1]).toFixed(2)} 行`);
    assert.match(await p.textContent('#wheel-v'), /已选/);
  });
  await check('滚轮选择器：鼠标滚轮一格一行', async () => {
    const col = p.locator('#wheel .ts-wheel-col').first();
    await col.focus(); await p.keyboard.press('Home'); await p.waitForTimeout(700);
    const box = await col.boundingBox();
    await p.mouse.move(box.x + box.width / 2, box.y + box.height / 2);
    await p.mouse.wheel(0, 100); await p.waitForTimeout(250); await p.mouse.wheel(0, 100); await p.waitForTimeout(800);
    assert.equal(await col.locator('[aria-selected="true"]').textContent(), '广东');
  });
  await check('颜色选择器：HEX 输入同步', async () => {
    const hex = p.locator('.ts-colorpicker-inline input');
    await hex.fill('#2F4E9E');
    assert.equal(await p.getAttribute('.ts-colorpicker-inline .ts-color-swatches button:nth-child(5)', 'aria-pressed'), 'true');
  });

  console.log('导航');
  await check('侧边栏折叠', async () => {
    await p.click('[data-sidebar-toggle]');
    assert.ok(await p.getAttribute('.ts-sidebar', 'data-collapsed') !== null);
    await p.click('[data-sidebar-toggle]');
  });
  await check('目录锚点：点击后页面用弹簧平滑滚到目标区块', async () => {
    const link = p.locator('.ts-anchor a[href="#data"]');
    await link.evaluate((e) => e.scrollIntoView({ block: 'center', behavior: 'instant' }));
    await p.waitForTimeout(300);
    await p.evaluate(() => { window.__sy = []; const loop = () => { window.__sy.push(scrollY); if (window.__sy.length < 120) requestAnimationFrame(loop); }; requestAnimationFrame(loop); });
    await link.click();
    await p.waitForTimeout(2000);
    const ys = await p.evaluate(() => window.__sy);
    const top = await p.evaluate(() => document.getElementById('data').getBoundingClientRect().top);
    assert.ok(Math.abs(top - 40) < 6, `没有停在目标区块：区块顶部离视口顶部 ${top.toFixed(1)}px，应约为 40`);
    const moving = new Set(ys.map((v) => Math.round(v))).size;
    assert.ok(moving > 15, `只有 ${moving} 个不同位置，像是直接跳过去的`);
    assert.equal(await link.getAttribute('aria-current'), 'true');
  });
  await check('分页：点击与折叠省略号', async () => {
    const pg = p.locator('.ts-pagination').first();
    await pg.locator('button[aria-label="第 7 页"]').click();
    assert.equal(await pg.locator('[aria-current="page"]').textContent(), '7');
    await pg.locator('button[aria-label="第 20 页"]').click();
    assert.deepEqual(await pg.locator('.ts-page').allTextContents(), ['', '1', '…', '16', '17', '18', '19', '20', '']);
  });
  await check('分页：翻页时按钮复用不重建，当前页选中砖用弹簧滑过去', async () => {
    const pg = p.locator('.ts-pagination').first();
    await pg.evaluate((e) => e.scrollIntoView({ block: 'center', behavior: 'instant' }));
    const same = await pg.evaluate((root) => { window.__btn1 = root.querySelector('button[aria-label="第 1 页"]'); return !!window.__btn1; });
    assert.ok(same);
    await p.evaluate(() => { window.__px = []; const ind = document.querySelector('.ts-pagination .ts-page-indicator'); const loop = () => { window.__px.push(ind.style.transform); if (window.__px.length < 40) requestAnimationFrame(loop); }; requestAnimationFrame(loop); });
    await pg.locator('button[aria-label="第 1 页"]').click();
    await p.waitForTimeout(800);
    const xs = await p.evaluate(() => window.__px);
    assert.ok(new Set(xs).size > 6, `选中砖没有滑动（只有 ${new Set(xs).size} 个位置）`);
    assert.ok(await pg.evaluate(() => window.__btn1.isConnected && window.__btn1.getAttribute('aria-current') === 'page'), '页码按钮被重建了');
    const ind = await pg.locator('.ts-page-indicator').boundingBox(), cur = await pg.locator('[aria-current="page"]').boundingBox();
    assert.ok(Math.abs(ind.x - cur.x) < 1 && Math.abs(ind.width - cur.width) < 1, '选中砖没有落在当前页上');
  });
  await check('右键菜单', async () => {
    await p.click('[data-context-menu="ctx"]', { button: 'right' });
    await p.waitForTimeout(300);
    assert.ok(await p.isVisible('#ctx'));
    await esc(); await p.waitForTimeout(300);
    assert.ok(await p.isHidden('#ctx'));
  });
  await check('命令面板：⌘K 打开、过滤、回车执行', async () => {
    await p.keyboard.press('Control+k');
    await p.waitForTimeout(300);
    await p.keyboard.type('深色');
    await p.waitForTimeout(200);
    await p.keyboard.press('Enter');
    await p.waitForTimeout(500);
    assert.equal(await p.getAttribute('html', 'data-theme'), 'dark');
    await p.evaluate(() => document.documentElement.removeAttribute('data-theme'));
  });

  console.log('数据展示');
  const budgets = () => p.$$eval('#tbl tbody tr td:nth-child(6)', (tds) => tds.map((td) => Number(td.dataset.value)));
  await check('表格：分页（每页 6 条）', async () => {
    assert.equal(await p.locator('#tbl tbody tr').count(), 6);
    assert.match(await p.textContent('.ts-table-footer'), /共 23 条，第 1–6 条/);
    await p.locator('.ts-table-footer button[aria-label="第 4 页"]').click();
    assert.equal(await p.locator('#tbl tbody tr').count(), 5);
    assert.match(await p.textContent('.ts-table-footer'), /第 19–23 条/);
  });
  await check('表格：数字列排序、三态、跨页', async () => {
    const th = p.locator('#tbl th', { hasText: '预算' }).locator('button');
    await th.click();
    let v = await budgets();
    assert.deepEqual(v, [...v].sort((a, b) => a - b));
    assert.match(await p.textContent('.ts-table-footer'), /第 1–6 条/, '排序后回到第 1 页');
    await th.click();
    v = await budgets();
    assert.deepEqual(v, [...v].sort((a, b) => b - a));
    assert.equal(await p.getAttribute('#tbl th:nth-child(6)', 'aria-sort'), 'descending');
    await th.click();
    assert.equal(await p.textContent('#tbl tbody tr:first-child td:nth-child(2)'), '设计令牌');
  });
  await check('表格：筛选与空状态', async () => {
    await p.fill('#tbl-filter', '组件');
    assert.equal(await p.locator('#tbl tbody tr').count(), 5);
    await p.fill('#tbl-filter', '不存在');
    assert.match(await p.textContent('#tbl tbody'), /没有符合条件/);
    await p.fill('#tbl-filter', '');
  });
  await check('表格：选择、全选半选、Shift 连选、批量栏', async () => {
    const boxes = p.locator('#tbl tbody .ts-select-cell input');
    await boxes.nth(0).check();
    assert.ok(await p.isVisible('#tbl-bulk'));
    assert.equal(await p.textContent('#tbl-bulk [data-bulk-count]'), '1');
    assert.ok(await p.$eval('#tbl thead .ts-checkbox', (b) => b.indeterminate));
    await boxes.nth(3).click({ modifiers: ['Shift'] });
    assert.equal(await p.textContent('#tbl-bulk [data-bulk-count]'), '4');
    await p.check('#tbl thead .ts-checkbox');
    assert.equal(await p.textContent('#tbl-bulk [data-bulk-count]'), '23');
    await p.click('#tbl-clear');
    assert.ok(await p.isHidden('#tbl-bulk'));
  });
  await check('表格：批量删除', async () => {
    const boxes = p.locator('#tbl tbody .ts-select-cell input');
    await boxes.nth(0).check(); await boxes.nth(1).check();
    await p.click('#tbl-del');
    await p.waitForTimeout(400);
    await p.click('dialog[open] .ts-danger');
    await p.waitForTimeout(500);
    assert.match(await p.textContent('.ts-table-footer'), /共 21 条/);
  });
  await check('表格：截断的备注有完整提示', async () => {
    assert.ok(await p.locator('#tbl td.ts-truncate[data-tooltip]').count() > 0);
  });
  await check('表格：刷新时显示骨架行', async () => {
    await p.click('#tbl-reload');
    assert.ok(await p.locator('#tbl .ts-table-loading').count() > 0);
    await p.waitForTimeout(1200);
    assert.equal(await p.locator('#tbl .ts-table-loading').count(), 0);
  });
  await check('表格：窄容器变成卡片', async () => {
    await p.$eval('#table-demo .ts-table-wrap', (w) => (w.style.width = '380px'));
    await p.waitForTimeout(200);
    assert.equal(await p.$eval('#tbl thead', (t) => getComputedStyle(t).display), 'none');
    assert.equal(await p.$eval('#tbl tbody tr', (t) => getComputedStyle(t).display), 'grid');
    await p.$eval('#table-demo .ts-table-wrap', (w) => (w.style.width = ''));
  });
  await check('树：键盘展开与选择', async () => {
    await p.click('.ts-tree-row:has-text("apple")');
    assert.equal(await p.locator('li:has(> .ts-tree-row:has-text("apple"))').getAttribute('aria-expanded'), 'true');
    await p.keyboard.press('ArrowDown'); await p.keyboard.press('Enter');
    assert.equal(await p.locator('li:has(> .ts-tree-row:has-text("Package.swift"))').getAttribute('aria-selected'), 'true');
  });
  await check('折叠面板：单开模式', async () => {
    await p.click('.ts-accordion summary:has-text("可以商用吗")');
    await p.waitForTimeout(500);
    assert.equal(await p.locator('.ts-accordion details[open]').count(), 1);
    assert.match(await p.textContent('.ts-accordion details[open] summary'), /商用/);
  });
  await check('工具提示：悬停出现，title 被替换', async () => {
    // 先滚动到位并停稳再悬停：工具提示在滚动时会主动隐藏
    await p.locator('[data-tooltip="下载"]').evaluate((e) => e.scrollIntoView({ block: 'center', behavior: 'instant' }));
    await p.waitForTimeout(300);
    await p.hover('[data-tooltip="下载"]');
    await p.waitForTimeout(800);
    assert.equal(await p.textContent('.ts-tooltip'), '下载');
    assert.equal(await p.locator('[aria-label="帮助"]').getAttribute('title'), null);
  });
  await check('气泡卡片', async () => {
    await p.click('[data-popover="pop1"]');
    await p.waitForTimeout(300);
    assert.ok(await p.isVisible('#pop1'));
    await esc();
  });
  await check('轮播：下一张', async () => {
    await p.click('.ts-carousel .ts-next');
    await p.waitForTimeout(800);
    assert.equal(await p.locator('.ts-carousel-dots [aria-current="true"]').getAttribute('aria-label'), '第 2 张');
  });
  await check('图片预览：打开、切换、关闭', async () => {
    await p.click('#thumbs img >> nth=0');
    await p.waitForTimeout(600);
    assert.ok(await p.isVisible('.ts-viewer'));
    await p.keyboard.press('ArrowRight');
    assert.equal(await p.textContent('.ts-viewer-bar span'), '2 / 4');
    await esc(); await p.waitForTimeout(400);
    assert.equal(await p.locator('.ts-viewer').count(), 0);
  });
  await check('代码块：复制', async () => {
    await p.click('.ts-code .ts-copy');
    const text = await p.evaluate(() => navigator.clipboard.readText());
    assert.ok(text.startsWith("import Tessera from './tessera.js';"), text);
    assert.ok(!/^\d/.test(text));
  });

  console.log('开发者组件');
  const ta = p.locator('#editor textarea');
  const val = () => ta.inputValue();
  await check('代码编辑器：Tab 缩进、Shift+Tab 反缩进，多行选中整块缩进', async () => {
    await ta.evaluate((t) => { t.scrollIntoView({ block: 'center' }); t.focus(); t.setSelectionRange(t.value.indexOf('let x'), t.value.indexOf('let x')); });
    await p.keyboard.press('Tab');
    assert.ok((await val()).includes('\n    let x = 0;'), '没有缩进');
    await p.keyboard.press('Shift+Tab');
    assert.ok((await val()).includes('\n  let x = 0;'), '没有反缩进');
    // 选中第 5、6 行整块缩进，再整块反缩进
    await ta.evaluate((t) => { const s = t.value.indexOf('    t.x'); const e = t.value.indexOf('\n  }'); t.setSelectionRange(s, e); });
    await p.keyboard.press('Tab');
    assert.ok((await val()).includes('\n      t.x = x;\n      x += t.width'), '多行没有一起缩进');
    await p.keyboard.press('Shift+Tab');
    assert.ok((await val()).includes('\n    t.x = x;\n    x += t.width'), '多行没有一起反缩进');
  });
  await check('代码编辑器：回车保持缩进，{ 之后多缩一级，括号中间回车把右括号放到下一行', async () => {
    await ta.evaluate((t) => { const i = t.value.indexOf('let x = 0;') + 'let x = 0;'.length; t.setSelectionRange(i, i); });
    await p.keyboard.press('Enter');
    await p.keyboard.type('if (x) {}');
    await p.keyboard.press('ArrowLeft');
    await p.keyboard.press('Enter');
    assert.ok((await val()).includes('\n  if (x) {\n    \n  }\n'), JSON.stringify((await val()).slice(60, 140)));
    await p.keyboard.press('Control+z'); await p.keyboard.press('Control+z'); await p.keyboard.press('Control+z');
  });
  await check('代码编辑器：行号、当前行与诊断提示同步', async () => {
    const n = (await val()).split('\n').length;
    assert.equal(await p.locator('#editor .ts-ce-gutter span').count(), n);
    assert.equal(await p.locator('#editor .ts-ce-gutter span[data-error]').count(), 1);
    assert.equal(await ta.getAttribute('aria-invalid'), 'true');
    await ta.evaluate((t) => { t.setSelectionRange(0, 0); t.focus(); });
    await p.keyboard.press('ArrowDown'); await p.waitForTimeout(50);
    assert.match(await p.textContent('#editor .ts-ce-pos'), /^第 2 行/);
    assert.equal(await p.locator('#editor .ts-ce-gutter span[data-current]').textContent(), '2');
  });
  await check('代码编辑器：输入法组合中 Tab 不缩进；Esc 之后 Tab 离开编辑器', async () => {
    const before = await val();
    await ta.evaluate((t) => {
      t.dispatchEvent(new CompositionEvent('compositionstart'));
      t.dispatchEvent(new KeyboardEvent('keydown', { key: 'Tab', isComposing: true, bubbles: true, cancelable: true }));
      t.dispatchEvent(new CompositionEvent('compositionend'));
    });
    assert.equal(await val(), before);
    await ta.focus();
    await p.keyboard.press('Escape');
    await p.keyboard.press('Tab');
    assert.notEqual(await p.evaluate(() => document.activeElement?.classList.contains('ts-ce-input')), true, '焦点仍在编辑器里');
  });
  await check('十六进制查看：点击选中、方向键移动、Shift 扩展、复制为十六进制', async () => {
    await p.locator('#hex').scrollIntoViewIfNeeded();
    await p.click('#hex .ts-hex-bytes [data-i="1"]');
    assert.match(await p.textContent('#hex .ts-hex-status'), /^偏移 0x1（1）：0x50 = 80 = 0b01010000，字符“P”，PNG 签名/);
    await p.keyboard.press('ArrowRight');
    await p.keyboard.press('Shift+ArrowRight');
    assert.match(await p.textContent('#hex .ts-hex-status'), /已选 2 个字节（2–3）/);
    // 十六进制与 ASCII 两栏同时选中
    assert.equal(await p.locator('#hex .ts-hex-ascii [data-selected]').count(), 2);
    await p.keyboard.press('Control+c');
    assert.equal(await p.evaluate(() => navigator.clipboard.readText()), '4E 47');
    await p.keyboard.press('ArrowDown');
    const cols = Number(await p.locator('#hex').evaluate((e) => getComputedStyle(e).getPropertyValue('--_cols')));
    assert.match(await p.textContent('#hex .ts-hex-status'), new RegExp(`偏移 0x${(3 + cols).toString(16).toUpperCase()}（${3 + cols}）`));
  });
  await check('十六进制查看：只渲染可见的行；Ctrl+End 跳到最后一个字节', async () => {
    const total = Math.ceil(await p.locator('#hex').evaluate((e) => e.querySelectorAll('.ts-hex-row').length));
    await p.keyboard.press('Control+End');
    await p.waitForTimeout(100);
    const status = await p.textContent('#hex .ts-hex-status');
    // 演示数据：29 字节 PNG 文件头 + 40 字节文字 + 420 字节，共 489 字节
    assert.match(status, /偏移 0x1E8（488）/, status);
    assert.ok(await p.locator('#hex [data-focus]').count() >= 1, '最后一个字节没有渲染出来');
    assert.ok(total < 489 / 16, `渲染了 ${total} 行`);
  });

  console.log('反馈');
  await check('横幅关闭', async () => {
    const n = await p.locator('.ts-banner').count();
    await p.click('.ts-banner >> nth=0 >> button[aria-label="关闭"]');
    await p.waitForTimeout(500);
    assert.equal(await p.locator('.ts-banner').count(), n - 1);
  });
  await check('确认框：危险操作默认聚焦取消，Esc 返回 false', async () => {
    await p.click('#btn-confirm');
    await p.waitForTimeout(400);
    assert.equal(await p.evaluate(() => document.activeElement.textContent), '取消');
    await esc(); await p.waitForTimeout(400);
    assert.equal(await p.textContent('#fb-result'), '你选择了：取消');
  });
  await check('侧边抽屉：打开与关闭', async () => {
    await p.click('[data-drawer="drawer"]');
    await p.waitForTimeout(700);
    assert.ok(await p.isVisible('#drawer'));
    await esc(); await p.waitForTimeout(800);
    assert.ok(await p.isHidden('#drawer'));
  });
  await check('操作面板：选择一项', async () => {
    await p.click('#btn-sheet');
    await p.waitForTimeout(600);
    await p.click('.ts-action:has-text("分享")');
    await p.waitForTimeout(700);
    assert.equal(await p.textContent('#fb-result'), '你选择了第 2 项');
  });
  await check('进度：数值与读屏属性', async () => {
    await p.click('#prog-btn');
    assert.equal(await p.getAttribute('#prog', 'aria-valuenow'), '55');
    assert.equal(await p.textContent('#ring .ts-ring-label'), '55%');
  });

  console.log('布局');
  await check('可拖拽分栏：键盘调整', async () => {
    await p.focus('.ts-split-handle');
    await p.keyboard.press('ArrowRight');
    assert.equal(await p.getAttribute('.ts-split-handle', 'aria-valuenow'), '42');
    await p.keyboard.press('Enter');
  });
  await check('滚动区域：底部渐隐', async () => {
    assert.ok(await p.getAttribute('.ts-scroll-area.ts-card', 'data-fade-bottom') !== null);
  });

  console.log('移动端手势');
  await check('左滑操作：拖动露出按钮', async () => {
    const row = p.locator('.ts-swipe-content').first();
    await row.scrollIntoViewIfNeeded();
    const b = await row.boundingBox();
    await p.mouse.move(b.x + b.width - 20, b.y + b.height / 2);
    await p.mouse.down();
    for (let i = 1; i <= 8; i++) { await p.mouse.move(b.x + b.width - 20 - i * 25, b.y + b.height / 2); await p.waitForTimeout(16); }
    await p.mouse.up();
    await p.waitForTimeout(700);
    const t = await row.evaluate((el) => new DOMMatrix(getComputedStyle(el).transform).m41);
    assert.ok(t < -100, `translateX=${t}`);
  });
  await check('字母索引：点 G 跳到对应分组', async () => {
    await p.locator('.ts-indexbar button', { hasText: 'G' }).click();
    await p.waitForTimeout(300);
    const top = await p.evaluate(() => { const c = document.getElementById('contacts'); const g = c.querySelector('[data-index="G"]'); return Math.abs(g.getBoundingClientRect().top - c.getBoundingClientRect().top); });
    assert.ok(top < 4, `offset=${top}`);
  });
  await check('上拉加载：滚到底加载更多', async () => {
    const before = await p.locator('#feed li').count();
    await p.evaluate(() => { const c = document.getElementById('ptr'); c.scrollTop = c.scrollHeight; });
    await p.waitForTimeout(1500);
    assert.ok((await p.locator('#feed li').count()) > before);
  });

  if (SHAPE === 'flow') {
    await check('流韵：持续掉帧时自动退回圆融', async () => {
      const got = p.evaluate(() => new Promise((res) => document.addEventListener('tessera:flow-degraded', (e) => res(e.detail.reason), { once: true })));
      // 持续占住主线程，帧率降到 15 左右
      await p.evaluate(() => { window.__busy = setInterval(() => { const t = performance.now(); while (performance.now() - t < 60); }, 64); });
      const reason = await Promise.race([got, new Promise((r) => setTimeout(() => r('timeout'), 9000))]);
      await p.evaluate(() => clearInterval(window.__busy));
      assert.equal(reason, 'fps');
      assert.equal(await p.evaluate(() => document.documentElement.dataset.shape), 'round');
      // 恢复流韵，继续后面的测试
      await p.evaluate(async () => { const T = (await import('/platforms/web/dist/tessera.js')).default; T.setShape('flow', { force: true }); });
      await p.waitForTimeout(400);
    });
  }

  console.log('手机');
  const m = await browser.newPage({ ...devices['iPhone 13'] });
  m.on('pageerror', (e) => errors.push('mobile: ' + e.message));
  await m.goto(PAGE);
  shotPage = m;
  await m.waitForTimeout(1200);
  await check('手机上日期选择器改为底部抽屉滚轮', async () => {
    await m.locator('.ts-datepicker').nth(1).tap();
    await m.waitForTimeout(800);
    assert.ok(await m.locator('.ts-sheet:visible .ts-wheel').count() === 1);
    assert.equal(await m.locator('.ts-sheet:visible .ts-wheel-col').count(), 3);
    await m.locator('.ts-sheet:visible .ts-button.ts-primary').tap();
    await m.waitForTimeout(600);
    assert.equal(await m.locator('.ts-datepicker').nth(1).locator('input:not([type=hidden])').inputValue(), '2026 年 11 月 20 日');
  });

  await check('手机：长按打开右键菜单只打开一次，不选中文字，手指轻微抖动不取消', async () => {
    const t = m.locator('[data-context-menu="ctx"]');
    await t.evaluate((e) => e.scrollIntoView({ block: 'center', behavior: 'instant' })); await m.waitForTimeout(300);
    await m.evaluate(() => { window.__opens = 0; const menu = document.getElementById('ctx'); new MutationObserver(() => { if (!menu.hidden) window.__opens++; }).observe(menu, { attributes: true, attributeFilter: ['hidden'] }); });
    const box = await t.locator('.ts-label').boundingBox();
    const x = box.x + box.width / 2, y = box.y + box.height / 2;
    const cdp = await m.context().newCDPSession(m);
    await cdp.send('Input.dispatchTouchEvent', { type: 'touchStart', touchPoints: [{ x, y }] });
    await m.waitForTimeout(200);
    await cdp.send('Input.dispatchTouchEvent', { type: 'touchMove', touchPoints: [{ x: x + 3, y: y + 2 }] });
    await m.waitForTimeout(600);
    await cdp.send('Input.dispatchTouchEvent', { type: 'touchEnd', touchPoints: [] });
    await m.waitForTimeout(400);
    const r = await m.evaluate(() => ({ opens: window.__opens, visible: !document.getElementById('ctx').hidden, sel: String(getSelection()), us: getComputedStyle(document.querySelector('[data-context-menu]')).userSelect }));
    assert.ok(r.visible, '长按没有打开菜单');
    assert.equal(r.opens, 1, `菜单打开了 ${r.opens} 次`);
    assert.equal(r.sel, '', `长按选中了文字：${r.sel}`);
    assert.equal(r.us, 'none');
    await m.locator('#ctx .ts-menu-item').first().tap(); await m.waitForTimeout(400);
  });
  await check('手机：自动补全的下拉列表不盖住输入框，滚动后仍贴着输入框', async () => {
    const input = m.locator('#city');
    await input.evaluate((el) => el.scrollIntoView({ block: 'center', behavior: 'instant' }));
    await input.tap();
    await m.waitForTimeout(500);
    const rel = () => m.evaluate(() => {
      const i = document.querySelector('#city'), a = i.closest('.ts-input').getBoundingClientRect();
      const l = document.getElementById(i.getAttribute('aria-controls'));
      if (!l || l.hidden) return null;
      const b = l.getBoundingClientRect();
      return { covers: b.top < a.bottom - 1 && b.bottom > a.top + 1 };
    });
    const before = await rel();
    assert.ok(before, '下拉列表没有打开');
    assert.equal(before.covers, false, '打开时盖住了输入框');
    await m.mouse.wheel(0, 150); await m.waitForTimeout(400);
    const after = await rel();
    if (after) assert.equal(after.covers, false, '滚动后盖住了输入框');
    await m.keyboard.press('Escape');
  });

  console.log('中文输入法');
  shotPage = p;
  const cdp = await p.context().newCDPSession(p);
  await check('输入法：拼音组合中不触发自动补全筛选，选定汉字后才筛选', async () => {
    const city = p.locator('#city');
    await city.evaluate((e) => { e.scrollIntoView({ block: 'center', behavior: 'instant' }); e.value = ''; e.dispatchEvent(new Event('input', { bubbles: true })); });
    await city.focus();
    await cdp.send('Input.imeSetComposition', { text: 'zhou', selectionStart: 4, selectionEnd: 4 });
    await p.waitForTimeout(300);
    const during = await p.evaluate(() => { const i = document.querySelector('#city'); const l = document.getElementById(i.getAttribute('aria-controls')); return l && !l.hidden ? l.textContent : ''; });
    assert.ok(!during.includes('没有匹配'), '组合中就按拼音字母筛选了：' + during.slice(0, 30));
    await cdp.send('Input.insertText', { text: '州' });
    await p.waitForTimeout(400);
    const after = await p.evaluate(() => { const i = document.querySelector('#city'); const l = document.getElementById(i.getAttribute('aria-controls')); return [...(l?.querySelectorAll('.ts-option') || [])].map((o) => o.textContent); });
    assert.ok(after.length > 0 && after.every((t) => t.includes('州')), '选定后没有按“州”筛选：' + after.join('、'));
    await p.keyboard.press('Escape');
  });
  const ip = await ctx.newPage();
  await ip.goto(`${BASE}/demo/index.html${SHAPE === 'tile' ? '' : `?shape=${SHAPE}&force`}`);
  await ip.waitForTimeout(800);
  shotPage = ip;
  const icdp = await ip.context().newCDPSession(ip);
  await check('输入法：拼音组合中不计字数，选定汉字后才计数', async () => {
    const name = ip.locator('#f-name');
    await name.evaluate((e) => e.scrollIntoView({ block: 'center', behavior: 'instant' }));
    await name.focus();
    await icdp.send('Input.imeSetComposition', { text: 'tessera', selectionStart: 7, selectionEnd: 7 });
    await ip.waitForTimeout(250);
    assert.equal(await ip.textContent('#f-count'), '0 / 20');
    await icdp.send('Input.insertText', { text: '拼贴' });
    await ip.waitForTimeout(250);
    assert.equal(await ip.textContent('#f-count'), '2 / 20');
  });
  await check('输入法：组合中的回车、Safari 补发的回车（keyCode 229）被拦下，不提交表单', async () => {
    const r = await ip.evaluate(() => {
      const input = document.querySelector('#f-name');
      let reached = 0;
      const count = () => { reached++; };
      input.addEventListener('keydown', count);
      const fire = (init) => { const e = new KeyboardEvent('keydown', { key: 'Enter', bubbles: true, cancelable: true, ...init }); input.dispatchEvent(e); return e.defaultPrevented; };
      const composing = fire({ isComposing: true });
      const safari = fire({ keyCode: 229 });
      const normal = fire({});
      input.removeEventListener('keydown', count);
      return { composing, safari, normal, reached };
    });
    assert.ok(r.composing, '组合中的回车没有被拦下');
    assert.ok(r.safari, 'keyCode 229 的回车没有被拦下');
    assert.ok(!r.normal, '选定汉字后的回车不应被拦下');
    assert.equal(r.reached, 1, '被拦下的回车仍传到了输入框的监听器');
  });
  await ip.close();

  console.log('输入框');
  shotPage = p;
  await check('输入框聚焦时没有双重框（内部原生控件不画焦点框）', async () => {
    const bad = [];
    for (const el of await p.locator('.ts-input > input:visible, .ts-input > textarea:visible, .ts-number input:visible').all()) {
      await el.evaluate((e) => e.scrollIntoView({ block: 'center', behavior: 'instant' }));
      await el.focus();
      const r = await el.evaluate((e) => { const s = getComputedStyle(e); return { id: e.id || e.getAttribute('aria-label') || e.placeholder, outline: s.outlineStyle !== 'none' && parseFloat(s.outlineWidth) > 0 }; });
      if (r.outline) bad.push(r.id);
      await el.evaluate((e) => e.blur());
      await p.keyboard.press('Escape');
    }
    assert.deepEqual(bad, []);
    await p.locator('#ta').evaluate((e) => { e.scrollIntoView({ block: 'center', behavior: 'instant' }); e.focus(); });
    await p.waitForTimeout(200);
  });

  console.log('窄屏');
  for (const [device, pageName] of [['iPhone SE', 'components'], ['iPhone SE', 'index'], ['iPhone 13', 'components'], ['iPhone 13', 'index']]) {
    const ctx2 = await browser.newContext({ ...devices[device] });
    const n = await ctx2.newPage();
    await n.goto(PAGE.replace('/components.html', `/${pageName}.html`));
    await n.waitForTimeout(800);
    shotPage = n;
    await check(`${device}：${pageName === 'index' ? '首页' : '组件总览'}不横向溢出`, async () => {
      const [sw, cw] = await n.evaluate(() => [document.documentElement.scrollWidth, document.documentElement.clientWidth]);
      assert.ok(sw <= cw, `页面宽 ${sw}px，超出屏幕 ${cw}px`);
    });
    await ctx2.close();
  }

  console.log(`\n${passed} 项通过，${failed} 项失败`);
  console.log(`截图：${SHOTS}（${shotNo} 张）`);
  if (errors.length) { console.log('页面报错：', errors); }
  await browser.close();
  process.exit(failed || errors.length ? 1 : 0);
})();
