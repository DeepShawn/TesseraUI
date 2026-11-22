using System.Collections.Generic;
using Microsoft.UI.Xaml;

namespace TesseraSample
{
    public sealed partial class MainWindow : Window
    {
        private static readonly string[] Shapes = { "tile", "round", "flow" };

        public MainWindow()
        {
            InitializeComponent();
            AppWindow.Resize(new Windows.Graphics.SizeInt32(560, 1000));
            ShapePicker.Items = new List<string> { "拼贴", "圆融", "流韵" };
            ThemePicker.Items = new List<string> { "跟随系统", "浅色", "深色" };
            GalleryPicker.Items = new List<string> { "首批组件", "表单与输入", "数据展示" };
            ShapePicker.SelectionChanged += (_, i) => Apply(Shapes[i], ThemePicker.SelectedIndex);
            ThemePicker.SelectionChanged += (_, i) => Apply(Shapes[ShapePicker.SelectedIndex], i);
            GalleryPicker.SelectionChanged += (_, i) => { Page = i; Apply(Shapes[ShapePicker.SelectedIndex], ThemePicker.SelectedIndex); };
            Apply("tile", 0);
        }

        public FrameworkElement GalleryRoot => (FrameworkElement)Host.Content;
        /// <summary>显示哪一页：0 首批组件，1 v0.5 表单与输入，2 v0.6 数据展示</summary>
        public int Page { get; set; }
        public FrameworkElement Scroll => Scroller;

        /// <summary>切换外观与主题。外观字典里的值由样式在加载时读取，所以切换后重新创建组件总览</summary>
        public void Apply(string shape, int theme)
        {
            // 流韵只在性能足够的设备上启用，否则显示为圆融
            if (shape == "flow" && !TesseraUI.TesseraResources.FlowCapable && !Snapshot.Running) shape = "round";
            App.SetShape(shape);
            var target = theme switch { 1 => ElementTheme.Light, 2 => ElementTheme.Dark, _ => ElementTheme.Default };
            // 先切到另一个主题再切回来，让窗口上已有元素的 ThemeResource 重新取值（外观字典变了）
            Root.RequestedTheme = target == ElementTheme.Dark ? ElementTheme.Light : ElementTheme.Dark;
            Root.RequestedTheme = target;
            Host.Content = Page switch { 1 => new FormsView(), 2 => new DataView(), _ => new GalleryView(Toasts) };
        }
    }
}
