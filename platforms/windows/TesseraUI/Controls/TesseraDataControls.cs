using System;
using System.Collections.Generic;
using System.Linq;
using System.Numerics;
using Microsoft.UI.Composition;
using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Automation;
using Microsoft.UI.Xaml.Automation.Peers;
using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Controls.Primitives;
using Microsoft.UI.Xaml.Hosting;
using Microsoft.UI.Xaml.Input;
using Microsoft.UI.Xaml.Media;
using Windows.ApplicationModel.DataTransfer;
using Windows.System;

namespace TesseraUI
{
    /// <summary>把同一个元素从上一次重建的容器移到新容器：WinUI 里不在可视树上的元素 Parent 可能是 null，所以记住上一次的容器</summary>
    internal sealed class Reparent
    {
        private Panel? _host;

        public void Into(Panel host, UIElement? el)
        {
            if (el == null) return;
            _host?.Children.Remove(el);
            host.Children.Add(el);
            _host = host;
        }
    }

    // ------------------------------------------------------------------
    // 折叠面板 Accordion：系统 Expander 排成一列砖；圆融是一整块面板，展开的那一行底色加深
    // ------------------------------------------------------------------

    /// <summary>折叠面板。Add(标题, 内容) 添加一项；Single 时同时只开一个</summary>
    public sealed partial class TesseraAccordion : ContentControl
    {
        public bool Single { get; set; }
        private readonly List<Expander> _items = new();
        private readonly StackPanel _panel = new();
        private readonly Grid _shell = new();

        public TesseraAccordion()
        {
            IsTabStop = false;
            HorizontalContentAlignment = HorizontalAlignment.Stretch;
            _shell.Children.Add(_panel);
            Content = _shell;
            Loaded += (_, _) => Paint();
            ActualThemeChanged += (_, _) => Paint();
        }

        public Expander Add(string header, UIElement content, bool expanded = false)
        {
            var body = new Grid { Padding = new Thickness(0, 0, 0, 4) };
            if (content is TextBlock t) { t.Foreground = DataUtil.Res("TesseraTextSecondaryBrush"); t.TextWrapping = TextWrapping.Wrap; }
            body.Children.Add(content);
            var e = new Expander
            {
                Header = new TextBlock { Text = header, FontSize = 15, FontWeight = Microsoft.UI.Text.FontWeights.SemiBold },
                Content = body, IsExpanded = expanded,
                HorizontalAlignment = HorizontalAlignment.Stretch, HorizontalContentAlignment = HorizontalAlignment.Stretch,
            };
            AutomationProperties.SetName(e, header);
            e.Expanding += (_, _) =>
            {
                if (Single) foreach (var other in _items) if (other != e) other.IsExpanded = false;
                Paint();
            };
            e.Collapsed += (_, _) => Paint();
            _items.Add(e);
            _panel.Children.Add(e);
            Paint();
            return e;
        }

        private void Paint()
        {
            var round = TesseraResources.IsRound;
            _panel.Spacing = round ? 0 : 3;
            _shell.Padding = new Thickness(round ? 6 : 0);
            _shell.CornerRadius = new CornerRadius(round ? (TesseraResources.IsFlow ? 26 : 28) : 0);
            _shell.Background = round ? DataUtil.Res("TesseraBgSurfaceBrush") : null;
            for (var i = 0; i < _items.Count; i++)
            {
                var e = _items[i];
                var open = e.IsExpanded;
                var fill = round ? (open ? DataUtil.Res("TesseraFillDefaultBrush") : new SolidColorBrush(Microsoft.UI.Colors.Transparent)) : DataUtil.Res("TesseraBgSurfaceBrush");
                var r = round ? DataUtil.Row(0, 1, true, true) : DataUtil.Row(i, _items.Count, true, true);
                e.CornerRadius = r;
                e.Resources["ExpanderHeaderBackground"] = fill;
                e.Resources["ExpanderContentBackground"] = fill;
                e.Resources["ExpanderHeaderBorderBrush"] = round ? fill : DataUtil.Res("TesseraBorderSurfaceBrush");
                e.Resources["ExpanderContentBorderBrush"] = round ? fill : DataUtil.Res("TesseraBorderSurfaceBrush");
                e.Resources["ExpanderHeaderForeground"] = DataUtil.Res("TesseraTextPrimaryBrush");
                e.Resources["ExpanderChevronForeground"] = DataUtil.Res("TesseraTextTertiaryBrush");
                e.Resources["ExpanderMinHeight"] = 56.0;
                e.Resources["ExpanderHeaderPadding"] = new Thickness(round ? 16 : 20, 0, 12, 0);
                e.Resources["ExpanderContentPadding"] = new Thickness(round ? 16 : 20, 0, round ? 16 : 20, round ? 12 : 16);
                e.Resources["ExpanderContentDownBorderThickness"] = new Thickness(round ? 0 : 1, 0, round ? 0 : 1, round ? 0 : 1);
                e.Resources["ExpanderHeaderBorderThickness"] = new Thickness(round ? 0 : 1);
            }
        }
    }

    // ------------------------------------------------------------------
    // 描述列表 DescriptionList：每一项是左右两块砖（标签 / 值）；Stacked 时上下排
    // ------------------------------------------------------------------

    public sealed partial class TesseraDescriptionList : ContentControl
    {
        private readonly List<(string Label, string Value)> _items = new();
        public double LabelWidth { get; set; } = 96;
        /// <summary>上下排；不设时宽度小于 360 自动上下排</summary>
        public bool? Stacked { get; set; }
        private bool _stacked;

        public TesseraDescriptionList()
        {
            IsTabStop = false;
            HorizontalContentAlignment = HorizontalAlignment.Stretch;
            Loaded += (_, _) => Rebuild();
            ActualThemeChanged += (_, _) => Rebuild();
            SizeChanged += (_, e) => { if ((Stacked ?? e.NewSize.Width < 360) != _stacked) Rebuild(); };
        }

        public void Add(string label, string value) { _items.Add((label, value)); Rebuild(); }

        private void Rebuild()
        {
            var round = TesseraResources.IsRound;
            _stacked = Stacked ?? (ActualWidth > 0 && ActualWidth < 360);
            var panel = new StackPanel { Spacing = round ? 0 : 3 };
            if (round) { panel.Background = DataUtil.Res("TesseraBgSurfaceBrush"); panel.CornerRadius = new CornerRadius(TesseraResources.IsFlow ? 18 : 22); panel.Padding = new Thickness(0, 6, 0, 6); }
            var fill = round ? null : DataUtil.Res("TesseraBgSurfaceBrush");
            for (var i = 0; i < _items.Count; i++)
            {
                var (label, value) = _items[i];
                var l = DataUtil.Text(label, brush: "TesseraTextSecondaryBrush");
                var v = DataUtil.Text(value);
                v.FontWeight = Microsoft.UI.Text.FontWeights.Medium;
                FrameworkElement row;
                if (_stacked)
                {
                    var s = new StackPanel { Spacing = 2, Padding = new Thickness(16, 10, 16, 10), Background = fill, CornerRadius = round ? new CornerRadius(0) : DataUtil.Row(i, _items.Count, true, true) };
                    s.Children.Add(l); s.Children.Add(v);
                    row = s;
                }
                else
                {
                    var g = new Grid { ColumnSpacing = round ? 0 : 3 };
                    g.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(LabelWidth) });
                    g.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(1, GridUnitType.Star) });
                    var lc = new Grid { Padding = new Thickness(16, 12, 16, 12), Background = fill, CornerRadius = round ? new CornerRadius(0) : DataUtil.Row(i, _items.Count, true, false) };
                    var vc = new Grid { Padding = new Thickness(16, 12, 16, 12), Background = fill, CornerRadius = round ? new CornerRadius(0) : DataUtil.Row(i, _items.Count, false, true) };
                    if (!round) { lc.CornerRadius = With(lc.CornerRadius, right: 6); vc.CornerRadius = With(vc.CornerRadius, left: 6); }
                    lc.Children.Add(l); vc.Children.Add(v);
                    Grid.SetColumn(vc, 1);
                    g.Children.Add(lc); g.Children.Add(vc);
                    row = g;
                }
                AutomationProperties.SetName(row, $"{label}：{value}");
                panel.Children.Add(row);
            }
            Content = panel;
        }

        private static CornerRadius With(CornerRadius c, double? left = null, double? right = null) =>
            new(left ?? c.TopLeft, right ?? c.TopRight, right ?? c.BottomRight, left ?? c.BottomLeft);
    }

    // ------------------------------------------------------------------
    // 时间线 Timeline：小砖节点 + 竖向砖缝连线
    // ------------------------------------------------------------------

    public sealed partial class TesseraTimeline : ContentControl
    {
        private readonly List<(string Title, string? Time, string? Body, string Tone)> _items = new();

        public TesseraTimeline()
        {
            IsTabStop = false;
            Loaded += (_, _) => Rebuild();
            ActualThemeChanged += (_, _) => Rebuild();
        }

        /// <param name="tone">neutral / accent / success / warning / danger</param>
        public void Add(string title, string? time = null, string? body = null, string tone = "neutral") { _items.Add((title, time, body, tone)); Rebuild(); }

        private void Rebuild()
        {
            var round = TesseraResources.IsRound;
            var panel = new StackPanel();
            for (var i = 0; i < _items.Count; i++)
            {
                var (title, time, body, tone) = _items[i];
                var last = i == _items.Count - 1;
                var g = new Grid { ColumnSpacing = 12 };
                g.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(28) });
                g.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(1, GridUnitType.Star) });
                var rail = new Grid();
                if (!last) rail.Children.Add(new Grid { Width = 3, Margin = new Thickness(0, 22, 0, 0), CornerRadius = new CornerRadius(1.5), Background = DataUtil.Res("TesseraFillDefaultBrush"), HorizontalAlignment = HorizontalAlignment.Center });
                rail.Children.Add(new Grid { Width = 14, Height = 14, Margin = new Thickness(0, 4, 0, 0), CornerRadius = new CornerRadius(round ? 7 : 5), Background = DataUtil.Tone(tone), HorizontalAlignment = HorizontalAlignment.Center, VerticalAlignment = VerticalAlignment.Top });
                g.Children.Add(rail);
                var text = new StackPanel { Padding = new Thickness(0, 1, 0, last ? 0 : 20) };
                if (time != null) text.Children.Add(DataUtil.Text(time, "TesseraCaptionTextStyle", "TesseraTextTertiaryBrush"));
                text.Children.Add(DataUtil.Text(title, "TesseraLabelTextStyle"));
                if (body != null) { var b = DataUtil.Text(body, brush: "TesseraTextSecondaryBrush"); b.Margin = new Thickness(0, 2, 0, 0); text.Children.Add(b); }
                Grid.SetColumn(text, 1);
                g.Children.Add(text);
                AutomationProperties.SetName(g, string.Join("，", new[] { time, title, body }.Where(s => !string.IsNullOrEmpty(s))));
                panel.Children.Add(g);
            }
            Content = panel;
        }
    }

    // ------------------------------------------------------------------
    // 统计数字 Stat：一组大数字砖，外角大；圆融是一整块面板
    // ------------------------------------------------------------------

    public sealed partial class TesseraStats : ContentControl
    {
        private readonly List<(string Label, string Value, string? Unit, string? Delta, int Trend)> _items = new();
        public int Columns { get; set; } = 2;

        public TesseraStats()
        {
            IsTabStop = false;
            HorizontalContentAlignment = HorizontalAlignment.Stretch;
            Loaded += (_, _) => Rebuild();
            ActualThemeChanged += (_, _) => Rebuild();
        }

        /// <param name="trend">1 上升，-1 下降，0 不显示箭头</param>
        public void Add(string label, string value, string? unit = null, string? delta = null, int trend = 0) { _items.Add((label, value, unit, delta, trend)); Rebuild(); }

        private void Rebuild()
        {
            var round = TesseraResources.IsRound;
            var cols = Math.Max(1, Columns);
            var rows = (_items.Count + cols - 1) / cols;
            var g = new Grid { RowSpacing = round ? 0 : 3, ColumnSpacing = round ? 0 : 3 };
            if (round) { g.Background = DataUtil.Res("TesseraBgSurfaceBrush"); g.CornerRadius = new CornerRadius(TesseraResources.IsFlow ? 26 : 28); }
            for (var c = 0; c < cols; c++) g.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(1, GridUnitType.Star) });
            for (var r = 0; r < rows; r++) g.RowDefinitions.Add(new RowDefinition { Height = GridLength.Auto });
            for (var i = 0; i < _items.Count; i++)
            {
                var (label, value, unit, delta, trend) = _items[i];
                int r = i / cols, c = i % cols;
                var lastInRow = Math.Min(cols, _items.Count - r * cols) - 1;
                double xl = 24, s = 6;
                var tile = new StackPanel { Spacing = 4, Padding = new Thickness(20) };
                if (!round)
                {
                    tile.Background = DataUtil.Res("TesseraBgSurfaceBrush");
                    tile.BorderBrush = DataUtil.Res("TesseraBorderSurfaceBrush");
                    tile.BorderThickness = new Thickness(1);
                    tile.CornerRadius = new CornerRadius(
                        r == 0 && c == 0 ? xl : s, r == 0 && (c == lastInRow || c == cols - 1) ? xl : s,
                        r == rows - 1 && c == lastInRow ? xl : s, r == rows - 1 && c == 0 ? xl : s);
                }
                tile.Children.Add(DataUtil.Text(label, "TesseraCaptionTextStyle", "TesseraTextSecondaryBrush"));
                var line = new StackPanel { Orientation = Orientation.Horizontal, Spacing = 4 };
                var big = new TextBlock { Text = value, FontSize = 32, FontWeight = Microsoft.UI.Text.FontWeights.SemiBold, Foreground = DataUtil.Res("TesseraTextPrimaryBrush") };
                if (Application.Current.Resources.TryGetValue("TesseraDisplayFontFamily", out var f) && f is FontFamily ff) big.FontFamily = ff;
                line.Children.Add(big);
                if (unit != null) line.Children.Add(new TextBlock { Text = unit, FontSize = 14, FontWeight = Microsoft.UI.Text.FontWeights.Medium, Foreground = DataUtil.Res("TesseraTextSecondaryBrush"), VerticalAlignment = VerticalAlignment.Bottom, Margin = new Thickness(0, 0, 0, 6) });
                tile.Children.Add(line);
                if (delta != null)
                {
                    var fg = trend > 0 ? "TesseraSuccessTextBrush" : trend < 0 ? "TesseraDangerTextBrush" : "TesseraTextSecondaryBrush";
                    var d = new StackPanel { Orientation = Orientation.Horizontal, Spacing = 2 };
                    if (trend != 0) d.Children.Add(new TesseraIcon { Icon = trend > 0 ? "arrow-up" : "arrow-down", Size = 14, Foreground = DataUtil.Res(fg) });
                    d.Children.Add(new TextBlock { Text = delta, FontSize = 12, FontWeight = Microsoft.UI.Text.FontWeights.SemiBold, Foreground = DataUtil.Res(fg) });
                    tile.Children.Add(d);
                }
                var spoken = $"{label} {value}{unit}" + (delta != null ? $"，较上期{(trend > 0 ? "上升" : trend < 0 ? "下降" : "")} {delta}" : "");
                AutomationProperties.SetName(tile, spoken);
                Grid.SetRow(tile, r); Grid.SetColumn(tile, c);
                g.Children.Add(tile);
            }
            Content = g;
        }
    }

    // ------------------------------------------------------------------
    // 进度 Progress：系统 ProgressBar / ProgressRing 套上 TesseraUI 外观，加上说明与百分比
    // ------------------------------------------------------------------

    /// <summary>线形进度：Value 为 null 时是不确定进度</summary>
    public sealed partial class TesseraProgress : ContentControl
    {
        public ProgressBar Bar { get; } = new() { Minimum = 0, Maximum = 1, Height = 8, MinHeight = 8 };
        private readonly TextBlock _label = new();
        private readonly TextBlock _value = new();

        public TesseraProgress()
        {
            IsTabStop = false;
            HorizontalContentAlignment = HorizontalAlignment.Stretch;
            var head = new Grid();
            head.Children.Add(_label);
            _value.HorizontalAlignment = HorizontalAlignment.Right;
            head.Children.Add(_value);
            var s = new StackPanel { Spacing = 6 };
            s.Children.Add(head);
            s.Children.Add(Bar);
            Content = s;
            // 轨道与进度条的高度要在模板加载之前设置（加载后再改资源不会生效）
            Bar.Resources["ProgressBarTrackHeight"] = 8.0;
            Bar.Resources["ProgressBarMinHeight"] = 8.0;
            Bar.Resources["ProgressBarTrackCornerRadius"] = new CornerRadius(4);
            Loaded += (_, _) => Paint();
            ActualThemeChanged += (_, _) => Paint();
        }

        public string Label { get => _label.Text; set { _label.Text = value; AutomationProperties.SetName(Bar, value); } }

        public double? Value
        {
            get => Bar.IsIndeterminate ? null : Bar.Value;
            set
            {
                Bar.IsIndeterminate = value == null;
                if (value != null) Bar.Value = Math.Clamp(value.Value, 0, 1);
                _value.Text = value == null ? "" : $"{Math.Round(value.Value * 100)}%";
            }
        }

        /// <summary>accent / success / danger</summary>
        public string Tone { get; set; } = "accent";

        private void Paint()
        {
            foreach (var t in new[] { _label, _value }) { if (DataUtil.StyleRes("TesseraCaptionTextStyle") is { } s) t.Style = s; t.Foreground = DataUtil.Res("TesseraTextSecondaryBrush"); }
            Bar.CornerRadius = new CornerRadius(4);
            Bar.Foreground = Tone == "accent" ? DataUtil.Res("TesseraAccentControlBrush") : DataUtil.Tone(Tone);
            Bar.Background = DataUtil.Res("TesseraFillControlOffBrush");
        }
    }

    /// <summary>环形进度：中间显示百分比（96 以上用标题字体）</summary>
    public sealed partial class TesseraProgressRing : ContentControl
    {
        public ProgressRing Ring { get; } = new() { IsIndeterminate = false, Minimum = 0, Maximum = 1 };
        private readonly TextBlock _text = new() { HorizontalAlignment = HorizontalAlignment.Center, VerticalAlignment = VerticalAlignment.Center };

        public TesseraProgressRing()
        {
            IsTabStop = false;
            var g = new Grid();
            g.Children.Add(Ring);
            g.Children.Add(_text);
            Content = g;
            Size = 48;
            Loaded += (_, _) => Paint();
            ActualThemeChanged += (_, _) => Paint();
        }

        public double Size { get => Ring.Width; set { Ring.Width = value; Ring.Height = value; Paint(); } }
        public double Value { get => Ring.Value; set { Ring.Value = Math.Clamp(value, 0, 1); _text.Text = $"{Math.Round(Ring.Value * 100)}%"; AutomationProperties.SetItemStatus(Ring, _text.Text); } }
        public string Label { get => AutomationProperties.GetName(Ring); set => AutomationProperties.SetName(Ring, value); }

        private void Paint()
        {
            var big = Ring.Width >= 80;
            _text.FontSize = big ? 22 : 12;
            _text.FontWeight = Microsoft.UI.Text.FontWeights.SemiBold;
            if (big && Application.Current?.Resources.TryGetValue("TesseraDisplayFontFamily", out var f) == true && f is FontFamily ff) _text.FontFamily = ff;
            _text.Foreground = DataUtil.Res("TesseraTextPrimaryBrush");
            Ring.Foreground = DataUtil.Res("TesseraAccentControlBrush");
            Ring.Background = DataUtil.Res("TesseraFillControlOffBrush");
        }
    }

    /// <summary>加载指示：四块小砖顺时针轮流亮起（圆融为四颗小圆点）</summary>
    public sealed partial class TesseraSpinner : ContentControl
    {
        private readonly Grid[] _tiles = new Grid[4];
        private readonly DispatcherTimer _timer = new() { Interval = TimeSpan.FromMilliseconds(250) };
        private int _step;
        public double Size { get; set; } = 20;

        public TesseraSpinner()
        {
            IsTabStop = false;
            AutomationProperties.SetName(this, "加载中");
            Loaded += (_, _) => { Build(); if (TesseraResources.AnimationsEnabled) _timer.Start(); };
            Unloaded += (_, _) => _timer.Stop();
            _timer.Tick += (_, _) => { _step = (_step + 1) % 4; Paint(); };
        }

        private void Build()
        {
            var gap = Size / 10;
            var tile = (Size - gap) / 2;
            var g = new Grid { Width = Size, Height = Size, RowSpacing = gap, ColumnSpacing = gap };
            g.RowDefinitions.Add(new RowDefinition()); g.RowDefinitions.Add(new RowDefinition());
            g.ColumnDefinitions.Add(new ColumnDefinition()); g.ColumnDefinitions.Add(new ColumnDefinition());
            // 顺时针：左上 → 右上 → 右下 → 左下
            var pos = new[] { (0, 0), (0, 1), (1, 1), (1, 0) };
            for (var i = 0; i < 4; i++)
            {
                _tiles[i] = new Grid { Width = tile, Height = tile, CornerRadius = new CornerRadius(TesseraResources.IsRound ? tile / 2 : tile / 3) };
                Grid.SetRow(_tiles[i], pos[i].Item1); Grid.SetColumn(_tiles[i], pos[i].Item2);
                g.Children.Add(_tiles[i]);
            }
            Content = g;
            Paint();
        }

        private void Paint()
        {
            for (var i = 0; i < 4; i++)
            {
                if (_tiles[i] == null) return;
                _tiles[i].Background = DataUtil.Res("TesseraTextSecondaryBrush");
                _tiles[i].Opacity = i == _step ? 1 : 0.25;
            }
        }
    }

    // ------------------------------------------------------------------
    // 横幅 Banner / 结果页 Result / 空状态 EmptyState / 按键 Kbd
    // ------------------------------------------------------------------

    /// <summary>横幅：整块浅色底，图标 + 标题 + 正文 + 操作；Dismissible 时右侧有关闭按钮</summary>
    public sealed partial class TesseraBanner : ContentControl
    {
        private readonly Reparent _actions = new();
        public string Text { get; set; } = "";
        public string? Title { get; set; }
        /// <summary>accent / success / warning / danger / neutral</summary>
        public string Tone { get; set; } = "accent";
        public bool Dismissible { get; set; }
        public UIElement? Actions { get; set; }
        public event EventHandler? Dismissed;

        public TesseraBanner()
        {
            IsTabStop = false;
            HorizontalContentAlignment = HorizontalAlignment.Stretch;
            Loaded += (_, _) => Rebuild();
            ActualThemeChanged += (_, _) => Rebuild();
        }

        private void Rebuild()
        {
            var round = TesseraResources.IsRound;
            var g = new Grid
            {
                ColumnSpacing = 12, Padding = new Thickness(round ? 18 : 16, 14, Dismissible ? 4 : 16, 14),
                CornerRadius = new CornerRadius(round ? (TesseraResources.IsFlow ? 18 : 22) : 18), Background = DataUtil.ToneSubtle(Tone),
            };
            g.ColumnDefinitions.Add(new ColumnDefinition { Width = GridLength.Auto });
            g.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(1, GridUnitType.Star) });
            g.ColumnDefinitions.Add(new ColumnDefinition { Width = GridLength.Auto });
            var icon = Tone switch { "success" => "success", "warning" => "warning", "danger" => "error", _ => "info" };
            var ic = new TesseraIcon { Icon = icon, Size = 20, Foreground = DataUtil.Tone(Tone == "neutral" ? "neutral" : Tone, true), VerticalAlignment = VerticalAlignment.Top, Margin = new Thickness(0, 1, 0, 0) };
            g.Children.Add(ic);
            var body = new StackPanel { Spacing = 2 };
            if (Title != null) body.Children.Add(DataUtil.Text(Title, "TesseraLabelTextStyle"));
            body.Children.Add(DataUtil.Text(Text));
            if (Actions is FrameworkElement a) { a.Margin = new Thickness(0, 10, 0, 0); _actions.Into(body, a); }
            Grid.SetColumn(body, 1);
            g.Children.Add(body);
            if (Dismissible)
            {
                var close = DataUtil.IconButton("close", "关闭提示");
                // 触控范围 40，但不把单行横幅撑高（上下各伸出 10）
                close.Margin = new Thickness(0, -10, 0, -10);
                close.VerticalAlignment = VerticalAlignment.Top;
                close.Click += (_, _) => Dismissed?.Invoke(this, EventArgs.Empty);
                Grid.SetColumn(close, 2);
                g.Children.Add(close);
            }
            if (Tone is "warning" or "danger") AutomationProperties.SetLiveSetting(g, AutomationLiveSetting.Polite);
            Content = g;
        }
    }

    /// <summary>结果页：一块大釉砖图标落下时轻轻弹一下。Status：success / error / info / notfound</summary>
    public sealed partial class TesseraResult : ContentControl
    {
        private readonly Reparent _actions = new();
        public string Status { get; set; } = "success";
        public string Title { get; set; } = "";
        public string? Text { get; set; }
        public UIElement? Actions { get; set; }

        public TesseraResult()
        {
            IsTabStop = false;
            HorizontalContentAlignment = HorizontalAlignment.Stretch;
            Loaded += (_, _) => Rebuild();
            ActualThemeChanged += (_, _) => Rebuild();
        }

        private void Rebuild()
        {
            var round = TesseraResources.IsRound;
            var s = new StackPanel { Spacing = 12, Padding = new Thickness(24, 48, 24, 48), HorizontalAlignment = HorizontalAlignment.Center };
            var fill = Status switch { "success" => "TesseraSuccessSolidBrush", "error" => "TesseraDangerSolidBrush", "notfound" => "TesseraTextPrimaryBrush", _ => "TesseraAccentSolidBrush" };
            var tile = new Grid { Width = 72, Height = 72, CornerRadius = new CornerRadius(round ? 36 : 26), Background = DataUtil.Res(fill), HorizontalAlignment = HorizontalAlignment.Center, Margin = new Thickness(0, 0, 0, 8) };
            tile.Children.Add(new Grid { Background = DataUtil.Res("TesseraSheenBrush"), Opacity = 0.6 });
            if (Status == "notfound")
            {
                var t = new TextBlock { Text = "404", FontSize = 22, FontWeight = Microsoft.UI.Text.FontWeights.SemiBold, Foreground = DataUtil.Res("TesseraBgSurfaceBrush"), HorizontalAlignment = HorizontalAlignment.Center, VerticalAlignment = VerticalAlignment.Center };
                if (Application.Current.Resources.TryGetValue("TesseraDisplayFontFamily", out var f) && f is FontFamily ff) t.FontFamily = ff;
                tile.Children.Add(t);
            }
            else
            {
                tile.Children.Add(new TesseraIcon { Icon = Status == "success" ? "check" : Status == "error" ? "close" : "info", Size = 36, Foreground = new SolidColorBrush(Microsoft.UI.Colors.White), HorizontalAlignment = HorizontalAlignment.Center, VerticalAlignment = VerticalAlignment.Center });
            }
            AutomationProperties.SetAccessibilityView(tile, AccessibilityView.Raw);
            s.Children.Add(tile);
            var title = DataUtil.Text(Title, "TesseraTitle1TextStyle");
            title.TextAlignment = TextAlignment.Center;
            title.HorizontalAlignment = HorizontalAlignment.Center;
            AutomationProperties.SetHeadingLevel(title, AutomationHeadingLevel.Level1);
            s.Children.Add(title);
            if (Text != null)
            {
                var t = DataUtil.Text(Text, "TesseraBodyTextStyle", "TesseraTextSecondaryBrush");
                t.TextAlignment = TextAlignment.Center; t.MaxWidth = 420; t.HorizontalAlignment = HorizontalAlignment.Center; t.Margin = new Thickness(0, 0, 0, 16);
                s.Children.Add(t);
            }
            if (Actions is FrameworkElement a) { a.HorizontalAlignment = HorizontalAlignment.Center; _actions.Into(s, a); }
            Content = s;
            Settle(tile);
        }

        /// <summary>图标落下：从上方 24 像素、0.6 倍淡入，弹簧落定（系统关闭动画时直接显示）</summary>
        private static void Settle(FrameworkElement el)
        {
            if (!TesseraResources.AnimationsEnabled) return;
            var v = ElementCompositionPreview.GetElementVisual(el);
            var c = v.Compositor;
            v.CenterPoint = new Vector3(36, 36, 0);
            var scale = c.CreateSpringVector3Animation();
            scale.InitialValue = new Vector3(0.6f, 0.6f, 1);
            scale.FinalValue = Vector3.One;
            scale.DampingRatio = TesseraMotion.Bouncy.DampingRatio;
            scale.Period = TimeSpan.FromMilliseconds(60);
            v.StartAnimation("Scale", scale);
            // 用 Translation（不是 Offset）：Offset 由 XAML 布局决定
            ElementCompositionPreview.SetIsTranslationEnabled(el, true);
            var drop = c.CreateSpringVector3Animation();
            drop.InitialValue = new Vector3(0, -24, 0);
            drop.FinalValue = Vector3.Zero;
            drop.DampingRatio = TesseraMotion.Bouncy.DampingRatio;
            drop.Period = TimeSpan.FromMilliseconds(60);
            v.StartAnimation("Translation", drop);
        }
    }

    /// <summary>空状态：一小片 3×3 马赛克，中间一块釉砖翘起来</summary>
    public sealed partial class TesseraEmptyState : ContentControl
    {
        private readonly Reparent _actions = new();
        public string Title { get; set; } = "";
        public string? Text { get; set; }
        public UIElement? Action { get; set; }

        public TesseraEmptyState()
        {
            IsTabStop = false;
            HorizontalContentAlignment = HorizontalAlignment.Stretch;
            Loaded += (_, _) => Rebuild();
            ActualThemeChanged += (_, _) => Rebuild();
        }

        private void Rebuild()
        {
            var round = TesseraResources.IsRound;
            var s = new StackPanel { Spacing = 8, Padding = new Thickness(24, 48, 24, 48), HorizontalAlignment = HorizontalAlignment.Center };
            var gap = round ? 6 : 3;
            var mosaic = new Grid { RowSpacing = gap, ColumnSpacing = gap, HorizontalAlignment = HorizontalAlignment.Center, Margin = new Thickness(0, 0, 0, 12) };
            for (var k = 0; k < 3; k++) { mosaic.RowDefinitions.Add(new RowDefinition { Height = new GridLength(22) }); mosaic.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(22) }); }
            for (var i = 0; i < 9; i++)
            {
                double big = 12, small = 6;
                var cr = round ? new CornerRadius(11) : new CornerRadius(i == 0 ? big : small, i == 2 ? big : small, i == 8 ? big : small, i == 6 ? big : small);
                var t = new Grid { CornerRadius = cr, Background = DataUtil.Res(i == 4 ? "TesseraAccentSolidBrush" : "TesseraFillDefaultBrush") };
                if (i == 4) t.RenderTransform = new CompositeTransform { TranslateY = -6, Rotation = round ? 0 : -8, CenterX = 11, CenterY = 11 };
                Grid.SetRow(t, i / 3); Grid.SetColumn(t, i % 3);
                mosaic.Children.Add(t);
            }
            AutomationProperties.SetAccessibilityView(mosaic, AccessibilityView.Raw);
            s.Children.Add(mosaic);
            var title = DataUtil.Text(Title, "TesseraTitle3TextStyle");
            title.HorizontalAlignment = HorizontalAlignment.Center; title.TextAlignment = TextAlignment.Center;
            AutomationProperties.SetHeadingLevel(title, AutomationHeadingLevel.Level2);
            s.Children.Add(title);
            if (Text != null)
            {
                var t = DataUtil.Text(Text, brush: "TesseraTextSecondaryBrush");
                t.HorizontalAlignment = HorizontalAlignment.Center; t.TextAlignment = TextAlignment.Center; t.MaxWidth = 360; t.Margin = new Thickness(0, 0, 0, 12);
                s.Children.Add(t);
            }
            if (Action is FrameworkElement a) { a.HorizontalAlignment = HorizontalAlignment.Center; _actions.Into(s, a); }
            Content = s;
        }
    }

    /// <summary>快捷键键帽：Keys = "Ctrl+K"（用 + 分隔多个键）</summary>
    public sealed partial class TesseraKbd : ContentControl
    {
        private string _keys = "";
        public string Keys { get => _keys; set { _keys = value; Rebuild(); } }

        public TesseraKbd()
        {
            IsTabStop = false;
            Loaded += (_, _) => Rebuild();
            ActualThemeChanged += (_, _) => Rebuild();
        }

        private void Rebuild()
        {
            var keys = _keys.Split('+', StringSplitOptions.RemoveEmptyEntries | StringSplitOptions.TrimEntries);
            var s = new StackPanel { Orientation = Orientation.Horizontal, Spacing = 4, VerticalAlignment = VerticalAlignment.Center };
            foreach (var k in keys)
            {
                var g = new Grid
                {
                    Padding = new Thickness(6, 3, 6, 4), CornerRadius = new CornerRadius(TesseraResources.IsRound ? 8 : 6),
                    Background = DataUtil.Res("TesseraBgSurfaceBrush"), BorderBrush = DataUtil.Res("TesseraBorderSubtleBrush"), BorderThickness = new Thickness(1),
                };
                var t = new TextBlock { Text = k, FontSize = 11, FontWeight = Microsoft.UI.Text.FontWeights.Medium, Foreground = DataUtil.Res("TesseraTextSecondaryBrush") };
                if (Application.Current?.Resources.TryGetValue("TesseraMonoFontFamily", out var f) == true && f is FontFamily ff) t.FontFamily = ff;
                g.Children.Add(t);
                s.Children.Add(g);
            }
            AutomationProperties.SetName(s, "快捷键 " + string.Join(" 加 ", keys));
            Content = s;
        }
    }

    /// <summary>提示气泡本身（静态展示、截图用）；实际使用 ToolTipService.SetToolTip，颜色由轻量样式统一</summary>
    public sealed partial class TesseraTooltipBubble : ContentControl
    {
        public TesseraTooltipBubble()
        {
            IsTabStop = false;
            Loaded += (_, _) => Paint();
            ActualThemeChanged += (_, _) => Paint();
        }

        public string Text { get; set; } = "";

        private void Paint()
        {
            var g = new Grid { Padding = new Thickness(TesseraResources.IsRound ? 12 : 10, 6, TesseraResources.IsRound ? 12 : 10, 6), CornerRadius = new CornerRadius(TesseraResources.IsRound ? 14 : 8), Background = DataUtil.Res("TesseraBgInverseBrush"), MaxWidth = 260, HorizontalAlignment = HorizontalAlignment.Left };
            g.Children.Add(new TextBlock { Text = Text, FontSize = 12.5, FontWeight = Microsoft.UI.Text.FontWeights.Medium, Foreground = DataUtil.Res("TesseraTextInverseBrush"), TextWrapping = TextWrapping.Wrap });
            Content = g;
        }
    }

    // ------------------------------------------------------------------
    // 代码块 CodeBlock：凹槽底色，等宽字体，长行在块内横向滚动；复制后图标变成勾
    // ------------------------------------------------------------------

    public sealed partial class TesseraCodeBlock : ContentControl
    {
        public string Code { get; set; } = "";
        public string? CodeLanguage { get; set; }
        public bool LineNumbers { get; set; }
        private DispatcherTimer? _reset;

        public TesseraCodeBlock()
        {
            IsTabStop = false;
            HorizontalContentAlignment = HorizontalAlignment.Stretch;
            Loaded += (_, _) => Rebuild();
            ActualThemeChanged += (_, _) => Rebuild();
        }

        private string Trimmed => Code.TrimEnd('\n', '\r');

        private void Rebuild()
        {
            var round = TesseraResources.IsRound;
            var root = new Grid { CornerRadius = new CornerRadius(round ? (TesseraResources.IsFlow ? 18 : 22) : 14), Background = DataUtil.Res("TesseraFillDefaultBrush") };
            root.RowDefinitions.Add(new RowDefinition { Height = GridLength.Auto });
            root.RowDefinitions.Add(new RowDefinition { Height = GridLength.Auto });
            var head = new Grid { Padding = new Thickness(round ? 18 : 16, 0, 0, 0), BorderBrush = DataUtil.Res("TesseraBorderSubtleBrush"), BorderThickness = new Thickness(0, 0, 0, round ? 0 : 1) };
            var mono = Application.Current?.Resources.TryGetValue("TesseraMonoFontFamily", out var f) == true && f is FontFamily ff ? ff : new FontFamily("Consolas");
            head.Children.Add(new TextBlock { Text = CodeLanguage ?? "", FontFamily = mono, FontSize = 12, FontWeight = Microsoft.UI.Text.FontWeights.Medium, Foreground = DataUtil.Res("TesseraTextSecondaryBrush"), VerticalAlignment = VerticalAlignment.Center });
            var copy = DataUtil.IconButton("copy", "复制代码");
            copy.HorizontalAlignment = HorizontalAlignment.Right;
            copy.Click += (_, _) =>
            {
                var p = new DataPackage();
                p.SetText(Trimmed);
                Clipboard.SetContent(p);
                copy.Content = new TesseraIcon { Icon = "check", Size = 18, Foreground = DataUtil.Res("TesseraSuccessTextBrush") };
                AutomationProperties.SetName(copy, "已复制");
                _reset?.Stop();
                var timer = new DispatcherTimer { Interval = TimeSpan.FromMilliseconds(1600) };
                timer.Tick += (_, _) => { timer.Stop(); copy.Content = new TesseraIcon { Icon = "copy", Size = 18 }; AutomationProperties.SetName(copy, "复制代码"); };
                _reset = timer;
                timer.Start();
            };
            head.Children.Add(copy);
            root.Children.Add(head);

            var lines = Trimmed.Split('\n');
            var body = new StackPanel { Orientation = Orientation.Horizontal, Spacing = 12, Padding = new Thickness(round ? 18 : 16, 14, round ? 18 : 16, 14) };
            if (LineNumbers)
            {
                var n = new TextBlock { Text = string.Join("\n", lines.Select((_, i) => (i + 1).ToString())), FontFamily = mono, FontSize = 13, LineHeight = 21, LineStackingStrategy = LineStackingStrategy.BlockLineHeight, TextAlignment = TextAlignment.Right, MinWidth = 22, Foreground = DataUtil.Res("TesseraTextTertiaryBrush") };
                AutomationProperties.SetAccessibilityView(n, AccessibilityView.Raw);
                body.Children.Add(n);
            }
            body.Children.Add(new TextBlock { Text = Trimmed, FontFamily = mono, FontSize = 13, LineHeight = 21, LineStackingStrategy = LineStackingStrategy.BlockLineHeight, Foreground = DataUtil.Res("TesseraTextPrimaryBrush"), IsTextSelectionEnabled = true });
            var scroller = new ScrollViewer { HorizontalScrollMode = ScrollMode.Enabled, HorizontalScrollBarVisibility = ScrollBarVisibility.Auto, VerticalScrollMode = ScrollMode.Disabled, VerticalScrollBarVisibility = ScrollBarVisibility.Disabled, Content = body };
            AutomationProperties.SetName(scroller, $"{CodeLanguage} 代码".Trim());
            Grid.SetRow(scroller, 1);
            root.Children.Add(scroller);
            Content = root;
        }
    }

    // ------------------------------------------------------------------
    // 气泡卡片面板 PopoverPanel：浮起的圆角面板（静态展示；弹出时用 Flyout + TesseraFlyoutPresenterStyle）
    // ------------------------------------------------------------------

    public sealed partial class TesseraPopoverPanel : ContentControl
    {
        private readonly Reparent _actions = new();
        public string? Title { get; set; }
        public UIElement? Body { get; set; }

        public TesseraPopoverPanel()
        {
            IsTabStop = false;
            Loaded += (_, _) => Rebuild();
            ActualThemeChanged += (_, _) => Rebuild();
        }

        private void Rebuild()
        {
            var round = TesseraResources.IsRound;
            var s = new StackPanel
            {
                Spacing = 6, Padding = new Thickness(20), MaxWidth = 340, HorizontalAlignment = HorizontalAlignment.Left,
                CornerRadius = new CornerRadius(round ? (TesseraResources.IsFlow ? 22 : 24) : 18),
                Background = DataUtil.Res("TesseraBgElevatedBrush"),
                BorderBrush = DataUtil.Res("TesseraBorderSubtleBrush"), BorderThickness = new Thickness(round ? 0 : 1),
                Shadow = new ThemeShadow(), Translation = new Vector3(0, 0, 16),
            };
            if (Title != null)
            {
                var t = DataUtil.Text(Title, "TesseraTitle3TextStyle");
                AutomationProperties.SetHeadingLevel(t, AutomationHeadingLevel.Level3);
                s.Children.Add(t);
            }
            _actions.Into(s, Body);
            Content = s;
        }
    }
}
