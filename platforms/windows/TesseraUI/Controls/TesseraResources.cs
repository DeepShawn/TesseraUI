using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Media;

namespace TesseraUI
{
    /// <summary>代码里取 TesseraUI 资源；找不到时返回透明画刷，不会抛异常（例如忘了合并资源字典）。</summary>
    public static class TesseraResources
    {
        private static readonly SolidColorBrush Transparent = new(Microsoft.UI.Colors.Transparent);

        public static Brush Brush(string key) =>
            Application.Current?.Resources is { } r && r.TryGetValue(key, out var v) && v is Brush b ? b : Transparent;

        public static double Double(string key, double fallback) =>
            Application.Current?.Resources is { } r && r.TryGetValue(key, out var v) && v is double d ? d : fallback;

        /// <summary>系统“动画效果”开关（设置 → 辅助功能 → 视觉效果）。关闭时所有弹簧改为瞬时。</summary>
        public static bool AnimationsEnabled => new Windows.UI.ViewManagement.UISettings().AnimationsEnabled;
    }
}
