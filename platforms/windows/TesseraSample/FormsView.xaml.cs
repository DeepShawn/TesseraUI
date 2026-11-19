using System;
using System.Collections.Generic;
using Microsoft.UI.Xaml.Controls;
using TesseraUI;

namespace TesseraSample
{
    public sealed partial class FormsView : UserControl
    {
        public FormsView()
        {
            InitializeComponent();
            Range.Items = new List<string> { "日", "周", "月" };
            Format.Items = new List<string> { "加粗", "斜体", "下划线" };
            Format.SetSelected(new[] { 0, 2 });
            Otp.Code = "2026";
            City.Options = new List<string> { "杭州", "广州", "苏州", "福州", "郑州" };
            City.Box.Header = "城市";
            City.Box.Text = "杭州";
            City.Box.PlaceholderText = "输入城市名";
            Tags.Tags.Add("设计");
            Tags.Tags.Add("动效");
            Price.Lower = 200; Price.Upper = 800;
            Upload.Files.Add(new TesseraFileUpload.Item { Name = "设计稿.pdf", Size = 2_400_000 });
            Upload.Files.Add(new TesseraFileUpload.Item { Name = "封面.png", Size = 820_000, Progress = 0.62 });
            Calendar.SetDisplayDate(new DateTimeOffset(2026, 11, 1, 0, 0, 0, TimeSpan.Zero));
            Calendar.SelectedDates.Add(new DateTimeOffset(2026, 11, 20, 0, 0, 0, TimeSpan.Zero));
            RangeCal.SetRange(new DateTimeOffset(2026, 11, 24, 0, 0, 0, TimeSpan.Zero), new DateTimeOffset(2026, 11, 27, 0, 0, 0, TimeSpan.Zero));
            Submit.Click += (_, _) => Validate();
            // 截图里展示提交后的状态（显示中文校验提示）
            Loaded += (_, _) => Validate();
        }

        private void Validate()
        {
            NameField.Error = TesseraRules.Check(NameBox.Text, TesseraRules.Required("姓名"));
            PhoneField.Error = TesseraRules.Check(PhoneBox.Text, TesseraRules.Required("手机号"), TesseraRules.Phone);
            MailField.Error = TesseraRules.Check(MailBox.Text, TesseraRules.Email);
        }
    }
}
