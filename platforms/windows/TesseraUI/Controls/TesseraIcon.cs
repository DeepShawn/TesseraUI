using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Automation;
using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Data;
using Microsoft.UI.Xaml.Markup;
using Microsoft.UI.Xaml.Media;
using Microsoft.UI.Xaml.Shapes;

namespace TesseraUI
{
    /// <summary>
    /// TesseraUI 图标：24×24 网格，描边 1.5、圆头圆角；颜色跟随 Foreground。
    /// <code>&lt;tessera:TesseraIcon Icon="search" Size="20" /&gt;</code>
    /// 装饰性图标默认对读屏隐藏；需要被朗读时设置 AutomationProperties.Name，并把 AutomationProperties.AccessibilityView 设为 Content。
    /// </summary>
    public sealed partial class TesseraIcon : ContentControl
    {
        public static readonly DependencyProperty IconProperty =
            DependencyProperty.Register(nameof(Icon), typeof(string), typeof(TesseraIcon), new PropertyMetadata(null, (d, _) => ((TesseraIcon)d).Rebuild()));
        public static readonly DependencyProperty SizeProperty =
            DependencyProperty.Register(nameof(Size), typeof(double), typeof(TesseraIcon), new PropertyMetadata(24d, (d, _) => ((TesseraIcon)d).Resize()));

        /// <summary>图标名，与 Web / Android / Apple 端一致，例如 "search"、"chevron-down"。</summary>
        public string? Icon { get => (string?)GetValue(IconProperty); set => SetValue(IconProperty, value); }
        public double Size { get => (double)GetValue(SizeProperty); set => SetValue(SizeProperty, value); }

        private readonly Viewbox _box = new() { Stretch = Stretch.Uniform };
        private readonly Canvas _canvas = new() { Width = 24, Height = 24 };

        public TesseraIcon()
        {
            IsTabStop = false;
            HorizontalContentAlignment = HorizontalAlignment.Stretch;
            VerticalContentAlignment = VerticalAlignment.Stretch;
            _box.Child = _canvas;
            Content = _box;
            AutomationProperties.SetAccessibilityView(this, Microsoft.UI.Xaml.Automation.Peers.AccessibilityView.Raw);
            Resize();
        }

        private void Resize()
        {
            Width = Size;
            Height = Size;
        }

        private void Rebuild()
        {
            _canvas.Children.Clear();
            if (Icon is null || !TesseraIconData.All.TryGetValue(Icon, out var shapes)) return;
            foreach (var s in shapes)
            {
                var path = new Path
                {
                    Data = (Geometry)XamlBindingHelper.ConvertValue(typeof(Geometry), s.Data),
                    StrokeThickness = 1.5,
                    StrokeStartLineCap = PenLineCap.Round,
                    StrokeEndLineCap = PenLineCap.Round,
                    StrokeLineJoin = PenLineJoin.Round,
                };
                var bind = new Binding { Source = this, Path = new PropertyPath(nameof(Foreground)) };
                if (s.Stroke) path.SetBinding(Shape.StrokeProperty, bind);
                if (s.Fill) path.SetBinding(Shape.FillProperty, new Binding { Source = this, Path = new PropertyPath(nameof(Foreground)) });
                _canvas.Children.Add(path);
            }
        }
    }
}
