using System;
using System.Collections.Generic;
using System.Globalization;
using System.Linq;
using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Automation;
using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Media;
using Microsoft.UI.Xaml.Shapes;

namespace TesseraUI
{
    /// <summary>数据展示组件共用的小工具</summary>
    internal static class DataUtil
    {
        public static Brush Res(string key) => TesseraResources.Brush(key);

        public static Style? StyleRes(string key) =>
            Application.Current?.Resources is { } r && r.TryGetValue(key, out var v) && v is Style s ? s : null;

        public static TextBlock Text(string text, string style = "TesseraBodySmTextStyle", string brush = "TesseraTextPrimaryBrush")
        {
            var t = new TextBlock { Text = text, Foreground = Res(brush), TextWrapping = TextWrapping.Wrap };
            if (StyleRes(style) is { } s) t.Style = s;
            t.Foreground = Res(brush);
            return t;
        }

        /// <summary>只有图标的按钮：40 的砖，触控范围至少 40</summary>
        public static Button IconButton(string icon, string label, double size = 18)
        {
            var b = new Button { Content = new TesseraIcon { Icon = icon, Size = size }, MinWidth = 40, MinHeight = 40, Padding = new Thickness(0) };
            if (StyleRes("TesseraIconButtonStyle") is { } s) b.Style = s;
            AutomationProperties.SetName(b, label);
            return b;
        }

        /// <summary>成组的第 i 行（共 n 行）的圆角：首行、末行外角大，其余是砖缝小角；leading / trailing 表示这一块在行的左半边还是右半边</summary>
        public static CornerRadius Row(int i, int n, bool leading, bool trailing)
        {
            var round = TesseraResources.IsRound;
            var outer = round ? (TesseraResources.IsFlow ? 18 : 22) : 18;
            var inner = round ? 0 : 6;
            var top = i == 0 ? outer : inner;
            var bottom = i == n - 1 ? outer : inner;
            return new CornerRadius(leading ? top : 0, trailing ? top : 0, trailing ? bottom : 0, leading ? bottom : 0);
        }

        public static double Grout => TesseraResources.IsRound ? 0 : 3;

        public static Brush Tone(string tone, bool text = false) => tone switch
        {
            "success" => Res(text ? "TesseraSuccessTextBrush" : "TesseraSuccessSolidBrush"),
            "warning" => Res(text ? "TesseraWarningTextBrush" : "TesseraWarningSolidBrush"),
            "danger" => Res(text ? "TesseraDangerTextBrush" : "TesseraDangerSolidBrush"),
            "accent" => Res(text ? "TesseraAccentTextBrush" : "TesseraAccentControlBrush"),
            _ => Res(text ? "TesseraTextSecondaryBrush" : "TesseraFillControlOffBrush"),
        };

        public static Brush ToneSubtle(string tone) => tone switch
        {
            "success" => Res("TesseraSuccessSubtleBrush"),
            "warning" => Res("TesseraWarningSubtleBrush"),
            "danger" => Res("TesseraDangerSubtleBrush"),
            "neutral" => Res("TesseraFillDefaultBrush"),
            _ => Res("TesseraAccentSubtleBrush"),
        };

        /// <summary>小标签砖</summary>
        public static FrameworkElement Tag(string text, string tone)
        {
            var g = new Grid { Height = 24, Padding = new Thickness(TesseraResources.IsRound ? 10 : 8, 0, TesseraResources.IsRound ? 10 : 8, 0), CornerRadius = new CornerRadius(TesseraResources.IsRound ? 12 : 8), Background = ToneSubtle(tone), HorizontalAlignment = HorizontalAlignment.Left, VerticalAlignment = VerticalAlignment.Center };
            g.Children.Add(new TextBlock { Text = text, FontSize = 12, FontWeight = Microsoft.UI.Text.FontWeights.SemiBold, Foreground = Tone(tone, true), VerticalAlignment = VerticalAlignment.Center });
            return g;
        }
    }

    public enum TesseraSortDirection { None, Ascending, Descending }
    public enum TesseraTableLayout { Automatic, Grid, Cards }

    /// <summary>表格列。Text 用于显示、筛选、排序和读屏；Cell 可自定义单元格内容</summary>
    public sealed class TesseraTableColumn
    {
        public string Header { get; set; } = "";
        public double Width { get; set; } = 120;
        public bool Sortable { get; set; }
        public bool Numeric { get; set; }
        public Func<object, string> Text { get; set; } = o => o?.ToString() ?? "";
        public Func<object, FrameworkElement>? Cell { get; set; }
    }

    /// <summary>
    /// 表格：每一行是一块砖，砖缝分隔，不用横线。
    /// 点表头排序（升序 → 降序 → 原顺序；数字按数值、中文按拼音），多选（全选含半选），固定首列（横向滚动后出现分隔阴影），
    /// 分页、加载骨架；Layout = Automatic 时宽度小于 CardBreakpoint（默认 560）每行变成一张卡片。
    /// </summary>
    public sealed partial class TesseraTable : ContentControl
    {
        public IList<TesseraTableColumn> Columns { get; } = new List<TesseraTableColumn>();
        private IList<object> _rows = new List<object>();
        public IList<object> Rows { get => _rows; set { _rows = value; _page = 1; Rebuild(); } }
        public Func<object, object> Key { get; set; } = r => r;
        public bool Selectable { get; set; }
        public ISet<object> Selected { get; } = new HashSet<object>();
        public event EventHandler? SelectionChanged;
        public bool StickyFirst { get; set; } = true;
        public int PageSize { get; set; }
        private bool _loading;
        public bool IsLoading { get => _loading; set { _loading = value; Rebuild(); } }
        private string _filter = "";
        public string Filter { get => _filter; set { _filter = value ?? ""; _page = 1; Rebuild(); } }
        public TesseraTableLayout Layout { get; set; } = TesseraTableLayout.Automatic;
        public double CardBreakpoint { get; set; } = 560;
        public string EmptyText { get; set; } = "没有符合条件的数据";

        private int _sortColumn = -1;
        private TesseraSortDirection _sortDirection = TesseraSortDirection.None;
        private int _page = 1;
        private bool _cards;
        private Rectangle _stickyShadow = new();

        private const double HeadHeight = 44, RowHeight = 48, SelectWidth = 48;
        private static readonly CompareInfo Zh = CultureInfo.GetCultureInfo("zh-CN").CompareInfo;

        public TesseraTable()
        {
            IsTabStop = false;
            HorizontalContentAlignment = HorizontalAlignment.Stretch;
            Loaded += (_, _) => Rebuild();
            ActualThemeChanged += (_, _) => Rebuild();
            SizeChanged += (_, e) =>
            {
                var cards = Layout == TesseraTableLayout.Cards || (Layout == TesseraTableLayout.Automatic && e.NewSize.Width < CardBreakpoint);
                if (cards != _cards) Rebuild();
            };
        }

        private static double? Number(string s)
        {
            var t = new string(s.Where(c => !",，¥$% ".Contains(c)).ToArray());
            return double.TryParse(t, NumberStyles.Float, CultureInfo.InvariantCulture, out var v) ? v : null;
        }

        /// <summary>按表格的规则排序：数字列按数值，其余按中文拼音</summary>
        public static IList<object> Sort(IList<object> rows, TesseraTableColumn column, TesseraSortDirection direction)
        {
            if (direction == TesseraSortDirection.None) return rows;
            var numeric = column.Numeric || rows.All(r => Number(column.Text(r)) != null);
            IEnumerable<object> sorted = numeric
                ? rows.OrderBy(r => Number(column.Text(r)) ?? 0)
                : rows.OrderBy(r => column.Text(r), Comparer<string>.Create((a, b) => Zh.Compare(a, b, CompareOptions.None)));
            var list = sorted.ToList();
            if (direction == TesseraSortDirection.Descending) list.Reverse();
            return list;
        }

        private IList<object> Visible()
        {
            var q = Filter.Trim().ToLowerInvariant();
            IList<object> rows = q.Length == 0 ? Rows : Rows.Where(r => Columns.Any(c => c.Text(r).ToLowerInvariant().Contains(q))).ToList();
            return _sortColumn >= 0 ? Sort(rows, Columns[_sortColumn], _sortDirection) : rows;
        }

        private void OnSort(int i)
        {
            _sortDirection = _sortColumn != i ? TesseraSortDirection.Ascending : _sortDirection switch
            {
                TesseraSortDirection.Ascending => TesseraSortDirection.Descending,
                TesseraSortDirection.Descending => TesseraSortDirection.None,
                _ => TesseraSortDirection.Ascending,
            };
            _sortColumn = _sortDirection == TesseraSortDirection.None ? -1 : i;
            _page = 1;
            Rebuild();
        }

        private void Rebuild()
        {
            if (Columns.Count == 0) return;
            var all = Visible();
            var pages = PageSize > 0 ? Math.Max(1, (all.Count + PageSize - 1) / PageSize) : 1;
            _page = Math.Clamp(_page, 1, pages);
            var shown = PageSize > 0 ? all.Skip((_page - 1) * PageSize).Take(PageSize).ToList() : all.ToList();
            _cards = Layout == TesseraTableLayout.Cards || (Layout == TesseraTableLayout.Automatic && ActualWidth > 0 && ActualWidth < CardBreakpoint);

            var root = new StackPanel { Spacing = 12 };
            if (!IsLoading && shown.Count == 0) root.Children.Add(Empty());
            else root.Children.Add(_cards ? BuildCards(shown) : BuildGrid(shown, all));
            if (PageSize > 0 && !IsLoading) root.Children.Add(Footer(all.Count, pages, shown.Count));
            Content = root;
        }

        private FrameworkElement Empty()
        {
            var g = new Grid { Padding = new Thickness(40), Background = DataUtil.Res("TesseraBgSurfaceBrush"), CornerRadius = DataUtil.Row(0, 1, true, true) };
            var t = DataUtil.Text(EmptyText, brush: "TesseraTextTertiaryBrush");
            t.HorizontalAlignment = HorizontalAlignment.Center;
            g.Children.Add(t);
            return g;
        }

        private Brush RowFill(int i, bool selected)
        {
            if (selected) return DataUtil.Res("TesseraAccentSubtleBrush");
            return DataUtil.Res("TesseraBgSurfaceBrush");
        }

        private FrameworkElement BuildGrid(List<object> rows, IList<object> all)
        {
            var fixedCols = StickyFirst ? 1 : 0;
            var count = IsLoading ? (PageSize > 0 ? PageSize : 5) : rows.Count;
            var hasFixed = Selectable || fixedCols > 0;
            var gap = DataUtil.Grout;

            var fixedPanel = new StackPanel { Spacing = gap };
            var scrollPanel = new StackPanel { Spacing = gap, HorizontalAlignment = HorizontalAlignment.Left };

            // 表头
            var fixedHead = new StackPanel { Orientation = Orientation.Horizontal, Height = HeadHeight };
            if (Selectable) fixedHead.Children.Add(Wrap(SelectAll(all), SelectWidth, HeadHeight));
            if (fixedCols > 0) fixedHead.Children.Add(HeadCell(0));
            fixedPanel.Children.Add(fixedHead);
            var head = new StackPanel { Orientation = Orientation.Horizontal, Height = HeadHeight };
            for (var i = fixedCols; i < Columns.Count; i++) head.Children.Add(HeadCell(i));
            scrollPanel.Children.Add(head);

            for (var r = 0; r < count; r++)
            {
                var row = IsLoading ? null : rows[r];
                var key = row == null ? null : Key(row);
                var sel = key != null && Selected.Contains(key);
                if (hasFixed)
                {
                    var fr = new Grid { Height = RowHeight, Background = RowFill(r, sel), CornerRadius = DataUtil.Row(r, count, true, false) };
                    var cells = new StackPanel { Orientation = Orientation.Horizontal };
                    if (Selectable) cells.Children.Add(Wrap(row == null ? new Grid() : RowCheck(row, key!, sel), SelectWidth, RowHeight));
                    if (fixedCols > 0) cells.Children.Add(BodyCell(0, row));
                    fr.Children.Add(cells);
                    // 拼贴：选中行左侧一道 3 像素釉色竖条；圆融只靠底色
                    if (sel && !TesseraResources.IsRound) fr.Children.Add(new Rectangle { Width = 3, HorizontalAlignment = HorizontalAlignment.Left, Fill = DataUtil.Res("TesseraAccentSolidBrush") });
                    fixedPanel.Children.Add(fr);
                }
                var sr = new StackPanel { Orientation = Orientation.Horizontal, Height = RowHeight, Background = RowFill(r, sel), CornerRadius = DataUtil.Row(r, count, !hasFixed, true) };
                for (var i = fixedCols; i < Columns.Count; i++) sr.Children.Add(BodyCell(i, row));
                scrollPanel.Children.Add(sr);
            }

            var scroller = new ScrollViewer
            {
                HorizontalScrollMode = ScrollMode.Enabled, HorizontalScrollBarVisibility = ScrollBarVisibility.Hidden,
                VerticalScrollMode = ScrollMode.Disabled, VerticalScrollBarVisibility = ScrollBarVisibility.Disabled,
                Content = scrollPanel, IsTabStop = false,
            };
            // 列宽加起来比容器窄时，行仍然铺满整个宽度
            scroller.SizeChanged += (_, e) =>
            {
                var w = Columns.Skip(fixedCols).Sum(c => c.Width);
                var fill = Math.Max(w, e.NewSize.Width);
                foreach (var child in scrollPanel.Children) if (child is FrameworkElement fe) fe.Width = fill;
            };
            // 每次重建都新建：旧的阴影还挂在上一个网格里，不能直接复用
            _stickyShadow = new Rectangle { Width = 10, IsHitTestVisible = false, Opacity = 0, HorizontalAlignment = HorizontalAlignment.Left };
            _stickyShadow.Fill = new LinearGradientBrush
            {
                StartPoint = new Windows.Foundation.Point(0, 0), EndPoint = new Windows.Foundation.Point(1, 0),
                GradientStops = { new GradientStop { Color = Microsoft.UI.ColorHelper.FromArgb(46, 31, 27, 22), Offset = 0 }, new GradientStop { Color = Microsoft.UI.ColorHelper.FromArgb(0, 31, 27, 22), Offset = 1 } },
            };
            var shadow = _stickyShadow;
            scroller.ViewChanged += (_, _) => shadow.Opacity = scroller.HorizontalOffset > 1 ? 1 : 0;

            var grid = new Grid();
            grid.ColumnDefinitions.Add(new ColumnDefinition { Width = GridLength.Auto });
            grid.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(1, GridUnitType.Star) });
            if (hasFixed) grid.Children.Add(fixedPanel);
            Grid.SetColumn(scroller, 1);
            grid.Children.Add(scroller);
            Grid.SetColumn(_stickyShadow, 1);
            if (hasFixed) grid.Children.Add(_stickyShadow);
            return grid;
        }

        private static FrameworkElement Wrap(FrameworkElement child, double w, double h)
        {
            var g = new Grid { Width = w, Height = h, Padding = new Thickness(8, 0, 0, 0) };
            child.VerticalAlignment = VerticalAlignment.Center;
            g.Children.Add(child);
            return g;
        }

        private FrameworkElement HeadCell(int i)
        {
            var col = Columns[i];
            var cell = new Grid { Width = col.Width, Height = HeadHeight, Padding = new Thickness(col.Sortable ? 10 : 16, 0, col.Sortable ? 10 : 16, 0) };
            var align = col.Numeric ? HorizontalAlignment.Right : HorizontalAlignment.Left;
            if (!col.Sortable)
            {
                var t = DataUtil.Text(col.Header, "TesseraLabelTextStyle", "TesseraTextSecondaryBrush");
                t.FontSize = 13; t.VerticalAlignment = VerticalAlignment.Center; t.HorizontalAlignment = align; t.TextWrapping = TextWrapping.NoWrap;
                cell.Children.Add(t);
                return cell;
            }
            var dir = _sortColumn == i ? _sortDirection : TesseraSortDirection.None;
            var active = dir != TesseraSortDirection.None;
            var fg = active ? "TesseraAccentTextBrush" : "TesseraTextSecondaryBrush";
            var label = DataUtil.Text(col.Header, "TesseraLabelTextStyle", fg);
            label.FontSize = 13; label.TextWrapping = TextWrapping.NoWrap; label.VerticalAlignment = VerticalAlignment.Center;
            var icon = new TesseraIcon { Icon = dir == TesseraSortDirection.Ascending ? "arrow-up" : dir == TesseraSortDirection.Descending ? "arrow-down" : "chevrons-up-down", Size = 14, Opacity = active ? 1 : 0.4, Foreground = DataUtil.Res(fg), VerticalAlignment = VerticalAlignment.Center };
            var content = new StackPanel { Orientation = Orientation.Horizontal, Spacing = 4 };
            if (col.Numeric) { content.Children.Add(icon); content.Children.Add(label); } else { content.Children.Add(label); content.Children.Add(icon); }
            var b = new Button { Content = content, MinHeight = 44, MinWidth = 44, Padding = new Thickness(6, 0, 6, 0), HorizontalAlignment = align, VerticalAlignment = VerticalAlignment.Center };
            if (DataUtil.StyleRes("TesseraTextButtonStyle") is { } s) b.Style = s;
            b.Padding = new Thickness(6, 0, 6, 0);
            AutomationProperties.SetName(b, $"{col.Header}，排序");
            AutomationProperties.SetItemStatus(b, dir == TesseraSortDirection.Ascending ? "升序" : dir == TesseraSortDirection.Descending ? "降序" : "未排序");
            b.Click += (_, _) => OnSort(i);
            cell.Children.Add(b);
            return cell;
        }

        private FrameworkElement BodyCell(int i, object? row)
        {
            var col = Columns[i];
            var cell = new Grid { Width = col.Width, Height = RowHeight, Padding = new Thickness(16, 0, 16, 0) };
            FrameworkElement content;
            if (row == null)
            {
                content = new Grid { Height = 12, Width = Math.Max(16, (col.Width - 32) * (0.4 + ((i * 23) % 40) / 100.0)), CornerRadius = new CornerRadius(TesseraResources.IsRound ? 6 : 6), Background = DataUtil.Res("TesseraFillDefaultBrush"), HorizontalAlignment = HorizontalAlignment.Left };
            }
            else if (col.Cell != null) content = col.Cell(row);
            else
            {
                var t = DataUtil.Text(col.Text(row));
                t.TextWrapping = TextWrapping.NoWrap; t.TextTrimming = TextTrimming.CharacterEllipsis;
                t.HorizontalAlignment = col.Numeric ? HorizontalAlignment.Right : HorizontalAlignment.Left;
                content = t;
            }
            content.VerticalAlignment = VerticalAlignment.Center;
            cell.Children.Add(content);
            return cell;
        }

        private CheckBox Check(bool? state, string label)
        {
            var c = new CheckBox { IsThreeState = false, IsChecked = state, MinWidth = 40, MinHeight = 40, Width = 40, Padding = new Thickness(0) };
            if (DataUtil.StyleRes("TesseraCheckBoxStyle") is { } s) c.Style = s;
            c.MinWidth = 40; c.Padding = new Thickness(0);
            AutomationProperties.SetName(c, label);
            return c;
        }

        private FrameworkElement SelectAll(IList<object> all)
        {
            var keys = all.Select(Key).ToList();
            var picked = keys.Count(Selected.Contains);
            bool? state = picked == 0 ? false : picked == keys.Count ? true : null;
            var c = Check(state, "全选");
            c.Click += (_, _) =>
            {
                if (state == true) foreach (var k in keys) Selected.Remove(k);
                else foreach (var k in keys) Selected.Add(k);
                SelectionChanged?.Invoke(this, EventArgs.Empty);
                Rebuild();
            };
            return c;
        }

        private FrameworkElement RowCheck(object row, object key, bool on)
        {
            var c = Check(on, $"选择 {Columns[0].Text(row)}");
            c.Click += (_, _) =>
            {
                if (!Selected.Remove(key)) Selected.Add(key);
                SelectionChanged?.Invoke(this, EventArgs.Empty);
                Rebuild();
            };
            return c;
        }

        private FrameworkElement BuildCards(List<object> rows)
        {
            var round = TesseraResources.IsRound;
            var panel = new StackPanel { Spacing = round ? 8 : 3 };
            var count = IsLoading ? (PageSize > 0 ? PageSize : 3) : rows.Count;
            for (var r = 0; r < count; r++)
            {
                var radius = round ? DataUtil.Row(0, 1, true, true) : DataUtil.Row(r, count, true, true);
                var card = new StackPanel { Padding = new Thickness(16, 6, 16, 6), CornerRadius = radius };
                if (IsLoading)
                {
                    card.Background = DataUtil.Res("TesseraBgSurfaceBrush");
                    card.Spacing = 10; card.Padding = new Thickness(16);
                    card.Children.Add(new Grid { Width = 140, Height = 14, CornerRadius = new CornerRadius(6), Background = DataUtil.Res("TesseraFillDefaultBrush"), HorizontalAlignment = HorizontalAlignment.Left });
                    card.Children.Add(new Grid { Width = 220, Height = 12, CornerRadius = new CornerRadius(6), Background = DataUtil.Res("TesseraFillDefaultBrush"), HorizontalAlignment = HorizontalAlignment.Left });
                    panel.Children.Add(card);
                    continue;
                }
                var row = rows[r];
                var key = Key(row);
                var sel = Selected.Contains(key);
                card.Background = RowFill(r, sel);
                var title = new StackPanel { Orientation = Orientation.Horizontal, Spacing = 4, MinHeight = 44 };
                if (Selectable) { var c = RowCheck(row, key, sel); c.VerticalAlignment = VerticalAlignment.Center; title.Children.Add(c); }
                FrameworkElement head = Columns[0].Cell?.Invoke(row) ?? DataUtil.Text(Columns[0].Text(row), "TesseraLabelTextStyle");
                if (head is TextBlock tb) tb.FontSize = 15;
                head.VerticalAlignment = VerticalAlignment.Center;
                title.Children.Add(head);
                card.Children.Add(title);
                for (var i = 1; i < Columns.Count; i++)
                {
                    var line = new Grid { Padding = new Thickness(0, 6, 0, 6) };
                    line.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(1, GridUnitType.Star) });
                    line.ColumnDefinitions.Add(new ColumnDefinition { Width = GridLength.Auto });
                    var name = DataUtil.Text(Columns[i].Header, "TesseraCaptionTextStyle", "TesseraTextSecondaryBrush");
                    name.VerticalAlignment = VerticalAlignment.Center;
                    line.Children.Add(name);
                    FrameworkElement value = Columns[i].Cell?.Invoke(row) ?? DataUtil.Text(Columns[i].Text(row));
                    value.VerticalAlignment = VerticalAlignment.Center;
                    if (value is TextBlock vt) vt.TextAlignment = TextAlignment.Right;
                    Grid.SetColumn(value, 1);
                    line.Children.Add(value);
                    card.Children.Add(line);
                }
                panel.Children.Add(card);
            }
            return panel;
        }

        private FrameworkElement Footer(int total, int pages, int shownCount)
        {
            var from = total == 0 ? 0 : (_page - 1) * PageSize + 1;
            var g = new Grid();
            g.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(1, GridUnitType.Star) });
            g.ColumnDefinitions.Add(new ColumnDefinition { Width = GridLength.Auto });
            var info = DataUtil.Text(total == 0 ? "共 0 条" : $"共 {total} 条，第 {from}–{from + shownCount - 1} 条", "TesseraCaptionTextStyle", "TesseraTextSecondaryBrush");
            info.VerticalAlignment = VerticalAlignment.Center;
            g.Children.Add(info);
            var nav = new StackPanel { Orientation = Orientation.Horizontal, Spacing = 2 };
            var prev = DataUtil.IconButton("chevron-left", "上一页");
            prev.IsEnabled = _page > 1;
            prev.Click += (_, _) => { _page--; Rebuild(); };
            var next = DataUtil.IconButton("chevron-right", "下一页");
            next.IsEnabled = _page < pages;
            next.Click += (_, _) => { _page++; Rebuild(); };
            var pageText = DataUtil.Text($"{_page} / {pages}", "TesseraCaptionTextStyle", "TesseraTextSecondaryBrush");
            pageText.VerticalAlignment = VerticalAlignment.Center;
            AutomationProperties.SetName(pageText, $"第 {_page} 页，共 {pages} 页");
            nav.Children.Add(prev); nav.Children.Add(pageText); nav.Children.Add(next);
            Grid.SetColumn(nav, 1);
            g.Children.Add(nav);
            return g;
        }
    }
}
