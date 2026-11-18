// TesseraUI v0.3.1 — 由 scripts/build.mjs 从 tokens/*.json 生成，请勿手改。
// 选择器：Calendar、DatePicker、DateRangePicker、TimePicker、WheelPicker、ColorPicker。
// 桌面端在弹出面板里选；手机上（触屏且窄屏）改为底部抽屉，日期和时间用滚轮选。
import { icon, once, clamp, place, follow, onOutside, animateIn, animateOut, emit, isCompact, bottomSheet, cssVar } from '../core.js';

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

  const render = (slide) => {
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
      grid.setAttribute('role', 'grid');
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
        if (k === today) btn.dataset.today = '';
        if ((min && k < min) || (max && k > max)) btn.disabled = true;
        const isSel = k === sel.start || k === sel.end;
        btn.setAttribute('aria-selected', isSel);
        if (mode === 'range' && a && b) {
          if (k > a && k < b) btn.dataset.inRange = '';
          if (k === a) btn.dataset.rangeStart = '';
          if (k === b) btn.dataset.rangeEnd = '';
          if (k === a || k === b) btn.setAttribute('aria-selected', 'true');
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
    }
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
      btn.setAttribute('aria-selected', k === a || k === b || k === sel.start);
    });
  };
  const pick = (k) => {
    focusKey = k;
    if (mode === 'range') {
      if (!sel.start || sel.end) sel = { start: k, end: null };
      else { sel = k < sel.start ? { start: k, end: sel.start } : { start: sel.start, end: k }; hoverKey = null; }
      render();
      if (sel.end) onSelect([sel.start, sel.end]);
    } else {
      sel = { start: k, end: null };
      render();
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
    const el = document.createElement('div');
    el.className = 'ts-wheel-col';
    el.tabIndex = 0;
    el.setAttribute('role', 'listbox');
    el.setAttribute('aria-label', col.label || `第 ${ci + 1} 列`);
    col.items.forEach((it, i) => {
      const item = document.createElement('div');
      item.className = 'ts-wheel-item';
      item.setAttribute('role', 'option');
      item.textContent = it.label;
      item.addEventListener('click', () => scrollTo(i, true));
      el.append(item);
    });
    root.append(el);
    const row = () => el.firstElementChild?.offsetHeight || 36;
    let index = Math.max(0, col.items.findIndex((x) => x.value === col.value));
    const mark = (i) => [...el.children].forEach((c, k) => c.setAttribute('aria-selected', k === i));
    const scrollTo = (i, smooth) => { i = clamp(i, 0, col.items.length - 1); el.scrollTo({ top: i * row(), behavior: smooth ? 'smooth' : 'instant' }); if (!smooth) settle(); };
    const current = () => clamp(Math.round(el.scrollTop / row()), 0, col.items.length - 1);
    let raf = 0, timer = 0, pending = null;
    const settle = () => {
      pending = null;
      const i = current();
      mark(i);
      if (i !== index) { index = i; onChange(values()); }
    };
    el.addEventListener('scroll', () => {
      cancelAnimationFrame(raf);
      raf = requestAnimationFrame(() => mark(current())); // 滚动中实时更新高亮
      clearTimeout(timer);
      timer = setTimeout(settle, 120); // 不支持 scrollend 的浏览器兜底
    }, { passive: true });
    el.addEventListener('scrollend', settle);
    el.addEventListener('keydown', (e) => {
      const d = { ArrowUp: -1, ArrowDown: 1, PageUp: -5, PageDown: 5 }[e.key];
      // 平滑滚动还没停时连按，以上一次的目标为基准累加
      if (d) { e.preventDefault(); pending = clamp((pending ?? current()) + d, 0, col.items.length - 1); scrollTo(pending, true); }
      if (e.key === 'Home') { e.preventDefault(); scrollTo(0, true); }
      if (e.key === 'End') { e.preventDefault(); scrollTo(col.items.length - 1, true); }
    });
    requestAnimationFrame(() => scrollTo(index, false));
    return { col, get index() { return index; }, scrollTo };
  }).filter(Boolean);
  const values = () => cols.map((c) => c.col.items[c.index].value);
  return { get value() { return values(); }, set(i, valueIndex) { cols[i].scrollTo(valueIndex, true); } };
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
      const build = () => wheel(w, {
        columns: [
          { label: '年', value: y, items: years.map((v) => ({ label: `${v} 年`, value: v })) },
          { label: '月', value: mo, items: Array.from({ length: 12 }, (_, i) => ({ label: `${i + 1} 月`, value: i + 1 })) },
          { label: '日', value: Math.min(da, new Date(y, mo, 0).getDate()), items: Array.from({ length: new Date(y, mo, 0).getDate() }, (_, i) => ({ label: `${i + 1} 日`, value: i + 1 })) },
        ],
        onChange: ([ny, nm, nd]) => {
          const days = new Date(ny, nm, 0).getDate();
          const rebuild = days !== new Date(y, mo, 0).getDate();
          [y, mo, da] = [ny, nm, Math.min(nd, days)];
          if (rebuild) build(); // 月份天数变了，重建“日”这一列
        },
      });
      build();
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
