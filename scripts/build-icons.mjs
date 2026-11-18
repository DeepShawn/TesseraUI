// 从 icons/source.mjs 生成 SVG 源文件和 Web 用的图标表。
import { mkdirSync, writeFileSync, rmSync } from 'node:fs';
import { join } from 'node:path';
import { ROOT } from './lib.mjs';
import { icons, categories } from '../icons/source.mjs';

// 分类表必须和图标一一对应
const listed = Object.values(categories).flat();
const missing = Object.keys(icons).filter((n) => !listed.includes(n));
const unknown = listed.filter((n) => !icons[n]);
if (missing.length || unknown.length) {
  console.error('图标分类不一致：未分类', missing, '；分类里有但没画', unknown);
  process.exit(1);
}

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
  `// 由 scripts/build-icons.mjs 生成，请勿手改。\nexport const icons = ${JSON.stringify(all, null, 2)};\nexport const categories = ${JSON.stringify(categories, null, 2)};\nexport default icons;\n`,
);
const fills = Object.keys(all).filter((n) => n.endsWith('-fill')).length;
console.log(`图标：已生成 ${Object.keys(all).length} 个 SVG（线性 ${Object.keys(all).length - fills}，实心 ${fills}）`);

// ==================================================================
// 原生平台：把每个图标拆成“路径 + 填充 / 描边”，再生成各平台格式
// ==================================================================
const num = (v) => +Number(v).toFixed(3);
/** 规范化路径字符串：命令与数字之间统一用空格分隔，避免各平台解析器对紧凑写法（如 .5.5）的差异 */
const normPath = (d) => (d.match(/[a-zA-Z]|-?(?:\d*\.\d+|\d+\.?\d*)(?:e-?\d+)?/g) || []).join(' ');
const attr = (tag, name) => { const m = new RegExp(`\\s${name}="([^"]*)"`).exec(tag); return m ? m[1] : null; };
function toShapes(markup, rootStroke) {
  const shapes = [];
  for (const [tag, el] of markup.matchAll(/<(path|circle|rect)\b[^>]*>/g)) {
    let d;
    if (el === 'path') d = attr(tag, 'd');
    else if (el === 'circle') {
      const cx = +attr(tag, 'cx'), cy = +attr(tag, 'cy'), r = +attr(tag, 'r');
      d = `M${num(cx - r)} ${cy}a${r} ${r} 0 1 0 ${num(2 * r)} 0a${r} ${r} 0 1 0 ${num(-2 * r)} 0Z`;
    } else {
      const x = +attr(tag, 'x'), y = +attr(tag, 'y'), w = +attr(tag, 'width'), h = +attr(tag, 'height'), rx = +(attr(tag, 'rx') || 0);
      d = rx
        ? `M${num(x + rx)} ${y}H${num(x + w - rx)}A${rx} ${rx} 0 0 1 ${num(x + w)} ${num(y + rx)}V${num(y + h - rx)}A${rx} ${rx} 0 0 1 ${num(x + w - rx)} ${num(y + h)}H${num(x + rx)}A${rx} ${rx} 0 0 1 ${x} ${num(y + h - rx)}V${num(y + rx)}A${rx} ${rx} 0 0 1 ${num(x + rx)} ${y}Z`
        : `M${x} ${y}h${w}v${h}h${-w}Z`;
    }
    const fillAttr = attr(tag, 'fill'), strokeAttr = attr(tag, 'stroke');
    const fill = fillAttr === 'currentColor';
    const stroke = strokeAttr ? strokeAttr === 'currentColor' : rootStroke && !fill;
    shapes.push({ d: normPath(d), fill, stroke, evenOdd: attr(tag, 'fill-rule') === 'evenodd' });
  }
  return shapes;
}
const shapesOf = {};
for (const [name, def] of Object.entries(icons)) {
  shapesOf[name] = toShapes(def.stroke, true);
  if (def.fill) shapesOf[`${name}-fill`] = toShapes(def.fill, false);
}
const pascalName = (n) => n.split('-').map((p) => p[0].toUpperCase() + p.slice(1)).join('');
const camelName = (n) => { const p = pascalName(n); return p[0].toLowerCase() + p.slice(1); };
const GEN = '由 scripts/build-icons.mjs 从 icons/source.mjs 生成，请勿手改。';
const write = (rel, content) => { const f = join(ROOT, rel); mkdirSync(join(f, '..'), { recursive: true }); writeFileSync(f, content); };

// ---------- Android：Compose ImageVector ----------
{
  const kt = [`// ${GEN}`, 'package com.tesseraui.icons', '', 'import androidx.compose.ui.graphics.vector.ImageVector', '',
    '/** TesseraUI 图标。线性版为默认，实心版加 Fill 后缀；与 Web、Apple、Windows 共用同一份源。 */',
    'object TesseraIcons {'];
  for (const [name, shapes] of Object.entries(shapesOf)) {
    const parts = shapes.map((s) => `P("${s.d}", fill = ${s.fill}, stroke = ${s.stroke}${s.evenOdd ? ', evenOdd = true' : ''})`).join(', ');
    kt.push(`    val ${pascalName(name)}: ImageVector by lazy { build("${name}", ${parts}) }`);
  }
  kt.push('', '    /** 按名称取图标，例如 TesseraIcons.byName("home-fill") */',
    '    fun byName(name: String): ImageVector? = when (name) {',
    ...Object.keys(shapesOf).map((n) => `        "${n}" -> ${pascalName(n)}`),
    '        else -> null', '    }', '',
    `    val names: List<String> = listOf(${Object.keys(shapesOf).map((n) => `"${n}"`).join(', ')})`, '}', '');
  write('platforms/android/tessera/src/main/kotlin/com/tesseraui/icons/TesseraIcons.kt', kt.join('\n'));
}

// ---------- Apple：Asset Catalog（SVG，保留矢量，模板渲染） ----------
{
  const base = 'platforms/apple/Sources/TesseraUI/Resources/TesseraIcons.xcassets';
  rmSync(join(ROOT, base), { recursive: true, force: true });
  write(`${base}/Contents.json`, JSON.stringify({ info: { author: 'xcode', version: 1 } }, null, 2) + '\n');
  for (const [name, content] of Object.entries(all)) {
    // CoreSVG 不认识 currentColor；模板渲染时颜色由前景色决定，这里写成黑色即可
    write(`${base}/${name}.imageset/${name}.svg`, content.replaceAll('currentColor', '#000000') + '\n');
    write(`${base}/${name}.imageset/Contents.json`, JSON.stringify({
      images: [{ filename: `${name}.svg`, idiom: 'universal' }],
      info: { author: 'xcode', version: 1 },
      properties: { 'preserves-vector-representation': true, 'template-rendering-intent': 'template' },
    }, null, 2) + '\n');
  }
  const swift = [`// ${GEN}`, 'import SwiftUI', '',
    '/** TesseraUI 图标名称。用法：TesseraIcon(.home) 或 Image(tessera: .homeFill) */',
    'public enum TesseraIconName: String, CaseIterable, Sendable {',
    ...Object.keys(all).map((n) => `    case ${camelName(n)} = "${n}"`), '}', ''];
  write('platforms/apple/Sources/TesseraUI/Generated/TesseraIconName.swift', swift.join('\n'));
}

// ---------- Windows：C# 路径数据（由 TesseraIcon 控件渲染） ----------
{
  const cs = [`// ${GEN}`, 'using System.Collections.Generic;', '', 'namespace TesseraUI', '{',
    '    /// <summary>图标路径数据。每个图标由若干路径组成，分别标明填充或描边；描边宽 1.5，圆头圆角。</summary>',
    '    public static class TesseraIconData', '    {',
    '        public readonly record struct Shape(string Data, bool Fill, bool Stroke);', '',
    '        public static readonly IReadOnlyDictionary<string, Shape[]> All = new Dictionary<string, Shape[]>', '        {'];
  for (const [name, shapes] of Object.entries(shapesOf)) {
    // XAML 路径语法：F0 表示奇偶填充，F1 表示非零填充
    cs.push(`            ["${name}"] = new[] { ${shapes.map((s) => `new Shape("${s.evenOdd ? 'F0 ' : 'F1 '}${s.d}", ${s.fill}, ${s.stroke})`).join(', ')} },`);
  }
  cs.push('        };', '    }', '}', '');
  write('platforms/windows/TesseraUI/Generated/TesseraIconData.g.cs', cs.join('\n'));
}
console.log(`图标：已导出 Android ImageVector、Apple Asset Catalog、Windows 路径数据（各 ${Object.keys(shapesOf).length} 个）`);
