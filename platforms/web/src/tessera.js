// TesseraUI Web 运行时：弹簧动画、手势、浮层、主题切换、中文输入法处理。无第三方依赖。
import { tokens } from './tokens.mjs';
import { icons } from './icons.mjs';

const { spring: SPRINGS, motion: M } = tokens;

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
        const done = Math.abs(this.value - this.target) < 0.01 && Math.abs(this.velocity) < 0.1;
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
class VelocityTracker {
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
const rubberBand = (overshoot, dimension, c = M.gesture.rubberBand) =>
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
  root.prepend(indicator);
  // offsetLeft 和绝对定位的 left 都以容器的内边距边为基准
  const moveTo = edgeIndicator(root, indicator, (label) => ({ l: label.offsetLeft, r: label.offsetLeft + label.offsetWidth }));
  const sync = (animate) => {
    const cur = root.querySelector('input:checked');
    if (cur) moveTo(cur.closest('label'), animate);
  };
  root.addEventListener('change', () => sync(true));
  sync(false);
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
const cssVar = (name) => getComputedStyle(document.documentElement).getPropertyValue(name).trim();
const springEasing = (name) => ({ easing: cssVar(`--ts-spring-${name}`), duration: parseFloat(cssVar(`--ts-spring-${name}-duration`)) });

function animateIn(el, from, preset = 'smooth') {
  if (prefersReducedMotion()) return el.animate([{ opacity: 0 }, { opacity: 1 }], { duration: M.reducedMotion.duration }).finished;
  const { easing, duration } = springEasing(preset);
  // 透明度用“时长 + 曲线”，变换用弹簧
  el.animate([{ opacity: 0 }, { opacity: 1 }], { duration: M.duration.normal, easing: 'cubic-bezier(0.2, 0, 0, 1)' });
  return el.animate([{ transform: from }, { transform: 'none' }], { duration, easing }).finished;
}
function animateOut(el, to = 'none') {
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
export function init(root = document) {
  document.documentElement.classList.add('ts-root');
  hydrateIcons(root);
  root.querySelectorAll('.ts-segmented').forEach(segmented);
  root.querySelectorAll('.ts-tabs').forEach((t) => tabs(t));
  guardEnterWhileComposing(root);
}

export default { init, Spring, icon, hydrateIcons, segmented, tabs, openDialog, closeDialog, menu, toast, bottomSheet, setTheme, setContrast, onCommittedInput, guardEnterWhileComposing };
