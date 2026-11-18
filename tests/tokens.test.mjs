import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync, existsSync } from 'node:fs';
import { join } from 'node:path';
import { ROOT, THEMES, loadTokens, resolveSemanticColors, springParams, simulateSpring, contrast, parseHex } from '../scripts/lib.mjs';

const tokens = loadTokens();
const colors = resolveSemanticColors(tokens);
const read = (p) => readFileSync(join(ROOT, p), 'utf8');

test('弹簧参数与规范表一致', () => {
  const expected = { snappy: [439, 36], smooth: [247, 31], bouncy: [158, 19] };
  for (const [k, [stiffness, damping]] of Object.entries(expected)) {
    const s = springParams(tokens.motion.spring[k]);
    assert.equal(Math.round(s.stiffness), stiffness, `${k} 刚度`);
    assert.equal(Math.round(s.damping), damping, `${k} 阻尼`);
  }
});

test('smooth 弹簧不回弹，bouncy 有回弹', () => {
  const peak = (k) => Math.max(...simulateSpring(springParams(tokens.motion.spring[k])).samples.map((p) => p.x));
  assert.ok(peak('smooth') <= 1.0001);
  assert.ok(peak('bouncy') > 1.01);
});

test('每个语义颜色在四种外观下都有值', () => {
  for (const [name, v] of Object.entries(colors)) {
    for (const t of THEMES) assert.ok(v[t] && Number.isFinite(v[t].a), `${name} / ${t}`);
  }
});

test('实色底在浅色、深色下用同一个值', () => {
  for (const n of ['accent.solid', 'danger.solid', 'success.solid', 'warning.solid']) {
    assert.deepEqual({ ...colors[n].light, ref: 0 }, { ...colors[n].dark, ref: 0 }, n);
  }
});

test('对比度抽查：主文字在页面底上 ≥ 4.5', () => {
  for (const t of THEMES) assert.ok(contrast(colors['text.primary'][t], colors['bg.canvas'][t]) >= 4.5);
});

test('四端产物都已生成且包含全部颜色', () => {
  const css = read('platforms/web/dist/tokens.css');
  const swift = read('platforms/apple/Sources/TesseraUI/Generated/TesseraTokens.swift');
  const kt = read('platforms/android/tessera/src/main/kotlin/com/tesseraui/TesseraTokens.kt');
  const xaml = read('platforms/windows/TesseraUI/Themes/TesseraTokens.xaml');
  for (const name of Object.keys(colors)) {
    assert.ok(css.includes(`--ts-${name.replace(/\./g, '-')}:`), `CSS 缺 ${name}`);
    const camel = name.split(/[.\-]/).map((p, i) => (i ? p[0].toUpperCase() + p.slice(1) : p)).join('');
    assert.ok(swift.includes(`static let ${camel} =`), `Swift 缺 ${name}`);
    assert.ok(kt.includes(`val ${camel}: Color`), `Kotlin 缺 ${name}`);
    const pascal = camel[0].toUpperCase() + camel.slice(1);
    assert.equal(xaml.split(`x:Key="Tessera${pascal}Brush"`).length - 1, 3, `XAML 三个主题字典都要有 ${name}`);
  }
});

test('产物版本号与 package.json 一致', () => {
  assert.ok(read('platforms/web/dist/tokens.css').includes(`v${tokens.version}`));
  assert.ok(read('platforms/windows/TesseraUI/TesseraUI.csproj').includes(`<Version>${tokens.version}</Version>`));
});

test('深色模式下浅色值未被误用：bg.canvas 深色是深色', () => {
  assert.ok(parseHex('#111113').r === colors['bg.canvas'].dark.r);
  assert.ok(existsSync(join(ROOT, 'icons/svg/heart-fill.svg')));
});
