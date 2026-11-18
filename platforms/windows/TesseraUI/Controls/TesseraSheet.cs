using System;
using System.Numerics;
using Microsoft.UI.Composition;
using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Automation;
using Microsoft.UI.Xaml.Automation.Peers;
using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Hosting;
using Microsoft.UI.Xaml.Input;
using Microsoft.UI.Xaml.Media;
using Windows.System;

namespace TesseraUI
{
    /// <summary>
    /// 底部抽屉：遮罩 + 从底部升起的面板（smooth 弹簧），顶部把手可拖动，在“半高 / 全高”两档之间吸附，
    /// 往下甩或拖过半高的一半即关闭。Esc 关闭；打开时焦点移入面板。放在页面根 Grid 的最上层。
    /// 宽窗口（≥ 640）上面板限宽 560 居中。
    /// </summary>
    public sealed partial class TesseraSheet : ContentControl
    {
        public static readonly DependencyProperty IsOpenProperty =
            DependencyProperty.Register(nameof(IsOpen), typeof(bool), typeof(TesseraSheet), new PropertyMetadata(false, (d, _) => ((TesseraSheet)d).OnIsOpenChanged()));
        public static readonly DependencyProperty TitleProperty =
            DependencyProperty.Register(nameof(Title), typeof(string), typeof(TesseraSheet), new PropertyMetadata(null, (d, _) => ((TesseraSheet)d).UpdateTitle()));
        public static readonly DependencyProperty SheetContentProperty =
            DependencyProperty.Register(nameof(SheetContent), typeof(object), typeof(TesseraSheet), new PropertyMetadata(null, (d, e) => ((TesseraSheet)d)._body.Content = e.NewValue));

        public bool IsOpen { get => (bool)GetValue(IsOpenProperty); set => SetValue(IsOpenProperty, value); }
        public string? Title { get => (string?)GetValue(TitleProperty); set => SetValue(TitleProperty, value); }
        /// <summary>面板里的内容</summary>
        public object? SheetContent { get => GetValue(SheetContentProperty); set => SetValue(SheetContentProperty, value); }

        /// <summary>两档高度，占可用高度的比例</summary>
        public double MediumDetent { get; set; } = 0.5;
        public double LargeDetent { get; set; } = 0.92;

        public event EventHandler? Closed;

        private readonly Grid _root = new();
        private readonly Border _scrim = new();
        private readonly Border _panel = new();
        private readonly Border _handle = new();
        private readonly TextBlock _title = new();
        private readonly ContentPresenter _body = new();
        private double _detent;
        private double _dragStartHeight;

        public TesseraSheet()
        {
            HorizontalContentAlignment = HorizontalAlignment.Stretch;
            VerticalContentAlignment = VerticalAlignment.Stretch;
            IsTabStop = false;
            Visibility = Visibility.Collapsed;
            Canvas.SetZIndex(this, TesseraZ.Modal);

            _scrim.Tapped += (_, _) => IsOpen = false;

            var grabArea = new Grid
            {
                Height = 24,
                Background = new SolidColorBrush(Microsoft.UI.Colors.Transparent),
                ManipulationMode = ManipulationModes.TranslateY | ManipulationModes.TranslateInertia,
            };
            _handle.Width = 36;
            _handle.Height = 5;
            _handle.CornerRadius = new CornerRadius(2.5);
            _handle.HorizontalAlignment = HorizontalAlignment.Center;
            _handle.VerticalAlignment = VerticalAlignment.Center;
            grabArea.Children.Add(_handle);
            grabArea.ManipulationStarted += (_, _) => _dragStartHeight = _panel.ActualHeight;
            grabArea.ManipulationDelta += OnDragDelta;
            grabArea.ManipulationCompleted += OnDragCompleted;
            AutomationProperties.SetName(grabArea, "拖动调整高度");

            _title.FontSize = 22;
            _title.Margin = new Thickness(0, 4, 0, 16);
            _title.TextWrapping = TextWrapping.Wrap;
            AutomationProperties.SetHeadingLevel(_title, AutomationHeadingLevel.Level1);

            var stack = new Grid { Padding = new Thickness(20, 0, 20, 20) };
            stack.RowDefinitions.Add(new RowDefinition { Height = GridLength.Auto });
            stack.RowDefinitions.Add(new RowDefinition { Height = GridLength.Auto });
            stack.RowDefinitions.Add(new RowDefinition { Height = new GridLength(1, GridUnitType.Star) });
            Grid.SetRow(_title, 1);
            var scroller = new ScrollViewer { Content = _body, VerticalScrollBarVisibility = ScrollBarVisibility.Auto };
            Grid.SetRow(scroller, 2);
            stack.Children.Add(grabArea);
            stack.Children.Add(_title);
            stack.Children.Add(scroller);

            _panel.Child = stack;
            _panel.VerticalAlignment = VerticalAlignment.Bottom;
            _panel.CornerRadius = new CornerRadius(32, 32, 0, 0);
            _panel.BorderThickness = new Thickness(1, 1, 1, 0);
            _panel.TabFocusNavigation = KeyboardNavigationMode.Cycle; // 焦点留在面板内
            AutomationProperties.SetIsDialog(_panel, true);

            _root.Children.Add(_scrim);
            _root.Children.Add(_panel);
            _root.SizeChanged += (_, _) => { if (IsOpen) ApplyHeight(); };
            Content = _root;

            ElementCompositionPreview.SetIsTranslationEnabled(_panel, true);
            ActualThemeChanged += (_, _) => ApplyColors();
        }

        private void ApplyColors()
        {
            _scrim.Background = TesseraResources.Brush("TesseraBgScrimBrush");
            _panel.Background = TesseraResources.Brush("TesseraBgElevatedBrush");
            _panel.BorderBrush = TesseraResources.Brush("TesseraBorderSurfaceBrush");
            _handle.Background = TesseraResources.Brush("TesseraBorderStrongBrush");
            _title.Foreground = TesseraResources.Brush("TesseraTextPrimaryBrush");
            _title.FontFamily = Application.Current?.Resources.TryGetValue("TesseraDisplayFontFamily", out var f) == true ? f as FontFamily : null;
        }

        private void UpdateTitle()
        {
            _title.Text = Title ?? "";
            _title.Visibility = string.IsNullOrEmpty(Title) ? Visibility.Collapsed : Visibility.Visible;
            AutomationProperties.SetName(_panel, Title ?? "");
        }

        private double Available => Math.Max(0, _root.ActualHeight > 0 ? _root.ActualHeight : ActualHeight);

        private void ApplyHeight()
        {
            _panel.Height = Available * _detent;
            _panel.MaxWidth = _root.ActualWidth >= 640 ? 560 : double.PositiveInfinity;
        }

        private void OnIsOpenChanged()
        {
            var visual = ElementCompositionPreview.GetElementVisual(_panel);
            var scrimVisual = ElementCompositionPreview.GetElementVisual(_scrim);
            if (IsOpen)
            {
                ApplyColors();
                UpdateTitle();
                Visibility = Visibility.Visible;
                _detent = MediumDetent;
                UpdateLayout();
                ApplyHeight();
                if (TesseraResources.AnimationsEnabled)
                {
                    visual.Properties.InsertVector3("Translation", new Vector3(0, (float)_panel.Height, 0));
                    Spring(visual, 0, TesseraMotion.Smooth);
                    scrimVisual.Opacity = 0;
                    Fade(scrimVisual, 1, TesseraMotion.DurationNormal);
                }
                else
                {
                    visual.Properties.InsertVector3("Translation", Vector3.Zero);
                    scrimVisual.Opacity = 1;
                }
                _panel.Loaded += FocusFirst;
                FocusFirst(null, null);
            }
            else
            {
                Closed?.Invoke(this, EventArgs.Empty);
                if (!TesseraResources.AnimationsEnabled) { Visibility = Visibility.Collapsed; return; }
                var batch = visual.Compositor.CreateScopedBatch(CompositionBatchTypes.Animation);
                Slide(visual, (float)_panel.ActualHeight, TesseraMotion.DurationNormal);
                Fade(scrimVisual, 0, TesseraMotion.DurationNormal);
                batch.End();
                batch.Completed += (_, _) => { if (!IsOpen) Visibility = Visibility.Collapsed; };
            }
        }

        private void FocusFirst(object? sender, RoutedEventArgs? e)
        {
            _panel.Loaded -= FocusFirst;
            var first = FocusManager.FindFirstFocusableElement(_panel);
            if (first is Control c) c.Focus(FocusState.Programmatic);
        }

        protected override void OnKeyDown(KeyRoutedEventArgs e)
        {
            if (e.Key == VirtualKey.Escape && IsOpen) { IsOpen = false; e.Handled = true; }
            base.OnKeyDown(e);
        }

        private void OnDragDelta(object sender, ManipulationDeltaRoutedEventArgs e)
        {
            var h = _dragStartHeight - e.Cumulative.Translation.Y;
            var max = Available * LargeDetent;
            // 超过最高档时加阻尼
            if (h > max) h = max + (h - max) * 0.2;
            _panel.Height = Math.Max(0, h);
        }

        private void OnDragCompleted(object sender, ManipulationCompletedRoutedEventArgs e)
        {
            var vy = e.Velocities.Linear.Y * 1000; // px/ms → px/s
            var fraction = Available > 0 ? _panel.ActualHeight / Available : 0;
            if (vy > TesseraMotion.FlingVelocity || fraction < MediumDetent / 2) { IsOpen = false; return; }
            _detent = vy < -TesseraMotion.FlingVelocity ? LargeDetent
                : vy > TesseraMotion.FlingVelocity * 0.5 ? MediumDetent
                : Math.Abs(fraction - MediumDetent) < Math.Abs(fraction - LargeDetent) ? MediumDetent : LargeDetent;
            ApplyHeight();
        }

        private static void Spring(Visual v, float y, TesseraSpring s)
        {
            var a = v.Compositor.CreateSpringVector3Animation();
            a.FinalValue = new Vector3(0, y, 0);
            a.DampingRatio = s.DampingRatio;
            a.Period = s.Period;
            v.StartAnimation("Translation", a);
        }

        private static void Slide(Visual v, float y, TimeSpan duration)
        {
            var a = v.Compositor.CreateVector3KeyFrameAnimation();
            a.InsertKeyFrame(1, new Vector3(0, y, 0));
            a.Duration = duration;
            v.StartAnimation("Translation", a);
        }

        private static void Fade(Visual v, float to, TimeSpan duration)
        {
            var a = v.Compositor.CreateScalarKeyFrameAnimation();
            a.InsertKeyFrame(1, to);
            a.Duration = duration;
            v.StartAnimation("Opacity", a);
        }
    }
}
