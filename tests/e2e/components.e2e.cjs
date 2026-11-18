// 组件交互端到端测试（需要 Playwright 与 Chromium）。
// 用法：先在仓库根目录启动静态服务器（npx http-server -p 5173 .），再运行 node tests/e2e/components.e2e.cjs
const { chromium, devices } = require('playwright');
const assert = require('node:assert/strict');

const BASE = process.env.TESSERA_BASE || 'http://localhost:5173';
let passed = 0, failed = 0;
async function check(name, fn) {
  try { await fn(); passed++; console.log(`  ✔ ${name}`); }
  catch (e) { failed++; console.log(`  ✖ ${name}\n    ${e.message.split('\n')[0]}`); }
}

(async () => {
  const browser = await chromium.launch();
  const ctx = await browser.newContext({ viewport: { width: 1400, height: 900 }, permissions: ['clipboard-read', 'clipboard-write'] });
  const p = await ctx.newPage();
  const errors = [];
  p.on('pageerror', (e) => errors.push(e.message));
  p.on('console', (m) => { if (m.type() === 'error') errors.push(m.text()); });
  await p.goto(`${BASE}/demo/components.html`);
  await p.waitForTimeout(1500);
  const esc = () => p.keyboard.press('Escape');

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
  await check('分页：点击与折叠省略号', async () => {
    const pg = p.locator('.ts-pagination').first();
    await pg.locator('button[aria-label="第 7 页"]').click();
    assert.equal(await pg.locator('[aria-current="page"]').textContent(), '7');
    await pg.locator('button[aria-label="第 20 页"]').click();
    assert.deepEqual(await pg.locator('.ts-page').allTextContents(), ['', '1', '…', '16', '17', '18', '19', '20', '']);
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
    await p.hover('[data-tooltip="下载"]');
    await p.waitForTimeout(700);
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

  console.log('手机');
  const m = await browser.newPage({ ...devices['iPhone 13'] });
  m.on('pageerror', (e) => errors.push('mobile: ' + e.message));
  await m.goto(`${BASE}/demo/components.html`);
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

  console.log(`\n${passed} 项通过，${failed} 项失败`);
  if (errors.length) { console.log('页面报错：', errors); }
  await browser.close();
  process.exit(failed || errors.length ? 1 : 0);
})();
