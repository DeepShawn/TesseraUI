using System;
using System.Linq;
using Microsoft.UI.Xaml;

namespace TesseraSample
{
    public partial class App : Application
    {
        private MainWindow? _window;

        public App() { InitializeComponent(); }

        protected override void OnLaunched(LaunchActivatedEventArgs args)
        {
            _window = new MainWindow();
            _window.Activate();
            // --snapshot <目录>：截图与无障碍检查，完成后以检查结果作为退出码
            var argv = Environment.GetCommandLineArgs();
            var i = Array.IndexOf(argv, "--snapshot");
            if (i >= 0)
            {
                var dir = argv.Length > i + 1 ? argv[i + 1] : "snapshots";
                // 事件处理里的异常不在 Snapshot 的 try 里：写进 error.txt 再退出，方便在 CI 日志里看到原因
                UnhandledException += (_, e) =>
                {
                    System.IO.Directory.CreateDirectory(dir);
                    System.IO.File.WriteAllText(System.IO.Path.Combine(dir, "error.txt"), e.Exception.ToString());
                    e.Handled = true;
                    Environment.Exit(2);
                };
                _ = Snapshot.RunAsync(_window, dir);
            }
        }

        /// <summary>切换外观：移除已合并的外观字典，再把新的加在最后（tile 不需要字典）</summary>
        public static void SetShape(string shape)
        {
            var merged = Current.Resources.MergedDictionaries;
            foreach (var d in merged.Where(d => d.Source?.OriginalString.Contains("TesseraShape") == true).ToList()) merged.Remove(d);
            if (shape is "round" or "flow")
                merged.Add(new ResourceDictionary { Source = new Uri($"ms-appx:///TesseraUI/Themes/TesseraShape{(shape == "round" ? "Round" : "Flow")}.xaml") });
        }
    }
}
