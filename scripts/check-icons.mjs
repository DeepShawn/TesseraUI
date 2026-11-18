// 图标规范检查：画布 24、线宽 1.5、圆头圆角、单色 currentColor、实心版命名。
import { readdirSync, readFileSync } from 'node:fs';
import { join } from 'node:path';
import { ROOT } from './lib.mjs';

const dir = join(ROOT, 'icons/svg');
const files = readdirSync(dir).filter((f) => f.endsWith('.svg'));
const errors = [];
for (const f of files) {
  const s = readFileSync(join(dir, f), 'utf8');
  const name = f.replace('.svg', '');
  if (!/^[a-z0-9]+(-[a-z0-9]+)*$/.test(name)) errors.push(`${f}: 命名必须是小写英文加短横线`);
  if (!s.includes('viewBox="0 0 24 24"')) errors.push(`${f}: 画布必须是 24×24`);
  const colors = [...s.matchAll(/(?:fill|stroke)="([^"]+)"/g)].map((m) => m[1]).filter((c) => !['none', 'currentColor'].includes(c));
  if (colors.length) errors.push(`${f}: 只能用 currentColor，发现 ${colors.join(', ')}`);
  if (name.endsWith('-fill')) {
    if (!files.includes(name.replace(/-fill$/, '') + '.svg')) errors.push(`${f}: 缺少对应的线性版`);
  } else {
    if (!s.includes('stroke-width="1.5"')) errors.push(`${f}: 线宽必须是 1.5`);
    if (!s.includes('stroke-linecap="round"') || !s.includes('stroke-linejoin="round"')) errors.push(`${f}: 线头和转角必须是圆的`);
  }
  // 坐标不能超出 2–22 的安全区（粗略检查：绝对坐标的数字）
  for (const m of s.matchAll(/\s(?:cx|cy|x|y)="([\d.]+)"/g)) {
    const v = +m[1];
    if (v < 2 || v > 22) errors.push(`${f}: 坐标 ${v} 超出 2–22 安全区`);
  }
}
if (errors.length) {
  console.error(errors.join('\n'));
  process.exit(1);
}
console.log(`✔ 图标检查通过（${files.length} 个）`);
