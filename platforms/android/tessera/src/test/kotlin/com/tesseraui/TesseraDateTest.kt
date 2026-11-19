package com.tesseraui

import com.tesseraui.components.TesseraDate
import com.tesseraui.components.TesseraRules
import com.tesseraui.components.normalizeDigits
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

/** TesseraDate 自己实现了日期运算（minSdk 24 不能直接用 java.time），这里逐日对照 java.time */
class TesseraDateTest {
    @Test fun matchesJavaTime() {
        var d = LocalDate.of(1899, 12, 25)
        while (d.year <= 2101) {
            val t = TesseraDate(d.year, d.monthValue, d.dayOfMonth)
            assertEquals("epochDay $d", d.toEpochDay(), t.epochDay)
            assertEquals("fromEpochDay $d", t, TesseraDate.fromEpochDay(d.toEpochDay()))
            assertEquals("星期 $d", d.dayOfWeek.value, t.dayOfWeek)
            assertEquals("天数 $d", d.lengthOfMonth(), TesseraDate.daysInMonth(d.year, d.monthValue))
            d = d.plusDays(1)
        }
    }

    @Test fun plusMonthsClampsDay() {
        assertEquals(TesseraDate(2024, 2, 29), TesseraDate(2024, 1, 31).plusMonths(1))
        assertEquals(TesseraDate(2025, 2, 28), TesseraDate(2025, 1, 31).plusMonths(1))
        assertEquals(TesseraDate(2025, 12, 31), TesseraDate(2026, 1, 31).plusMonths(-1))
    }

    @Test fun fullWidthDigits() {
        assertEquals("12.5", normalizeDigits("１２．５"))
        assertEquals("-3", normalizeDigits("－３"))
    }

    @Test fun rules() {
        assertEquals("请填写姓名", TesseraRules.required("姓名")(""))
        assertNull(TesseraRules.phone("138 0013 8000"))
        assertNull(TesseraRules.phone("１３８００１３８０００"))
        assertEquals("请输入 11 位手机号", TesseraRules.phone("12345"))
        assertEquals("邮箱格式不正确", TesseraRules.email("a@b"))
    }
}
