// 从 icons/source.mjs 生成 SVG 源文件和 Web 用的图标表。
import { mkdirSync, writeFileSync, rmSync } from 'node:fs';
import { join } from 'node:path';
import { ROOT } from './lib.mjs';
import { icons } from '../icons/source.mjs';

const svg = (body, filled) =>
  `<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none"${
    filled ? '' : ' stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"'
  }>${body}</svg>`;

const outDir = join(ROOT, 'icons/svg');
rmSync(outDir, { recursive: true, force: true });
mkdirSync(outDir, { recursive: true });
const all = {};
for (const [name, def] of Object.entries(icons)) {
  all[name] = svg(def.stroke, false);
  if (def.fill) all[`${name}-fill`] = svg(def.fill, true);
}
for (const [name, content] of Object.entries(all)) writeFileSync(join(outDir, `${name}.svg`), content + '\n');
mkdirSync(join(ROOT, 'platforms/web/dist'), { recursive: true });
writeFileSync(
  join(ROOT, 'platforms/web/dist/icons.mjs'),
  `// 由 scripts/build-icons.mjs 生成，请勿手改。\nexport const icons = ${JSON.stringify(all, null, 2)};\nexport default icons;\n`,
);
console.log(`图标：已生成 ${Object.keys(all).length} 个 SVG`);
