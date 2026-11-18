using System;
using System.Numerics;
using Microsoft.UI;
using Microsoft.UI.Composition;
using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Controls.Primitives;
using Microsoft.UI.Xaml.Hosting;
using Microsoft.UI.Xaml.Input;
using Microsoft.UI.Xaml.Media;
using Windows.UI;

namespace TesseraUI
{
    /// <summary>
    /// 流韵外观的动效（附加属性）。只在合并了 TesseraShapeFlow.xaml、且系统“动画效果”打开时生效，其他外观下什么也不做。
    /// <list type="bullet">
    /// <item><c>tessera:TesseraFlow.Ripple="True"</c>：按下处荡开两圈清透的水波，被控件圆角裁切（按钮、列表项）</item>
    /// <item><c>tessera:TesseraFlow.Splash="True"</c>：开关、复选框、单选切换时溅起两圈涟漪；复选框、单选像水珠一样弹一下</item>
    /// </list>
    /// TesseraUI 的按钮、开关、复选框、单选样式已经默认挂上，不需要手动设置。
    /// </summary>
    public static class TesseraFlow
    {
        public static readonly DependencyProperty RippleProperty =
            DependencyProperty.RegisterAttached("Ripple", typeof(bool), typeof(TesseraFlow), new PropertyMetadata(false, OnRippleChanged));
        public static readonly DependencyProperty SplashProperty =
            DependencyProperty.RegisterAttached("Splash", typeof(bool), typeof(TesseraFlow), new PropertyMetadata(false, OnSplashChanged));

        public static bool GetRipple(DependencyObject o) => (bool)o.GetValue(RippleProperty);
        public static void SetRipple(DependencyObject o, bool v) => o.SetValue(RippleProperty, v);
        public static bool GetSplash(DependencyObject o) => (bool)o.GetValue(SplashProperty);
        public static void SetSplash(DependencyObject o, bool v) => o.SetValue(SplashProperty, v);

        private static bool Active => TesseraResources.IsFlow && TesseraResources.AnimationsEnabled;

        // ------------------------------------------------------------------
        // 水波
        // ------------------------------------------------------------------

        private static readonly PointerEventHandler PressedHandler = OnPressed;

        private static void OnRippleChanged(DependencyObject d, DependencyPropertyChangedEventArgs e)
        {
            if (d is not UIElement el) return;
            el.RemoveHandler(UIElement.PointerPressedEvent, PressedHandler);
            if ((bool)e.NewValue) el.AddHandler(UIElement.PointerPressedEvent, PressedHandler, handledEventsToo: true);
        }

        private static void OnPressed(object sender, PointerRoutedEventArgs e)
        {
            if (!Active || sender is not FrameworkElement el || el.ActualWidth <= 0) return;
            var p = e.GetCurrentPoint(el).Position;
            var host = Host(el, clip: true);
            var w = (float)el.ActualWidth;
            var h = (float)el.ActualHeight;
            var center = new Vector2((float)p.X, (float)p.Y);
            var far = 2 * MathF.Sqrt(MathF.Pow(MathF.Max(center.X, w - center.X), 2) + MathF.Pow(MathF.Max(center.Y, h - center.Y), 2));
            var color = WaveColor(el);
            for (var ring = 0; ring < 2; ring++)
                Ring(host, center, far / 2, color, thickness: far * (ring == 0 ? 0.12f : 0.08f), delayMs: ring * 90, durationMs: TesseraFlowLimits.RippleDurationMs);
        }

        /// <summary>水波颜色：实色按钮用白色 22%，其余用文字色 13%</summary>
        private static Color WaveColor(FrameworkElement el)
        {
            var fg = (el as Control)?.Foreground as SolidColorBrush;
            var c = fg?.Color ?? Colors.Black;
            var light = c.R + c.G + c.B > 600;
            return Color.FromArgb((byte)(light ? 56 : 33), c.R, c.G, c.B);
        }

        // ------------------------------------------------------------------
        // 开关、复选框、单选的涟漪与水珠弹跳
        // ------------------------------------------------------------------

        private static void OnSplashChanged(DependencyObject d, DependencyPropertyChangedEventArgs e)
        {
            if (!(bool)e.NewValue) return;
            switch (d)
            {
                case ToggleSwitch ts:
                    ts.Toggled += (_, _) => Splash(ts, ts.IsOn, isSwitch: true);
                    break;
                case ToggleButton tb: // CheckBox、RadioButton
                    tb.Checked += (_, _) => Splash(tb, true, isSwitch: false);
                    tb.Unchecked += (_, _) => Splash(tb, false, isSwitch: false);
                    break;
            }
        }

        private static void Splash(FrameworkElement el, bool on, bool isSwitch)
        {
            if (!Active) return;
            // 开关轨道 52×30 在控件左侧；复选框、单选的方块 20×20 在左侧
            var y = (float)el.ActualHeight / 2;
            var center = isSwitch ? new Vector2(on ? 52 - 15 : 15, y) : new Vector2(10, y);
            var radius = isSwitch ? 36f : 26f;
            var accent = TesseraResources.Brush(on ? "TesseraAccentControlBrush" : "TesseraTextTertiaryBrush") as SolidColorBrush;
            var c = accent?.Color ?? Colors.Gray;
            var color = Color.FromArgb(190, c.R, c.G, c.B);
            var host = Host(el, clip: false);
            for (var ring = 0; ring < 2; ring++)
                Ring(host, center, radius, color, thickness: ring == 0 ? 1.5f : 1f, delayMs: ring * 110, durationMs: TesseraFlowLimits.SplashDurationMs, fromScale: 0.3f);
            if (!isSwitch && on) Bead(el, center);
        }

        /// <summary>水珠弹跳：0.82 → 1.12 → 0.97 → 1</summary>
        private static void Bead(UIElement el, Vector2 center)
        {
            var v = ElementCompositionPreview.GetElementVisual(el);
            v.CenterPoint = new Vector3(center, 0);
            var a = v.Compositor.CreateVector3KeyFrameAnimation();
            a.InsertKeyFrame(0.25f, new Vector3(0.82f, 0.82f, 1));
            a.InsertKeyFrame(0.55f, new Vector3(1.12f, 1.12f, 1));
            a.InsertKeyFrame(0.8f, new Vector3(0.97f, 0.97f, 1));
            a.InsertKeyFrame(1f, Vector3.One);
            a.Duration = TimeSpan.FromMilliseconds(480);
            v.StartAnimation("Scale", a);
        }

        // ------------------------------------------------------------------
        // 公共：在控件上方叠一层 ShapeVisual，画一圈会扩散、淡出的环
        // ------------------------------------------------------------------

        private static ShapeVisual Host(FrameworkElement el, bool clip)
        {
            var compositor = ElementCompositionPreview.GetElementVisual(el).Compositor;
            if (ElementCompositionPreview.GetElementChildVisual(el) is not ShapeVisual host)
            {
                host = compositor.CreateShapeVisual();
                ElementCompositionPreview.SetElementChildVisual(el, host);
            }
            host.Size = new Vector2((float)el.ActualWidth, (float)el.ActualHeight);
            if (clip)
            {
                var g = compositor.CreateRoundedRectangleGeometry();
                g.Size = host.Size;
                var r = (float)((el as Control)?.CornerRadius.TopLeft ?? 0);
                g.CornerRadius = new Vector2(MathF.Min(r, host.Size.Y / 2));
                host.Clip = compositor.CreateGeometricClip(g);
            }
            else host.Clip = null;
            return host;
        }

        private static void Ring(ShapeVisual host, Vector2 center, float radius, Color color, float thickness, int delayMs, int durationMs, float fromScale = 0f)
        {
            var c = host.Compositor;
            var geometry = c.CreateEllipseGeometry();
            geometry.Center = center;
            geometry.Radius = new Vector2(radius);
            var shape = c.CreateSpriteShape(geometry);
            shape.StrokeBrush = c.CreateColorBrush(color);
            shape.StrokeThickness = thickness;
            shape.CenterPoint = center;
            shape.Scale = new Vector2(fromScale);
            host.Shapes.Add(shape);

            var easing = c.CreateCubicBezierEasingFunction(new Vector2(0.2f, 0.7f), new Vector2(0.2f, 1f));
            var batch = c.CreateScopedBatch(CompositionBatchTypes.Animation);
            var grow = c.CreateVector2KeyFrameAnimation();
            grow.InsertKeyFrame(0f, new Vector2(fromScale));
            grow.InsertKeyFrame(1f, Vector2.One, easing);
            grow.Duration = TimeSpan.FromMilliseconds(durationMs);
            grow.DelayTime = TimeSpan.FromMilliseconds(delayMs);
            grow.DelayBehavior = AnimationDelayBehavior.SetInitialValueBeforeDelay;
            shape.StartAnimation("Scale", grow);
            // 颜色淡出：前 60% 保持，之后渐隐
            var fade = c.CreateColorKeyFrameAnimation();
            fade.InsertKeyFrame(0.6f, color);
            fade.InsertKeyFrame(1f, Color.FromArgb(0, color.R, color.G, color.B));
            fade.Duration = TimeSpan.FromMilliseconds(durationMs);
            fade.DelayTime = TimeSpan.FromMilliseconds(delayMs);
            shape.StrokeBrush.StartAnimation("Color", fade);
            batch.End();
            batch.Completed += (_, _) => host.Shapes.Remove(shape);
        }
    }
}
