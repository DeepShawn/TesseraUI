package com.tesseraui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.tesseraui.Tessera
import com.tesseraui.TesseraRadius
import com.tesseraui.TesseraRound
import com.tesseraui.TesseraSpace
import com.tesseraui.TesseraText
import com.tesseraui.foundation.isRound
import com.tesseraui.foundation.roundRadius
import com.tesseraui.foundation.showOutline

// ------------------------------------------------------------------
// 表单 Form：中文校验提示；提交时校验全部字段，出错的字段显示提示
// ------------------------------------------------------------------

/** 校验规则：返回 null 表示通过，否则返回中文提示 */
typealias TesseraRule = (String) -> String?

/** 常用规则，提示语与 Web 端 validateField 一致 */
object TesseraRules {
    fun required(name: String): TesseraRule = { if (it.isBlank()) "请填写$name" else null }
    fun minLength(n: Int): TesseraRule = { if (it.isNotEmpty() && it.length < n) "至少 $n 个字" else null }
    fun maxLength(n: Int): TesseraRule = { if (it.length > n) "最多 $n 个字，已超出 ${it.length - n} 个" else null }
    val email: TesseraRule = { if (it.isNotEmpty() && !Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$").matches(it)) "邮箱格式不正确" else null }
    /** 中国大陆手机号：1 开头 11 位，允许空格和连字符分隔 */
    val phone: TesseraRule = { v ->
        val d = normalizeDigits(v).filter { it.isDigit() }
        if (v.isNotEmpty() && !Regex("^1[3-9]\\d{9}$").matches(d)) "请输入 11 位手机号" else null
    }
    fun pattern(regex: Regex, message: String): TesseraRule = { if (it.isNotEmpty() && !regex.matches(it)) message else null }
}

/**
 * 表单状态。每个字段用 field(name, value, rules) 取当前错误；submit() 校验全部字段并显示错误，返回是否全部通过。
 * 用户还没提交、也没离开过某个字段时，不提前显示错误（不在第一次输入时就报错）。
 */
@Stable
class TesseraFormState {
    private val touched = mutableStateMapOf<String, Boolean>()
    private val errors = mutableStateMapOf<String, String?>()
    private val rules = mutableMapOf<String, Pair<() -> String, List<TesseraRule>>>()
    private var submitted = false

    /** 注册字段并返回要显示的错误（未提交且未触碰时为 null） */
    fun field(name: String, value: String, vararg rule: TesseraRule): String? {
        rules[name] = { value } to rule.toList()
        val e = rule.firstNotNullOfOrNull { it(value) }
        errors[name] = e
        return if (submitted || touched[name] == true) e else null
    }
    /** 字段失焦时调用：之后开始显示这个字段的错误 */
    fun touch(name: String) { touched[name] = true }
    /** 校验全部字段；返回第一个出错字段的名字，全部通过返回 null */
    fun submit(): String? {
        submitted = true
        touched.keys.toList().forEach { touched[it] = true }
        rules.forEach { (k, v) -> touched[k] = true; errors[k] = v.second.firstNotNullOfOrNull { r -> r(v.first()) } }
        return rules.keys.firstOrNull { errors[it] != null }
    }
    val isValid: Boolean get() = errors.values.all { it == null }
}

@Composable
fun rememberTesseraFormState(): TesseraFormState = remember { TesseraFormState() }

/** 表单：字段之间 20dp 间距 */
@Composable
fun TesseraForm(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(TesseraSpace.s5), content = content)
}

/**
 * 表单字段：标签（必填时后面有一个“*”，读屏读作“必填”）+ 内容 + 帮助或错误提示。
 * 用于给没有内置标签的控件（滑块、评分、上传、选择组等）加上统一的标签和提示。
 */
@Composable
fun TesseraFormField(
    label: String,
    modifier: Modifier = Modifier,
    required: Boolean = false,
    help: String? = null,
    error: String? = null,
    content: @Composable () -> Unit,
) {
    val c = Tessera.colors
    Column(modifier.fillMaxWidth().semantics(mergeDescendants = false) { if (error != null) error(error) }, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row {
            TesseraText(label, style = Tessera.type.label)
            if (required) TesseraText(" *", style = Tessera.type.label, color = c.dangerText, modifier = Modifier.semantics { this.contentDescription = "必填" })
        }
        content()
        val msg = error ?: help
        if (msg != null) TesseraText(msg, style = Tessera.type.caption, color = if (error != null) c.dangerText else c.textSecondary)
    }
}

/** 字段分组：一块带标题的面板（拼贴是带描边的砖面，圆融是大圆角无描边） */
@Composable
fun TesseraFieldGroup(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = Tessera.colors
    val shape = RoundedCornerShape(if (isRound) roundRadius(TesseraRound.card) else TesseraRadius.lg)
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(c.bgSurface)
            .then(if (showOutline) Modifier.border(1.dp, c.borderSubtle, shape) else Modifier)
            .padding(TesseraSpace.s5),
        verticalArrangement = Arrangement.spacedBy(TesseraSpace.s4),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(TesseraSpace.s1)) {
            TesseraText(title, style = Tessera.type.title3, modifier = Modifier.semantics { heading() })
            if (description != null) TesseraText(description, style = Tessera.type.caption, color = c.textSecondary)
        }
        content()
    }
}
