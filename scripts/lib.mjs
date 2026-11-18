// TesseraUI token 解析库：读取 tokens/*.json，解析引用，计算派生值。
// 无第三方依赖，Node 18+。
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';

export const ROOT = join(dirname(fileURLToPath(import.meta.url)), '..');
const readJSON = (p) => JSON.parse(readFileSync(join(ROOT, p), 'utf8'));

export const THEMES = ['light', 'dark', 'light-hc', 'dark-hc'];
export const baseTheme = (t) => t.replace('-hc', '');

export function loadTokens() {
  const primitives = readJSON('tokens/color.primitives.json');
  const semantic = readJSON('tokens/color.semantic.json');
  const foundation = readJSON('tokens/foundation.json');
  const motion = readJSON('tokens/motion.json');
  const pkg = readJSON('package.json');
  return { primitives, semantic, foundation, motion, version: pkg.version };
}

// ---------- 颜色 ----------

export function parseHex(hex) {
  const h = hex.replace('#', '');
  const n = parseInt(h.length === 3 ? h.split('').map((c) => c + c).join('') : h.slice(0, 6), 16);
  const a = h.length === 8 ? parseInt(h.slice(6), 16) / 255 : 1;
  return { r: (n >> 16) & 255, g: (n >> 8) & 255, b: n & 255, a };
}

function lookupPrimitive(primitives, ref) {
  const path = ref.replace(/^\{|\}$/g, '').split('.');
  let v = primitives;
  for (const k of path) {
    v = v?.[k];
    if (v === undefined) throw new Error(`未知的色板引用 ${ref}`);
  }
  if (typeof v !== 'string') throw new Error(`色板引用 ${ref} 不是颜色`);
  return v;
}

/** 把语义 token 的一个值解析成 {r,g,b,a}，并保留引用信息 */
export function resolveColor(primitives, value) {
  if (value === 'transparent') return { r: 0, g: 0, b: 0, a: 0, ref: 'transparent' };
  if (typeof value === 'string') {
    const hex = value.startsWith('{') ? lookupPrimitive(primitives, value) : value;
    return { ...parseHex(hex), ref: value };
  }
  if (value && typeof value === 'object' && 'color' in value) {
    const base = resolveColor(primitives, value.color);
    return { ...base, a: base.a * value.alpha, ref: `${value.color} @ ${value.alpha}` };
  }
  throw new Error(`无法解析颜色值 ${JSON.stringify(value)}`);
}

/** 返回 { [tokenName]: { light, dark, 'light-hc', 'dark-hc' } }，每项是 {r,g,b,a,ref} */
export function resolveSemanticColors({ primitives, semantic }) {
  const out = {};
  for (const [name, def] of Object.entries(semantic)) {
    if (name.startsWith('$')) continue;
    if (!def.light || !def.dark) throw new Error(`${name} 缺少 light 或 dark`);
    out[name] = {};
    for (const theme of THEMES) {
      const v = def[theme] ?? def[baseTheme(theme)];
      out[name][theme] = resolveColor(primitives, v);
    }
    out[name].$description = def.$description;
    out[name].$hcOverride = { 'light-hc': 'light-hc' in def, 'dark-hc': 'dark-hc' in def };
  }
  return out;
}

const hex2 = (n) => Math.round(n).toString(16).padStart(2, '0').toUpperCase();
export const toHex = (c) => `#${hex2(c.r)}${hex2(c.g)}${hex2(c.b)}`;
export const toHex8 = (c) => `${toHex(c)}${hex2(c.a * 255)}`; // #RRGGBBAA
export const toARGB = (c) => `#${hex2(c.a * 255)}${toHex(c).slice(1)}`; // #AARRGGBB（XAML、Android）
export const toCssColor = (c) =>
  c.a === 1 ? toHex(c) : c.a === 0 ? 'transparent' : `rgb(${c.r} ${c.g} ${c.b} / ${+c.a.toFixed(3)})`;

/** 把半透明颜色叠到不透明底色上 */
export function composite(fg, bg) {
  const a = fg.a;
  return {
    r: fg.r * a + bg.r * (1 - a),
    g: fg.g * a + bg.g * (1 - a),
    b: fg.b * a + bg.b * (1 - a),
    a: 1,
  };
}

function channel(c) {
  const s = c / 255;
  return s <= 0.04045 ? s / 12.92 : ((s + 0.055) / 1.055) ** 2.4;
}
export const luminance = (c) => 0.2126 * channel(c.r) + 0.7152 * channel(c.g) + 0.0722 * channel(c.b);
export function contrast(a, b) {
  const [l1, l2] = [luminance(a), luminance(b)].sort((x, y) => y - x);
  return (l1 + 0.05) / (l2 + 0.05);
}

// ---------- 命名 ----------

export const camel = (name) =>
  name
    .split(/[.\-]/)
    .map((p, i) => (i === 0 ? p : p.charAt(0).toUpperCase() + p.slice(1)))
    .join('');
export const pascal = (name) => {
  const c = camel(name);
  return c.charAt(0).toUpperCase() + c.slice(1);
};
export const kebab = (name) => name.replace(/\./g, '-');

// ---------- 弹簧 ----------

export function springParams({ duration, bounce }) {
  const T = duration / 1000;
  const stiffness = (2 * Math.PI / T) ** 2;
  const damping = (4 * Math.PI * (1 - bounce)) / T;
  return {
    duration: T,
    bounce,
    mass: 1,
    stiffness: +stiffness.toFixed(1),
    damping: +damping.toFixed(1),
    dampingRatio: +(1 - bounce).toFixed(3),
  };
}

/** 数值模拟弹簧从 0 到 1 的阶跃响应，返回采样点和收敛时间（秒） */
export function simulateSpring({ stiffness, damping, mass = 1 }, { epsilon = 0.001, dt = 1 / 2000 } = {}) {
  let x = 0, v = 0, t = 0;
  const samples = [{ t: 0, x: 0 }];
  let settledAt = null;
  while (t < 5) {
    // 半隐式欧拉
    const a = (-stiffness * (x - 1) - damping * v) / mass;
    v += a * dt;
    x += v * dt;
    t += dt;
    samples.push({ t, x });
    const settled = Math.abs(x - 1) < epsilon && Math.abs(v) < epsilon * 10;
    if (settled && settledAt === null) settledAt = t;
    if (!settled) settledAt = null;
    if (settledAt !== null && t - settledAt > 0.05) break;
  }
  return { samples, settle: settledAt ?? t };
}

/** 生成 CSS linear() 缓动函数，用于在纯 CSS 里模拟弹簧 */
export function springToCssLinear(params, points = 40) {
  const { samples, settle } = simulateSpring(params);
  const stops = [];
  for (let i = 0; i <= points; i++) {
    const target = (settle * i) / points;
    const s = samples.reduce((best, p) => (Math.abs(p.t - target) < Math.abs(best.t - target) ? p : best), samples[0]);
    stops.push(i === points ? 1 : +s.x.toFixed(4));
  }
  return { css: `linear(${stops.join(', ')})`, settleMs: Math.round(settle * 1000) };
}
