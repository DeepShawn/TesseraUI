// TesseraUI v0.6.0 — 由 scripts/build.mjs 从 tokens/*.json 生成，请勿手改。
// 操作与输入类组件的交互：TextArea 自动高度、PasswordField、NumberInput、OTPInput、
// Autocomplete、TagInput、Slider、Rating、FileUpload、表单校验。
import {
  Spring, motion as M, icon, once, uid, clamp, place, follow, onOutside, animateIn, animateOut, emit, esc,
  onCommittedInput,
} from '../core.js';

/** 全角数字、全角小数点和负号转半角（中文输入法下常见） */
export const toHalfWidth = (s) =>
  s.replace(/[０-９]/g, (c) => String.fromCharCode(c.charCodeAt(0) - 0xfee0)).replace(/．|。/g, '.').replace(/－/g, '-');

// ------------------------------------------------------------------
// 多行文本：自动高度
// ------------------------------------------------------------------
export function autosize(root) {
  const ta = root.matches('textarea') ? root : root.querySelector('textarea');
  const fit = () => { ta.style.height = 'auto'; ta.style.height = `${ta.scrollHeight}px`; };
  ta.addEventListener('input', fit);
  requestAnimationFrame(fit);
}

// ------------------------------------------------------------------
// 密码框：显示 / 隐藏，大写锁定提示
// ------------------------------------------------------------------
export function passwordField(root) {
  const input = root.querySelector('input');
  const btn = document.createElement('button');
  btn.type = 'button';
  btn.className = 'ts-icon-button ts-reveal ts-pressable ts-state';
  const sync = () => {
    const shown = input.type === 'text';
    btn.replaceChildren(icon(shown ? 'eye-off' : 'eye'));
    btn.setAttribute('aria-label', shown ? '隐藏密码' : '显示密码');
    btn.setAttribute('aria-pressed', shown);
  };
  btn.addEventListener('click', () => {
    const pos = input.selectionStart;
    input.type = input.type === 'password' ? 'text' : 'password';
    sync();
    input.focus();
    input.setSelectionRange(pos, pos);
  });
  root.append(btn);
  sync();
  const field = root.closest('.ts-field');
  const caps = (e) => { if (field && e.getModifierState) field.toggleAttribute('data-caps', e.getModifierState('CapsLock')); };
  input.addEventListener('keydown', caps);
  input.addEventListener('keyup', caps);
  input.addEventListener('blur', () => field?.removeAttribute('data-caps'));
}

// ------------------------------------------------------------------
// 数字步进器：长按连续加减（越按越快），方向键、PageUp/PageDown、Home/End，全角数字自动转换
// ------------------------------------------------------------------
export function numberInput(root) {
  const input = root.querySelector('input');
  const num = (name, d) => (input.getAttribute(name) !== null && input.getAttribute(name) !== '' ? Number(input.getAttribute(name)) : d);
  const min = num('min', -Infinity), max = num('max', Infinity), step = num('step', 1);
  const decimals = (String(step).split('.')[1] || '').length;
  input.type = 'text';
  input.inputMode = decimals ? 'decimal' : 'numeric';
  input.setAttribute('role', 'spinbutton');
  input.autocomplete = 'off';
  if (Number.isFinite(min)) input.setAttribute('aria-valuemin', min);
  if (Number.isFinite(max)) input.setAttribute('aria-valuemax', max);

  const wrap = document.createElement('div');
  wrap.className = 'ts-input';
  input.replaceWith(wrap);
  wrap.append(input);
  const mk = (name, label, delta) => {
    const b = document.createElement('button');
    b.type = 'button';
    b.tabIndex = -1; // 键盘用方向键，按钮只给指针用
    b.className = 'ts-icon-button ts-filled ts-pressable ts-state';
    b.setAttribute('aria-label', label);
    b.append(icon(name));
    let timer;
    const stop = () => clearTimeout(timer);
    b.addEventListener('pointerdown', (e) => {
      e.preventDefault();
      bump(delta);
      let wait = 400;
      const repeat = () => { bump(delta); wait = Math.max(40, wait * 0.8); timer = setTimeout(repeat, wait); };
      timer = setTimeout(repeat, wait);
    });
    ['pointerup', 'pointerleave', 'pointercancel'].forEach((t) => b.addEventListener(t, stop));
    return b;
  };
  const dec = mk('minus', '减少', -1), inc = mk('plus', '增加', 1);
  root.prepend(dec);
  root.append(inc);

  const read = () => { const v = parseFloat(toHalfWidth(input.value)); return Number.isFinite(v) ? v : null; };
  const set = (v, fire = true) => {
    v = clamp(Math.round(v / step) * step, min, max);
    const text = v.toFixed(decimals);
    const changed = input.value !== text;
    input.value = text;
    input.setAttribute('aria-valuenow', v);
    dec.disabled = v <= min;
    inc.disabled = v >= max;
    if (changed && fire) emit(input, 'change', { value: v });
  };
  const bump = (n) => set((read() ?? (Number.isFinite(min) ? min : 0)) + n * step);
  input.addEventListener('keydown', (e) => {
    if (e.isComposing) return;
    const map = { ArrowUp: 1, ArrowDown: -1, PageUp: 10, PageDown: -10 };
    if (map[e.key]) { e.preventDefault(); bump(map[e.key]); }
    if (e.key === 'Home' && Number.isFinite(min)) { e.preventDefault(); set(min); }
    if (e.key === 'End' && Number.isFinite(max)) { e.preventDefault(); set(max); }
  });
  input.addEventListener('blur', () => { const v = read(); if (v !== null) set(v); });
  const v0 = read();
  if (v0 !== null) set(v0, false);
}

// ------------------------------------------------------------------
// 验证码：一个真正的输入框（支持粘贴、短信自动填充、读屏），格子只负责显示
// ------------------------------------------------------------------
export function otpInput(root) {
  const length = Number(root.dataset.length || 6);
  const input = document.createElement('input');
  Object.assign(input, { inputMode: 'numeric', autocomplete: 'one-time-code', maxLength: length, spellcheck: false });
  input.setAttribute('aria-label', root.getAttribute('aria-label') || `${length} 位验证码`);
  const cells = Array.from({ length }, () => {
    const c = document.createElement('span');
    c.className = 'ts-otp-cell';
    c.setAttribute('aria-hidden', 'true');
    return c;
  });
  root.append(...cells, input);
  const render = () => {
    const v = input.value;
    const active = document.activeElement === input ? Math.min(v.length, length - 1) : -1;
    cells.forEach((c, i) => {
      const was = c.textContent;
      c.textContent = v[i] || '';
      c.toggleAttribute('data-filled', !!v[i]);
      if (v[i] && was !== v[i]) { c.removeAttribute('data-filled'); void c.offsetWidth; c.setAttribute('data-filled', ''); } // 重新触发落砖动画
      c.toggleAttribute('data-active', i === active);
    });
  };
  const clean = () => {
    const v = toHalfWidth(input.value).replace(/\D/g, '').slice(0, length);
    if (v !== input.value) input.value = v;
    root.removeAttribute('data-invalid');
    render();
    if (v.length === length) emit(root, 'complete', { value: v });
  };
  let composing = false;
  input.addEventListener('compositionstart', () => (composing = true));
  input.addEventListener('compositionend', () => { composing = false; clean(); });
  input.addEventListener('input', () => { if (!composing) clean(); });
  ['focus', 'blur', 'select', 'keyup', 'click'].forEach((t) => input.addEventListener(t, render));
  root.addEventListener('click', () => input.focus());
  render();
  return {
    get value() { return input.value; },
    clear() { input.value = ''; render(); },
    invalid() { root.setAttribute('data-invalid', ''); input.select(); },
  };
}

// ------------------------------------------------------------------
// 下拉列表（自动补全、选择器共用）：键盘上下、回车选中、Esc 关闭
// ------------------------------------------------------------------
export function listbox({ anchor, input, onPick }) {
  const list = document.createElement('ul');
  list.className = 'ts-listbox';
  list.id = uid('listbox');
  list.setAttribute('role', 'listbox');
  list.hidden = true;
  document.body.append(list);
  let items = [], active = -1, offOutside = null, unfollow = null;
  input.setAttribute('role', 'combobox');
  input.setAttribute('aria-controls', list.id);
  input.setAttribute('aria-expanded', 'false');
  input.setAttribute('aria-autocomplete', 'list');

  const highlight = (i) => {
    active = i;
    [...list.children].forEach((li, k) => li.setAttribute('aria-selected', k === i));
    const li = list.children[i];
    if (li && li.id) { input.setAttribute('aria-activedescendant', li.id); li.scrollIntoView({ block: 'nearest' }); }
    else input.removeAttribute('aria-activedescendant');
  };
  const api = {
    render(options, query = '') {
      items = options;
      list.replaceChildren();
      if (!options.length) {
        list.innerHTML = `<li class="ts-listbox-empty" role="presentation">没有匹配的结果</li>`;
      }
      options.forEach((o, i) => {
        const li = document.createElement('li');
        li.className = 'ts-option';
        li.id = `${list.id}-${i}`;
        li.setAttribute('role', 'option');
        const label = esc(o.label);
        const q = query.trim();
        const at = q ? o.label.toLowerCase().indexOf(q.toLowerCase()) : -1;
        li.innerHTML = (at >= 0 ? `${esc(o.label.slice(0, at))}<mark>${esc(o.label.slice(at, at + q.length))}</mark>${esc(o.label.slice(at + q.length))}` : label) +
          (o.hint ? `<span class="ts-caption">${esc(o.hint)}</span>` : '');
        li.addEventListener('pointerdown', (e) => e.preventDefault()); // 不让输入框失焦
        li.addEventListener('click', () => onPick(o));
        list.append(li);
      });
      highlight(options.length ? 0 : -1);
    },
    open() {
      if (!list.hidden) return;
      list.hidden = false;
      list.style.minWidth = `${anchor.offsetWidth}px`;
      place(anchor, list, { placement: 'bottom-start', offset: 6 });
      // 页面滚动、手机键盘弹出时跟着输入框走，不会盖住输入框
      unfollow = follow(anchor, list, { placement: 'bottom-start', offset: 6 });
      animateIn(list, 'scale(0.98)');
      input.setAttribute('aria-expanded', 'true');
      offOutside = onOutside([anchor, list], () => api.close());
    },
    async close() {
      if (list.hidden) return;
      offOutside?.();
      unfollow?.(); unfollow = null;
      input.setAttribute('aria-expanded', 'false');
      input.removeAttribute('aria-activedescendant');
      await animateOut(list);
      list.hidden = true;
      list.getAnimations().forEach((a) => a.cancel());
    },
    get isOpen() { return !list.hidden; },
    keydown(e) {
      if (e.isComposing || e.keyCode === 229) return false;
      if (e.key === 'ArrowDown') { e.preventDefault(); if (list.hidden) api.open(); else highlight(Math.min(items.length - 1, active + 1)); return true; }
      if (e.key === 'ArrowUp') { e.preventDefault(); highlight(Math.max(0, active - 1)); return true; }
      if (e.key === 'Enter' && !list.hidden && items[active]) { e.preventDefault(); onPick(items[active]); return true; }
      if (e.key === 'Escape' && !list.hidden) { e.preventDefault(); api.close(); return true; }
      return false;
    },
    reposition() { if (!list.hidden) place(anchor, list, { placement: 'bottom-start', offset: 6 }); },
    el: list,
  };
  addEventListener('resize', api.reposition);
  return api;
}

const parseOptions = (raw) => (raw ? JSON.parse(raw) : []).map((o) => (typeof o === 'string' ? { label: o, value: o } : { value: o.label, ...o }));

// ------------------------------------------------------------------
// 自动补全：只在输入法选定之后过滤
// ------------------------------------------------------------------
export function autocomplete(root, { options, source } = {}) {
  const input = root.querySelector('input');
  const box = root.querySelector('.ts-input') || root;
  let all = options || parseOptions(root.dataset.options);
  const lb = listbox({
    anchor: box, input,
    onPick: (o) => { input.value = o.label; lb.close(); emit(root, 'select', o); },
  });
  const filter = async (q) => {
    const list = source ? await source(q) : all.filter((o) => !q || o.label.toLowerCase().includes(q.toLowerCase()) || (o.keywords || '').includes(q));
    lb.render(list.slice(0, 50), q);
    if (document.activeElement === input) lb.open();
  };
  onCommittedInput(input, (v) => filter(v));
  input.addEventListener('focus', () => filter(input.value));
  input.addEventListener('keydown', (e) => lb.keydown(e));
  return { setOptions(o) { all = parseOptions(JSON.stringify(o)); }, close: () => lb.close() };
}

// ------------------------------------------------------------------
// 标签输入：回车、逗号、顿号添加（输入法组合中不算）；退格先选中最后一个，再按一次删除
// ------------------------------------------------------------------
export function tagInput(root) {
  const input = root.querySelector('input');
  const max = Number(root.dataset.max || Infinity);
  let tags = (root.dataset.value || '').split(/[,，]/).map((s) => s.trim()).filter(Boolean);
  const hidden = root.querySelector('input[type="hidden"]');
  const render = (fresh) => {
    root.querySelectorAll('.ts-tag').forEach((t) => t.remove());
    tags.forEach((t, i) => {
      const el = document.createElement('span');
      el.className = 'ts-tag';
      if (fresh !== i) el.style.animation = 'none';
      el.innerHTML = `<span>${esc(t)}</span>`;
      const x = document.createElement('button');
      x.type = 'button';
      x.setAttribute('aria-label', `移除 ${t}`);
      x.append(icon('close'));
      x.addEventListener('click', () => { tags.splice(i, 1); render(); input.focus(); changed(); });
      el.append(x);
      input.before(el);
    });
    if (hidden) hidden.value = tags.join(',');
    input.placeholder = tags.length ? '' : input.dataset.placeholder || input.placeholder;
  };
  const changed = () => emit(root, 'change', { tags: [...tags] });
  const add = (raw) => {
    const parts = raw.split(/[,，、\n]/).map((s) => s.trim()).filter(Boolean);
    let added = false;
    for (const p of parts) if (!tags.includes(p) && tags.length < max) { tags.push(p); added = true; }
    input.value = '';
    if (added) { render(tags.length - 1); changed(); }
  };
  input.dataset.placeholder = input.placeholder;
  input.addEventListener('keydown', (e) => {
    if (e.isComposing || e.keyCode === 229) return; // 拼音还没选定
    const armed = root.querySelector('.ts-tag[data-armed]');
    if ((e.key === 'Enter' || e.key === ',') && input.value.trim()) { e.preventDefault(); add(input.value); return; }
    if (e.key === 'Backspace' && !input.value && tags.length) {
      e.preventDefault();
      if (armed) { tags.pop(); render(); changed(); }
      else root.querySelectorAll('.ts-tag').forEach((t, i, a) => i === a.length - 1 && t.setAttribute('data-armed', ''));
      return;
    }
    armed?.removeAttribute('data-armed');
  });
  input.addEventListener('input', (e) => { if (!e.isComposing && /[,，、]$/.test(input.value)) add(input.value); });
  input.addEventListener('paste', (e) => {
    const text = e.clipboardData.getData('text');
    if (/[,，、\n]/.test(text)) { e.preventDefault(); add(text); }
  });
  input.addEventListener('blur', () => { if (input.value.trim()) add(input.value); });
  root.addEventListener('click', (e) => { if (e.target === root) input.focus(); });
  render();
  return { get tags() { return [...tags]; } };
}

// ------------------------------------------------------------------
// 滑块（单值 / 范围）：点轨道跳过去（弹簧），拖动 1:1 跟手，键盘可调
// ------------------------------------------------------------------
export function slider(root) {
  const min = Number(root.dataset.min ?? 0), max = Number(root.dataset.max ?? 100), step = Number(root.dataset.step ?? 1);
  const unit = root.dataset.unit || '';
  const values = String(root.dataset.value ?? min).split(',').map(Number);
  const range = values.length === 2;
  const labels = (root.dataset.label || '').split(',');
  root.innerHTML = '<div class="ts-slider-track"><div class="ts-slider-fill"></div></div>';
  const track = root.firstElementChild, fill = track.firstElementChild;
  if (root.dataset.ticks !== undefined) {
    const n = Math.round((max - min) / step);
    if (n <= 20) { const t = document.createElement('div'); t.className = 'ts-slider-ticks'; t.innerHTML = '<i></i>'.repeat(n + 1); track.append(t); }
  }
  const pct = (v) => ((v - min) / (max - min)) * 100;
  const snap = (v) => clamp(Math.round((v - min) / step) * step + min, min, max);
  const fmt = (v) => `${+v.toFixed(6)}${unit}`;

  const thumbs = values.map((v, i) => {
    const t = document.createElement('div');
    t.className = 'ts-slider-thumb';
    t.tabIndex = 0;
    t.setAttribute('role', 'slider');
    t.setAttribute('aria-valuemin', min);
    t.setAttribute('aria-valuemax', max);
    t.setAttribute('aria-label', labels[i] || root.getAttribute('aria-label') || (range ? (i ? '最大值' : '最小值') : '数值'));
    t.innerHTML = '<span class="ts-slider-value"></span>';
    track.append(t);
    const s = { el: t, value: snap(v), pos: new Spring(pct(snap(v)), 'snappy', () => draw()) };
    return s;
  });
  const draw = () => {
    thumbs.forEach((t) => { t.el.style.left = `${t.pos.value}%`; });
    const a = range ? thumbs[0].pos.value : 0, b = range ? thumbs[1].pos.value : thumbs[0].pos.value;
    fill.style.left = `${Math.min(a, b)}%`;
    fill.style.width = `${Math.abs(b - a)}%`;
  };
  const setValue = (t, v, { animate = true, fire = 'input' } = {}) => {
    const i = thumbs.indexOf(t);
    if (range) v = i === 0 ? Math.min(v, thumbs[1].value) : Math.max(v, thumbs[0].value); // 两个滑块不能交叉
    v = snap(v);
    const changed = v !== t.value;
    t.value = v;
    t.el.setAttribute('aria-valuenow', v);
    t.el.setAttribute('aria-valuetext', fmt(v));
    t.el.firstElementChild.textContent = fmt(v);
    if (animate) t.pos.to(pct(v)); else t.pos.set(pct(v));
    if (changed && fire) emit(root, fire, { value: range ? thumbs.map((x) => x.value) : v });
  };
  thumbs.forEach((t) => setValue(t, t.value, { animate: false, fire: null }));

  const valueAt = (x) => { const r = track.getBoundingClientRect(); return min + clamp((x - r.left) / r.width, 0, 1) * (max - min); };
  let drag = null;
  root.addEventListener('pointerdown', (e) => {
    if (e.button !== 0) return;
    const v = valueAt(e.clientX);
    const t = e.target.closest('.ts-slider-thumb') ? thumbs.find((x) => x.el === e.target.closest('.ts-slider-thumb'))
      : thumbs.reduce((a, b) => (Math.abs(b.value - v) < Math.abs(a.value - v) ? b : a));
    drag = { t, id: e.pointerId, moved: false };
    root.setPointerCapture(e.pointerId);
    root.setAttribute('data-dragging', '');
    thumbs.forEach((x) => x.el.toggleAttribute('data-active', x === t));
    t.el.focus({ preventScroll: true });
    if (!e.target.closest('.ts-slider-thumb')) setValue(t, v); // 点轨道：滑块用弹簧跳过去
  });
  root.addEventListener('pointermove', (e) => {
    if (!drag || e.pointerId !== drag.id) return;
    drag.moved = true;
    const v = valueAt(e.clientX);
    drag.t.pos.stop();
    setValue(drag.t, v, { animate: false });
    // 拖动时滑块 1:1 跟手（不吸附到刻度），松手再吸附
    const raw = pct(range ? (thumbs.indexOf(drag.t) === 0 ? Math.min(v, thumbs[1].value) : Math.max(v, thumbs[0].value)) : v);
    drag.t.pos.set(clamp(raw, 0, 100));
  });
  const end = () => {
    if (!drag) return;
    drag.t.pos.to(pct(drag.t.value));
    emit(root, 'change', { value: range ? thumbs.map((x) => x.value) : drag.t.value });
    root.removeAttribute('data-dragging');
    drag = null;
  };
  root.addEventListener('pointerup', end);
  root.addEventListener('pointercancel', end);
  thumbs.forEach((t) => t.el.addEventListener('keydown', (e) => {
    const big = Math.max(step, (max - min) / 10);
    const map = { ArrowRight: step, ArrowUp: step, ArrowLeft: -step, ArrowDown: -step, PageUp: big, PageDown: -big };
    let v = null;
    if (e.key in map) v = t.value + map[e.key];
    if (e.key === 'Home') v = min;
    if (e.key === 'End') v = max;
    if (v === null) return;
    e.preventDefault();
    setValue(t, v, { fire: 'change' });
  }));
  if (root.hasAttribute('aria-disabled')) thumbs.forEach((t) => (t.el.tabIndex = -1));
  return { get value() { return range ? thumbs.map((x) => x.value) : thumbs[0].value; }, set value(v) { [].concat(v).forEach((x, i) => setValue(thumbs[i], x)); } };
}

// ------------------------------------------------------------------
// 评分：悬停预览，点击选中（弹一下），方向键调整
// ------------------------------------------------------------------
export function rating(root) {
  const max = Number(root.dataset.max || 5);
  let value = Number(root.dataset.value || 0);
  const readonly = root.hasAttribute('data-readonly');
  root.setAttribute('role', readonly ? 'img' : 'radiogroup');
  root.setAttribute('aria-label', root.getAttribute('aria-label') || '评分');
  // 只读评分是一张“图”：星星用不可交互的 span，读屏只读整体的“评分 3 / 5”
  const btns = Array.from({ length: max }, (_, i) => {
    const b = document.createElement(readonly ? 'span' : 'button');
    if (readonly) { b.className = 'ts-rating-star'; b.setAttribute('aria-hidden', 'true'); }
    else { b.type = 'button'; b.setAttribute('role', 'radio'); b.setAttribute('aria-label', `${i + 1} 星`); }
    b.append(icon('star-fill'));
    root.append(b);
    return b;
  });
  const show = (n) => btns.forEach((b, i) => b.toggleAttribute('data-on', i < n));
  const set = (n) => {
    value = n;
    if (!readonly) btns.forEach((b, i) => { b.setAttribute('aria-checked', i + 1 === n); b.tabIndex = i + 1 === (n || 1) ? 0 : -1; });
    if (readonly) root.setAttribute('aria-label', `评分 ${n} / ${max}`);
    show(n);
  };
  if (!readonly) {
    btns.forEach((b, i) => {
      b.addEventListener('pointerenter', () => show(i + 1));
      b.addEventListener('click', () => {
        set(value === i + 1 ? 0 : i + 1); // 再点一次当前分数可清零
        b.setAttribute('data-pop', '');
        setTimeout(() => b.removeAttribute('data-pop'), 120);
        emit(root, 'change', { value });
      });
    });
    root.addEventListener('pointerleave', () => show(value));
    root.addEventListener('keydown', (e) => {
      const d = { ArrowRight: 1, ArrowUp: 1, ArrowLeft: -1, ArrowDown: -1 }[e.key];
      if (!d) return;
      e.preventDefault();
      set(clamp(value + d, 1, max));
      btns[value - 1].focus();
      emit(root, 'change', { value });
    });
  }
  set(value);
}

// ------------------------------------------------------------------
// 文件上传：点击或拖入；列表里每个文件有进度条
// 业务方监听 'files' 事件，用 detail.items[i].progress(0–100) / done() / fail(msg) 更新状态
// ------------------------------------------------------------------
export function fileUpload(root) {
  const picker = document.createElement('input');
  picker.type = 'file';
  if (root.dataset.accept) picker.accept = root.dataset.accept;
  picker.multiple = root.hasAttribute('data-multiple');
  root.append(picker);
  root.tabIndex = 0;
  root.setAttribute('role', 'button');
  let list = root.nextElementSibling?.classList.contains('ts-upload-list') ? root.nextElementSibling : null;
  if (!list) { list = document.createElement('ul'); list.className = 'ts-upload-list'; root.after(list); }
  const size = (n) => (n < 1024 ? `${n} B` : n < 1048576 ? `${(n / 1024).toFixed(1)} KB` : `${(n / 1048576).toFixed(1)} MB`);
  const add = (files) => {
    const items = [...files].map((file) => {
      const li = document.createElement('li');
      li.className = 'ts-upload-item ts-settle';
      li.innerHTML = `<span class="ts-avatar ts-sm"></span><span class="ts-grow" style="min-width:0"><span class="ts-label" style="display:block;overflow:hidden;text-overflow:ellipsis;white-space:nowrap">${esc(file.name)}</span><span class="ts-caption ts-secondary">${size(file.size)}</span></span>`;
      li.firstElementChild.append(icon(file.type.startsWith('image/') ? 'image' : 'file'));
      const bar = document.createElement('div');
      bar.className = 'ts-progress';
      bar.innerHTML = '<div class="ts-progress-bar"></div>';
      bar.setAttribute('role', 'progressbar');
      bar.setAttribute('aria-label', `${file.name} 上传进度`);
      li.children[1].append(bar);
      const rm = document.createElement('button');
      rm.className = 'ts-icon-button ts-sm ts-pressable ts-state';
      rm.setAttribute('aria-label', `移除 ${file.name}`);
      rm.append(icon('close'));
      rm.addEventListener('click', () => li.remove());
      li.append(rm);
      list.append(li);
      const status = li.querySelector('.ts-caption');
      return {
        file, el: li,
        progress(p) { bar.style.setProperty('--value', p); bar.setAttribute('aria-valuenow', p); status.textContent = `${size(file.size)} · ${Math.round(p)}%`; },
        done() { bar.remove(); status.textContent = `${size(file.size)} · 已上传`; li.firstElementChild.className = 'ts-avatar ts-sm ts-pigment-olive'; li.firstElementChild.replaceChildren(icon('check')); },
        fail(msg = '上传失败') { bar.remove(); status.textContent = msg; status.style.color = 'var(--ts-danger-text)'; },
      };
    });
    emit(root, 'files', { files: [...files], items });
  };
  root.addEventListener('click', (e) => { if (e.target !== picker) picker.click(); });
  root.addEventListener('keydown', (e) => { if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); picker.click(); } });
  picker.addEventListener('change', () => { if (picker.files.length) add(picker.files); picker.value = ''; });
  ['dragenter', 'dragover'].forEach((t) => root.addEventListener(t, (e) => { e.preventDefault(); root.setAttribute('data-over', ''); }));
  ['dragleave', 'drop'].forEach((t) => root.addEventListener(t, () => root.removeAttribute('data-over')));
  root.addEventListener('drop', (e) => { e.preventDefault(); if (e.dataTransfer.files.length) add(e.dataTransfer.files); });
  return { add };
}

// ------------------------------------------------------------------
// 表单校验：用浏览器自带的约束校验，提示语换成中文，错误写进表单项的帮助文字
// ------------------------------------------------------------------
const messages = (el) => {
  const v = el.validity;
  if (v.valueMissing) return el.type === 'checkbox' ? '请勾选此项' : '此项为必填';
  if (v.typeMismatch) return { email: '邮箱格式不正确', url: '网址格式不正确' }[el.type] || '格式不正确';
  if (v.tooShort) return `至少需要 ${el.minLength} 个字符`;
  if (v.tooLong) return `最多 ${el.maxLength} 个字符`;
  if (v.rangeUnderflow) return `不能小于 ${el.min}`;
  if (v.rangeOverflow) return `不能大于 ${el.max}`;
  if (v.patternMismatch) return el.dataset.patternMessage || '格式不正确';
  if (v.customError) return el.validationMessage;
  return '';
};
export function validateField(el) {
  const field = el.closest('.ts-field');
  const help = field?.querySelector('.ts-field-help');
  const msg = el.checkValidity() ? '' : messages(el);
  if (field) {
    if (help && !help.dataset.original) help.dataset.original = help.textContent;
    field.toggleAttribute('data-invalid', !!msg);
    if (help) help.textContent = msg || help.dataset.original || '';
  }
  el.setAttribute('aria-invalid', !!msg);
  return !msg;
}
export function form(root) {
  root.noValidate = true;
  const fields = () => [...root.querySelectorAll('input, select, textarea')].filter((el) => el.willValidate);
  root.addEventListener('submit', (e) => {
    const bad = fields().filter((el) => !validateField(el));
    if (bad.length) {
      e.preventDefault();
      e.stopImmediatePropagation();
      bad[0].focus();
      emit(root, 'invalid-submit', { fields: bad });
    }
  });
  // 提交过一次之后，边输入边校验（输入法组合中不校验）
  root.addEventListener('input', (e) => {
    if (e.isComposing || !e.target.closest('.ts-field[data-invalid]')) return;
    validateField(e.target);
  });
  root.addEventListener('compositionend', (e) => { if (e.target.closest('.ts-field[data-invalid]')) validateField(e.target); });
  root.addEventListener('focusout', (e) => { if (e.target.willValidate && e.target.value) validateField(e.target); });
}

export function initForms(root = document) {
  const $ = (sel, fn) => root.querySelectorAll(sel).forEach((el) => once(el, sel, fn));
  $('.ts-textarea[data-autosize]', autosize);
  $('.ts-password', passwordField);
  $('.ts-number', numberInput);
  $('.ts-otp', otpInput);
  $('.ts-autocomplete', (el) => autocomplete(el));
  $('.ts-taginput', tagInput);
  $('.ts-slider', slider);
  $('.ts-rating', rating);
  $('.ts-upload', fileUpload);
  $('form.ts-form', form);
}
