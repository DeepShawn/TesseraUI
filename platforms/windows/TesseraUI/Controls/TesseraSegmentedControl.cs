using System;
using System.Collections.Generic;
using System.Numerics;
using Microsoft.UI.Composition;
using Microsoft.UI.Input;
using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Automation;
using Microsoft.UI.Xaml.Automation.Peers;
using Microsoft.UI.Xaml.Automation.Provider;
using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Hosting;
using Microsoft.UI.Xaml.Input;
using Microsoft.UI.Xaml.Media;
using Windows.System;

namespace TesseraUI
{
    /// <summary>
    /// 分段控件：凹槽里一块会滑动的砖。可以点选，也可以从任意一格按下后左右拖动，松手吸附到最近的一格（snappy 弹簧）。
    /// Glass = true 时滑块换成毛玻璃（半透明 + 亮边）。键盘：左右方向键、Home / End。
    /// 圆融外观（合并 TesseraShapeRound.xaml）下：一整条浅底胶囊，选中块是一颗浮起的白色胶囊。
    /// </summary>
    public sealed partial class TesseraSegmentedControl : ContentControl
    {
        public static readonly DependencyProperty ItemsProperty =
            DependencyProperty.Register(nameof(Items), typeof(IList<string>), typeof(TesseraSegmentedControl), new PropertyMetadata(null, (d, _) => ((TesseraSegmentedControl)d).Rebuild()));
        public static readonly DependencyProperty SelectedIndexProperty =
            DependencyProperty.Register(nameof(SelectedIndex), typeof(int), typeof(TesseraSegmentedControl), new PropertyMetadata(0, (d, e) => ((TesseraSegmentedControl)d).OnSelectedIndexChanged((int)e.OldValue)));
        public static readonly DependencyProperty GlassProperty =
            DependencyProperty.Register(nameof(Glass), typeof(bool), typeof(TesseraSegmentedControl), new PropertyMetadata(false, (d, _) => ((TesseraSegmentedControl)d).ApplyThumbStyle()));

        public IList<string>? Items { get => (IList<string>?)GetValue(ItemsProperty); set => SetValue(ItemsProperty, value); }
        public int SelectedIndex { get => (int)GetValue(SelectedIndexProperty); set => SetValue(SelectedIndexProperty, value); }
        public bool Glass { get => (bool)GetValue(GlassProperty); set => SetValue(GlassProperty, value); }

        public event EventHandler<int>? SelectionChanged;

        private const double Pad = 3; // 砖缝
        private readonly Grid _root = new();
        private readonly Border _track = new();
        private readonly Border _thumb = new() { HorizontalAlignment = HorizontalAlignment.Left };
        private readonly Grid _labels = new() { IsHitTestVisible = false };
        private readonly List<TextBlock> _texts = new();
        private Visual? _thumbVisual;
        private double _segmentWidth;
        private bool _dragging;
        private bool _moved;
        private double _pressX;
        private double _dragOffset;
        private float _thumbX;

        public TesseraSegmentedControl()
        {
            IsTabStop = true;
            UseSystemFocusVisuals = true;
            HorizontalContentAlignment = HorizontalAlignment.Stretch;
            VerticalContentAlignment = VerticalAlignment.Stretch;
            MinHeight = 40;

            _track.BorderThickness = new Thickness(1);
            _thumb.Margin = new Thickness(Pad);
            _labels.Margin = new Thickness(Pad);
            _root.Children.Add(_track);
            _root.Children.Add(_thumb);
            _root.Children.Add(_labels);
            _root.Background = new SolidColorBrush(Microsoft.UI.Colors.Transparent);
            Content = _root;

            ElementCompositionPreview.SetIsTranslationEnabled(_thumb, true);
            _thumbVisual = ElementCompositionPreview.GetElementVisual(_thumb);

            _root.SizeChanged += (_, _) => Layout(animate: false);
            _root.PointerPressed += OnPressed;
            _root.PointerMoved += OnMoved;
            _root.PointerReleased += OnReleased;
            _root.PointerCaptureLost += (_, _) => { if (_dragging) Finish(); };
            ActualThemeChanged += (_, _) => { ApplyTrackStyle(); ApplyThumbStyle(); UpdateLabels(); };
            Loaded += (_, _) => { ApplyTrackStyle(); ApplyThumbStyle(); Layout(animate: false); };
        }

        private static Brush Res(string key) => TesseraResources.Brush(key);

        private void ApplyTrackStyle()
        {
            var round = TesseraResources.IsRound;
            var flow = TesseraResources.IsFlow;
            // 拼贴：圆角 14 的凹槽；圆融：胶囊，没有描边；流韵：方圆之间（17），没有描边
            _track.CornerRadius = new CornerRadius(flow ? 17 + Pad : round ? 999 : 14 + Pad);
            _thumb.CornerRadius = new CornerRadius(flow ? 17 : round ? 999 : 14);
            _track.Background = Res("TesseraFillDefaultBrush");
            _track.BorderBrush = Res("TesseraBorderSubtleBrush");
            _track.BorderThickness = new Thickness(round ? 0 : 1);
        }

        private void ApplyThumbStyle()
        {
            var round = TesseraResources.IsRound;
            _thumb.Background = Res(Glass ? "TesseraMaterialGlassThumbBrush" : round ? "TesseraBgElevatedBrush" : "TesseraBgSurfaceBrush");
            _thumb.BorderBrush = Res(Glass ? "TesseraMaterialGlassRimBrush" : "TesseraBorderSurfaceBrush");
            _thumb.BorderThickness = new Thickness(Glass || !round ? 1 : 0);
        }

        private void Rebuild()
        {
            _labels.Children.Clear();
            _labels.ColumnDefinitions.Clear();
            _texts.Clear();
            var items = Items ?? Array.Empty<string>();
            for (var i = 0; i < items.Count; i++)
            {
                _labels.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(1, GridUnitType.Star) });
                var t = new TextBlock
                {
                    Text = items[i],
                    FontSize = 14,
                    FontWeight = Microsoft.UI.Text.FontWeights.SemiBold,
                    HorizontalAlignment = HorizontalAlignment.Center,
                    VerticalAlignment = VerticalAlignment.Center,
                    TextTrimming = TextTrimming.CharacterEllipsis,
                    Margin = new Thickness(12, 0, 12, 0),
                };
                Grid.SetColumn(t, i);
                _labels.Children.Add(t);
                _texts.Add(t);
            }
            Layout(animate: false);
            UpdateLabels();
        }

        private int Count => Items?.Count ?? 0;

        private void Layout(bool animate)
        {
            var n = Count;
            if (n == 0 || _root.ActualWidth <= 0) return;
            _segmentWidth = (_root.ActualWidth - Pad * 2) / n;
            _thumb.Width = Math.Max(0, _segmentWidth);
            MoveThumb((float)(_segmentWidth * Math.Clamp(SelectedIndex, 0, n - 1)), animate);
        }

        private void UpdateLabels()
        {
            for (var i = 0; i < _texts.Count; i++)
                _texts[i].Foreground = Res(i == SelectedIndex ? "TesseraTextPrimaryBrush" : "TesseraTextSecondaryBrush");
            if (Items is { } items && SelectedIndex >= 0 && SelectedIndex < items.Count)
                AutomationProperties.SetItemStatus(this, items[SelectedIndex]);
        }

        private void OnSelectedIndexChanged(int old)
        {
            UpdateLabels();
            if (TesseraResources.IsFlow && TesseraResources.AnimationsEnabled && _thumbVisual is not null) Tension();
            if (!_dragging) Layout(animate: true);
            if (FrameworkElementAutomationPeer.FromElement(this) is SegmentedPeer peer && Items is { } items && SelectedIndex >= 0 && SelectedIndex < items.Count)
                peer.RaisePropertyChangedEvent(ValuePatternIdentifiers.ValueProperty, old >= 0 && old < items.Count ? items[old] : "", items[SelectedIndex]);
            SelectionChanged?.Invoke(this, SelectedIndex);
        }

        /// <summary>流韵：选中块移动时像液面一样被拉扁，停下时回弹</summary>
        private void Tension()
        {
            var v = _thumbVisual!;
            v.CenterPoint = new Vector3((float)_thumb.ActualWidth / 2, (float)_thumb.ActualHeight / 2, 0);
            var a = v.Compositor.CreateVector3KeyFrameAnimation();
            a.InsertKeyFrame(0.30f, new Vector3(1, 0.84f, 1));
            a.InsertKeyFrame(0.62f, new Vector3(1, 1.06f, 1));
            a.InsertKeyFrame(0.82f, new Vector3(1, 0.98f, 1));
            a.InsertKeyFrame(1f, Vector3.One);
            a.Duration = TimeSpan.FromMilliseconds(520);
            v.StartAnimation("Scale", a);
        }

        private void MoveThumb(float x, bool animate)
        {
            _thumbX = x;
            if (_thumbVisual is null) return;
            _thumbVisual.StopAnimation("Translation");
            var target = new Vector3(x, 0, 0);
            if (!animate || !TesseraResources.AnimationsEnabled)
            {
                _thumbVisual.Properties.InsertVector3("Translation", target);
                return;
            }
            var spring = _thumbVisual.Compositor.CreateSpringVector3Animation();
            spring.FinalValue = target;
            spring.DampingRatio = TesseraMotion.Snappy.DampingRatio;
            spring.Period = TesseraMotion.Snappy.Period;
            _thumbVisual.StartAnimation("Translation", spring);
        }

        private void OnPressed(object sender, PointerRoutedEventArgs e)
        {
            if (Count == 0 || !IsEnabled) return;
            Focus(FocusState.Pointer);
            _root.CapturePointer(e.Pointer);
            _dragging = true;
            _moved = false;
            _pressX = e.GetCurrentPoint(_root).Position.X - Pad;
            // 按在滑块上：保持手指与滑块的相对位置；按在别的格子上：滑块中心跟到手指下
            var onThumb = _pressX >= _thumbX && _pressX <= _thumbX + _segmentWidth;
            _dragOffset = onThumb ? _pressX - _thumbX : _segmentWidth / 2;
            e.Handled = true;
        }

        private void OnMoved(object sender, PointerRoutedEventArgs e)
        {
            if (!_dragging) return;
            var x = e.GetCurrentPoint(_root).Position.X - Pad;
            if (!_moved && Math.Abs(x - _pressX) < 4) return;
            _moved = true;
            var max = _segmentWidth * (Count - 1);
            MoveThumb((float)Math.Clamp(x - _dragOffset, 0, max), animate: false);
            e.Handled = true;
        }

        private void OnReleased(object sender, PointerRoutedEventArgs e)
        {
            if (!_dragging) return;
            var x = e.GetCurrentPoint(_root).Position.X - Pad;
            _root.ReleasePointerCapture(e.Pointer);
            Finish(_moved ? null : (int)Math.Floor(x / _segmentWidth));
            e.Handled = true;
        }

        private void Finish(int? tapped = null)
        {
            _dragging = false;
            if (_segmentWidth <= 0) return;
            var index = Math.Clamp(tapped ?? (int)Math.Round(_thumbX / _segmentWidth), 0, Count - 1);
            if (index == SelectedIndex) Layout(animate: true);
            else SelectedIndex = index; // 触发 OnSelectedIndexChanged → 弹簧吸附
        }

        protected override void OnKeyDown(KeyRoutedEventArgs e)
        {
            var n = Count;
            if (n == 0) return;
            var rtl = FlowDirection == FlowDirection.RightToLeft;
            int? next = e.Key switch
            {
                VirtualKey.Left => SelectedIndex + (rtl ? 1 : -1),
                VirtualKey.Right => SelectedIndex + (rtl ? -1 : 1),
                VirtualKey.Home => 0,
                VirtualKey.End => n - 1,
                _ => null,
            };
            if (next is int i)
            {
                SelectedIndex = Math.Clamp(i, 0, n - 1);
                e.Handled = true;
            }
            base.OnKeyDown(e);
        }

        protected override AutomationPeer OnCreateAutomationPeer() => new SegmentedPeer(this);

        /// <summary>读屏：报告为“分段控件”，值为当前选中项；方向键切换。</summary>
        private sealed partial class SegmentedPeer : FrameworkElementAutomationPeer, IValueProvider
        {
            private readonly TesseraSegmentedControl _owner;
            public SegmentedPeer(TesseraSegmentedControl owner) : base(owner) { _owner = owner; }
            protected override AutomationControlType GetAutomationControlTypeCore() => AutomationControlType.Tab;
            protected override string GetLocalizedControlTypeCore() => "分段控件";
            protected override object GetPatternCore(PatternInterface patternInterface) =>
                patternInterface == PatternInterface.Value ? this : base.GetPatternCore(patternInterface);
            public bool IsReadOnly => false;
            public string Value => _owner.Items is { } items && _owner.SelectedIndex >= 0 && _owner.SelectedIndex < items.Count ? items[_owner.SelectedIndex] : "";
            public void SetValue(string value)
            {
                var i = _owner.Items?.IndexOf(value) ?? -1;
                if (i >= 0) _owner.SelectedIndex = i;
            }
        }
    }
}
