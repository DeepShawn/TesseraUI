using System;
using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Automation;
using Microsoft.UI.Xaml.Controls;

namespace TesseraUI
{
    /// <summary>
    /// 日期范围日历：CalendarView 的多选模式加上范围逻辑。第一次点起点，第二次点终点（先晚后早自动调换），
    /// 中间的日期一起选中；再点一次重新开始。Start / End 为选中的范围，RangeChanged 在两端都选好后触发。
    /// </summary>
    public sealed partial class TesseraDateRangeCalendar : ContentControl
    {
        public CalendarView Calendar { get; } = new() { SelectionMode = CalendarViewSelectionMode.Multiple };
        public DateTimeOffset? Start { get; private set; }
        public DateTimeOffset? End { get; private set; }
        public event EventHandler? RangeChanged;
        private bool _updating;

        public TesseraDateRangeCalendar()
        {
            IsTabStop = false;
            Content = Calendar;
            AutomationProperties.SetName(Calendar, "选择日期范围");
            Calendar.Loaded += (_, _) =>
            {
                if (Application.Current.Resources.TryGetValue("TesseraCalendarViewStyle", out var s) && s is Style style) Calendar.Style = style;
            };
            Calendar.SelectedDatesChanged += (_, e) =>
            {
                if (_updating || e.AddedDates.Count == 0) return;
                var d = e.AddedDates[0].Date;
                _updating = true;
                if (Start == null || End != null)
                {
                    // 重新开始：只保留这一天
                    Calendar.SelectedDates.Clear();
                    Calendar.SelectedDates.Add(d);
                    Start = d; End = null;
                }
                else
                {
                    var a = Start.Value.Date < d ? Start.Value.Date : d;
                    var b = Start.Value.Date < d ? d : Start.Value.Date;
                    Calendar.SelectedDates.Clear();
                    for (var x = a; x <= b; x = x.AddDays(1)) Calendar.SelectedDates.Add(x);
                    Start = a; End = b;
                    RangeChanged?.Invoke(this, EventArgs.Empty);
                }
                _updating = false;
                AutomationProperties.SetHelpText(Calendar, End != null ? $"已选 {Start:M 月 d 日} 至 {End:M 月 d 日}" : $"开始日期 {Start:M 月 d 日}，请选择结束日期");
            };
        }

        /// <summary>设置范围（例如恢复已保存的值）</summary>
        public void SetRange(DateTimeOffset start, DateTimeOffset end)
        {
            _updating = true;
            Calendar.SelectedDates.Clear();
            var a = start.Date < end.Date ? start.Date : end.Date;
            var b = start.Date < end.Date ? end.Date : start.Date;
            for (var x = a; x <= b; x = x.AddDays(1)) Calendar.SelectedDates.Add(x);
            Start = a; End = b;
            Calendar.SetDisplayDate(a);
            _updating = false;
        }
    }
}
