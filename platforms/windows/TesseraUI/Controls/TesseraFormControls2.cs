using System;
using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.Linq;
using System.Text.RegularExpressions;
using Microsoft.UI.Input;
using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Automation;
using Microsoft.UI.Xaml.Automation.Peers;
using Microsoft.UI.Xaml.Automation.Provider;
using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Input;
using Microsoft.UI.Xaml.Media;
using Microsoft.UI.Xaml.Shapes;
using Windows.ApplicationModel.DataTransfer;
using Windows.Foundation;
using Windows.System;
using Windows.UI.Core;

namespace TesseraUI
{
    // ------------------------------------------------------------------
    // 标签输入 TagInput
    // ------------------------------------------------------------------

    /// <summary>
    /// 标签输入：已输入的标签是一排小砖。回车、英文 / 中文逗号、顿号、分号添加（拼音组合中不会），重复的不添加；
    /// 输入框为空时按退格，先选中最后一个标签，再按一次删除；点标签上的 × 删除。
    /// </summary>
    public sealed partial class TesseraTagInput : ContentControl
    {
        public ObservableCollection<string> Tags { get; } = new();
        public int MaxTags { get; set; } = int.MaxValue;
        public string PlaceholderText { get => _box.PlaceholderText; set => _box.PlaceholderText = value; }

        private static readonly char[] Separators = { ',', '，', '、', ';', '；' };
        private readonly TesseraWrapPanel _wrap = new() { Spacing = 6 };
        private readonly TextBox _box = new() { MinWidth = 120, MinHeight = 40, Margin = new Thickness(0, -6, 0, -6), BorderThickness = new Thickness(0), Background = new SolidColorBrush(Microsoft.UI.Colors.Transparent), PlaceholderText = "输入后回车添加" };
        private readonly Border _frame = new() { Padding = new Thickness(6), MinHeight = 40 };
        private bool _composing;
        private bool _armed;

        public TesseraTagInput()
        {
            IsTabStop = false;
            _frame.Child = _wrap;
            Content = _frame;
            Tags.CollectionChanged += (_, _) => Rebuild();
            _box.TextCompositionStarted += (_, _) => _composing = true;
            _box.TextCompositionEnded += (_, _) => { _composing = false; Split(); };
            _box.TextChanged += (_, _) => { _armed = false; if (!_composing) Split(); };
            _box.KeyDown += (_, e) =>
            {
                if (e.Key == VirtualKey.Enter && !_composing) { Add(_box.Text); _box.Text = ""; e.Handled = true; }
                else if (e.Key == VirtualKey.Back && _box.Text.Length == 0 && Tags.Count > 0)
                {
                    if (_armed) { Tags.RemoveAt(Tags.Count - 1); _armed = false; } else { _armed = true; Rebuild(); }
                    e.Handled = true;
                }
            };
            _box.GotFocus += (_, _) => Paint();
            _box.LostFocus += (_, _) => Paint();
            Loaded += (_, _) => { Rebuild(); Paint(); };
            ActualThemeChanged += (_, _) => { Rebuild(); Paint(); };
        }

        private void Add(string raw)
        {
            var t = raw.Trim();
            if (t.Length > 0 && !Tags.Contains(t) && Tags.Count < MaxTags) Tags.Add(t);
        }

        private void Split()
        {
            if (_box.Text.IndexOfAny(Separators) < 0) return;
            var parts = _box.Text.Split(Separators);
            foreach (var p in parts.Take(parts.Length - 1)) Add(p);
            _box.Text = parts[^1];
            _box.SelectionStart = _box.Text.Length;
        }

        private void Rebuild()
        {
            _wrap.Children.Clear();
            for (var i = 0; i < Tags.Count; i++)
            {
                var t = Tags[i];
                var armed = _armed && i == Tags.Count - 1;
                var remove = new Button
                {
                    Content = new TesseraIcon { Icon = "close", Size = 14 },
                    // 看得见的范围 28，可点范围 40×40（负边距伸出去，不改变标签的大小）
                    Width = 40, Height = 40, Margin = new Thickness(-6), Padding = new Thickness(0), BorderThickness = new Thickness(0),
                    Background = new SolidColorBrush(Microsoft.UI.Colors.Transparent),
                    Foreground = FormUtil.Res(armed ? "TesseraTextOnSolidBrush" : "TesseraAccentTextBrush"),
                };
                AutomationProperties.SetName(remove, $"删除标签 {t}");
                remove.Click += (_, _) => Tags.Remove(t);
                var row = new StackPanel { Orientation = Orientation.Horizontal };
                row.Children.Add(new TextBlock { Text = t, FontSize = 12, FontWeight = Microsoft.UI.Text.FontWeights.SemiBold, VerticalAlignment = VerticalAlignment.Center, Margin = new Thickness(10, 0, 0, 0), Foreground = FormUtil.Res(armed ? "TesseraTextOnSolidBrush" : "TesseraAccentTextBrush") });
                row.Children.Add(remove);
                _wrap.Children.Add(new Border { Child = row, Height = 28, CornerRadius = new CornerRadius(TesseraResources.IsRound ? 999 : 8), Background = FormUtil.Res(armed ? "TesseraAccentSolidBrush" : "TesseraAccentSubtleBrush") });
            }
            _wrap.Children.Add(_box);
            AutomationProperties.SetHelpText(_box, Tags.Count == 0 ? "没有标签" : $"已有 {Tags.Count} 个标签：{string.Join("、", Tags)}");
        }

        private void Paint()
        {
            var focused = _box.FocusState != FocusState.Unfocused;
            _frame.CornerRadius = new CornerRadius(TesseraResources.IsFlow ? 17 : TesseraResources.IsRound ? 20 : 14);
            _frame.Background = FormUtil.Res(focused ? "TesseraBgSurfaceBrush" : "TesseraFillDefaultBrush");
            _frame.BorderBrush = FormUtil.Res(focused ? "TesseraFocusRingBrush" : "TesseraBorderFieldBrush");
            _frame.BorderThickness = new Thickness(focused || !TesseraResources.IsRound ? 1 : 0);
        }
    }

    // ------------------------------------------------------------------
    // 自动补全 Autocomplete：拼音组合中不筛选
    // ------------------------------------------------------------------

    /// <summary>
    /// 自动补全：AutoSuggestBox 加上输入法处理。拼音还没选定时候选列表保持不变，选定汉字后才按包含关系筛选（不区分大小写）。
    /// </summary>
    public sealed partial class TesseraAutocomplete : ContentControl
    {
        public IList<string> Options { get; set; } = new List<string>();
        public int MaxItems { get; set; } = 6;
        public AutoSuggestBox Box { get; } = new() { QueryIcon = new SymbolIcon(Symbol.Find) };
        public event EventHandler<string>? Chosen;
        private bool _composing;

        public TesseraAutocomplete()
        {
            IsTabStop = false;
            Content = Box;
            Box.Loaded += (_, _) =>
            {
                if (Application.Current.Resources.TryGetValue("TesseraSearchBoxStyle", out var s) && s is Style style) Box.Style = style;
                // AutoSuggestBox 里面的 TextBox：监听输入法组合
                if (FindChild<TextBox>(Box) is { } inner)
                {
                    inner.TextCompositionStarted += (_, _) => _composing = true;
                    inner.TextCompositionEnded += (_, _) => { _composing = false; Filter(); };
                }
            };
            Box.TextChanged += (_, e) => { if (e.Reason == AutoSuggestionBoxTextChangeReason.UserInput && !_composing) Filter(); };
            Box.SuggestionChosen += (_, e) => Chosen?.Invoke(this, e.SelectedItem?.ToString() ?? "");
            Box.QuerySubmitted += (_, e) => { if (e.ChosenSuggestion is string s) Chosen?.Invoke(this, s); else if (Box.ItemsSource is IList<string> l && l.Count > 0) { Box.Text = l[0]; Chosen?.Invoke(this, l[0]); } };
        }

        private void Filter()
        {
            var q = Box.Text.Trim();
            var list = (q.Length == 0 ? Options : Options.Where(o => o.Contains(q, StringComparison.OrdinalIgnoreCase))).Take(MaxItems).ToList();
            Box.ItemsSource = list.Count > 0 ? list : new List<string> { $"没有匹配“{q}”的结果" };
            Box.IsSuggestionListOpen = q.Length > 0;
        }

        internal static T? FindChild<T>(DependencyObject root) where T : DependencyObject
        {
            for (var i = 0; i < VisualTreeHelper.GetChildrenCount(root); i++)
            {
                var c = VisualTreeHelper.GetChild(root, i);
                if (c is T t) return t;
                if (FindChild<T>(c) is { } found) return found;
            }
            return null;
        }
    }

    // ------------------------------------------------------------------
    // 范围滑块 RangeSlider
    // ------------------------------------------------------------------

    /// <summary>
    /// 范围滑块：两个白色小砖滑块分别调整起点和终点，不能交叉；Step &gt; 0 时按步长吸附。
    /// 每个滑块可用键盘（左右方向键、Home / End）和读屏（RangeValue 模式）调整。单值滑块请用系统 Slider + TesseraSliderStyle。
    /// </summary>
    public sealed partial class TesseraRangeSlider : ContentControl
    {
        public double Minimum { get; set; }
        public double Maximum { get; set; } = 100;
        public double Step { get; set; }
        public double Lower { get => _lower; set { _lower = Snap(value); Layout(); } }
        public double Upper { get => _upper; set { _upper = Snap(value); Layout(); } }
        public string Label { get; set; } = "范围";
        public event EventHandler? RangeChanged;

        private double _lower = 20, _upper = 80;
        private const double Thumb = 22;
        private readonly Grid _root = new() { Height = 44, Background = new SolidColorBrush(Microsoft.UI.Colors.Transparent) };
        private readonly Border _track = new() { Height = 8, CornerRadius = new CornerRadius(4), VerticalAlignment = VerticalAlignment.Center, Margin = new Thickness(Thumb / 2, 0, Thumb / 2, 0) };
        private readonly Border _fill = new() { Height = 8, CornerRadius = new CornerRadius(4), VerticalAlignment = VerticalAlignment.Center, HorizontalAlignment = HorizontalAlignment.Left };
        private readonly RangeThumb _a, _b;
        private RangeThumb? _drag;

        public TesseraRangeSlider()
        {
            IsTabStop = false;
            _a = new RangeThumb(this, 0);
            _b = new RangeThumb(this, 1);
            _root.Children.Add(_track);
            _root.Children.Add(_fill);
            _root.Children.Add(_a);
            _root.Children.Add(_b);
            Content = _root;
            _root.SizeChanged += (_, _) => Layout();
            _root.PointerPressed += (_, e) =>
            {
                var v = ValueAt(e.GetCurrentPoint(_root).Position.X);
                _drag = Math.Abs(v - _lower) <= Math.Abs(v - _upper) ? _a : _b;
                _root.CapturePointer(e.Pointer);
                Set(_drag.Index, v);
            };
            _root.PointerMoved += (_, e) => { if (_drag != null) Set(_drag.Index, ValueAt(e.GetCurrentPoint(_root).Position.X)); };
            _root.PointerReleased += (_, e) => { _drag = null; _root.ReleasePointerCapture(e.Pointer); };
            Loaded += (_, _) => { Paint(); Layout(); };
            ActualThemeChanged += (_, _) => Paint();
        }

        private double Usable => Math.Max(1, _root.ActualWidth - Thumb);
        private double ValueAt(double x) => Minimum + Math.Clamp((x - Thumb / 2) / Usable, 0, 1) * (Maximum - Minimum);
        private double Snap(double v)
        {
            v = Math.Clamp(v, Minimum, Maximum);
            return Step > 0 ? Math.Clamp(Minimum + Math.Round((v - Minimum) / Step) * Step, Minimum, Maximum) : v;
        }

        internal double ValueOf(int i) => i == 0 ? _lower : _upper;
        internal void Set(int i, double v)
        {
            v = Snap(v);
            if (i == 0) v = Math.Min(v, _upper); else v = Math.Max(v, _lower);
            if (v == ValueOf(i)) return;
            if (i == 0) _lower = v; else _upper = v;
            Layout();
            RangeChanged?.Invoke(this, EventArgs.Empty);
        }
        internal double SmallStep => Step > 0 ? Step : (Maximum - Minimum) / 100;

        private void Layout()
        {
            if (_root.ActualWidth <= 0) return;
            double F(double v) => (v - Minimum) / Math.Max(1e-9, Maximum - Minimum);
            // 滑块的可点范围 44，比看得见的 22 大一圈：向左多出 11
            _a.Margin = new Thickness(Usable * F(_lower) - 11, 0, 0, 0);
            _b.Margin = new Thickness(Usable * F(_upper) - 11, 0, 0, 0);
            _fill.Margin = new Thickness(Thumb / 2 + Usable * F(_lower), 0, 0, 0);
            _fill.Width = Math.Max(0, Usable * (F(_upper) - F(_lower)));
        }

        private void Paint()
        {
            _track.Background = FormUtil.Res("TesseraFillControlOffBrush");
            _fill.Background = FormUtil.Res("TesseraAccentControlBrush");
            foreach (var t in new[] { _a, _b })
            {
                t.Visual.CornerRadius = new CornerRadius(TesseraResources.IsRound ? 11 : 8);
                t.Visual.Background = new SolidColorBrush(Microsoft.UI.Colors.White);
                t.Visual.BorderBrush = FormUtil.Res("TesseraBorderFieldBrush");
            }
        }

        /// <summary>范围滑块里的一个滑块：可聚焦、方向键调整、读屏 RangeValue</summary>
        internal sealed partial class RangeThumb : Grid
        {
            public readonly TesseraRangeSlider Owner;
            public readonly int Index;
            /// <summary>看得见的 22 白色小砖</summary>
            public readonly Border Visual = new() { Width = Thumb, Height = Thumb, BorderThickness = new Thickness(1), HorizontalAlignment = HorizontalAlignment.Center, VerticalAlignment = VerticalAlignment.Center };
            public RangeThumb(TesseraRangeSlider owner, int index)
            {
                Owner = owner; Index = index;
                Width = 44; Height = 44;
                Background = new SolidColorBrush(Microsoft.UI.Colors.Transparent);
                Children.Add(Visual);
                HorizontalAlignment = HorizontalAlignment.Left;
                VerticalAlignment = VerticalAlignment.Center;
                IsTabStop = true;
                UseSystemFocusVisuals = true;
                KeyDown += (_, e) =>
                {
                    var d = e.Key switch { VirtualKey.Left or VirtualKey.Down => -Owner.SmallStep, VirtualKey.Right or VirtualKey.Up => Owner.SmallStep, _ => 0 };
                    if (d != 0) { Owner.Set(Index, Owner.ValueOf(Index) + d); e.Handled = true; }
                    if (e.Key == VirtualKey.Home) { Owner.Set(Index, Owner.Minimum); e.Handled = true; }
                    if (e.Key == VirtualKey.End) { Owner.Set(Index, Owner.Maximum); e.Handled = true; }
                };
            }
            protected override AutomationPeer OnCreateAutomationPeer() => new Peer(this);

            private sealed partial class Peer : FrameworkElementAutomationPeer, IRangeValueProvider
            {
                private readonly RangeThumb _t;
                public Peer(RangeThumb t) : base(t) { _t = t; }
                protected override string GetNameCore() => $"{_t.Owner.Label}{(_t.Index == 0 ? "起点" : "终点")}";
                protected override AutomationControlType GetAutomationControlTypeCore() => AutomationControlType.Slider;
                protected override object GetPatternCore(PatternInterface p) => p == PatternInterface.RangeValue ? this : base.GetPatternCore(p);
                public bool IsReadOnly => false;
                public double LargeChange => _t.Owner.SmallStep * 10;
                public double Maximum => _t.Index == 0 ? _t.Owner.Upper : _t.Owner.Maximum;
                public double Minimum => _t.Index == 0 ? _t.Owner.Minimum : _t.Owner.Lower;
                public double SmallChange => _t.Owner.SmallStep;
                public double Value => _t.Owner.ValueOf(_t.Index);
                public void SetValue(double value) => _t.Owner.Set(_t.Index, value);
            }
        }
    }

    // ------------------------------------------------------------------
    // 文件上传 FileUpload
    // ------------------------------------------------------------------

    /// <summary>
    /// 文件上传：虚线框的凹槽区域，可点击（触发 PickRequested，由应用用 FileOpenPicker 选择）或把文件拖进来（Dropped）。
    /// 选中的文件由应用放进 Files，每个文件一块砖，带进度和删除。
    /// </summary>
    public sealed partial class TesseraFileUpload : ContentControl
    {
        public sealed class Item { public string Name { get; set; } = ""; public long Size { get; set; } public double? Progress { get; set; } public string? Error { get; set; } }

        public ObservableCollection<Item> Files { get; } = new();
        public string Title { get; set; } = "点击选择或拖入文件";
        public string Hint { get; set; } = "支持 PDF、图片，单个不超过 20 MB";
        public event EventHandler? PickRequested;
        public event EventHandler<IReadOnlyList<Windows.Storage.IStorageItem>>? Dropped;

        private readonly StackPanel _root = new() { Spacing = 12 };
        private readonly Button _zone = new() { HorizontalAlignment = HorizontalAlignment.Stretch, HorizontalContentAlignment = HorizontalAlignment.Center, Padding = new Thickness(24, 32, 24, 32) };
        private readonly StackPanel _list = new() { Spacing = 3 };

        public TesseraFileUpload()
        {
            IsTabStop = false;
            _root.Children.Add(_zone);
            _root.Children.Add(_list);
            Content = _root;
            _zone.Click += (_, _) => PickRequested?.Invoke(this, EventArgs.Empty);
            _zone.AllowDrop = true;
            _zone.DragOver += (_, e) => { e.AcceptedOperation = DataPackageOperation.Copy; _zone.Background = FormUtil.Res("TesseraAccentSubtleBrush"); };
            _zone.DragLeave += (_, _) => _zone.Background = FormUtil.Res("TesseraFillDefaultBrush");
            _zone.Drop += async (_, e) =>
            {
                _zone.Background = FormUtil.Res("TesseraFillDefaultBrush");
                if (e.DataView.Contains(StandardDataFormats.StorageItems)) Dropped?.Invoke(this, await e.DataView.GetStorageItemsAsync());
            };
            Files.CollectionChanged += (_, _) => Rebuild();
            Loaded += (_, _) => { Paint(); Rebuild(); };
            ActualThemeChanged += (_, _) => { Paint(); Rebuild(); };
        }

        private void Paint()
        {
            var col = new StackPanel { Spacing = 8, HorizontalAlignment = HorizontalAlignment.Center };
            col.Children.Add(new TesseraIcon { Icon = "upload", Size = 32, Foreground = FormUtil.Res("TesseraAccentTextBrush"), HorizontalAlignment = HorizontalAlignment.Center });
            col.Children.Add(new TextBlock { Text = Title, FontSize = 14, FontWeight = Microsoft.UI.Text.FontWeights.SemiBold, HorizontalAlignment = HorizontalAlignment.Center, Foreground = FormUtil.Res("TesseraTextPrimaryBrush") });
            col.Children.Add(new TextBlock { Text = Hint, FontSize = 12, HorizontalAlignment = HorizontalAlignment.Center, Foreground = FormUtil.Res("TesseraTextSecondaryBrush") });
            var grid = new Grid();
            grid.Children.Add(new Rectangle { Stroke = FormUtil.Res("TesseraBorderStrongBrush"), StrokeThickness = 1.5, StrokeDashArray = new DoubleCollection { 4, 3 }, RadiusX = 10, RadiusY = 10, Margin = new Thickness(-16, -24, -16, -24) });
            grid.Children.Add(col);
            _zone.Content = grid;
            _zone.Background = FormUtil.Res("TesseraFillDefaultBrush");
            _zone.BorderThickness = new Thickness(0);
            _zone.CornerRadius = new CornerRadius(TesseraResources.IsFlow ? 18 : TesseraResources.IsRound ? 22 : 18);
            AutomationProperties.SetName(_zone, Title);
            AutomationProperties.SetHelpText(_zone, Hint);
        }

        private void Rebuild()
        {
            _list.Children.Clear();
            for (var i = 0; i < Files.Count; i++)
            {
                var f = Files[i];
                var status = f.Error ?? (f.Progress is double p ? $"上传中 {(int)(p * 100)}%" : Size(f.Size));
                var text = new StackPanel { VerticalAlignment = VerticalAlignment.Center };
                text.Children.Add(new TextBlock { Text = f.Name, FontSize = 14, FontWeight = Microsoft.UI.Text.FontWeights.SemiBold, Foreground = FormUtil.Res("TesseraTextPrimaryBrush") });
                text.Children.Add(new TextBlock { Text = status, FontSize = 12, Foreground = FormUtil.Res(f.Error != null ? "TesseraDangerTextBrush" : "TesseraTextSecondaryBrush") });
                if (f.Progress is double pr) text.Children.Add(new ProgressBar { Value = pr * 100, Maximum = 100, Margin = new Thickness(0, 6, 0, 0) });
                var remove = new Button { Content = new TesseraIcon { Icon = "close", Size = 18 }, Width = 44, Height = 44, Padding = new Thickness(0), BorderThickness = new Thickness(0), Background = new SolidColorBrush(Microsoft.UI.Colors.Transparent) };
                AutomationProperties.SetName(remove, $"移除 {f.Name}");
                remove.Click += (_, _) => Files.Remove(f);
                var row = new Grid { ColumnSpacing = 12, Padding = new Thickness(16, 8, 8, 8) };
                row.ColumnDefinitions.Add(new ColumnDefinition { Width = GridLength.Auto });
                row.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(1, GridUnitType.Star) });
                row.ColumnDefinitions.Add(new ColumnDefinition { Width = GridLength.Auto });
                var icon = new TesseraIcon { Icon = f.Error != null ? "error" : "file-text", Size = 22, Foreground = FormUtil.Res(f.Error != null ? "TesseraDangerTextBrush" : "TesseraAccentTextBrush"), VerticalAlignment = VerticalAlignment.Center };
                row.Children.Add(icon);
                Grid.SetColumn(text, 1); row.Children.Add(text);
                Grid.SetColumn(remove, 2); row.Children.Add(remove);
                var first = i == 0; var last = i == Files.Count - 1;
                var r = TesseraResources.IsRound ? 18.0 : 18.0;
                _list.Children.Add(new Border
                {
                    Child = row,
                    Background = FormUtil.Res("TesseraBgSurfaceBrush"),
                    CornerRadius = new CornerRadius(first ? r : 6, first ? r : 6, last ? r : 6, last ? r : 6),
                });
            }
            _list.Spacing = TesseraResources.IsRound ? 0 : 3;
        }

        private static string Size(long b) => b >= 1 << 20 ? $"{b / 1048576.0:F1} MB" : b >= 1024 ? $"{b / 1024} KB" : $"{b} B";
    }

    // ------------------------------------------------------------------
    // 表单字段与校验
    // ------------------------------------------------------------------

    /// <summary>常用校验规则，提示语与 Web、Android、Apple 一致。返回 null 表示通过</summary>
    public static class TesseraRules
    {
        public static Func<string, string?> Required(string name) => v => string.IsNullOrWhiteSpace(v) ? $"请填写{name}" : null;
        public static Func<string, string?> MinLength(int n) => v => v.Length > 0 && v.Length < n ? $"至少 {n} 个字" : null;
        public static Func<string, string?> MaxLength(int n) => v => v.Length > n ? $"最多 {n} 个字，已超出 {v.Length - n} 个" : null;
        public static readonly Func<string, string?> Email = v => v.Length > 0 && !Regex.IsMatch(v, @"^[^@\s]+@[^@\s]+\.[^@\s]+$") ? "邮箱格式不正确" : null;
        /// <summary>中国大陆手机号：1 开头 11 位，允许空格和连字符分隔、全角数字</summary>
        public static readonly Func<string, string?> Phone = v =>
        {
            var d = new string(FormUtil.NormalizeDigits(v).Where(char.IsAsciiDigit).ToArray());
            return v.Length > 0 && !Regex.IsMatch(d, @"^1[3-9]\d{9}$") ? "请输入 11 位手机号" : null;
        };
        /// <summary>依次检查，返回第一个错误</summary>
        public static string? Check(string value, params Func<string, string?>[] rules) => rules.Select(r => r(value)).FirstOrDefault(e => e != null);
    }

    /// <summary>
    /// 表单字段：标签（必填时后面有红色“*”，读屏读作“必填”）+ 内容 + 帮助或错误提示（错误时红色，并通过 UI 自动化通知朗读）
    /// </summary>
    public sealed partial class TesseraFormField : ContentControl
    {
        public static readonly DependencyProperty FieldProperty =
            DependencyProperty.Register(nameof(Field), typeof(UIElement), typeof(TesseraFormField), new PropertyMetadata(null, (d, e) => ((TesseraFormField)d)._slot.Content = e.NewValue));
        public static readonly DependencyProperty ErrorProperty =
            DependencyProperty.Register(nameof(Error), typeof(string), typeof(TesseraFormField), new PropertyMetadata(null, (d, _) => ((TesseraFormField)d).Paint()));

        public UIElement? Field { get => (UIElement?)GetValue(FieldProperty); set => SetValue(FieldProperty, value); }
        public string? Error { get => (string?)GetValue(ErrorProperty); set => SetValue(ErrorProperty, value); }
        public string Label { get => _label.Text; set => _label.Text = value; }
        public bool Required { get => _star.Visibility == Visibility.Visible; set => _star.Visibility = value ? Visibility.Visible : Visibility.Collapsed; }
        public string? Help { get; set; }

        private readonly TextBlock _label = new() { FontSize = 14, FontWeight = Microsoft.UI.Text.FontWeights.SemiBold };
        private readonly TextBlock _star = new() { Text = " *", FontSize = 14, FontWeight = Microsoft.UI.Text.FontWeights.SemiBold, Visibility = Visibility.Collapsed };
        private readonly ContentPresenter _slot = new();
        private readonly TextBlock _message = new() { FontSize = 12, TextWrapping = TextWrapping.Wrap };

        public TesseraFormField()
        {
            IsTabStop = false;
            AutomationProperties.SetName(_star, "必填");
            var head = new StackPanel { Orientation = Orientation.Horizontal };
            head.Children.Add(_label);
            head.Children.Add(_star);
            var col = new StackPanel { Spacing = 6 };
            col.Children.Add(head);
            col.Children.Add(_slot);
            col.Children.Add(_message);
            Content = col;
            Loaded += (_, _) => Paint();
            ActualThemeChanged += (_, _) => Paint();
        }

        private void Paint()
        {
            _label.Foreground = FormUtil.Res("TesseraTextPrimaryBrush");
            _star.Foreground = FormUtil.Res("TesseraDangerTextBrush");
            var msg = Error ?? Help;
            _message.Text = msg ?? "";
            _message.Visibility = string.IsNullOrEmpty(msg) ? Visibility.Collapsed : Visibility.Visible;
            _message.Foreground = FormUtil.Res(Error != null ? "TesseraDangerTextBrush" : "TesseraTextSecondaryBrush");
            if (Error != null && FrameworkElementAutomationPeer.FromElement(this) is { } peer)
                peer.RaiseNotificationEvent(AutomationNotificationKind.Other, AutomationNotificationProcessing.ImportantMostRecent, $"{Label}：{Error}", "TesseraFormFieldError");
        }
    }

    /// <summary>密码框附加属性：CapsLockWarning = true 时，大写锁定打开会在描述（Description）里显示“大写锁定已打开”</summary>
    public static class TesseraPassword
    {
        public static readonly DependencyProperty CapsLockWarningProperty =
            DependencyProperty.RegisterAttached("CapsLockWarning", typeof(bool), typeof(TesseraPassword), new PropertyMetadata(false, OnChanged));
        public static bool GetCapsLockWarning(DependencyObject o) => (bool)o.GetValue(CapsLockWarningProperty);
        public static void SetCapsLockWarning(DependencyObject o, bool v) => o.SetValue(CapsLockWarningProperty, v);

        private static void OnChanged(DependencyObject d, DependencyPropertyChangedEventArgs e)
        {
            if (d is not PasswordBox box || !(bool)e.NewValue) return;
            void Check()
            {
                var on = (InputKeyboardSource.GetKeyStateForCurrentThread(VirtualKey.CapitalLock) & CoreVirtualKeyStates.Locked) != 0;
                box.Description = on ? "大写锁定已打开" : null;
            }
            box.KeyUp += (_, _) => Check();
            box.GotFocus += (_, _) => Check();
            box.LostFocus += (_, _) => box.Description = null;
        }
    }
}
