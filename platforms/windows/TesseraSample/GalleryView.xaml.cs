using System;
using System.Collections.Generic;
using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using TesseraUI;

namespace TesseraSample
{
    public sealed partial class GalleryView : UserControl
    {
        public GalleryView(TesseraToastHost toasts)
        {
            InitializeComponent();
            Segments.Items = new List<string> { "日", "周", "月", "全年" };
            Segments.SelectedIndex = 1;
            PrimaryButton.Click += (_, _) => toasts.Show("已保存", icon: "check");
            ToastButton.Click += (_, _) => toasts.Show("已移到回收站", icon: "trash", actionTitle: "撤销", onAction: () => { });
            DialogButton.Click += async (_, _) =>
            {
                var dialog = new ContentDialog
                {
                    Style = (Style)Application.Current.Resources["TesseraContentDialogStyle"],
                    Title = "发布 v0.4？",
                    Content = "发布后会生成 0.4.0 标签和更新日志。",
                    PrimaryButtonText = "发布",
                    CloseButtonText = "取消",
                    XamlRoot = XamlRoot,
                    RequestedTheme = ActualTheme,
                };
                await dialog.ShowAsync();
            };
            SheetButton.Click += (_, _) => toasts.Show("底部抽屉见 README 的 TesseraSheet 用法", icon: "info");
        }
    }
}
