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
    /// <item><c>tessera:TesseraFlow.Splash="True"</c>：开关、复选框、单选切换时溅起两圈涟漪；开关滑块像水滴一样先被拉扁再回弹；复选框、单选像水珠一样弹一下</item>
    /// <item><c>tessera:TesseraFlow.PressMorph="True"</c>：按下时圆角变成胶囊，松手 bouncy 弹回（形状会呼吸）。成组控件（四角圆角不一致）不变形</item>
    /// <item><c>tessera:TesseraFlow.Surface="True"</c>：浮层出现时像从水下浮上来：由淡到实、由 97% 放大到 100%、轻轻上浮。
    /// Web 端还有“由模糊到清晰”，WinUI 的元素模糊需要 Win2D，为了不增加依赖，这里不做模糊</item>
    /// </list>
    /// TesseraUI 的按钮、开关、复选框、单选样式已经默认挂上，不需要手动设置。
    /// </summary>
    public static class TesseraFlow
    {
        public static readonly DependencyProperty RippleProperty =
            DependencyProperty.RegisterAttached("Ripple", typeof(bool), typeof(TesseraFlow), new PropertyMetadata(false, OnRippleChanged));
        public static readonly DependencyProperty SplashProperty =
            DependencyProperty.RegisterAttached("Splash", typeof(bool), typeof(TesseraFlow), new PropertyMetadata(false, OnSplashChanged));

        public static readonly DependencyProperty PressMorphProperty =
            DependencyProperty.RegisterAttached("PressMorph", typeof(bool), typeof(TesseraFlow), new PropertyMetadata(false, OnPressMorphChanged));
        public static readonly DependencyProperty SurfaceProperty =
            DependencyProperty.RegisterAttached("Surface", typeof(bool), typeof(TesseraFlow), new PropertyMetadata(false, OnSurfaceChanged));
        public static bool GetPressMorph(DependencyObject o) => (bool)o.GetValue(PressMorphProperty);
        public static void SetPressMorph(DependencyObject o, bool v) => o.SetValue(PressMorphProperty, v);
        public static bool GetSurface(DependencyObject o) => (bool)o.GetValue(SurfaceProperty);
        public static void SetSurface(DependencyObject o, bool v) => o.SetValue(SurfaceProperty, v);

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
            if (isSwitch) Drop(el);
        }

        /// <summary>开关滑块水滴形变：先被拉扁（1.22 × 0.82），再回弹，最后轻轻晃两下，共 620ms</summary>
        private static void Drop(FrameworkElement toggle)
        {
            if (FindChild(toggle, "SwitchKnob") is not FrameworkElement knob) return;
            var v = ElementCompositionPreview.GetElementVisual(knob);
            v.CenterPoint = new Vector3((float)knob.ActualWidth / 2, (float)knob.ActualHeight / 2, 0);
            var a = v.Compositor.CreateVector3KeyFrameAnimation();
            var linear = v.Compositor.CreateLinearEasingFunction();
            a.InsertKeyFrame(0.18f, new Vector3(1.22f, 0.82f, 1), linear);
            a.InsertKeyFrame(0.40f, new Vector3(0.9f, 1.1f, 1), linear);
            a.InsertKeyFrame(0.62f, new Vector3(1.05f, 0.96f, 1), linear);
            a.InsertKeyFrame(0.82f, new Vector3(0.98f, 1.02f, 1), linear);
            a.InsertKeyFrame(1f, Vector3.One, linear);
            a.Duration = TimeSpan.FromMilliseconds(620);
            v.StartAnimation("Scale", a);
        }

        private static DependencyObject? FindChild(DependencyObject root, string name)
        {
            for (var i = 0; i < VisualTreeHelper.GetChildrenCount(root); i++)
            {
                var child = VisualTreeHelper.GetChild(root, i);
                if (child is FrameworkElement fe && fe.Name == name) return child;
                if (FindChild(child, name) is { } found) return found;
            }
            return null;
        }

        // ------------------------------------------------------------------
        // 形状会呼吸：按下时圆角变成胶囊（120ms），松手 bouncy 弹回（520ms，略微过冲）
        // CornerRadius 不是合成属性，按帧插值；只有按下与松手两次，开销很小
        // ------------------------------------------------------------------

        private static readonly DependencyProperty RestRadiusProperty =
            DependencyProperty.RegisterAttached("RestRadius", typeof(object), typeof(TesseraFlow), new PropertyMetadata(null));
        private static readonly DependencyProperty MorphTokenProperty =
            DependencyProperty.RegisterAttached("MorphToken", typeof(object), typeof(TesseraFlow), new PropertyMetadata(null));
        private static readonly PointerEventHandler MorphDown = (s, _) => Morph(s as Control, pressed: true);
        private static readonly PointerEventHandler MorphUp = (s, _) => Morph(s as Control, pressed: false);

        private static void OnPressMorphChanged(DependencyObject d, DependencyPropertyChangedEventArgs e)
        {
            if (d is not UIElement el) return;
            el.RemoveHandler(UIElement.PointerPressedEvent, MorphDown);
            el.RemoveHandler(UIElement.PointerReleasedEvent, MorphUp);
            el.RemoveHandler(UIElement.PointerCaptureLostEvent, MorphUp);
            el.RemoveHandler(UIElement.PointerExitedEvent, MorphUp);
            if (!(bool)e.NewValue) return;
            el.AddHandler(UIElement.PointerPressedEvent, MorphDown, handledEventsToo: true);
            el.AddHandler(UIElement.PointerReleasedEvent, MorphUp, handledEventsToo: true);
            el.AddHandler(UIElement.PointerCaptureLostEvent, MorphUp, handledEventsToo: true);
            el.AddHandler(UIElement.PointerExitedEvent, MorphUp, handledEventsToo: true);
        }

        private static void Morph(Control? c, bool pressed)
        {
            if (c is null || c.ActualHeight <= 0 || !c.IsEnabled) return;
            var rest = c.GetValue(RestRadiusProperty) as CornerRadius?;
            if (pressed)
            {
                if (!Active) return;
                var r = c.CornerRadius;
                // 成组控件（只有部分角是圆的）保持一整块，不变形
                if (r.TopLeft != r.TopRight || r.TopLeft != r.BottomLeft || r.TopLeft != r.BottomRight) return;
                if (rest is null) { rest = r; c.SetValue(RestRadiusProperty, r); }
                Animate(c, (float)c.CornerRadius.TopLeft, (float)(c.ActualHeight / 2), 120, bouncy: false);
            }
            else if (rest is { } restRadius)
            {
                c.ClearValue(RestRadiusProperty);
                Animate(c, (float)c.CornerRadius.TopLeft, (float)restRadius.TopLeft, 520, bouncy: true, done: () => c.ClearValue(Control.CornerRadiusProperty));
            }
        }

        private static void Animate(Control c, float from, float to, int ms, bool bouncy, Action? done = null)
        {
            var token = new object();
            c.SetValue(MorphTokenProperty, token);
            var start = DateTime.UtcNow;
            EventHandler<object>? tick = null;
            tick = (_, _) =>
            {
                if (!ReferenceEquals(c.GetValue(MorphTokenProperty), token)) { CompositionTarget.Rendering -= tick; return; }
                var t = Math.Min(1, (DateTime.UtcNow - start).TotalMilliseconds / ms);
                // bouncy：带一次轻微过冲的回弹；按下：ease-out
                var k = bouncy ? 1 - Math.Exp(-6 * t) * Math.Cos(7 * t) : 1 - Math.Pow(1 - t, 3);
                if (t >= 1) k = 1;
                var r = Math.Max(0, from + (to - from) * k);
                c.CornerRadius = new CornerRadius(r);
                if (t >= 1) { CompositionTarget.Rendering -= tick; done?.Invoke(); }
            };
            CompositionTarget.Rendering += tick;
        }

        // ------------------------------------------------------------------
        // 浮层浮现
        // ------------------------------------------------------------------

        private static void OnSurfaceChanged(DependencyObject d, DependencyPropertyChangedEventArgs e)
        {
            if (d is FrameworkElement fe && (bool)e.NewValue) fe.Loaded += (_, _) => Surface(fe);
        }

        /// <summary>浮层浮现：420ms，由淡到实、由 97% 到 100%、上浮 6px</summary>
        public static void Surface(FrameworkElement el)
        {
            if (!Active) return;
            var v = ElementCompositionPreview.GetElementVisual(el);
            var c = v.Compositor;
            // 加载时尺寸可能还没算出来，中心点用表达式跟随视觉对象的实际大小
            var center = c.CreateExpressionAnimation("Vector3(this.Target.Size.X / 2, this.Target.Size.Y / 2, 0)");
            v.StartAnimation("CenterPoint", center);
            var easing = c.CreateCubicBezierEasingFunction(new Vector2(0.2f, 0.8f), new Vector2(0.2f, 1f));
            var dur = TimeSpan.FromMilliseconds(420);
            var scale = c.CreateVector3KeyFrameAnimation();
            scale.InsertKeyFrame(0f, new Vector3(0.97f, 0.97f, 1));
            scale.InsertKeyFrame(1f, Vector3.One, easing);
            scale.Duration = dur;
            var fade = c.CreateScalarKeyFrameAnimation();
            fade.InsertKeyFrame(0f, 0f);
            fade.InsertKeyFrame(1f, 1f, easing);
            fade.Duration = dur;
            var rise = c.CreateVector3KeyFrameAnimation();
            rise.InsertKeyFrame(0f, new Vector3(0, 6, 0));
            rise.InsertKeyFrame(1f, Vector3.Zero, easing);
            rise.Duration = dur;
            ElementCompositionPreview.SetIsTranslationEnabled(el, true);
            v.StartAnimation("Scale", scale);
            v.StartAnimation("Opacity", fade);
            v.StartAnimation("Translation", rise);
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
