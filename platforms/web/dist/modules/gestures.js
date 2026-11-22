// TesseraUI v0.6.0 — 由 scripts/build.mjs 从 tokens/*.json 生成，请勿手改。
// 移动端手势：SwipeAction、PullToRefresh、InfiniteScroll、IndexBar
import { Spring, VelocityTracker, motion as M, icon, once, clamp, emit, rubberBand } from '../core.js';

// ------------------------------------------------------------------
// 左滑操作：左滑露出操作按钮；甩过去或拉过 60% 直接执行第一个（危险）操作；同时只开一行
// 结构：<div class="ts-swipe"><div class="ts-swipe-content">…</div><div class="ts-swipe-actions"><button>…</button></div></div>
// ------------------------------------------------------------------
let openSwipe = null;
let outsideBound = false;
export function swipeAction(root) {
  if (!outsideBound) { // 点别处时收起已展开的那一行
    outsideBound = true;
    addEventListener('pointerdown', (e) => { if (openSwipe && !e.target.closest('.ts-swipe')) openSwipe.close(); }, true);
  }
  const content = root.querySelector('.ts-swipe-content');
  const actions = root.querySelector('.ts-swipe-actions');
  const W = () => actions.offsetWidth;
  const x = new Spring(0, 'smooth', (v) => {
    content.style.transform = `translateX(${v}px)`;
    // 按钮从右往左依次露出
    [...actions.children].forEach((b, i, arr) => { b.style.transform = `translateX(${Math.max(0, (W() + v) * ((arr.length - i) / arr.length))}px)`; });
  });
  x.precision = 0.5;
  x.set(0);
  const close = (velocity = 0) => { x.to(0, { velocity }); if (openSwipe === api) openSwipe = null; };
  const open = (velocity = 0) => { if (openSwipe && openSwipe !== api) openSwipe.close(); x.to(-W(), { velocity }); openSwipe = api; };
  const api = { open, close };
  const vt = new VelocityTracker();
  let s = null;
  content.addEventListener('pointerdown', (e) => { s = { x: e.clientX, y: e.clientY, ox: x.value, moved: false, id: e.pointerId }; vt.points = []; x.stop(); });
  content.addEventListener('pointermove', (e) => {
    if (!s || e.pointerId !== s.id) return;
    const dx = e.clientX - s.x, dy = e.clientY - s.y;
    if (!s.moved) { if (Math.abs(dx) < 8 || Math.abs(dx) < Math.abs(dy) * 1.2) return; s.moved = true; content.setPointerCapture(e.pointerId); }
    vt.add(e.clientX);
    let v = s.ox + dx;
    if (v > 0) v = rubberBand(v, 40);
    if (v < -W()) v = -W() + rubberBand(v + W(), root.offsetWidth * 0.5);
    x.set(v);
  });
  let justDragged = false;
  content.addEventListener('pointerup', () => {
    if (!s) return;
    const moved = s.moved; s = null;
    if (!moved) return;
    justDragged = true; // 拖完松手后浏览器还会补一个 click，不能让它把刚拉开的行又关上
    setTimeout(() => (justDragged = false), 0);
    const v = vt.get();
    const full = actions.querySelector('[data-full]');
    if (full && (x.value < -root.offsetWidth * 0.6)) { x.to(-root.offsetWidth, { velocity: v }).then(() => full.click()); return; }
    if (v < -M.gesture.flingVelocity || (v < M.gesture.flingVelocity && x.value < -W() / 2)) open(v);
    else close(v);
  });
  content.addEventListener('click', (e) => {
    if (justDragged) { e.preventDefault(); e.stopPropagation(); return; }
    if (openSwipe === api) { e.preventDefault(); e.stopPropagation(); close(); } // 已展开时点内容区先收起
  }, true);
  actions.addEventListener('click', () => setTimeout(() => close(), 50));
  // 键盘和读屏：操作按钮仍可通过 Tab 访问，获得焦点时自动展开
  actions.addEventListener('focusin', () => open());
  root.addEventListener('focusout', (e) => { if (!root.contains(e.relatedTarget)) close(); });
  return api;
}

// ------------------------------------------------------------------
// 下拉刷新：在滚动容器顶部往下拉；拉过阈值松手触发 refresh 事件，
// 业务方调用 event.detail.done() 结束刷新
// ------------------------------------------------------------------
export function pullToRefresh(root, { threshold = 64, onRefresh } = {}) {
  // 可滚动区域：键盘可以聚焦后用方向键滚动
  if (!root.hasAttribute('tabindex')) root.tabIndex = 0;
  if (!root.hasAttribute('aria-label') && !root.hasAttribute('aria-labelledby')) root.setAttribute('aria-label', '可下拉刷新的列表');
  const content = root.querySelector('.ts-ptr-content') || root.firstElementChild;
  const ind = document.createElement('div');
  ind.className = 'ts-ptr-indicator';
  ind.setAttribute('aria-hidden', 'true');
  ind.append(icon('arrow-down'));
  root.prepend(ind);
  const status = document.createElement('div');
  status.className = 'ts-sr-only';
  status.setAttribute('role', 'status');
  status.style.cssText = 'position:absolute;width:1px;height:1px;overflow:hidden;clip:rect(0 0 0 0)';
  root.append(status);
  let refreshing = false;
  const y = new Spring(0, 'smooth', (v) => {
    content.style.transform = v ? `translateY(${v}px)` : '';
    ind.style.transform = `translateY(${Math.max(-48, v - 52)}px) rotate(${refreshing ? 0 : v * 2}deg)`;
    ind.style.opacity = clamp(v / threshold, 0, 1);
  });
  let s = null;
  root.addEventListener('touchstart', (e) => { if (root.scrollTop <= 0 && !refreshing) s = { y: e.touches[0].clientY, active: false }; }, { passive: true });
  root.addEventListener('touchmove', (e) => {
    if (!s) return;
    const dy = e.touches[0].clientY - s.y;
    if (dy <= 0 && !s.active) { s = null; return; }
    if (root.scrollTop > 0) return;
    s.active = true;
    e.preventDefault(); // 接管下拉，不让页面整体回弹
    const v = rubberBand(dy, threshold * 2.5, 0.8);
    y.set(v);
    ind.toggleAttribute('data-armed', v >= threshold);
  }, { passive: false });
  const end = async () => {
    if (!s) return;
    const active = s.active; s = null;
    if (!active) return;
    if (y.value >= threshold) {
      refreshing = true;
      ind.removeAttribute('data-armed');
      ind.replaceChildren(document.createElement('span'));
      ind.firstElementChild.className = 'ts-spinner';
      ind.firstElementChild.innerHTML = '<i></i><i></i><i></i><i></i>';
      status.textContent = '正在刷新';
      y.to(threshold * 0.85);
      await new Promise((resolve) => { onRefresh ? Promise.resolve(onRefresh()).then(resolve) : emit(root, 'refresh', { done: resolve }); });
      status.textContent = '刷新完成';
      refreshing = false;
      ind.replaceChildren(icon('arrow-down'));
    }
    y.to(0);
  };
  root.addEventListener('touchend', end);
  root.addEventListener('touchcancel', end);
}

// ------------------------------------------------------------------
// 上拉加载：底部哨兵进入视野时触发 load；业务方 resolve 是否还有更多
// ------------------------------------------------------------------
export function infiniteScroll(root, { onLoad, rootMargin = '200px' } = {}) {
  const status = document.createElement('div');
  status.className = 'ts-infinite-status';
  status.setAttribute('role', 'status');
  root.after(status);
  let loading = false, ended = false;
  const io = new IntersectionObserver(async ([e]) => {
    if (!e.isIntersecting || loading || ended) return;
    loading = true;
    status.innerHTML = '<span class="ts-spinner"><i></i><i></i><i></i><i></i></span>正在加载';
    let more = true;
    try { more = await (onLoad ? onLoad() : new Promise((r) => emit(root, 'load', { done: r }))); }
    catch { status.innerHTML = ''; const b = document.createElement('button'); b.className = 'ts-button ts-sm ts-pressable ts-state'; b.textContent = '加载失败，点此重试'; b.onclick = () => { loading = false; io.unobserve(status); io.observe(status); }; status.append(b); return; }
    loading = false;
    if (more === false) { ended = true; status.textContent = '没有更多了'; io.disconnect(); }
    else { status.textContent = ''; io.unobserve(status); io.observe(status); } // 若哨兵仍在视野内，继续加载
  }, { root: root.closest('.ts-scroll-area, .ts-ptr') || null, rootMargin });
  io.observe(status);
}

// ------------------------------------------------------------------
// 字母索引栏：点击或在索引条上滑动快速跳到分组；滑动时显示大字气泡
// 结构：滚动容器里的每个分组标题带 data-index="A"
// ------------------------------------------------------------------
export function indexBar(container) {
  const groups = [...container.querySelectorAll('[data-index]')];
  if (!groups.length) return;
  // 分组标题是吸顶元素，吸住时 offsetTop 会变；在每个标题前放一个不吸顶的标记，用它的位置来跳转
  if (getComputedStyle(container).position === 'static') container.style.position = 'relative';
  // 字母索引就是这个列表的导航，滚动条多余，隐藏（仍可滑动）
  container.classList.add('ts-has-indexbar');
  const marks = groups.map((g) => { const m = document.createElement('span'); m.style.cssText = 'display:block;height:0'; m.setAttribute('aria-hidden', 'true'); g.before(m); return m; });
  const host = container.parentElement;
  if (getComputedStyle(host).position === 'static') host.style.position = 'relative';
  const bar = document.createElement('nav');
  bar.className = 'ts-indexbar';
  bar.setAttribute('aria-label', '索引');
  groups.forEach((g) => {
    const b = document.createElement('button');
    b.type = 'button';
    b.textContent = g.dataset.index;
    b.setAttribute('aria-label', `跳到 ${g.dataset.index}`);
    bar.append(b);
  });
  const bubble = document.createElement('div');
  bubble.className = 'ts-index-bubble';
  bubble.hidden = true;
  bubble.setAttribute('aria-hidden', 'true');
  host.append(bar, bubble);
  let current = -1;
  const jump = (i, smooth = false) => {
    i = clamp(i, 0, groups.length - 1);
    if (i === current) return;
    current = i;
    container.scrollTo({ top: marks[i].offsetTop, behavior: smooth ? 'smooth' : 'instant' });
    [...bar.children].forEach((b, k) => b.setAttribute('aria-current', k === i));
    bubble.textContent = groups[i].dataset.index;
    const r = bar.children[i].getBoundingClientRect(), hr = host.getBoundingClientRect();
    bubble.style.top = `${r.top - hr.top + r.height / 2}px`;
    navigator.vibrate?.(5);
  };
  const at = (y) => { const r = bar.getBoundingClientRect(); return Math.floor(((y - r.top - 6) / (r.height - 12)) * groups.length); };
  bar.addEventListener('pointerdown', (e) => {
    bar.setPointerCapture(e.pointerId);
    bar.setAttribute('data-active', '');
    bubble.hidden = false;
    current = -1;
    jump(at(e.clientY));
    bar.onpointermove = (ev) => jump(at(ev.clientY));
  });
  const end = () => { bar.onpointermove = null; bar.removeAttribute('data-active'); bubble.hidden = true; };
  bar.addEventListener('pointerup', end);
  bar.addEventListener('pointercancel', end);
  bar.addEventListener('keydown', (e) => { const b = e.target.closest('button'); if (b && e.key === 'Enter') { current = -1; jump([...bar.children].indexOf(b), true); } });
  container.addEventListener('scroll', () => {
    if (bar.hasAttribute('data-active')) return;
    let i = 0;
    marks.forEach((m, k) => { if (m.offsetTop <= container.scrollTop + 1) i = k; });
    [...bar.children].forEach((b, k) => b.setAttribute('aria-current', k === i));
  }, { passive: true });
}

export function initGestures(root = document) {
  const $ = (sel, fn) => root.querySelectorAll(sel).forEach((el) => once(el, sel, fn));
  $('.ts-swipe', swipeAction);
  $('.ts-ptr', (el) => pullToRefresh(el));
  $('[data-indexbar]', indexBar);
}
