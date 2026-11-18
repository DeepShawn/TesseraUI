// TesseraUI v0.3.0 — 由 scripts/build.mjs 从 tokens/*.json 生成，请勿手改。
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

/**
 * 触感反馈。Android 浏览器用 Vibration API；iOS Safari（18+）借用系统开关控件自带的触感。
 * 桌面浏览器没有触感硬件，调用无效果。必须在用户操作（点击、按下）的回调里调用。
 * @param {'light'|'medium'|'success'} kind
 */
export function haptic(kind = 'light') {
  if (navigator.vibrate) {
    navigator.vibrate(kind === 'success' ? [8, 40, 12] : kind === 'medium' ? 14 : 8);
    return;
  }
  if (!isIOS()) return;
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
  iosHaptic.click();
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

  document.addEventListener('pointerdown', (e) => {
    if (!isFlow() || e.button > 0) return;
    const host = e.target.closest?.(RIPPLE_HOSTS);
    if (!host) return;
    if (!quiet()) ripple(host, e.clientX, e.clientY);
    if (host.matches('.ts-button.ts-primary, .ts-button.ts-danger, .ts-fab')) haptic('light');
  }, { passive: true });

  document.addEventListener('change', (e) => {
    if (!isFlow()) return;
    const t = e.target;
    if (!(t instanceof HTMLInputElement)) return;
    if (t.matches('.ts-switch')) { haptic(t.checked ? 'medium' : 'light'); if (!quiet()) splash(t, true); }
    else if (t.matches('.ts-checkbox, .ts-radio')) { haptic('light'); if (!quiet()) splash(t, false); }
    else if (t.closest('.ts-segmented')) {
      haptic('light');
      const seg = t.closest('.ts-segmented');
      if (!quiet()) { seg.classList.remove('is-flowing'); void seg.offsetWidth; seg.classList.add('is-flowing'); setTimeout(() => seg.classList.remove('is-flowing'), 600); }
    }
  });

  document.addEventListener('click', (e) => {
    if (!isFlow()) return;
    if (e.target.closest?.('.ts-tabbar-item, .ts-tab, .ts-page:not([data-gap])')) haptic('light');
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
