using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Runtime.InteropServices.WindowsRuntime;
using System.Threading.Tasks;
using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Automation.Peers;
using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Media;
using Microsoft.UI.Xaml.Media.Imaging;
using Windows.Graphics.Imaging;

namespace TesseraSample
{
    /// <summary>
    /// 持续集成用：TesseraSample.exe --snapshot &lt;目录&gt;
    /// 首批组件、表单、数据展示三页 × 三种外观 × 浅色 / 深色，每种截一张，并做无障碍检查：
    /// 每个可聚焦的元素都要有读屏名称，尺寸不小于 40×40（Windows 触控目标的最小建议值）。
    /// 结果写到 a11y.txt；有问题时退出码为 1，出错为 2。
    /// 高对比度主题由系统设置决定，应用无法强制切换，这里不截。
    /// </summary>
    public static class Snapshot
    {
        public static bool Running { get; private set; }
        private const double MinTarget = 40;

        public static async Task RunAsync(MainWindow window, string dir)
        {
            Running = true;
            var code = 0;
            try
            {
                Directory.CreateDirectory(dir);
                var report = new List<string>();
                var checkedCount = 0;
                foreach (var pageIndex in new[] { 0, 1, 2 })
                foreach (var shape in new[] { "tile", "round", "flow" })
                {
                    window.Page = pageIndex;
                    var page = pageIndex switch { 1 => "forms-", 2 => "data-", _ => "" };
                    // 进度：如果进程在原生代码里崩溃（没有托管异常），可以从这里看到停在哪一步
                    File.AppendAllText(Path.Combine(dir, "progress.txt"), $"{page}{shape} 开始\n");
                    foreach (var (theme, name) in new[] { (1, "light"), (2, "dark") })
                    {
                        window.Apply(shape, theme);
                        await Task.Delay(1200); // 等待布局与入场动效结束
                        var root = window.GalleryRoot;
                        await SavePng(root, Path.Combine(dir, $"{page}{shape}-{name}.png"));
                        if (theme != 1) continue; // 无障碍只与结构有关，每种外观检查一次
                        foreach (var el in Descendants(root))
                        {
                            if (el.Visibility != Visibility.Visible || el.ActualWidth <= 0) continue;
                            var peer = FrameworkElementAutomationPeer.CreatePeerForElement(el);
                            if (peer is null || !peer.IsKeyboardFocusable() || !peer.IsEnabled()) continue;
                            checkedCount++;
                            var label = peer.GetName();
                            var type = el.GetType().Name;
                            if (string.IsNullOrWhiteSpace(label)) report.Add($"{page}{shape}：{type} 没有读屏名称（位于 {Ancestry(el)}）");
                            if (el.ActualWidth < MinTarget || el.ActualHeight < MinTarget)
                                report.Add($"{page}{shape}：{type}“{label}”尺寸只有 {el.ActualWidth:0}×{el.ActualHeight:0}");
                        }
                    }
                }
                if (checkedCount < 30) report.Add($"只找到 {checkedCount} 个可聚焦元素，检查本身可能失效");
                var lines = new List<string> { $"无障碍检查：共检查 {checkedCount} 个可聚焦元素，问题 {report.Count} 个" };
                lines.AddRange(report);
                File.WriteAllLines(Path.Combine(dir, "a11y.txt"), lines);
                code = report.Count == 0 ? 0 : 1;
            }
            catch (Exception e)
            {
                File.WriteAllText(Path.Combine(dir, "error.txt"), e.ToString());
                code = 2;
            }
            Environment.Exit(code);
        }

        private static IEnumerable<FrameworkElement> Descendants(DependencyObject root)
        {
            for (var i = 0; i < VisualTreeHelper.GetChildrenCount(root); i++)
            {
                var child = VisualTreeHelper.GetChild(root, i);
                if (child is FrameworkElement fe) yield return fe;
                foreach (var d in Descendants(child)) yield return d;
            }
        }

        private static async Task SavePng(UIElement el, string path)
        {
            var rtb = new RenderTargetBitmap();
            await rtb.RenderAsync(el);
            var pixels = (await rtb.GetPixelsAsync()).ToArray();
            using var file = File.Create(path);
            var encoder = await BitmapEncoder.CreateAsync(BitmapEncoder.PngEncoderId, file.AsRandomAccessStream());
            encoder.SetPixelData(BitmapPixelFormat.Bgra8, BitmapAlphaMode.Premultiplied, (uint)rtb.PixelWidth, (uint)rtb.PixelHeight, 96, 96, pixels);
            await encoder.FlushAsync();
        }

        /// <summary>往上三层的类型与名称，方便定位没有读屏名称的元素</summary>
        private static string Ancestry(DependencyObject el)
        {
            var parts = new System.Collections.Generic.List<string>();
            for (var p = VisualTreeHelper.GetParent(el); p != null && parts.Count < 3; p = VisualTreeHelper.GetParent(p))
            {
                var n = p is FrameworkElement f && !string.IsNullOrEmpty(f.Name) ? $"#{f.Name}" : "";
                parts.Add(p.GetType().Name + n);
            }
            return string.Join(" ← ", parts);
        }
    }
}
