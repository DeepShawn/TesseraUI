// TesseraUI v0.6.0 — 由 scripts/build.mjs 从 tokens/*.json 生成，请勿手改。
// 选择器：Calendar、DatePicker、DateRangePicker、TimePicker、WheelPicker、ColorPicker。
// 桌面端在弹出面板里选；手机上（触屏且窄屏）改为底部抽屉，日期和时间用滚轮选。
import { icon, once, clamp, place, follow, onOutside, animateIn, animateOut, emit, isCompact, bottomSheet, cssVar, Spring, VelocityTracker, rubberBand, uid, springEasing, prefersReducedMotion } from '../core.js';
import { haptic } from './flow.js';

// ------------------------------------------------------------------
// 日期工具（本地时区，字符串格式 YYYY-MM-DD）
// ------------------------------------------------------------------
const pad = (n) => String(n).padStart(2, '0');
export const toKey = (d) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
export const fromKey = (k) => { if (!k) return null; const [y, m, d] = k.split('-').map(Number); return new Date(y, m - 1, d); };
const addDays = (d, n) => new Date(d.getFullYear(), d.getMonth(), d.getDate() + n);
const addMonths = (d, n) => new Date(d.getFullYear(), d.getMonth() + n, 1);
const sameMonth = (a, b) => a.getFullYear() === b.getFullYear() && a.getMonth() === b.getMonth();
const WEEK = ['一', '二', '三', '四', '五', '六', '日']; // 中国习惯：周一开始
export const formatDate = (k) => { const d = fromKey(k); return d ? `${d.getFullYear()} 年 ${d.getMonth() + 1} 月 ${d.getDate()} 日` : ''; };

// ------------------------------------------------------------------
// 日历：单选或范围；方向键移动、PageUp/PageDown 翻月、回车选中
// ------------------------------------------------------------------
export function calendar(root, { mode = 'single', value = null, min = null, max = null, months = 1, onSelect = () => {} } = {}) {
  root.classList.add('ts-calendars');
  let sel = mode === 'range' ? { start: value?.[0] ?? null, end: value?.[1] ?? null } : { start: value, end: null };
  let view = fromKey(sel.start) || new Date();
  view = new Date(view.getFullYear(), view.getMonth(), 1);
  let focusKey = sel.start || toKey(new Date());
  let hoverKey = null;
  const today = toKey(new Date());

  // 选中砖：单选时是一块独立的方块，换选日期时用弹簧滑过去，而不是原地瞬间换色
  const useIndicator = mode !== 'range' && months === 1;
  let indicator = null, ix = null, iy = null;
  const placeIndicator = (animate) => {
    if (!indicator) return;
    // 只在选中砖所在的这一格里找：月份切换动画期间，滑出中的旧月份也还在页面上
    const btn = indicator.parentElement?.querySelector(`.ts-day[data-key="${sel.start}"]:not([data-outside])`);
    if (!btn) { indicator.style.opacity = '0'; return; }
    const wasHidden = indicator.style.opacity === '0';
    indicator.style.width = `${btn.offsetWidth}px`;
    indicator.style.height = `${btn.offsetHeight}px`;
    indicator.style.opacity = '1';
    if (!animate || wasHidden || prefersReducedMotion()) {
      ix.set(btn.offsetLeft); iy.set(btn.offsetTop);
      if (animate && wasHidden && !prefersReducedMotion()) { const { easing, duration } = springEasing('bouncy'); indicator.animate([{ scale: 0.6 }, { scale: 1 }], { duration, easing }); }
      return;
    }
    ix.to(btn.offsetLeft); iy.to(btn.offsetTop);
    // 落下时像砖块压实一下
    const { easing, duration } = springEasing('bouncy');
    indicator.animate([{ scale: 0.86 }, { scale: 1 }], { duration, easing });
  };

  const render = (slide) => {
    // 月份切换：旧的日期格留在原位滑出，新的从另一侧滑入
    const oldGrids = slide && !prefersReducedMotion() ? [...root.querySelectorAll('.ts-calendar-grid')] : [];
    root.replaceChildren();
    for (let m = 0; m < months; m++) {
      const month = addMonths(view, m);
      const cal = document.createElement('div');
      cal.className = 'ts-calendar';
      const head = document.createElement('div');
      head.className = 'ts-calendar-head';
      const title = document.createElement('div');
      title.className = 'ts-calendar-title';
      title.setAttribute('aria-live', 'polite');
      title.textContent = `${month.getFullYear()} 年 ${month.getMonth() + 1} 月`;
      const nav = (name, label, d) => {
        const b = document.createElement('button');
        b.type = 'button';
        b.className = 'ts-icon-button ts-sm ts-pressable ts-state';
        b.setAttribute('aria-label', label);
        b.append(icon(name));
        b.addEventListener('click', () => { view = addMonths(view, d); focusKey = toKey(addMonths(fromKey(focusKey), d)); render(d > 0 ? 'next' : 'prev'); });
        return b;
      };
      if (m === 0) head.append(nav('chevron-left', '上个月', -1));
      head.append(title);
      if (m === months - 1) head.append(nav('chevron-right', '下个月', 1));
      const grid = document.createElement('div');
      grid.className = 'ts-calendar-grid';
      // 日期是一组切换按钮：用 group + aria-pressed（grid 角色要求 row / gridcell 结构）
      grid.setAttribute('role', 'group');
      grid.setAttribute('aria-label', title.textContent);
      if (slide) grid.dataset.slide = slide;
      WEEK.forEach((w) => { const s = document.createElement('div'); s.className = 'ts-calendar-dow'; s.textContent = w; s.setAttribute('aria-hidden', 'true'); grid.append(s); });
      const first = month;
      const offset = (first.getDay() + 6) % 7;
      const start = addDays(first, -offset);
      const [a, b] = rangeBounds();
      for (let i = 0; i < 42; i++) {
        const d = addDays(start, i);
        const k = toKey(d);
        const btn = document.createElement('button');
        btn.type = 'button';
        btn.className = 'ts-day';
        btn.textContent = d.getDate();
        btn.dataset.key = k;
        btn.setAttribute('aria-label', formatDate(k) + `，星期${WEEK[(d.getDay() + 6) % 7]}`);
        btn.tabIndex = k === focusKey && sameMonth(d, month) ? 0 : -1;
        if (!sameMonth(d, month)) btn.dataset.outside = '';
        if (d.getDay() === 0 || d.getDay() === 6) btn.dataset.weekend = '';
        if (k === today) { btn.dataset.today = ''; btn.setAttribute('aria-current', 'date'); }
        if ((min && k < min) || (max && k > max)) btn.disabled = true;
        const isSel = k === sel.start || k === sel.end;
        btn.setAttribute('aria-pressed', isSel);
        if (mode === 'range' && a && b) {
          if (k > a && k < b) btn.dataset.inRange = '';
          if (k === a) btn.dataset.rangeStart = '';
          if (k === b) btn.dataset.rangeEnd = '';
          if (k === a || k === b) btn.setAttribute('aria-pressed', 'true');
        }
        btn.addEventListener('click', () => pick(k));
        btn.addEventListener('pointerenter', () => { if (mode === 'range' && sel.start && !sel.end) { hoverKey = k; paintRange(); } });
        grid.append(btn);
      }
      grid.addEventListener('keydown', keydown);
      cal.append(head, grid);
      root.append(cal);
      // 第二个月只显示这个月的日期，避免两个月重复出现同一天
      if (months > 1) cal.querySelectorAll('.ts-day[data-outside]').forEach((x) => { x.style.visibility = 'hidden'; x.tabIndex = -1; });
      if (useIndicator) {
        grid.classList.add('has-indicator');
        indicator = document.createElement('span');
        indicator.className = 'ts-day-indicator';
        indicator.setAttribute('aria-hidden', 'true');
        indicator.style.opacity = '0';
        grid.prepend(indicator);
        ix = new Spring(0, 'snappy', () => (indicator.style.transform = `translate(${ix.value}px, ${iy.value}px)`));
        iy = new Spring(0, 'snappy', () => (indicator.style.transform = `translate(${ix.value}px, ${iy.value}px)`));
        ix.precision = iy.precision = 0.3;
      }
      const old = oldGrids[m];
      if (old) {
        // 像翻页一样整块推走：旧的一个月滑出，新的从另一侧滑入（新月份的入场在 CSS 的 data-slide 里，两者用同一条弹簧曲线同步）
        const dir = slide === 'next' ? -1 : 1;
        cal.style.position = 'relative';
        cal.style.clipPath = 'inset(-8px 0 -8px 0)'; // 只在动画期间左右裁切，平时不影响焦点框
        old.style.cssText = `position:absolute;left:${grid.offsetLeft}px;top:${grid.offsetTop}px;width:${grid.offsetWidth || old.offsetWidth}px;pointer-events:none;margin:0`;
        old.setAttribute('aria-hidden', 'true');
        old.inert = true;
        old.classList.add('is-leaving');
        old.removeAttribute('data-slide');
        old.querySelectorAll('button').forEach((b) => (b.tabIndex = -1));
        cal.append(old);
        const { easing, duration } = springEasing('smooth');
        old.animate([{ transform: 'none', opacity: 1 }, { transform: `translateX(calc(${dir * 100}% + ${dir * 24}px))`, opacity: 0.4 }], { duration, easing, fill: 'forwards' })
          .finished.then(() => { old.remove(); cal.style.clipPath = ''; });
      }
    }
    if (useIndicator) requestAnimationFrame(() => placeIndicator(false));
  };
  // 范围选择时，第二次点击前用悬停位置预览范围
  const rangeBounds = () => {
    const s = sel.start, e = sel.end || hoverKey;
    if (!s || !e) return [s, null];
    return s <= e ? [s, e] : [e, s];
  };
  const paintRange = () => {
    const [a, b] = rangeBounds();
    root.querySelectorAll('.ts-day').forEach((btn) => {
      const k = btn.dataset.key;
      btn.toggleAttribute('data-in-range', !!(a && b && k > a && k < b));
      btn.toggleAttribute('data-range-start', !!(b && k === a));
      btn.toggleAttribute('data-range-end', !!(b && k === b));
      btn.setAttribute('aria-pressed', k === a || k === b || k === sel.start);
    });
  };
  const pick = (k) => {
    focusKey = k;
    if (mode === 'range') {
      if (!sel.start || sel.end) sel = { start: k, end: null };
      else { sel = k < sel.start ? { start: k, end: sel.start } : { start: sel.start, end: k }; hoverKey = null; }
      paintRange();
      if (sel.end) onSelect([sel.start, sel.end]);
    } else {
      sel = { start: k, end: null };
      root.querySelectorAll('.ts-calendar-grid:not(.is-leaving) .ts-day').forEach((b) => { b.setAttribute('aria-pressed', b.dataset.key === k); b.tabIndex = b.dataset.key === k && !b.hasAttribute('data-outside') ? 0 : -1; });
      if (useIndicator) placeIndicator(true);
      onSelect(k);
    }
    root.querySelector(`.ts-day[data-key="${k}"]:not([data-outside])`)?.focus();
  };
  function keydown(e) {
    const step = { ArrowLeft: -1, ArrowRight: 1, ArrowUp: -7, ArrowDown: 7 }[e.key];
    let next = null;
    const cur = fromKey(focusKey);
    if (step) next = addDays(cur, step);
    else if (e.key === 'PageUp') next = new Date(cur.getFullYear(), cur.getMonth() - 1, Math.min(cur.getDate(), 28));
    else if (e.key === 'PageDown') next = new Date(cur.getFullYear(), cur.getMonth() + 1, Math.min(cur.getDate(), 28));
    else if (e.key === 'Home') next = addDays(cur, -((cur.getDay() + 6) % 7));
    else if (e.key === 'End') next = addDays(cur, 6 - ((cur.getDay() + 6) % 7));
    if (!next) return;
    e.preventDefault();
    focusKey = toKey(next);
    const last = addMonths(view, months - 1);
    let slide = null;
    if (next < view) { view = new Date(next.getFullYear(), next.getMonth(), 1); slide = 'prev'; }
    else if (next >= addMonths(last, 1)) { view = addMonths(new Date(next.getFullYear(), next.getMonth(), 1), -(months - 1)); slide = 'next'; }
    render(slide);
    root.querySelector(`.ts-day[data-key="${focusKey}"]:not([data-outside])`)?.focus();
  }
  render();
  return {
    get value() { return mode === 'range' ? [sel.start, sel.end] : sel.start; },
    set value(v) { sel = mode === 'range' ? { start: v?.[0] ?? null, end: v?.[1] ?? null } : { start: v, end: null }; if (sel.start) { const d = fromKey(sel.start); view = new Date(d.getFullYear(), d.getMonth(), 1); focusKey = sel.start; } render(); },
    focus() { root.querySelector('.ts-day[tabindex="0"]')?.focus(); },
  };
}

// ------------------------------------------------------------------
// 滚轮选择器：每列是一个原生滚动容器（scroll-snap 吸附），惯性和手感交给系统
// ------------------------------------------------------------------
export function wheel(root, { columns, onChange = () => {} } = {}) {
  root.classList.add('ts-wheel');
  root.replaceChildren();
  const cols = columns.map((col, ci) => {
    if (col.separator) { const s = document.createElement('span'); s.className = 'ts-wheel-sep'; s.textContent = col.separator; root.append(s); return null; }
    const c = wheelColumn(col, ci, () => onChange(values()));
    root.append(c.el);
    return c;
  }).filter(Boolean);
  const values = () => cols.map((c) => c.value);
  return {
    get value() { return values(); },
    /** 滚到第 i 列的第 valueIndex 项（弹簧动画） */
    set(i, valueIndex) { cols[i].scrollTo(valueIndex, true); },
    /** 替换第 i 列的选项（例如月份变了，“日”的天数跟着变），尽量保持原来的值 */
    setItems(i, items, value) { cols[i].setItems(items, value); },
  };
}

// 滚轮的一列：自己处理拖动、惯性和吸附，不用浏览器的滚动吸附（那是硬停，没有弹簧）。
// 位置 pos 以像素计，第 i 项在 pos = i * row 时居中。松手时带着手指的速度进入一个略欠阻尼的弹簧，
// 弹簧的目标是按速度投射后最近的一行：投射距离取 2ζv/ω，这样松手瞬间加速度为零，接手处没有顿挫，最后轻轻回弹落定。
// 选项沿圆柱排开：离中心越远，倾斜越大、越淡、越小，像真实的滚筒。
const WHEEL_SPRING = { omega: 13, zeta: 0.7 };
function wheelColumn(col, ci, changed) {
  const el = document.createElement('div');
  el.className = 'ts-wheel-col';
  el.tabIndex = 0;
  el.setAttribute('role', 'listbox');
  el.setAttribute('aria-label', col.label || `第 ${ci + 1} 列`);
  const track = document.createElement('div');
  track.className = 'ts-wheel-track';
  el.append(track);

  let items = [], nodes = [], index = 0;
  const row = () => parseFloat(getComputedStyle(el).getPropertyValue('--_row')) || 36;
  const maxPos = () => Math.max(0, (items.length - 1) * row());
  const { omega, zeta } = WHEEL_SPRING;
  const spring = new Spring(0, { stiffness: omega * omega, damping: 2 * zeta * omega }, (v) => render(v));
  spring.precision = 0.3;
  let pos = 0, shown = new Set(), lastMark = -1;
  let dragging = false, startY = 0, startPos = 0, moved = false, pid = null;

  // 圆柱：每行对应 18°，半径由行高决定，可见范围 ±4 行
  const STEP = 18 * Math.PI / 180;
  function render(p) {
    pos = p;
    const r = row(), radius = r / STEP, now = new Set();
    const center = pos / r;
    const from = Math.max(0, Math.floor(center - 4.5)), to = Math.min(items.length - 1, Math.ceil(center + 4.5));
    for (let i = from; i <= to; i++) {
      const a = clamp((i - center) * STEP, -Math.PI / 2, Math.PI / 2);
      const n = nodes[i];
      n.style.transform = `translateY(${(radius * Math.sin(a)).toFixed(2)}px) rotateX(${(-a * 180 / Math.PI).toFixed(2)}deg)`;
      n.style.opacity = Math.max(0, Math.cos(a) ** 1.6).toFixed(3);
      n.style.visibility = 'visible';
      now.add(i);
    }
    for (const i of shown) if (!now.has(i) && nodes[i]) nodes[i].style.visibility = 'hidden';
    shown = now;
    // 经过一行就更新高亮；流韵外观下每经过一行轻触一下
    const i = clamp(Math.round(center), 0, items.length - 1);
    if (i !== lastMark) {
      if (lastMark !== -1 && dragging && document.documentElement.dataset.shape === 'flow') haptic('selection');
      nodes[lastMark]?.setAttribute('aria-selected', 'false');
      nodes[i]?.setAttribute('aria-selected', 'true');
      if (nodes[i]) el.setAttribute('aria-activedescendant', nodes[i].id);
      lastMark = i;
    }
  }
  const settle = () => {
    const i = clamp(Math.round(pos / row()), 0, items.length - 1);
    if (i !== index) { index = i; changed(); }
  };
  const scrollTo = (i, animate, velocity) => {
    i = clamp(i, 0, items.length - 1);
    if (!animate) { spring.set(i * row()); settle(); return; }
    spring.to(i * row(), velocity === undefined ? {} : { velocity }).then(settle);
  };

  function setItems(list, value) {
    items = list;
    track.replaceChildren();
    nodes = list.map((it, i) => {
      const n = document.createElement('div');
      n.className = 'ts-wheel-item';
      n.id = uid('wheel');
      n.setAttribute('role', 'option');
      n.setAttribute('aria-selected', 'false');
      n.style.visibility = 'hidden';
      n.textContent = it.label;
      track.append(n);
      return n;
    });
    shown = new Set(); lastMark = -1;
    const k = list.findIndex((x) => x.value === value);
    index = k >= 0 ? k : clamp(index, 0, list.length - 1);
    spring.set(index * row());
  }

  // ---------- 拖动 ----------
  const tracker = new VelocityTracker();
  el.addEventListener('pointerdown', (e) => {
    if (e.button !== 0) return;
    spring.stop();
    dragging = true; moved = false; pid = e.pointerId;
    startY = e.clientY; startPos = pos;
    tracker.points = []; tracker.add(pos);
  });
  el.addEventListener('pointermove', (e) => {
    if (!dragging || e.pointerId !== pid) return;
    const dy = startY - e.clientY;
    if (!moved && Math.abs(dy) < 4) return;
    if (!moved) { moved = true; el.setPointerCapture(pid); el.classList.add('is-dragging'); }
    const raw = startPos + dy, max = maxPos(), h = el.clientHeight;
    // 拉出两端有橡皮筋阻力
    const p = raw < 0 ? rubberBand(raw, h) : raw > max ? max + rubberBand(raw - max, h) : raw;
    render(p); spring.value = p;
    tracker.add(p);
  });
  const release = (e) => {
    if (!dragging || e.pointerId !== pid) return;
    dragging = false; el.classList.remove('is-dragging');
    if (!moved) {
      // 轻点：选中点到的那一行
      const rect = el.getBoundingClientRect();
      const i = Math.round((pos + e.clientY - (rect.top + rect.height / 2)) / row());
      scrollTo(i, true);
      return;
    }
    const v = e.type === 'pointercancel' ? 0 : tracker.get();
    const projected = pos + v * (2 * zeta / omega);
    scrollTo(Math.round(projected / row()), true, v);
  };
  el.addEventListener('pointerup', release);
  el.addEventListener('pointercancel', release);

  // ---------- 鼠标滚轮与触控板 ----------
  let wheelTarget = null, wheelTimer = 0;
  el.addEventListener('wheel', (e) => {
    const r = row();
    const d = e.deltaMode === 1 ? e.deltaY * r : e.deltaMode === 2 ? e.deltaY * el.clientHeight : e.deltaY;
    const base = wheelTarget ?? spring.target;
    // 已在一端还往外滚：交给页面滚动
    if ((base <= 0 && d < 0) || (base >= maxPos() && d > 0)) return;
    e.preventDefault();
    const notch = e.deltaMode === 1 || (Math.abs(d) >= 50 && Number.isInteger(e.deltaY));
    if (notch) {
      // 鼠标滚轮：一格一行，弹簧过去
      wheelTarget = clamp(Math.round(base / r) + Math.sign(d), 0, items.length - 1) * r;
      spring.to(wheelTarget).then(() => { wheelTarget = null; settle(); });
    } else {
      // 触控板：跟着手指走，停下后吸附到最近一行
      wheelTarget = clamp(base + d, -r * 0.6, maxPos() + r * 0.6);
      spring.to(wheelTarget, { preset: { stiffness: 400, damping: 40 } });
      clearTimeout(wheelTimer);
      wheelTimer = setTimeout(() => {
        spring.setPreset({ stiffness: omega * omega, damping: 2 * zeta * omega });
        const t = wheelTarget; wheelTarget = null;
        scrollTo(Math.round(t / r), true);
      }, 120);
    }
  }, { passive: false });

  // ---------- 键盘 ----------
  el.addEventListener('keydown', (e) => {
    const d = { ArrowUp: -1, ArrowDown: 1, PageUp: -5, PageDown: 5 }[e.key];
    // 动画还没停时连按，以上一次的目标为基准累加
    const target = Math.round(spring.target / row());
    if (d) { e.preventDefault(); scrollTo(target + d, true); }
    if (e.key === 'Home') { e.preventDefault(); scrollTo(0, true); }
    if (e.key === 'End') { e.preventDefault(); scrollTo(items.length - 1, true); }
  });

  setItems(col.items, col.value);
  // 加入页面、量出行高之后再摆一次
  requestAnimationFrame(() => spring.set(index * row()));
  return {
    el, col, scrollTo, setItems,
    get index() { return index; },
    get value() { return items[index]?.value; },
  };
}

// ------------------------------------------------------------------
// 弹出层：桌面是锚定面板，手机是底部抽屉
// ------------------------------------------------------------------
function makeSurface(anchor, title) {
  if (isCompact()) {
    const sheet = document.createElement('div');
    sheet.className = 'ts-sheet';
    sheet.style.height = 'auto';
    sheet.setAttribute('aria-label', title);
    sheet.innerHTML = `<div class="ts-sheet-handle"></div><h3 class="ts-title-3" style="margin:4px 0 16px">${title}</h3><div class="ts-picker-body"></div>`;
    document.body.append(sheet);
    const ctl = bottomSheet(sheet, { detents: [1] });
    return { body: sheet.querySelector('.ts-picker-body'), el: sheet, open: () => ctl.open(), close: () => ctl.close(), compact: true };
  }
  const panel = document.createElement('div');
  panel.className = 'ts-picker-panel';
  panel.setAttribute('role', 'dialog');
  panel.setAttribute('aria-label', title);
  panel.hidden = true;
  panel.innerHTML = '<div class="ts-picker-body"></div>';
  document.body.append(panel);
  let off = null, unfollow = null;
  const api = {
    body: panel.firstElementChild, el: panel, compact: false,
    open() {
      if (!panel.hidden) return;
      panel.hidden = false;
      place(anchor, panel, { placement: 'bottom-start' });
      unfollow = follow(anchor, panel, { placement: 'bottom-start' });
      animateIn(panel, 'scale(0.96)');
      off = onOutside([anchor, panel], () => api.close());
    },
    async close() {
      if (panel.hidden) return;
      off?.();
      unfollow?.(); unfollow = null;
      await animateOut(panel);
      panel.hidden = true;
      panel.getAnimations().forEach((a) => a.cancel());
    },
  };
  panel.addEventListener('keydown', (e) => { if (e.key === 'Escape') { api.close(); anchor.querySelector('input')?.focus(); } });
  return api;
}
const footer = (buttons) => {
  const f = document.createElement('div');
  f.className = 'ts-picker-footer';
  buttons.forEach(([label, cls, fn]) => {
    const b = document.createElement('button');
    b.type = 'button';
    b.className = `ts-button ts-sm ${cls} ts-pressable ts-state`;
    b.textContent = label;
    b.addEventListener('click', fn);
    f.append(b);
  });
  return f;
};
const bindTrigger = (root, input, open) => {
  root.classList.add('ts-picker-trigger');
  input.readOnly = true;
  input.setAttribute('aria-haspopup', 'dialog');
  root.addEventListener('click', open);
  input.addEventListener('keydown', (e) => { if (e.key === 'Enter' || e.key === ' ' || e.key === 'ArrowDown') { e.preventDefault(); open(); } });
};

// ------------------------------------------------------------------
// 日期选择器
// ------------------------------------------------------------------
export function datePicker(root) {
  const input = root.querySelector('input');
  const hidden = document.createElement('input');
  hidden.type = 'hidden';
  hidden.name = input.name;
  input.removeAttribute('name');
  root.append(hidden);
  const min = root.dataset.min || null, max = root.dataset.max || null;
  let value = root.dataset.value || null;
  const show = () => { input.value = value ? formatDate(value) : ''; hidden.value = value || ''; };
  let surface = null;
  const commit = (k) => { value = k; show(); emit(root, 'change', { value }); surface.close(); input.focus(); };
  const open = () => {
    surface ??= makeSurface(root, '选择日期');
    surface.body.replaceChildren();
    if (surface.compact) {
      // 手机：年 / 月 / 日 三列滚轮
      const d = fromKey(value) || new Date();
      const years = Array.from({ length: 101 }, (_, i) => d.getFullYear() - 50 + i);
      const w = document.createElement('div');
      let y = d.getFullYear(), mo = d.getMonth() + 1, da = d.getDate();
      const daysOf = (yy, mm) => Array.from({ length: new Date(yy, mm, 0).getDate() }, (_, i) => ({ label: `${i + 1} 日`, value: i + 1 }));
      const wp = wheel(w, {
        columns: [
          { label: '年', value: y, items: years.map((v) => ({ label: `${v} 年`, value: v })) },
          { label: '月', value: mo, items: Array.from({ length: 12 }, (_, i) => ({ label: `${i + 1} 月`, value: i + 1 })) },
          { label: '日', value: Math.min(da, new Date(y, mo, 0).getDate()), items: daysOf(y, mo) },
        ],
        onChange: ([ny, nm, nd]) => {
          const days = new Date(ny, nm, 0).getDate();
          const rebuild = days !== new Date(y, mo, 0).getDate();
          [y, mo, da] = [ny, nm, Math.min(nd, days)];
          // 月份天数变了，只替换“日”这一列，其他两列不动
          if (rebuild) wp.setItems(2, daysOf(y, mo), da);
        },
      });
      surface.body.append(w, footer([['取消', '', () => surface.close()], ['确定', 'ts-primary', () => commit(`${y}-${pad(mo)}-${pad(da)}`)]]));
    } else {
      const cal = document.createElement('div');
      const c = calendar(cal, { value, min, max, onSelect: commit });
      surface.body.append(cal, footer([['清除', 'ts-text', () => { value = null; show(); emit(root, 'change', { value }); surface.close(); }], ['今天', '', () => commit(toKey(new Date()))]]));
      requestAnimationFrame(() => c.focus());
    }
    surface.open();
  };
  bindTrigger(root, input, open);
  show();
  return { get value() { return value; }, set value(v) { value = v; show(); } };
}

// ------------------------------------------------------------------
// 日期范围选择器：桌面并排两个月
// ------------------------------------------------------------------
export function dateRangePicker(root) {
  const input = root.querySelector('input');
  let value = (root.dataset.value || '').split(',').filter(Boolean);
  if (value.length !== 2) value = [null, null];
  const show = () => { input.value = value[0] ? `${formatDate(value[0])} – ${formatDate(value[1])}` : ''; };
  let surface = null;
  const open = () => {
    surface ??= makeSurface(root, '选择日期范围');
    surface.body.replaceChildren();
    const cal = document.createElement('div');
    const hint = document.createElement('span');
    hint.className = 'ts-caption ts-secondary';
    hint.textContent = '先选开始日期，再选结束日期';
    const c = calendar(cal, {
      mode: 'range', value, months: surface.compact ? 1 : 2,
      onSelect: (v) => { value = v; show(); emit(root, 'change', { value }); setTimeout(() => surface.close(), 180); },
    });
    const f = footer([['清除', 'ts-text', () => { value = [null, null]; show(); c.value = value; emit(root, 'change', { value }); }]]);
    f.prepend(hint);
    surface.body.append(cal, f);
    surface.open();
    requestAnimationFrame(() => c.focus());
  };
  bindTrigger(root, input, open);
  show();
  return { get value() { return value; } };
}

// ------------------------------------------------------------------
// 时间选择器：时、分两列滚轮
// ------------------------------------------------------------------
export function timePicker(root) {
  const input = root.querySelector('input');
  const step = Number(root.dataset.step || 1);
  let value = root.dataset.value || '';
  const show = () => { input.value = value; };
  let surface = null;
  const open = () => {
    surface ??= makeSurface(root, '选择时间');
    surface.body.replaceChildren();
    let [h, m] = (value || '09:00').split(':').map(Number);
    m = Math.round(m / step) * step % 60;
    const w = document.createElement('div');
    w.style.minWidth = '220px';
    wheel(w, {
      columns: [
        { label: '时', value: h, items: Array.from({ length: 24 }, (_, i) => ({ label: pad(i), value: i })) },
        { separator: ':' },
        { label: '分', value: m, items: Array.from({ length: 60 / step }, (_, i) => ({ label: pad(i * step), value: i * step })) },
      ],
      onChange: ([nh, nm]) => { h = nh; m = nm; },
    });
    surface.body.append(w, footer([
      ['现在', 'ts-text', () => { const d = new Date(); value = `${pad(d.getHours())}:${pad(Math.floor(d.getMinutes() / step) * step)}`; show(); emit(root, 'change', { value }); surface.close(); }],
      ['确定', 'ts-primary', () => { value = `${pad(h)}:${pad(m)}`; show(); emit(root, 'change', { value }); surface.close(); input.focus(); }],
    ]));
    surface.open();
    requestAnimationFrame(() => w.querySelector('.ts-wheel-col')?.focus({ preventScroll: true }));
  };
  bindTrigger(root, input, open);
  show();
  return { get value() { return value; } };
}

// ------------------------------------------------------------------
// 颜色选择器：饱和度 / 明度面板 + 色相条 + HEX 输入 + 预设色（取 TesseraUI 的颜料色）
// ------------------------------------------------------------------
const hsvToRgb = (h, s, v) => {
  const f = (n) => { const k = (n + h / 60) % 6; return v - v * s * Math.max(0, Math.min(k, 4 - k, 1)); };
  return [f(5), f(3), f(1)].map((x) => Math.round(x * 255));
};
const rgbToHsv = (r, g, b) => {
  r /= 255; g /= 255; b /= 255;
  const mx = Math.max(r, g, b), mn = Math.min(r, g, b), d = mx - mn;
  let h = 0;
  if (d) h = mx === r ? ((g - b) / d) % 6 : mx === g ? (b - r) / d + 2 : (r - g) / d + 4;
  return [(h * 60 + 360) % 360, mx ? d / mx : 0, mx];
};
const toHex = (rgb) => '#' + rgb.map((x) => x.toString(16).padStart(2, '0')).join('').toUpperCase();
const parseHex = (s) => { const m = /^#?([0-9a-f]{3}|[0-9a-f]{6})$/i.exec(s.trim()); if (!m) return null; let h = m[1]; if (h.length === 3) h = h.split('').map((c) => c + c).join(''); return [0, 2, 4].map((i) => parseInt(h.slice(i, i + 2), 16)); };

export function colorPicker(root, { value = '#0F6B63', onChange = () => {} } = {}) {
  root.classList.add('ts-colorpicker');
  root.innerHTML = `
    <div class="ts-color-area" role="slider" tabindex="0" aria-label="饱和度和明度"><span class="ts-color-handle"></span></div>
    <div class="ts-color-hue" role="slider" tabindex="0" aria-label="色相" aria-valuemin="0" aria-valuemax="360"><span class="ts-color-handle"></span></div>
    <div class="ts-color-row"><span class="ts-color-preview"></span><div class="ts-input ts-sm" style="flex:1"><input aria-label="十六进制颜色值" spellcheck="false" maxlength="7"></div></div>
    <div class="ts-color-swatches" role="group" aria-label="预设颜色"></div>`;
  const [area, hue, row, swatchesEl] = root.children;
  const hex = row.querySelector('input'), preview = row.querySelector('.ts-color-preview');
  let [h, s, v] = rgbToHsv(...(parseHex(value) || [15, 107, 99]));
  const names = ['accent-solid', 'pigment-cinnabar', 'pigment-ochre', 'pigment-olive', 'pigment-lapis', 'pigment-amethyst', 'danger-solid', 'text-primary'];
  names.forEach((n) => {
    const c = cssVar(`--ts-${n}`);
    const b = document.createElement('button');
    b.type = 'button';
    b.style.backgroundColor = c;
    b.dataset.hex = c.toUpperCase();
    b.setAttribute('aria-label', c);
    b.addEventListener('click', () => { [h, s, v] = rgbToHsv(...parseHex(c)); update(true); });
    swatchesEl.append(b);
  });
  const update = (fire, fromInput) => {
    const rgb = hsvToRgb(h, s, v), x = toHex(rgb);
    area.style.setProperty('--_hue', h);
    area.firstElementChild.style.cssText = `left:${s * 100}%;top:${(1 - v) * 100}%;background:${x}`;
    hue.firstElementChild.style.cssText = `left:${(h / 360) * 100}%;background:hsl(${h} 100% 50%)`;
    hue.setAttribute('aria-valuenow', Math.round(h));
    area.setAttribute('aria-valuetext', `饱和度 ${Math.round(s * 100)}%，明度 ${Math.round(v * 100)}%`);
    preview.style.background = x;
    if (!fromInput) hex.value = x;
    swatchesEl.querySelectorAll('button').forEach((b) => b.setAttribute('aria-pressed', b.dataset.hex === x));
    if (fire) { onChange(x); emit(root, 'change', { value: x }); }
  };
  const drag = (el, fn) => {
    const at = (e) => { const r = el.getBoundingClientRect(); fn(clamp((e.clientX - r.left) / r.width, 0, 1), clamp((e.clientY - r.top) / r.height, 0, 1)); update(true); };
    el.addEventListener('pointerdown', (e) => { el.setPointerCapture(e.pointerId); at(e); el.onpointermove = at; });
    el.addEventListener('pointerup', () => (el.onpointermove = null));
  };
  drag(area, (x, y) => { s = x; v = 1 - y; });
  drag(hue, (x) => { h = x * 360; });
  area.addEventListener('keydown', (e) => {
    const d = { ArrowLeft: [-0.02, 0], ArrowRight: [0.02, 0], ArrowUp: [0, 0.02], ArrowDown: [0, -0.02] }[e.key];
    if (d) { e.preventDefault(); s = clamp(s + d[0], 0, 1); v = clamp(v + d[1], 0, 1); update(true); }
  });
  hue.addEventListener('keydown', (e) => {
    const d = { ArrowLeft: -2, ArrowDown: -2, ArrowRight: 2, ArrowUp: 2 }[e.key];
    if (d) { e.preventDefault(); h = (h + d + 360) % 360; update(true); }
  });
  hex.addEventListener('input', () => { const rgb = parseHex(hex.value); if (rgb) { [h, s, v] = rgbToHsv(...rgb); update(true, true); } });
  hex.addEventListener('blur', () => update(false));
  update(false);
  return { get value() { return toHex(hsvToRgb(h, s, v)); } };
}

/** 颜色输入框：点击弹出颜色选择器 */
export function colorField(root) {
  const input = root.querySelector('input');
  const sw = document.createElement('span');
  sw.className = 'ts-color-trigger-swatch';
  root.prepend(sw);
  let value = root.dataset.value || '#0F6B63';
  const show = () => { input.value = value; sw.style.background = value; };
  let surface = null;
  bindTrigger(root, input, () => {
    surface ??= makeSurface(root, '选择颜色');
    surface.body.replaceChildren();
    const p = document.createElement('div');
    colorPicker(p, { value, onChange: (x) => { value = x; show(); emit(root, 'change', { value }); } });
    surface.body.append(p);
    surface.open();
  });
  show();
}

export function initPickers(root = document) {
  const $ = (sel, fn) => root.querySelectorAll(sel).forEach((el) => once(el, sel, fn));
  $('.ts-calendar-inline', (el) => calendar(el, { mode: el.dataset.mode, months: Number(el.dataset.months || 1), value: el.dataset.mode === 'range' ? (el.dataset.value || '').split(',') : el.dataset.value }));
  $('.ts-datepicker', datePicker);
  $('.ts-daterange', dateRangePicker);
  $('.ts-timepicker', timePicker);
  $('.ts-colorfield', colorField);
  $('.ts-colorpicker-inline', (el) => colorPicker(el, { value: el.dataset.value }));
}
