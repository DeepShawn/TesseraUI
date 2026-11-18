// TesseraUI 图标源。
// 规格：24×24 画布，图形在中间 20×20（2–22）；线条 1.5px，圆头圆角；
// 图标里的方形用 3px 圆角（大方形 3–4px），和组件“圆角方砖”的观感一致。
// stroke = 线性版；fill = 实心版（可选，生成 -fill 版本，用于选中状态）。
const dot = (cx, cy, r = 1) => `<circle cx="${cx}" cy="${cy}" r="${r}" fill="currentColor" stroke="none"/>`;

export const categories = {
  箭头方向: ['arrow-left', 'arrow-right', 'arrow-up', 'arrow-down', 'arrow-up-right', 'arrow-down-left', 'chevron-left', 'chevron-right', 'chevron-up', 'chevron-down', 'chevrons-up-down', 'corner-up-left', 'corner-down-right', 'undo', 'redo', 'expand', 'collapse', 'external-link'],
  通用操作: ['plus', 'minus', 'close', 'check', 'plus-circle', 'minus-circle', 'edit', 'trash', 'copy', 'paste', 'share', 'download', 'upload', 'refresh', 'search', 'zoom-in', 'zoom-out', 'filter', 'sliders', 'sort', 'more', 'more-vertical', 'link', 'bookmark', 'flag', 'tag', 'eye', 'eye-off', 'print', 'archive', 'send', 'history', 'check-square'],
  导航: ['home', 'menu', 'settings', 'grid', 'list', 'sidebar', 'maximize', 'minimize', 'compass', 'globe', 'location', 'calendar', 'clock'],
  状态: ['success', 'warning', 'error', 'info', 'help', 'loader', 'bolt'],
  文件: ['file', 'file-text', 'file-plus', 'folder', 'folder-open', 'image', 'video', 'paperclip', 'cloud', 'book'],
  通讯: ['message', 'message-circle', 'phone', 'mail', 'bell', 'bell-off', 'at-sign', 'inbox'],
  媒体: ['play', 'pause', 'stop', 'skip-back', 'skip-forward', 'volume', 'mute', 'camera', 'mic', 'mic-off', 'music', 'headphones', 'shuffle', 'repeat'],
  用户: ['user', 'users', 'user-plus', 'login', 'logout', 'lock', 'unlock', 'shield', 'shield-check', 'key'],
  商务: ['cart', 'bag', 'wallet', 'credit-card', 'coupon', 'receipt', 'package', 'gift', 'store', 'truck', 'chart-bar', 'trending-up', 'percent', 'briefcase'],
  其他: ['heart', 'star', 'sun', 'moon', 'smile', 'sparkle', 'wifi', 'bluetooth', 'battery', 'layers', 'scan', 'lightbulb', 'code', 'terminal'],
};

const FILE = 'M7 2.5h6.5L19 8v11.5a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2v-15a2 2 0 0 1 2-2z';
const FILE_FOLD = 'M13.5 2.5V6a2 2 0 0 0 2 2H19';
const SPEAKER = 'M4 9.5h2.5L11 5.5v13l-4.5-4H4a1 1 0 0 1-1-1v-3a1 1 0 0 1 1-1z';
const MIC = '<rect x="9" y="2.5" width="6" height="11" rx="3"/><path d="M5.5 11a6.5 6.5 0 0 0 13 0M12 17.5V21"/>';
const BELL = 'M6 16v-5a6 6 0 1 1 12 0v5l1.5 2h-15L6 16z';
const HEART = 'M12 20s-8-4.6-8-10.5A4.5 4.5 0 0 1 12 6.6a4.5 4.5 0 0 1 8 2.9C20 15.4 12 20 12 20z';
const STAR = 'M12 3.5l2.6 5.3 5.9.9-4.25 4.1 1 5.85L12 16.9l-5.25 2.75 1-5.85L3.5 9.7l5.9-.9L12 3.5z';
const HOME = 'M4 10.6a2 2 0 0 1 .74-1.55l6-4.9a2 2 0 0 1 2.52 0l6 4.9a2 2 0 0 1 .74 1.55V19a1.5 1.5 0 0 1-1.5 1.5H15v-5.5a1 1 0 0 0-1-1h-4a1 1 0 0 0-1 1v5.5H5.5A1.5 1.5 0 0 1 4 19v-8.4z';
const PLAY = 'M7 5.2v13.6a1 1 0 0 0 1.5.86l11-6.8a1 1 0 0 0 0-1.72l-11-6.8A1 1 0 0 0 7 5.2z';
const BOOKMARK = 'M7 3h10a2 2 0 0 1 2 2v15.2a.6.6 0 0 1-.95.5L12 16.5l-6.05 4.2a.6.6 0 0 1-.95-.5V5a2 2 0 0 1 2-2z';
const MESSAGE = 'M4 7a3 3 0 0 1 3-3h10a3 3 0 0 1 3 3v7a3 3 0 0 1-3 3H9.5l-4.3 3.4A.75.75 0 0 1 4 19.8V7z';
const FOLDER = 'M2.5 7a2.5 2.5 0 0 1 2.5-2.5h3.6a2 2 0 0 1 1.5.7l1.4 1.6h7.5A2.5 2.5 0 0 1 21.5 9.3V17a2.5 2.5 0 0 1-2.5 2.5H5A2.5 2.5 0 0 1 2.5 17V7z';
const PIN = 'M12 21s-7-6.1-7-11.5A7 7 0 0 1 19 9.5C19 14.9 12 21 12 21z';
const MOON = 'M20 14.5A8 8 0 1 1 9.5 4a6.5 6.5 0 0 0 10.5 10.5z';
const SHIELD = 'M12 3l7.5 3v5.5c0 4.6-3.1 8.3-7.5 9.5-4.4-1.2-7.5-4.9-7.5-9.5V6L12 3z';
const USER_HEAD = '<circle cx="12" cy="8" r="4"/>';
const USER_BODY = 'M4 20.5c.8-3.8 4-6 8-6s7.2 2.2 8 6';
const LOCK_BODY = '<rect x="4" y="10" width="16" height="11" rx="3.5"/>';
const P = (d, extra = '') => `<path d="${d}"${extra}/>`;
const F = (d) => `<path d="${d}" fill="currentColor"/>`;

export const icons = {
  // ---------------- 箭头方向
  'arrow-left': { stroke: P('M19 12H5M11 6l-6 6 6 6') },
  'arrow-right': { stroke: P('M5 12h14M13 6l6 6-6 6') },
  'arrow-up': { stroke: P('M12 19V5M6 11l6-6 6 6') },
  'arrow-down': { stroke: P('M12 5v14M6 13l6 6 6-6') },
  'arrow-up-right': { stroke: P('M7 17L17 7M8.5 7H17v8.5') },
  'arrow-down-left': { stroke: P('M17 7L7 17M15.5 17H7V8.5') },
  'chevron-left': { stroke: P('M15 5l-7 7 7 7') },
  'chevron-right': { stroke: P('M9 5l7 7-7 7') },
  'chevron-up': { stroke: P('M5 15l7-7 7 7') },
  'chevron-down': { stroke: P('M5 9l7 7 7-7') },
  'chevrons-up-down': { stroke: P('M8 9l4-4 4 4M8 15l4 4 4-4') },
  'corner-up-left': { stroke: P('M9 14L4 9l5-5M4 9h10a6 6 0 0 1 6 6v5') },
  'corner-down-right': { stroke: P('M15 10l5 5-5 5M20 15H10a6 6 0 0 1-6-6V4') },
  undo: { stroke: P('M9 14L4 9l5-5M4 9h11a5 5 0 0 1 0 10h-3') },
  redo: { stroke: P('M15 14l5-5-5-5M20 9H9a5 5 0 0 0 0 10h3') },
  expand: { stroke: P('M14 4h6v6M20 4l-6.5 6.5M10 20H4v-6M4 20l6.5-6.5') },
  collapse: { stroke: P('M20 4l-6 6M14 5v5h5M4 20l6-6M10 19v-5H5') },
  'external-link': { stroke: P('M14 4h6v6M20 4l-9 9') + P('M18 14v4a2.5 2.5 0 0 1-2.5 2.5h-9A2.5 2.5 0 0 1 4 18V8.5A2.5 2.5 0 0 1 6.5 6H10') },

  // ---------------- 通用操作
  plus: { stroke: P('M12 4v16M4 12h16') },
  minus: { stroke: P('M4 12h16') },
  close: { stroke: P('M6 6l12 12M18 6L6 18') },
  check: { stroke: P('M4 12.5l5 5L20 6.5') },
  'plus-circle': { stroke: '<circle cx="12" cy="12" r="9"/>' + P('M12 8v8M8 12h8') },
  'minus-circle': { stroke: '<circle cx="12" cy="12" r="9"/>' + P('M8 12h8') },
  edit: { stroke: P('M4 20h4L19 9a2.83 2.83 0 0 0-4-4L4 16v4z') + P('M13.5 6.5l4 4') },
  trash: { stroke: P('M4 6h16M9 6V4.5A1.5 1.5 0 0 1 10.5 3h3A1.5 1.5 0 0 1 15 4.5V6') + P('M6 6l.9 12.6A2.5 2.5 0 0 0 9.4 21h5.2a2.5 2.5 0 0 0 2.5-2.4L18 6') + P('M10 11v5M14 11v5') },
  copy: { stroke: '<rect x="8" y="8" width="12.5" height="12.5" rx="3"/>' + P('M16 8V6.5A3 3 0 0 0 13 3.5H6.5a3 3 0 0 0-3 3V13a3 3 0 0 0 3 3H8') },
  paste: { stroke: P('M9 4.5H7.5a2.5 2.5 0 0 0-2.5 2.5v11.5A2.5 2.5 0 0 0 7.5 21h9a2.5 2.5 0 0 0 2.5-2.5V7a2.5 2.5 0 0 0-2.5-2.5H15') + '<rect x="9" y="2.5" width="6" height="4" rx="1.5"/>' + P('M9 12h6M9 16h4') },
  share: { stroke: P('M12 4v11M8 8l4-4 4 4') + P('M8 11H7.5A2.5 2.5 0 0 0 5 13.5v4A2.5 2.5 0 0 0 7.5 20h9a2.5 2.5 0 0 0 2.5-2.5v-4a2.5 2.5 0 0 0-2.5-2.5H16') },
  download: { stroke: P('M12 4v11M7 10l5 5 5-5M5 20h14') },
  upload: { stroke: P('M12 15V4M7 9l5-5 5 5M5 20h14') },
  refresh: { stroke: P('M20 12a8 8 0 1 1-2.34-5.66') + P('M20 4v4h-4') },
  search: { stroke: '<circle cx="10.5" cy="10.5" r="6.5"/>' + P('M15.5 15.5L20 20') },
  'zoom-in': { stroke: '<circle cx="10.5" cy="10.5" r="6.5"/>' + P('M15.5 15.5L20 20M10.5 8v5M8 10.5h5') },
  'zoom-out': { stroke: '<circle cx="10.5" cy="10.5" r="6.5"/>' + P('M15.5 15.5L20 20M8 10.5h5') },
  filter: { stroke: P('M4 6h16M7 12h10M10 18h4') },
  sliders: { stroke: P('M4 7h9M18 7h2M4 17h2M11 17h9') + '<circle cx="15.5" cy="7" r="2.5"/><circle cx="8.5" cy="17" r="2.5"/>' },
  sort: { stroke: P('M7 4v16M3.5 16.5L7 20l3.5-3.5M17 20V4M13.5 7.5L17 4l3.5 3.5') },
  more: { stroke: dot(5, 12, 1.4) + dot(12, 12, 1.4) + dot(19, 12, 1.4) },
  'more-vertical': { stroke: dot(12, 5, 1.4) + dot(12, 12, 1.4) + dot(12, 19, 1.4) },
  link: { stroke: P('M10 14a4.5 4.5 0 0 0 6.36 0l3.18-3.18a4.5 4.5 0 0 0-6.36-6.36l-1.6 1.6') + P('M14 10a4.5 4.5 0 0 0-6.36 0l-3.18 3.18a4.5 4.5 0 0 0 6.36 6.36l1.6-1.6') },
  bookmark: { stroke: P(BOOKMARK), fill: F(BOOKMARK) },
  flag: { stroke: P('M5 21V4.5M5 4.5h11.2a.8.8 0 0 1 .7 1.15L15 9.5l1.9 3.85a.8.8 0 0 1-.7 1.15H5'), fill: P('M5 21V4.5', ' stroke="currentColor" stroke-width="1.5" stroke-linecap="round"') + F('M5 4.5h11.2a.8.8 0 0 1 .7 1.15L15 9.5l1.9 3.85a.8.8 0 0 1-.7 1.15H5z') },
  tag: { stroke: P('M3 12.2V5.5A2.5 2.5 0 0 1 5.5 3h6.7a2 2 0 0 1 1.4.6l7.6 7.6a2 2 0 0 1 0 2.8l-6.8 6.8a2 2 0 0 1-2.8 0l-7.6-7.6a2 2 0 0 1-.6-1.4z') + dot(7.8, 7.8, 1.3) },
  eye: { stroke: P('M2.5 12S6 5.5 12 5.5 21.5 12 21.5 12 18 18.5 12 18.5 2.5 12 2.5 12z') + '<circle cx="12" cy="12" r="3"/>' },
  'eye-off': { stroke: P('M3 3l18 18') + P('M10.6 5.6c.45-.07.92-.1 1.4-.1 6 0 9.5 6.5 9.5 6.5a17 17 0 0 1-2.6 3.4M6.6 6.6C3.9 8.3 2.5 12 2.5 12S6 18.5 12 18.5c1.7 0 3.3-.5 4.6-1.3') + P('M9.9 9.9a3 3 0 0 0 4.2 4.2') },
  print: { stroke: P('M7 9V4.5A1.5 1.5 0 0 1 8.5 3h7A1.5 1.5 0 0 1 17 4.5V9') + P('M7 17H5.5A2.5 2.5 0 0 1 3 14.5v-3A2.5 2.5 0 0 1 5.5 9h13a2.5 2.5 0 0 1 2.5 2.5v3a2.5 2.5 0 0 1-2.5 2.5H17') + '<rect x="7" y="14" width="10" height="7" rx="1.5"/>' },
  archive: { stroke: '<rect x="3" y="4" width="18" height="5" rx="2"/>' + P('M5 9v9a2.5 2.5 0 0 0 2.5 2.5h9A2.5 2.5 0 0 0 19 18V9M10 13h4') },
  send: { stroke: P('M20.5 3.5L10 14M20.5 3.5l-6.2 16.8a.6.6 0 0 1-1.1.05L10 14l-6.35-3.2a.6.6 0 0 1 .05-1.1L20.5 3.5z') },
  history: { stroke: P('M3.5 12a8.5 8.5 0 1 0 2.5-6M3.5 4v4h4') + P('M12 8v4l3 2') },
  'check-square': { stroke: '<rect x="3.5" y="3.5" width="17" height="17" rx="4"/>' + P('M8 12.5l3 3 5-6') },

  // ---------------- 导航
  home: { stroke: P(HOME), fill: F(HOME) },
  menu: { stroke: P('M4 7h16M4 12h16M4 17h16') },
  settings: { stroke: '<circle cx="12" cy="12" r="3"/>' + P('M12 2.75l2 1.2 2.3-.2 1.1 2 2 1.15-.2 2.3 1.05 1.8-1.05 1.8.2 2.3-2 1.15-1.1 2-2.3-.2-2 1.2-2-1.2-2.3.2-1.1-2-2-1.15.2-2.3L2.75 12 3.8 10.2l-.2-2.3 2-1.15 1.1-2 2.3.2 2-1.2z') },
  grid: { stroke: '<rect x="3.5" y="3.5" width="7" height="7" rx="2.5"/><rect x="13.5" y="3.5" width="7" height="7" rx="2.5"/><rect x="3.5" y="13.5" width="7" height="7" rx="2.5"/><rect x="13.5" y="13.5" width="7" height="7" rx="2.5"/>' },
  list: { stroke: P('M9 6h11M9 12h11M9 18h11') + dot(4.5, 6, 1.2) + dot(4.5, 12, 1.2) + dot(4.5, 18, 1.2) },
  sidebar: { stroke: '<rect x="3" y="4" width="18" height="16" rx="3.5"/>' + P('M9.5 4v16') },
  maximize: { stroke: P('M4 9V6.5A2.5 2.5 0 0 1 6.5 4H9M15 4h2.5A2.5 2.5 0 0 1 20 6.5V9M20 15v2.5a2.5 2.5 0 0 1-2.5 2.5H15M9 20H6.5A2.5 2.5 0 0 1 4 17.5V15') },
  minimize: { stroke: P('M9 4v2.5A2.5 2.5 0 0 1 6.5 9H4M20 9h-2.5A2.5 2.5 0 0 1 15 6.5V4M15 20v-2.5a2.5 2.5 0 0 1 2.5-2.5H20M4 15h2.5A2.5 2.5 0 0 1 9 17.5V20') },
  compass: { stroke: '<circle cx="12" cy="12" r="9"/>' + P('M15.5 8.5l-1.8 4.6a1 1 0 0 1-.6.6l-4.6 1.8 1.8-4.6a1 1 0 0 1 .6-.6l4.6-1.8z') },
  globe: { stroke: '<circle cx="12" cy="12" r="9"/>' + P('M3 12h18M12 3c2.4 2.5 3.6 5.5 3.6 9s-1.2 6.5-3.6 9c-2.4-2.5-3.6-5.5-3.6-9S9.6 5.5 12 3z') },
  location: { stroke: P(PIN) + '<circle cx="12" cy="9.5" r="2.5"/>', fill: `<path fill="currentColor" fill-rule="evenodd" d="${PIN}M12 7a2.5 2.5 0 1 0 0 5 2.5 2.5 0 0 0 0-5z"/>` },
  calendar: { stroke: '<rect x="3" y="5" width="18" height="16" rx="3.5"/>' + P('M3 10h18M8 3v4M16 3v4') },
  clock: { stroke: '<circle cx="12" cy="12" r="9"/>' + P('M12 7v5l3 2') },

  // ---------------- 状态
  success: { stroke: '<circle cx="12" cy="12" r="9"/>' + P('M8 12.25l2.75 2.75L16 9.5'), fill: '<path fill="currentColor" fill-rule="evenodd" d="M12 3a9 9 0 1 0 0 18 9 9 0 0 0 0-18zm4.53 7.03a.75.75 0 0 0-1.06-1.06l-4.72 4.72-2.22-2.22a.75.75 0 0 0-1.06 1.06l2.75 2.75a.75.75 0 0 0 1.06 0l5.25-5.25z"/>' },
  warning: { stroke: P('M10.27 4.5a2 2 0 0 1 3.46 0l7 12.1A2 2 0 0 1 19 19.6H5a2 2 0 0 1-1.73-3l7-12.1z') + P('M12 9.5v4') + dot(12, 16.5) },
  error: { stroke: '<circle cx="12" cy="12" r="9"/>' + P('M9 9l6 6M15 9l-6 6') },
  info: { stroke: '<circle cx="12" cy="12" r="9"/>' + P('M12 11v5') + dot(12, 7.75) },
  help: { stroke: '<circle cx="12" cy="12" r="9"/>' + P('M9.6 9.4a2.5 2.5 0 1 1 3.4 2.4c-.6.3-1 .8-1 1.5v.4') + dot(12, 16.75) },
  loader: { stroke: P('M12 3a9 9 0 1 0 9 9') },
  bolt: { stroke: P('M13 2.5L4.8 13a.6.6 0 0 0 .5 1H12l-1 7.5L19.2 11a.6.6 0 0 0-.5-1H12l1-7.5z') },

  // ---------------- 文件
  file: { stroke: P(FILE) + P(FILE_FOLD) },
  'file-text': { stroke: P(FILE) + P(FILE_FOLD) + P('M8.5 13h7M8.5 17h4.5') },
  'file-plus': { stroke: P(FILE) + P(FILE_FOLD) + P('M12 11.5v6M9 14.5h6') },
  folder: { stroke: P(FOLDER), fill: F(FOLDER) },
  'folder-open': { stroke: P('M3 17.5V7a2.5 2.5 0 0 1 2.5-2.5h3.1a2 2 0 0 1 1.5.7l1.4 1.6h6A2.5 2.5 0 0 1 20 9.3v.7') + P('M3 17.5l2.3-6a2.5 2.5 0 0 1 2.3-1.5H20a1.2 1.2 0 0 1 1.1 1.6L19 17.8a2.5 2.5 0 0 1-2.3 1.7H4.5A1.5 1.5 0 0 1 3 17.5z') },
  image: { stroke: '<rect x="2.5" y="4" width="19" height="16" rx="3.5"/><circle cx="8.5" cy="9.5" r="1.5"/>' + P('M21.5 16l-4.6-4.6a1.5 1.5 0 0 0-2.1 0L7 19.5') },
  video: { stroke: '<rect x="2.5" y="6" width="13" height="12" rx="3.5"/>' + P('M15.5 10.5l5.1-2.9a.6.6 0 0 1 .9.5v7.8a.6.6 0 0 1-.9.5l-5.1-2.9') },
  paperclip: { stroke: P('M20 11.5l-8.2 8.2a5 5 0 0 1-7.1-7.1l8.5-8.5a3.3 3.3 0 0 1 4.7 4.7l-8.5 8.5a1.7 1.7 0 0 1-2.4-2.4l7.8-7.8') },
  cloud: { stroke: P('M7 19a5 5 0 0 1-.8-9.94A6.5 6.5 0 0 1 18.6 9a4.5 4.5 0 0 1-1.1 10H7z') },
  book: { stroke: P('M5 19V5.5A2.5 2.5 0 0 1 7.5 3H19v14H7a2 2 0 0 0-2 2zm0 0a2 2 0 0 0 2 2h12') },

  // ---------------- 通讯
  message: { stroke: P(MESSAGE), fill: F(MESSAGE) },
  'message-circle': { stroke: P('M20.5 11.5a8.5 8.5 0 0 1-12.4 7.55L3.8 20.2a.4.4 0 0 1-.5-.5l1.2-4.1A8.5 8.5 0 1 1 20.5 11.5z') },
  phone: { stroke: P('M5.5 3.5h2.3a1 1 0 0 1 .95.68l1.1 3.3a1 1 0 0 1-.3 1.08L8 9.8a11 11 0 0 0 6.2 6.2l1.24-1.55a1 1 0 0 1 1.08-.3l3.3 1.1a1 1 0 0 1 .68.95v2.3a2 2 0 0 1-2 2A16.5 16.5 0 0 1 3.5 5.5a2 2 0 0 1 2-2z') },
  mail: { stroke: '<rect x="2.5" y="4.5" width="19" height="15" rx="3.5"/>' + P('M3 7.5l7.9 5.3a2 2 0 0 0 2.2 0L21 7.5') },
  bell: { stroke: P(BELL) + P('M10 20.5a2.2 2.2 0 0 0 4 0'), fill: F(BELL) + P('M10 20.5a2.2 2.2 0 0 0 4 0', ' stroke="currentColor" stroke-width="1.5" stroke-linecap="round"') },
  'bell-off': { stroke: P('M3 3l18 18') + P('M6.6 7.9A6 6 0 0 0 6 11v5l-1.5 2H17M9.4 5.6A6 6 0 0 1 18 11v4') + P('M10 20.5a2.2 2.2 0 0 0 4 0') },
  'at-sign': { stroke: '<circle cx="12" cy="12" r="4"/>' + P('M16 8v5a2.5 2.5 0 0 0 5 0v-1a9 9 0 1 0-3.5 7.1') },
  inbox: { stroke: P('M3 13h4.6a1 1 0 0 1 .9.55L9.2 15a1 1 0 0 0 .9.55h3.8a1 1 0 0 0 .9-.55l.7-1.45a1 1 0 0 1 .9-.55H21') + P('M5.6 5.6A2.5 2.5 0 0 1 7.9 4h8.2a2.5 2.5 0 0 1 2.3 1.6L21 13v4.5a2.5 2.5 0 0 1-2.5 2.5h-13A2.5 2.5 0 0 1 3 17.5V13l2.6-7.4z') },

  // ---------------- 媒体
  play: { stroke: P(PLAY), fill: F(PLAY) },
  pause: { stroke: '<rect x="6" y="4" width="4" height="16" rx="1.5"/><rect x="14" y="4" width="4" height="16" rx="1.5"/>', fill: '<rect x="6" y="4" width="4" height="16" rx="1.5" fill="currentColor"/><rect x="14" y="4" width="4" height="16" rx="1.5" fill="currentColor"/>' },
  stop: { stroke: '<rect x="5" y="5" width="14" height="14" rx="3.5"/>', fill: '<rect x="5" y="5" width="14" height="14" rx="3.5" fill="currentColor"/>' },
  'skip-back': { stroke: P('M19 6.1v11.8a.8.8 0 0 1-1.25.66L9.2 12.66a.8.8 0 0 1 0-1.32l8.55-5.9A.8.8 0 0 1 19 6.1zM5 5v14') },
  'skip-forward': { stroke: P('M5 6.1v11.8a.8.8 0 0 0 1.25.66l8.55-5.9a.8.8 0 0 0 0-1.32L6.25 5.44A.8.8 0 0 0 5 6.1zM19 5v14') },
  volume: { stroke: P(SPEAKER) + P('M15 9a4 4 0 0 1 0 6M17.8 6.3a8 8 0 0 1 0 11.4') },
  mute: { stroke: P(SPEAKER) + P('M15.5 9.5l5 5M20.5 9.5l-5 5') },
  camera: { stroke: P('M3 8.5A2.5 2.5 0 0 1 5.5 6h1.8l1.2-1.6A1 1 0 0 1 9.3 4h5.4a1 1 0 0 1 .8.4L16.7 6h1.8A2.5 2.5 0 0 1 21 8.5v9a2.5 2.5 0 0 1-2.5 2.5h-13A2.5 2.5 0 0 1 3 17.5v-9z') + '<circle cx="12" cy="13" r="3.5"/>' },
  mic: { stroke: MIC },
  'mic-off': { stroke: MIC + P('M3 3l18 18') },
  music: { stroke: P('M9 18V5.3a1 1 0 0 1 .8-1l9-1.7a1 1 0 0 1 1.2 1V16') + '<circle cx="6.5" cy="18" r="2.5"/><circle cx="17.5" cy="16" r="2.5"/>' },
  headphones: { stroke: P('M3.5 18v-5a8.5 8.5 0 0 1 17 0v5') + '<rect x="3.5" y="13.5" width="4.5" height="7" rx="2"/><rect x="16" y="13.5" width="4.5" height="7" rx="2"/>' },
  shuffle: { stroke: P('M16 4h4v4M4 20L20 4M20 16v4h-4M15 15l5 5M4 4l5 5') },
  repeat: { stroke: P('M17 2.5l3 3-3 3M4 11v-1A4.5 4.5 0 0 1 8.5 5.5H20M7 21.5l-3-3 3-3M20 13v1a4.5 4.5 0 0 1-4.5 4.5H4') },

  // ---------------- 用户
  user: { stroke: USER_HEAD + P(USER_BODY), fill: '<circle cx="12" cy="8" r="4" fill="currentColor"/>' + F(USER_BODY + 'a.6.6 0 0 1-.6.7H4.6a.6.6 0 0 1-.6-.7z') },
  users: { stroke: '<circle cx="9" cy="8" r="3.5"/>' + P('M2.5 20c.6-3.4 3.2-5.5 6.5-5.5s5.9 2.1 6.5 5.5M15.5 4.6a3.5 3.5 0 0 1 0 6.8M18 14.8c1.9.8 3.1 2.7 3.5 5.2') },
  'user-plus': { stroke: '<circle cx="10" cy="8" r="4"/>' + P('M3 20.5c.8-3.8 3.6-6 7-6 1.4 0 2.8.4 3.9 1.1M19 13v6M16 16h6') },
  login: { stroke: P('M14 4h3a3 3 0 0 1 3 3v10a3 3 0 0 1-3 3h-3M4 12h10M10 8l4 4-4 4') },
  logout: { stroke: P('M10 4H7a3 3 0 0 0-3 3v10a3 3 0 0 0 3 3h3M10 12h10M16 8l4 4-4 4') },
  lock: { stroke: LOCK_BODY + P('M8 10V7a4 4 0 1 1 8 0v3M12 14.5v2') },
  unlock: { stroke: LOCK_BODY + P('M8 10V7a4 4 0 0 1 7.75-1.4M12 14.5v2') },
  shield: { stroke: P(SHIELD), fill: F(SHIELD) },
  'shield-check': { stroke: P(SHIELD) + P('M9 12l2 2 4-4') },
  key: { stroke: '<circle cx="8" cy="15" r="4.5"/>' + P('M11.2 11.8L20 3M17 6l2.5 2.5M14.5 8.5l2 2') },

  // ---------------- 商务
  cart: { stroke: P('M2.5 3.5h2.2l2.3 11.2a1.5 1.5 0 0 0 1.47 1.2h9.06a1.5 1.5 0 0 0 1.46-1.15L21 7H5.6') + '<circle cx="9" cy="20" r="1.25"/><circle cx="17" cy="20" r="1.25"/>' },
  bag: { stroke: P('M5.4 8h13.2a1 1 0 0 1 1 1.1l-.9 9.6a2.5 2.5 0 0 1-2.5 2.3H7.8a2.5 2.5 0 0 1-2.5-2.3l-.9-9.6a1 1 0 0 1 1-1.1zM9 10V7a3 3 0 0 1 6 0v3') },
  wallet: { stroke: '<rect x="3" y="5" width="18" height="15" rx="3.5"/>' + P('M21 10h-4a2.5 2.5 0 0 0 0 5h4') + dot(16.5, 12.5, 1) },
  'credit-card': { stroke: '<rect x="2.5" y="5" width="19" height="14" rx="3.5"/>' + P('M2.5 10h19M6.5 15h4') },
  coupon: { stroke: P('M3 8a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2v1.5a2.5 2.5 0 0 0 0 5V16a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-1.5a2.5 2.5 0 0 0 0-5V8z') + P('M14.5 8.5v1M14.5 11.5v1M14.5 14.5v1') },
  receipt: { stroke: P('M5 4.5A1.5 1.5 0 0 1 6.5 3h11A1.5 1.5 0 0 1 19 4.5v16l-2.4-1.5-2.3 1.5-2.3-1.5-2.3 1.5-2.3-1.5L5 20.5v-16z') + P('M9 8h6M9 12h6M9 16h3') },
  package: { stroke: P('M11 3.5a2 2 0 0 1 2 0l6 3.4a2 2 0 0 1 1 1.7v6.8a2 2 0 0 1-1 1.7l-6 3.4a2 2 0 0 1-2 0l-6-3.4a2 2 0 0 1-1-1.7V8.6a2 2 0 0 1 1-1.7l6-3.4z') + P('M4.3 7.6L12 12l7.7-4.4M12 12v9') },
  gift: { stroke: '<rect x="3" y="8" width="18" height="4" rx="1.5"/>' + P('M5 12v6.5A2.5 2.5 0 0 0 7.5 21h9a2.5 2.5 0 0 0 2.5-2.5V12M12 8v13M12 8S10.5 3.5 8 3.5a2.25 2.25 0 0 0 0 4.5M12 8s1.5-4.5 4-4.5a2.25 2.25 0 0 1 0 4.5') },
  store: { stroke: P('M3.5 9l1.2-3.8A1.5 1.5 0 0 1 6.1 4h11.8a1.5 1.5 0 0 1 1.4 1.2L20.5 9M3.5 9a2.8 2.8 0 0 0 5.6 0 2.9 2.9 0 0 0 5.8 0 2.8 2.8 0 0 0 5.6 0') + P('M5 12v7a1.5 1.5 0 0 0 1.5 1.5h11A1.5 1.5 0 0 0 19 19v-7M10 20.5v-4a1 1 0 0 1 1-1h2a1 1 0 0 1 1 1v4') },
  truck: { stroke: P('M2.5 7A2 2 0 0 1 4.5 5h8a1 1 0 0 1 1 1v10.5H2.5V7zM13.5 9h4.1a1 1 0 0 1 .78.37l2.9 3.6a1 1 0 0 1 .22.63V16.5h-8') + '<circle cx="7" cy="17.5" r="2"/><circle cx="17" cy="17.5" r="2"/>' },
  'chart-bar': { stroke: P('M4 20h16M7 16v-5M12 16V6M17 16V9') },
  'trending-up': { stroke: P('M3 17l6-6 4 4 8-8M15 7h6v6') },
  percent: { stroke: P('M19 5L5 19') + '<circle cx="7" cy="7" r="2.5"/><circle cx="17" cy="17" r="2.5"/>' },
  briefcase: { stroke: '<rect x="3" y="7" width="18" height="13" rx="3.5"/>' + P('M9 7V5.5A1.5 1.5 0 0 1 10.5 4h3A1.5 1.5 0 0 1 15 5.5V7M3 12.5h18') },

  // ---------------- 其他
  heart: { stroke: P(HEART), fill: F(HEART) },
  star: { stroke: P(STAR), fill: F(STAR) },
  sun: { stroke: '<circle cx="12" cy="12" r="4"/>' + P('M12 2.5v2M12 19.5v2M4.6 4.6L6 6M18 18l1.4 1.4M2.5 12h2M19.5 12h2M4.6 19.4L6 18M18 6l1.4-1.4') },
  moon: { stroke: P(MOON), fill: F(MOON) },
  smile: { stroke: '<circle cx="12" cy="12" r="9"/>' + P('M8.5 14.5a4.5 4.5 0 0 0 7 0') + dot(9, 10) + dot(15, 10) },
  sparkle: { stroke: P('M11 3.5c.6 4.2 1.8 5.4 6 6-4.2.6-5.4 1.8-6 6-.6-4.2-1.8-5.4-6-6 4.2-.6 5.4-1.8 6-6zM18.5 15c.3 1.6.9 2.2 2.5 2.5-1.6.3-2.2.9-2.5 2.5-.3-1.6-.9-2.2-2.5-2.5 1.6-.3 2.2-.9 2.5-2.5z') },
  wifi: { stroke: P('M2.5 9a14 14 0 0 1 19 0M5.5 12.5a9.5 9.5 0 0 1 13 0M8.5 16a5 5 0 0 1 7 0') + dot(12, 19.25, 1.25) },
  bluetooth: { stroke: P('M7 7l10 10-5 4.5v-19L17 7 7 17') },
  battery: { stroke: '<rect x="2.5" y="7" width="16" height="10" rx="3"/>' + P('M21.5 10.5v3M6.5 10.5v3M10 10.5v3') },
  layers: { stroke: P('M11.1 3.45a2 2 0 0 1 1.8 0l7.6 3.8a.5.5 0 0 1 0 .9l-7.6 3.8a2 2 0 0 1-1.8 0l-7.6-3.8a.5.5 0 0 1 0-.9l7.6-3.8zM3 12.5l8.1 4.05a2 2 0 0 0 1.8 0L21 12.5M3 16.5l8.1 4.05a2 2 0 0 0 1.8 0L21 16.5') },
  scan: { stroke: P('M4 8.5V6.5A2.5 2.5 0 0 1 6.5 4h2M15.5 4h2A2.5 2.5 0 0 1 20 6.5v2M20 15.5v2a2.5 2.5 0 0 1-2.5 2.5h-2M8.5 20h-2A2.5 2.5 0 0 1 4 17.5v-2M4 12h16') },
  lightbulb: { stroke: P('M9 18h6M10 21h4M8.5 14.5a6 6 0 1 1 7 0c-.9.7-1.5 1.8-1.5 3v.5h-4v-.5c0-1.2-.6-2.3-1.5-3z') },
  code: { stroke: P('M8 7l-5 5 5 5M16 7l5 5-5 5M13.5 4.5l-3 15') },
  terminal: { stroke: '<rect x="3" y="4" width="18" height="16" rx="3.5"/>' + P('M7 9l3 3-3 3M12.5 15H17') },
};
