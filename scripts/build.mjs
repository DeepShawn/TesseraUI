// TesseraUI token 构建：tokens/*.json → 四端平台文件。
// 用法：node scripts/build.mjs
import { mkdirSync, writeFileSync, readFileSync, readdirSync, rmSync } from 'node:fs';
import { dirname, join, relative } from 'node:path';
import {
  ROOT, THEMES, loadTokens, resolveSemanticColors, springParams, springToCssLinear,
  toHex, toHex8, toARGB, toCssColor, camel, pascal, kebab,
} from './lib.mjs';

const tokens = loadTokens();
const { foundation: F, motion: M, version } = tokens;
const colors = resolveSemanticColors(tokens);
const springs = Object.fromEntries(Object.entries(M.spring).map(([k, v]) => [k, { ...springParams(v), default: !!v.default }]));

const HEADER = (c = '//') => `${c} TesseraUI v${version} — 由 scripts/build.mjs 从 tokens/*.json 生成，请勿手改。`;
const written = [];
function emit(path, content) {
  const full = join(ROOT, path);
  mkdirSync(dirname(full), { recursive: true });
  writeFileSync(full, content.endsWith('\n') ? content : content + '\n');
  written.push(relative(ROOT, full));
}

// 阴影：深色模式不透明度统一改为 40%
const shadowLayers = (level, theme) =>
  F.shadow[level].map((l) => ({ ...l, alpha: theme.startsWith('dark') ? F.shadow.$darkAlpha : l.alpha }));
const SHADOW_LEVELS = ['1', '2', '3'];
const textStyles = Object.entries(F.font.style);
const weightNum = (w) => F.font.weight[w];

// ==================================================================
// Web：CSS 自定义属性 + ESM + JSON
// ==================================================================
function buildWeb() {
  const colorBlock = (theme, onlyOverrides = false) =>
    Object.entries(colors)
      .filter(([, v]) => !onlyOverrides || v.$hcOverride[theme])
      .map(([name, v]) => `  --ts-${kebab(name)}: ${toCssColor(v[theme])};`)
      .join('\n');
  const shadowBlock = (theme) =>
    SHADOW_LEVELS.map(
      (lv) => {
        const c = colors['shadow.color'][theme];
        return `  --ts-shadow-${lv}: ${shadowLayers(lv, theme).map((l) => `0 ${l.y}px ${l.blur}px rgb(${c.r} ${c.g} ${c.b} / ${l.alpha})`).join(', ')};`;
      },
    ).join('\n');

  const family = (list) => list.map((f) => (/[\s]/.test(f) && !f.includes('-apple') ? `"${f}"` : f)).join(', ');
  const sans = family([...F.font.family.latin, ...F.font.family.cjk.web, ...F.font.family.fallback.web]);
  const display = family([...F.font.family.display, ...F.font.family.cjkSerif.web, ...F.font.family.displayFallback.web]);
  const fam = (s) => (s.family === 'display' ? 'var(--ts-font-serif)' : 'var(--ts-font-sans)');

  const springCss = Object.entries(springs).map(([k, s]) => {
    const { css, settleMs } = springToCssLinear(s);
    return `  --ts-spring-${k}: ${css};\n  --ts-spring-${k}-duration: ${settleMs}ms;`;
  });

  const lines = [
    `/* ${HEADER('').slice(1)} */`,
    '',
    '/* ---------- 非颜色 token ---------- */',
    ':root {',
    `  --ts-font-sans: ${sans};`,
    `  --ts-font-serif: ${display};`,
    `  --ts-font-serif-axes: "SOFT" ${F.font.displayAxes.SOFT}, "WONK" ${F.font.displayAxes.WONK};`,
    `  --ts-font-mono: ${family(F.font.family.mono)};`,
    `  --ts-font-weight-regular: ${F.font.weight.regular};`,
    `  --ts-font-weight-semibold: ${F.font.weight.semibold};`,
    ...textStyles.map(
      ([k, s]) => `  --ts-font-${k}: ${weightNum(s.weight)} ${s.size}px/${s.lineHeight}px ${fam(s)};\n  --ts-tracking-${k}: ${s.tracking}em;`,
    ),
    `  --ts-line-height-reading: ${F.font.readingLineHeight};`,
    ...Object.entries(F.space).map(([k, v]) => `  --ts-space-${k}: ${v}px;`),
    ...Object.entries(F.size).map(([k, v]) => `  --ts-size-${k}: ${v}px;`),
    ...Object.entries(F.radius).map(([k, v]) => `  --ts-radius-${k}: ${v}px;`),
    `  --ts-border-width: ${F.border.width}px;`,
    `  --ts-focus-ring-width: ${F.focus.ringWidth}px;`,
    `  --ts-focus-ring-offset: ${F.focus.ringOffset}px;`,
    `  --ts-focus-glow-width: ${F.focus.glowWidth}px;`,
    `  --ts-opacity-disabled: ${F.opacity.disabled};`,
    `  --ts-glass-blur: ${F.material.glass.blur}px;`,
    `  --ts-glass-saturation: ${F.material.glass.saturation * 100}%;`,
    `  --ts-tile-grout: ${F.tile.grout}px;`,
    `  --ts-tile-edge: ${F.tile.edge}px;`,
    `  --ts-round-pill: ${F.shape.round.pill}px;`,
    `  --ts-round-card: ${F.shape.round.card}px;`,
    `  --ts-round-panel: ${F.shape.round.panel}px;`,
    `  --ts-round-row: ${F.shape.round.row}px;`,
    `  --ts-round-menu: ${F.shape.round.menu}px;`,
    `  --ts-round-gap: ${F.shape.round.gap}px;`,
    `  --ts-round-press-scale: ${F.shape.round.pressScale};`,
    `  --ts-flow-control: ${F.shape.flow.control}px;`,
    `  --ts-flow-card: ${F.shape.flow.card}px;`,
    `  --ts-flow-panel: ${F.shape.flow.panel}px;`,
    `  --ts-flow-row: ${F.shape.flow.row}px;`,
    `  --ts-flow-menu: ${F.shape.flow.menu}px;`,
    `  --ts-flow-press-scale: ${F.shape.flow.pressScale};`,
    `  --ts-flow-ripple-duration: ${F.shape.flow.rippleDuration}ms;`,
    `  --ts-flow-splash-duration: ${F.shape.flow.splashDuration}ms;`,
    ...Object.entries(F.z).map(([k, v]) => `  --ts-z-${k}: ${v};`),
    ...Object.entries(M.duration).map(([k, v]) => `  --ts-duration-${k}: ${v}ms;`),
    ...Object.entries(M.ease).map(
      ([k, v]) => `  --ts-ease-${k}: ${Array.isArray(v) ? `cubic-bezier(${v.join(', ')})` : v};`,
    ),
    ...springCss,
    `  --ts-press-sink: ${M.press.sink}px;`,
    `  --ts-press-duration: ${M.press.duration}ms;`,
    `  --ts-settle-lift: ${M.settle.lift}px;`,
    `  --ts-settle-scale: ${M.settle.scale};`,
    '}',
    '',
    '/* ---------- 颜色：浅色（默认） ---------- */',
    ':root {',
    '  color-scheme: light;',
    colorBlock('light'),
    shadowBlock('light'),
    '}',
    '',
    '/* ---------- 颜色：深色（跟随系统，或 data-theme="dark"） ---------- */',
    '@media (prefers-color-scheme: dark) {',
    '  :root:not([data-theme="light"]) {',
    '    color-scheme: dark;',
    colorBlock('dark').replace(/^/gm, '  '),
    shadowBlock('dark').replace(/^/gm, '  '),
    '  }',
    '}',
    ':root[data-theme="dark"] {',
    '  color-scheme: dark;',
    colorBlock('dark'),
    shadowBlock('dark'),
    '}',
    '',
    '/* ---------- 增强对比度（跟随系统，或 data-contrast="more"） ---------- */',
    '@media (prefers-contrast: more) {',
    `  :root:not([data-contrast="normal"]) {\n${colorBlock('light-hc', true).replace(/^/gm, '  ')}\n  }`,
    `  :root[data-theme="dark"]:not([data-contrast="normal"]) {\n${colorBlock('dark-hc', true).replace(/^/gm, '  ')}\n  }`,
    '}',
    '@media (prefers-contrast: more) and (prefers-color-scheme: dark) {',
    `  :root:not([data-theme="light"]):not([data-contrast="normal"]) {\n${colorBlock('dark-hc', true).replace(/^/gm, '  ')}\n  }`,
    '}',
    `:root[data-contrast="more"] {\n${colorBlock('light-hc', true)}\n}`,
    `:root[data-contrast="more"][data-theme="dark"] {\n${colorBlock('dark-hc', true)}\n}`,
    '@media (prefers-color-scheme: dark) {',
    `  :root[data-contrast="more"]:not([data-theme="light"]) {\n${colorBlock('dark-hc', true).replace(/^/gm, '  ')}\n  }`,
    '}',
  ];
  emit('platforms/web/dist/tokens.css', lines.join('\n'));

  // 扁平 JSON：给设计工具、文档站、测试用
  const flat = { version, color: {}, shadow: {}, spring: springs, foundation: F, motion: M };
  for (const theme of THEMES) {
    flat.color[theme] = Object.fromEntries(Object.entries(colors).map(([n, v]) => [n, toHex8(v[theme])]));
    flat.shadow[theme] = Object.fromEntries(SHADOW_LEVELS.map((lv) => [lv, shadowLayers(lv, theme)]));
  }
  // 组件样式和运行时：tessera.css = tokens + 组件
  rmSync(join(ROOT, 'platforms/web/dist/modules'), { recursive: true, force: true });
  // 组件样式按文件名顺序合并：00-base、10-forms……
  const cssDir = join(ROOT, 'platforms/web/src/css');
  const componentsCss = readdirSync(cssDir).filter((f) => f.endsWith('.css')).sort()
    .map((f) => `/* ===== ${f} ===== */\n` + readFileSync(join(cssDir, f), 'utf8')).join('\n');
  emit('platforms/web/dist/tessera.css', lines.join('\n') + '\n\n' + componentsCss);
  // 运行时脚本：入口、核心与各组件模块原样复制（保持相对 import 路径）
  const srcDir = join(ROOT, 'platforms/web/src');
  const jsFiles = [
    ...readdirSync(srcDir).filter((f) => f.endsWith('.js')),
    ...readdirSync(join(srcDir, 'modules')).filter((f) => f.endsWith('.js')).map((f) => `modules/${f}`),
  ];
  for (const f of jsFiles) emit(`platforms/web/dist/${f}`, `${HEADER()}\n` + readFileSync(join(srcDir, f), 'utf8'));

  emit('platforms/web/dist/tokens.json', JSON.stringify(flat, null, 2));
  emit(
    'platforms/web/dist/tokens.mjs',
    `${HEADER()}\nexport const tokens = ${JSON.stringify(flat, null, 2)};\nexport default tokens;`,
  );
}

// ==================================================================
// Apple：SwiftUI（iOS 17 / macOS 14 起）
// ==================================================================
function buildApple() {
  const rgba = (c) => `0x${toHex8(c).slice(1)}`;
  const colorLines = Object.entries(colors).map(([name, v]) => {
    const doc = v.$description ? `    /// ${v.$description}\n` : '';
    return `${doc}    public static let ${camel(name)} = Color.tessera(light: ${rgba(v.light)}, dark: ${rgba(v.dark)}, lightHC: ${rgba(v['light-hc'])}, darkHC: ${rgba(v['dark-hc'])})`;
  });
  const swiftKey = (k) => (/^\d/.test(k) ? `s${k}` : camel(k)); // Swift 标识符不能以数字开头
  const shadowSwift = (lv) => {
    const L = shadowLayers(lv, 'light'), D = shadowLayers(lv, 'dark');
    return `    public static let level${lv} = TesseraShadow(layers: [${L.map((l, i) => `.init(y: ${l.y}, blur: ${l.blur}, lightOpacity: ${l.alpha}, darkOpacity: ${D[i].alpha})`).join(', ')}])`;
  };
  const ease = (v) => (Array.isArray(v) ? v : null);

  const swift = `${HEADER()}
import SwiftUI

// MARK: - 颜色

public enum TesseraColor {
${colorLines.join('\n')}
}

// MARK: - 字体

public enum TesseraFontFamily {
    /// 标题：软衬线 ${F.font.family.display[0]}（可变字体，SOFT=${F.font.displayAxes.SOFT}）。需加入 App 并在 Info.plist 注册。
    public static let display = "${F.font.family.display[0]}"
    /// 正文：${F.font.family.latin[0]}。需加入 App 并在 Info.plist 注册；未注册时回退到系统字体。
    public static let text = "${F.font.family.latin[0]}"
    /// 标题里的中文：系统自带的宋体。
    public static let cjkSerif = "${F.font.family.cjkSerif.apple[0]}"
    /// 正文里的中文：系统自动回退到苹方。
    public static let cjk = "${F.font.family.cjk.apple[0]}"
}

public struct TesseraTextStyle: Sendable {
    public enum Family: Sendable { case display, text }
    public let size: CGFloat
    public let lineHeight: CGFloat
    public let weight: Font.Weight
    public let family: Family
    /// 字距，单位 em
    public let tracking: CGFloat
    public let relativeTo: Font.TextStyle
}

public extension TesseraTextStyle {
${textStyles
  .map(([k, s]) => {
    const rel = { display: '.largeTitle', 'title-1': '.title', 'title-2': '.title2', 'title-3': '.title3', body: '.body', 'body-sm': '.subheadline', label: '.subheadline', caption: '.caption', overline: '.caption2' }[k];
    return `    static let ${camel(k)} = TesseraTextStyle(size: ${s.size}, lineHeight: ${s.lineHeight}, weight: ${s.weight === 'semibold' ? '.semibold' : '.regular'}, family: .${s.family}, tracking: ${s.tracking}, relativeTo: ${rel})`;
  })
  .join('\n')}
}

// MARK: - 间距、尺寸、圆角

public enum TesseraSpace {
${Object.entries(F.space).map(([k, v]) => `    public static let ${swiftKey(k)}: CGFloat = ${v}`).join('\n')}
}

public enum TesseraSize {
${Object.entries(F.size).map(([k, v]) => `    public static let ${swiftKey(k)}: CGFloat = ${v}`).join('\n')}
}

/// 使用时搭配 \`RoundedRectangle(cornerRadius:style: .continuous)\`，即连续曲率圆角。
public enum TesseraRadius {
${Object.entries(F.radius).map(([k, v]) => `    public static let ${swiftKey(k)}: CGFloat = ${v}`).join('\n')}
}

/// 拼贴：成组的砖之间留缝，实色砖底部有砖厚
public enum TesseraTile {
    public static let grout: CGFloat = ${F.tile.grout}
    public static let edge: CGFloat = ${F.tile.edge}
}

/// 圆融外观的形状数值（\`.tesseraShape(.round)\` 时使用）
public enum TesseraRound {
    public static let pill: CGFloat = ${F.shape.round.pill}
    public static let card: CGFloat = ${F.shape.round.card}
    public static let panel: CGFloat = ${F.shape.round.panel}
    public static let row: CGFloat = ${F.shape.round.row}
    public static let menu: CGFloat = ${F.shape.round.menu}
    public static let gap: CGFloat = ${F.shape.round.gap}
    public static let groupGap: CGFloat = ${F.shape.round.groupGap}
    public static let pressScale: CGFloat = ${F.shape.round.pressScale}
}

/// 流韵外观的形状与动效数值（\`.tesseraShape(.flow)\` 时使用）
public enum TesseraFlow {
    public static let control: CGFloat = ${F.shape.flow.control}
    public static let card: CGFloat = ${F.shape.flow.card}
    public static let panel: CGFloat = ${F.shape.flow.panel}
    public static let row: CGFloat = ${F.shape.flow.row}
    public static let menu: CGFloat = ${F.shape.flow.menu}
    public static let pressScale: CGFloat = ${F.shape.flow.pressScale}
    public static let rippleDuration: Double = ${F.shape.flow.rippleDuration / 1000}
    public static let splashDuration: Double = ${F.shape.flow.splashDuration / 1000}
    public static let minCores = ${F.shape.flow.minCores}
    public static let minMemoryGB = ${F.shape.flow.minMemoryGB}
}

public enum TesseraFocus {
    public static let ringWidth: CGFloat = ${F.focus.ringWidth}
    public static let ringOffset: CGFloat = ${F.focus.ringOffset}
    public static let glowWidth: CGFloat = ${F.focus.glowWidth}
}

public enum TesseraOpacity {
    public static let disabled: Double = ${F.opacity.disabled}
}

// MARK: - 阴影

public struct TesseraShadowLayer: Sendable {
    public let y: CGFloat
    public let blur: CGFloat
    public let lightOpacity: Double
    public let darkOpacity: Double
}

public struct TesseraShadow: Sendable {
    public let layers: [TesseraShadowLayer]
${SHADOW_LEVELS.map(shadowSwift).join('\n')}
}

// MARK: - 动效

public enum TesseraMotion {
${Object.entries(springs)
  .map(([k, s]) => `    /// 刚度 ${s.stiffness}，阻尼 ${s.damping}${s.default ? '（默认）' : ''}\n    public static let ${k} = Animation.spring(duration: ${s.duration}, bounce: ${s.bounce})`)
  .join('\n')}
${Object.entries(M.duration).map(([k, v]) => `    public static let duration${pascal(k)}: Double = ${v / 1000}`).join('\n')}
${Object.entries(M.ease)
  .filter(([, v]) => ease(v))
  .map(([k, v]) => `    public static func ${camel('ease-' + k)}(_ duration: Double) -> Animation { .timingCurve(${v.join(', ')}, duration: duration) }`)
  .join('\n')}
    /// 按下时砖往下沉的距离
    public static let pressSink: CGFloat = ${M.press.sink}
    public static let pressDuration: Double = ${M.press.duration / 1000}
    public static let settleLift: CGFloat = ${M.settle.lift}
    public static let settleScale: CGFloat = ${M.settle.scale}
    public static let flingVelocity: CGFloat = ${M.gesture.flingVelocity}
    public static let staggerStep: Double = ${M.stagger.step / 1000}
    public static let staggerMaxItems = ${M.stagger.maxItems}
    public static let toastDuration: Double = ${M.toast.duration / 1000}
    public static let toastDurationWithAction: Double = ${M.toast.durationWithAction / 1000}
}

// MARK: - 层级

public enum TesseraZ {
${Object.entries(F.z).map(([k, v]) => `    public static let ${camel(k)}: Double = ${v}`).join('\n')}
}
`;
  emit('platforms/apple/Sources/TesseraUI/Generated/TesseraTokens.swift', swift);
}

// ==================================================================
// Android：Jetpack Compose + 传统 View 的 XML 资源
// ==================================================================
function buildAndroid() {
  const pkg = 'com.tesseraui';
  const argb = (c) => `Color(0x${toARGB(c).slice(1)})`;
  const names = Object.keys(colors);
  const set = (theme) =>
    `TesseraColors(\n${names.map((n) => `    ${camel(n)} = ${argb(colors[n][theme])},`).join('\n')}\n)`;
  const ktKey = (k) => (/^\d/.test(k) ? `s${k}` : camel(k));

  const kt = `${HEADER()}
package ${pkg}

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

@Immutable
data class TesseraColors(
${names.map((n) => `    ${colors[n].$description ? `/** ${colors[n].$description} */ ` : ''}val ${camel(n)}: Color,`).join('\n')}
)

val TesseraLightColors = ${set('light')}

val TesseraDarkColors = ${set('dark')}

val TesseraLightHighContrastColors = ${set('light-hc')}

val TesseraDarkHighContrastColors = ${set('dark-hc')}

object TesseraType {
${textStyles
  .map(([k, s]) => `    /** ${s.family === 'display' ? '标题字体（软衬线）' : '正文字体'} */\n    val ${camel(k)} = TextStyle(fontSize = ${s.size}.sp, lineHeight = ${s.lineHeight}.sp, fontWeight = FontWeight.${s.weight === 'semibold' ? 'SemiBold' : 'Normal'}, letterSpacing = ${s.tracking}.em)`)
  .join('\n')}
    const val READING_LINE_HEIGHT = ${F.font.readingLineHeight}f
}

object TesseraSpace {
${Object.entries(F.space).map(([k, v]) => `    val ${ktKey(k)} = ${v}.dp`).join('\n')}
}

object TesseraSize {
${Object.entries(F.size).map(([k, v]) => `    val ${ktKey(k)} = ${v}.dp`).join('\n')}
}

object TesseraRadius {
${Object.entries(F.radius).map(([k, v]) => `    val ${ktKey(k)} = ${v}.dp`).join('\n')}
}

object TesseraOpacity {
    const val DISABLED = ${F.opacity.disabled}f
}

/** 拼贴：成组的砖之间留缝，实色砖底部有砖厚 */
object TesseraTile {
    val grout = ${F.tile.grout}.dp
    val edge = ${F.tile.edge}.dp
}

/** 圆融外观的形状数值（TesseraTheme(shape = TesseraShape.Round) 时使用） */
object TesseraRound {
    val pill = ${F.shape.round.pill}.dp
    val card = ${F.shape.round.card}.dp
    val panel = ${F.shape.round.panel}.dp
    val row = ${F.shape.round.row}.dp
    val menu = ${F.shape.round.menu}.dp
    val gap = ${F.shape.round.gap}.dp
    val groupGap = ${F.shape.round.groupGap}.dp
    const val PRESS_SCALE = ${F.shape.round.pressScale}f
}

/** 流韵外观的形状与动效数值（TesseraTheme(shape = TesseraShape.Flow) 时使用） */
object TesseraFlow {
    val control = ${F.shape.flow.control}.dp
    val card = ${F.shape.flow.card}.dp
    val panel = ${F.shape.flow.panel}.dp
    val row = ${F.shape.flow.row}.dp
    val menu = ${F.shape.flow.menu}.dp
    const val PRESS_SCALE = ${F.shape.flow.pressScale}f
    const val RIPPLE_DURATION = ${F.shape.flow.rippleDuration}
    const val SPLASH_DURATION = ${F.shape.flow.splashDuration}
    const val MIN_CORES = ${F.shape.flow.minCores}
    const val MIN_MEMORY_GB = ${F.shape.flow.minMemoryGB}
}

object TesseraFocus {
    val ringWidth = ${F.focus.ringWidth}.dp
    val ringOffset = ${F.focus.ringOffset}.dp
    val glowWidth = ${F.focus.glowWidth}.dp
}

object TesseraElevation {
    /** 阴影层：y 偏移、模糊半径、浅色不透明度、深色不透明度 */
    data class Layer(val y: Float, val blur: Float, val lightAlpha: Float, val darkAlpha: Float)
${SHADOW_LEVELS.map((lv) => {
    const L = shadowLayers(lv, 'light'), D = shadowLayers(lv, 'dark');
    return `    val level${lv} = listOf(${L.map((l, i) => `Layer(${l.y}f, ${l.blur}f, ${l.alpha}f, ${D[i].alpha}f)`).join(', ')})`;
  }).join('\n')}
}

object TesseraMotion {
${Object.entries(springs)
  .map(([k, s]) => `    /** 时长 ${s.duration}s，回弹 ${s.bounce}${s.default ? '（默认）' : ''} */\n    fun <T> ${k}(): SpringSpec<T> = spring(dampingRatio = ${s.dampingRatio}f, stiffness = ${s.stiffness}f)`)
  .join('\n')}
${Object.entries(M.duration).map(([k, v]) => `    const val DURATION_${k.toUpperCase()} = ${v}`).join('\n')}
${Object.entries(M.ease)
  .map(([k, v]) => `    val Ease${pascal(k)} = ${Array.isArray(v) ? `CubicBezierEasing(${v.map((n) => n + 'f').join(', ')})` : 'LinearEasing'}`)
  .join('\n')}
    /** 按下时砖往下沉的距离（dp） */
    const val PRESS_SINK_DP = ${M.press.sink}f
    const val PRESS_DURATION = ${M.press.duration}
    const val SETTLE_LIFT_DP = ${M.settle.lift}f
    const val SETTLE_SCALE = ${M.settle.scale}f
    const val FLING_VELOCITY_DP = ${M.gesture.flingVelocity}f
    const val STAGGER_STEP = ${M.stagger.step}
    const val STAGGER_MAX_ITEMS = ${M.stagger.maxItems}
    const val TOAST_DURATION = ${M.toast.duration}L
    const val TOAST_DURATION_WITH_ACTION = ${M.toast.durationWithAction}L
}

object TesseraZ {
${Object.entries(F.z).map(([k, v]) => `    const val ${k.toUpperCase()} = ${v}f`).join('\n')}
}
`;
  emit('platforms/android/tessera/src/main/kotlin/com/tesseraui/TesseraTokens.kt', kt);

  // 传统 View 系统：values / values-night
  const xml = (theme) =>
    `<?xml version="1.0" encoding="utf-8"?>\n<!-- ${HEADER('').slice(1)} -->\n<resources>\n${names
      .map((n) => `    <color name="tessera_${kebab(n).replace(/-/g, '_')}">${toARGB(colors[n][theme])}</color>`)
      .join('\n')}\n</resources>`;
  emit('platforms/android/tessera/src/main/res/values/tessera_colors.xml', xml('light'));
  emit('platforms/android/tessera/src/main/res/values-night/tessera_colors.xml', xml('dark'));
  emit(
    'platforms/android/tessera/src/main/res/values/tessera_dimens.xml',
    `<?xml version="1.0" encoding="utf-8"?>\n<!-- ${HEADER('').slice(1)} -->\n<resources>\n${[
      ...Object.entries(F.space).map(([k, v]) => `    <dimen name="tessera_space_${k}">${v}dp</dimen>`),
      ...Object.entries(F.size).map(([k, v]) => `    <dimen name="tessera_size_${k.replace(/-/g, '_')}">${v}dp</dimen>`),
      ...Object.entries(F.radius).map(([k, v]) => `    <dimen name="tessera_radius_${k}">${v}dp</dimen>`),
      ...textStyles.map(([k, s]) => `    <dimen name="tessera_font_${k.replace(/-/g, '_')}">${s.size}sp</dimen>`),
    ].join('\n')}\n</resources>`,
  );
}

// ==================================================================
// Windows：WinUI 3 / XAML ResourceDictionary + C# 动效常量
// ==================================================================
// 增强对比度时 Windows 使用系统的高对比度主题色，这里把每个语义 token 映射到系统色。
const WIN_HC = {
  'bg.inverse': 'SystemColorWindowTextColor',
  'text.inverse': 'SystemColorWindowColor',
  'bg.': 'SystemColorWindowColor',
  'fill.default': 'SystemColorWindowColor',
  'text.primary': 'SystemColorWindowTextColor',
  'text.secondary': 'SystemColorWindowTextColor',
  'text.tertiary': 'SystemColorGrayTextColor',
  'text.on-solid': 'SystemColorHighlightTextColor',
  'border.': 'SystemColorWindowTextColor',
  'accent.text': 'SystemColorHotlightColor',
  'accent.': 'SystemColorHighlightColor',
  '.text': 'SystemColorWindowTextColor',
  '.solid': 'SystemColorHighlightColor',
  '.subtle': 'SystemColorWindowColor',
  'focus.': 'SystemColorHighlightColor',
  selection: 'SystemColorHighlightColor',
  'state.': null, // 叠加层在高对比度下不使用
  'material.': 'SystemColorWindowColor',
  'edge.': null,
  sheen: null,
  'shadow.': null,
};
function winHcFor(name) {
  if (name.startsWith('pigment.')) return name.endsWith('-subtle') ? 'SystemColorWindowColor' : 'SystemColorWindowTextColor';
  for (const [k, v] of Object.entries(WIN_HC)) {
    if (name === k || name.startsWith(k) || (k.startsWith('.') && name.endsWith(k))) return v;
  }
  return 'SystemColorWindowTextColor';
}

function buildWindows() {
  const key = (n) => `Tessera${pascal(n)}`;
  const themeDict = (theme) =>
    Object.entries(colors)
      .map(([n, v]) => `            <Color x:Key="${key(n)}Color">${toARGB(v[theme])}</Color>\n            <SolidColorBrush x:Key="${key(n)}Brush" Color="${toARGB(v[theme])}" />`)
      .join('\n') +
    // 外观相关的画刷：拼贴时列表行是砖面色、列表外框透明；圆融时相反（见 TesseraShapeRound.xaml）
    `\n            <SolidColorBrush x:Key="TesseraListItemBackgroundBrush" Color="${toARGB(colors['bg.surface'][theme])}" />\n            <SolidColorBrush x:Key="TesseraListContainerBrush" Color="Transparent" />`;
  const hcDict = Object.keys(colors)
    .map((n) => {
      const sys = winHcFor(n);
      return sys
        ? `            <StaticResource x:Key="${key(n)}Color" ResourceKey="${sys}" />\n            <SolidColorBrush x:Key="${key(n)}Brush" Color="{ThemeResource ${sys}}" />`
        : `            <Color x:Key="${key(n)}Color">#00000000</Color>\n            <SolidColorBrush x:Key="${key(n)}Brush" Color="Transparent" />`;
    })
    .join('\n') +
    `\n            <SolidColorBrush x:Key="TesseraListItemBackgroundBrush" Color="{ThemeResource SystemColorWindowColor}" />\n            <SolidColorBrush x:Key="TesseraListContainerBrush" Color="Transparent" />`;
  const fontFamily = [...F.font.family.latin, ...F.font.family.cjk.windows, F.font.family.fallback.windows].join(', ');
  const displayFamily = [...F.font.family.display, ...F.font.family.cjkSerif.windows, F.font.family.fallback.windows].join(', ');
  const xaml = `<!-- ${HEADER('').slice(1)} -->
<!-- 用法：在 App.xaml 的 MergedDictionaries 里引用本文件（放在 XamlControlsResources 之后）。 -->
<ResourceDictionary
    xmlns="http://schemas.microsoft.com/winfx/2006/xaml/presentation"
    xmlns:x="http://schemas.microsoft.com/winfx/2006/xaml">

    <ResourceDictionary.ThemeDictionaries>
        <ResourceDictionary x:Key="Light">
${themeDict('light')}
        </ResourceDictionary>
        <ResourceDictionary x:Key="Dark">
${themeDict('dark')}
        </ResourceDictionary>
        <!-- Windows 的“对比度主题”由用户自选配色，组件必须使用系统色，不能用自己的色值。 -->
        <ResourceDictionary x:Key="HighContrast">
${hcDict}
        </ResourceDictionary>
    </ResourceDictionary.ThemeDictionaries>

    <!-- 字体：英文 ${F.font.family.latin[0]}，中文${F.font.family.cjk.windows[0]}，回退 ${F.font.family.fallback.windows} -->
    <FontFamily x:Key="TesseraFontFamily">${fontFamily}</FontFamily>
    <!-- 标题：软衬线 ${F.font.family.display[0]} + 思源宋体（需随应用打包；未打包时回退到微软雅黑） -->
    <FontFamily x:Key="TesseraDisplayFontFamily">${displayFamily}</FontFamily>
    <FontFamily x:Key="TesseraMonoFontFamily">${F.font.family.mono.filter((f) => f !== 'monospace').join(', ')}</FontFamily>
${textStyles
  .map(([k, s]) => `    <Style x:Key="Tessera${pascal(k)}TextStyle" TargetType="TextBlock">
        <Setter Property="FontFamily" Value="{StaticResource ${s.family === 'display' ? 'TesseraDisplayFontFamily' : 'TesseraFontFamily'}}" />
        <Setter Property="FontSize" Value="${s.size}" />
        <Setter Property="CharacterSpacing" Value="${Math.round(s.tracking * 1000)}" />
        <Setter Property="LineHeight" Value="${s.lineHeight}" />
        <Setter Property="LineStackingStrategy" Value="BlockLineHeight" />
        <Setter Property="FontWeight" Value="${s.weight === 'semibold' ? 'SemiBold' : 'Normal'}" />
    </Style>`)
  .join('\n')}

    <!-- 间距、尺寸 -->
${Object.entries(F.space).map(([k, v]) => `    <x:Double x:Key="TesseraSpace${k}">${v}</x:Double>`).join('\n')}
${Object.entries(F.size).map(([k, v]) => `    <x:Double x:Key="TesseraSize${pascal(k)}">${v}</x:Double>`).join('\n')}

    <!-- 圆角。WinUI 的 CornerRadius 是普通圆弧，没有连续曲率；胶囊形用 高度 ÷ 2。 -->
${Object.entries(F.radius).map(([k, v]) => `    <CornerRadius x:Key="TesseraRadius${pascal(k)}">${v}</CornerRadius>`).join('\n')}

    <!-- 拼贴：砖缝、砖厚 -->
    <x:Double x:Key="TesseraTileGrout">${F.tile.grout}</x:Double>
    <x:Double x:Key="TesseraTileEdge">${F.tile.edge}</x:Double>
    <Thickness x:Key="TesseraTileEdgeThickness">0,0,0,${F.tile.edge}</Thickness>
    <!-- 外观：tile（拼贴）。合并 TesseraShapeRound.xaml 后变为 round -->
    <x:String x:Key="TesseraShape">${F.shape.default}</x:String>
    <!-- 随外观变化的数值。控件模板用 ThemeResource 引用，合并 TesseraShapeRound.xaml 后自动换成圆融的值 -->
    <x:Double x:Key="TesseraPressSinkY">${M.press.sink}</x:Double>
    <x:Double x:Key="TesseraPressScale">1</x:Double>
    <x:Double x:Key="TesseraTileEdgeOpacity">1</x:Double>
    <x:Double x:Key="TesseraRoundSheenOpacity">0</x:Double>
    <CornerRadius x:Key="TesseraSwitchTrackRadius">13</CornerRadius>
    <CornerRadius x:Key="TesseraSwitchThumbRadius">10</CornerRadius>
    <CornerRadius x:Key="TesseraCheckboxRadius">7</CornerRadius>
    <CornerRadius x:Key="TesseraBadgeRadius">7</CornerRadius>
    <CornerRadius x:Key="TesseraAvatarRadius">14</CornerRadius>
    <CornerRadius x:Key="TesseraMenuItemRadius">14</CornerRadius>
    <CornerRadius x:Key="TesseraSkeletonRadius">${F.radius.grout}</CornerRadius>
    <CornerRadius x:Key="TesseraListContainerRadius">${F.radius.lg}</CornerRadius>
    <CornerRadius x:Key="TesseraListItemRadius">${F.radius.grout}</CornerRadius>
    <Thickness x:Key="TesseraListItemMargin">0,0,0,${F.tile.grout}</Thickness>
    <Thickness x:Key="TesseraListContainerPadding">0</Thickness>
    <Thickness x:Key="TesseraSurfaceBorderThickness">1</Thickness>
    <Thickness x:Key="TesseraNavBarBorderThickness">0,0,0,1</Thickness>

    <!-- 焦点框：对应 WinUI 的 FocusVisualPrimaryThickness / FocusVisualMargin -->
    <Thickness x:Key="TesseraFocusRingThickness">${F.focus.ringWidth}</Thickness>
    <Thickness x:Key="TesseraFocusRingMargin">-${F.focus.ringOffset + F.focus.ringWidth}</Thickness>
    <x:Double x:Key="TesseraDisabledOpacity">${F.opacity.disabled}</x:Double>
</ResourceDictionary>
`;
  emit('platforms/windows/TesseraUI/Themes/TesseraTokens.xaml', xaml);

  // ---------- 轻量样式：把 WinUI 系统控件的资源键映射到 TesseraUI 颜色 ----------
  // 合并到 App.xaml 后，系统控件（TextBox、ComboBox、CheckBox、菜单、对话框、导航视图等）整体换成 TesseraUI 外观。
  // 高对比度不覆盖，交给系统配色。
  const WINUI_BRUSHES = {
    ApplicationPageBackgroundThemeBrush: 'bg.canvas', SolidBackgroundFillColorBaseBrush: 'bg.canvas',
    LayerFillColorDefaultBrush: 'bg.surface', CardBackgroundFillColorDefaultBrush: 'bg.surface', CardStrokeColorDefaultBrush: 'border.surface',
    TextFillColorPrimaryBrush: 'text.primary', TextFillColorSecondaryBrush: 'text.secondary', TextFillColorTertiaryBrush: 'text.tertiary',
    TextOnAccentFillColorPrimaryBrush: 'text.on-solid', AccentFillColorDefaultBrush: 'accent.solid', AccentTextFillColorPrimaryBrush: 'accent.text',
    ControlFillColorDefaultBrush: 'fill.default', ControlStrokeColorDefaultBrush: 'border.subtle', SubtleFillColorSecondaryBrush: 'state.hover', SubtleFillColorTertiaryBrush: 'state.pressed',
    SmokeFillColorDefaultBrush: 'bg.scrim', SystemFillColorCriticalBrush: 'danger.text', SystemFillColorSuccessBrush: 'success.text', SystemFillColorCautionBrush: 'warning.text',
    TextControlBackground: 'fill.default', TextControlBackgroundPointerOver: 'fill.default', TextControlBackgroundFocused: 'bg.surface', TextControlBackgroundDisabled: 'fill.default',
    TextControlBorderBrush: 'border.field', TextControlBorderBrushPointerOver: 'border.field', TextControlBorderBrushFocused: 'focus.ring',
    TextControlForeground: 'text.primary', TextControlForegroundPointerOver: 'text.primary', TextControlForegroundFocused: 'text.primary',
    TextControlPlaceholderForeground: 'text.tertiary', TextControlPlaceholderForegroundPointerOver: 'text.tertiary', TextControlPlaceholderForegroundFocused: 'text.tertiary',
    TextControlSelectionHighlightColor: 'selection',
    ComboBoxBackground: 'fill.default', ComboBoxBackgroundPointerOver: 'fill.default', ComboBoxBackgroundPressed: 'fill.default', ComboBoxBackgroundFocused: 'bg.surface',
    ComboBoxBorderBrush: 'border.field', ComboBoxBorderBrushPointerOver: 'border.field', ComboBoxForeground: 'text.primary',
    ComboBoxDropDownBackground: 'bg.elevated', ComboBoxDropDownBorderBrush: 'border.subtle',
    CheckBoxCheckBackgroundFillUnchecked: 'fill.default', CheckBoxCheckBackgroundStrokeUnchecked: 'border.strong',
    CheckBoxCheckBackgroundFillChecked: 'accent.control', CheckBoxCheckBackgroundFillCheckedPointerOver: 'accent.control', CheckBoxCheckBackgroundFillCheckedPressed: 'accent.control',
    CheckBoxCheckBackgroundStrokeChecked: 'accent.control', CheckBoxCheckGlyphForegroundChecked: 'text.on-solid',
    RadioButtonOuterEllipseFill: 'fill.default', RadioButtonOuterEllipseStroke: 'border.strong',
    RadioButtonOuterEllipseCheckedFill: 'accent.control', RadioButtonOuterEllipseCheckedStroke: 'accent.control', RadioButtonCheckGlyphFill: 'text.on-solid',
    MenuFlyoutPresenterBackground: 'bg.elevated', MenuFlyoutPresenterBorderBrush: 'border.subtle', MenuFlyoutItemBackgroundPointerOver: 'state.hover',
    MenuFlyoutItemBackgroundPressed: 'state.pressed', MenuFlyoutItemForeground: 'text.primary', MenuFlyoutSeparatorBackground: 'bg.canvas',
    FlyoutPresenterBackground: 'bg.elevated', FlyoutBorderThemeBrush: 'border.subtle',
    ContentDialogBackground: 'bg.elevated', ContentDialogForeground: 'text.primary', ContentDialogBorderBrush: 'border.surface', ContentDialogTopOverlay: 'bg.elevated',
    ListViewItemBackgroundPointerOver: 'state.hover', ListViewItemBackgroundPressed: 'state.pressed', ListViewItemBackgroundSelected: 'accent.subtle',
    ListViewItemBackgroundSelectedPointerOver: 'accent.subtle', ListViewItemForeground: 'text.primary',
    NavigationViewDefaultPaneBackground: 'bg.canvas', NavigationViewExpandedPaneBackground: 'bg.canvas', NavigationViewTopPaneBackground: 'bg.canvas',
    NavigationViewItemBackgroundSelected: 'accent.subtle', NavigationViewItemBackgroundSelectedPointerOver: 'accent.subtle', NavigationViewItemForegroundSelected: 'accent.text',
    NavigationViewSelectionIndicatorForeground: 'accent.solid',
    ProgressBarForeground: 'accent.control', ProgressBarBackground: 'fill.control-off', ProgressRingForegroundThemeBrush: 'accent.control',
    SliderTrackValueFill: 'accent.control', SliderTrackFill: 'fill.control-off',
    TeachingTipBackgroundBrush: 'bg.elevated', InfoBarInformationalSeverityBackgroundBrush: 'accent.subtle',
    InfoBarSuccessSeverityBackgroundBrush: 'success.subtle', InfoBarWarningSeverityBackgroundBrush: 'warning.subtle', InfoBarErrorSeverityBackgroundBrush: 'danger.subtle',
  };
  const themeOverrides = (theme) => Object.entries(WINUI_BRUSHES)
    .map(([k, t]) => `            <SolidColorBrush x:Key="${k}" Color="${toARGB(colors[t][theme])}" />`).join('\n');
  const glaze = tokens.primitives.glaze;
  const accent = { SystemAccentColor: glaze['600'], SystemAccentColorLight1: glaze['500'], SystemAccentColorLight2: glaze['400'], SystemAccentColorLight3: glaze['300'],
    SystemAccentColorDark1: glaze['700'], SystemAccentColorDark2: glaze['800'], SystemAccentColorDark3: glaze['900'] };
  emit('platforms/windows/TesseraUI/Themes/TesseraTheme.xaml', `<!-- ${HEADER('').slice(1)} -->
<!-- 轻量样式：合并在 XamlControlsResources 与 TesseraTokens.xaml 之后，系统控件整体换成 TesseraUI 外观。 -->
<ResourceDictionary
    xmlns="http://schemas.microsoft.com/winfx/2006/xaml/presentation"
    xmlns:x="http://schemas.microsoft.com/winfx/2006/xaml">

    <ResourceDictionary.ThemeDictionaries>
        <ResourceDictionary x:Key="Light">
${themeOverrides('light')}
        </ResourceDictionary>
        <ResourceDictionary x:Key="Dark">
${themeOverrides('dark')}
        </ResourceDictionary>
        <!-- 高对比度：不覆盖，使用用户选择的系统配色 -->
        <ResourceDictionary x:Key="HighContrast" />
    </ResourceDictionary.ThemeDictionaries>

    <!-- 主色：孔雀釉 -->
${Object.entries(accent).map(([k, v]) => `    <Color x:Key="${k}">#FF${v.slice(1)}</Color>`).join('\n')}

    <!-- 圆角：控件 ${F.radius.md}，浮层 ${F.radius.lg} -->
    <CornerRadius x:Key="ControlCornerRadius">${F.radius.md}</CornerRadius>
    <CornerRadius x:Key="OverlayCornerRadius">${F.radius.lg}</CornerRadius>
    <Thickness x:Key="TextControlBorderThemeThicknessFocused">1</Thickness>
</ResourceDictionary>
`);

  // 圆融外观：合并在 TesseraTokens.xaml、TesseraTheme.xaml 之后，覆盖圆角、砖缝和砖厚
  const RD = F.shape.round;
  const roundXaml = `<!-- ${HEADER('').slice(1)} -->
<!-- 圆融外观：胶囊与大圆角，成组控件合成一整块，没有砖缝和分割线。
     用法：在 App.xaml 中紧跟 TesseraTokens.xaml、TesseraTheme.xaml 之后合并本文件；不合并即为默认的“拼贴”外观。
     WinUI 会把超过半高的圆角自动收成半圆，所以 ${RD.pill} 就是胶囊。 -->
<ResourceDictionary
    xmlns="http://schemas.microsoft.com/winfx/2006/xaml/presentation"
    xmlns:x="http://schemas.microsoft.com/winfx/2006/xaml">

    <!-- ThemeDictionaries 必须放在最前面 -->
    <ResourceDictionary.ThemeDictionaries>
        <ResourceDictionary x:Key="Light">
            <SolidColorBrush x:Key="TesseraListItemBackgroundBrush" Color="Transparent" />
            <SolidColorBrush x:Key="TesseraListContainerBrush" Color="${toARGB(colors['bg.surface'].light)}" />
        </ResourceDictionary>
        <ResourceDictionary x:Key="Dark">
            <SolidColorBrush x:Key="TesseraListItemBackgroundBrush" Color="Transparent" />
            <SolidColorBrush x:Key="TesseraListContainerBrush" Color="${toARGB(colors['bg.surface'].dark)}" />
        </ResourceDictionary>
        <ResourceDictionary x:Key="HighContrast">
            <SolidColorBrush x:Key="TesseraListItemBackgroundBrush" Color="Transparent" />
            <SolidColorBrush x:Key="TesseraListContainerBrush" Color="{ThemeResource SystemColorWindowColor}" />
        </ResourceDictionary>
    </ResourceDictionary.ThemeDictionaries>

    <!-- 成组控件内侧不留圆角、不留缝，合成一整块 -->
    <CornerRadius x:Key="TesseraRadiusGrout">0</CornerRadius>
    <x:Double x:Key="TesseraTileGrout">${RD.groupGap}</x:Double>
    <!-- 去掉砖厚 -->
    <x:Double x:Key="TesseraTileEdge">0</x:Double>
    <Thickness x:Key="TesseraTileEdgeThickness">0</Thickness>

    <CornerRadius x:Key="TesseraRadiusSm">${RD.pill}</CornerRadius>
    <CornerRadius x:Key="TesseraRadiusMd">${F.size.md / 2}</CornerRadius>
    <CornerRadius x:Key="TesseraRadiusLg">${RD.menu}</CornerRadius>
    <CornerRadius x:Key="TesseraRadiusXl">${RD.card}</CornerRadius>
    <CornerRadius x:Key="TesseraRadius2xl">${RD.panel}</CornerRadius>
    <CornerRadius x:Key="TesseraRoundRow">${RD.row}</CornerRadius>
    <CornerRadius x:Key="TesseraRoundPill">${RD.pill}</CornerRadius>
    <x:Double x:Key="TesseraRoundGap">${RD.gap}</x:Double>
    <x:Double x:Key="TesseraRoundPressScale">${RD.pressScale}</x:Double>
    <!-- 标记：自定义控件据此切换到圆融外观 -->
    <x:String x:Key="TesseraShape">round</x:String>

    <!-- 按压：轻捏，不下沉；去掉砖厚，实色控件加整面柔光 -->
    <x:Double x:Key="TesseraPressSinkY">0</x:Double>
    <x:Double x:Key="TesseraPressScale">${RD.pressScale}</x:Double>
    <x:Double x:Key="TesseraTileEdgeOpacity">0</x:Double>
    <x:Double x:Key="TesseraRoundSheenOpacity">1</x:Double>
    <CornerRadius x:Key="TesseraSwitchTrackRadius">15</CornerRadius>
    <CornerRadius x:Key="TesseraSwitchThumbRadius">12</CornerRadius>
    <CornerRadius x:Key="TesseraCheckboxRadius">8</CornerRadius>
    <CornerRadius x:Key="TesseraBadgeRadius">9</CornerRadius>
    <CornerRadius x:Key="TesseraAvatarRadius">20</CornerRadius>
    <CornerRadius x:Key="TesseraMenuItemRadius">${RD.menu - 6}</CornerRadius>
    <CornerRadius x:Key="TesseraSkeletonRadius">${RD.pill}</CornerRadius>
    <!-- 列表：整个列表是一整块圆角面，行与行之间没有缝 -->
    <CornerRadius x:Key="TesseraListContainerRadius">${RD.card}</CornerRadius>
    <CornerRadius x:Key="TesseraListItemRadius">${RD.row}</CornerRadius>
    <Thickness x:Key="TesseraListItemMargin">0</Thickness>
    <Thickness x:Key="TesseraListContainerPadding">6</Thickness>
    <!-- 没有描边和分割线 -->
    <Thickness x:Key="TesseraSurfaceBorderThickness">0</Thickness>
    <Thickness x:Key="TesseraNavBarBorderThickness">0</Thickness>

    <!-- 系统控件 -->
    <CornerRadius x:Key="ControlCornerRadius">${F.size.md / 2}</CornerRadius>
    <CornerRadius x:Key="OverlayCornerRadius">${RD.menu}</CornerRadius>
</ResourceDictionary>
`;
  emit('platforms/windows/TesseraUI/Themes/TesseraShapeRound.xaml', roundXaml);

  // 流韵外观：以圆融为基础，圆角介于方砖与胶囊之间，按压轻捏 96%
  const FL = F.shape.flow;
  emit('platforms/windows/TesseraUI/Themes/TesseraShapeFlow.xaml', roundXaml
    .replace('<!-- 圆融外观：胶囊与大圆角，成组控件合成一整块，没有砖缝和分割线。', '<!-- 流韵外观：以圆融为基础（成组控件合成一整块、没有砖缝和分割线），圆角介于方砖与胶囊之间，按压时变成胶囊。\n     流韵的水波、涟漪等动效在 WinUI 上逐步提供，见 README。')
    .replace('紧跟 TesseraTokens.xaml、TesseraTheme.xaml 之后合并本文件；不合并即为默认的“拼贴”外观。', '紧跟 TesseraTokens.xaml、TesseraTheme.xaml 之后合并本文件（不要同时合并 TesseraShapeRound.xaml）。')
    .replace(`<CornerRadius x:Key="TesseraRadiusMd">${F.size.md / 2}</CornerRadius>`, `<CornerRadius x:Key="TesseraRadiusMd">${FL.control}</CornerRadius>`)
    .replace(`<CornerRadius x:Key="TesseraRadiusLg">${RD.menu}</CornerRadius>`, `<CornerRadius x:Key="TesseraRadiusLg">${FL.menu}</CornerRadius>`)
    .replace(`<CornerRadius x:Key="TesseraRadiusXl">${RD.card}</CornerRadius>`, `<CornerRadius x:Key="TesseraRadiusXl">${FL.card}</CornerRadius>`)
    .replace(`<CornerRadius x:Key="TesseraRadius2xl">${RD.panel}</CornerRadius>`, `<CornerRadius x:Key="TesseraRadius2xl">${FL.panel}</CornerRadius>`)
    .replace(`<CornerRadius x:Key="TesseraRoundRow">${RD.row}</CornerRadius>`, `<CornerRadius x:Key="TesseraRoundRow">${FL.row}</CornerRadius>`)
    .replace(`<CornerRadius x:Key="TesseraListContainerRadius">${RD.card}</CornerRadius>`, `<CornerRadius x:Key="TesseraListContainerRadius">${FL.card}</CornerRadius>`)
    .replace(`<CornerRadius x:Key="TesseraListItemRadius">${RD.row}</CornerRadius>`, `<CornerRadius x:Key="TesseraListItemRadius">${FL.row}</CornerRadius>`)
    .replace(`<CornerRadius x:Key="TesseraMenuItemRadius">${RD.menu - 6}</CornerRadius>`, `<CornerRadius x:Key="TesseraMenuItemRadius">${FL.menu - 6}</CornerRadius>`)
    .replace(`<x:Double x:Key="TesseraPressScale">${RD.pressScale}</x:Double>`, `<x:Double x:Key="TesseraPressScale">${FL.pressScale}</x:Double>`)
    .replace('<x:String x:Key="TesseraShape">round</x:String>', '<x:String x:Key="TesseraShape">flow</x:String>')
    .replace(`<CornerRadius x:Key="ControlCornerRadius">${F.size.md / 2}</CornerRadius>`, `<CornerRadius x:Key="ControlCornerRadius">${FL.control}</CornerRadius>`)
    .replace(`<CornerRadius x:Key="OverlayCornerRadius">${RD.menu}</CornerRadius>`, `<CornerRadius x:Key="OverlayCornerRadius">${FL.menu}</CornerRadius>`));

  const cs = `${HEADER()}
using System;
using System.Numerics;

namespace TesseraUI
{
    /// <summary>弹簧参数。WinUI Composition 的 SpringNaturalMotionAnimation 使用 DampingRatio + Period，Period 即本库的“时长”。</summary>
    public readonly record struct TesseraSpring(float DampingRatio, TimeSpan Period, float Stiffness, float Damping);

    public static class TesseraMotion
    {
${Object.entries(springs)
  .map(([k, s]) => `        public static readonly TesseraSpring ${pascal(k)} = new(${s.dampingRatio}f, TimeSpan.FromMilliseconds(${s.duration * 1000}), ${s.stiffness}f, ${s.damping}f);`)
  .join('\n')}
${Object.entries(M.duration).map(([k, v]) => `        public static readonly TimeSpan Duration${pascal(k)} = TimeSpan.FromMilliseconds(${v});`).join('\n')}
${Object.entries(M.ease)
  .filter(([, v]) => Array.isArray(v))
  .map(([k, v]) => `        public static readonly (Vector2 P1, Vector2 P2) Ease${pascal(k)} = (new(${v[0]}f, ${v[1]}f), new(${v[2]}f, ${v[3]}f));`)
  .join('\n')}
        /// <summary>按下时砖往下沉的距离（epx）</summary>
        public const float PressSink = ${M.press.sink}f;
        public const int PressDurationMs = ${M.press.duration};
        public const float SettleLift = ${M.settle.lift}f;
        public const float SettleScale = ${M.settle.scale}f;
        public const float FlingVelocity = ${M.gesture.flingVelocity}f;
        public const int StaggerStepMs = ${M.stagger.step};
        public const int StaggerMaxItems = ${M.stagger.maxItems};
        public const int ToastDurationMs = ${M.toast.duration};
        public const int ToastDurationWithActionMs = ${M.toast.durationWithAction};
    }

    /// <summary>流韵外观的动效数值与启用条件</summary>
    public static class TesseraFlowLimits
    {
        public const int MinCores = ${F.shape.flow.minCores};
        public const int MinMemoryGB = ${F.shape.flow.minMemoryGB};
        public const float PressScale = ${F.shape.flow.pressScale}f;
        public const int RippleDurationMs = ${F.shape.flow.rippleDuration};
        public const int SplashDurationMs = ${F.shape.flow.splashDuration};
    }

    public static class TesseraZ
    {
${Object.entries(F.z).map(([k, v]) => `        public const int ${pascal(k)} = ${v};`).join('\n')}
    }
}
`;
  emit('platforms/windows/TesseraUI/Generated/TesseraTokens.g.cs', cs);
}

buildWeb();
buildApple();
buildAndroid();
buildWindows();
console.log(`TesseraUI v${version}：已生成 ${written.length} 个文件`);
for (const f of written) console.log('  ' + f);
