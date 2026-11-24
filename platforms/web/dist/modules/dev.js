// TesseraUI v0.6.1 — 由 scripts/build.mjs 从 tokens/*.json 生成，请勿手改。
// 开发者组件：CodeEditor（代码编辑器）、HexView（十六进制查看）
import { once, emit, esc, clamp, uid } from '../core.js';

// ------------------------------------------------------------------
// 语法高亮：轻量分词（注释、字符串、数字、关键字、函数名），覆盖常见语言；不认识的语言只做转义
// ------------------------------------------------------------------
const KEYWORDS = {
  js: 'await async break case catch class const continue default delete do else export extends false finally for from function if import in instanceof let new null of return static super switch this throw true try typeof undefined var void while yield',
  kotlin: 'as break class continue do else false for fun if in interface is null object package return super this throw true try typealias typeof val var when while data sealed private public internal override suspend import',
  swift: 'as break case catch class continue default defer do else enum extension false for func guard if import in init let nil private protocol public return self static struct switch throw throws true try var while some any async await',
  csharp: 'abstract as async await base bool break case catch class const continue default do double else enum false finally for foreach if in int interface internal is namespace new null object out override private protected public readonly return sealed static string struct switch this throw true try using var void while',
  python: 'and as assert async await break class continue def del elif else except False finally for from global if import in is lambda None nonlocal not or pass raise return True try while with yield',
  json: 'true false null',
};
KEYWORDS.ts = KEYWORDS.js + ' interface type enum implements private public readonly declare keyof as';
const ALIASES = { javascript: 'js', typescript: 'ts', kt: 'kotlin', cs: 'csharp', 'c#': 'csharp', py: 'python' };

/** 把代码转成带 ts-tok-* 类名的 HTML（只做转义与着色，不改变字符） */
export function highlight(code, lang = '') {
  const l = ALIASES[lang.toLowerCase()] || lang.toLowerCase();
  const kw = KEYWORDS[l];
  if (!kw) return esc(code);
  const words = new Set(kw.split(' '));
  const hashComment = l === 'python';
  const re = hashComment
    ? /(#[^\n]*)|("""[\s\S]*?"""|'''[\s\S]*?'''|"(?:\\.|[^"\\\n])*"|'(?:\\.|[^'\\\n])*')|(\b\d[\d_]*(?:\.\d+)?(?:e[+-]?\d+)?\b|\b0x[\da-f]+\b)|([A-Za-z_$][\w$]*)(?=\s*\()|([A-Za-z_$][\w$]*)/gi
    : /(\/\/[^\n]*|\/\*[\s\S]*?(?:\*\/|$))|("(?:\\.|[^"\\\n])*"|'(?:\\.|[^'\\\n])*'|`(?:\\.|[^`\\])*`)|(\b\d[\d_]*(?:\.\d+)?(?:e[+-]?\d+)?[fFLu]?\b|\b0x[\da-f]+\b)|([A-Za-z_$][\w$]*)(?=\s*\()|([A-Za-z_$][\w$]*)/gi;
  let out = '', last = 0, m;
  while ((m = re.exec(code))) {
    out += esc(code.slice(last, m.index));
    const [t, com, str, num, fn, word] = m;
    if (com) out += `<span class="ts-tok-com">${esc(t)}</span>`;
    else if (str) out += `<span class="ts-tok-str">${esc(t)}</span>`;
    else if (num) out += `<span class="ts-tok-num">${esc(t)}</span>`;
    else if (fn) out += words.has(fn) ? `<span class="ts-tok-kw">${esc(t)}</span>` : `<span class="ts-tok-fn">${esc(t)}</span>`;
    else if (word && words.has(word)) out += `<span class="ts-tok-kw">${esc(t)}</span>`;
    else out += esc(t);
    last = m.index + t.length;
  }
  return out + esc(code.slice(last));
}

// ------------------------------------------------------------------
// 代码编辑器 CodeEditor
// - 高亮层（pre）在下，透明的 textarea 在上：输入、选择、撤销、输入法、读屏都用系统原生的文本框
// - 行号、当前行底色、第几行第几列
// - Tab 缩进 / Shift+Tab 反缩进（多行选中时整块缩进）；回车保持上一行缩进，在 { [ ( : 之后多缩一级
// - 按 Esc 之后再按 Tab 会离开编辑器（避免键盘被困住）
// - 输入法组合中不处理任何快捷键
// - setDiagnostics([{ line, message, severity }])：出错的行号变红、整行淡红底，提示列在编辑器下方并朗读
// 用法：<div class="ts-code-editor" data-lang="js" data-label="脚本"><textarea>…</textarea></div>
// ------------------------------------------------------------------
export function codeEditor(root, { indent = Number(root.dataset.indent || 2), lang = root.dataset.lang || '', readOnly = root.hasAttribute('data-readonly') } = {}) {
  const src = root.querySelector('textarea') || document.createElement('textarea');
  const initial = src.value;
  const label = root.dataset.label || '代码';
  const id = uid('code-editor');
  root.innerHTML = `
    <div class="ts-ce-head"><span class="ts-ce-lang">${esc(lang)}</span><span class="ts-ce-pos" aria-hidden="true"></span></div>
    <div class="ts-ce-body">
      <div class="ts-ce-gutter" aria-hidden="true"></div>
      <div class="ts-ce-stage">
        <div class="ts-ce-line" aria-hidden="true"></div>
        <pre class="ts-ce-highlight" aria-hidden="true"><code></code></pre>
        <textarea class="ts-ce-input" id="${id}" spellcheck="false" autocapitalize="off" autocomplete="off" autocorrect="off" wrap="off" aria-label="${esc(label)}" aria-describedby="${id}-hint ${id}-diag"></textarea>
      </div>
    </div>
    <p class="ts-ce-hint" id="${id}-hint">按 Tab 缩进；按 Esc 后再按 Tab 可离开编辑器</p>
    <ul class="ts-ce-diagnostics" id="${id}-diag" aria-live="polite"></ul>`;
  const ta = root.querySelector('textarea'), code = root.querySelector('pre code'), gutter = root.querySelector('.ts-ce-gutter');
  const lineBar = root.querySelector('.ts-ce-line'), pos = root.querySelector('.ts-ce-pos'), diag = root.querySelector('.ts-ce-diagnostics');
  const stage = root.querySelector('.ts-ce-stage');
  ta.value = initial;
  ta.readOnly = readOnly;
  if (readOnly) root.setAttribute('data-readonly', '');
  const unit = ' '.repeat(indent);
  let errors = new Map(), exitOnTab = false, composing = false;

  const lineHeight = () => parseFloat(getComputedStyle(ta).lineHeight) || 21;
  const caretLine = () => ta.value.slice(0, ta.selectionStart).split('\n').length - 1;
  function render() {
    // 末尾补一个换行：最后一行是空行时，高亮层和输入框高度一致
    code.innerHTML = highlight(ta.value, lang) + '\n';
    const n = ta.value.split('\n').length;
    if (gutter.childElementCount !== n) gutter.innerHTML = Array.from({ length: n }, (_, i) => `<span>${i + 1}</span>`).join('');
    [...gutter.children].forEach((s, i) => s.toggleAttribute('data-error', errors.has(i + 1)));
    stage.querySelectorAll('.ts-ce-error-line').forEach((e) => e.remove());
    const h = lineHeight();
    errors.forEach((_, line) => {
      const bar = document.createElement('div');
      bar.className = 'ts-ce-error-line';
      bar.setAttribute('aria-hidden', 'true');
      bar.style.top = `calc(var(--_py) + ${(line - 1) * h}px)`;
      stage.prepend(bar);
    });
    caret();
  }
  function caret() {
    const line = caretLine();
    const col = ta.selectionStart - ta.value.lastIndexOf('\n', ta.selectionStart - 1);
    lineBar.style.top = `calc(var(--_py) + ${line * lineHeight()}px)`;
    [...gutter.children].forEach((s, i) => s.toggleAttribute('data-current', i === line));
    pos.textContent = `第 ${line + 1} 行，第 ${col} 列`;
  }
  // 插入文字：用 execCommand 保留浏览器的撤销记录；不支持时退回直接改值
  const insert = (text) => {
    if (!document.execCommand?.('insertText', false, text)) ta.setRangeText(text, ta.selectionStart, ta.selectionEnd, 'end');
  };
  const lineStartOf = (i) => ta.value.lastIndexOf('\n', i - 1) + 1;

  function indentLines(outdent) {
    const v = ta.value, s = ta.selectionStart, e = ta.selectionEnd;
    const from = lineStartOf(s);
    const to = e > s && v[e - 1] === '\n' ? e - 1 : e;
    const block = v.slice(from, to);
    const lines = block.split('\n');
    const changed = lines.map((l) => (outdent ? l.replace(new RegExp(`^ {1,${indent}}|^\\t`), '') : unit + l)).join('\n');
    ta.setSelectionRange(from, to);
    insert(changed);
    const firstDelta = outdent ? lines[0].length - changed.split('\n')[0].length : indent;
    ta.setSelectionRange(Math.max(from, s + (outdent ? -firstDelta : firstDelta)), from + changed.length);
  }

  ta.addEventListener('compositionstart', () => (composing = true));
  ta.addEventListener('compositionend', () => { composing = false; render(); });
  ta.addEventListener('keydown', (e) => {
    if (e.isComposing || composing || e.keyCode === 229 || ta.readOnly) return;
    if (e.key === 'Escape') { exitOnTab = true; return; }
    if (e.key === 'Tab') {
      if (exitOnTab) { exitOnTab = false; return; } // 交给浏览器移动焦点
      e.preventDefault();
      const multi = ta.value.slice(ta.selectionStart, ta.selectionEnd).includes('\n');
      if (e.shiftKey || multi) indentLines(e.shiftKey);
      else insert(unit);
      return;
    }
    exitOnTab = false;
    if (e.key === 'Enter' && !e.ctrlKey && !e.metaKey) {
      e.preventDefault();
      const v = ta.value, s = ta.selectionStart;
      const line = v.slice(lineStartOf(s), s);
      let lead = line.match(/^[ \t]*/)[0];
      if (/[{[(:]\s*$/.test(line)) lead += unit;
      // 光标正好在一对括号中间：把右括号放到下一行，与本行对齐
      const after = v[s];
      if (/[{[(]\s*$/.test(line) && /[}\])]/.test(after || '')) {
        const base = line.match(/^[ \t]*/)[0];
        insert(`\n${lead}\n${base}`);
        const p = ta.selectionStart - base.length - 1;
        ta.setSelectionRange(p, p);
      } else insert(`\n${lead}`);
    }
  });
  ta.addEventListener('input', () => { render(); emit(root, 'change', { value: ta.value }); });
  ['keyup', 'click', 'select', 'focus'].forEach((t) => ta.addEventListener(t, caret));
  document.addEventListener('selectionchange', () => { if (document.activeElement === ta) caret(); });
  ta.addEventListener('blur', () => (exitOnTab = false));
  new ResizeObserver(() => render()).observe(root);
  render();

  return {
    get value() { return ta.value; },
    set value(v) { ta.value = v; render(); },
    textarea: ta,
    /** 诊断信息：[{ line: 3, message: '缺少右括号' }]，传空数组清除 */
    setDiagnostics(list = []) {
      errors = new Map(list.map((d) => [d.line, d.message]));
      diag.innerHTML = list.map((d) => `<li><span class="ts-ce-diag-line">第 ${d.line} 行</span>${esc(d.message)}</li>`).join('');
      ta.setAttribute('aria-invalid', list.length ? 'true' : 'false');
      render();
    },
  };
}

// ------------------------------------------------------------------
// 十六进制查看 HexView
// - 偏移 / 16 个字节（窄于 560 时 8 个）/ ASCII 三栏；不可打印字符显示为“·”
// - 虚拟滚动：只渲染可见的行，几 MB 的文件也不卡
// - 点击或方向键选中一个字节，十六进制与 ASCII 两栏同步高亮；Shift 扩展选区，Ctrl+C 复制为十六进制
// - 状态栏读出偏移、十六进制 / 十进制 / 二进制值和字符（读屏朗读）
// - highlights：[{ from, to, tone, label }] 给一段字节着色（例如文件头），下方显示图例
// 用法：hexView(el, { data: Uint8Array, highlights })
// ------------------------------------------------------------------
const hex2 = (n) => n.toString(16).toUpperCase().padStart(2, '0');
const printable = (b) => (b >= 0x20 && b < 0x7f ? String.fromCharCode(b) : '·');

export function hexView(root, { data = new Uint8Array(0), highlights = [], columns, label = root.dataset.label || '十六进制数据' } = {}) {
  const id = uid('hex');
  root.innerHTML = `
    <div class="ts-hex-head" aria-hidden="true"><span class="ts-hex-off">偏移</span><span class="ts-hex-bytes"></span><span class="ts-hex-ascii">ASCII</span></div>
    <div class="ts-hex-scroll" tabindex="0" role="grid" aria-label="${esc(label)}" aria-describedby="${id}-status" aria-readonly="true"><div class="ts-hex-space"><div class="ts-hex-rows"></div></div></div>
    <div class="ts-hex-status" id="${id}-status" aria-live="polite"></div>
    <div class="ts-hex-legend"></div>`;
  const scroll = root.querySelector('.ts-hex-scroll'), space = root.querySelector('.ts-hex-space'), rowsEl = root.querySelector('.ts-hex-rows');
  const head = root.querySelector('.ts-hex-bytes'), status = root.querySelector('.ts-hex-status'), legend = root.querySelector('.ts-hex-legend');
  let cols = columns || 16, anchor = 0, focus = 0, hover = -1;
  const rowH = () => parseFloat(getComputedStyle(root).getPropertyValue('--_row')) || 26;
  const toneOf = (i) => highlights.find((h) => i >= h.from && i < h.to);
  legend.innerHTML = highlights.map((h) => `<span><i data-tone="${h.tone || 'accent'}"></i>${esc(h.label || '')} <code>${h.from}–${h.to - 1}</code></span>`).join('');

  function layout() {
    const c = columns || (root.clientWidth && root.clientWidth < 560 ? 8 : 16);
    if (c !== cols || !head.childElementCount) {
      cols = c;
      root.style.setProperty('--_cols', cols);
      root.toggleAttribute('data-narrow', cols < 16);
      head.innerHTML = Array.from({ length: cols }, (_, i) => `<span>${hex2(i)}</span>`).join('');
    }
    space.style.height = `${Math.ceil(data.length / cols) * rowH()}px`;
    paint();
  }
  function paint() {
    const h = rowH(), total = Math.ceil(data.length / cols);
    const first = Math.max(0, Math.floor(scroll.scrollTop / h) - 8);
    const last = Math.min(total, Math.ceil((scroll.scrollTop + scroll.clientHeight) / h) + 8);
    const lo = Math.min(anchor, focus), hi = Math.max(anchor, focus);
    let html = '';
    for (let r = first; r < last; r++) {
      const off = r * cols;
      let bytes = '', ascii = '';
      for (let k = 0; k < cols; k++) {
        const i = off + k;
        if (i >= data.length) { bytes += '<span class="ts-hex-pad"></span>'; continue; }
        const t = toneOf(i);
        const attrs = `data-i="${i}"${i >= lo && i <= hi ? ' data-selected' : ''}${i === focus ? ` id="${id}-b${i}" data-focus` : ''}${i === hover ? ' data-hover' : ''}${t ? ` data-tone="${t.tone || 'accent'}"` : ''}${k === cols / 2 ? ' data-mid' : ''}`;
        bytes += `<span role="gridcell" ${attrs} aria-label="${hex2(data[i])}">${hex2(data[i])}</span>`;
        ascii += `<span ${attrs.replace(/ id="[^"]+"/, '')} aria-hidden="true">${esc(printable(data[i]))}</span>`;
      }
      html += `<div class="ts-hex-row" role="row" style="top:${r * h}px"><span class="ts-hex-off" role="rowheader">${off.toString(16).toUpperCase().padStart(8, '0')}</span><span class="ts-hex-bytes">${bytes}</span><span class="ts-hex-ascii">${ascii}</span></div>`;
    }
    rowsEl.innerHTML = html;
    if (data.length) scroll.setAttribute('aria-activedescendant', `${id}-b${focus}`);
  }
  function describe() {
    if (!data.length) { status.textContent = '没有数据'; return; }
    const b = data[focus];
    const lo = Math.min(anchor, focus), hi = Math.max(anchor, focus);
    const range = hi > lo ? `，已选 ${hi - lo + 1} 个字节（${lo}–${hi}）` : '';
    const t = toneOf(focus);
    status.textContent = `偏移 0x${focus.toString(16).toUpperCase()}（${focus}）：0x${hex2(b)} = ${b} = 0b${b.toString(2).padStart(8, '0')}${b >= 0x20 && b < 0x7f ? `，字符“${String.fromCharCode(b)}”` : ''}${t?.label ? `，${t.label}` : ''}${range}`;
  }
  function select(i, extend = false) {
    focus = clamp(i, 0, Math.max(0, data.length - 1));
    if (!extend) anchor = focus;
    // 选中的字节滚到可见范围内
    const h = rowH(), top = Math.floor(focus / cols) * h;
    if (top < scroll.scrollTop) scroll.scrollTop = top;
    else if (top + h > scroll.scrollTop + scroll.clientHeight) scroll.scrollTop = top + h - scroll.clientHeight;
    paint();
    describe();
    emit(root, 'select', { offset: focus, from: Math.min(anchor, focus), to: Math.max(anchor, focus), value: data[focus] });
  }
  rowsEl.addEventListener('pointerdown', (e) => {
    const cell = e.target.closest('[data-i]');
    if (!cell) return;
    e.preventDefault();
    scroll.focus({ preventScroll: true });
    select(Number(cell.dataset.i), e.shiftKey);
  });
  rowsEl.addEventListener('pointerover', (e) => {
    const cell = e.target.closest('[data-i]');
    const i = cell ? Number(cell.dataset.i) : -1;
    if (i === hover) return;
    hover = i;
    // 悬停：十六进制与 ASCII 两栏的同一个字节一起高亮（只改类名，不重绘）
    rowsEl.querySelectorAll('[data-hover]').forEach((x) => x.removeAttribute('data-hover'));
    if (i >= 0) rowsEl.querySelectorAll(`[data-i="${i}"]`).forEach((x) => x.setAttribute('data-hover', ''));
  });
  rowsEl.addEventListener('pointerleave', () => { hover = -1; rowsEl.querySelectorAll('[data-hover]').forEach((x) => x.removeAttribute('data-hover')); });
  scroll.addEventListener('keydown', (e) => {
    const page = Math.max(1, Math.floor(scroll.clientHeight / rowH())) * cols;
    const map = {
      ArrowRight: focus + 1, ArrowLeft: focus - 1, ArrowDown: focus + cols, ArrowUp: focus - cols,
      PageDown: focus + page, PageUp: focus - page,
      Home: e.ctrlKey || e.metaKey ? 0 : focus - (focus % cols),
      End: e.ctrlKey || e.metaKey ? data.length - 1 : Math.min(data.length - 1, focus - (focus % cols) + cols - 1),
    };
    if (e.key in map) { e.preventDefault(); select(map[e.key], e.shiftKey); return; }
    if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'c') {
      e.preventDefault();
      navigator.clipboard?.writeText(selectionHex());
      status.textContent = `已复制 ${Math.abs(focus - anchor) + 1} 个字节`;
    }
  });
  scroll.addEventListener('focus', describe);
  // 横向滚动时表头跟着走（窄屏上 ASCII 栏在右侧，需要横向滚动才能看到）
  const headRow = root.querySelector('.ts-hex-head');
  scroll.addEventListener('scroll', () => { headRow.scrollLeft = scroll.scrollLeft; requestAnimationFrame(paint); }, { passive: true });
  new ResizeObserver(layout).observe(root);
  const selectionHex = () => {
    const lo = Math.min(anchor, focus), hi = Math.max(anchor, focus);
    return [...data.subarray(lo, hi + 1)].map(hex2).join(' ');
  };
  layout();
  describe();
  return {
    select,
    get selection() { return { from: Math.min(anchor, focus), to: Math.max(anchor, focus) }; },
    get selectionHex() { return selectionHex(); },
    setData(d, hl = highlights) { data = d; highlights = hl; anchor = focus = 0; layout(); describe(); },
  };
}

export function initDev(root = document) {
  root.querySelectorAll('.ts-code-editor').forEach((el) => once(el, 'code-editor', () => { el._tsEditor = codeEditor(el); }));
}
