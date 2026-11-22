using System;
using System.Collections.Generic;
using System.Linq;
using System.Runtime.InteropServices.WindowsRuntime;
using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Media;
using Microsoft.UI.Xaml.Media.Imaging;
using TesseraUI;

namespace TesseraSample
{
    /// <summary>示例数据：订单</summary>
    public sealed record SampleOrder(string Id, string Customer, string City, string Amount, string Status);

    public sealed partial class DataView : UserControl
    {
        private static readonly SampleOrder[] OrdersData =
        {
            new("A-1024", "王小明", "杭州", "1,280", "已发货"),
            new("A-1025", "李思", "北京", "356", "待付款"),
            new("A-1026", "陈一", "广州", "12,040", "已完成"),
            new("A-1027", "张安", "成都", "89", "已退款"),
            new("A-1028", "赵宁", "上海", "2,400", "已发货"),
        };

        private static string Tone(string s) => s switch { "已完成" => "success", "待付款" => "warning", "已退款" => "neutral", _ => "accent" };

        private static IEnumerable<TesseraTableColumn> Columns() => new[]
        {
            new TesseraTableColumn { Header = "客户", Width = 96, Sortable = true, Text = o => ((SampleOrder)o).Customer },
            new TesseraTableColumn { Header = "金额", Width = 100, Sortable = true, Numeric = true, Text = o => ((SampleOrder)o).Amount },
            new TesseraTableColumn { Header = "城市", Width = 84, Sortable = true, Text = o => ((SampleOrder)o).City },
            new TesseraTableColumn { Header = "状态", Width = 100, Text = o => ((SampleOrder)o).Status, Cell = o => StatusTag(((SampleOrder)o).Status) },
            new TesseraTableColumn { Header = "订单号", Width = 100, Text = o => ((SampleOrder)o).Id },
        };

        private static FrameworkElement StatusTag(string s)
        {
            var key = Tone(s) switch { "success" => "TesseraSuccessTagStyle", "warning" => "TesseraWarningTagStyle", "neutral" => "TesseraTagStyle", _ => "TesseraAccentTagStyle" };
            var b = new Border { Style = (Style)Application.Current.Resources[key], HorizontalAlignment = HorizontalAlignment.Left };
            b.Child = new TextBlock { Text = s, Style = (Style)Application.Current.Resources["TesseraTagTextStyle"] };
            return b;
        }

        private static void Setup(TesseraTable t, IEnumerable<TesseraTableColumn> cols, IList<object> rows)
        {
            foreach (var c in cols) t.Columns.Add(c);
            t.Key = o => ((SampleOrder)o).Id;
            t.Rows = rows;
        }

        public DataView()
        {
            InitializeComponent();
            var rows = OrdersData.Cast<object>().ToList();

            Setup(Orders, Columns(), rows);
            Orders.Selectable = true; Orders.PageSize = 4; Orders.Layout = TesseraTableLayout.Grid;
            Orders.Selected.Add("A-1025");
            Setup(Cards, Columns(), rows.Take(2).ToList());
            Cards.Selectable = true; Cards.Layout = TesseraTableLayout.Cards;
            Cards.Selected.Add("A-1024");
            Setup(Skeleton, Columns().Take(3), rows);
            Skeleton.PageSize = 2; Skeleton.Layout = TesseraTableLayout.Grid; Skeleton.IsLoading = true;
            Setup(Empty, Columns(), rows);
            Empty.Layout = TesseraTableLayout.Grid; Empty.Filter = "深圳";

            // 树
            TreeViewNode Node(string label, params TreeViewNode[] children)
            {
                var n = new TreeViewNode { Content = label, IsExpanded = children.Length > 0 && label is "src" or "components" };
                foreach (var c in children) n.Children.Add(c);
                return n;
            }
            Files.RootNodes.Add(Node("src", Node("components", Node("Button.cs"), Node("Table.cs")), Node("Theme.cs")));
            Files.RootNodes.Add(Node("docs", Node("spec.md")));
            Files.RootNodes.Add(Node("README.md"));

            Faq.Add("如何退换货？", new TextBlock { Text = "收货 7 天内可申请无理由退货，商品需保持完好。" }, expanded: true);
            Faq.Add("多久发货？", new TextBlock { Text = "付款后 48 小时内发货。" });
            Faq.Add("支持哪些付款方式？", new TextBlock { Text = "支付宝、微信支付、银行卡。" });

            Details.Add("订单号", "A-1024");
            Details.Add("下单时间", "2026 年 11 月 19 日 09:30");
            Details.Add("收货地址", "浙江省杭州市西湖区文三路 90 号");
            Details.Add("备注", "工作日送货");

            Stats.Add("本月收入", "¥86,240", delta: "12.5%", trend: 1);
            Stats.Add("新增用户", "1,208", "人", "3.1%", -1);
            Stats.Add("转化率", "4.6", "%");
            Stats.Add("退款率", "0.8", "%", "0.2%", -1);

            Shipping.Add("已签收", "11 月 19 日 14:02", "本人签收，感谢使用", "success");
            Shipping.Add("派送中", "11 月 19 日 09:15", "快递员：周师傅 138 0000 0000", "accent");
            Shipping.Add("已到达杭州转运中心", "11 月 18 日 22:40");
            Shipping.Add("已发货", "11 月 18 日 10:05");

            var popoverBody = new StackPanel { Spacing = 10 };
            popoverBody.Children.Add(new TextBlock { Text = "江浙沪次日达，其他地区 2～3 天送达。", TextWrapping = TextWrapping.Wrap, Style = (Style)Application.Current.Resources["TesseraBodySmTextStyle"], Foreground = TesseraResources.Brush("TesseraTextSecondaryBrush") });
            var actions = new StackPanel { Orientation = Orientation.Horizontal, Spacing = 8 };
            actions.Children.Add(new Button { Content = "知道了", Style = (Style)Application.Current.Resources["TesseraPrimaryButtonStyle"] });
            actions.Children.Add(new Button { Content = "查看详情", Style = (Style)Application.Current.Resources["TesseraSecondaryButtonStyle"] });
            popoverBody.Children.Add(actions);
            Popover.Body = popoverBody;

            Code.Code = "var table = new TesseraTable();\ntable.Columns.Add(new TesseraTableColumn { Header = \"客户\" });\ntable.Selectable = true;\ntable.Rows = orders;";

            var palette = new[] { ("TesseraAccentSolidBrush", "TesseraAccentControlBrush"), ("TesseraPigmentLapisBrush", "TesseraPigmentOchreBrush"), ("TesseraPigmentOliveBrush", "TesseraPigmentCinnabarBrush") };
            var titles = new[] { "秋季上新", "会员日", "以旧换新" };
            for (var i = 0; i < 3; i++)
            {
                var g = new Grid();
                g.Children.Add(new Image { Source = Mosaic(palette[i].Item1, palette[i].Item2), Stretch = Stretch.UniformToFill });
                g.Children.Add(new TextBlock { Text = titles[i], Margin = new Thickness(16), VerticalAlignment = VerticalAlignment.Bottom, Foreground = new SolidColorBrush(Microsoft.UI.Colors.White), Style = (Style)Application.Current.Resources["TesseraTitle3TextStyle"] });
                Banners.Add(g);
            }
            Banners.Go(1);

            var images = new List<ImageSource> { Mosaic(palette[0].Item1, palette[0].Item2), Mosaic(palette[1].Item1, palette[1].Item2) };
            var names = new List<string> { "釉色马赛克", "青金马赛克" };
            foreach (var img in images) Viewer.Images.Add(img);
            foreach (var n in names) Viewer.Descriptions.Add(n);
            OpenViewer.Click += (_, _) => TesseraImageViewer.Show(XamlRoot, images, names);

            Upload.Label = "上传中"; Upload.Value = 0.62;
            Done.Label = "已完成"; Done.Value = 1;
            Preparing.Label = "正在准备…"; Preparing.Value = null;
            Storage.Label = "存储空间"; Storage.Value = 0.36;
            Goal.Label = "本周目标"; Goal.Value = 0.82;

            Update.Actions = new Button { Content = "立即重启", Style = (Style)Application.Current.Resources["TesseraPrimaryButtonStyle"], HorizontalAlignment = HorizontalAlignment.Left };
            var paidActions = new StackPanel { Orientation = Orientation.Horizontal, Spacing = 8 };
            paidActions.Children.Add(new Button { Content = "查看订单", Style = (Style)Application.Current.Resources["TesseraPrimaryButtonStyle"] });
            paidActions.Children.Add(new Button { Content = "继续购物", Style = (Style)Application.Current.Resources["TesseraSecondaryButtonStyle"] });
            Paid.Actions = paidActions;
            Failed.Actions = new Button { Content = "重试", Style = (Style)Application.Current.Resources["TesseraPrimaryButtonStyle"] };
            NoFavorites.Action = new Button { Content = "去逛逛", Style = (Style)Application.Current.Resources["TesseraSecondaryButtonStyle"] };
        }

        /// <summary>示例图片：一片釉色马赛克（不依赖图片资源）</summary>
        private static ImageSource Mosaic(string baseKey, string accentKey)
        {
            const int w = 400, h = 260, cols = 8, rows = 5, gap = 4;
            var baseColor = (TesseraResources.Brush(baseKey) as SolidColorBrush)?.Color ?? Microsoft.UI.Colors.Teal;
            var accent = (TesseraResources.Brush(accentKey) as SolidColorBrush)?.Color ?? Microsoft.UI.Colors.Orange;
            var bmp = new WriteableBitmap(w, h);
            var px = new byte[w * h * 4];
            double cw = (w - gap * (cols + 1)) / (double)cols, ch = (h - gap * (rows + 1)) / (double)rows;
            for (var y = 0; y < h; y++)
            for (var x = 0; x < w; x++)
            {
                var c = baseColor;
                var col = (int)((x - gap) / (cw + gap));
                var row = (int)((y - gap) / (ch + gap));
                var inX = x - gap - col * (cw + gap);
                var inY = y - gap - row * (ch + gap);
                if (col < cols && row < rows && inX >= 0 && inX < cw && inY >= 0 && inY < ch)
                {
                    var k = (row * 7 + col * 3) % 5;
                    c = k switch
                    {
                        0 => accent,
                        1 => Mix(baseColor, accent, 0.7),
                        2 => Mix(baseColor, Microsoft.UI.Colors.White, 0.18),
                        3 => Mix(baseColor, Microsoft.UI.Colors.Black, 0.12),
                        _ => Mix(baseColor, Microsoft.UI.Colors.White, 0.06),
                    };
                }
                var i = (y * w + x) * 4;
                px[i] = c.B; px[i + 1] = c.G; px[i + 2] = c.R; px[i + 3] = 255;
            }
            using (var s = bmp.PixelBuffer.AsStream()) s.Write(px, 0, px.Length);
            bmp.Invalidate();
            return bmp;
        }

        private static Windows.UI.Color Mix(Windows.UI.Color a, Windows.UI.Color b, double t) =>
            Microsoft.UI.ColorHelper.FromArgb(255, (byte)(a.R + (b.R - a.R) * t), (byte)(a.G + (b.G - a.G) * t), (byte)(a.B + (b.B - a.B) * t));
    }
}
