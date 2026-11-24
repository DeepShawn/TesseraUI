using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Text.RegularExpressions;
using Microsoft.UI.Input;
using Microsoft.UI.Text;
using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Automation;
using Microsoft.UI.Xaml.Automation.Peers;
using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Input;
using Microsoft.UI.Xaml.Media;
using Windows.ApplicationModel.DataTransfer;
using Windows.System;
using Windows.UI.Core;

namespace TesseraUI
{
    public enum TesseraTokenKind { Keyword, String, Number, Function, Comment }

    /// <summary>语法高亮与编辑操作（与 Web、Android、Apple 同一套规则；纯函数，便于测试）</summary>
    public static class TesseraCode
    {
        private static readonly Dictionary<string, HashSet<string>> Keywords;
        private static readonly Dictionary<string, string> Aliases = new() { ["javascript"] = "js", ["typescript"] = "ts", ["kt"] = "kotlin", ["cs"] = "csharp", ["c#"] = "csharp", ["py"] = "python" };
        private static readonly Regex CLike = new(@"(//[^\n]*|/\*[\s\S]*?(?:\*/|$))|(""(?:\\.|[^""\\\n])*""|'(?:\\.|[^'\\\n])*'|`(?:\\.|[^`\\])*`)|(\b\d[\d_]*(?:\.\d+)?(?:[eE][+-]?\d+)?[fFLu]?\b|\b0x[\da-fA-F]+\b)|([A-Za-z_$][\w$]*)(?=\s*\()|([A-Za-z_$][\w$]*)", RegexOptions.Compiled);
        private static readonly Regex Hash = new(@"(#[^\n]*)|(""""""[\s\S]*?""""""|'''[\s\S]*?'''|""(?:\\.|[^""\\\n])*""|'(?:\\.|[^'\\\n])*')|(\b\d[\d_]*(?:\.\d+)?(?:[eE][+-]?\d+)?\b|\b0x[\da-fA-F]+\b)|([A-Za-z_$][\w$]*)(?=\s*\()|([A-Za-z_$][\w$]*)", RegexOptions.Compiled);

        static TesseraCode()
        {
            var js = "await async break case catch class const continue default delete do else export extends false finally for from function if import in instanceof let new null of return static super switch this throw true try typeof undefined var void while yield";
            var raw = new Dictionary<string, string>
            {
                ["js"] = js,
                ["ts"] = js + " interface type enum implements private public readonly declare keyof as",
                ["kotlin"] = "as break class continue do else false for fun if in interface is null object package return super this throw true try typealias typeof val var when while data sealed private public internal override suspend import",
                ["swift"] = "as break case catch class continue default defer do else enum extension false for func guard if import in init let nil private protocol public return self static struct switch throw throws true try var while some any async await",
                ["csharp"] = "abstract as async await base bool break case catch class const continue default do double else enum false finally for foreach if in int interface internal is namespace new null object out override private protected public readonly return sealed static string struct switch this throw true try using var void while",
                ["python"] = "and as assert async await break class continue def del elif else except False finally for from global if import in is lambda None nonlocal not or pass raise return True try while with yield",
                ["json"] = "true false null",
            };
            Keywords = raw.ToDictionary(kv => kv.Key, kv => new HashSet<string>(kv.Value.Split(' ')));
        }

        /// <summary>按语言分词，返回需要着色的 (起点, 长度, 类别)；不认识的语言返回空</summary>
        public static List<(int Start, int Length, TesseraTokenKind Kind)> Tokens(string code, string language)
        {
            var key = Aliases.TryGetValue(language.ToLowerInvariant(), out var a) ? a : language.ToLowerInvariant();
            var list = new List<(int, int, TesseraTokenKind)>();
            if (!Keywords.TryGetValue(key, out var words)) return list;
            foreach (Match m in (key == "python" ? Hash : CLike).Matches(code))
            {
                if (m.Groups[1].Success) list.Add((m.Index, m.Length, TesseraTokenKind.Comment));
                else if (m.Groups[2].Success) list.Add((m.Index, m.Length, TesseraTokenKind.String));
                else if (m.Groups[3].Success) list.Add((m.Index, m.Length, TesseraTokenKind.Number));
                else if (m.Groups[4].Success) list.Add((m.Index, m.Length, words.Contains(m.Value) ? TesseraTokenKind.Keyword : TesseraTokenKind.Function));
                else if (m.Groups[5].Success && words.Contains(m.Value)) list.Add((m.Index, m.Length, TesseraTokenKind.Keyword));
            }
            return list;
        }

        private static int LineStart(string t, int i) => i <= 0 ? 0 : t.LastIndexOf('\n', i - 1) + 1;

        /// <summary>Tab：插入一级缩进；选中多行时整块缩进</summary>
        public static (string Text, int Start, int End) Indent(string t, int s, int e, string unit)
        {
            if (!t.Substring(s, e - s).Contains('\n')) return (t.Remove(s, e - s).Insert(s, unit), s + unit.Length, s + unit.Length);
            var from = LineStart(t, s);
            var to = e > s && t[e - 1] == '\n' ? e - 1 : e;
            var block = string.Join("\n", t.Substring(from, to - from).Split('\n').Select(l => unit + l));
            return (t.Remove(from, to - from).Insert(from, block), s + unit.Length, from + block.Length);
        }

        /// <summary>Shift+Tab：当前行（或选中的每一行）去掉一级缩进</summary>
        public static (string Text, int Start, int End) Outdent(string t, int s, int e, string unit)
        {
            var from = LineStart(t, s);
            int to;
            if (e > s) to = t[e - 1] == '\n' ? e - 1 : e;
            else { var nl = t.IndexOf('\n', from); to = nl < 0 ? t.Length : nl; }
            var lines = t.Substring(from, to - from).Split('\n');
            var strip = new Regex($"^ {{1,{unit.Length}}}|^\t");
            var outLines = lines.Select(l => strip.Replace(l, "", 1)).ToArray();
            var block = string.Join("\n", outLines);
            var start = Math.Max(from, s - (lines[0].Length - outLines[0].Length));
            return (t.Remove(from, to - from).Insert(from, block), start, e > s ? from + block.Length : start);
        }

        /// <summary>回车：保持上一行缩进；以 { [ ( : 结尾时多缩一级；光标在一对括号中间时把右括号放到下一行</summary>
        public static (string Text, int Start, int End) Newline(string t, int s, int e, string unit)
        {
            var from = LineStart(t, s);
            var line = t.Substring(from, s - from);
            var baseIndent = new string(line.TakeWhile(c => c == ' ' || c == '\t').ToArray());
            var trimmed = line.TrimEnd();
            var opens = trimmed.Length > 0 && "{[(:".Contains(trimmed[^1]);
            var lead = opens ? baseIndent + unit : baseIndent;
            var rest = t.Remove(s, e - s);
            if (opens && trimmed[^1] != ':' && s < rest.Length && "}])".Contains(rest[s]))
            {
                var ins = "\n" + lead + "\n" + baseIndent;
                return (rest.Insert(s, ins), s + 1 + lead.Length, s + 1 + lead.Length);
            }
            var insert = "\n" + lead;
            return (rest.Insert(s, insert), s + insert.Length, s + insert.Length);
        }

        public static string Hex2(byte b) => b.ToString("X2");

        /// <summary>十六进制查看的状态说明（与其他端一致）</summary>
        public static string Describe(byte[] data, int from, int to, int focus, IEnumerable<TesseraHexRange>? ranges = null)
        {
            if (data.Length == 0) return "没有数据";
            var b = data[focus];
            var ch = b >= 0x20 && b < 0x7F ? $"，字符“{(char)b}”" : "";
            var r = ranges?.FirstOrDefault(x => focus >= x.From && focus < x.To);
            var sel = to > from ? $"，已选 {to - from + 1} 个字节（{from}–{to}）" : "";
            return $"偏移 0x{focus:X}（{focus}）：0x{Hex2(b)} = {b} = 0b{Convert.ToString(b, 2).PadLeft(8, '0')}{ch}{(r != null ? "，" + r.Label : "")}{sel}";
        }
    }

    public sealed class TesseraDiagnostic
    {
        public int Line { get; set; }
        public string Message { get; set; } = "";
    }

    // ------------------------------------------------------------------
    // 代码编辑器 CodeEditor
    // ------------------------------------------------------------------

    /// <summary>
    /// 代码编辑器：凹槽底色，左侧行号栏；语法高亮、当前行底色、第几行第几列。
    /// Tab / Shift+Tab 缩进（多行整块），按 Esc 之后再按 Tab 离开编辑器；回车保持缩进、{ 之后多缩一级；
    /// 输入法组合中不做任何处理。着色会写进 RichEditBox 的撤销记录，所以撤销 / 重做（Ctrl+Z / Ctrl+Y）由编辑器自己管理。
    /// </summary>
    public sealed partial class TesseraCodeEditor : ContentControl
    {
        public RichEditBox Box { get; } = new()
        {
            AcceptsReturn = true, TextWrapping = TextWrapping.NoWrap, IsSpellCheckEnabled = false, IsTextPredictionEnabled = false,
            FontSize = 13, Padding = new Thickness(14, 12, 14, 12), MinHeight = 120, BorderThickness = new Thickness(0),
            ClipboardCopyFormat = RichEditClipboardFormat.PlainText, DisabledFormattingAccelerators = DisabledFormattingAccelerators.All,
        };
        public string CodeLanguage { get => _lang; set { _lang = value; _langText.Text = value; Restyle(); } }
        public int IndentSize { get; set; } = 2;
        public event EventHandler? TextChanged;

        private string _lang = "";
        private string _text = "";
        private bool _composing, _exitOnTab, _applying;
        private readonly List<(string Text, int Sel)> _undo = new();
        private readonly List<(string Text, int Sel)> _redo = new();
        private readonly TextBlock _gutter = new() { FontSize = 13, LineHeight = 21, LineStackingStrategy = LineStackingStrategy.BlockLineHeight, TextAlignment = TextAlignment.Right, MinWidth = 26, Margin = new Thickness(12, 12, 10, 12) };
        private readonly TextBlock _langText = new() { FontSize = 12, VerticalAlignment = VerticalAlignment.Center };
        private readonly TextBlock _pos = new() { FontSize = 12, HorizontalAlignment = HorizontalAlignment.Right, VerticalAlignment = VerticalAlignment.Center };
        private readonly Canvas _bars = new() { IsHitTestVisible = false };
        private readonly StackPanel _diag = new();
        private readonly TextBlock _hint = new() { Text = "Tab 缩进；按 Esc 后再按 Tab 可离开编辑器", FontSize = 12, Margin = new Thickness(16, 6, 16, 6) };
        private List<TesseraDiagnostic> _diagnostics = new();
        private const double LineH = 21;

        public string Text
        {
            get => _text;
            set { _text = (value ?? "").Replace("\r\n", "\n").Replace('\r', '\n'); SetBox(_text, 0); _undo.Clear(); _redo.Clear(); }
        }

        public TesseraCodeEditor()
        {
            IsTabStop = false;
            HorizontalContentAlignment = HorizontalAlignment.Stretch;
            AutomationProperties.SetName(Box, "代码");
            Box.TextCompositionStarted += (_, _) => _composing = true;
            Box.TextCompositionEnded += (_, _) => { _composing = false; OnChanged(); };
            Box.TextChanged += (_, _) => { if (!_applying && !_composing) OnChanged(); };
            Box.SelectionChanged += (_, _) => Caret();
            Box.GotFocus += (_, _) => Caret();
            Box.LostFocus += (_, _) => { _exitOnTab = false; Caret(); };
            Box.PreviewKeyDown += OnKey;
            Loaded += (_, _) => Build();
            ActualThemeChanged += (_, _) => Build();
        }

        public void SetDiagnostics(IEnumerable<TesseraDiagnostic> list)
        {
            _diagnostics = list.ToList();
            AutomationProperties.SetHelpText(Box, string.Join("；", _diagnostics.Select(d => $"第 {d.Line} 行：{d.Message}")));
            Paint();
        }

        // 版面只在第一次加载时搭一次；之后外观或主题变化只改颜色、圆角（WinUI 里把同一个元素移到新容器会出错）
        private Grid? _root, _head, _hintWrap;

        private void Layout()
        {
            _root = new Grid();
            for (var i = 0; i < 4; i++) _root.RowDefinitions.Add(new RowDefinition { Height = GridLength.Auto });
            _head = new Grid { Padding = new Thickness(16, 8, 16, 8) };
            _head.Children.Add(_langText);
            _head.Children.Add(_pos);
            AutomationProperties.SetAccessibilityView(_pos, AccessibilityView.Raw);
            _root.Children.Add(_head);
            var body = new Grid();
            body.ColumnDefinitions.Add(new ColumnDefinition { Width = GridLength.Auto });
            body.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(1, GridUnitType.Star) });
            AutomationProperties.SetAccessibilityView(_gutter, AccessibilityView.Raw);
            body.Children.Add(_gutter);
            var stage = new Grid();
            stage.Children.Add(_bars);
            stage.Children.Add(Box);
            Grid.SetColumn(stage, 1);
            body.Children.Add(stage);
            var scroller = new ScrollViewer { Content = body, MaxHeight = 360, HorizontalScrollMode = ScrollMode.Auto, HorizontalScrollBarVisibility = ScrollBarVisibility.Auto, VerticalScrollMode = ScrollMode.Auto, VerticalScrollBarVisibility = ScrollBarVisibility.Auto, IsTabStop = false };
            Grid.SetRow(scroller, 1);
            _root.Children.Add(scroller);
            _hintWrap = new Grid();
            _hintWrap.Children.Add(_hint);
            Grid.SetRow(_hintWrap, 2);
            _root.Children.Add(_hintWrap);
            AutomationProperties.SetLiveSetting(_diag, AutomationLiveSetting.Polite);
            Grid.SetRow(_diag, 3);
            _root.Children.Add(_diag);
            ScrollViewer.SetVerticalScrollBarVisibility(Box, ScrollBarVisibility.Disabled);
            ScrollViewer.SetVerticalScrollMode(Box, ScrollMode.Disabled);
            ScrollViewer.SetHorizontalScrollBarVisibility(Box, ScrollBarVisibility.Disabled);
            ScrollViewer.SetHorizontalScrollMode(Box, ScrollMode.Disabled);
            Content = _root;
        }

        private void Build()
        {
            if (_root == null) Layout();
            var round = TesseraResources.IsRound;
            var mono = Application.Current?.Resources.TryGetValue("TesseraMonoFontFamily", out var f) == true && f is FontFamily ff ? ff : new FontFamily("Consolas");
            Box.FontFamily = mono; _gutter.FontFamily = mono; _langText.FontFamily = mono; _pos.FontFamily = mono;
            // RichEditBox 自己的底色、边框、焦点底色都换成透明：外框统一画凹槽与焦点光晕
            foreach (var k in new[] { "TextControlBackground", "TextControlBackgroundPointerOver", "TextControlBackgroundFocused", "TextControlBorderBrush", "TextControlBorderBrushPointerOver", "TextControlBorderBrushFocused" })
                Box.Resources[k] = new SolidColorBrush(Microsoft.UI.Colors.Transparent);
            Box.Resources["TextControlForeground"] = DataUtil.Res("TesseraTextPrimaryBrush");
            Box.Resources["TextControlForegroundFocused"] = DataUtil.Res("TesseraTextPrimaryBrush");
            Box.Resources["TextControlForegroundPointerOver"] = DataUtil.Res("TesseraTextPrimaryBrush");
            _root!.CornerRadius = new CornerRadius(round ? (TesseraResources.IsFlow ? 18 : 22) : 14);
            _root.Background = DataUtil.Res("TesseraFillDefaultBrush");
            _root.BorderThickness = new Thickness(round ? 0 : 1);
            _root.BorderBrush = DataUtil.Res("TesseraBorderFieldBrush");
            _head!.BorderBrush = DataUtil.Res("TesseraBorderSubtleBrush");
            _head.BorderThickness = new Thickness(0, 0, 0, round ? 0 : 1);
            _hintWrap!.BorderBrush = DataUtil.Res("TesseraBorderSubtleBrush");
            _hintWrap.BorderThickness = new Thickness(0, round ? 0 : 1, 0, 0);
            _hintWrap.Visibility = Box.IsReadOnly ? Visibility.Collapsed : Visibility.Visible;
            foreach (var t in new[] { _langText, _pos, _gutter, _hint }) t.Foreground = DataUtil.Res("TesseraTextSecondaryBrush");
            _diag.Background = DataUtil.Res("TesseraDangerSubtleBrush");
            if (_text.Length > 0 && ReadBox() != _text) SetBox(_text, 0);
            Restyle();
            Paint();
        }

        private static void Detach(FrameworkElement el)
        {
            switch (el.Parent)
            {
                case Panel p: p.Children.Remove(el); break;
                case ContentControl c: c.Content = null; break;
            }
        }

        private string ReadBox()
        {
            Box.TextDocument.GetText(TextGetOptions.None, out var t);
            // RichEditBox 用 \r 表示换行，末尾总有一个段落结束符
            if (t.EndsWith("\r")) t = t[..^1];
            return t.Replace('\r', '\n');
        }

        private void SetBox(string text, int sel, int? end = null)
        {
            _applying = true;
            Box.TextDocument.SetText(TextSetOptions.None, text);
            Box.TextDocument.Selection.SetRange(sel, end ?? sel);
            _applying = false;
            Restyle();
            Caret();
        }

        private void OnChanged()
        {
            var t = ReadBox();
            if (t == _text) return;
            Push(_text, Box.TextDocument.Selection.StartPosition);
            _redo.Clear();
            _text = t;
            Restyle();
            Caret();
            TextChanged?.Invoke(this, EventArgs.Empty);
        }

        private void Push(string text, int sel)
        {
            _undo.Add((text, sel));
            if (_undo.Count > 200) _undo.RemoveAt(0);
        }

        /// <summary>着色：只改格式，不改文字；输入法组合中跳过</summary>
        private void Restyle()
        {
            if (_composing) return;
            var doc = Box.TextDocument;
            _applying = true;
            doc.BatchDisplayUpdates();
            var all = doc.GetRange(0, int.MaxValue);
            all.CharacterFormat.ForegroundColor = Color("TesseraTextPrimaryBrush");
            all.CharacterFormat.Bold = FormatEffect.Off;
            all.CharacterFormat.Italic = FormatEffect.Off;
            all.ParagraphFormat.SetLineSpacing(LineSpacingRule.Exactly, (float)(LineH * 0.75));
            foreach (var (s, len, kind) in TesseraCode.Tokens(_text, _lang))
            {
                var r = doc.GetRange(s, s + len);
                r.CharacterFormat.ForegroundColor = kind switch
                {
                    TesseraTokenKind.Keyword => Color("TesseraPigmentLapisTextBrush"),
                    TesseraTokenKind.String => Color("TesseraPigmentOliveTextBrush"),
                    TesseraTokenKind.Number => Color("TesseraPigmentOchreTextBrush"),
                    TesseraTokenKind.Function => Color("TesseraPigmentAmethystTextBrush"),
                    _ => Color("TesseraTextSecondaryBrush"),
                };
                if (kind == TesseraTokenKind.Keyword) r.CharacterFormat.Bold = FormatEffect.On;
                if (kind == TesseraTokenKind.Comment) r.CharacterFormat.Italic = FormatEffect.On;
            }
            doc.ApplyDisplayUpdates();
            _applying = false;
            Paint();
        }

        private static Windows.UI.Color Color(string key) => (DataUtil.Res(key) as SolidColorBrush)?.Color ?? Microsoft.UI.Colors.Black;

        private (int Line, int Col) CaretPos()
        {
            var s = Math.Min(Box.TextDocument.Selection.StartPosition, _text.Length);
            var line = _text.Take(s).Count(c => c == '\n');
            var col = s - (s == 0 ? -1 : _text.LastIndexOf('\n', s - 1));
            return (line, col);
        }

        private void Caret()
        {
            var (line, col) = CaretPos();
            _pos.Text = $"第 {line + 1} 行，第 {col} 列";
            Paint();
        }

        private void Paint()
        {
            var n = _text.Count(c => c == '\n') + 1;
            var (line, _) = CaretPos();
            var focused = Box.FocusState != FocusState.Unfocused;
            var errors = _diagnostics.Select(d => d.Line).ToHashSet();
            _gutter.Inlines.Clear();
            for (var i = 1; i <= n; i++)
            {
                var run = new Microsoft.UI.Xaml.Documents.Run { Text = i == n ? $"{i}" : $"{i}\n" };
                if (errors.Contains(i)) { run.Foreground = DataUtil.Res("TesseraDangerTextBrush"); run.FontWeight = Microsoft.UI.Text.FontWeights.SemiBold; }
                else if (focused && i == line + 1) { run.Foreground = DataUtil.Res("TesseraTextPrimaryBrush"); run.FontWeight = Microsoft.UI.Text.FontWeights.SemiBold; }
                _gutter.Inlines.Add(run);
            }
            _bars.Children.Clear();
            foreach (var e in errors) AddBar(e - 1, Microsoft.UI.ColorHelper.FromArgb(31, Color("TesseraDangerSolidBrush").R, Color("TesseraDangerSolidBrush").G, Color("TesseraDangerSolidBrush").B));
            if (focused) { var c = Color("TesseraTextPrimaryBrush"); AddBar(line, Microsoft.UI.ColorHelper.FromArgb(13, c.R, c.G, c.B)); }
            _diag.Children.Clear();
            foreach (var d in _diagnostics)
            {
                var row = new StackPanel { Orientation = Orientation.Horizontal, Spacing = 8, Padding = new Thickness(16, 6, 16, 6) };
                row.Children.Add(new TextBlock { Text = $"第 {d.Line} 行", FontWeight = Microsoft.UI.Text.FontWeights.SemiBold, Foreground = DataUtil.Res("TesseraTextPrimaryBrush") });
                row.Children.Add(new TextBlock { Text = d.Message, Foreground = DataUtil.Res("TesseraTextPrimaryBrush"), TextWrapping = TextWrapping.Wrap });
                _diag.Children.Add(row);
            }
        }

        private void AddBar(int line, Windows.UI.Color color)
        {
            var bar = new Microsoft.UI.Xaml.Shapes.Rectangle { Height = LineH, Width = 4000, Fill = new SolidColorBrush(color) };
            Canvas.SetTop(bar, 12 + line * LineH);
            _bars.Children.Add(bar);
        }

        private void Apply((string Text, int Start, int End) r)
        {
            Push(_text, Box.TextDocument.Selection.StartPosition);
            _redo.Clear();
            _text = r.Text;
            SetBox(_text, r.Start, r.End);
            TextChanged?.Invoke(this, EventArgs.Empty);
        }

        private void OnKey(object sender, KeyRoutedEventArgs e)
        {
            if (_composing || Box.IsReadOnly) return;
            var ctrl = InputKeyboardSource.GetKeyStateForCurrentThread(VirtualKey.Control).HasFlag(CoreVirtualKeyStates.Down);
            var shift = InputKeyboardSource.GetKeyStateForCurrentThread(VirtualKey.Shift).HasFlag(CoreVirtualKeyStates.Down);
            var sel = Box.TextDocument.Selection;
            int s = Math.Min(sel.StartPosition, _text.Length), en = Math.Min(sel.EndPosition, _text.Length);
            var unit = new string(' ', IndentSize);
            switch (e.Key)
            {
                case VirtualKey.Escape:
                    _exitOnTab = true;
                    return;
                case VirtualKey.Tab:
                    if (_exitOnTab)
                    {
                        // Esc 之后：把焦点交给下一个（Shift 时上一个）控件
                        _exitOnTab = false;
                        FocusManager.TryMoveFocus(shift ? FocusNavigationDirection.Previous : FocusNavigationDirection.Next, new FindNextElementOptions { SearchRoot = XamlRoot.Content });
                        e.Handled = true;
                        return;
                    }
                    Apply(shift ? TesseraCode.Outdent(_text, s, en, unit) : TesseraCode.Indent(_text, s, en, unit));
                    e.Handled = true;
                    return;
                case VirtualKey.Enter when !ctrl:
                    _exitOnTab = false;
                    Apply(TesseraCode.Newline(_text, s, en, unit));
                    e.Handled = true;
                    return;
                case VirtualKey.Z when ctrl:
                    if (shift) Redo(); else Undo();
                    e.Handled = true;
                    return;
                case VirtualKey.Y when ctrl:
                    Redo();
                    e.Handled = true;
                    return;
                default:
                    _exitOnTab = false;
                    return;
            }
        }

        public void Undo()
        {
            if (_undo.Count == 0) return;
            var (t, sel) = _undo[^1];
            _undo.RemoveAt(_undo.Count - 1);
            _redo.Add((_text, Box.TextDocument.Selection.StartPosition));
            _text = t;
            SetBox(t, Math.Min(sel, t.Length));
            TextChanged?.Invoke(this, EventArgs.Empty);
        }

        public void Redo()
        {
            if (_redo.Count == 0) return;
            var (t, sel) = _redo[^1];
            _redo.RemoveAt(_redo.Count - 1);
            Push(_text, Box.TextDocument.Selection.StartPosition);
            _text = t;
            SetBox(t, Math.Min(sel, t.Length));
            TextChanged?.Invoke(this, EventArgs.Empty);
        }
    }

    // ------------------------------------------------------------------
    // 十六进制查看 HexView
    // ------------------------------------------------------------------

    public sealed class TesseraHexRange
    {
        public int From { get; set; }
        public int To { get; set; }
        public string Label { get; set; } = "";
        /// <summary>accent / lapis / ochre / olive / amethyst / cinnabar</summary>
        public string Tone { get; set; } = "accent";
    }

    /// <summary>
    /// 十六进制查看：偏移 / 16 个字节（宽度小于 560 时 8 个）/ ASCII 三栏，只渲染可见的行。
    /// 点击选中一个字节（两栏同步），Shift + 点击扩展；方向键、PageUp / PageDown、Home / End（Ctrl 到首尾）移动，Shift 扩展；
    /// Ctrl+C 复制为十六进制。整个区域是一个可聚焦元素，状态栏实时读出当前字节。
    /// </summary>
    public sealed partial class TesseraHexView : ContentControl
    {
        public IList<TesseraHexRange> Highlights { get; } = new List<TesseraHexRange>();
        public int? Columns { get; set; }
        public double ViewHeight { get; set; } = 312;
        private byte[] _data = Array.Empty<byte>();
        public byte[] Data { get => _data; set { _data = value ?? Array.Empty<byte>(); _anchor = _focus = 0; Rebuild(); } }
        public event EventHandler? SelectionChanged;
        public (int From, int To) Selection => (Math.Min(_anchor, _focus), Math.Max(_anchor, _focus));

        private int _anchor, _focus, _cols = 16;
        private readonly ScrollViewer _scroll = new() { HorizontalScrollMode = ScrollMode.Auto, HorizontalScrollBarVisibility = ScrollBarVisibility.Auto, VerticalScrollMode = ScrollMode.Auto, VerticalScrollBarVisibility = ScrollBarVisibility.Auto, IsTabStop = true, UseSystemFocusVisuals = true };
        private readonly Canvas _canvas = new();
        private readonly TextBlock _status = new() { FontSize = 12, TextWrapping = TextWrapping.Wrap, Margin = new Thickness(14, 6, 6, 6), VerticalAlignment = VerticalAlignment.Center };
        private readonly StackPanel _head = new() { Orientation = Orientation.Horizontal, Padding = new Thickness(14, 6, 14, 6) };
        private FontFamily _mono = new("Consolas");
        private const double RowH = 26, Off = 76, Cell = 24, Gap = 6, AsciiGap = 12, Ch = 10, Pad = 14;

        public TesseraHexView()
        {
            IsTabStop = false;
            HorizontalContentAlignment = HorizontalAlignment.Stretch;
            _scroll.Content = _canvas;
            AutomationProperties.SetName(_scroll, "十六进制数据");
            _scroll.ViewChanged += (_, _) => { PaintRows(); _head.RenderTransform = new TranslateTransform { X = -_scroll.HorizontalOffset }; };
            _scroll.KeyDown += OnKey;
            _scroll.GotFocus += (_, _) => Describe();
            _canvas.PointerPressed += OnPress;
            Loaded += (_, _) => Rebuild();
            ActualThemeChanged += (_, _) => Rebuild();
            SizeChanged += (_, e) => { var c = Columns ?? (e.NewSize.Width < 560 ? 8 : 16); if (c != _cols) Rebuild(); };
        }

        public string Label { get => AutomationProperties.GetName(_scroll); set => AutomationProperties.SetName(_scroll, value); }

        private double ByteX(int k) => Pad + Off + k * Cell + (k >= _cols / 2 ? Gap : 0);
        private double AsciiX(int k) => Pad + Off + _cols * Cell + Gap + AsciiGap + k * Ch;
        private double ContentWidth => AsciiX(_cols) + Pad;

        public void Select(int i, bool extend = false)
        {
            if (_data.Length == 0) return;
            _focus = Math.Clamp(i, 0, _data.Length - 1);
            if (!extend) _anchor = _focus;
            var top = _focus / _cols * RowH;
            if (top < _scroll.VerticalOffset) _scroll.ChangeView(null, top, null, true);
            else if (top + RowH > _scroll.VerticalOffset + _scroll.ViewportHeight) _scroll.ChangeView(null, top + RowH - _scroll.ViewportHeight, null, true);
            PaintRows();
            Describe();
            SelectionChanged?.Invoke(this, EventArgs.Empty);
        }

        // 版面只搭一次（WinUI 里把同一个元素移到新容器会出错）；之后只换内容和颜色
        private StackPanel? _root, _legend;
        private Grid? _headWrap, _foot;

        private void Layout()
        {
            _root = new StackPanel();
            _headWrap = new Grid();
            _headWrap.Children.Add(_head);
            AutomationProperties.SetAccessibilityView(_head, AccessibilityView.Raw);
            _root.Children.Add(_headWrap);
            _root.Children.Add(_scroll);
            _foot = new Grid();
            _foot.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(1, GridUnitType.Star) });
            _foot.ColumnDefinitions.Add(new ColumnDefinition { Width = GridLength.Auto });
            AutomationProperties.SetLiveSetting(_status, AutomationLiveSetting.Polite);
            _foot.Children.Add(_status);
            var copy = DataUtil.IconButton("copy", "复制选中的字节");
            copy.Click += (_, _) => Copy();
            Grid.SetColumn(copy, 1);
            _foot.Children.Add(copy);
            _root.Children.Add(_foot);
            _legend = new StackPanel { Orientation = Orientation.Horizontal, Spacing = 16, Padding = new Thickness(14, 0, 14, 10) };
            _root.Children.Add(_legend);
            Content = _root;
        }

        private void Rebuild()
        {
            if (_root == null) Layout();
            _cols = Columns ?? (ActualWidth > 0 && ActualWidth < 560 ? 8 : 16);
            _mono = Application.Current?.Resources.TryGetValue("TesseraMonoFontFamily", out var f) == true && f is FontFamily ff ? ff : new FontFamily("Consolas");
            var round = TesseraResources.IsRound;
            _root!.CornerRadius = new CornerRadius(round ? (TesseraResources.IsFlow ? 18 : 22) : 14);
            _root.Background = DataUtil.Res("TesseraFillDefaultBrush");
            _head.Children.Clear();
            _head.Children.Add(MonoText("偏移", Off, 12, "TesseraTextSecondaryBrush"));
            for (var k = 0; k < _cols; k++)
            {
                if (k == _cols / 2) _head.Children.Add(new Grid { Width = Gap });
                var t = MonoText(TesseraCode.Hex2((byte)k), Cell, 12, "TesseraTextSecondaryBrush"); t.TextAlignment = TextAlignment.Center;
                _head.Children.Add(t);
            }
            _head.Children.Add(new Grid { Width = AsciiGap });
            _head.Children.Add(MonoText("ASCII", 60, 12, "TesseraTextSecondaryBrush"));
            _headWrap!.BorderBrush = DataUtil.Res("TesseraBorderSubtleBrush");
            _headWrap.BorderThickness = new Thickness(0, 0, 0, round ? 0 : 1);
            _scroll.Height = ViewHeight;
            _canvas.Width = ContentWidth;
            _canvas.Height = Math.Ceiling(_data.Length / (double)_cols) * RowH;
            _canvas.Background = new SolidColorBrush(Microsoft.UI.Colors.Transparent);
            _foot!.BorderBrush = DataUtil.Res("TesseraBorderSubtleBrush");
            _foot.BorderThickness = new Thickness(0, round ? 0 : 1, 0, 0);
            _status.FontFamily = _mono;
            _status.Foreground = DataUtil.Res("TesseraTextSecondaryBrush");
            _legend!.Children.Clear();
            _legend.Visibility = Highlights.Count > 0 ? Visibility.Visible : Visibility.Collapsed;
            foreach (var h in Highlights)
            {
                var item = new StackPanel { Orientation = Orientation.Horizontal, Spacing = 6 };
                item.Children.Add(new Grid { Width = 10, Height = 10, CornerRadius = new CornerRadius(3), Background = ToneSolid(h.Tone), VerticalAlignment = VerticalAlignment.Center });
                item.Children.Add(new TextBlock { Text = $"{h.Label} {h.From}–{h.To - 1}", FontSize = 12, Foreground = DataUtil.Res("TesseraTextSecondaryBrush") });
                _legend.Children.Add(item);
            }
            PaintRows();
            Describe();
        }

        private TextBlock MonoText(string text, double width, double size, string brush) =>
            new() { Text = text, Width = width, FontSize = size, FontFamily = _mono, Foreground = DataUtil.Res(brush), VerticalAlignment = VerticalAlignment.Center };

        private static Brush ToneSubtle(string tone) => DataUtil.Res(tone switch
        {
            "lapis" => "TesseraPigmentLapisSubtleBrush", "ochre" => "TesseraPigmentOchreSubtleBrush", "olive" => "TesseraPigmentOliveSubtleBrush",
            "amethyst" => "TesseraPigmentAmethystSubtleBrush", "cinnabar" => "TesseraPigmentCinnabarSubtleBrush", _ => "TesseraAccentSubtleBrush",
        });

        private static Brush ToneSolid(string tone) => DataUtil.Res(tone switch
        {
            "lapis" => "TesseraPigmentLapisBrush", "ochre" => "TesseraPigmentOchreBrush", "olive" => "TesseraPigmentOliveBrush",
            "amethyst" => "TesseraPigmentAmethystBrush", "cinnabar" => "TesseraPigmentCinnabarBrush", _ => "TesseraAccentControlBrush",
        });

        /// <summary>只画可见的行（前后各多画 8 行）</summary>
        private void PaintRows()
        {
            _canvas.Children.Clear();
            if (_data.Length == 0) return;
            var total = (int)Math.Ceiling(_data.Length / (double)_cols);
            var first = Math.Max(0, (int)(_scroll.VerticalOffset / RowH) - 8);
            var last = Math.Min(total, (int)Math.Ceiling((_scroll.VerticalOffset + Math.Max(_scroll.ViewportHeight, ViewHeight)) / RowH) + 8);
            var (lo, hi) = Selection;
            var radius = new CornerRadius(TesseraResources.IsRound ? 6 : 4);
            for (var r = first; r < last; r++)
            {
                var y = r * RowH;
                var off = MonoText((r * _cols).ToString("X8"), Off, 13, "TesseraTextSecondaryBrush");
                Canvas.SetLeft(off, Pad); Canvas.SetTop(off, y + 4);
                _canvas.Children.Add(off);
                for (var k = 0; k < _cols; k++)
                {
                    var i = r * _cols + k;
                    if (i >= _data.Length) break;
                    var focus = i == _focus; var picked = i >= lo && i <= hi;
                    var tone = Highlights.FirstOrDefault(h => i >= h.From && i < h.To);
                    Brush? bg = focus ? DataUtil.Res("TesseraAccentSolidBrush") : picked ? DataUtil.Res("TesseraTextPrimaryBrush") : tone != null ? ToneSubtle(tone.Tone) : null;
                    var fg = focus ? "TesseraTextOnSolidBrush" : picked ? "TesseraBgSurfaceBrush" : "TesseraTextPrimaryBrush";
                    AddCell(TesseraCode.Hex2(_data[i]), ByteX(k), y, Cell - 2, bg, fg, radius);
                    var b = _data[i];
                    AddCell(b >= 0x20 && b < 0x7F ? ((char)b).ToString() : "·", AsciiX(k), y, Ch, bg, focus || picked ? fg : "TesseraTextSecondaryBrush", radius);
                }
            }
        }

        private void AddCell(string text, double x, double y, double w, Brush? bg, string fg, CornerRadius radius)
        {
            var g = new Grid { Width = w, Height = 22, CornerRadius = radius, Background = bg };
            g.Children.Add(new TextBlock { Text = text, FontFamily = _mono, FontSize = 13, Foreground = DataUtil.Res(fg), HorizontalAlignment = HorizontalAlignment.Center, VerticalAlignment = VerticalAlignment.Center });
            AutomationProperties.SetAccessibilityView(g, AccessibilityView.Raw);
            Canvas.SetLeft(g, x); Canvas.SetTop(g, y + 2);
            _canvas.Children.Add(g);
        }

        /// <summary>按点击位置算出是第几个字节（十六进制栏或 ASCII 栏）</summary>
        private void OnPress(object sender, PointerRoutedEventArgs e)
        {
            var p = e.GetCurrentPoint(_canvas).Position;
            var row = (int)(p.Y / RowH);
            int k = -1;
            for (var c = 0; c < _cols; c++)
                if ((p.X >= ByteX(c) && p.X < ByteX(c) + Cell) || (p.X >= AsciiX(c) && p.X < AsciiX(c) + Ch)) { k = c; break; }
            if (k < 0) return;
            var i = row * _cols + k;
            if (i >= _data.Length) return;
            _scroll.Focus(FocusState.Pointer);
            var shift = InputKeyboardSource.GetKeyStateForCurrentThread(VirtualKey.Shift).HasFlag(CoreVirtualKeyStates.Down);
            Select(i, shift);
            e.Handled = true;
        }

        private void OnKey(object sender, KeyRoutedEventArgs e)
        {
            var ctrl = InputKeyboardSource.GetKeyStateForCurrentThread(VirtualKey.Control).HasFlag(CoreVirtualKeyStates.Down);
            var shift = InputKeyboardSource.GetKeyStateForCurrentThread(VirtualKey.Shift).HasFlag(CoreVirtualKeyStates.Down);
            var page = Math.Max(1, (int)(_scroll.ViewportHeight / RowH)) * _cols;
            int? target = e.Key switch
            {
                VirtualKey.Right => _focus + 1, VirtualKey.Left => _focus - 1,
                VirtualKey.Down => _focus + _cols, VirtualKey.Up => _focus - _cols,
                VirtualKey.PageDown => _focus + page, VirtualKey.PageUp => _focus - page,
                VirtualKey.Home => ctrl ? 0 : _focus - _focus % _cols,
                VirtualKey.End => ctrl ? _data.Length - 1 : Math.Min(_data.Length - 1, _focus - _focus % _cols + _cols - 1),
                _ => null,
            };
            if (target is int t) { Select(t, shift); e.Handled = true; return; }
            if (ctrl && e.Key == VirtualKey.C) { Copy(); e.Handled = true; }
        }

        public string SelectionHex
        {
            get { var (lo, hi) = Selection; return _data.Length == 0 ? "" : string.Join(" ", _data.Skip(lo).Take(hi - lo + 1).Select(TesseraCode.Hex2)); }
        }

        private void Copy()
        {
            if (_data.Length == 0) return;
            var p = new DataPackage();
            p.SetText(SelectionHex);
            Clipboard.SetContent(p);
            var (lo, hi) = Selection;
            _status.Text = $"已复制 {hi - lo + 1} 个字节";
        }

        private void Describe()
        {
            var (lo, hi) = Selection;
            _status.Text = TesseraCode.Describe(_data, lo, hi, _focus, Highlights);
            AutomationProperties.SetHelpText(_scroll, _status.Text);
        }
    }
}
