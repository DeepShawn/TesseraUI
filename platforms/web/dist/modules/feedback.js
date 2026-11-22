// TesseraUI v0.6.0 — 由 scripts/build.mjs 从 tokens/*.json 生成，请勿手改。
// 反馈：Banner、ConfirmDialog、Drawer、ActionSheet、Progress
import { Spring, VelocityTracker, motion as M, icon, once, html, esc, rubberBand, openDialog, closeDialog, prefersReducedMotion } from '../core.js';

// ------------------------------------------------------------------
// 横幅：右侧关闭按钮，收起时高度平滑过渡
// ------------------------------------------------------------------
export function banner(root) {
  if (!root.hasAttribute('data-dismissible')) return;
  const b = document.createElement('button');
  b.type = 'button';
  b.className = 'ts-icon-button ts-sm ts-pressable ts-state';
  b.setAttribute('aria-label', '关闭');
  b.append(icon('close'));
  b.addEventListener('click', async () => {
    if (!prefersReducedMotion()) {
      root.style.overflow = 'hidden';
      await root.animate([{ height: `${root.offsetHeight}px`, opacity: 1 }, { height: '0px', opacity: 0, paddingTop: 0, paddingBottom: 0, marginTop: 0, marginBottom: 0 }], { duration: M.duration.normal, easing: 'cubic-bezier(0.2, 0, 0, 1)', fill: 'forwards' }).finished;
    }
    root.remove();
  });
  root.append(b);
}

// ------------------------------------------------------------------
// 确认框：替代 window.confirm()，返回 Promise<boolean>
// ------------------------------------------------------------------
export function confirm({ title, message = '', confirmText = '确定', cancelText = '取消', danger = false } = {}) {
  return new Promise((resolve) => {
    const dlg = html(`<dialog class="ts-dialog" aria-labelledby="ts-confirm-title" aria-describedby="ts-confirm-body">
      <h2 class="ts-dialog-title" id="ts-confirm-title">${esc(title)}</h2>
      ${message ? `<p class="ts-dialog-body" id="ts-confirm-body">${esc(message)}</p>` : ''}
      <div class="ts-dialog-actions ts-tiles">
        <button class="ts-button ts-pressable ts-state" data-v="0">${esc(cancelText)}</button>
        <button class="ts-button ${danger ? 'ts-danger' : 'ts-primary'} ts-pressable ts-state" data-v="1">${esc(confirmText)}</button>
      </div></dialog>`);
    document.body.append(dlg);
    let done = false;
    const finish = async (v) => {
      if (done) return;
      done = true;
      await closeDialog(dlg);
      dlg.remove();
      resolve(v);
    };
    dlg.querySelectorAll('[data-v]').forEach((b) => b.addEventListener('click', () => finish(b.dataset.v === '1')));
    dlg.addEventListener('cancel', (e) => { e.preventDefault(); finish(false); });
    dlg.addEventListener('click', (e) => { if (e.target === dlg) finish(false); });
    openDialog(dlg);
    // 危险操作默认聚焦“取消”，避免误按回车
    dlg.querySelector(danger ? '[data-v="0"]' : '[data-v="1"]').focus();
  });
}

// ------------------------------------------------------------------
// 侧边抽屉：smooth 滑入；可以横向拖动关闭（继承速度）；Esc、点遮罩关闭；焦点锁在抽屉内
// ------------------------------------------------------------------
export function drawer(el) {
  const side = el.dataset.side === 'left' ? -1 : 1;
  el.hidden = true;
  el.setAttribute('role', 'dialog');
  el.setAttribute('aria-modal', 'true');
  const scrim = document.createElement('div');
  scrim.className = 'ts-scrim';
  scrim.hidden = true;
  el.before(scrim);
  const W = () => el.offsetWidth;
  const x = new Spring(0, 'smooth', (v) => {
    el.style.transform = `translateX(${v}px)`;
    scrim.style.opacity = 1 - Math.min(1, Math.abs(v) / W());
  });
  x.precision = 0.5;
  let prevFocus = null;
  const open = () => {
    prevFocus = document.activeElement;
    el.hidden = false; scrim.hidden = false;
    x.set(side * W());
    x.to(0);
    (el.querySelector('[autofocus], button, [href], input, select, textarea') || el).focus({ preventScroll: true });
  };
  const close = (velocity = 0) => {
    x.to(side * W(), { velocity }).then(() => { if (x.target !== 0) { el.hidden = true; scrim.hidden = true; prevFocus?.focus?.(); } });
  };
  el.querySelectorAll('[data-close]').forEach((b) => b.addEventListener('click', () => close()));
  scrim.addEventListener('click', () => close());
  el.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') close();
    if (e.key === 'Tab') { // 焦点循环
      const f = [...el.querySelectorAll('button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])')].filter((n) => !n.disabled && n.offsetParent);
      if (!f.length) return;
      if (e.shiftKey && document.activeElement === f[0]) { e.preventDefault(); f.at(-1).focus(); }
      else if (!e.shiftKey && document.activeElement === f.at(-1)) { e.preventDefault(); f[0].focus(); }
    }
  });
  const vt = new VelocityTracker();
  let start = null;
  el.addEventListener('pointerdown', (e) => { if (e.pointerType === 'mouse') return; start = { x: e.clientX, y: e.clientY, moved: false, id: e.pointerId }; vt.points = []; });
  el.addEventListener('pointermove', (e) => {
    if (!start || e.pointerId !== start.id) return;
    const dx = e.clientX - start.x, dy = e.clientY - start.y;
    if (!start.moved) { if (Math.abs(dx) < 8 || Math.abs(dx) < Math.abs(dy)) return; start.moved = true; el.setPointerCapture(e.pointerId); }
    vt.add(e.clientX);
    x.set(dx * side > 0 ? dx : rubberBand(dx, 40)); // 往里拖有阻力
  });
  el.addEventListener('pointerup', () => {
    if (!start?.moved) { start = null; return; }
    start = null;
    const v = vt.get();
    if (x.value * side > W() * 0.35 || v * side > M.gesture.flingVelocity) close(v);
    else x.to(0, { velocity: v });
  });
  return { open, close };
}

// ------------------------------------------------------------------
// 操作面板：从底部升起；返回被点中的索引，取消返回 -1
// actions: [{ label, icon, danger }]
// ------------------------------------------------------------------
export function actionSheet({ title, actions = [], cancelText = '取消' } = {}) {
  return new Promise((resolve) => {
    const scrim = document.createElement('div');
    scrim.className = 'ts-scrim';
    const sheet = document.createElement('div');
    sheet.className = 'ts-action-sheet';
    sheet.setAttribute('role', 'dialog');
    sheet.setAttribute('aria-modal', 'true');
    sheet.setAttribute('aria-label', title || '操作');
    const group = document.createElement('div');
    group.className = 'ts-action-group';
    if (title) { const t = document.createElement('div'); t.className = 'ts-action-title'; t.textContent = title; group.append(t); }
    actions.forEach((a, i) => {
      const b = document.createElement('button');
      b.type = 'button';
      b.className = 'ts-action ts-state';
      if (a.danger) b.dataset.danger = '';
      if (a.icon) b.append(icon(a.icon));
      b.append(a.label);
      b.addEventListener('click', () => finish(i));
      group.append(b);
    });
    const cancel = document.createElement('button');
    cancel.type = 'button';
    cancel.className = 'ts-action ts-cancel ts-state';
    cancel.textContent = cancelText;
    cancel.addEventListener('click', () => finish(-1));
    sheet.append(group, cancel);
    document.body.append(scrim, sheet);
    const y = new Spring(0, 'smooth', (v) => { sheet.style.transform = `translateY(${v}px)`; scrim.style.opacity = 1 - Math.min(1, v / sheet.offsetHeight); });
    y.precision = 0.5;
    y.set(sheet.offsetHeight + 20);
    y.to(0);
    (sheet.querySelector('.ts-action') || cancel).focus({ preventScroll: true });
    let done = false;
    async function finish(i) {
      if (done) return;
      done = true;
      await y.to(sheet.offsetHeight + 20, { preset: 'smooth' });
      scrim.remove(); sheet.remove();
      resolve(i);
    }
    scrim.addEventListener('click', () => finish(-1));
    sheet.addEventListener('keydown', (e) => { if (e.key === 'Escape') finish(-1); });
  });
}

// ------------------------------------------------------------------
// 进度：设置线形或环形进度（0–100），null 表示不确定进度
// ------------------------------------------------------------------
export function setProgress(el, value) {
  el.setAttribute('role', 'progressbar');
  el.setAttribute('aria-valuemin', 0);
  el.setAttribute('aria-valuemax', 100);
  if (value === null) { el.setAttribute('data-indeterminate', ''); el.removeAttribute('aria-valuenow'); return; }
  el.removeAttribute('data-indeterminate');
  el.style.setProperty('--value', value);
  el.setAttribute('aria-valuenow', Math.round(value));
  const label = el.querySelector('.ts-ring-label');
  if (label) label.textContent = `${Math.round(value)}%`;
}
/** 生成环形进度的 SVG */
export function ring(el) {
  if (el.querySelector('svg')) return;
  // 线宽按 --thick / --size 换算到 100×100 的 viewBox 里，保证不同尺寸下线宽一致
  const cs = getComputedStyle(el);
  const sw = (parseFloat(cs.getPropertyValue('--thick')) / parseFloat(cs.getPropertyValue('--size'))) * 100 || 10;
  const r = 50 - sw / 2;
  el.insertAdjacentHTML('afterbegin', `<svg viewBox="0 0 100 100" aria-hidden="true"><circle class="ts-ring-track" cx="50" cy="50" r="${r}" pathLength="100" stroke-width="${sw}"/><circle class="ts-ring-bar" cx="50" cy="50" r="${r}" pathLength="100" stroke-width="${sw}"/></svg>`);
  if (!el.querySelector('.ts-ring-label')) el.insertAdjacentHTML('beforeend', '<span class="ts-ring-label"></span>');
  setProgress(el, Number(el.dataset.value || 0));
}

export function initFeedback(root = document) {
  const $ = (sel, fn) => root.querySelectorAll(sel).forEach((el) => once(el, sel, fn));
  $('.ts-banner', banner);
  $('.ts-drawer', (el) => { const d = drawer(el); el._tsDrawer = d; });
  $('[data-drawer]', (b) => b.addEventListener('click', () => document.getElementById(b.dataset.drawer)?._tsDrawer?.open()));
  $('.ts-ring', ring);
  $('.ts-progress[data-value]', (el) => { if (!el.querySelector('.ts-progress-bar')) el.innerHTML = '<div class="ts-progress-bar"></div>'; setProgress(el, el.hasAttribute('data-indeterminate') ? null : Number(el.dataset.value)); });
  $('.ts-progress[data-indeterminate]', (el) => { if (!el.querySelector('.ts-progress-bar')) el.innerHTML = '<div class="ts-progress-bar"></div>'; setProgress(el, null); });
  $('.ts-spinner', (el) => { if (!el.children.length) { el.innerHTML = '<i></i><i></i><i></i><i></i>'; el.setAttribute('role', 'status'); el.setAttribute('aria-label', el.getAttribute('aria-label') || '加载中'); } });
}
