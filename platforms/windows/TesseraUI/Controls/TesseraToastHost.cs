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

namespace TesseraUI
{
    /// <summary>
    /// 提示消息宿主：放在页面根 Grid 的最上层（跨所有行列）。
    /// 消息是一块反色砖，从顶部落下（bouncy 弹簧），停留 3 秒（带操作 5 秒），可向上滑走；出现时由读屏朗读。
    /// <code>ToastHost.Show("已保存", icon: "check");</code>
    /// </summary>
    public sealed partial class TesseraToastHost : Grid
    {
        private Border? _current;
        private DispatcherTimer? _timer;

        public TesseraToastHost()
        {
            VerticalAlignment = VerticalAlignment.Top;
            HorizontalAlignment = HorizontalAlignment.Stretch;
            Padding = new Thickness(16, 16, 16, 0);
            Canvas.SetZIndex(this, TesseraZ.Toast);
            AutomationProperties.SetLandmarkType(this, AutomationLandmarkType.Custom);
            AutomationProperties.SetLocalizedLandmarkType(this, "通知");
        }

        public void Show(string text, string? icon = null, string? actionTitle = null, Action? onAction = null)
        {
            Dismiss(animated: false);

            var row = new StackPanel { Orientation = Orientation.Horizontal, Spacing = 12, VerticalAlignment = VerticalAlignment.Center };
            var fg = TesseraResources.Brush("TesseraTextInverseBrush");
            if (icon is not null) row.Children.Add(new TesseraIcon { Icon = icon, Size = 20, Foreground = fg });
            row.Children.Add(new TextBlock
            {
                Text = text, Foreground = fg, FontSize = 14, TextWrapping = TextWrapping.Wrap, MaxLines = 2,
                VerticalAlignment = VerticalAlignment.Center, MaxWidth = 360,
            });
            if (actionTitle is not null)
            {
                var action = new HyperlinkButton
                {
                    Content = actionTitle, Foreground = fg, FontWeight = Microsoft.UI.Text.FontWeights.SemiBold,
                    MinHeight = 44, Padding = new Thickness(8, 0, 8, 0),
                };
                action.Click += (_, _) => { onAction?.Invoke(); Dismiss(); };
                row.Children.Add(action);
            }

            var toast = new Border
            {
                Child = row,
                MinHeight = 48,
                MaxWidth = 480,
                HorizontalAlignment = HorizontalAlignment.Center,
                Padding = new Thickness(16, 0, actionTitle is null ? 20 : 8, 0),
                CornerRadius = new CornerRadius(18),
                Background = TesseraResources.Brush("TesseraBgInverseBrush"),
                BorderBrush = TesseraResources.Brush("TesseraEdgeSolidBrush"),
                BorderThickness = new Thickness(0, 0, 0, 2),
                ManipulationMode = ManipulationModes.TranslateY | ManipulationModes.TranslateInertia,
            };
            AutomationProperties.SetName(toast, text);
            AutomationProperties.SetLiveSetting(toast, AutomationLiveSetting.Polite);
            toast.ManipulationDelta += (_, e) =>
            {
                var y = e.Cumulative.Translation.Y;
                SetOffset(toast, (float)(y > 0 ? y * 0.3 : y)); // 往下拉有阻尼，往上跟手
            };
            toast.ManipulationCompleted += (_, e) =>
            {
                if (e.Cumulative.Translation.Y < -24 || e.Velocities.Linear.Y * 1000 < -TesseraMotion.FlingVelocity) Dismiss();
                else Spring(toast, 0, TesseraMotion.Bouncy);
            };

            Children.Add(toast);
            _current = toast;
            ElementCompositionPreview.SetIsTranslationEnabled(toast, true);
            var visual = ElementCompositionPreview.GetElementVisual(toast);
            if (TesseraResources.AnimationsEnabled)
            {
                visual.Opacity = 0;
                visual.Properties.InsertVector3("Translation", new Vector3(0, -24, 0));
                Spring(toast, 0, TesseraMotion.Bouncy);
                Fade(visual, 1, TesseraMotion.DurationFast);
            }

            toast.Loaded += (_, _) =>
            {
                var peer = FrameworkElementAutomationPeer.FromElement(toast) ?? FrameworkElementAutomationPeer.CreatePeerForElement(toast);
                peer?.RaiseNotificationEvent(AutomationNotificationKind.Other, AutomationNotificationProcessing.ImportantMostRecent, text, "TesseraToast");
            };

            _timer = new DispatcherTimer { Interval = TimeSpan.FromMilliseconds(actionTitle is null ? TesseraMotion.ToastDurationMs : TesseraMotion.ToastDurationWithActionMs) };
            _timer.Tick += (_, _) => Dismiss();
            _timer.Start();
        }

        public void Dismiss(bool animated = true)
        {
            _timer?.Stop();
            _timer = null;
            if (_current is not { } toast) return;
            _current = null;
            if (!animated || !TesseraResources.AnimationsEnabled) { Children.Remove(toast); return; }
            var visual = ElementCompositionPreview.GetElementVisual(toast);
            var batch = visual.Compositor.CreateScopedBatch(CompositionBatchTypes.Animation);
            Fade(visual, 0, TesseraMotion.DurationFast);
            batch.End();
            batch.Completed += (_, _) => Children.Remove(toast);
        }

        private static void SetOffset(UIElement el, float y)
        {
            var v = ElementCompositionPreview.GetElementVisual(el);
            v.StopAnimation("Translation");
            v.Properties.InsertVector3("Translation", new Vector3(0, y, 0));
        }

        private static void Spring(UIElement el, float y, TesseraSpring s)
        {
            var v = ElementCompositionPreview.GetElementVisual(el);
            var a = v.Compositor.CreateSpringVector3Animation();
            a.FinalValue = new Vector3(0, y, 0);
            a.DampingRatio = s.DampingRatio;
            a.Period = s.Period;
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
