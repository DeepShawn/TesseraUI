// TesseraUI v0.6.0 — 由 scripts/build.mjs 从 tokens/*.json 生成，请勿手改。
// 流韵外观（data-shape="flow"）的交互：水波、开关溅起的涟漪、跟随指针的釉光、液面张力、触感反馈。
// 全部用事件委托挂在 document 上，只在流韵外观下生效；切换外观不需要重新初始化。
// “减弱动态效果”打开时不产生任何动效，只保留触感反馈。
import { prefersReducedMotion } from '../core.js';

const isFlow = () => document.documentElement.dataset.shape === 'flow';
const quiet = () => prefersReducedMotion();

// ------------------------------------------------------------------
// 触感反馈
// ------------------------------------------------------------------

let iosHaptic = null;
const isIOS = () => /iP(hone|ad|od)/.test(navigator.userAgent) || (navigator.platform === 'MacIntel' && navigator.maxTouchPoints > 1);

/** 四端统一的触感种类：选择（切换选项）、轻触、中等（开关打开）、确认（成功）、警告（危险操作、失败） */
export const HAPTICS = ['selection', 'light', 'medium', 'success', 'warning'];
const PATTERNS = { selection: 6, light: 8, medium: 14, success: [8, 40, 12], warning: [18, 60, 18, 60, 18] };

/**
 * 触感反馈。Android 浏览器用 Vibration API；iOS Safari 18+ 借用系统开关控件自带的触感（只有一种强度）；
 * 桌面浏览器没有触感硬件。必须在用户操作（点击、按下）的回调里调用。
 * 返回是否尝试触发了触感：浏览器不支持时静默降级并返回 false，不会抛错。
 * @param {'selection'|'light'|'medium'|'success'|'warning'} kind
 * @returns {boolean}
 */
export function haptic(kind = 'light') {
  const pattern = PATTERNS[kind] ?? PATTERNS.light;
  try {
    if (typeof navigator.vibrate === 'function') return navigator.vibrate(pattern) === true;
    if (!isIOS()) return false;
    if (!iosHaptic) {
      iosHaptic = document.createElement('label');
      iosHaptic.setAttribute('aria-hidden', 'true');
      iosHaptic.style.cssText = 'position:fixed;width:1px;height:1px;overflow:hidden;opacity:0;pointer-events:none;left:-10px;top:-10px';
      const input = document.createElement('input');
      input.type = 'checkbox';
      input.setAttribute('switch', '');
      input.tabIndex = -1;
      iosHaptic.append(input);
      document.body.append(iosHaptic);
    }
    // 旧版 iOS 不认识 switch 属性，此时没有触感，也不会有副作用
    if (!('switch' in iosHaptic.firstChild)) return false;
    iosHaptic.click();
    return true;
  } catch {
    return false;
  }
}

// ------------------------------------------------------------------
// 性能保护：持续掉帧、电量低、页面切到后台时的处理
// ------------------------------------------------------------------

let monitorFrame = 0, monitorLast = 0, frames = 0, windowStart = 0, slowWindows = 0;
const MIN_FPS = 45;

function degrade(reason) {
  stopMonitor();
  document.documentElement.dataset.shape = 'round';
  window.dispatchEvent(new Event('resize'));
  document.dispatchEvent(new CustomEvent('tessera:shape', { detail: 'round' }));
  document.dispatchEvent(new CustomEvent('tessera:flow-degraded', { detail: { reason } }));
}

function tick(t) {
  monitorFrame = 0;
  if (!isFlow() || document.hidden) return;
  if (!windowStart) windowStart = t;
  // 两帧间隔超过 1 秒说明页面被挂起过（切后台、锁屏），不计入
  if (monitorLast && t - monitorLast > 1000) { windowStart = t; frames = 0; }
  monitorLast = t;
  frames++;
  if (t - windowStart >= 1000) {
    const fps = (frames * 1000) / (t - windowStart);
    slowWindows = fps < MIN_FPS ? slowWindows + 1 : 0;
    windowStart = t; frames = 0;
    // 连续 2 个 1 秒窗口低于 45 帧：退回圆融
    if (slowWindows >= 2) { degrade('fps'); return; }
  }
  monitorFrame = requestAnimationFrame(tick);
}

function startMonitor() {
  if (monitorFrame || !isFlow() || document.hidden) return;
  windowStart = 0; frames = 0; slowWindows = 0; monitorLast = 0;
  monitorFrame = requestAnimationFrame(tick);
}

function stopMonitor() {
  cancelAnimationFrame(monitorFrame);
  monitorFrame = 0;
}

async function checkBattery() {
  if (!isFlow() || !navigator.getBattery) return;
  try {
    const b = await navigator.getBattery();
    const low = () => !b.charging && b.level <= 0.2;
    if (low()) degrade('battery');
    else b.addEventListener('levelchange', () => { if (isFlow() && low()) degrade('battery'); }, { once: false });
  } catch { /* 不支持就跳过 */ }
}

// ------------------------------------------------------------------
// 水波：按下处荡开两圈柔和的涟漪（不是实心墨点）
// ------------------------------------------------------------------

const RIPPLE_HOSTS = [
  '.ts-button:not(:disabled):not([aria-busy="true"])', '.ts-icon-button:not(:disabled)', '.ts-fab',
  '.ts-list-item', '.ts-tab', '.ts-tabbar-item', '.ts-page:not([data-gap])', '.ts-menu-item', '.ts-option',
  '.ts-sidebar-item', '.ts-action', '.ts-command-item', '.ts-tree-row', '.ts-accordion summary', '.ts-day:not(:disabled)',
].join(',');

function ripple(host, x, y) {
  const r = host.getBoundingClientRect();
  const ink = document.createElement('span');
  ink.className = 'ts-flow-ink';
  ink.setAttribute('aria-hidden', 'true');
  const cx = x - r.left, cy = y - r.top;
  const size = 2 * Math.hypot(Math.max(cx, r.width - cx), Math.max(cy, r.height - cy));
  for (let i = 0; i < 2; i++) {
    const w = document.createElement('i');
    w.style.cssText = `left:${cx - size / 2}px;top:${cy - size / 2}px;width:${size}px;height:${size}px;animation-delay:${i * 90}ms`;
    ink.append(w);
  }
  host.append(ink);
  setTimeout(() => ink.remove(), 900);
}

// ------------------------------------------------------------------
// 开关：溅起涟漪 + 滑块像水滴一样晃动
// ------------------------------------------------------------------

function splash(input, strong) {
  const host = input.closest('label') || input.parentElement;
  if (!host) return;
  const hr = host.getBoundingClientRect(), r = input.getBoundingClientRect();
  const isSwitch = input.classList.contains('ts-switch');
  // 开关：从滑块落脚处溅起；复选框、单选：从中心
  const cx = isSwitch ? (input.checked ? r.right - r.height / 2 : r.left + r.height / 2) : r.left + r.width / 2;
  const cy = r.top + r.height / 2;
  const s = document.createElement('span');
  s.className = 'ts-flow-splash' + (input.checked ? ' is-on' : '');
  s.setAttribute('aria-hidden', 'true');
  const size = (isSwitch ? r.height * 2.4 : r.width * 2.6) * (strong ? 1 : 0.8);
  s.style.cssText = `left:${cx - hr.left - size / 2}px;top:${cy - hr.top - size / 2}px;width:${size}px;height:${size}px`;
  s.innerHTML = '<i></i><i></i>';
  host.append(s);
  setTimeout(() => s.remove(), 800);
  input.classList.remove('is-splash');
  void input.offsetWidth; // 重新触发动画
  input.classList.add('is-splash');
  setTimeout(() => input.classList.remove('is-splash'), 700);
}

// ------------------------------------------------------------------
// 跟随指针的釉光：鼠标在控件上移动时，一小片高光跟着走
// ------------------------------------------------------------------

const GLINT_HOSTS = '.ts-button:not(.ts-text), .ts-icon-button.ts-filled, .ts-fab, .ts-card, .ts-segmented-indicator';
let glintTarget = null, glintFrame = 0, glintX = 0, glintY = 0;

function moveGlint() {
  glintFrame = 0;
  if (!glintTarget) return;
  const r = glintTarget.getBoundingClientRect();
  glintTarget.style.setProperty('--mx', `${glintX - r.left}px`);
  glintTarget.style.setProperty('--my', `${glintY - r.top}px`);
}

function clearGlint(el) {
  if (!el) return;
  el.style.removeProperty('--mx');
  el.style.removeProperty('--my');
}

// ------------------------------------------------------------------
// 安装
// ------------------------------------------------------------------

let installed = false;

/** 安装流韵外观的交互（只需一次；tessera.js 的 init() 会自动调用） */
export function initFlow() {
  if (installed || typeof document === 'undefined') return;
  installed = true;

  // 性能保护：流韵外观下持续监测帧率；页面切到后台时暂停监测与釉光；电量低于 20% 且未充电时退回圆融
  const onShape = () => { if (isFlow()) { startMonitor(); checkBattery(); } else stopMonitor(); };
  document.addEventListener('tessera:shape', onShape);
  document.addEventListener('visibilitychange', () => {
    if (document.hidden) { stopMonitor(); clearGlint(glintTarget); glintTarget = null; } else onShape();
  });
  onShape();

  document.addEventListener('pointerdown', (e) => {
    if (!isFlow() || e.button > 0) return;
    const host = e.target.closest?.(RIPPLE_HOSTS);
    if (!host) return;
    if (!quiet()) ripple(host, e.clientX, e.clientY);
    if (host.matches('.ts-button.ts-danger')) haptic('warning');
    else if (host.matches('.ts-button.ts-primary, .ts-fab')) haptic('light');
  }, { passive: true });

  document.addEventListener('change', (e) => {
    if (!isFlow()) return;
    const t = e.target;
    if (!(t instanceof HTMLInputElement)) return;
    if (t.matches('.ts-switch')) { haptic(t.checked ? 'medium' : 'light'); if (!quiet()) splash(t, true); }
    else if (t.matches('.ts-checkbox, .ts-radio')) { haptic('selection'); if (!quiet()) splash(t, false); }
    else if (t.closest('.ts-segmented')) {
      haptic('selection');
      const seg = t.closest('.ts-segmented');
      if (!quiet()) { seg.classList.remove('is-flowing'); void seg.offsetWidth; seg.classList.add('is-flowing'); setTimeout(() => seg.classList.remove('is-flowing'), 600); }
    }
  });

  document.addEventListener('click', (e) => {
    if (!isFlow()) return;
    if (e.target.closest?.('.ts-tabbar-item, .ts-tab, .ts-page:not([data-gap])')) haptic('selection');
  });

  document.addEventListener('pointermove', (e) => {
    if (!isFlow() || e.pointerType !== 'mouse' || quiet()) return;
    const host = e.target.closest?.(GLINT_HOSTS);
    if (host !== glintTarget) { clearGlint(glintTarget); glintTarget = host; }
    if (!host) return;
    glintX = e.clientX; glintY = e.clientY;
    if (!glintFrame) glintFrame = requestAnimationFrame(moveGlint);
  }, { passive: true });

  document.addEventListener('pointerout', (e) => {
    if (!glintTarget || (e.relatedTarget && glintTarget.contains(e.relatedTarget))) return;
    if (e.target === glintTarget || glintTarget.contains(e.target)) { clearGlint(glintTarget); glintTarget = null; }
  }, { passive: true });
}
