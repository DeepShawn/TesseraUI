// TesseraUI v0.6.0 — 由 scripts/build.mjs 从 tokens/*.json 生成，请勿手改。
// 布局：SplitPane、ScrollArea
import { once, clamp, emit } from '../core.js';

// ------------------------------------------------------------------
// 可拖拽分栏：拖动或用方向键调整；双击还原；位置可记在 localStorage
// ------------------------------------------------------------------
export function splitPane(root) {
  const vertical = root.classList.contains('ts-vertical');
  const min = Number(root.dataset.min || 15), max = Number(root.dataset.max || 85);
  const initial = Number(root.dataset.position || 50);
  const key = root.dataset.persist ? `ts-split:${root.dataset.persist}` : null;
  const handle = document.createElement('div');
  handle.className = 'ts-split-handle';
  handle.tabIndex = 0;
  handle.setAttribute('role', 'separator');
  handle.setAttribute('aria-orientation', vertical ? 'horizontal' : 'vertical');
  handle.setAttribute('aria-valuemin', min);
  handle.setAttribute('aria-valuemax', max);
  handle.setAttribute('aria-label', root.dataset.label || '调整分栏大小');
  root.firstElementChild.after(handle);
  let pos = initial;
  try { const v = key && localStorage.getItem(key); if (v) pos = Number(v); } catch {}
  const set = (p, save) => {
    pos = clamp(p, min, max);
    root.style.setProperty('--_pos', `${pos}%`);
    handle.setAttribute('aria-valuenow', Math.round(pos));
    if (save) { try { key && localStorage.setItem(key, pos); } catch {} emit(root, 'resize', { position: pos }); }
  };
  set(pos);
  handle.addEventListener('pointerdown', (e) => {
    handle.setPointerCapture(e.pointerId);
    handle.setAttribute('data-dragging', '');
    const r = root.getBoundingClientRect();
    const move = (ev) => set(vertical ? ((ev.clientY - r.top) / r.height) * 100 : ((ev.clientX - r.left) / r.width) * 100);
    handle.onpointermove = move;
    handle.onpointerup = handle.onpointercancel = () => { handle.onpointermove = null; handle.removeAttribute('data-dragging'); set(pos, true); };
  });
  handle.addEventListener('dblclick', () => set(initial, true));
  handle.addEventListener('keydown', (e) => {
    const d = { ArrowLeft: -2, ArrowUp: -2, ArrowRight: 2, ArrowDown: 2 }[e.key];
    if (d) { e.preventDefault(); set(pos + d * (e.shiftKey ? 5 : 1), true); }
    if (e.key === 'Home') { e.preventDefault(); set(min, true); }
    if (e.key === 'End') { e.preventDefault(); set(max, true); }
    if (e.key === 'Enter') { e.preventDefault(); set(initial, true); }
  });
}

// ------------------------------------------------------------------
// 滚动区域：内容超出时在上下边缘加渐隐，提示还能继续滚
// ------------------------------------------------------------------
export function scrollArea(root) {
  if (!root.hasAttribute('tabindex')) root.tabIndex = 0; // 键盘可滚动
  const update = () => {
    root.toggleAttribute('data-fade-top', root.scrollTop > 2);
    root.toggleAttribute('data-fade-bottom', root.scrollTop + root.clientHeight < root.scrollHeight - 2);
  };
  root.addEventListener('scroll', update, { passive: true });
  new ResizeObserver(update).observe(root);
  update();
}

export function initLayout(root = document) {
  const $ = (sel, fn) => root.querySelectorAll(sel).forEach((el) => once(el, sel, fn));
  $('.ts-split', splitPane);
  $('.ts-scroll-area', scrollArea);
}
