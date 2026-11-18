// 由 scripts/build-icons.mjs 从 icons/source.mjs 生成，请勿手改。
package com.tesseraui.icons

import androidx.compose.ui.graphics.vector.ImageVector

/** TesseraUI 图标。线性版为默认，实心版加 Fill 后缀；与 Web、Apple、Windows 共用同一份源。 */
object TesseraIcons {
    val ArrowLeft: ImageVector by lazy { build("arrow-left", P("M 19 12 H 5 M 11 6 l -6 6 6 6", fill = false, stroke = true)) }
    val ArrowRight: ImageVector by lazy { build("arrow-right", P("M 5 12 h 14 M 13 6 l 6 6 -6 6", fill = false, stroke = true)) }
    val ArrowUp: ImageVector by lazy { build("arrow-up", P("M 12 19 V 5 M 6 11 l 6 -6 6 6", fill = false, stroke = true)) }
    val ArrowDown: ImageVector by lazy { build("arrow-down", P("M 12 5 v 14 M 6 13 l 6 6 6 -6", fill = false, stroke = true)) }
    val ArrowUpRight: ImageVector by lazy { build("arrow-up-right", P("M 7 17 L 17 7 M 8.5 7 H 17 v 8.5", fill = false, stroke = true)) }
    val ArrowDownLeft: ImageVector by lazy { build("arrow-down-left", P("M 17 7 L 7 17 M 15.5 17 H 7 V 8.5", fill = false, stroke = true)) }
    val ChevronLeft: ImageVector by lazy { build("chevron-left", P("M 15 5 l -7 7 7 7", fill = false, stroke = true)) }
    val ChevronRight: ImageVector by lazy { build("chevron-right", P("M 9 5 l 7 7 -7 7", fill = false, stroke = true)) }
    val ChevronUp: ImageVector by lazy { build("chevron-up", P("M 5 15 l 7 -7 7 7", fill = false, stroke = true)) }
    val ChevronDown: ImageVector by lazy { build("chevron-down", P("M 5 9 l 7 7 7 -7", fill = false, stroke = true)) }
    val ChevronsUpDown: ImageVector by lazy { build("chevrons-up-down", P("M 8 9 l 4 -4 4 4 M 8 15 l 4 4 4 -4", fill = false, stroke = true)) }
    val CornerUpLeft: ImageVector by lazy { build("corner-up-left", P("M 9 14 L 4 9 l 5 -5 M 4 9 h 10 a 6 6 0 0 1 6 6 v 5", fill = false, stroke = true)) }
    val CornerDownRight: ImageVector by lazy { build("corner-down-right", P("M 15 10 l 5 5 -5 5 M 20 15 H 10 a 6 6 0 0 1 -6 -6 V 4", fill = false, stroke = true)) }
    val Undo: ImageVector by lazy { build("undo", P("M 9 14 L 4 9 l 5 -5 M 4 9 h 11 a 5 5 0 0 1 0 10 h -3", fill = false, stroke = true)) }
    val Redo: ImageVector by lazy { build("redo", P("M 15 14 l 5 -5 -5 -5 M 20 9 H 9 a 5 5 0 0 0 0 10 h 3", fill = false, stroke = true)) }
    val Expand: ImageVector by lazy { build("expand", P("M 14 4 h 6 v 6 M 20 4 l -6.5 6.5 M 10 20 H 4 v -6 M 4 20 l 6.5 -6.5", fill = false, stroke = true)) }
    val Collapse: ImageVector by lazy { build("collapse", P("M 20 4 l -6 6 M 14 5 v 5 h 5 M 4 20 l 6 -6 M 10 19 v -5 H 5", fill = false, stroke = true)) }
    val ExternalLink: ImageVector by lazy { build("external-link", P("M 14 4 h 6 v 6 M 20 4 l -9 9", fill = false, stroke = true), P("M 18 14 v 4 a 2.5 2.5 0 0 1 -2.5 2.5 h -9 A 2.5 2.5 0 0 1 4 18 V 8.5 A 2.5 2.5 0 0 1 6.5 6 H 10", fill = false, stroke = true)) }
    val Plus: ImageVector by lazy { build("plus", P("M 12 4 v 16 M 4 12 h 16", fill = false, stroke = true)) }
    val Minus: ImageVector by lazy { build("minus", P("M 4 12 h 16", fill = false, stroke = true)) }
    val Close: ImageVector by lazy { build("close", P("M 6 6 l 12 12 M 18 6 L 6 18", fill = false, stroke = true)) }
    val Check: ImageVector by lazy { build("check", P("M 4 12.5 l 5 5 L 20 6.5", fill = false, stroke = true)) }
    val PlusCircle: ImageVector by lazy { build("plus-circle", P("M 3 12 a 9 9 0 1 0 18 0 a 9 9 0 1 0 -18 0 Z", fill = false, stroke = true), P("M 12 8 v 8 M 8 12 h 8", fill = false, stroke = true)) }
    val MinusCircle: ImageVector by lazy { build("minus-circle", P("M 3 12 a 9 9 0 1 0 18 0 a 9 9 0 1 0 -18 0 Z", fill = false, stroke = true), P("M 8 12 h 8", fill = false, stroke = true)) }
    val Edit: ImageVector by lazy { build("edit", P("M 4 20 h 4 L 19 9 a 2.83 2.83 0 0 0 -4 -4 L 4 16 v 4 z", fill = false, stroke = true), P("M 13.5 6.5 l 4 4", fill = false, stroke = true)) }
    val Trash: ImageVector by lazy { build("trash", P("M 4 6 h 16 M 9 6 V 4.5 A 1.5 1.5 0 0 1 10.5 3 h 3 A 1.5 1.5 0 0 1 15 4.5 V 6", fill = false, stroke = true), P("M 6 6 l .9 12.6 A 2.5 2.5 0 0 0 9.4 21 h 5.2 a 2.5 2.5 0 0 0 2.5 -2.4 L 18 6", fill = false, stroke = true), P("M 10 11 v 5 M 14 11 v 5", fill = false, stroke = true)) }
    val Copy: ImageVector by lazy { build("copy", P("M 11 8 H 17.5 A 3 3 0 0 1 20.5 11 V 17.5 A 3 3 0 0 1 17.5 20.5 H 11 A 3 3 0 0 1 8 17.5 V 11 A 3 3 0 0 1 11 8 Z", fill = false, stroke = true), P("M 16 8 V 6.5 A 3 3 0 0 0 13 3.5 H 6.5 a 3 3 0 0 0 -3 3 V 13 a 3 3 0 0 0 3 3 H 8", fill = false, stroke = true)) }
    val Paste: ImageVector by lazy { build("paste", P("M 9 4.5 H 7.5 a 2.5 2.5 0 0 0 -2.5 2.5 v 11.5 A 2.5 2.5 0 0 0 7.5 21 h 9 a 2.5 2.5 0 0 0 2.5 -2.5 V 7 a 2.5 2.5 0 0 0 -2.5 -2.5 H 15", fill = false, stroke = true), P("M 10.5 2.5 H 13.5 A 1.5 1.5 0 0 1 15 4 V 5 A 1.5 1.5 0 0 1 13.5 6.5 H 10.5 A 1.5 1.5 0 0 1 9 5 V 4 A 1.5 1.5 0 0 1 10.5 2.5 Z", fill = false, stroke = true), P("M 9 12 h 6 M 9 16 h 4", fill = false, stroke = true)) }
    val Share: ImageVector by lazy { build("share", P("M 12 4 v 11 M 8 8 l 4 -4 4 4", fill = false, stroke = true), P("M 8 11 H 7.5 A 2.5 2.5 0 0 0 5 13.5 v 4 A 2.5 2.5 0 0 0 7.5 20 h 9 a 2.5 2.5 0 0 0 2.5 -2.5 v -4 a 2.5 2.5 0 0 0 -2.5 -2.5 H 16", fill = false, stroke = true)) }
    val Download: ImageVector by lazy { build("download", P("M 12 4 v 11 M 7 10 l 5 5 5 -5 M 5 20 h 14", fill = false, stroke = true)) }
    val Upload: ImageVector by lazy { build("upload", P("M 12 15 V 4 M 7 9 l 5 -5 5 5 M 5 20 h 14", fill = false, stroke = true)) }
    val Refresh: ImageVector by lazy { build("refresh", P("M 20 12 a 8 8 0 1 1 -2.34 -5.66", fill = false, stroke = true), P("M 20 4 v 4 h -4", fill = false, stroke = true)) }
    val Search: ImageVector by lazy { build("search", P("M 4 10.5 a 6.5 6.5 0 1 0 13 0 a 6.5 6.5 0 1 0 -13 0 Z", fill = false, stroke = true), P("M 15.5 15.5 L 20 20", fill = false, stroke = true)) }
    val ZoomIn: ImageVector by lazy { build("zoom-in", P("M 4 10.5 a 6.5 6.5 0 1 0 13 0 a 6.5 6.5 0 1 0 -13 0 Z", fill = false, stroke = true), P("M 15.5 15.5 L 20 20 M 10.5 8 v 5 M 8 10.5 h 5", fill = false, stroke = true)) }
    val ZoomOut: ImageVector by lazy { build("zoom-out", P("M 4 10.5 a 6.5 6.5 0 1 0 13 0 a 6.5 6.5 0 1 0 -13 0 Z", fill = false, stroke = true), P("M 15.5 15.5 L 20 20 M 8 10.5 h 5", fill = false, stroke = true)) }
    val Filter: ImageVector by lazy { build("filter", P("M 4 6 h 16 M 7 12 h 10 M 10 18 h 4", fill = false, stroke = true)) }
    val Sliders: ImageVector by lazy { build("sliders", P("M 4 7 h 9 M 18 7 h 2 M 4 17 h 2 M 11 17 h 9", fill = false, stroke = true), P("M 13 7 a 2.5 2.5 0 1 0 5 0 a 2.5 2.5 0 1 0 -5 0 Z", fill = false, stroke = true), P("M 6 17 a 2.5 2.5 0 1 0 5 0 a 2.5 2.5 0 1 0 -5 0 Z", fill = false, stroke = true)) }
    val Sort: ImageVector by lazy { build("sort", P("M 7 4 v 16 M 3.5 16.5 L 7 20 l 3.5 -3.5 M 17 20 V 4 M 13.5 7.5 L 17 4 l 3.5 3.5", fill = false, stroke = true)) }
    val More: ImageVector by lazy { build("more", P("M 3.6 12 a 1.4 1.4 0 1 0 2.8 0 a 1.4 1.4 0 1 0 -2.8 0 Z", fill = true, stroke = false), P("M 10.6 12 a 1.4 1.4 0 1 0 2.8 0 a 1.4 1.4 0 1 0 -2.8 0 Z", fill = true, stroke = false), P("M 17.6 12 a 1.4 1.4 0 1 0 2.8 0 a 1.4 1.4 0 1 0 -2.8 0 Z", fill = true, stroke = false)) }
    val MoreVertical: ImageVector by lazy { build("more-vertical", P("M 10.6 5 a 1.4 1.4 0 1 0 2.8 0 a 1.4 1.4 0 1 0 -2.8 0 Z", fill = true, stroke = false), P("M 10.6 12 a 1.4 1.4 0 1 0 2.8 0 a 1.4 1.4 0 1 0 -2.8 0 Z", fill = true, stroke = false), P("M 10.6 19 a 1.4 1.4 0 1 0 2.8 0 a 1.4 1.4 0 1 0 -2.8 0 Z", fill = true, stroke = false)) }
    val Link: ImageVector by lazy { build("link", P("M 10 14 a 4.5 4.5 0 0 0 6.36 0 l 3.18 -3.18 a 4.5 4.5 0 0 0 -6.36 -6.36 l -1.6 1.6", fill = false, stroke = true), P("M 14 10 a 4.5 4.5 0 0 0 -6.36 0 l -3.18 3.18 a 4.5 4.5 0 0 0 6.36 6.36 l 1.6 -1.6", fill = false, stroke = true)) }
    val Bookmark: ImageVector by lazy { build("bookmark", P("M 7 3 h 10 a 2 2 0 0 1 2 2 v 15.2 a .6 .6 0 0 1 -.95 .5 L 12 16.5 l -6.05 4.2 a .6 .6 0 0 1 -.95 -.5 V 5 a 2 2 0 0 1 2 -2 z", fill = false, stroke = true)) }
    val BookmarkFill: ImageVector by lazy { build("bookmark-fill", P("M 7 3 h 10 a 2 2 0 0 1 2 2 v 15.2 a .6 .6 0 0 1 -.95 .5 L 12 16.5 l -6.05 4.2 a .6 .6 0 0 1 -.95 -.5 V 5 a 2 2 0 0 1 2 -2 z", fill = true, stroke = false)) }
    val Flag: ImageVector by lazy { build("flag", P("M 5 21 V 4.5 M 5 4.5 h 11.2 a .8 .8 0 0 1 .7 1.15 L 15 9.5 l 1.9 3.85 a .8 .8 0 0 1 -.7 1.15 H 5", fill = false, stroke = true)) }
    val FlagFill: ImageVector by lazy { build("flag-fill", P("M 5 21 V 4.5", fill = false, stroke = true), P("M 5 4.5 h 11.2 a .8 .8 0 0 1 .7 1.15 L 15 9.5 l 1.9 3.85 a .8 .8 0 0 1 -.7 1.15 H 5 z", fill = true, stroke = false)) }
    val Tag: ImageVector by lazy { build("tag", P("M 3 12.2 V 5.5 A 2.5 2.5 0 0 1 5.5 3 h 6.7 a 2 2 0 0 1 1.4 .6 l 7.6 7.6 a 2 2 0 0 1 0 2.8 l -6.8 6.8 a 2 2 0 0 1 -2.8 0 l -7.6 -7.6 a 2 2 0 0 1 -.6 -1.4 z", fill = false, stroke = true), P("M 6.5 7.8 a 1.3 1.3 0 1 0 2.6 0 a 1.3 1.3 0 1 0 -2.6 0 Z", fill = true, stroke = false)) }
    val Eye: ImageVector by lazy { build("eye", P("M 2.5 12 S 6 5.5 12 5.5 21.5 12 21.5 12 18 18.5 12 18.5 2.5 12 2.5 12 z", fill = false, stroke = true), P("M 9 12 a 3 3 0 1 0 6 0 a 3 3 0 1 0 -6 0 Z", fill = false, stroke = true)) }
    val EyeOff: ImageVector by lazy { build("eye-off", P("M 3 3 l 18 18", fill = false, stroke = true), P("M 10.6 5.6 c .45 -.07 .92 -.1 1.4 -.1 6 0 9.5 6.5 9.5 6.5 a 17 17 0 0 1 -2.6 3.4 M 6.6 6.6 C 3.9 8.3 2.5 12 2.5 12 S 6 18.5 12 18.5 c 1.7 0 3.3 -.5 4.6 -1.3", fill = false, stroke = true), P("M 9.9 9.9 a 3 3 0 0 0 4.2 4.2", fill = false, stroke = true)) }
    val Print: ImageVector by lazy { build("print", P("M 7 9 V 4.5 A 1.5 1.5 0 0 1 8.5 3 h 7 A 1.5 1.5 0 0 1 17 4.5 V 9", fill = false, stroke = true), P("M 7 17 H 5.5 A 2.5 2.5 0 0 1 3 14.5 v -3 A 2.5 2.5 0 0 1 5.5 9 h 13 a 2.5 2.5 0 0 1 2.5 2.5 v 3 a 2.5 2.5 0 0 1 -2.5 2.5 H 17", fill = false, stroke = true), P("M 8.5 14 H 15.5 A 1.5 1.5 0 0 1 17 15.5 V 19.5 A 1.5 1.5 0 0 1 15.5 21 H 8.5 A 1.5 1.5 0 0 1 7 19.5 V 15.5 A 1.5 1.5 0 0 1 8.5 14 Z", fill = false, stroke = true)) }
    val Archive: ImageVector by lazy { build("archive", P("M 5 4 H 19 A 2 2 0 0 1 21 6 V 7 A 2 2 0 0 1 19 9 H 5 A 2 2 0 0 1 3 7 V 6 A 2 2 0 0 1 5 4 Z", fill = false, stroke = true), P("M 5 9 v 9 a 2.5 2.5 0 0 0 2.5 2.5 h 9 A 2.5 2.5 0 0 0 19 18 V 9 M 10 13 h 4", fill = false, stroke = true)) }
    val Send: ImageVector by lazy { build("send", P("M 20.5 3.5 L 10 14 M 20.5 3.5 l -6.2 16.8 a .6 .6 0 0 1 -1.1 .05 L 10 14 l -6.35 -3.2 a .6 .6 0 0 1 .05 -1.1 L 20.5 3.5 z", fill = false, stroke = true)) }
    val History: ImageVector by lazy { build("history", P("M 3.5 12 a 8.5 8.5 0 1 0 2.5 -6 M 3.5 4 v 4 h 4", fill = false, stroke = true), P("M 12 8 v 4 l 3 2", fill = false, stroke = true)) }
    val CheckSquare: ImageVector by lazy { build("check-square", P("M 7.5 3.5 H 16.5 A 4 4 0 0 1 20.5 7.5 V 16.5 A 4 4 0 0 1 16.5 20.5 H 7.5 A 4 4 0 0 1 3.5 16.5 V 7.5 A 4 4 0 0 1 7.5 3.5 Z", fill = false, stroke = true), P("M 8 12.5 l 3 3 5 -6", fill = false, stroke = true)) }
    val Home: ImageVector by lazy { build("home", P("M 4 10.6 a 2 2 0 0 1 .74 -1.55 l 6 -4.9 a 2 2 0 0 1 2.52 0 l 6 4.9 a 2 2 0 0 1 .74 1.55 V 19 a 1.5 1.5 0 0 1 -1.5 1.5 H 15 v -5.5 a 1 1 0 0 0 -1 -1 h -4 a 1 1 0 0 0 -1 1 v 5.5 H 5.5 A 1.5 1.5 0 0 1 4 19 v -8.4 z", fill = false, stroke = true)) }
    val HomeFill: ImageVector by lazy { build("home-fill", P("M 4 10.6 a 2 2 0 0 1 .74 -1.55 l 6 -4.9 a 2 2 0 0 1 2.52 0 l 6 4.9 a 2 2 0 0 1 .74 1.55 V 19 a 1.5 1.5 0 0 1 -1.5 1.5 H 15 v -5.5 a 1 1 0 0 0 -1 -1 h -4 a 1 1 0 0 0 -1 1 v 5.5 H 5.5 A 1.5 1.5 0 0 1 4 19 v -8.4 z", fill = true, stroke = false)) }
    val Menu: ImageVector by lazy { build("menu", P("M 4 7 h 16 M 4 12 h 16 M 4 17 h 16", fill = false, stroke = true)) }
    val Settings: ImageVector by lazy { build("settings", P("M 9 12 a 3 3 0 1 0 6 0 a 3 3 0 1 0 -6 0 Z", fill = false, stroke = true), P("M 12 2.75 l 2 1.2 2.3 -.2 1.1 2 2 1.15 -.2 2.3 1.05 1.8 -1.05 1.8 .2 2.3 -2 1.15 -1.1 2 -2.3 -.2 -2 1.2 -2 -1.2 -2.3 .2 -1.1 -2 -2 -1.15 .2 -2.3 L 2.75 12 3.8 10.2 l -.2 -2.3 2 -1.15 1.1 -2 2.3 .2 2 -1.2 z", fill = false, stroke = true)) }
    val Grid: ImageVector by lazy { build("grid", P("M 6 3.5 H 8 A 2.5 2.5 0 0 1 10.5 6 V 8 A 2.5 2.5 0 0 1 8 10.5 H 6 A 2.5 2.5 0 0 1 3.5 8 V 6 A 2.5 2.5 0 0 1 6 3.5 Z", fill = false, stroke = true), P("M 16 3.5 H 18 A 2.5 2.5 0 0 1 20.5 6 V 8 A 2.5 2.5 0 0 1 18 10.5 H 16 A 2.5 2.5 0 0 1 13.5 8 V 6 A 2.5 2.5 0 0 1 16 3.5 Z", fill = false, stroke = true), P("M 6 13.5 H 8 A 2.5 2.5 0 0 1 10.5 16 V 18 A 2.5 2.5 0 0 1 8 20.5 H 6 A 2.5 2.5 0 0 1 3.5 18 V 16 A 2.5 2.5 0 0 1 6 13.5 Z", fill = false, stroke = true), P("M 16 13.5 H 18 A 2.5 2.5 0 0 1 20.5 16 V 18 A 2.5 2.5 0 0 1 18 20.5 H 16 A 2.5 2.5 0 0 1 13.5 18 V 16 A 2.5 2.5 0 0 1 16 13.5 Z", fill = false, stroke = true)) }
    val List: ImageVector by lazy { build("list", P("M 9 6 h 11 M 9 12 h 11 M 9 18 h 11", fill = false, stroke = true), P("M 3.3 6 a 1.2 1.2 0 1 0 2.4 0 a 1.2 1.2 0 1 0 -2.4 0 Z", fill = true, stroke = false), P("M 3.3 12 a 1.2 1.2 0 1 0 2.4 0 a 1.2 1.2 0 1 0 -2.4 0 Z", fill = true, stroke = false), P("M 3.3 18 a 1.2 1.2 0 1 0 2.4 0 a 1.2 1.2 0 1 0 -2.4 0 Z", fill = true, stroke = false)) }
    val Sidebar: ImageVector by lazy { build("sidebar", P("M 6.5 4 H 17.5 A 3.5 3.5 0 0 1 21 7.5 V 16.5 A 3.5 3.5 0 0 1 17.5 20 H 6.5 A 3.5 3.5 0 0 1 3 16.5 V 7.5 A 3.5 3.5 0 0 1 6.5 4 Z", fill = false, stroke = true), P("M 9.5 4 v 16", fill = false, stroke = true)) }
    val Maximize: ImageVector by lazy { build("maximize", P("M 4 9 V 6.5 A 2.5 2.5 0 0 1 6.5 4 H 9 M 15 4 h 2.5 A 2.5 2.5 0 0 1 20 6.5 V 9 M 20 15 v 2.5 a 2.5 2.5 0 0 1 -2.5 2.5 H 15 M 9 20 H 6.5 A 2.5 2.5 0 0 1 4 17.5 V 15", fill = false, stroke = true)) }
    val Minimize: ImageVector by lazy { build("minimize", P("M 9 4 v 2.5 A 2.5 2.5 0 0 1 6.5 9 H 4 M 20 9 h -2.5 A 2.5 2.5 0 0 1 15 6.5 V 4 M 15 20 v -2.5 a 2.5 2.5 0 0 1 2.5 -2.5 H 20 M 4 15 h 2.5 A 2.5 2.5 0 0 1 9 17.5 V 20", fill = false, stroke = true)) }
    val Compass: ImageVector by lazy { build("compass", P("M 3 12 a 9 9 0 1 0 18 0 a 9 9 0 1 0 -18 0 Z", fill = false, stroke = true), P("M 15.5 8.5 l -1.8 4.6 a 1 1 0 0 1 -.6 .6 l -4.6 1.8 1.8 -4.6 a 1 1 0 0 1 .6 -.6 l 4.6 -1.8 z", fill = false, stroke = true)) }
    val Globe: ImageVector by lazy { build("globe", P("M 3 12 a 9 9 0 1 0 18 0 a 9 9 0 1 0 -18 0 Z", fill = false, stroke = true), P("M 3 12 h 18 M 12 3 c 2.4 2.5 3.6 5.5 3.6 9 s -1.2 6.5 -3.6 9 c -2.4 -2.5 -3.6 -5.5 -3.6 -9 S 9.6 5.5 12 3 z", fill = false, stroke = true)) }
    val Location: ImageVector by lazy { build("location", P("M 12 21 s -7 -6.1 -7 -11.5 A 7 7 0 0 1 19 9.5 C 19 14.9 12 21 12 21 z", fill = false, stroke = true), P("M 9.5 9.5 a 2.5 2.5 0 1 0 5 0 a 2.5 2.5 0 1 0 -5 0 Z", fill = false, stroke = true)) }
    val LocationFill: ImageVector by lazy { build("location-fill", P("M 12 21 s -7 -6.1 -7 -11.5 A 7 7 0 0 1 19 9.5 C 19 14.9 12 21 12 21 z M 12 7 a 2.5 2.5 0 1 0 0 5 2.5 2.5 0 0 0 0 -5 z", fill = true, stroke = false, evenOdd = true)) }
    val Calendar: ImageVector by lazy { build("calendar", P("M 6.5 5 H 17.5 A 3.5 3.5 0 0 1 21 8.5 V 17.5 A 3.5 3.5 0 0 1 17.5 21 H 6.5 A 3.5 3.5 0 0 1 3 17.5 V 8.5 A 3.5 3.5 0 0 1 6.5 5 Z", fill = false, stroke = true), P("M 3 10 h 18 M 8 3 v 4 M 16 3 v 4", fill = false, stroke = true)) }
    val Clock: ImageVector by lazy { build("clock", P("M 3 12 a 9 9 0 1 0 18 0 a 9 9 0 1 0 -18 0 Z", fill = false, stroke = true), P("M 12 7 v 5 l 3 2", fill = false, stroke = true)) }
    val Success: ImageVector by lazy { build("success", P("M 3 12 a 9 9 0 1 0 18 0 a 9 9 0 1 0 -18 0 Z", fill = false, stroke = true), P("M 8 12.25 l 2.75 2.75 L 16 9.5", fill = false, stroke = true)) }
    val SuccessFill: ImageVector by lazy { build("success-fill", P("M 12 3 a 9 9 0 1 0 0 18 9 9 0 0 0 0 -18 z m 4.53 7.03 a .75 .75 0 0 0 -1.06 -1.06 l -4.72 4.72 -2.22 -2.22 a .75 .75 0 0 0 -1.06 1.06 l 2.75 2.75 a .75 .75 0 0 0 1.06 0 l 5.25 -5.25 z", fill = true, stroke = false, evenOdd = true)) }
    val Warning: ImageVector by lazy { build("warning", P("M 10.27 4.5 a 2 2 0 0 1 3.46 0 l 7 12.1 A 2 2 0 0 1 19 19.6 H 5 a 2 2 0 0 1 -1.73 -3 l 7 -12.1 z", fill = false, stroke = true), P("M 12 9.5 v 4", fill = false, stroke = true), P("M 11 16.5 a 1 1 0 1 0 2 0 a 1 1 0 1 0 -2 0 Z", fill = true, stroke = false)) }
    val Error: ImageVector by lazy { build("error", P("M 3 12 a 9 9 0 1 0 18 0 a 9 9 0 1 0 -18 0 Z", fill = false, stroke = true), P("M 9 9 l 6 6 M 15 9 l -6 6", fill = false, stroke = true)) }
    val Info: ImageVector by lazy { build("info", P("M 3 12 a 9 9 0 1 0 18 0 a 9 9 0 1 0 -18 0 Z", fill = false, stroke = true), P("M 12 11 v 5", fill = false, stroke = true), P("M 11 7.75 a 1 1 0 1 0 2 0 a 1 1 0 1 0 -2 0 Z", fill = true, stroke = false)) }
    val Help: ImageVector by lazy { build("help", P("M 3 12 a 9 9 0 1 0 18 0 a 9 9 0 1 0 -18 0 Z", fill = false, stroke = true), P("M 9.6 9.4 a 2.5 2.5 0 1 1 3.4 2.4 c -.6 .3 -1 .8 -1 1.5 v .4", fill = false, stroke = true), P("M 11 16.75 a 1 1 0 1 0 2 0 a 1 1 0 1 0 -2 0 Z", fill = true, stroke = false)) }
    val Loader: ImageVector by lazy { build("loader", P("M 12 3 a 9 9 0 1 0 9 9", fill = false, stroke = true)) }
    val Bolt: ImageVector by lazy { build("bolt", P("M 13 2.5 L 4.8 13 a .6 .6 0 0 0 .5 1 H 12 l -1 7.5 L 19.2 11 a .6 .6 0 0 0 -.5 -1 H 12 l 1 -7.5 z", fill = false, stroke = true)) }
    val File: ImageVector by lazy { build("file", P("M 7 2.5 h 6.5 L 19 8 v 11.5 a 2 2 0 0 1 -2 2 H 7 a 2 2 0 0 1 -2 -2 v -15 a 2 2 0 0 1 2 -2 z", fill = false, stroke = true), P("M 13.5 2.5 V 6 a 2 2 0 0 0 2 2 H 19", fill = false, stroke = true)) }
    val FileText: ImageVector by lazy { build("file-text", P("M 7 2.5 h 6.5 L 19 8 v 11.5 a 2 2 0 0 1 -2 2 H 7 a 2 2 0 0 1 -2 -2 v -15 a 2 2 0 0 1 2 -2 z", fill = false, stroke = true), P("M 13.5 2.5 V 6 a 2 2 0 0 0 2 2 H 19", fill = false, stroke = true), P("M 8.5 13 h 7 M 8.5 17 h 4.5", fill = false, stroke = true)) }
    val FilePlus: ImageVector by lazy { build("file-plus", P("M 7 2.5 h 6.5 L 19 8 v 11.5 a 2 2 0 0 1 -2 2 H 7 a 2 2 0 0 1 -2 -2 v -15 a 2 2 0 0 1 2 -2 z", fill = false, stroke = true), P("M 13.5 2.5 V 6 a 2 2 0 0 0 2 2 H 19", fill = false, stroke = true), P("M 12 11.5 v 6 M 9 14.5 h 6", fill = false, stroke = true)) }
    val Folder: ImageVector by lazy { build("folder", P("M 2.5 7 a 2.5 2.5 0 0 1 2.5 -2.5 h 3.6 a 2 2 0 0 1 1.5 .7 l 1.4 1.6 h 7.5 A 2.5 2.5 0 0 1 21.5 9.3 V 17 a 2.5 2.5 0 0 1 -2.5 2.5 H 5 A 2.5 2.5 0 0 1 2.5 17 V 7 z", fill = false, stroke = true)) }
    val FolderFill: ImageVector by lazy { build("folder-fill", P("M 2.5 7 a 2.5 2.5 0 0 1 2.5 -2.5 h 3.6 a 2 2 0 0 1 1.5 .7 l 1.4 1.6 h 7.5 A 2.5 2.5 0 0 1 21.5 9.3 V 17 a 2.5 2.5 0 0 1 -2.5 2.5 H 5 A 2.5 2.5 0 0 1 2.5 17 V 7 z", fill = true, stroke = false)) }
    val FolderOpen: ImageVector by lazy { build("folder-open", P("M 3 17.5 V 7 a 2.5 2.5 0 0 1 2.5 -2.5 h 3.1 a 2 2 0 0 1 1.5 .7 l 1.4 1.6 h 6 A 2.5 2.5 0 0 1 20 9.3 v .7", fill = false, stroke = true), P("M 3 17.5 l 2.3 -6 a 2.5 2.5 0 0 1 2.3 -1.5 H 20 a 1.2 1.2 0 0 1 1.1 1.6 L 19 17.8 a 2.5 2.5 0 0 1 -2.3 1.7 H 4.5 A 1.5 1.5 0 0 1 3 17.5 z", fill = false, stroke = true)) }
    val Image: ImageVector by lazy { build("image", P("M 6 4 H 18 A 3.5 3.5 0 0 1 21.5 7.5 V 16.5 A 3.5 3.5 0 0 1 18 20 H 6 A 3.5 3.5 0 0 1 2.5 16.5 V 7.5 A 3.5 3.5 0 0 1 6 4 Z", fill = false, stroke = true), P("M 7 9.5 a 1.5 1.5 0 1 0 3 0 a 1.5 1.5 0 1 0 -3 0 Z", fill = false, stroke = true), P("M 21.5 16 l -4.6 -4.6 a 1.5 1.5 0 0 0 -2.1 0 L 7 19.5", fill = false, stroke = true)) }
    val Video: ImageVector by lazy { build("video", P("M 6 6 H 12 A 3.5 3.5 0 0 1 15.5 9.5 V 14.5 A 3.5 3.5 0 0 1 12 18 H 6 A 3.5 3.5 0 0 1 2.5 14.5 V 9.5 A 3.5 3.5 0 0 1 6 6 Z", fill = false, stroke = true), P("M 15.5 10.5 l 5.1 -2.9 a .6 .6 0 0 1 .9 .5 v 7.8 a .6 .6 0 0 1 -.9 .5 l -5.1 -2.9", fill = false, stroke = true)) }
    val Paperclip: ImageVector by lazy { build("paperclip", P("M 20 11.5 l -8.2 8.2 a 5 5 0 0 1 -7.1 -7.1 l 8.5 -8.5 a 3.3 3.3 0 0 1 4.7 4.7 l -8.5 8.5 a 1.7 1.7 0 0 1 -2.4 -2.4 l 7.8 -7.8", fill = false, stroke = true)) }
    val Cloud: ImageVector by lazy { build("cloud", P("M 7 19 a 5 5 0 0 1 -.8 -9.94 A 6.5 6.5 0 0 1 18.6 9 a 4.5 4.5 0 0 1 -1.1 10 H 7 z", fill = false, stroke = true)) }
    val Book: ImageVector by lazy { build("book", P("M 5 19 V 5.5 A 2.5 2.5 0 0 1 7.5 3 H 19 v 14 H 7 a 2 2 0 0 0 -2 2 z m 0 0 a 2 2 0 0 0 2 2 h 12", fill = false, stroke = true)) }
    val Message: ImageVector by lazy { build("message", P("M 4 7 a 3 3 0 0 1 3 -3 h 10 a 3 3 0 0 1 3 3 v 7 a 3 3 0 0 1 -3 3 H 9.5 l -4.3 3.4 A .75 .75 0 0 1 4 19.8 V 7 z", fill = false, stroke = true)) }
    val MessageFill: ImageVector by lazy { build("message-fill", P("M 4 7 a 3 3 0 0 1 3 -3 h 10 a 3 3 0 0 1 3 3 v 7 a 3 3 0 0 1 -3 3 H 9.5 l -4.3 3.4 A .75 .75 0 0 1 4 19.8 V 7 z", fill = true, stroke = false)) }
    val MessageCircle: ImageVector by lazy { build("message-circle", P("M 20.5 11.5 a 8.5 8.5 0 0 1 -12.4 7.55 L 3.8 20.2 a .4 .4 0 0 1 -.5 -.5 l 1.2 -4.1 A 8.5 8.5 0 1 1 20.5 11.5 z", fill = false, stroke = true)) }
    val Phone: ImageVector by lazy { build("phone", P("M 5.5 3.5 h 2.3 a 1 1 0 0 1 .95 .68 l 1.1 3.3 a 1 1 0 0 1 -.3 1.08 L 8 9.8 a 11 11 0 0 0 6.2 6.2 l 1.24 -1.55 a 1 1 0 0 1 1.08 -.3 l 3.3 1.1 a 1 1 0 0 1 .68 .95 v 2.3 a 2 2 0 0 1 -2 2 A 16.5 16.5 0 0 1 3.5 5.5 a 2 2 0 0 1 2 -2 z", fill = false, stroke = true)) }
    val Mail: ImageVector by lazy { build("mail", P("M 6 4.5 H 18 A 3.5 3.5 0 0 1 21.5 8 V 16 A 3.5 3.5 0 0 1 18 19.5 H 6 A 3.5 3.5 0 0 1 2.5 16 V 8 A 3.5 3.5 0 0 1 6 4.5 Z", fill = false, stroke = true), P("M 3 7.5 l 7.9 5.3 a 2 2 0 0 0 2.2 0 L 21 7.5", fill = false, stroke = true)) }
    val Bell: ImageVector by lazy { build("bell", P("M 6 16 v -5 a 6 6 0 1 1 12 0 v 5 l 1.5 2 h -15 L 6 16 z", fill = false, stroke = true), P("M 10 20.5 a 2.2 2.2 0 0 0 4 0", fill = false, stroke = true)) }
    val BellFill: ImageVector by lazy { build("bell-fill", P("M 6 16 v -5 a 6 6 0 1 1 12 0 v 5 l 1.5 2 h -15 L 6 16 z", fill = true, stroke = false), P("M 10 20.5 a 2.2 2.2 0 0 0 4 0", fill = false, stroke = true)) }
    val BellOff: ImageVector by lazy { build("bell-off", P("M 3 3 l 18 18", fill = false, stroke = true), P("M 6.6 7.9 A 6 6 0 0 0 6 11 v 5 l -1.5 2 H 17 M 9.4 5.6 A 6 6 0 0 1 18 11 v 4", fill = false, stroke = true), P("M 10 20.5 a 2.2 2.2 0 0 0 4 0", fill = false, stroke = true)) }
    val AtSign: ImageVector by lazy { build("at-sign", P("M 8 12 a 4 4 0 1 0 8 0 a 4 4 0 1 0 -8 0 Z", fill = false, stroke = true), P("M 16 8 v 5 a 2.5 2.5 0 0 0 5 0 v -1 a 9 9 0 1 0 -3.5 7.1", fill = false, stroke = true)) }
    val Inbox: ImageVector by lazy { build("inbox", P("M 3 13 h 4.6 a 1 1 0 0 1 .9 .55 L 9.2 15 a 1 1 0 0 0 .9 .55 h 3.8 a 1 1 0 0 0 .9 -.55 l .7 -1.45 a 1 1 0 0 1 .9 -.55 H 21", fill = false, stroke = true), P("M 5.6 5.6 A 2.5 2.5 0 0 1 7.9 4 h 8.2 a 2.5 2.5 0 0 1 2.3 1.6 L 21 13 v 4.5 a 2.5 2.5 0 0 1 -2.5 2.5 h -13 A 2.5 2.5 0 0 1 3 17.5 V 13 l 2.6 -7.4 z", fill = false, stroke = true)) }
    val Play: ImageVector by lazy { build("play", P("M 7 5.2 v 13.6 a 1 1 0 0 0 1.5 .86 l 11 -6.8 a 1 1 0 0 0 0 -1.72 l -11 -6.8 A 1 1 0 0 0 7 5.2 z", fill = false, stroke = true)) }
    val PlayFill: ImageVector by lazy { build("play-fill", P("M 7 5.2 v 13.6 a 1 1 0 0 0 1.5 .86 l 11 -6.8 a 1 1 0 0 0 0 -1.72 l -11 -6.8 A 1 1 0 0 0 7 5.2 z", fill = true, stroke = false)) }
    val Pause: ImageVector by lazy { build("pause", P("M 7.5 4 H 8.5 A 1.5 1.5 0 0 1 10 5.5 V 18.5 A 1.5 1.5 0 0 1 8.5 20 H 7.5 A 1.5 1.5 0 0 1 6 18.5 V 5.5 A 1.5 1.5 0 0 1 7.5 4 Z", fill = false, stroke = true), P("M 15.5 4 H 16.5 A 1.5 1.5 0 0 1 18 5.5 V 18.5 A 1.5 1.5 0 0 1 16.5 20 H 15.5 A 1.5 1.5 0 0 1 14 18.5 V 5.5 A 1.5 1.5 0 0 1 15.5 4 Z", fill = false, stroke = true)) }
    val PauseFill: ImageVector by lazy { build("pause-fill", P("M 7.5 4 H 8.5 A 1.5 1.5 0 0 1 10 5.5 V 18.5 A 1.5 1.5 0 0 1 8.5 20 H 7.5 A 1.5 1.5 0 0 1 6 18.5 V 5.5 A 1.5 1.5 0 0 1 7.5 4 Z", fill = true, stroke = false), P("M 15.5 4 H 16.5 A 1.5 1.5 0 0 1 18 5.5 V 18.5 A 1.5 1.5 0 0 1 16.5 20 H 15.5 A 1.5 1.5 0 0 1 14 18.5 V 5.5 A 1.5 1.5 0 0 1 15.5 4 Z", fill = true, stroke = false)) }
    val Stop: ImageVector by lazy { build("stop", P("M 8.5 5 H 15.5 A 3.5 3.5 0 0 1 19 8.5 V 15.5 A 3.5 3.5 0 0 1 15.5 19 H 8.5 A 3.5 3.5 0 0 1 5 15.5 V 8.5 A 3.5 3.5 0 0 1 8.5 5 Z", fill = false, stroke = true)) }
    val StopFill: ImageVector by lazy { build("stop-fill", P("M 8.5 5 H 15.5 A 3.5 3.5 0 0 1 19 8.5 V 15.5 A 3.5 3.5 0 0 1 15.5 19 H 8.5 A 3.5 3.5 0 0 1 5 15.5 V 8.5 A 3.5 3.5 0 0 1 8.5 5 Z", fill = true, stroke = false)) }
    val SkipBack: ImageVector by lazy { build("skip-back", P("M 19 6.1 v 11.8 a .8 .8 0 0 1 -1.25 .66 L 9.2 12.66 a .8 .8 0 0 1 0 -1.32 l 8.55 -5.9 A .8 .8 0 0 1 19 6.1 z M 5 5 v 14", fill = false, stroke = true)) }
    val SkipForward: ImageVector by lazy { build("skip-forward", P("M 5 6.1 v 11.8 a .8 .8 0 0 0 1.25 .66 l 8.55 -5.9 a .8 .8 0 0 0 0 -1.32 L 6.25 5.44 A .8 .8 0 0 0 5 6.1 z M 19 5 v 14", fill = false, stroke = true)) }
    val Volume: ImageVector by lazy { build("volume", P("M 4 9.5 h 2.5 L 11 5.5 v 13 l -4.5 -4 H 4 a 1 1 0 0 1 -1 -1 v -3 a 1 1 0 0 1 1 -1 z", fill = false, stroke = true), P("M 15 9 a 4 4 0 0 1 0 6 M 17.8 6.3 a 8 8 0 0 1 0 11.4", fill = false, stroke = true)) }
    val Mute: ImageVector by lazy { build("mute", P("M 4 9.5 h 2.5 L 11 5.5 v 13 l -4.5 -4 H 4 a 1 1 0 0 1 -1 -1 v -3 a 1 1 0 0 1 1 -1 z", fill = false, stroke = true), P("M 15.5 9.5 l 5 5 M 20.5 9.5 l -5 5", fill = false, stroke = true)) }
    val Camera: ImageVector by lazy { build("camera", P("M 3 8.5 A 2.5 2.5 0 0 1 5.5 6 h 1.8 l 1.2 -1.6 A 1 1 0 0 1 9.3 4 h 5.4 a 1 1 0 0 1 .8 .4 L 16.7 6 h 1.8 A 2.5 2.5 0 0 1 21 8.5 v 9 a 2.5 2.5 0 0 1 -2.5 2.5 h -13 A 2.5 2.5 0 0 1 3 17.5 v -9 z", fill = false, stroke = true), P("M 8.5 13 a 3.5 3.5 0 1 0 7 0 a 3.5 3.5 0 1 0 -7 0 Z", fill = false, stroke = true)) }
    val Mic: ImageVector by lazy { build("mic", P("M 12 2.5 H 12 A 3 3 0 0 1 15 5.5 V 10.5 A 3 3 0 0 1 12 13.5 H 12 A 3 3 0 0 1 9 10.5 V 5.5 A 3 3 0 0 1 12 2.5 Z", fill = false, stroke = true), P("M 5.5 11 a 6.5 6.5 0 0 0 13 0 M 12 17.5 V 21", fill = false, stroke = true)) }
    val MicOff: ImageVector by lazy { build("mic-off", P("M 12 2.5 H 12 A 3 3 0 0 1 15 5.5 V 10.5 A 3 3 0 0 1 12 13.5 H 12 A 3 3 0 0 1 9 10.5 V 5.5 A 3 3 0 0 1 12 2.5 Z", fill = false, stroke = true), P("M 5.5 11 a 6.5 6.5 0 0 0 13 0 M 12 17.5 V 21", fill = false, stroke = true), P("M 3 3 l 18 18", fill = false, stroke = true)) }
    val Music: ImageVector by lazy { build("music", P("M 9 18 V 5.3 a 1 1 0 0 1 .8 -1 l 9 -1.7 a 1 1 0 0 1 1.2 1 V 16", fill = false, stroke = true), P("M 4 18 a 2.5 2.5 0 1 0 5 0 a 2.5 2.5 0 1 0 -5 0 Z", fill = false, stroke = true), P("M 15 16 a 2.5 2.5 0 1 0 5 0 a 2.5 2.5 0 1 0 -5 0 Z", fill = false, stroke = true)) }
    val Headphones: ImageVector by lazy { build("headphones", P("M 3.5 18 v -5 a 8.5 8.5 0 0 1 17 0 v 5", fill = false, stroke = true), P("M 5.5 13.5 H 6 A 2 2 0 0 1 8 15.5 V 18.5 A 2 2 0 0 1 6 20.5 H 5.5 A 2 2 0 0 1 3.5 18.5 V 15.5 A 2 2 0 0 1 5.5 13.5 Z", fill = false, stroke = true), P("M 18 13.5 H 18.5 A 2 2 0 0 1 20.5 15.5 V 18.5 A 2 2 0 0 1 18.5 20.5 H 18 A 2 2 0 0 1 16 18.5 V 15.5 A 2 2 0 0 1 18 13.5 Z", fill = false, stroke = true)) }
    val Shuffle: ImageVector by lazy { build("shuffle", P("M 16 4 h 4 v 4 M 4 20 L 20 4 M 20 16 v 4 h -4 M 15 15 l 5 5 M 4 4 l 5 5", fill = false, stroke = true)) }
    val Repeat: ImageVector by lazy { build("repeat", P("M 17 2.5 l 3 3 -3 3 M 4 11 v -1 A 4.5 4.5 0 0 1 8.5 5.5 H 20 M 7 21.5 l -3 -3 3 -3 M 20 13 v 1 a 4.5 4.5 0 0 1 -4.5 4.5 H 4", fill = false, stroke = true)) }
    val User: ImageVector by lazy { build("user", P("M 8 8 a 4 4 0 1 0 8 0 a 4 4 0 1 0 -8 0 Z", fill = false, stroke = true), P("M 4 20.5 c .8 -3.8 4 -6 8 -6 s 7.2 2.2 8 6", fill = false, stroke = true)) }
    val UserFill: ImageVector by lazy { build("user-fill", P("M 8 8 a 4 4 0 1 0 8 0 a 4 4 0 1 0 -8 0 Z", fill = true, stroke = false), P("M 4 20.5 c .8 -3.8 4 -6 8 -6 s 7.2 2.2 8 6 a .6 .6 0 0 1 -.6 .7 H 4.6 a .6 .6 0 0 1 -.6 -.7 z", fill = true, stroke = false)) }
    val Users: ImageVector by lazy { build("users", P("M 5.5 8 a 3.5 3.5 0 1 0 7 0 a 3.5 3.5 0 1 0 -7 0 Z", fill = false, stroke = true), P("M 2.5 20 c .6 -3.4 3.2 -5.5 6.5 -5.5 s 5.9 2.1 6.5 5.5 M 15.5 4.6 a 3.5 3.5 0 0 1 0 6.8 M 18 14.8 c 1.9 .8 3.1 2.7 3.5 5.2", fill = false, stroke = true)) }
    val UserPlus: ImageVector by lazy { build("user-plus", P("M 6 8 a 4 4 0 1 0 8 0 a 4 4 0 1 0 -8 0 Z", fill = false, stroke = true), P("M 3 20.5 c .8 -3.8 3.6 -6 7 -6 1.4 0 2.8 .4 3.9 1.1 M 19 13 v 6 M 16 16 h 6", fill = false, stroke = true)) }
    val Login: ImageVector by lazy { build("login", P("M 14 4 h 3 a 3 3 0 0 1 3 3 v 10 a 3 3 0 0 1 -3 3 h -3 M 4 12 h 10 M 10 8 l 4 4 -4 4", fill = false, stroke = true)) }
    val Logout: ImageVector by lazy { build("logout", P("M 10 4 H 7 a 3 3 0 0 0 -3 3 v 10 a 3 3 0 0 0 3 3 h 3 M 10 12 h 10 M 16 8 l 4 4 -4 4", fill = false, stroke = true)) }
    val Lock: ImageVector by lazy { build("lock", P("M 7.5 10 H 16.5 A 3.5 3.5 0 0 1 20 13.5 V 17.5 A 3.5 3.5 0 0 1 16.5 21 H 7.5 A 3.5 3.5 0 0 1 4 17.5 V 13.5 A 3.5 3.5 0 0 1 7.5 10 Z", fill = false, stroke = true), P("M 8 10 V 7 a 4 4 0 1 1 8 0 v 3 M 12 14.5 v 2", fill = false, stroke = true)) }
    val Unlock: ImageVector by lazy { build("unlock", P("M 7.5 10 H 16.5 A 3.5 3.5 0 0 1 20 13.5 V 17.5 A 3.5 3.5 0 0 1 16.5 21 H 7.5 A 3.5 3.5 0 0 1 4 17.5 V 13.5 A 3.5 3.5 0 0 1 7.5 10 Z", fill = false, stroke = true), P("M 8 10 V 7 a 4 4 0 0 1 7.75 -1.4 M 12 14.5 v 2", fill = false, stroke = true)) }
    val Shield: ImageVector by lazy { build("shield", P("M 12 3 l 7.5 3 v 5.5 c 0 4.6 -3.1 8.3 -7.5 9.5 -4.4 -1.2 -7.5 -4.9 -7.5 -9.5 V 6 L 12 3 z", fill = false, stroke = true)) }
    val ShieldFill: ImageVector by lazy { build("shield-fill", P("M 12 3 l 7.5 3 v 5.5 c 0 4.6 -3.1 8.3 -7.5 9.5 -4.4 -1.2 -7.5 -4.9 -7.5 -9.5 V 6 L 12 3 z", fill = true, stroke = false)) }
    val ShieldCheck: ImageVector by lazy { build("shield-check", P("M 12 3 l 7.5 3 v 5.5 c 0 4.6 -3.1 8.3 -7.5 9.5 -4.4 -1.2 -7.5 -4.9 -7.5 -9.5 V 6 L 12 3 z", fill = false, stroke = true), P("M 9 12 l 2 2 4 -4", fill = false, stroke = true)) }
    val Key: ImageVector by lazy { build("key", P("M 3.5 15 a 4.5 4.5 0 1 0 9 0 a 4.5 4.5 0 1 0 -9 0 Z", fill = false, stroke = true), P("M 11.2 11.8 L 20 3 M 17 6 l 2.5 2.5 M 14.5 8.5 l 2 2", fill = false, stroke = true)) }
    val Cart: ImageVector by lazy { build("cart", P("M 2.5 3.5 h 2.2 l 2.3 11.2 a 1.5 1.5 0 0 0 1.47 1.2 h 9.06 a 1.5 1.5 0 0 0 1.46 -1.15 L 21 7 H 5.6", fill = false, stroke = true), P("M 7.75 20 a 1.25 1.25 0 1 0 2.5 0 a 1.25 1.25 0 1 0 -2.5 0 Z", fill = false, stroke = true), P("M 15.75 20 a 1.25 1.25 0 1 0 2.5 0 a 1.25 1.25 0 1 0 -2.5 0 Z", fill = false, stroke = true)) }
    val Bag: ImageVector by lazy { build("bag", P("M 5.4 8 h 13.2 a 1 1 0 0 1 1 1.1 l -.9 9.6 a 2.5 2.5 0 0 1 -2.5 2.3 H 7.8 a 2.5 2.5 0 0 1 -2.5 -2.3 l -.9 -9.6 a 1 1 0 0 1 1 -1.1 z M 9 10 V 7 a 3 3 0 0 1 6 0 v 3", fill = false, stroke = true)) }
    val Wallet: ImageVector by lazy { build("wallet", P("M 6.5 5 H 17.5 A 3.5 3.5 0 0 1 21 8.5 V 16.5 A 3.5 3.5 0 0 1 17.5 20 H 6.5 A 3.5 3.5 0 0 1 3 16.5 V 8.5 A 3.5 3.5 0 0 1 6.5 5 Z", fill = false, stroke = true), P("M 21 10 h -4 a 2.5 2.5 0 0 0 0 5 h 4", fill = false, stroke = true), P("M 15.5 12.5 a 1 1 0 1 0 2 0 a 1 1 0 1 0 -2 0 Z", fill = true, stroke = false)) }
    val CreditCard: ImageVector by lazy { build("credit-card", P("M 6 5 H 18 A 3.5 3.5 0 0 1 21.5 8.5 V 15.5 A 3.5 3.5 0 0 1 18 19 H 6 A 3.5 3.5 0 0 1 2.5 15.5 V 8.5 A 3.5 3.5 0 0 1 6 5 Z", fill = false, stroke = true), P("M 2.5 10 h 19 M 6.5 15 h 4", fill = false, stroke = true)) }
    val Coupon: ImageVector by lazy { build("coupon", P("M 3 8 a 2 2 0 0 1 2 -2 h 14 a 2 2 0 0 1 2 2 v 1.5 a 2.5 2.5 0 0 0 0 5 V 16 a 2 2 0 0 1 -2 2 H 5 a 2 2 0 0 1 -2 -2 v -1.5 a 2.5 2.5 0 0 0 0 -5 V 8 z", fill = false, stroke = true), P("M 14.5 8.5 v 1 M 14.5 11.5 v 1 M 14.5 14.5 v 1", fill = false, stroke = true)) }
    val Receipt: ImageVector by lazy { build("receipt", P("M 5 4.5 A 1.5 1.5 0 0 1 6.5 3 h 11 A 1.5 1.5 0 0 1 19 4.5 v 16 l -2.4 -1.5 -2.3 1.5 -2.3 -1.5 -2.3 1.5 -2.3 -1.5 L 5 20.5 v -16 z", fill = false, stroke = true), P("M 9 8 h 6 M 9 12 h 6 M 9 16 h 3", fill = false, stroke = true)) }
    val Package: ImageVector by lazy { build("package", P("M 11 3.5 a 2 2 0 0 1 2 0 l 6 3.4 a 2 2 0 0 1 1 1.7 v 6.8 a 2 2 0 0 1 -1 1.7 l -6 3.4 a 2 2 0 0 1 -2 0 l -6 -3.4 a 2 2 0 0 1 -1 -1.7 V 8.6 a 2 2 0 0 1 1 -1.7 l 6 -3.4 z", fill = false, stroke = true), P("M 4.3 7.6 L 12 12 l 7.7 -4.4 M 12 12 v 9", fill = false, stroke = true)) }
    val Gift: ImageVector by lazy { build("gift", P("M 4.5 8 H 19.5 A 1.5 1.5 0 0 1 21 9.5 V 10.5 A 1.5 1.5 0 0 1 19.5 12 H 4.5 A 1.5 1.5 0 0 1 3 10.5 V 9.5 A 1.5 1.5 0 0 1 4.5 8 Z", fill = false, stroke = true), P("M 5 12 v 6.5 A 2.5 2.5 0 0 0 7.5 21 h 9 a 2.5 2.5 0 0 0 2.5 -2.5 V 12 M 12 8 v 13 M 12 8 S 10.5 3.5 8 3.5 a 2.25 2.25 0 0 0 0 4.5 M 12 8 s 1.5 -4.5 4 -4.5 a 2.25 2.25 0 0 1 0 4.5", fill = false, stroke = true)) }
    val Store: ImageVector by lazy { build("store", P("M 3.5 9 l 1.2 -3.8 A 1.5 1.5 0 0 1 6.1 4 h 11.8 a 1.5 1.5 0 0 1 1.4 1.2 L 20.5 9 M 3.5 9 a 2.8 2.8 0 0 0 5.6 0 2.9 2.9 0 0 0 5.8 0 2.8 2.8 0 0 0 5.6 0", fill = false, stroke = true), P("M 5 12 v 7 a 1.5 1.5 0 0 0 1.5 1.5 h 11 A 1.5 1.5 0 0 0 19 19 v -7 M 10 20.5 v -4 a 1 1 0 0 1 1 -1 h 2 a 1 1 0 0 1 1 1 v 4", fill = false, stroke = true)) }
    val Truck: ImageVector by lazy { build("truck", P("M 2.5 7 A 2 2 0 0 1 4.5 5 h 8 a 1 1 0 0 1 1 1 v 10.5 H 2.5 V 7 z M 13.5 9 h 4.1 a 1 1 0 0 1 .78 .37 l 2.9 3.6 a 1 1 0 0 1 .22 .63 V 16.5 h -8", fill = false, stroke = true), P("M 5 17.5 a 2 2 0 1 0 4 0 a 2 2 0 1 0 -4 0 Z", fill = false, stroke = true), P("M 15 17.5 a 2 2 0 1 0 4 0 a 2 2 0 1 0 -4 0 Z", fill = false, stroke = true)) }
    val ChartBar: ImageVector by lazy { build("chart-bar", P("M 4 20 h 16 M 7 16 v -5 M 12 16 V 6 M 17 16 V 9", fill = false, stroke = true)) }
    val TrendingUp: ImageVector by lazy { build("trending-up", P("M 3 17 l 6 -6 4 4 8 -8 M 15 7 h 6 v 6", fill = false, stroke = true)) }
    val Percent: ImageVector by lazy { build("percent", P("M 19 5 L 5 19", fill = false, stroke = true), P("M 4.5 7 a 2.5 2.5 0 1 0 5 0 a 2.5 2.5 0 1 0 -5 0 Z", fill = false, stroke = true), P("M 14.5 17 a 2.5 2.5 0 1 0 5 0 a 2.5 2.5 0 1 0 -5 0 Z", fill = false, stroke = true)) }
    val Briefcase: ImageVector by lazy { build("briefcase", P("M 6.5 7 H 17.5 A 3.5 3.5 0 0 1 21 10.5 V 16.5 A 3.5 3.5 0 0 1 17.5 20 H 6.5 A 3.5 3.5 0 0 1 3 16.5 V 10.5 A 3.5 3.5 0 0 1 6.5 7 Z", fill = false, stroke = true), P("M 9 7 V 5.5 A 1.5 1.5 0 0 1 10.5 4 h 3 A 1.5 1.5 0 0 1 15 5.5 V 7 M 3 12.5 h 18", fill = false, stroke = true)) }
    val Heart: ImageVector by lazy { build("heart", P("M 12 20 s -8 -4.6 -8 -10.5 A 4.5 4.5 0 0 1 12 6.6 a 4.5 4.5 0 0 1 8 2.9 C 20 15.4 12 20 12 20 z", fill = false, stroke = true)) }
    val HeartFill: ImageVector by lazy { build("heart-fill", P("M 12 20 s -8 -4.6 -8 -10.5 A 4.5 4.5 0 0 1 12 6.6 a 4.5 4.5 0 0 1 8 2.9 C 20 15.4 12 20 12 20 z", fill = true, stroke = false)) }
    val Star: ImageVector by lazy { build("star", P("M 12 3.5 l 2.6 5.3 5.9 .9 -4.25 4.1 1 5.85 L 12 16.9 l -5.25 2.75 1 -5.85 L 3.5 9.7 l 5.9 -.9 L 12 3.5 z", fill = false, stroke = true)) }
    val StarFill: ImageVector by lazy { build("star-fill", P("M 12 3.5 l 2.6 5.3 5.9 .9 -4.25 4.1 1 5.85 L 12 16.9 l -5.25 2.75 1 -5.85 L 3.5 9.7 l 5.9 -.9 L 12 3.5 z", fill = true, stroke = false)) }
    val Sun: ImageVector by lazy { build("sun", P("M 8 12 a 4 4 0 1 0 8 0 a 4 4 0 1 0 -8 0 Z", fill = false, stroke = true), P("M 12 2.5 v 2 M 12 19.5 v 2 M 4.6 4.6 L 6 6 M 18 18 l 1.4 1.4 M 2.5 12 h 2 M 19.5 12 h 2 M 4.6 19.4 L 6 18 M 18 6 l 1.4 -1.4", fill = false, stroke = true)) }
    val Moon: ImageVector by lazy { build("moon", P("M 20 14.5 A 8 8 0 1 1 9.5 4 a 6.5 6.5 0 0 0 10.5 10.5 z", fill = false, stroke = true)) }
    val MoonFill: ImageVector by lazy { build("moon-fill", P("M 20 14.5 A 8 8 0 1 1 9.5 4 a 6.5 6.5 0 0 0 10.5 10.5 z", fill = true, stroke = false)) }
    val Smile: ImageVector by lazy { build("smile", P("M 3 12 a 9 9 0 1 0 18 0 a 9 9 0 1 0 -18 0 Z", fill = false, stroke = true), P("M 8.5 14.5 a 4.5 4.5 0 0 0 7 0", fill = false, stroke = true), P("M 8 10 a 1 1 0 1 0 2 0 a 1 1 0 1 0 -2 0 Z", fill = true, stroke = false), P("M 14 10 a 1 1 0 1 0 2 0 a 1 1 0 1 0 -2 0 Z", fill = true, stroke = false)) }
    val Sparkle: ImageVector by lazy { build("sparkle", P("M 11 3.5 c .6 4.2 1.8 5.4 6 6 -4.2 .6 -5.4 1.8 -6 6 -.6 -4.2 -1.8 -5.4 -6 -6 4.2 -.6 5.4 -1.8 6 -6 z M 18.5 15 c .3 1.6 .9 2.2 2.5 2.5 -1.6 .3 -2.2 .9 -2.5 2.5 -.3 -1.6 -.9 -2.2 -2.5 -2.5 1.6 -.3 2.2 -.9 2.5 -2.5 z", fill = false, stroke = true)) }
    val Wifi: ImageVector by lazy { build("wifi", P("M 2.5 9 a 14 14 0 0 1 19 0 M 5.5 12.5 a 9.5 9.5 0 0 1 13 0 M 8.5 16 a 5 5 0 0 1 7 0", fill = false, stroke = true), P("M 10.75 19.25 a 1.25 1.25 0 1 0 2.5 0 a 1.25 1.25 0 1 0 -2.5 0 Z", fill = true, stroke = false)) }
    val Bluetooth: ImageVector by lazy { build("bluetooth", P("M 7 7 l 10 10 -5 4.5 v -19 L 17 7 7 17", fill = false, stroke = true)) }
    val Battery: ImageVector by lazy { build("battery", P("M 5.5 7 H 15.5 A 3 3 0 0 1 18.5 10 V 14 A 3 3 0 0 1 15.5 17 H 5.5 A 3 3 0 0 1 2.5 14 V 10 A 3 3 0 0 1 5.5 7 Z", fill = false, stroke = true), P("M 21.5 10.5 v 3 M 6.5 10.5 v 3 M 10 10.5 v 3", fill = false, stroke = true)) }
    val Layers: ImageVector by lazy { build("layers", P("M 11.1 3.45 a 2 2 0 0 1 1.8 0 l 7.6 3.8 a .5 .5 0 0 1 0 .9 l -7.6 3.8 a 2 2 0 0 1 -1.8 0 l -7.6 -3.8 a .5 .5 0 0 1 0 -.9 l 7.6 -3.8 z M 3 12.5 l 8.1 4.05 a 2 2 0 0 0 1.8 0 L 21 12.5 M 3 16.5 l 8.1 4.05 a 2 2 0 0 0 1.8 0 L 21 16.5", fill = false, stroke = true)) }
    val Scan: ImageVector by lazy { build("scan", P("M 4 8.5 V 6.5 A 2.5 2.5 0 0 1 6.5 4 h 2 M 15.5 4 h 2 A 2.5 2.5 0 0 1 20 6.5 v 2 M 20 15.5 v 2 a 2.5 2.5 0 0 1 -2.5 2.5 h -2 M 8.5 20 h -2 A 2.5 2.5 0 0 1 4 17.5 v -2 M 4 12 h 16", fill = false, stroke = true)) }
    val Lightbulb: ImageVector by lazy { build("lightbulb", P("M 9 18 h 6 M 10 21 h 4 M 8.5 14.5 a 6 6 0 1 1 7 0 c -.9 .7 -1.5 1.8 -1.5 3 v .5 h -4 v -.5 c 0 -1.2 -.6 -2.3 -1.5 -3 z", fill = false, stroke = true)) }
    val Code: ImageVector by lazy { build("code", P("M 8 7 l -5 5 5 5 M 16 7 l 5 5 -5 5 M 13.5 4.5 l -3 15", fill = false, stroke = true)) }
    val Terminal: ImageVector by lazy { build("terminal", P("M 6.5 4 H 17.5 A 3.5 3.5 0 0 1 21 7.5 V 16.5 A 3.5 3.5 0 0 1 17.5 20 H 6.5 A 3.5 3.5 0 0 1 3 16.5 V 7.5 A 3.5 3.5 0 0 1 6.5 4 Z", fill = false, stroke = true), P("M 7 9 l 3 3 -3 3 M 12.5 15 H 17", fill = false, stroke = true)) }

    /** 按名称取图标，例如 TesseraIcons.byName("home-fill") */
    fun byName(name: String): ImageVector? = when (name) {
        "arrow-left" -> ArrowLeft
        "arrow-right" -> ArrowRight
        "arrow-up" -> ArrowUp
        "arrow-down" -> ArrowDown
        "arrow-up-right" -> ArrowUpRight
        "arrow-down-left" -> ArrowDownLeft
        "chevron-left" -> ChevronLeft
        "chevron-right" -> ChevronRight
        "chevron-up" -> ChevronUp
        "chevron-down" -> ChevronDown
        "chevrons-up-down" -> ChevronsUpDown
        "corner-up-left" -> CornerUpLeft
        "corner-down-right" -> CornerDownRight
        "undo" -> Undo
        "redo" -> Redo
        "expand" -> Expand
        "collapse" -> Collapse
        "external-link" -> ExternalLink
        "plus" -> Plus
        "minus" -> Minus
        "close" -> Close
        "check" -> Check
        "plus-circle" -> PlusCircle
        "minus-circle" -> MinusCircle
        "edit" -> Edit
        "trash" -> Trash
        "copy" -> Copy
        "paste" -> Paste
        "share" -> Share
        "download" -> Download
        "upload" -> Upload
        "refresh" -> Refresh
        "search" -> Search
        "zoom-in" -> ZoomIn
        "zoom-out" -> ZoomOut
        "filter" -> Filter
        "sliders" -> Sliders
        "sort" -> Sort
        "more" -> More
        "more-vertical" -> MoreVertical
        "link" -> Link
        "bookmark" -> Bookmark
        "bookmark-fill" -> BookmarkFill
        "flag" -> Flag
        "flag-fill" -> FlagFill
        "tag" -> Tag
        "eye" -> Eye
        "eye-off" -> EyeOff
        "print" -> Print
        "archive" -> Archive
        "send" -> Send
        "history" -> History
        "check-square" -> CheckSquare
        "home" -> Home
        "home-fill" -> HomeFill
        "menu" -> Menu
        "settings" -> Settings
        "grid" -> Grid
        "list" -> List
        "sidebar" -> Sidebar
        "maximize" -> Maximize
        "minimize" -> Minimize
        "compass" -> Compass
        "globe" -> Globe
        "location" -> Location
        "location-fill" -> LocationFill
        "calendar" -> Calendar
        "clock" -> Clock
        "success" -> Success
        "success-fill" -> SuccessFill
        "warning" -> Warning
        "error" -> Error
        "info" -> Info
        "help" -> Help
        "loader" -> Loader
        "bolt" -> Bolt
        "file" -> File
        "file-text" -> FileText
        "file-plus" -> FilePlus
        "folder" -> Folder
        "folder-fill" -> FolderFill
        "folder-open" -> FolderOpen
        "image" -> Image
        "video" -> Video
        "paperclip" -> Paperclip
        "cloud" -> Cloud
        "book" -> Book
        "message" -> Message
        "message-fill" -> MessageFill
        "message-circle" -> MessageCircle
        "phone" -> Phone
        "mail" -> Mail
        "bell" -> Bell
        "bell-fill" -> BellFill
        "bell-off" -> BellOff
        "at-sign" -> AtSign
        "inbox" -> Inbox
        "play" -> Play
        "play-fill" -> PlayFill
        "pause" -> Pause
        "pause-fill" -> PauseFill
        "stop" -> Stop
        "stop-fill" -> StopFill
        "skip-back" -> SkipBack
        "skip-forward" -> SkipForward
        "volume" -> Volume
        "mute" -> Mute
        "camera" -> Camera
        "mic" -> Mic
        "mic-off" -> MicOff
        "music" -> Music
        "headphones" -> Headphones
        "shuffle" -> Shuffle
        "repeat" -> Repeat
        "user" -> User
        "user-fill" -> UserFill
        "users" -> Users
        "user-plus" -> UserPlus
        "login" -> Login
        "logout" -> Logout
        "lock" -> Lock
        "unlock" -> Unlock
        "shield" -> Shield
        "shield-fill" -> ShieldFill
        "shield-check" -> ShieldCheck
        "key" -> Key
        "cart" -> Cart
        "bag" -> Bag
        "wallet" -> Wallet
        "credit-card" -> CreditCard
        "coupon" -> Coupon
        "receipt" -> Receipt
        "package" -> Package
        "gift" -> Gift
        "store" -> Store
        "truck" -> Truck
        "chart-bar" -> ChartBar
        "trending-up" -> TrendingUp
        "percent" -> Percent
        "briefcase" -> Briefcase
        "heart" -> Heart
        "heart-fill" -> HeartFill
        "star" -> Star
        "star-fill" -> StarFill
        "sun" -> Sun
        "moon" -> Moon
        "moon-fill" -> MoonFill
        "smile" -> Smile
        "sparkle" -> Sparkle
        "wifi" -> Wifi
        "bluetooth" -> Bluetooth
        "battery" -> Battery
        "layers" -> Layers
        "scan" -> Scan
        "lightbulb" -> Lightbulb
        "code" -> Code
        "terminal" -> Terminal
        else -> null
    }

    val names: List<String> = listOf("arrow-left", "arrow-right", "arrow-up", "arrow-down", "arrow-up-right", "arrow-down-left", "chevron-left", "chevron-right", "chevron-up", "chevron-down", "chevrons-up-down", "corner-up-left", "corner-down-right", "undo", "redo", "expand", "collapse", "external-link", "plus", "minus", "close", "check", "plus-circle", "minus-circle", "edit", "trash", "copy", "paste", "share", "download", "upload", "refresh", "search", "zoom-in", "zoom-out", "filter", "sliders", "sort", "more", "more-vertical", "link", "bookmark", "bookmark-fill", "flag", "flag-fill", "tag", "eye", "eye-off", "print", "archive", "send", "history", "check-square", "home", "home-fill", "menu", "settings", "grid", "list", "sidebar", "maximize", "minimize", "compass", "globe", "location", "location-fill", "calendar", "clock", "success", "success-fill", "warning", "error", "info", "help", "loader", "bolt", "file", "file-text", "file-plus", "folder", "folder-fill", "folder-open", "image", "video", "paperclip", "cloud", "book", "message", "message-fill", "message-circle", "phone", "mail", "bell", "bell-fill", "bell-off", "at-sign", "inbox", "play", "play-fill", "pause", "pause-fill", "stop", "stop-fill", "skip-back", "skip-forward", "volume", "mute", "camera", "mic", "mic-off", "music", "headphones", "shuffle", "repeat", "user", "user-fill", "users", "user-plus", "login", "logout", "lock", "unlock", "shield", "shield-fill", "shield-check", "key", "cart", "bag", "wallet", "credit-card", "coupon", "receipt", "package", "gift", "store", "truck", "chart-bar", "trending-up", "percent", "briefcase", "heart", "heart-fill", "star", "star-fill", "sun", "moon", "moon-fill", "smile", "sparkle", "wifi", "bluetooth", "battery", "layers", "scan", "lightbulb", "code", "terminal")
}
