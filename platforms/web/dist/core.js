// TesseraUI v0.6.0 — 由 scripts/build.mjs 从 tokens/*.json 生成，请勿手改。
// TesseraUI Web 运行时核心：弹簧动画、手势工具、浮层、主题切换、中文输入法处理。无第三方依赖。
// 各类组件的交互在 modules/*.js 中，入口为 tessera.js。
import { tokens } from './tokens.mjs';
import { icons } from './icons.mjs';

const { spring: SPRINGS, motion: M } = tokens;
export { M as motion, tokens };

export const prefersReducedMotion = () =>
  document.documentElement.dataset.motion === 'reduce' ||
  matchMedia('(prefers-reduced-motion: reduce)').matches;

// ------------------------------------------------------------------
// 弹簧：可随时打断、可继承速度（质量 1）
// ------------------------------------------------------------------
export class Spring {
  /**
   * @param {number} value 初始值
   * @param {'snappy'|'smooth'|'bouncy'|{stiffness:number,damping:number}} preset
   * @param {(v:number)=>void} onUpdate
   */
  constructor(value, preset = 'smooth', onUpdate = () => {}) {
    this.value = value;
    this.target = value;
    this.velocity = 0;
    this.onUpdate = onUpdate;
    this.setPreset(preset);
    this._raf = 0;
    this._resolve = null;
    /** 停止判定的精度：单位是这个弹簧自己的单位。像素位移可设为 0.5，缩放、百分比保持默认 */
    this.precision = 0.01;
  }
  setPreset(preset) {
    const p = typeof preset === 'string' ? SPRINGS[preset] : preset;
    this.stiffness = p.stiffness;
    this.damping = p.damping;
  }
  /** 立即跳到某个值（拖动时 1:1 跟手用） */
  set(value) {
    this.stop();
    this.value = this.target = value;
    this.velocity = 0;
    this.onUpdate(value);
  }
  /** 动画到目标值；velocity 为单位/秒，用来继承手指速度 */
  to(target, { velocity, preset } = {}) {
    if (preset) this.setPreset(preset);
    if (velocity !== undefined) this.velocity = velocity;
    this.target = target;
    if (prefersReducedMotion()) { this.set(target); return Promise.resolve(); }
    if (!this._raf) {
      let last = performance.now();
      const tick = (now) => {
        // 固定小步长积分，避免掉帧时数值发散
        let dt = Math.min((now - last) / 1000, 1 / 20);
        last = now;
        while (dt > 0) {
          const h = Math.min(dt, 1 / 240);
          const a = -this.stiffness * (this.value - this.target) - this.damping * this.velocity;
          this.velocity += a * h;
          this.value += this.velocity * h;
          dt -= h;
        }
        const done = Math.abs(this.value - this.target) < this.precision && Math.abs(this.velocity) < this.precision * 10;
        if (done) { this.value = this.target; this.velocity = 0; }
        this.onUpdate(this.value);
        if (done) { this._raf = 0; this._resolve?.(); this._resolve = null; }
        else this._raf = requestAnimationFrame(tick);
      };
      this._raf = requestAnimationFrame(tick);
    }
    return new Promise((r) => { this._resolve = r; });
  }
  stop() {
    cancelAnimationFrame(this._raf);
    this._raf = 0;
  }
}

/** 根据最近的指针位置估算速度（px/s） */
export class VelocityTracker {
  constructor() { this.points = []; }
  add(v, t = performance.now()) {
    this.points.push({ v, t });
    while (this.points.length > 2 && t - this.points[0].t > 100) this.points.shift();
  }
  get() {
    if (this.points.length < 2) return 0;
    const a = this.points[0], b = this.points[this.points.length - 1];
    return b.t === a.t ? 0 : ((b.v - a.v) / (b.t - a.t)) * 1000;
  }
}

/** 拖出边界后的橡皮筋阻力 */
export const rubberBand = (overshoot, dimension, c = M.gesture.rubberBand) =>
  (1 - 1 / ((Math.abs(overshoot) * c) / dimension + 1)) * dimension * Math.sign(overshoot);

// ------------------------------------------------------------------
// 图标
// ------------------------------------------------------------------
export function icon(name, { label } = {}) {
  const svg = icons[name];
  if (!svg) throw new Error(`TesseraUI：没有名为 ${name} 的图标`);
  const span = document.createElement('span');
  span.className = 'ts-icon';
  span.innerHTML = svg;
  if (label) { span.setAttribute('role', 'img'); span.setAttribute('aria-label', label); }
  else span.setAttribute('aria-hidden', 'true');
  return span;
}
/** 把页面上所有 <i data-icon="name"> 换成 SVG 图标 */
export function hydrateIcons(root = document) {
  for (const el of root.querySelectorAll('i[data-icon]')) {
    const s = icon(el.dataset.icon, { label: el.getAttribute('aria-label') ?? undefined });
    s.classList.add(...el.classList);
    el.replaceWith(s);
  }
}

// ------------------------------------------------------------------
// 选中指示器：前沿用 snappy，后沿用 smooth；移动时自然拉长，到位后收拢
// ------------------------------------------------------------------
function edgeIndicator(container, indicator, getRect, { stretchWithScale = false } = {}) {
  let left = null, right = null;
  const render = () => {
    const l = left.value, r = right.value;
    if (stretchWithScale) {
      // 细条：用 scaleX 拉长（基准宽 100px）
      indicator.style.transform = `translateX(${l}px) scaleX(${(r - l) / 100})`;
    } else {
      // 胶囊：改绝对定位元素的宽度，只影响它自己，不会让页面重排；圆头不会被压扁
      indicator.style.transform = `translateX(${l}px)`;
      indicator.style.width = `${r - l}px`;
    }
  };
  const moveTo = (el, animate = true) => {
    const { l, r } = getRect(el);
    if (!left || !animate) {
      left = new Spring(l, 'smooth', render);
      right = new Spring(r, 'smooth', render);
      render();
      return;
    }
    const goingRight = l > left.target;
    (goingRight ? right : left).to(goingRight ? r : l, { preset: 'snappy' });
    (goingRight ? left : right).to(goingRight ? l : r, { preset: 'smooth' });
  };
  // 拖动时直接设置两条边（1:1 跟手，不加弹簧）
  moveTo.drag = (l, r) => { left.set(l); right.set(r); };
  // 追手指：从未选中的块起手时，选中砖先用 snappy 追到手指下面
  moveTo.chase = (l, r) => { left.to(l, { preset: 'snappy' }); right.to(r, { preset: 'snappy' }); };
  moveTo.current = () => ({ l: left.value, r: right.value });
  new ResizeObserver(() => {
    const cur = container.querySelector('[aria-selected="true"], input:checked');
    if (cur) moveTo(cur.closest('label') ?? cur, false);
  }).observe(container);
  return moveTo;
}

export function segmented(root) {
  const indicator = document.createElement('div');
  indicator.className = 'ts-segmented-indicator';
  indicator.setAttribute('aria-hidden', 'true');
  // 砖面单独画一层：选中砖滑过时盖在砖面上，文字在最上层
  const tiles = document.createElement('div');
  tiles.className = 'ts-segmented-tiles';
  tiles.setAttribute('aria-hidden', 'true');
  tiles.innerHTML = '<i></i>'.repeat(root.querySelectorAll('label').length);
  root.prepend(tiles, indicator);
  // offsetLeft 和绝对定位的 left 都以容器的内边距边为基准
  const moveTo = edgeIndicator(root, indicator, (label) => ({ l: label.offsetLeft, r: label.offsetLeft + label.offsetWidth }));
  const labels = [...root.querySelectorAll('label')];
  const sync = (animate) => {
    const cur = root.querySelector('input:checked');
    if (!cur) return;
    const label = cur.closest('label');
    // 选中砖的圆角跟随落脚的那块砖：首尾两块外角大
    const i = labels.indexOf(label), o = 'var(--_outer)', g = 'var(--ts-radius-grout)';
    indicator.style.borderRadius = labels.length === 1 ? o : i === 0 ? `${o} ${g} ${g} ${o}` : i === labels.length - 1 ? `${g} ${o} ${o} ${g}` : g;
    moveTo(label, animate);
  };
  root.addEventListener('change', () => sync(true));
  sync(false);

  // 按住任意一块左右拖动来选择：
  // - 从选中砖起手：选中砖 1:1 跟手
  // - 从其他块起手：选中砖先追到手指下面，再跟着手指走
  // 松手落到最近的一块；甩得够快就朝甩的方向走一块
  const vt = new VelocityTracker();
  let drag = null, suppressClick = false;
  root.addEventListener('pointerdown', (e) => {
    const label = e.target.closest('label');
    if (!label || e.button !== 0 || root.querySelector('input:disabled')) return;
    const { l, r } = moveTo.current();
    const onSelected = !!label.querySelector('input:checked');
    const w = label.offsetWidth;
    const rootX = root.getBoundingClientRect().left;
    drag = { x: e.clientX, l, w: onSelected ? r - l : w, moved: false, id: e.pointerId, onSelected, rootX };
    vt.points = [];
  });
  root.addEventListener('pointermove', (e) => {
    if (!drag || e.pointerId !== drag.id) return;
    const dx = e.clientX - drag.x;
    if (!drag.moved) {
      if (Math.abs(dx) < 4) return;
      drag.moved = true;
      root.setPointerCapture(e.pointerId);
      root.classList.add('is-dragging');
    }
    vt.add(e.clientX);
    const max = root.clientWidth - drag.w;
    // 从选中砖起手保持按下时的相对位置；从其他块起手让砖的中心对准手指
    let l = drag.onSelected ? drag.l + dx : e.clientX - drag.rootX - drag.w / 2;
    if (l < 0) l = rubberBand(l, drag.w * 0.5);
    if (l > max) l = max + rubberBand(l - max, drag.w * 0.5);
    drag.lastL = l;
    if (drag.onSelected) moveTo.drag(l, l + drag.w);
    else moveTo.chase(l, l + drag.w);
  });
  const end = () => {
    if (!drag) return;
    const d = drag;
    drag = null;
    if (!d.moved) return;
    root.classList.remove('is-dragging');
    suppressClick = true;
    // 追手指模式下砖可能还没追上，按手指的位置算落点
    const { l, r } = moveTo.current();
    const center = d.onSelected ? (l + r) / 2 : d.lastL + d.w / 2;
    let i = labels.reduce((best, lb, k) => {
      const c = lb.offsetLeft + lb.offsetWidth / 2;
      return Math.abs(c - center) < Math.abs(labels[best].offsetLeft + labels[best].offsetWidth / 2 - center) ? k : best;
    }, 0);
    const v = vt.get();
    const from = labels.findIndex((lb) => lb.querySelector('input:checked'));
    if (Math.abs(v) > M.gesture.flingVelocity && i === from) i = Math.max(0, Math.min(labels.length - 1, i + Math.sign(v)));
    const input = labels[i].querySelector('input');
    if (input.checked) sync(true);
    else { input.checked = true; input.dispatchEvent(new Event('change', { bubbles: true })); }
  };
  root.addEventListener('pointerup', end);
  root.addEventListener('pointercancel', end);
  // 拖完松手时浏览器还会补一个 click，挡掉它，免得又选回起点那块
  root.addEventListener('click', (e) => { if (suppressClick) { e.preventDefault(); suppressClick = false; } }, true);
}

// ------------------------------------------------------------------
// 开关：可以点，也可以按住滑块从左往右拖
// ------------------------------------------------------------------
export function switchControl(input) {
  if (input._tsSwitch) return;
  input._tsSwitch = true;
  const vt = new VelocityTracker();
  let drag = null, suppressClick = false;
  const px = (name) => parseFloat(getComputedStyle(input).getPropertyValue(name)) || 0;
  // 滑块可走的距离：轨道宽 − 两侧内边距 − 滑块宽 − 拉宽量
  const travel = () => input.clientWidth - px('--_pad') * 2 - px('--_thumb') - 6;

  input.addEventListener('pointerdown', (e) => {
    if (input.disabled || e.button !== 0) return;
    drag = { x: e.clientX, pos: input.checked ? 1 : 0, moved: false, id: e.pointerId };
    vt.points = [];
  });
  input.addEventListener('pointermove', (e) => {
    if (!drag || e.pointerId !== drag.id) return;
    const dx = e.clientX - drag.x;
    if (!drag.moved) {
      if (Math.abs(dx) < 3) return;
      drag.moved = true;
      input.setPointerCapture(e.pointerId);
      input.classList.add('is-dragging');
    }
    vt.add(e.clientX);
    let pos = drag.pos + dx / travel();
    // 拖出两端有橡皮筋阻力
    if (pos < 0) pos = rubberBand(pos, 0.15);
    if (pos > 1) pos = 1 + rubberBand(pos - 1, 0.15);
    input.style.setProperty('--_pos', pos);
    input.dataset.preview = pos > 0.5 ? 'on' : 'off';
  });
  const end = () => {
    if (!drag) return;
    const d = drag;
    drag = null;
    if (!d.moved) return;
    const pos = parseFloat(input.style.getPropertyValue('--_pos'));
    const v = vt.get();
    const next = Math.abs(v) > M.gesture.flingVelocity ? v > 0 : pos > 0.5;
    // 同一帧里去掉拖动状态和内联位置：滑块从当前位置用 snappy 弹到终点
    input.classList.remove('is-dragging');
    input.style.removeProperty('--_pos');
    delete input.dataset.preview;
    suppressClick = true;
    if (next !== input.checked) {
      input.checked = next;
      input.dispatchEvent(new Event('input', { bubbles: true }));
      input.dispatchEvent(new Event('change', { bubbles: true }));
    }
  };
  input.addEventListener('pointerup', end);
  input.addEventListener('pointercancel', end);
  // 拖完松手后浏览器补发的 click 会再切换一次，这里挡掉
  input.addEventListener('click', (e) => { if (suppressClick) { e.preventDefault(); suppressClick = false; } });
}

export function tabs(root, { onChange } = {}) {
  const indicator = document.createElement('div');
  indicator.className = 'ts-tabs-indicator';
  root.append(indicator);
  const list = [...root.querySelectorAll('[role="tab"]')];
  const moveTo = edgeIndicator(root, indicator, (t) => ({ l: t.offsetLeft, r: t.offsetLeft + t.offsetWidth }), { stretchWithScale: true });
  const select = (tab, animate = true) => {
    for (const t of list) { t.setAttribute('aria-selected', t === tab); t.tabIndex = t === tab ? 0 : -1; }
    moveTo(tab, animate);
    onChange?.(tab);
  };
  root.addEventListener('click', (e) => { const t = e.target.closest('[role="tab"]'); if (t) select(t); });
  root.addEventListener('keydown', (e) => {
    const i = list.indexOf(document.activeElement);
    if (i < 0) return;
    const next = e.key === 'ArrowRight' ? list[(i + 1) % list.length] : e.key === 'ArrowLeft' ? list[(i - 1 + list.length) % list.length] : null;
    if (next) { e.preventDefault(); next.focus(); select(next); }
  });
  select(list.find((t) => t.getAttribute('aria-selected') === 'true') ?? list[0], false);
}

// ------------------------------------------------------------------
// 浮层通用动画（WAAPI + 弹簧 linear() 曲线）
// ------------------------------------------------------------------
export const cssVar = (name) => getComputedStyle(document.documentElement).getPropertyValue(name).trim();
export const springEasing = (name) => ({ easing: cssVar(`--ts-spring-${name}`), duration: parseFloat(cssVar(`--ts-spring-${name}-duration`)) });

export function animateIn(el, from, preset = 'smooth') {
  if (prefersReducedMotion()) return el.animate([{ opacity: 0 }, { opacity: 1 }], { duration: M.reducedMotion.duration }).finished;
  const { easing, duration } = springEasing(preset);
  // 透明度用“时长 + 曲线”，变换用弹簧
  el.animate([{ opacity: 0 }, { opacity: 1 }], { duration: M.duration.normal, easing: 'cubic-bezier(0.2, 0, 0, 1)' });
  return el.animate([{ transform: from }, { transform: 'none' }], { duration, easing }).finished;
}
export function animateOut(el, to = 'none') {
  const keyframes = prefersReducedMotion() ? [{ opacity: 1 }, { opacity: 0 }] : [{ opacity: 1, transform: 'none' }, { opacity: 0, transform: to }];
  return el.animate(keyframes, { duration: M.duration.fast, easing: 'cubic-bezier(0.3, 0, 0.8, 0.15)', fill: 'forwards' }).finished;
}

// ------------------------------------------------------------------
// 对话框：遮罩 250ms 淡入；面板 0.96→1 + 淡入（smooth）；关闭 150ms 缩到 0.98 淡出
// ------------------------------------------------------------------
export function openDialog(dialog) {
  dialog.showModal();
  animateIn(dialog, 'scale(0.96)');
  if (!dialog._tsBound) {
    dialog._tsBound = true;
    dialog.addEventListener('cancel', (e) => { e.preventDefault(); closeDialog(dialog); });
    dialog.addEventListener('click', (e) => { if (e.target === dialog) closeDialog(dialog); }); // 点遮罩关闭
  }
}
export async function closeDialog(dialog, returnValue) {
  await animateOut(dialog, 'scale(0.98)');
  dialog.close(returnValue);
  dialog.getAnimations().forEach((a) => a.cancel());
}

// ------------------------------------------------------------------
// 下拉菜单：从触发位置展开（0.96→1 + 淡入，smooth）；关闭只淡出 150ms
// ------------------------------------------------------------------
export function menu(trigger, menuEl) {
  menuEl.hidden = true;
  menuEl.setAttribute('role', 'menu');
  trigger.setAttribute('aria-haspopup', 'menu');
  trigger.setAttribute('aria-expanded', 'false');
  const items = () => [...menuEl.querySelectorAll('.ts-menu-item')];
  items().forEach((i) => { i.setAttribute('role', 'menuitem'); i.tabIndex = -1; });

  const open = () => {
    menuEl.hidden = false;
    const r = trigger.getBoundingClientRect();
    const m = menuEl.getBoundingClientRect();
    const below = r.bottom + 8 + m.height < innerHeight;
    const top = below ? r.bottom + 8 : r.top - 8 - m.height;
    const left = Math.min(Math.max(8, r.left), innerWidth - m.width - 8);
    Object.assign(menuEl.style, { top: `${top}px`, left: `${left}px` });
    menuEl.style.transformOrigin = `${r.left + r.width / 2 - left}px ${below ? 0 : m.height}px`;
    trigger.setAttribute('aria-expanded', 'true');
    animateIn(menuEl, 'scale(0.96)');
    items()[0]?.focus();
    setTimeout(() => document.addEventListener('pointerdown', outside, true));
  };
  const close = async (focusTrigger = true) => {
    if (menuEl.hidden) return;
    document.removeEventListener('pointerdown', outside, true);
    trigger.setAttribute('aria-expanded', 'false');
    await animateOut(menuEl);
    menuEl.hidden = true;
    menuEl.getAnimations().forEach((a) => a.cancel());
    if (focusTrigger) trigger.focus();
  };
  const outside = (e) => { if (!menuEl.contains(e.target) && !trigger.contains(e.target)) close(false); };
  trigger.addEventListener('click', () => (menuEl.hidden ? open() : close()));
  menuEl.addEventListener('keydown', (e) => {
    const list = items(), i = list.indexOf(document.activeElement);
    if (e.key === 'ArrowDown') { e.preventDefault(); list[(i + 1) % list.length].focus(); }
    if (e.key === 'ArrowUp') { e.preventDefault(); list[(i - 1 + list.length) % list.length].focus(); }
    if (e.key === 'Escape' || e.key === 'Tab') close();
  });
  menuEl.addEventListener('click', (e) => { if (e.target.closest('.ts-menu-item')) close(); });
  return { open, close };
}

// ------------------------------------------------------------------
// 提示消息：从顶部滑入，0.9→1（bouncy）；停留 3 秒（带操作 5 秒）；可上滑关掉；离场 150ms
// ------------------------------------------------------------------
let toaster;
export function toast(message, { type, iconName, action } = {}) {
  if (!toaster) {
    toaster = document.createElement('div');
    toaster.className = 'ts-toaster';
    toaster.setAttribute('role', 'status');
    toaster.setAttribute('aria-live', 'polite');
    document.body.append(toaster);
  }
  const el = document.createElement('div');
  el.className = `ts-toast${type ? ` ts-${type}` : ''}`;
  const ic = iconName ?? { success: 'success-fill', danger: 'error' }[type];
  if (ic) el.append(icon(ic));
  el.append(document.createTextNode(message));
  if (action) {
    const b = document.createElement('button');
    b.className = 'ts-button ts-sm ts-text ts-pressable ts-state';
    b.textContent = action.label;
    b.onclick = () => { action.onClick?.(); dismiss(); };
    el.append(b);
  }
  toaster.append(el);
  animateIn(el, 'translateY(-100%) scale(0.9)', 'bouncy');

  let timer = setTimeout(() => dismiss(), action ? M.toast.durationWithAction : M.toast.duration);
  let gone = false;
  async function dismiss(dy = 0) {
    if (gone) return;
    gone = true;
    clearTimeout(timer);
    await animateOut(el, `translateY(${dy - 24}px)`);
    el.remove();
  }
  // 上滑关闭
  const y = new Spring(0, 'bouncy', (v) => (el.style.transform = `translateY(${v}px)`));
  const vt = new VelocityTracker();
  let startY = null;
  el.addEventListener('pointerdown', (e) => { if (e.target.closest('button')) return; startY = e.clientY; el.setPointerCapture(e.pointerId); clearTimeout(timer); vt.points = []; });
  el.addEventListener('pointermove', (e) => {
    if (startY === null) return;
    const d = e.clientY - startY;
    vt.add(e.clientY);
    y.set(d < 0 ? d : rubberBand(d, 60)); // 向下拖有阻力
  });
  el.addEventListener('pointerup', () => {
    if (startY === null) return;
    startY = null;
    const v = vt.get();
    if (y.value < -24 || v < -M.gesture.flingVelocity) dismiss(y.value);
    else { y.to(0, { velocity: v }); timer = setTimeout(() => dismiss(), M.toast.duration); }
  });
  return { dismiss };
}

// ------------------------------------------------------------------
// 底部抽屉：smooth 滑入；拖动 1:1 跟手；松手继承速度，> 500px/s 切到下一档，否则停到最近档位；拖出边界有橡皮筋阻力
// ------------------------------------------------------------------
export function bottomSheet(sheet, { detents = [0.5, 0.92] } = {}) {
  const scrim = document.createElement('div');
  scrim.className = 'ts-scrim';
  scrim.hidden = true;
  sheet.before(scrim);
  sheet.hidden = true;
  sheet.setAttribute('role', 'dialog');
  sheet.setAttribute('aria-modal', 'true');

  const H = () => sheet.offsetHeight;
  // 档位用“抽屉顶端距离屏幕底部的高度”表示；translateY = H − 可见高度
  const stops = () => [0, ...detents.map((d) => Math.min(d * innerHeight, H()))];
  const y = new Spring(0, 'smooth', (v) => {
    sheet.style.transform = `translateY(${v}px)`;
    const visible = H() - v;
    scrim.style.opacity = Math.max(0, Math.min(1, visible / (stops()[1] || 1)));
  });

  y.precision = 0.5;
  const snapTo = (visible, velocity = 0) => {
    const p = y.to(H() - visible, { velocity, preset: 'smooth' });
    if (visible === 0) p.then(() => { if (y.target === H()) { sheet.hidden = true; scrim.hidden = true; } });
    return p;
  };
  const open = () => {
    sheet.hidden = false; scrim.hidden = false;
    y.set(H());
    snapTo(stops()[1]);
    sheet.querySelector('button, [href], input, [tabindex]')?.focus({ preventScroll: true });
  };
  const close = () => snapTo(0);

  const handle = sheet.querySelector('.ts-sheet-handle') ?? sheet;
  const vt = new VelocityTracker();
  let start = null;
  handle.addEventListener('pointerdown', (e) => {
    start = { pointer: e.clientY, y: y.value };
    y.stop();
    vt.points = [];
    handle.setPointerCapture(e.pointerId);
  });
  handle.addEventListener('pointermove', (e) => {
    if (!start) return;
    vt.add(e.clientY);
    let next = start.y + (e.clientY - start.pointer);
    const min = H() - stops().at(-1);
    if (next < min) next = min + rubberBand(next - min, innerHeight * 0.3);
    y.set(next);
  });
  handle.addEventListener('pointerup', () => {
    if (!start) return;
    start = null;
    const v = vt.get(); // 正值向下
    const visible = H() - y.value;
    const s = stops();
    let target;
    if (Math.abs(v) > M.gesture.flingVelocity) {
      // 朝滑动方向切到下一档
      target = v > 0 ? [...s].reverse().find((x) => x < visible - 1) ?? 0 : s.find((x) => x > visible + 1) ?? s.at(-1);
    } else {
      target = s.reduce((a, b) => (Math.abs(b - visible) < Math.abs(a - visible) ? b : a));
    }
    snapTo(target, v);
  });
  scrim.addEventListener('click', close);
  sheet.addEventListener('keydown', (e) => { if (e.key === 'Escape') close(); });
  return { open, close };
}

// ------------------------------------------------------------------
// 主题：浅色 / 深色 / 跟随系统；切换时整屏 250ms 交叉淡化
// ------------------------------------------------------------------
export function setTheme(theme) {
  const apply = () => {
    if (theme === 'system') delete document.documentElement.dataset.theme;
    else document.documentElement.dataset.theme = theme;
  };
  if (document.startViewTransition && !prefersReducedMotion()) document.startViewTransition(apply);
  else apply();
}
export function setContrast(more) {
  const apply = () => {
    if (more) document.documentElement.dataset.contrast = 'more';
    else delete document.documentElement.dataset.contrast;
  };
  if (document.startViewTransition && !prefersReducedMotion()) document.startViewTransition(apply);
  else apply();
}

// ------------------------------------------------------------------
// 中文输入法：拼音还没选定时，不触发搜索、校验、字数统计，回车也不提交
// ------------------------------------------------------------------
export function onCommittedInput(input, callback) {
  let composing = false;
  input.addEventListener('compositionstart', () => { composing = true; });
  input.addEventListener('compositionend', () => { composing = false; callback(input.value); });
  input.addEventListener('input', (e) => { if (!composing && !e.isComposing) callback(input.value); });
}
export function guardEnterWhileComposing(root = document) {
  root.addEventListener('keydown', (e) => {
    // keyCode 229 是 Safari 在 compositionend 之后补发的回车
    if (e.key === 'Enter' && (e.isComposing || e.keyCode === 229)) { e.preventDefault(); e.stopPropagation(); }
  }, true);
}

// ------------------------------------------------------------------
// 一键初始化
// ------------------------------------------------------------------
export function initCore(root = document) {
  document.documentElement.classList.add('ts-root');
  hydrateIcons(root);
  root.querySelectorAll('.ts-segmented').forEach((el) => once(el, 'segmented', segmented));
  root.querySelectorAll('.ts-switch').forEach(switchControl);
  root.querySelectorAll('.ts-tabs').forEach((el) => once(el, 'tabs', () => tabs(el)));
  once(root, 'ime', () => guardEnterWhileComposing(root));
}

/**
 * 设备是否适合“流韵”外观：流韵有持续的流体动效（水波、高光跟随、弹性形变），只在性能较好的设备上启用。
 * 条件：没有打开“减弱动态效果”、没有开省流量，CPU 不少于 6 核、内存不少于 4GB（浏览器不提供时按满足处理）。
 */
export function flowCapable() {
  if (prefersReducedMotion()) return false;
  const nav = navigator;
  if (nav.connection && nav.connection.saveData) return false;
  if ((nav.hardwareConcurrency ?? 8) < 6) return false;
  if ((nav.deviceMemory ?? 8) < 4) return false;
  return true;
}

/**
 * 外观：'tile'（拼贴，默认）、'round'（圆融）或 'flow'（流韵）。
 * 设备不适合流韵时自动改用圆融（可传 { force: true } 强制）。返回实际使用的外观。
 */
export function setShape(shape, { force = false } = {}) {
  let applied = shape === 'round' || shape === 'flow' ? shape : 'tile';
  if (applied === 'flow' && !force && !flowCapable()) applied = 'round';
  const apply = () => {
    if (applied === 'tile') delete document.documentElement.dataset.shape;
    else document.documentElement.dataset.shape = applied;
    // 分段控制、标签页的指示器按新的尺寸重新定位
    window.dispatchEvent(new Event('resize'));
    document.dispatchEvent(new CustomEvent('tessera:shape', { detail: applied }));
  };
  if (document.startViewTransition && !prefersReducedMotion()) document.startViewTransition(apply);
  else apply();
  return applied;
}
export function setMaterial(glass) {
  const apply = () => {
    if (glass) document.documentElement.dataset.material = 'glass';
    else delete document.documentElement.dataset.material;
  };
  if (document.startViewTransition && !prefersReducedMotion()) document.startViewTransition(apply);
  else apply();
}

// ------------------------------------------------------------------
// 通用工具（供各组件模块使用）
// ------------------------------------------------------------------

/** 每个元素只初始化一次 */
const inited = new WeakMap();
export function once(el, key, fn) {
  let set = inited.get(el);
  if (!set) inited.set(el, (set = new Set()));
  if (set.has(key)) return;
  set.add(key);
  fn(el);
}

let uidSeq = 0;
export const uid = (prefix = 'ts') => `${prefix}-${++uidSeq}`;

export const clamp = (v, min, max) => Math.min(max, Math.max(min, v));

/**
 * 把浮层放到锚点旁边：优先 placement 方向，放不下自动翻到对侧，并限制在视口内。
 * placement：'bottom-start' | 'bottom' | 'top' | 'top-start' | 'right' | 'left'
 */
export function place(anchor, panel, { placement = 'bottom-start', offset = 8 } = {}) {
  const r = anchor.getBoundingClientRect ? anchor.getBoundingClientRect() : anchor; // 也接受 {left, top, right, bottom, width, height}
  // 先恢复浮层的自然高度再测量（上一次可能因为放不下被压缩过）
  if (panel.dataset.squeezed) { panel.style.maxHeight = ''; panel.style.overflowY = ''; delete panel.dataset.squeezed; }
  let m = panel.getBoundingClientRect();
  const vw = innerWidth, vh = innerHeight, pad = 8;
  let [side, align] = placement.split('-');
  if (side === 'bottom' || side === 'top') {
    const below = vh - pad - r.bottom - offset, above = r.top - offset - pad;
    if (side === 'bottom' && m.height > below && m.height <= above) side = 'top';
    else if (side === 'top' && m.height > above && m.height <= below) side = 'bottom';
    else if (m.height > below && m.height > above) {
      // 上下都放不下：放到空间大的一侧，并把浮层压到刚好放得下（内部滚动），绝不盖住锚点（例如手机键盘弹出时的输入框）
      side = below >= above ? 'bottom' : 'top';
      panel.style.maxHeight = `${Math.max(96, Math.floor(side === 'bottom' ? below : above))}px`;
      panel.style.overflowY = 'auto';
      panel.dataset.squeezed = '1';
      m = panel.getBoundingClientRect();
    }
  }
  else if (side === 'right' && r.right + offset + m.width > vw - pad) side = 'left';
  else if (side === 'left' && r.left - offset - m.width < pad) side = 'right';
  let top, left;
  if (side === 'bottom' || side === 'top') {
    top = side === 'bottom' ? r.bottom + offset : r.top - offset - m.height;
    left = align === 'start' ? r.left : align === 'end' ? r.right - m.width : r.left + r.width / 2 - m.width / 2;
  } else {
    left = side === 'right' ? r.right + offset : r.left - offset - m.width;
    top = r.top + r.height / 2 - m.height / 2;
  }
  left = clamp(left, pad, vw - m.width - pad);
  top = clamp(top, pad, vh - m.height - pad);
  Object.assign(panel.style, { left: `${left}px`, top: `${top}px` });
  // 动画从锚点方向展开
  const ox = clamp(r.left + r.width / 2 - left, 0, m.width);
  const oy = side === 'bottom' ? 0 : side === 'top' ? m.height : clamp(r.top + r.height / 2 - top, 0, m.height);
  panel.style.transformOrigin = `${ox}px ${oy}px`;
  panel.dataset.side = side;
  return side;
}

/**
 * 浮层跟随锚点：页面或任意滚动容器滚动、窗口大小变化、手机软键盘弹出（visualViewport 变化）时重新定位。
 * 返回停止函数，浮层关闭时调用。
 */
export function follow(anchor, panel, opts) {
  let frame = 0;
  const update = () => {
    if (frame) return;
    frame = requestAnimationFrame(() => { frame = 0; if (!panel.hidden) place(anchor, panel, opts); });
  };
  const vv = window.visualViewport;
  addEventListener('scroll', update, { passive: true, capture: true });
  addEventListener('resize', update, { passive: true });
  vv?.addEventListener('resize', update, { passive: true });
  vv?.addEventListener('scroll', update, { passive: true });
  return () => {
    cancelAnimationFrame(frame);
    removeEventListener('scroll', update, { capture: true });
    removeEventListener('resize', update);
    vv?.removeEventListener('resize', update);
    vv?.removeEventListener('scroll', update);
  };
}

/** 点击元素外部时回调；返回取消函数 */
export function onOutside(els, cb) {
  const list = [].concat(els);
  const h = (e) => { if (!list.some((el) => el && el.contains(e.target))) cb(e); };
  setTimeout(() => document.addEventListener('pointerdown', h, true));
  return () => document.removeEventListener('pointerdown', h, true);
}

/** 浮层的打开 / 关闭（带动画），供弹出类组件共用 */
export function popup(panel) {
  let unfollow = null;
  return {
    open(anchor, opts) {
      panel.hidden = false;
      if (anchor) { place(anchor, panel, opts); unfollow?.(); unfollow = follow(anchor, panel, opts); }
      animateIn(panel, 'scale(0.96)');
    },
    async close() {
      if (panel.hidden) return;
      unfollow?.(); unfollow = null;
      await animateOut(panel);
      panel.hidden = true;
      panel.getAnimations().forEach((a) => a.cancel());
    },
  };
}

/** 是否应使用手机式交互（底部抽屉里的滚轮等） */
export const isCompact = () => matchMedia('(pointer: coarse) and (max-width: 640px)').matches;

/** 把字符串转成 DOM 片段 */
export function html(str) {
  const t = document.createElement('template');
  t.innerHTML = str.trim();
  return t.content.firstElementChild;
}

/** 转义要插进 HTML 的文本 */
export const esc = (s) => String(s).replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));

/** 派发一个冒泡的 change 事件，detail 里带上值 */
export const emit = (el, type, detail) => el.dispatchEvent(new CustomEvent(type, { bubbles: true, detail }));
