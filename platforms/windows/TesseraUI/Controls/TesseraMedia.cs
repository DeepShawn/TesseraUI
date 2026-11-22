using System;
using System.Collections.Generic;
using Microsoft.UI.Input;
using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Automation;
using Microsoft.UI.Xaml.Automation.Peers;
using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Controls.Primitives;
using Microsoft.UI.Xaml.Input;
using Microsoft.UI.Xaml.Media;
using Windows.System;

namespace TesseraUI
{
    // ------------------------------------------------------------------
    // 轮播 Carousel：系统 FlipView（跟手翻页、键盘左右键）+ 前后按钮 + 圆点
    // ------------------------------------------------------------------

    /// <summary>
    /// 轮播。Add(内容) 添加一页；AutoplaySeconds 大于 0 时自动播放（鼠标悬停或键盘聚焦时暂停，系统关闭动画时不自动播放）。
    /// 圆点只是视觉指示，读屏通过页面名称（第 i 张，共 n 张）和前后按钮操作。
    /// </summary>
    public sealed partial class TesseraCarousel : ContentControl
    {
        public FlipView Flip { get; } = new() { Height = 200 };
        private readonly StackPanel _dots = new() { Orientation = Orientation.Horizontal, Spacing = 6, HorizontalAlignment = HorizontalAlignment.Center };
        private readonly Button _prev = DataUtil.IconButton("chevron-left", "上一张", 20);
        private readonly Button _next = DataUtil.IconButton("chevron-right", "下一张", 20);
        private readonly List<FlipViewItem> _pages = new();
        private readonly DispatcherTimer _auto = new();
        private bool _paused;

        public double AutoplaySeconds
        {
            get => _auto.Interval.TotalSeconds;
            set { _auto.Interval = TimeSpan.FromSeconds(Math.Max(1, value)); if (value > 0 && TesseraResources.AnimationsEnabled) _auto.Start(); else _auto.Stop(); }
        }

        public double PageHeight { get => Flip.Height; set => Flip.Height = value; }

        public TesseraCarousel()
        {
            IsTabStop = false;
            HorizontalContentAlignment = HorizontalAlignment.Stretch;
            AutomationProperties.SetName(Flip, "轮播");
            var stage = new Grid();
            stage.Children.Add(Flip);
            foreach (var (b, align) in new[] { (_prev, HorizontalAlignment.Left), (_next, HorizontalAlignment.Right) })
            {
                b.HorizontalAlignment = align;
                b.VerticalAlignment = VerticalAlignment.Center;
                b.Margin = new Thickness(12, 0, 12, 0);
                b.Width = 44; b.Height = 44;
                stage.Children.Add(b);
            }
            _prev.Click += (_, _) => Go(Flip.SelectedIndex - 1);
            _next.Click += (_, _) => Go(Flip.SelectedIndex + 1);
            AutomationProperties.SetAccessibilityView(_dots, AccessibilityView.Raw);
            var root = new StackPanel { Spacing = 4 };
            root.Children.Add(stage);
            root.Children.Add(_dots);
            Content = root;
            Flip.SelectionChanged += (_, _) => Sync();
            PointerEntered += (_, _) => _paused = true;
            PointerExited += (_, _) => _paused = false;
            GotFocus += (_, _) => _paused = true;
            LostFocus += (_, _) => _paused = false;
            _auto.Tick += (_, _) => { if (!_paused && _pages.Count > 0) Go(Flip.SelectedIndex >= _pages.Count - 1 ? 0 : Flip.SelectedIndex + 1); };
            Loaded += (_, _) => Paint();
            Unloaded += (_, _) => _auto.Stop();
            ActualThemeChanged += (_, _) => Paint();
        }

        public void Add(UIElement content)
        {
            var item = new FlipViewItem { Content = content };
            _pages.Add(item);
            Flip.Items.Add(item);
            for (var i = 0; i < _pages.Count; i++) AutomationProperties.SetName(_pages[i], $"第 {i + 1} 张，共 {_pages.Count} 张");
            var dot = new Grid { Height = 8, Width = 8 };
            var hit = new Grid { Height = 24, Background = new SolidColorBrush(Microsoft.UI.Colors.Transparent) };
            hit.Children.Add(dot);
            var index = _pages.Count - 1;
            hit.Tapped += (_, _) => Go(index);
            _dots.Children.Add(hit);
            Sync();
        }

        public void Go(int i)
        {
            if (_pages.Count == 0) return;
            Flip.SelectedIndex = Math.Clamp(i, 0, _pages.Count - 1);
        }

        private void Paint()
        {
            Flip.CornerRadius = new CornerRadius(TesseraResources.IsRound ? (TesseraResources.IsFlow ? 26 : 28) : 24);
            Flip.Background = new SolidColorBrush(Microsoft.UI.Colors.Transparent);
            foreach (var b in new[] { _prev, _next })
            {
                b.Background = DataUtil.Res("TesseraBgElevatedBrush");
                b.CornerRadius = new CornerRadius(TesseraResources.IsRound ? 22 : 14);
                b.Shadow = new ThemeShadow();
                b.Translation = new System.Numerics.Vector3(0, 0, 8);
            }
            Sync();
        }

        private void Sync()
        {
            var i = Math.Max(0, Flip.SelectedIndex);
            _prev.Visibility = i > 0 ? Visibility.Visible : Visibility.Collapsed;
            _next.Visibility = i < _pages.Count - 1 ? Visibility.Visible : Visibility.Collapsed;
            for (var k = 0; k < _dots.Children.Count; k++)
            {
                if (_dots.Children[k] is not Grid hit || hit.Children[0] is not Grid dot) continue;
                var on = k == i;
                dot.Width = on ? 22 : 8;
                dot.CornerRadius = new CornerRadius(TesseraResources.IsRound ? 4 : 3);
                dot.Background = DataUtil.Res(on ? "TesseraTextPrimaryBrush" : "TesseraFillControlOffBrush");
            }
        }
    }

    // ------------------------------------------------------------------
    // 图片预览 ImageViewer：全屏查看；滚轮 / 双指 / 双击缩放，放大后拖动平移；
    // 未放大时下滑关闭（背景随之变淡）、左右滑切换；Esc 关闭，←→ 切换，+ − 缩放
    // ------------------------------------------------------------------

    public sealed partial class TesseraImageViewer : ContentControl
    {
        public IList<ImageSource> Images { get; } = new List<ImageSource>();
        public IList<string> Descriptions { get; } = new List<string>();
        public event EventHandler? Closed;

        private int _index;
        private readonly ScrollViewer _scroller = new()
        {
            ZoomMode = ZoomMode.Enabled, MinZoomFactor = 1, MaxZoomFactor = 5,
            HorizontalScrollBarVisibility = ScrollBarVisibility.Hidden, VerticalScrollBarVisibility = ScrollBarVisibility.Hidden,
            HorizontalScrollMode = ScrollMode.Auto, VerticalScrollMode = ScrollMode.Auto, IsTabStop = false,
        };
        private readonly Image _image = new() { Stretch = Stretch.Uniform, Margin = new Thickness(16, 64, 16, 64) };
        private readonly Grid _backdrop = new();
        private readonly TextBlock _counter = new() { FontSize = 14, FontWeight = Microsoft.UI.Text.FontWeights.SemiBold, Foreground = new SolidColorBrush(Microsoft.UI.Colors.White), VerticalAlignment = VerticalAlignment.Center };
        private readonly CompositeTransform _drag = new();

        public TesseraImageViewer()
        {
            IsTabStop = true;
            HorizontalContentAlignment = HorizontalAlignment.Stretch;
            VerticalContentAlignment = VerticalAlignment.Stretch;
            AutomationProperties.SetName(this, "图片预览");
            _scroller.Content = _image;
            _image.RenderTransform = _drag;
            Loaded += (_, _) => { Build(); Show(_index); };
            KeyDown += OnKey;
            _scroller.DoubleTapped += (_, e) =>
            {
                // 双击：以点按处为中心放大到 2.5 倍；再双击还原
                var p = e.GetPosition(_image);
                if (_scroller.ZoomFactor > 1.01f) _scroller.ChangeView(0, 0, 1);
                else _scroller.ChangeView(p.X * 2.5 - _scroller.ViewportWidth / 2, p.Y * 2.5 - _scroller.ViewportHeight / 2, 2.5f);
            };
            // 未放大时：拖动用来关闭（竖向）或切换（横向）；放大后交给 ScrollViewer 平移
            _image.ManipulationMode = ManipulationModes.TranslateX | ManipulationModes.TranslateY | ManipulationModes.System;
            _image.ManipulationDelta += (_, e) =>
            {
                if (_scroller.ZoomFactor > 1.01f) return;
                _drag.TranslateX += e.Delta.Translation.X * 0.4;
                _drag.TranslateY += e.Delta.Translation.Y;
                _backdrop.Opacity = Math.Max(0.3, 1 - Math.Abs(_drag.TranslateY) / 1200);
            };
            _image.ManipulationCompleted += (_, _) =>
            {
                if (_scroller.ZoomFactor > 1.01f) return;
                if (Math.Abs(_drag.TranslateY) > 120) { Close(); return; }
                if (Math.Abs(_drag.TranslateX) > 32 && Images.Count > 1) Show(_drag.TranslateX < 0 ? _index + 1 : _index - 1);
                _drag.TranslateX = 0; _drag.TranslateY = 0; _backdrop.Opacity = 1;
            };
        }

        public int StartIndex { get => _index; set => _index = value; }

        private void Build()
        {
            var root = new Grid();
            _backdrop.Background = new SolidColorBrush(Microsoft.UI.ColorHelper.FromArgb(235, 0, 0, 0));
            root.Children.Add(_backdrop);
            root.Children.Add(_scroller);
            var top = new Grid { Padding = new Thickness(16, 12, 16, 12), VerticalAlignment = VerticalAlignment.Top };
            top.Children.Add(_counter);
            var close = ViewerButton("close", "关闭", new CornerRadius(TesseraResources.IsRound ? 22 : 14), true);
            close.HorizontalAlignment = HorizontalAlignment.Right;
            close.Click += (_, _) => Close();
            top.Children.Add(close);
            root.Children.Add(top);

            var round = TesseraResources.IsRound;
            var tools = new StackPanel { Orientation = Orientation.Horizontal, Spacing = round ? 0 : 3, HorizontalAlignment = HorizontalAlignment.Center, VerticalAlignment = VerticalAlignment.Bottom, Margin = new Thickness(0, 0, 0, 16) };
            if (round) { tools.Padding = new Thickness(4); tools.CornerRadius = new CornerRadius(26); tools.Background = new SolidColorBrush(Microsoft.UI.ColorHelper.FromArgb(31, 255, 255, 255)); }
            var list = new List<(string, string, Action)>();
            if (Images.Count > 1) list.Add(("chevron-left", "上一张", () => Show(_index - 1)));
            list.Add(("zoom-out", "缩小", () => Zoom(1 / 1.5)));
            list.Add(("zoom-in", "放大", () => Zoom(1.5)));
            if (Images.Count > 1) list.Add(("chevron-right", "下一张", () => Show(_index + 1)));
            for (var i = 0; i < list.Count; i++)
            {
                var (icon, label, act) = list[i];
                var radius = round ? new CornerRadius(22) : new CornerRadius(i == 0 ? 14 : 6, i == list.Count - 1 ? 14 : 6, i == list.Count - 1 ? 14 : 6, i == 0 ? 14 : 6);
                var b = ViewerButton(icon, label, radius, !round);
                b.Click += (_, _) => act();
                tools.Children.Add(b);
            }
            root.Children.Add(tools);
            Content = root;
        }

        private static Button ViewerButton(string icon, string label, CornerRadius radius, bool filled)
        {
            var b = new Button
            {
                Content = new TesseraIcon { Icon = icon, Size = 20, Foreground = new SolidColorBrush(Microsoft.UI.Colors.White) },
                Width = 44, Height = 44, Padding = new Thickness(0), CornerRadius = radius, BorderThickness = new Thickness(0),
                Background = filled ? new SolidColorBrush(Microsoft.UI.ColorHelper.FromArgb(31, 255, 255, 255)) : new SolidColorBrush(Microsoft.UI.Colors.Transparent),
            };
            b.Resources["ButtonBackgroundPointerOver"] = new SolidColorBrush(Microsoft.UI.ColorHelper.FromArgb(56, 255, 255, 255));
            b.Resources["ButtonBackgroundPressed"] = new SolidColorBrush(Microsoft.UI.ColorHelper.FromArgb(77, 255, 255, 255));
            AutomationProperties.SetName(b, label);
            return b;
        }

        private void Zoom(double factor) => _scroller.ChangeView(null, null, (float)Math.Clamp(_scroller.ZoomFactor * factor, 1, 5));

        private void Show(int i)
        {
            if (Images.Count == 0) return;
            _index = (i + Images.Count) % Images.Count;
            _image.Source = Images[_index];
            var name = _index < Descriptions.Count ? Descriptions[_index] : "图片";
            AutomationProperties.SetName(_image, name);
            _counter.Text = Images.Count > 1 ? $"{_index + 1} / {Images.Count}" : name;
            _scroller.ChangeView(0, 0, 1, true);
            _drag.TranslateX = 0; _drag.TranslateY = 0; _backdrop.Opacity = 1;
        }

        private void OnKey(object sender, KeyRoutedEventArgs e)
        {
            switch (e.Key)
            {
                case VirtualKey.Escape: Close(); break;
                case VirtualKey.Left: if (Images.Count > 1) Show(_index - 1); break;
                case VirtualKey.Right: if (Images.Count > 1) Show(_index + 1); break;
                case (VirtualKey)187: case VirtualKey.Add: Zoom(1.5); break;
                case (VirtualKey)189: case VirtualKey.Subtract: Zoom(1 / 1.5); break;
                default: return;
            }
            e.Handled = true;
        }

        private Popup? _popup;

        private void Close()
        {
            Closed?.Invoke(this, EventArgs.Empty);
            if (_popup != null) _popup.IsOpen = false;
        }

        /// <summary>在窗口上方全屏打开图片预览</summary>
        public static TesseraImageViewer Show(XamlRoot root, IList<ImageSource> images, IList<string>? descriptions = null, int start = 0)
        {
            var v = new TesseraImageViewer { StartIndex = start, Width = root.Size.Width, Height = root.Size.Height };
            foreach (var img in images) v.Images.Add(img);
            if (descriptions != null) foreach (var d in descriptions) v.Descriptions.Add(d);
            var popup = new Popup { XamlRoot = root, Child = v, IsLightDismissEnabled = false };
            v._popup = popup;
            popup.Opened += (_, _) => v.Focus(FocusState.Programmatic);
            popup.IsOpen = true;
            return v;
        }
    }
}
