// TesseraUI v0.6.0 — 由 scripts/build.mjs 从 tokens/*.json 生成，请勿手改。
// 导航：Sidebar、Pagination、ContextMenu、CommandPalette、Anchor
import { Spring, clamp, prefersReducedMotion, springEasing, icon, once, place, onOutside, animateIn, animateOut, emit, esc, menu as bindMenu, openDialog, closeDialog, onCommittedInput } from '../core.js';
import { haptic } from './flow.js';

// ------------------------------------------------------------------
// 侧边栏：可折叠成只显示图标；折叠状态记在 localStorage
// ------------------------------------------------------------------
export function sidebar(root) {
  const key = root.dataset.persist ? `ts-sidebar:${root.dataset.persist}` : null;
  const toggle = root.querySelector('[data-sidebar-toggle]');
  const set = (collapsed) => {
    root.toggleAttribute('data-collapsed', collapsed);
    toggle?.setAttribute('aria-expanded', !collapsed);
    toggle?.setAttribute('aria-label', collapsed ? '展开侧边栏' : '收起侧边栏');
    // 折叠后图标旁没有文字，给每一项补上悬停提示
    root.querySelectorAll('.ts-sidebar-item').forEach((a) => {
      const label = a.querySelector('.ts-sidebar-label')?.textContent;
      if (collapsed && label) a.dataset.tooltip = label; else delete a.dataset.tooltip;
    });
    try { if (key) localStorage.setItem(key, collapsed ? '1' : '0'); } catch {}
  };
  let initial = root.hasAttribute('data-collapsed');
  try { if (key && localStorage.getItem(key) !== null) initial = localStorage.getItem(key) === '1'; } catch {}
  set(initial);
  toggle?.addEventListener('click', () => set(!root.hasAttribute('data-collapsed')));
  return { collapse: () => set(true), expand: () => set(false) };
}

// ------------------------------------------------------------------
// 分页：页数多时中间折叠成省略号；左右方向键翻页
// ------------------------------------------------------------------
export function pagination(root, { total = Number(root.dataset.total || 1), page = Number(root.dataset.page || 1), onChange } = {}) {
  root.setAttribute('role', 'navigation');
  root.setAttribute('aria-label', root.getAttribute('aria-label') || '分页');
  root.classList.add('ts-pagination');
  const pages = document.createElement('div');
  pages.className = 'ts-pages';
  const info = document.createElement('span');
  info.className = 'ts-caption ts-secondary';
  root.replaceChildren(pages, info);
  const list = () => {
    if (total <= 7) return Array.from({ length: total }, (_, i) => i + 1);
    if (page <= 4) return [1, 2, 3, 4, 5, '…', total];
    if (page >= total - 3) return [1, '…', total - 4, total - 3, total - 2, total - 1, total];
    return [1, '…', page - 1, page, page + 1, '…', total];
  };
  // 按钮按“键”复用（上一页、下一页、各页码、两个省略号），翻页时不整体重建：
  // 按下的动效不会被打断，焦点不丢；页码位置变了的用 FLIP 滑到新位置，当前页底下的深色砖用弹簧滑过去
  const nodes = new Map();
  const indicator = document.createElement('span');
  indicator.className = 'ts-page-indicator';
  indicator.setAttribute('aria-hidden', 'true');
  const ix = new Spring(0, 'snappy', () => paintIndicator());
  const iw = new Spring(36, 'snappy', () => paintIndicator());
  ix.precision = iw.precision = 0.3;
  const paintIndicator = () => { indicator.style.transform = `translateX(${ix.value}px)`; indicator.style.width = `${iw.value}px`; };
  const node = (key, make) => { let n = nodes.get(key); if (!n) { n = make(); n.dataset.key = key; nodes.set(key, n); } return n; };
  const pageBtn = (p) => node(`p${p}`, () => {
    const b = document.createElement('button');
    b.type = 'button';
    b.className = 'ts-page ts-pressable';
    b.textContent = p;
    b.addEventListener('click', () => go(p));
    return b;
  });
  const arrow = (key, label, iconName, d) => node(key, () => {
    const b = document.createElement('button');
    b.type = 'button';
    b.className = 'ts-page ts-pressable';
    b.append(icon(iconName));
    b.setAttribute('aria-label', label);
    b.addEventListener('click', () => go(page + d));
    return b;
  });
  const gap = (key) => node(key, () => { const s = document.createElement('span'); s.className = 'ts-page'; s.dataset.gap = ''; s.textContent = '…'; s.setAttribute('aria-hidden', 'true'); return s; });
  let first = true;
  const render = () => {
    const animate = !first && !prefersReducedMotion();
    // FLIP 第一步：记下已有按钮的位置
    const before = new Map();
    if (animate) for (const [k, n] of nodes) if (n.isConnected) before.set(k, n.offsetLeft);
    const list_ = list();
    let gaps = 0;
    const order = [arrow('prev', '上一页', 'chevron-left', -1), ...list_.map((p) => (p === '…' ? gap(`gap${gaps++}`) : pageBtn(p))), arrow('next', '下一页', 'chevron-right', 1)];
    const keep = new Set(order);
    for (const n of [...pages.children]) if (n !== indicator && !keep.has(n)) n.remove();
    if (indicator.parentNode !== pages) pages.prepend(indicator);
    order.forEach((n, i) => { if (pages.children[i + 1] !== n) pages.insertBefore(n, pages.children[i + 1] ?? null); });
    for (const n of order) {
      if (n.dataset.gap !== undefined) continue;
      const k = n.dataset.key;
      if (k === 'prev') n.disabled = page <= 1;
      else if (k === 'next') n.disabled = page >= total;
      else {
        const p = Number(k.slice(1)), cur = p === page;
        n.toggleAttribute('aria-current', cur);
        if (cur) n.setAttribute('aria-current', 'page');
        if (cur) n.removeAttribute('aria-label'); else n.setAttribute('aria-label', `第 ${p} 页`);
      }
    }
    info.textContent = `第 ${page} / ${total} 页`;
    // FLIP 第二步：位置变了的滑过去，新出现的淡入
    if (animate) {
      const { easing, duration } = springEasing('snappy');
      for (const n of order) {
        const was = before.get(n.dataset.key);
        if (was === undefined) n.animate([{ opacity: 0, transform: 'scale(0.85)' }, { opacity: 1, transform: 'none' }], { duration, easing });
        else if (Math.abs(was - n.offsetLeft) > 0.5) n.animate([{ transform: `translateX(${was - n.offsetLeft}px)` }, { transform: 'none' }], { duration, easing });
      }
    }
    const cur = pages.querySelector('[aria-current="page"]');
    if (cur) {
      if (animate) { ix.to(cur.offsetLeft); iw.to(cur.offsetWidth); } else { ix.set(cur.offsetLeft); iw.set(cur.offsetWidth); }
      // 当前页在可横向滑动的窄屏上滚进可见范围
      const l = cur.offsetLeft, r = l + cur.offsetWidth;
      if (l < pages.scrollLeft || r > pages.scrollLeft + pages.clientWidth) pages.scrollTo({ left: l - (pages.clientWidth - cur.offsetWidth) / 2, behavior: animate ? 'smooth' : 'instant' });
    }
    first = false;
  };
  const go = (p) => {
    p = Math.max(1, Math.min(total, p));
    if (p === page) return;
    const hadFocus = pages.contains(document.activeElement);
    page = p;
    render();
    // 焦点留在被点的按钮上（上一页 / 下一页 / 页码），不跳动、不滚动页面
    if (hadFocus && !pages.contains(document.activeElement)) pages.querySelector('[aria-current]')?.focus({ preventScroll: true });
    onChange?.(page);
    emit(root, 'change', { page });
  };
  root.addEventListener('keydown', (e) => { if (e.key === 'ArrowLeft') go(page - 1); if (e.key === 'ArrowRight') go(page + 1); });
  render();
  return { go, get page() { return page; }, setTotal(t) { total = t; page = Math.min(page, t); render(); } };
}

// ------------------------------------------------------------------
// 右键菜单：右键或长按（触屏 500ms）在手指 / 指针位置打开
// ------------------------------------------------------------------
export function contextMenu(target, menuEl) {
  menuEl.hidden = true;
  menuEl.setAttribute('role', 'menu');
  // 长按时不让系统选中文字、弹出选择手柄或系统菜单（会盖住我们的菜单）
  target.classList.add('ts-context-target');
  const items = () => [...menuEl.querySelectorAll('.ts-menu-item')];
  items().forEach((i) => { i.setAttribute('role', 'menuitem'); i.tabIndex = -1; });
  let off = null, openedAt = 0;
  const open = (x, y, { touch = false } = {}) => {
    // 触屏长按时，浏览器自己的 contextmenu 事件和我们的长按计时会先后触发：只打开一次
    if (!menuEl.hidden && performance.now() - openedAt < 800) return;
    openedAt = performance.now();
    getSelection()?.removeAllRanges();
    menuEl.hidden = false;
    place({ left: x, right: x, top: y, bottom: y, width: 0, height: 0 }, menuEl, { placement: 'bottom-start', offset: 2 });
    animateIn(menuEl, 'scale(0.96)');
    if (touch) haptic('light');
    items()[0]?.focus({ preventScroll: true });
    off?.();
    off = onOutside(menuEl, () => close());
  };
  const close = async () => {
    if (menuEl.hidden) return;
    off?.(); off = null;
    await animateOut(menuEl);
    menuEl.hidden = true;
    menuEl.getAnimations().forEach((a) => a.cancel());
  };
  let lastTouch = 0;
  target.addEventListener('contextmenu', (e) => { e.preventDefault(); open(e.clientX, e.clientY, { touch: performance.now() - lastTouch < 1000 }); });
  let timer, sx = 0, sy = 0;
  target.addEventListener('pointerdown', (e) => {
    if (e.pointerType !== 'touch') return;
    lastTouch = performance.now();
    sx = e.clientX; sy = e.clientY;
    clearTimeout(timer);
    timer = setTimeout(() => open(sx, sy, { touch: true }), 500);
  });
  // 手指轻微抖动不算移动；超过 8px 才认为是在滚动页面
  target.addEventListener('pointermove', (e) => { if (Math.hypot(e.clientX - sx, e.clientY - sy) > 8) clearTimeout(timer); });
  ['pointerup', 'pointercancel'].forEach((t) => target.addEventListener(t, () => clearTimeout(timer)));
  // 键盘：Shift+F10 或菜单键
  target.addEventListener('keydown', (e) => {
    if ((e.shiftKey && e.key === 'F10') || e.key === 'ContextMenu') { e.preventDefault(); const r = target.getBoundingClientRect(); open(r.left + 16, r.top + 16); }
  });
  menuEl.addEventListener('keydown', (e) => {
    const list = items(), i = list.indexOf(document.activeElement);
    if (e.key === 'ArrowDown') { e.preventDefault(); list[(i + 1) % list.length].focus(); }
    if (e.key === 'ArrowUp') { e.preventDefault(); list[(i - 1 + list.length) % list.length].focus(); }
    if (e.key === 'Escape' || e.key === 'Tab') { close(); target.focus?.(); }
  });
  menuEl.addEventListener('click', (e) => { if (e.target.closest('.ts-menu-item')) close(); });
  return { open, close };
}

// ------------------------------------------------------------------
// 命令面板：⌘K / Ctrl+K 打开；输入法选定后才过滤；上下键选择、回车执行
// commands: [{ label, group, icon, keywords, shortcut, run }]
// ------------------------------------------------------------------
export function commandPalette({ commands = [], placeholder = '搜索命令、页面或设置…', hotkey = true } = {}) {
  const dlg = document.createElement('dialog');
  dlg.className = 'ts-command';
  dlg.setAttribute('aria-label', '命令面板');
  dlg.innerHTML = `
    <div class="ts-command-search"><input role="combobox" aria-expanded="true" aria-controls="ts-command-list" aria-autocomplete="list" placeholder="${esc(placeholder)}"></div>
    <div class="ts-command-list" id="ts-command-list" role="listbox"></div>
    <div class="ts-command-foot"><span><span class="ts-kbd">↑</span><span class="ts-kbd">↓</span>选择</span><span><span class="ts-kbd">↵</span>执行</span><span><span class="ts-kbd">Esc</span>关闭</span></div>`;
  dlg.querySelector('.ts-command-search').prepend(icon('search'));
  document.body.append(dlg);
  const input = dlg.querySelector('input'), listEl = dlg.querySelector('.ts-command-list');
  let shown = [], active = 0;
  const score = (c, q) => {
    if (!q) return 1;
    const hay = `${c.label} ${c.keywords || ''}`.toLowerCase();
    q = q.toLowerCase();
    if (hay.startsWith(q)) return 3;
    if (hay.includes(q)) return 2;
    // 子序列匹配：输入的每个字依次出现即可
    let i = 0;
    for (const ch of hay) if (ch === q[i]) i++;
    return i === q.length ? 1 : 0;
  };
  const mark = (label, q) => {
    const at = q ? label.toLowerCase().indexOf(q.toLowerCase()) : -1;
    return at < 0 ? esc(label) : `${esc(label.slice(0, at))}<mark>${esc(label.slice(at, at + q.length))}</mark>${esc(label.slice(at + q.length))}`;
  };
  const render = (q = '') => {
    shown = commands.map((c) => [c, score(c, q)]).filter(([, s]) => s).sort((a, b) => b[1] - a[1]).map(([c]) => c);
    listEl.replaceChildren();
    if (!shown.length) { listEl.innerHTML = `<div class="ts-command-empty">没有找到“${esc(q)}”相关的命令</div>`; return; }
    const groups = new Map();
    shown.forEach((c) => { const g = c.group || '命令'; if (!groups.has(g)) groups.set(g, []); groups.get(g).push(c); });
    shown = [...groups.values()].flat(); // 按分组顺序重新排列，键盘上下与视觉顺序一致
    let idx = 0;
    groups.forEach((items, g) => {
      const h = document.createElement('div');
      h.className = 'ts-overline ts-command-group';
      h.textContent = g;
      listEl.append(h);
      items.forEach((c) => {
        const i = idx++;
        const el = document.createElement('div');
        el.className = 'ts-command-item';
        el.id = `ts-cmd-${i}`;
        el.setAttribute('role', 'option');
        el.innerHTML = `<span style="flex:1">${mark(c.label, q)}</span>${c.shortcut ? `<span class="ts-kbd">${esc(c.shortcut)}</span>` : ''}`;
        el.prepend(icon(c.icon || 'arrow-right'));
        el.addEventListener('pointermove', () => highlight(i));
        el.addEventListener('click', () => run(i));
        listEl.append(el);
      });
    });
    highlight(0);
  };
  const highlight = (i) => {
    active = i;
    listEl.querySelectorAll('.ts-command-item').forEach((el, k) => el.setAttribute('aria-selected', k === i));
    const el = listEl.querySelector(`#ts-cmd-${i}`);
    if (el) { input.setAttribute('aria-activedescendant', el.id); el.scrollIntoView({ block: 'nearest' }); }
  };
  const run = async (i) => { const c = shown[i]; if (!c) return; await close(); c.run?.(); };
  const open = () => { if (dlg.open) return; input.value = ''; render(); openDialog(dlg); input.focus(); };
  const close = () => closeDialog(dlg);
  onCommittedInput(input, (v) => render(v.trim()));
  input.addEventListener('keydown', (e) => {
    if (e.isComposing || e.keyCode === 229) return;
    if (e.key === 'ArrowDown') { e.preventDefault(); highlight(Math.min(shown.length - 1, active + 1)); }
    if (e.key === 'ArrowUp') { e.preventDefault(); highlight(Math.max(0, active - 1)); }
    if (e.key === 'Enter') { e.preventDefault(); run(active); }
  });
  if (hotkey) addEventListener('keydown', (e) => {
    if ((e.metaKey || e.ctrlKey) && e.key.toLowerCase() === 'k') { e.preventDefault(); dlg.open ? close() : open(); }
  });
  return { open, close, setCommands(c) { commands = c; } };
}

// ------------------------------------------------------------------
// 目录锚点：滚动时高亮当前区块，指示条用弹簧跟过去
// ------------------------------------------------------------------
// 页面滚动用弹簧（浏览器自带的 smooth 是固定曲线，起停生硬）；用户一碰滚轮、触屏或按键就停下，交还给原生滚动
let pageSpring = null;
function springScroll(top) {
  const max = document.documentElement.scrollHeight - innerHeight;
  top = clamp(top, 0, Math.max(0, max));
  if (prefersReducedMotion()) { scrollTo({ top, behavior: 'instant' }); return; }
  if (!pageSpring) {
    pageSpring = new Spring(scrollY, 'smooth', (v) => scrollTo({ top: v, behavior: 'instant' }));
    pageSpring.precision = 0.5;
    const stop = () => pageSpring.stop();
    ['wheel', 'touchstart', 'pointerdown', 'keydown'].forEach((t) => addEventListener(t, stop, { passive: true }));
  }
  pageSpring.set(scrollY);
  pageSpring.to(top);
}

export function anchor(root, { offset = 80 } = {}) {
  const links = [...root.querySelectorAll('a[href^="#"]')];
  const targets = links.map((a) => document.getElementById(decodeURIComponent(a.hash.slice(1)))).filter(Boolean);
  const bar = document.createElement('span');
  bar.className = 'ts-anchor-indicator';
  bar.setAttribute('aria-hidden', 'true');
  root.append(bar);
  const y = new Spring(0, 'smooth', (v) => (bar.style.transform = `translateY(${v}px)`));
  const h = new Spring(30, 'smooth', (v) => (bar.style.height = `${v}px`));
  let current = -1;
  const set = (i, animate = true) => {
    if (i === current) return;
    current = i;
    links.forEach((a, k) => a.setAttribute('aria-current', k === i));
    const a = links[i];
    if (!a) return;
    if (animate) { y.to(a.offsetTop); h.to(a.offsetHeight); } else { y.set(a.offsetTop); h.set(a.offsetHeight); }
  };
  const update = () => {
    let i = 0;
    targets.forEach((t, k) => { if (t.getBoundingClientRect().top - offset <= 1) i = k; });
    if (innerHeight + scrollY >= document.documentElement.scrollHeight - 2) i = targets.length - 1; // 滚到底时高亮最后一项
    set(i);
  };
  addEventListener('scroll', () => requestAnimationFrame(update), { passive: true });
  links.forEach((a, i) => a.addEventListener('click', (e) => {
    const t = targets[i];
    if (!t) return;
    e.preventDefault();
    springScroll(t.getBoundingClientRect().top + scrollY - offset + 2);
    history.replaceState(null, '', a.hash);
    set(i);
  }));
  requestAnimationFrame(() => { update(); set(current, false); });
}

export function initNavigation(root = document) {
  const $ = (sel, fn) => root.querySelectorAll(sel).forEach((el) => once(el, sel, fn));
  $('.ts-sidebar', sidebar);
  $('.ts-pagination[data-total]', (el) => pagination(el));
  $('.ts-anchor', (el) => anchor(el, { offset: Number(el.dataset.offset || 80) }));
  $('[data-context-menu]', (el) => { const m = document.getElementById(el.dataset.contextMenu); if (m) contextMenu(el, m); });
  $('[data-menu]', (el) => { const m = document.getElementById(el.dataset.menu); if (m) bindMenu(el, m); });
}
