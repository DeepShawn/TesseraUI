using System;
using System.Collections.Generic;
using System.Globalization;
using System.Linq;
using System.Text;
using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Automation;
using Microsoft.UI.Xaml.Automation.Peers;
using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Controls.Primitives;
using Microsoft.UI.Xaml.Input;
using Microsoft.UI.Xaml.Media;
using Windows.Foundation;
using Windows.System;

namespace TesseraUI
{
    /// <summary>表单与输入组件共用的小工具</summary>
    internal static class FormUtil
    {
        public static Brush Res(string key) => TesseraResources.Brush(key);

        /// <summary>拼贴组里第 i 块（共 n 块）的圆角：外角 outer、内角 6；圆融 / 流韵合成一整块（内角 0，外角胶囊或方圆之间）</summary>
        public static CornerRadius Group(int i, int n, double outer)
        {
            var round = TesseraResources.IsRound;
            var o = round ? (TesseraResources.IsFlow ? 17 : 999) : outer;
            var inner = round ? 0 : 6;
            if (n <= 1) return new CornerRadius(o);
            if (i == 0) return new CornerRadius(o, inner, inner, o);
            if (i == n - 1) return new CornerRadius(inner, o, o, inner);
            return new CornerRadius(inner);
        }

        public static double GroupGap => TesseraResources.IsRound ? 0 : 3;

        /// <summary>全角数字、全角小数点与负号转为半角（中文输入法下常见）</summary>
        public static string NormalizeDigits(string s)
        {
            var b = new StringBuilder(s.Length);
            foreach (var ch in s)
            {
                if (ch >= '０' && ch <= '９') b.Append((char)('0' + (ch - '０')));
                else if (ch is '．' or '。') b.Append('.');
                else if (ch is '－' or '—' or '−') b.Append('-');
                else b.Append(ch);
            }
            return b.ToString();
        }
    }

    // ------------------------------------------------------------------
    // 按钮组 ButtonGroup
    // ------------------------------------------------------------------

    /// <summary>
    /// 按钮组：一组拼贴砖，选中的一块是深色实砖；圆融 / 流韵合成一整块胶囊。
    /// Multiple = false 时单选（读屏为一组单选按钮），true 时可多选（每项是切换按钮）。
    /// </summary>
    public sealed partial class TesseraButtonGroup : ContentControl
    {
        public static readonly DependencyProperty ItemsProperty =
            DependencyProperty.Register(nameof(Items), typeof(IList<string>), typeof(TesseraButtonGroup), new PropertyMetadata(null, (d, _) => ((TesseraButtonGroup)d).Rebuild()));
        public static readonly DependencyProperty MultipleProperty =
            DependencyProperty.Register(nameof(Multiple), typeof(bool), typeof(TesseraButtonGroup), new PropertyMetadata(false, (d, _) => ((TesseraButtonGroup)d).Rebuild()));

        public IList<string>? Items { get => (IList<string>?)GetValue(ItemsProperty); set => SetValue(ItemsProperty, value); }
        public bool Multiple { get => (bool)GetValue(MultipleProperty); set => SetValue(MultipleProperty, value); }

        /// <summary>选中项的下标</summary>
        public ISet<int> Selected { get; } = new SortedSet<int> { 0 };
        public event EventHandler? SelectionChanged;

        private readonly StackPanel _panel = new() { Orientation = Orientation.Horizontal };
        private readonly List<ToggleButton> _buttons = new();

        public TesseraButtonGroup()
        {
            IsTabStop = false;
            Content = _panel;
            Loaded += (_, _) => Rebuild();
            ActualThemeChanged += (_, _) => Paint();
            AutomationProperties.SetAccessibilityView(this, AccessibilityView.Raw);
        }

        public void SetSelected(IEnumerable<int> indices)
        {
            Selected.Clear();
            foreach (var i in indices) Selected.Add(i);
            Paint();
        }

        private void Rebuild()
        {
            _panel.Children.Clear();
            _buttons.Clear();
            _panel.Spacing = FormUtil.GroupGap;
            var items = Items ?? Array.Empty<string>();
            for (var i = 0; i < items.Count; i++)
            {
                var index = i;
                var b = new ToggleButton
                {
                    Content = items[i],
                    MinHeight = 40,
                    MinWidth = 44,
                    Padding = new Thickness(16, 0, 16, 0),
                    FontWeight = Microsoft.UI.Text.FontWeights.SemiBold,
                    BorderThickness = new Thickness(0),
                };
                b.Click += (_, _) =>
                {
                    if (Multiple) { if (!Selected.Remove(index)) Selected.Add(index); }
                    else { Selected.Clear(); Selected.Add(index); }
                    Paint();
                    SelectionChanged?.Invoke(this, EventArgs.Empty);
                };
                AutomationProperties.SetName(b, items[i]);
                _buttons.Add(b);
                _panel.Children.Add(b);
            }
            Paint();
        }

        private void Paint()
        {
            for (var i = 0; i < _buttons.Count; i++)
            {
                var on = Selected.Contains(i);
                var b = _buttons[i];
                b.IsChecked = on;
                b.CornerRadius = FormUtil.Group(i, _buttons.Count, 14);
                // 选中：深色实砖；未选中：浅砖。ToggleButton 自带的选中色用资源覆盖
                b.Resources["ToggleButtonBackground"] = FormUtil.Res("TesseraFillDefaultBrush");
                b.Resources["ToggleButtonBackgroundPointerOver"] = FormUtil.Res("TesseraFillDefaultBrush");
                b.Resources["ToggleButtonBackgroundPressed"] = FormUtil.Res("TesseraStatePressedBrush");
                b.Resources["ToggleButtonForeground"] = FormUtil.Res("TesseraTextPrimaryBrush");
                b.Resources["ToggleButtonBackgroundChecked"] = FormUtil.Res("TesseraTextPrimaryBrush");
                b.Resources["ToggleButtonBackgroundCheckedPointerOver"] = FormUtil.Res("TesseraTextPrimaryBrush");
                b.Resources["ToggleButtonBackgroundCheckedPressed"] = FormUtil.Res("TesseraTextPrimaryBrush");
                b.Resources["ToggleButtonForegroundChecked"] = FormUtil.Res("TesseraBgSurfaceBrush");
                b.Resources["ToggleButtonForegroundCheckedPointerOver"] = FormUtil.Res("TesseraBgSurfaceBrush");
                b.Resources["ToggleButtonForegroundCheckedPressed"] = FormUtil.Res("TesseraBgSurfaceBrush");
                if (!Multiple) AutomationProperties.SetPositionInSet(b, i + 1);
                if (!Multiple) AutomationProperties.SetSizeOfSet(b, _buttons.Count);
            }
        }
    }

    // ------------------------------------------------------------------
    // 分割按钮 SplitButton
    // ------------------------------------------------------------------

    /// <summary>
    /// 分割按钮：左边执行默认操作（Click），右边打开 Flyout（放 MenuFlyout）。两块砖拼在一起。
    /// </summary>
    public sealed partial class TesseraSplitButton : ContentControl
    {
        public static readonly DependencyProperty TextProperty =
            DependencyProperty.Register(nameof(Text), typeof(string), typeof(TesseraSplitButton), new PropertyMetadata("", (d, e) => ((TesseraSplitButton)d)._main.Content = e.NewValue));
        public static readonly DependencyProperty FlyoutProperty =
            DependencyProperty.Register(nameof(Flyout), typeof(FlyoutBase), typeof(TesseraSplitButton), new PropertyMetadata(null, (d, e) => ((TesseraSplitButton)d)._arrow.Flyout = (FlyoutBase)e.NewValue));
        public static readonly DependencyProperty IsPrimaryProperty =
            DependencyProperty.Register(nameof(IsPrimary), typeof(bool), typeof(TesseraSplitButton), new PropertyMetadata(true, (d, _) => ((TesseraSplitButton)d).Paint()));

        public string Text { get => (string)GetValue(TextProperty); set => SetValue(TextProperty, value); }
        public FlyoutBase? Flyout { get => (FlyoutBase?)GetValue(FlyoutProperty); set => SetValue(FlyoutProperty, value); }
        public bool IsPrimary { get => (bool)GetValue(IsPrimaryProperty); set => SetValue(IsPrimaryProperty, value); }
        public event RoutedEventHandler? Click;

        private readonly Button _main = new();
        private readonly Button _arrow = new() { Width = 40 };

        public TesseraSplitButton()
        {
            IsTabStop = false;
            var panel = new StackPanel { Orientation = Orientation.Horizontal };
            panel.Children.Add(_main);
            panel.Children.Add(_arrow);
            Content = panel;
            _arrow.Content = new TesseraIcon { Icon = "chevron-down", Size = 18 };
            AutomationProperties.SetName(_arrow, "更多选项");
            _main.Click += (s, e) => Click?.Invoke(this, e);
            Loaded += (_, _) => Paint();
            ActualThemeChanged += (_, _) => Paint();
        }

        private void Paint()
        {
            var key = IsPrimary ? "TesseraPrimaryButtonStyle" : "TesseraSecondaryButtonStyle";
            if (Application.Current.Resources.TryGetValue(key, out var s) && s is Style style && !ReferenceEquals(_main.Style, style)) { _main.Style = style; _arrow.Style = style; }
            ((StackPanel)Content).Spacing = FormUtil.GroupGap;
            _main.CornerRadius = FormUtil.Group(0, 2, 14);
            _arrow.CornerRadius = FormUtil.Group(1, 2, 14);
            _arrow.Padding = new Thickness(0);
        }
    }

    // ------------------------------------------------------------------
    // 数字步进 NumberInput
    // ------------------------------------------------------------------

    /// <summary>
    /// 数字步进：− [数值] + 三块砖拼在一起。按住 − / + 先停 400ms，然后每 120ms 调一次（RepeatButton）。
    /// 可直接输入（全角数字自动转半角），失焦或回车时按 Minimum / Maximum / Step 修正。读屏可用上下方向键调整。
    /// </summary>
    public sealed partial class TesseraNumberInput : ContentControl
    {
        public static readonly DependencyProperty ValueProperty =
            DependencyProperty.Register(nameof(Value), typeof(double), typeof(TesseraNumberInput), new PropertyMetadata(0.0, (d, _) => ((TesseraNumberInput)d).Sync()));
        public double Value { get => (double)GetValue(ValueProperty); set => SetValue(ValueProperty, value); }
        public double Minimum { get; set; } = double.MinValue;
        public double Maximum { get; set; } = double.MaxValue;
        public double Step { get; set; } = 1;
        public int Decimals { get; set; }
        public string Header { get => _header.Text; set { _header.Text = value; _header.Visibility = string.IsNullOrEmpty(value) ? Visibility.Collapsed : Visibility.Visible; AutomationProperties.SetName(_box, value); } }
        public event EventHandler<double>? ValueChanged;

        private readonly TextBlock _header = new() { Visibility = Visibility.Collapsed, FontWeight = Microsoft.UI.Text.FontWeights.SemiBold, FontSize = 14, Margin = new Thickness(0, 0, 0, 6) };
        private readonly RepeatButton _minus = new() { Delay = 400, Interval = 120, Width = 40, Height = 40 };
        private readonly RepeatButton _plus = new() { Delay = 400, Interval = 120, Width = 40, Height = 40 };
        private readonly TextBox _box = new() { Width = 88, TextAlignment = TextAlignment.Center, InputScope = new InputScope { Names = { new InputScopeName(InputScopeNameValue.Number) } } };

        public TesseraNumberInput()
        {
            IsTabStop = false;
            var row = new StackPanel { Orientation = Orientation.Horizontal };
            row.Children.Add(_minus);
            row.Children.Add(_box);
            row.Children.Add(_plus);
            var col = new StackPanel();
            col.Children.Add(_header);
            col.Children.Add(row);
            Content = col;
            _minus.Content = new TesseraIcon { Icon = "minus", Size = 18 };
            _plus.Content = new TesseraIcon { Icon = "plus", Size = 18 };
            AutomationProperties.SetName(_minus, "减少");
            AutomationProperties.SetName(_plus, "增加");
            _minus.Click += (_, _) => Bump(-1);
            _plus.Click += (_, _) => Bump(1);
            _box.LostFocus += (_, _) => Commit();
            _box.KeyDown += (_, e) =>
            {
                if (e.Key == VirtualKey.Enter) { Commit(); e.Handled = true; }
                if (e.Key == VirtualKey.Up) { Bump(1); e.Handled = true; }
                if (e.Key == VirtualKey.Down) { Bump(-1); e.Handled = true; }
            };
            Loaded += (_, _) => { Paint(); Sync(); };
            ActualThemeChanged += (_, _) => Paint();
        }

        private string Fmt(double v) => Decimals == 0 ? Math.Round(v).ToString(CultureInfo.InvariantCulture) : v.ToString("F" + Decimals, CultureInfo.InvariantCulture);
        private double Fix(double v)
        {
            var s = Step > 0 ? Math.Round(v / Step) * Step : v;
            return Math.Clamp(s, Minimum, Maximum);
        }

        private void Bump(int dir)
        {
            var next = Fix(Value + dir * Step);
            if (next == Value) return;
            Value = next;
            ValueChanged?.Invoke(this, next);
        }

        private void Commit()
        {
            var ok = double.TryParse(FormUtil.NormalizeDigits(_box.Text), NumberStyles.Float, CultureInfo.InvariantCulture, out var v);
            var fixedV = Fix(ok ? v : Value);
            if (fixedV != Value) { Value = fixedV; ValueChanged?.Invoke(this, fixedV); }
            Sync();
        }

        private void Sync()
        {
            _box.Text = Fmt(Value);
            _minus.IsEnabled = Value > Minimum;
            _plus.IsEnabled = Value < Maximum;
            AutomationProperties.SetHelpText(_box, $"当前 {Fmt(Value)}，按上下方向键调整");
        }

        private void Paint()
        {
            ((StackPanel)((StackPanel)Content).Children[1]).Spacing = FormUtil.GroupGap;
            _minus.CornerRadius = FormUtil.Group(0, 3, 14);
            _box.CornerRadius = FormUtil.Group(1, 3, 14);
            _plus.CornerRadius = FormUtil.Group(2, 3, 14);
            foreach (var b in new[] { _minus, _plus })
            {
                b.Background = FormUtil.Res("TesseraFillDefaultBrush");
                b.Foreground = FormUtil.Res("TesseraTextPrimaryBrush");
                b.BorderThickness = new Thickness(0);
                b.Padding = new Thickness(0);
            }
            if (Application.Current.Resources.TryGetValue("TesseraTextBoxStyle", out var s) && s is Style style && !ReferenceEquals(_box.Style, style)) _box.Style = style;
            _box.CornerRadius = FormUtil.Group(1, 3, 14);
            _box.FontWeight = Microsoft.UI.Text.FontWeights.SemiBold;
        }
    }

    // ------------------------------------------------------------------
    // 验证码 OTPInput
    // ------------------------------------------------------------------

    /// <summary>
    /// 验证码输入：一排凹槽格子，一个透明的输入框接收输入；可直接粘贴整段文字（自动取出数字），填满后触发 Completed。
    /// </summary>
    public sealed partial class TesseraOtpInput : ContentControl
    {
        public int Length { get; set; } = 6;
        public string Code { get => _box.Text; set => _box.Text = value; }
        public event EventHandler<string>? Completed;

        private readonly Grid _root = new();
        private readonly StackPanel _cells = new() { Orientation = Orientation.Horizontal, Spacing = 8, IsHitTestVisible = false };
        private readonly TextBox _box = new() { Opacity = 0.01, InputScope = new InputScope { Names = { new InputScopeName(InputScopeNameValue.NumericPin) } } };
        private bool _updating;

        public TesseraOtpInput()
        {
            IsTabStop = false;
            _root.Children.Add(_box);
            _root.Children.Add(_cells);
            Content = _root;
            _box.TextChanged += (_, _) =>
            {
                if (_updating) return;
                var digits = new string(FormUtil.NormalizeDigits(_box.Text).Where(char.IsAsciiDigit).Take(Length).ToArray());
                if (digits != _box.Text) { _updating = true; _box.Text = digits; _box.SelectionStart = digits.Length; _updating = false; }
                Paint();
                if (digits.Length == Length) Completed?.Invoke(this, digits);
            };
            _box.GotFocus += (_, _) => Paint();
            _box.LostFocus += (_, _) => Paint();
            Loaded += (_, _) => { Build(); Paint(); };
            ActualThemeChanged += (_, _) => Paint();
            AutomationProperties.SetName(_box, "验证码");
        }

        private void Build()
        {
            _cells.Children.Clear();
            for (var i = 0; i < Length; i++)
                _cells.Children.Add(new Border { Width = 44, Height = 56, Child = new TextBlock { FontSize = 24, FontWeight = Microsoft.UI.Text.FontWeights.SemiBold, HorizontalAlignment = HorizontalAlignment.Center, VerticalAlignment = VerticalAlignment.Center } });
            _box.Width = Length * 44 + (Length - 1) * 8;
            _box.Height = 56;
            AutomationProperties.SetHelpText(_box, $"共 {Length} 位数字");
        }

        private void Paint()
        {
            var focused = _box.FocusState != FocusState.Unfocused;
            var code = _box.Text;
            for (var i = 0; i < _cells.Children.Count; i++)
            {
                var b = (Border)_cells.Children[i];
                var active = focused && (i == code.Length || (i == Length - 1 && code.Length == Length));
                b.CornerRadius = new CornerRadius(TesseraResources.IsRound ? 14 : 14);
                b.Background = FormUtil.Res(active ? "TesseraBgSurfaceBrush" : "TesseraFillDefaultBrush");
                b.BorderBrush = FormUtil.Res(active ? "TesseraFocusRingBrush" : "TesseraBorderFieldBrush");
                b.BorderThickness = new Thickness(active || !TesseraResources.IsRound ? 1 : 0);
                ((TextBlock)b.Child).Text = i < code.Length ? code[i].ToString() : "";
                ((TextBlock)b.Child).Foreground = FormUtil.Res("TesseraTextPrimaryBrush");
            }
        }
    }

    // ------------------------------------------------------------------
    // 自动换行面板（标签输入）
    // ------------------------------------------------------------------

    /// <summary>从左到右排列，放不下时换行</summary>
    public sealed partial class TesseraWrapPanel : Panel
    {
        public double Spacing { get; set; } = 6;

        protected override Size MeasureOverride(Size available)
        {
            double x = 0, y = 0, lineH = 0, maxW = 0;
            foreach (var c in Children)
            {
                c.Measure(new Size(double.PositiveInfinity, double.PositiveInfinity));
                var s = c.DesiredSize;
                if (x > 0 && x + s.Width > available.Width) { x = 0; y += lineH + Spacing; lineH = 0; }
                x += s.Width + Spacing;
                maxW = Math.Max(maxW, x - Spacing);
                lineH = Math.Max(lineH, s.Height);
            }
            return new Size(double.IsInfinity(available.Width) ? maxW : available.Width, y + lineH);
        }

        protected override Size ArrangeOverride(Size final)
        {
            double x = 0, y = 0, lineH = 0;
            foreach (var c in Children)
            {
                var s = c.DesiredSize;
                if (x > 0 && x + s.Width > final.Width) { x = 0; y += lineH + Spacing; lineH = 0; }
                c.Arrange(new Rect(x, y, s.Width, s.Height));
                x += s.Width + Spacing;
                lineH = Math.Max(lineH, s.Height);
            }
            return final;
        }
    }
}
