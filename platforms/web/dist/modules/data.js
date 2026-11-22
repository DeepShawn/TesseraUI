// TesseraUI v0.6.0 — 由 scripts/build.mjs 从 tokens/*.json 生成，请勿手改。
// 数据展示：Table、Tree、Accordion、Tooltip、Popover、Carousel、ImageViewer、CodeBlock
import { Spring, VelocityTracker, motion as M, icon, once, uid, clamp, place, follow, onOutside, animateIn, animateOut, emit, esc, prefersReducedMotion, onCommittedInput } from '../core.js';
import { pagination } from './navigation.js';

// ------------------------------------------------------------------
// 表格
// - 点表头排序：升序 → 降序 → 原顺序；数字按数值、中文按拼音顺序
// - 筛选框：输入法选定后才过滤
// - data-selectable：选择列、全选（含半选态）、Shift 连选，选中后显示批量操作栏
// - data-page-size：分页（与排序、筛选联动）
// - data-sticky-first：固定首列，横向滚动后才出现分隔阴影；纵向滚动后表头出现阴影
// - data-responsive：容器变窄时每行变成一张卡片
// - .ts-truncate 单元格：文字被截断时自动加工具提示
// ------------------------------------------------------------------
const collator = new Intl.Collator('zh-Hans-CN', { numeric: true, sensitivity: 'base' });
const numOf = (s) => parseFloat(String(s).replace(/[,，¥$%\s]/g, ''));
export function table(root, { filterInput, pageSize = Number(root.dataset.pageSize || 0), bulk } = {}) {
  const wrap = root.closest('.ts-table-wrap') || root.parentElement;
  const tbody = root.tBodies[0];
  const headRow = root.tHead.rows[0];
  const selectable = root.hasAttribute('data-selectable');
  const responsive = root.hasAttribute('data-responsive');
  bulk ??= root.dataset.bulk ? document.getElementById(root.dataset.bulk) : null;

  // 选择列
  let allBox = null;
  if (selectable) {
    const th = document.createElement('th');
    th.className = 'ts-select-cell';
    th.innerHTML = '<input type="checkbox" class="ts-checkbox" aria-label="全选">';
    headRow.prepend(th);
    allBox = th.firstElementChild;
    [...tbody.rows].forEach((r, i) => {
      const td = document.createElement('td');
      td.className = 'ts-select-cell';
      const label = r.cells[0]?.textContent.trim() || `第 ${i + 1} 行`;
      td.innerHTML = `<input type="checkbox" class="ts-checkbox" aria-label="选择 ${esc(label)}">`;
      r.prepend(td);
      r.setAttribute('aria-selected', 'false');
    });
  }
  const heads = [...headRow.cells];
  const original = [...tbody.rows];
  // 窄屏卡片模式：给每个单元格补上列名，并包一层便于排版
  const firstData = selectable ? 1 : 0;
  if (responsive) original.forEach((r) => [...r.cells].forEach((td, i) => {
    const th = heads[i];
    const isTitle = i === firstData, isActions = th?.classList.contains('ts-actions-col'), isSelect = td.classList.contains('ts-select-cell');
    td.dataset.label = isTitle || isActions || isSelect ? '' : th?.textContent.trim() || '';
    if (isTitle) td.classList.add('ts-card-title');
    if (isActions) td.classList.add('ts-actions-cell');
    const inner = document.createElement('span');
    inner.className = 'ts-cell-inner';
    inner.append(...td.childNodes);
    td.append(inner);
  }));

  // 排序
  let sortCol = -1, dir = 'none';
  const cellValue = (row, i) => { const c = row.cells[i]; return c?.dataset.value ?? c?.textContent.trim() ?? ''; };
  const sortIcon = (th) => th.querySelector('.ts-sort .ts-icon').replaceWith(icon(th.getAttribute('aria-sort') === 'ascending' ? 'arrow-up' : th.getAttribute('aria-sort') === 'descending' ? 'arrow-down' : 'chevrons-up-down'));
  heads.forEach((th, i) => {
    if (!th.hasAttribute('data-sortable')) return;
    const btn = document.createElement('button');
    btn.type = 'button';
    btn.className = 'ts-sort';
    btn.append(...th.childNodes, icon('chevrons-up-down'));
    th.append(btn);
    th.setAttribute('aria-sort', 'none');
    btn.addEventListener('click', () => {
      dir = sortCol !== i ? 'ascending' : dir === 'ascending' ? 'descending' : dir === 'descending' ? 'none' : 'ascending';
      sortCol = dir === 'none' ? -1 : i;
      heads.forEach((h) => { if (h.hasAttribute('aria-sort')) { h.setAttribute('aria-sort', h === th && dir !== 'none' ? dir : 'none'); sortIcon(h); } });
      page = 1;
      apply();
      emit(root, 'sort', { column: i, direction: dir });
    });
  });

  // 筛选、分页
  let query = '', page = 1, loading = false;
  const colspan = heads.length;
  const empty = document.createElement('tr');
  empty.className = 'ts-table-empty';
  empty.innerHTML = `<td colspan="${colspan}">没有符合条件的数据</td>`;
  let footer = null, pager = null, info = null;
  if (pageSize) {
    footer = document.createElement('div');
    footer.className = 'ts-table-footer';
    info = document.createElement('span');
    const pg = document.createElement('div');
    footer.append(info, pg);
    wrap.after(footer);
    pager = pagination(pg, { total: 1, page: 1, onChange: (p) => { page = p; apply(false); wrap.scrollTop = 0; } });
  }
  let visible = original;
  function apply(resetPage = true) {
    if (loading) return;
    let rows = original.filter((r) => !query || r.textContent.toLowerCase().includes(query.toLowerCase()));
    if (sortCol >= 0) {
      const numeric = rows.every((r) => !isNaN(numOf(cellValue(r, sortCol))));
      rows = [...rows].sort((a, b) => {
        const x = cellValue(a, sortCol), y = cellValue(b, sortCol);
        const c = numeric ? numOf(x) - numOf(y) : collator.compare(x, y);
        return dir === 'ascending' ? c : -c;
      });
    }
    visible = rows;
    let shown = rows;
    if (pageSize) {
      const pages = Math.max(1, Math.ceil(rows.length / pageSize));
      if (resetPage === true) page = Math.min(page, pages);
      pager.setTotal(pages);
      if (pager.page !== page) pager.go(page);
      shown = rows.slice((page - 1) * pageSize, page * pageSize);
      const from = rows.length ? (page - 1) * pageSize + 1 : 0;
      info.textContent = `共 ${rows.length} 条${rows.length ? `，第 ${from}–${from + shown.length - 1} 条` : ''}`;
    }
    tbody.replaceChildren(...(shown.length ? shown : [empty]));
    syncSelection();
    truncateTips();
  }

  // 选择
  let anchor = null;
  const selected = () => original.filter((r) => r.getAttribute('aria-selected') === 'true');
  function syncSelection() {
    if (!selectable) return;
    const sel = selected();
    const inView = visible.filter((r) => r.getAttribute('aria-selected') === 'true').length;
    allBox.checked = visible.length > 0 && inView === visible.length;
    allBox.indeterminate = inView > 0 && inView < visible.length;
    if (bulk) {
      bulk.hidden = sel.length === 0;
      const c = bulk.querySelector('[data-bulk-count]');
      if (c) c.textContent = sel.length;
    }
  }
  const setRow = (r, on) => { r.setAttribute('aria-selected', on); r.querySelector('.ts-select-cell input').checked = on; };
  const changed = () => { syncSelection(); emit(root, 'selectionchange', { rows: selected(), count: selected().length }); };
  if (selectable) {
    allBox.addEventListener('change', () => { visible.forEach((r) => setRow(r, allBox.checked)); changed(); });
    tbody.addEventListener('click', (e) => {
      const box = e.target.closest('.ts-select-cell input');
      if (!box) return;
      const row = box.closest('tr');
      // Shift + 点击：从上一次点的行连选到这一行
      if (e.shiftKey && anchor && anchor !== row) {
        const list = [...tbody.rows];
        const [a, b] = [list.indexOf(anchor), list.indexOf(row)].sort((x, y) => x - y);
        list.slice(a, b + 1).forEach((r) => setRow(r, box.checked));
      } else setRow(row, box.checked);
      anchor = row;
      changed();
    });
  }

  // 滚动阴影
  const onScroll = () => {
    wrap.toggleAttribute('data-scrolled-x', wrap.scrollLeft > 1);
    wrap.toggleAttribute('data-scrolled-y', wrap.scrollTop > 1);
  };
  wrap.addEventListener('scroll', onScroll, { passive: true });

  // 截断提示
  function truncateTips() {
    root.querySelectorAll('.ts-truncate').forEach((td) => {
      if (td.scrollWidth > td.clientWidth + 1) td.dataset.tooltip = td.textContent.trim();
      else delete td.dataset.tooltip;
    });
  }
  new ResizeObserver(() => truncateTips()).observe(wrap);

  if (filterInput) onCommittedInput(filterInput, (v) => { query = v.trim(); page = 1; apply(); });
  apply();
  onScroll();

  return {
    refresh: () => apply(false),
    get selected() { return selected(); },
    clearSelection() { original.forEach((r) => setRow(r, false)); changed(); },
    /** 删除行（例如批量删除后） */
    remove(rows) { rows.forEach((r) => { const i = original.indexOf(r); if (i >= 0) original.splice(i, 1); }); changed(); apply(false); },
    /** 加载中：用骨架行占位 */
    setLoading(on, count = pageSize || 5) {
      loading = on;
      if (on) {
        const sk = Array.from({ length: count }, () => {
          const tr = document.createElement('tr');
          tr.className = 'ts-table-loading';
          tr.setAttribute('aria-hidden', 'true');
          tr.innerHTML = heads.map((_, i) => `<td><span class="ts-skeleton" style="width:${40 + ((i * 23) % 50)}%"></span></td>`).join('');
          return tr;
        });
        tbody.replaceChildren(...sk);
        root.setAttribute('aria-busy', 'true');
      } else { root.removeAttribute('aria-busy'); apply(false); }
    },
  };
}

// ------------------------------------------------------------------
// 树：方向键浏览（右展开、左收起），回车 / 空格选中
// ------------------------------------------------------------------
export function tree(root) {
  root.setAttribute('role', 'tree');
  const items = () => [...root.querySelectorAll('li')].filter((li) => !li.parentElement.closest('li[aria-expanded="false"]'));
  root.querySelectorAll('li').forEach((li) => {
    li.setAttribute('role', 'treeitem');
    const row = li.querySelector(':scope > .ts-tree-row');
    const sub = li.querySelector(':scope > ul');
    row.tabIndex = -1;
    const t = document.createElement('span');
    t.className = 'ts-tree-toggle';
    t.append(icon('chevron-right'));
    row.prepend(t);
    if (sub) { sub.setAttribute('role', 'group'); if (!li.hasAttribute('aria-expanded')) li.setAttribute('aria-expanded', 'false'); }
    row.addEventListener('click', (e) => {
      if (sub && (e.target.closest('.ts-tree-toggle') || !li.hasAttribute('data-selectable'))) toggle(li);
      select(li);
    });
  });
  const first = root.querySelector('li > .ts-tree-row');
  if (first) first.tabIndex = 0;
  const toggle = (li, open = li.getAttribute('aria-expanded') !== 'true') => {
    if (!li.querySelector(':scope > ul')) return;
    li.setAttribute('aria-expanded', open);
    const sub = li.querySelector(':scope > ul');
    if (open && !prefersReducedMotion()) sub.animate([{ opacity: 0, transform: 'translateY(-4px)' }, { opacity: 1, transform: 'none' }], { duration: M.duration.normal, easing: 'cubic-bezier(0.2, 0, 0, 1)' });
  };
  const focus = (li) => { root.querySelectorAll('.ts-tree-row').forEach((r) => (r.tabIndex = -1)); const r = li.querySelector(':scope > .ts-tree-row'); r.tabIndex = 0; r.focus(); };
  const select = (li) => {
    root.querySelectorAll('[aria-selected="true"]').forEach((x) => x.setAttribute('aria-selected', 'false'));
    li.setAttribute('aria-selected', 'true');
    focus(li);
    emit(root, 'select', { item: li, value: li.dataset.value ?? li.querySelector('.ts-tree-row').textContent.trim() });
  };
  root.addEventListener('keydown', (e) => {
    const li = e.target.closest('li');
    if (!li) return;
    const list = items(), i = list.indexOf(li);
    const open = li.getAttribute('aria-expanded');
    if (e.key === 'ArrowDown') { e.preventDefault(); list[i + 1] && focus(list[i + 1]); }
    else if (e.key === 'ArrowUp') { e.preventDefault(); list[i - 1] && focus(list[i - 1]); }
    else if (e.key === 'ArrowRight') { e.preventDefault(); if (open === 'false') toggle(li, true); else if (open === 'true') focus(li.querySelector('li')); }
    else if (e.key === 'ArrowLeft') { e.preventDefault(); if (open === 'true') toggle(li, false); else { const p = li.parentElement.closest('li'); p && focus(p); } }
    else if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); if (open !== null && !li.hasAttribute('data-selectable')) toggle(li); select(li); }
  });
}

// ------------------------------------------------------------------
// 折叠面板：<details> 打开 / 收起时高度平滑过渡；data-single 时同时只开一个
// ------------------------------------------------------------------
export function accordion(root) {
  const single = root.hasAttribute('data-single');
  root.querySelectorAll(':scope > details').forEach((d) => {
    const summary = d.querySelector('summary');
    summary.addEventListener('click', (e) => {
      e.preventDefault();
      if (d.open) collapse(d);
      else {
        if (single) root.querySelectorAll(':scope > details[open]').forEach(collapse);
        expand(d);
      }
    });
  });
  const run = (d, from, to, done) => {
    if (prefersReducedMotion()) { done?.(); return; }
    d.style.overflow = 'hidden';
    const a = d.animate({ height: [`${from}px`, `${to}px`] }, { duration: M.duration.normal + 50, easing: 'cubic-bezier(0.2, 0, 0, 1)' });
    a.onfinish = () => { d.style.overflow = ''; done?.(); };
  };
  const expand = (d) => { const from = d.offsetHeight; d.open = true; run(d, from, d.offsetHeight); };
  const collapse = (d) => {
    const from = d.offsetHeight, to = d.querySelector('summary').offsetHeight;
    if (prefersReducedMotion()) { d.open = false; return; }
    run(d, from, to, () => (d.open = false));
  };
}

// ------------------------------------------------------------------
// 工具提示：替代原生 title；悬停 500ms 或键盘聚焦时出现；Esc 关闭
// 用法：<button data-tooltip="复制">…</button>
// ------------------------------------------------------------------
let tip, tipTimer, tipFor = null;
export function tooltips(root = document) {
  if (!tip) {
    tip = document.createElement('div');
    tip.className = 'ts-tooltip';
    tip.setAttribute('role', 'tooltip');
    tip.id = uid('tooltip');
    tip.hidden = true;
    document.body.append(tip);
    addEventListener('keydown', (e) => { if (e.key === 'Escape') hide(); });
    addEventListener('scroll', () => hide(), { passive: true, capture: true });
  }
  const show = (el, delay) => {
    clearTimeout(tipTimer);
    tipTimer = setTimeout(() => {
      tipFor = el;
      tip.textContent = el.dataset.tooltip;
      tip.hidden = false;
      el.setAttribute('aria-describedby', tip.id);
      place(el, tip, { placement: el.dataset.tooltipPlacement || 'top', offset: 6 });
      if (!prefersReducedMotion()) tip.animate([{ opacity: 0, transform: 'scale(.94)' }, { opacity: 1, transform: 'none' }], { duration: M.duration.fast, easing: 'cubic-bezier(0.2, 0, 0, 1)' });
    }, delay);
  };
  const hide = () => { clearTimeout(tipTimer); if (tipFor) tipFor.removeAttribute('aria-describedby'); tipFor = null; if (tip) tip.hidden = true; };
  once(root, 'tooltips', () => {
    root.addEventListener('pointerover', (e) => { const el = e.target.closest?.('[data-tooltip]'); if (el && e.pointerType !== 'touch') show(el, tipFor ? 0 : 500); });
    root.addEventListener('pointerout', (e) => { const el = e.target.closest?.('[data-tooltip]'); if (el && !el.contains(e.relatedTarget)) hide(); });
    root.addEventListener('focusin', (e) => { const el = e.target.closest?.('[data-tooltip]'); if (el && el.matches(':focus-visible')) show(el, 0); });
    root.addEventListener('focusout', hide);
    root.addEventListener('pointerdown', hide);
  });
  // 页面里的 title 属性换成 data-tooltip，避免浏览器自带的提示框
  (root.body || root).querySelectorAll?.('[title]:not(svg *):not(iframe)').forEach((el) => {
    if (el.closest('[data-keep-title]')) return;
    el.dataset.tooltip = el.title;
    if (!el.hasAttribute('aria-label') && !el.textContent.trim()) el.setAttribute('aria-label', el.title);
    el.removeAttribute('title');
  });
}

// ------------------------------------------------------------------
// 气泡卡片：点击触发，内容可交互；点外部或 Esc 关闭
// ------------------------------------------------------------------
export function popover(trigger, panel, { placement = 'bottom' } = {}) {
  panel.classList.add('ts-popover');
  panel.hidden = true;
  panel.setAttribute('role', 'dialog');
  if (!panel.id) panel.id = uid('popover');
  trigger.setAttribute('aria-expanded', 'false');
  trigger.setAttribute('aria-controls', panel.id);
  let off = null, unfollow = null;
  const open = () => {
    panel.hidden = false;
    place(trigger, panel, { placement, offset: 10 });
    unfollow = follow(trigger, panel, { placement, offset: 10 });
    animateIn(panel, 'scale(0.96)');
    trigger.setAttribute('aria-expanded', 'true');
    off = onOutside([trigger, panel], close);
  };
  async function close() {
    if (panel.hidden) return;
    off?.();
    unfollow?.(); unfollow = null;
    trigger.setAttribute('aria-expanded', 'false');
    await animateOut(panel);
    panel.hidden = true;
    panel.getAnimations().forEach((a) => a.cancel());
  }
  trigger.addEventListener('click', () => (panel.hidden ? open() : close()));
  panel.addEventListener('keydown', (e) => { if (e.key === 'Escape') { close(); trigger.focus(); } });
  return { open, close };
}

// ------------------------------------------------------------------
// 轮播：原生横向滚动 + 吸附（手势惯性由系统提供）；圆点和前后按钮同步
// ------------------------------------------------------------------
export function carousel(root) {
  const track = root.querySelector('.ts-carousel-track');
  const slides = [...track.children];
  root.setAttribute('role', 'region');
  root.setAttribute('aria-roledescription', '轮播');
  slides.forEach((s, i) => { s.setAttribute('role', 'group'); s.setAttribute('aria-label', `第 ${i + 1} 张，共 ${slides.length} 张`); });
  const mk = (cls, name, label, d) => {
    const b = document.createElement('button');
    b.type = 'button';
    b.className = `ts-icon-button ts-filled ts-carousel-nav ${cls} ts-pressable ts-state`;
    b.setAttribute('aria-label', label);
    b.append(icon(name));
    b.addEventListener('click', () => go(index + d));
    root.append(b);
    return b;
  };
  const prev = mk('ts-prev', 'chevron-left', '上一张', -1), next = mk('ts-next', 'chevron-right', '下一张', 1);
  const dots = document.createElement('div');
  dots.className = 'ts-carousel-dots';
  slides.forEach((_, i) => { const b = document.createElement('button'); b.type = 'button'; b.setAttribute('aria-label', `第 ${i + 1} 张`); b.addEventListener('click', () => go(i)); dots.append(b); });
  root.append(dots);
  let index = 0;
  const go = (i) => { i = clamp(i, 0, slides.length - 1); slides[i].scrollIntoView({ behavior: prefersReducedMotion() ? 'instant' : 'smooth', block: 'nearest', inline: 'center' }); };
  const sync = () => {
    const c = track.scrollLeft + track.clientWidth / 2;
    index = slides.reduce((best, s, i) => (Math.abs(s.offsetLeft + s.offsetWidth / 2 - c) < Math.abs(slides[best].offsetLeft + slides[best].offsetWidth / 2 - c) ? i : best), 0);
    [...dots.children].forEach((d, i) => d.setAttribute('aria-current', i === index));
    prev.disabled = index === 0;
    next.disabled = index === slides.length - 1;
  };
  track.addEventListener('scroll', () => requestAnimationFrame(sync), { passive: true });
  track.tabIndex = 0;
  track.addEventListener('keydown', (e) => { if (e.key === 'ArrowLeft') { e.preventDefault(); go(index - 1); } if (e.key === 'ArrowRight') { e.preventDefault(); go(index + 1); } });
  // 自动播放：data-autoplay="5000"，鼠标悬停、聚焦或减弱动态时暂停
  const auto = Number(root.dataset.autoplay || 0);
  if (auto) {
    let paused = false;
    ['pointerenter', 'focusin'].forEach((t) => root.addEventListener(t, () => (paused = true)));
    ['pointerleave', 'focusout'].forEach((t) => root.addEventListener(t, () => (paused = false)));
    setInterval(() => { if (!paused && !prefersReducedMotion() && !document.hidden) go(index === slides.length - 1 ? 0 : index + 1); }, auto);
  }
  sync();
  return { go };
}

// ------------------------------------------------------------------
// 图片预览：点击图片全屏查看；滚轮 / 双指 / 双击缩放，拖动平移，下滑关闭
// 用法：<img src="…" data-viewer="组名">，同组图片可左右切换
// ------------------------------------------------------------------
export function imageViewer(img) {
  img.addEventListener('click', () => openViewer(img));
}
function openViewer(start) {
  const group = start.dataset.viewer ? [...document.querySelectorAll(`img[data-viewer="${CSS.escape(start.dataset.viewer)}"]`)] : [start];
  let i = group.indexOf(start);
  const ov = document.createElement('div');
  ov.className = 'ts-viewer';
  ov.setAttribute('role', 'dialog');
  ov.setAttribute('aria-modal', 'true');
  ov.setAttribute('aria-label', '图片预览');
  ov.innerHTML = `<img alt=""><div class="ts-viewer-bar"><span></span></div><div class="ts-viewer-tools"></div>`;
  const pic = ov.querySelector('img'), counter = ov.querySelector('.ts-viewer-bar span'), tools = ov.querySelector('.ts-viewer-tools');
  const btn = (name, label, fn, parent) => { const b = document.createElement('button'); b.type = 'button'; b.className = 'ts-icon-button ts-pressable'; b.setAttribute('aria-label', label); b.append(icon(name)); b.addEventListener('click', (e) => { e.stopPropagation(); fn(); }); parent.append(b); return b; };
  btn('close', '关闭', () => close(), ov.querySelector('.ts-viewer-bar'));
  if (group.length > 1) btn('chevron-left', '上一张', () => show(i - 1), tools);
  btn('zoom-out', '缩小', () => zoomTo(scale / 1.5), tools);
  btn('zoom-in', '放大', () => zoomTo(scale * 1.5), tools);
  if (group.length > 1) btn('chevron-right', '下一张', () => show(i + 1), tools);
  document.body.append(ov);
  const prevFocus = document.activeElement;
  ov.tabIndex = -1;
  ov.focus();
  let scale = 1, x = 0, y = 0;
  const S = new Spring(1, 'smooth'), X = new Spring(0, 'smooth'), Y = new Spring(0, 'smooth');
  const render = () => { pic.style.transform = `translate(${X.value}px, ${Y.value}px) scale(${S.value})`; ov.style.backgroundColor = `rgb(0 0 0 / ${0.92 * clamp(1 - Math.abs(Y.value) / 400, 0.3, 1) * (S.value < 1 ? S.value : 1)})`; };
  S.onUpdate = X.onUpdate = Y.onUpdate = render;
  const limit = () => { const r = pic.getBoundingClientRect(); const w = (pic.offsetWidth * scale - innerWidth) / 2, h = (pic.offsetHeight * scale - innerHeight) / 2; return [Math.max(0, w), Math.max(0, h)]; };
  const zoomTo = (s, cx = 0, cy = 0) => {
    s = clamp(s, 1, 5);
    // 以指定点为中心缩放
    x = cx - (cx - x) * (s / scale); y = cy - (cy - y) * (s / scale);
    scale = s;
    if (s === 1) { x = 0; y = 0; }
    const [lx, ly] = limit();
    x = clamp(x, -lx, lx); y = clamp(y, -ly, ly);
    S.to(scale); X.to(x); Y.to(y);
  };
  const show = (n) => {
    i = (n + group.length) % group.length;
    pic.src = group[i].currentSrc || group[i].src;
    pic.alt = group[i].alt;
    counter.textContent = group.length > 1 ? `${i + 1} / ${group.length}` : group[i].alt || '';
    scale = 1; x = 0; y = 0; S.set(1); X.set(0); Y.set(0);
  };
  show(i);
  // 进场：从原图位置放大到全屏
  if (!prefersReducedMotion()) {
    const r = start.getBoundingClientRect();
    requestAnimationFrame(() => {
      const t = pic.getBoundingClientRect();
      if (!t.width) return;
      const s0 = r.width / t.width;
      S.set(s0); X.set(r.left + r.width / 2 - (t.left + t.width / 2)); Y.set(r.top + r.height / 2 - (t.top + t.height / 2));
      S.to(1); X.to(0); Y.to(0);
    });
  }
  const pointers = new Map();
  let pinch = null, pan = null, lastTap = 0;
  const vt = new VelocityTracker();
  ov.addEventListener('pointerdown', (e) => {
    if (e.target.closest('button')) return;
    ov.setPointerCapture(e.pointerId);
    pointers.set(e.pointerId, { x: e.clientX, y: e.clientY });
    if (pointers.size === 2) {
      const [a, b] = [...pointers.values()];
      pinch = { d: Math.hypot(a.x - b.x, a.y - b.y), s: scale };
      pan = null;
    } else {
      pan = { x: e.clientX, y: e.clientY, ox: X.value, oy: Y.value, moved: false };
      vt.points = [];
      const now = performance.now();
      if (now - lastTap < 280) { // 双击：放大到 2.5 倍 / 还原
        const cx = e.clientX - innerWidth / 2, cy = e.clientY - innerHeight / 2;
        zoomTo(scale > 1 ? 1 : 2.5, cx, cy);
        pan = null;
      }
      lastTap = now;
    }
  });
  ov.addEventListener('pointermove', (e) => {
    if (!pointers.has(e.pointerId)) return;
    pointers.set(e.pointerId, { x: e.clientX, y: e.clientY });
    if (pinch && pointers.size === 2) {
      const [a, b] = [...pointers.values()];
      scale = clamp(pinch.s * Math.hypot(a.x - b.x, a.y - b.y) / pinch.d, 0.6, 6);
      S.set(scale);
      return;
    }
    if (!pan) return;
    const dx = e.clientX - pan.x, dy = e.clientY - pan.y;
    if (Math.abs(dx) + Math.abs(dy) > 4) pan.moved = true;
    vt.add(e.clientY);
    if (scale === 1) { X.set(pan.ox + dx * 0.4); Y.set(pan.oy + dy); } // 未放大：竖向拖动用来关闭
    else { x = pan.ox + dx; y = pan.oy + dy; X.set(x); Y.set(y); }
  });
  const up = (e) => {
    pointers.delete(e.pointerId);
    if (pinch) { if (pointers.size < 2) { pinch = null; zoomTo(scale); } return; }
    if (!pan) return;
    const wasPan = pan; pan = null;
    if (scale === 1) {
      const v = vt.get();
      if (Math.abs(Y.value) > 120 || Math.abs(v) > M.gesture.flingVelocity) close(v);
      else if (!wasPan.moved && e.target === ov) close();
      else { X.to(0); Y.to(0, { velocity: v }); }
    } else {
      const [lx, ly] = limit();
      x = clamp(X.value, -lx, lx); y = clamp(Y.value, -ly, ly);
      X.to(x); Y.to(y);
    }
  };
  ov.addEventListener('pointerup', up);
  ov.addEventListener('pointercancel', up);
  ov.addEventListener('wheel', (e) => { e.preventDefault(); zoomTo(scale * (e.deltaY < 0 ? 1.15 : 1 / 1.15), e.clientX - innerWidth / 2, e.clientY - innerHeight / 2); }, { passive: false });
  ov.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') close();
    if (e.key === 'ArrowLeft' && group.length > 1) show(i - 1);
    if (e.key === 'ArrowRight' && group.length > 1) show(i + 1);
    if (e.key === '+' || e.key === '=') zoomTo(scale * 1.5);
    if (e.key === '-') zoomTo(scale / 1.5);
  });
  document.documentElement.style.overflow = 'hidden';
  async function close(v = 0) {
    document.documentElement.style.overflow = '';
    const a = ov.animate([{ opacity: 1 }, { opacity: 0 }], { duration: M.duration.fast, fill: 'forwards' });
    if (v) Y.to(Y.value + Math.sign(v) * 300, { velocity: v });
    await a.finished;
    ov.remove();
    prevFocus?.focus?.();
  }
}

// ------------------------------------------------------------------
// 代码块：复制按钮（复制成功后图标变成勾）
// ------------------------------------------------------------------
export function codeBlock(root) {
  const pre = root.querySelector('pre');
  // 长代码可以横向滚动：让键盘也能进入并滚动
  if (!pre.hasAttribute('tabindex')) pre.tabIndex = 0;
  if (!pre.hasAttribute('aria-label')) pre.setAttribute('aria-label', `${root.dataset.lang || ''} 代码`.trim());
  if (root.hasAttribute('data-lines')) {
    const code = pre.querySelector('code') || pre;
    code.innerHTML = code.innerHTML.replace(/\n$/, '').split('\n').map((l) => `<span class="ts-line">${l}</span>`).join('\n');
  }
  let head = root.querySelector('.ts-code-head');
  if (!head) { head = document.createElement('div'); head.className = 'ts-code-head'; head.innerHTML = `<span>${root.dataset.lang || ''}</span>`; root.prepend(head); }
  const btn = document.createElement('button');
  btn.type = 'button';
  btn.className = 'ts-icon-button ts-sm ts-copy ts-pressable ts-state';
  btn.setAttribute('aria-label', '复制代码');
  btn.dataset.tooltip = '复制';
  btn.append(icon('copy'));
  btn.addEventListener('click', async () => {
    const text = pre.innerText.replace(/^\s*\d+ ?/gm, (m) => (root.hasAttribute('data-lines') ? '' : m));
    try { await navigator.clipboard.writeText(root.hasAttribute('data-lines') ? [...pre.querySelectorAll('.ts-line')].map((l) => l.textContent).join('\n') : pre.innerText); }
    catch { const r = document.createRange(); r.selectNodeContents(pre); getSelection().removeAllRanges(); getSelection().addRange(r); return; }
    btn.replaceChildren(icon('check'));
    btn.setAttribute('data-copied', '');
    btn.setAttribute('aria-label', '已复制');
    setTimeout(() => { btn.replaceChildren(icon('copy')); btn.removeAttribute('data-copied'); btn.setAttribute('aria-label', '复制代码'); }, 1600);
  });
  head.append(btn);
}

export function initData(root = document) {
  const $ = (sel, fn) => root.querySelectorAll(sel).forEach((el) => once(el, sel, fn));
  $('table.ts-table', (el) => { el._tsTable = table(el, { filterInput: el.dataset.filter ? document.getElementById(el.dataset.filter) : null }); });
  $('.ts-tree', tree);
  $('.ts-accordion', accordion);
  $('.ts-carousel', carousel);
  $('img[data-viewer]', imageViewer);
  $('.ts-code', codeBlock);
  $('[data-popover]', (el) => { const p = document.getElementById(el.dataset.popover); if (p) popover(el, p, { placement: el.dataset.popoverPlacement }); });
  tooltips(root);
}
