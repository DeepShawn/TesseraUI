// TesseraUI v0.6.0 — 由 scripts/build.mjs 从 tokens/*.json 生成，请勿手改。
// TesseraUI Web 入口：汇总核心与各组件模块。
// 用法：import Tessera from './tessera.js'; Tessera.init();
// 也可以按需引入单个模块，例如 import { slider } from './modules/forms.js';
import * as core from './core.js';
import * as forms from './modules/forms.js';
import * as pickers from './modules/pickers.js';
import * as navigation from './modules/navigation.js';
import * as data from './modules/data.js';
import * as feedback from './modules/feedback.js';
import * as layout from './modules/layout.js';
import * as gestures from './modules/gestures.js';
import * as flow from './modules/flow.js';

export * from './core.js';
export * from './modules/forms.js';
export * from './modules/pickers.js';
export * from './modules/navigation.js';
export * from './modules/data.js';
export * from './modules/feedback.js';
export * from './modules/layout.js';
export * from './modules/gestures.js';
export * from './modules/flow.js';

/** 初始化 root 范围内的全部组件；对同一元素重复调用是安全的 */
export function init(root = document) {
  core.initCore(root);
  forms.initForms(root);
  pickers.initPickers(root);
  navigation.initNavigation(root);
  data.initData(root);
  feedback.initFeedback(root);
  layout.initLayout(root);
  gestures.initGestures(root);
  flow.initFlow();
}

export default { ...core, ...forms, ...pickers, ...navigation, ...data, ...feedback, ...layout, ...gestures, ...flow, init };
