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

        /// <summary>当前外观：tile（拼贴）、round（圆融，合并了 TesseraShapeRound.xaml）或 flow（流韵，合并了 TesseraShapeFlow.xaml）</summary>
        public static string Shape =>
            Application.Current?.Resources is { } r && r.TryGetValue("TesseraShape", out var v) && v is string s ? s : "tile";

        /// <summary>圆融或流韵：成组控件合成一整块，没有砖缝和描边</summary>
        public static bool IsRound => Shape is "round" or "flow";

        /// <summary>流韵：圆角介于方砖与胶囊之间，开启流体动效</summary>
        public static bool IsFlow => Shape == "flow";

        /// <summary>
        /// 设备是否适合流韵的持续动效：系统“动画效果”打开、处理器不少于 6 核、内存不少于 4GB。
        /// 不满足时，应用应改为合并 TesseraShapeRound.xaml。
        /// </summary>
        public static bool FlowCapable =>
            AnimationsEnabled && System.Environment.ProcessorCount >= TesseraFlowLimits.MinCores &&
            System.GC.GetGCMemoryInfo().TotalAvailableMemoryBytes >= (long)(TesseraFlowLimits.MinMemoryGB * 0.9 * 1024 * 1024 * 1024);

        /// <summary>读取 CornerRadius 资源；找不到时返回 fallback</summary>
        public static CornerRadius Radius(string key, double fallback) =>
            Application.Current?.Resources is { } r && r.TryGetValue(key, out var v) && v is CornerRadius c ? c : new CornerRadius(fallback);

        /// <summary>系统“动画效果”开关（设置 → 辅助功能 → 视觉效果）。关闭时所有弹簧改为瞬时。</summary>
        public static bool AnimationsEnabled => new Windows.UI.ViewManagement.UISettings().AnimationsEnabled;
    }
}
