// 对比度自动检查（WCAG 2.x）。任何一项不达标就以非零状态退出。
// 用法：node scripts/check-contrast.mjs [--report docs/contrast-report.md]
import { writeFileSync } from 'node:fs';
import { join } from 'node:path';
import { ROOT, THEMES, loadTokens, resolveSemanticColors, composite, contrast, toHex } from './lib.mjs';

const tokens = loadTokens();
const C = resolveSemanticColors(tokens);
const get = (name, theme) => C[name][theme];

const SURFACES = ['bg.canvas', 'bg.surface', 'bg.elevated', 'bg.elevated-2'];
const TEXT = ['text.primary', 'text.secondary', 'text.tertiary', 'accent.text', 'danger.text', 'success.text', 'warning.text'];
const STATUS = ['danger', 'success', 'warning'];
const AA = 4.5;
const AA_LARGE = 3; // 大字、图标、控件边界

const checks = [];
const add = (theme, fg, bg, ratio, min, note = '') =>
  checks.push({ theme, fg, bg, ratio, min, pass: ratio >= min, note });

for (const theme of THEMES) {
  // 1. 所有文字色 × 页面底、卡片、浮层
  for (const s of SURFACES) {
    const bg = get(s, theme);
    for (const t of TEXT) add(theme, t, s, contrast(get(t, theme), bg), AA);
  }
  // 2. 状态文字 × 自己的浅底色（浅底色叠在卡片和浮层上）
  for (const st of [...STATUS, 'accent']) {
    for (const s of ['bg.surface', 'bg.elevated']) {
      const bg = composite(get(`${st}.subtle`, theme), get(s, theme));
      add(theme, `${st}.text`, `${st}.subtle on ${s}`, contrast(get(`${st}.text`, theme), bg), AA);
    }
  }
  // 2b. 颜料文字 × 颜料浅底（头像、分类标签）
  for (const pg of ['cinnabar', 'ochre', 'lapis', 'amethyst', 'olive']) {
    for (const s of ['bg.surface', 'bg.elevated']) {
      const bg = composite(get(`pigment.${pg}-subtle`, theme), get(s, theme));
      add(theme, `pigment.${pg}-text`, `pigment.${pg}-subtle on ${s}`, contrast(get(`pigment.${pg}-text`, theme), bg), AA);
    }
  }
  // 2c. 反色砖（提示消息、工具提示）
  add(theme, 'text.inverse', 'bg.inverse', contrast(get('text.inverse', theme), get('bg.inverse', theme)), AA);
  // 3. 文字放在输入框底色上（输入框底色叠在卡片和页面底上）
  for (const s of ['bg.canvas', 'bg.surface', 'bg.elevated']) {
    const bg = composite(get('fill.default', theme), get(s, theme));
    for (const t of ['text.primary', 'text.secondary']) {
      add(theme, t, `fill.default on ${s}`, contrast(get(t, theme), bg), AA);
    }
  }
  // 4. 实色按钮上的白字，含悬停、按下叠加层
  for (const st of ['accent', ...STATUS]) {
    const solid = get(`${st}.solid`, theme);
    add(theme, 'text.on-solid', `${st}.solid`, contrast(get('text.on-solid', theme), solid), AA);
    const hover = composite(get('state.solid-hover', theme), solid);
    add(theme, 'text.on-solid', `${st}.solid + hover`, contrast(get('text.on-solid', theme), hover), AA);
    const pressed = composite(get('state.solid-pressed', theme), solid);
    add(theme, 'text.on-solid', `${st}.solid + pressed`, contrast(get('text.on-solid', theme), pressed), AA);
  }
  // 5. 键盘焦点框 × 底色（非文字，3:1）
  for (const s of SURFACES) {
    add(theme, 'focus.ring', s, contrast(get('focus.ring', theme), get(s, theme)), AA_LARGE, '非文字');
  }
  // 6. 实色控件（开关开启、复选框选中）对底色，以及上面的白色勾号/滑块（非文字，3:1）
  for (const s of SURFACES) {
    add(theme, 'accent.control', s, contrast(get('accent.control', theme), get(s, theme)), AA_LARGE, '非文字');
  }
  add(theme, 'text.on-solid', 'accent.control', contrast(get('text.on-solid', theme), get('accent.control', theme)), AA_LARGE, '勾号');
  // 7. 增强对比度下的控件描边（非文字，3:1）
  if (theme.endsWith('-hc')) {
    for (const s of ['bg.canvas', 'bg.surface']) {
      const border = composite(get('border.field', theme), get(s, theme));
      add(theme, 'border.field', s, contrast(border, get(s, theme)), AA_LARGE, '非文字');
    }
  }
}

const failed = checks.filter((c) => !c.pass);
const fmt = (r) => r.toFixed(2);

// 文本报告
let md = `# 对比度报告\n\n> 由 \`npm run check:contrast\` 自动生成，请勿手改。标准：文字 ≥ ${AA}:1；焦点框、实色控件、控件描边 ≥ ${AA_LARGE}:1。\n> 占位文字、禁用文字（\`text.tertiary\`）按 WCAG 规定不在检查范围内。\n\n`;
md += `共 ${checks.length} 项，${failed.length ? `**${failed.length} 项未通过**` : '全部通过'}。\n\n`;
for (const theme of THEMES) {
  md += `## ${theme}\n\n| 前景 | 背景 | 对比度 | 要求 | 结果 |\n|---|---|---|---|---|\n`;
  for (const c of checks.filter((x) => x.theme === theme)) {
    md += `| \`${c.fg}\` | \`${c.bg}\` | ${fmt(c.ratio)} | ${c.min} | ${c.pass ? '✅' : '❌'} |\n`;
  }
  md += '\n';
}

const reportArg = process.argv.indexOf('--report');
if (reportArg > -1) writeFileSync(join(ROOT, process.argv[reportArg + 1]), md);

for (const theme of THEMES) {
  const list = checks.filter((c) => c.theme === theme);
  const worst = list.reduce((a, b) => (a.ratio < b.ratio ? a : b));
  console.log(`${theme.padEnd(9)} ${list.length} 项，最低 ${fmt(worst.ratio)}（${worst.fg} on ${worst.bg}）`);
}
if (failed.length) {
  console.error(`\n✖ ${failed.length} 项未达标：`);
  for (const f of failed) console.error(`  [${f.theme}] ${f.fg} on ${f.bg}: ${fmt(f.ratio)} < ${f.min}`);
  process.exit(1);
}
console.log(`\n✔ 对比度检查全部通过（${checks.length} 项）`);
