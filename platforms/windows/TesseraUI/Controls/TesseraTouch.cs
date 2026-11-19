using System;
using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Controls.Primitives;
using Microsoft.UI.Xaml.Media;

namespace TesseraUI
{
    /// <summary>
    /// 触控目标：系统复合控件（时间选择、颜色选择等）内部的按钮、输入框、滑块默认只有 32 高，
    /// 不到 Windows 建议的 40。给外层控件设 <c>tessera:TesseraTouch.MinTarget="40"</c>，
    /// 加载后把模板里所有可交互的部分撑到至少这个高度（只改 MinHeight，不换样式）。
    /// </summary>
    public static class TesseraTouch
    {
        public static readonly DependencyProperty MinTargetProperty = DependencyProperty.RegisterAttached(
            "MinTarget", typeof(double), typeof(TesseraTouch), new PropertyMetadata(0d, OnMinTargetChanged));

        public static double GetMinTarget(DependencyObject o) => (double)o.GetValue(MinTargetProperty);
        public static void SetMinTarget(DependencyObject o, double v) => o.SetValue(MinTargetProperty, v);

        private static void OnMinTargetChanged(DependencyObject d, DependencyPropertyChangedEventArgs e)
        {
            if (d is not FrameworkElement fe || (double)e.NewValue <= 0) return;
            fe.Loaded += (_, _) =>
            {
                Apply(fe);
                // 有的模板部件在第一次布局之后才生成，再补一遍
                fe.DispatcherQueue.TryEnqueue(Microsoft.UI.Dispatching.DispatcherQueuePriority.Low, () => Apply(fe));
            };
        }

        /// <summary>把 root 模板里的按钮、输入框、下拉框、滑块的最小高度设为 MinTarget</summary>
        public static void Apply(FrameworkElement root)
        {
            var min = GetMinTarget(root);
            Walk(root, el =>
            {
                if (ReferenceEquals(el, root)) return;
                if (el is ButtonBase or TextBox or ComboBox or RangeBase && el is Control c && c.MinHeight < min) c.MinHeight = min;
            });
        }

        private static void Walk(DependencyObject o, Action<FrameworkElement> visit)
        {
            if (o is FrameworkElement fe) visit(fe);
            var n = VisualTreeHelper.GetChildrenCount(o);
            for (var i = 0; i < n; i++) Walk(VisualTreeHelper.GetChild(o, i), visit);
        }
    }
}
