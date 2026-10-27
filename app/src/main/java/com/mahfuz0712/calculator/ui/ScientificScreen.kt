package com.mahfuz0712.calculator.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mahfuz0712.calculator.engine.ExprEngine
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

data class Hist(val expr: String, val result: String)

class SciState {
    var expr by mutableStateOf("")
    var result by mutableStateOf("")
    var degrees by mutableStateOf(true)
    var showFn by mutableStateOf(false)
    var ans by mutableDoubleStateOf(0.0)
    val history = mutableStateListOf<Hist>()
    private var justEvaluated = false

    private val tokens = listOf(
        "asin(", "acos(", "atan(", "sinh(", "cosh(", "tanh(", "sin(", "cos(", "tan(",
        "log2(", "ln(", "log(", "√(", "exp(", "abs(", "cbrt(", "ans"
    )

    fun press(v: String) {
        if (v != "=" && result.startsWith("Error")) result = ""
        when (v) {
            "AC" -> { expr = ""; result = ""; justEvaluated = false }
            "DEL" -> {
                justEvaluated = false
                result = ""
                val t = tokens.firstOrNull { expr.endsWith(it) }
                expr = if (t != null) expr.dropLast(t.length) else expr.dropLast(1)
            }
            "=" -> evaluate()
            else -> {
                if (justEvaluated) {
                    val cont = v in listOf("+", "-", "×", "÷", "^", "^2", "!", "%")
                    expr = if (cont && result.isNotEmpty()) result else ""
                    result = ""
                    justEvaluated = false
                }
                expr += v
            }
        }
    }

    fun evaluate() {
        if (expr.isBlank()) return
        try {
            val v = ExprEngine.evaluate(expr, degrees, mapOf("ans" to ans))
            val t = ExprEngine.format(v)
            history.add(0, Hist(expr, t))
            if (history.size > 50) history.removeAt(history.lastIndex)
            ans = v
            result = t
            justEvaluated = true
        } catch (e: Exception) {
            result = "Error: ${e.message ?: "invalid"}"
            justEvaluated = false
        }
    }

    fun recall(h: Hist) {
        expr = h.expr
        result = ""
        justEvaluated = false
    }

    fun preview(): String? =
        if (expr.isBlank()) null
        else runCatching { ExprEngine.format(ExprEngine.evaluate(expr, degrees, mapOf("ans" to ans))) }.getOrNull()
}

private val basicRows = listOf(
    listOf(Key("AC", "AC", KeyKind.DANGER), Key("⌫", "DEL", KeyKind.OP), Key("%", "%", KeyKind.OP), Key("÷", "÷", KeyKind.OP)),
    listOf(Key("7"), Key("8"), Key("9"), Key("×", "×", KeyKind.OP)),
    listOf(Key("4"), Key("5"), Key("6"), Key("−", "-", KeyKind.OP)),
    listOf(Key("1"), Key("2"), Key("3"), Key("+", "+", KeyKind.OP)),
    listOf(Key("0"), Key("."), Key("ANS", "ans", KeyKind.FN), Key("=", "=", KeyKind.ACCENT))
)

private fun fn(label: String, value: String = label) = Key(label, value, KeyKind.FN)

private val sciPhone = listOf(
    listOf(fn("sin", "sin("), fn("cos", "cos("), fn("tan", "tan("), fn("ln", "ln("), fn("log", "log(")),
    listOf(fn("√", "√("), fn("xʸ", "^"), fn("π"), fn("("), fn(")"))
)

private val sciTablet = listOf(
    listOf(fn("sin", "sin("), fn("cos", "cos("), fn("tan", "tan("), fn("asin", "asin("), fn("acos", "acos("), fn("atan", "atan(")),
    listOf(fn("sinh", "sinh("), fn("cosh", "cosh("), fn("tanh", "tanh("), fn("ln", "ln("), fn("log", "log("), fn("log₂", "log2(")),
    listOf(fn("√", "√("), fn("∛", "cbrt("), fn("xʸ", "^"), fn("x²", "^2"), fn("n!", "!"), fn("|x|", "abs(")),
    listOf(fn("π"), fn("e"), fn("("), fn(")"), fn("x"), fn("eˣ", "exp("))
)

@Composable
fun ScientificScreen(s: SciState, tablet: Boolean) {
    if (tablet) SciTablet(s) else SciPhone(s)
}

@Composable
private fun SciPhone(s: SciState) {
    Column(Modifier.fillMaxSize()) {
        SciDisplay(s, false, Modifier.fillMaxWidth().weight(0.8f))
        Spacer(Modifier.height(6.dp))
        if (s.showFn) {
            KeyGrid(sciPhone, Modifier.fillMaxWidth().weight(0.7f), 15.sp) { s.press(it.value) }
            Spacer(Modifier.height(4.dp))
        }
        KeyGrid(basicRows, Modifier.fillMaxWidth().weight(1.6f), 22.sp) { s.press(it.value) }
    }
}

@Composable
private fun SciTablet(s: SciState) {
    Row(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1.5f).fillMaxHeight()) {
            SciDisplay(s, true, Modifier.fillMaxWidth().weight(0.8f))
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth().weight(1.6f)) {
                KeyGrid(sciTablet, Modifier.weight(1.3f).fillMaxHeight(), 16.sp) { s.press(it.value) }
                Spacer(Modifier.width(8.dp))
                KeyGrid(basicRows, Modifier.weight(1f).fillMaxHeight(), 24.sp) { s.press(it.value) }
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f).fillMaxHeight()) {
            GraphPanel(s.expr, s.degrees, Modifier.fillMaxWidth().weight(1f))
            Spacer(Modifier.height(10.dp))
            HistoryPanel(s, Modifier.fillMaxWidth().weight(1f))
        }
    }
}

@Composable
private fun SciDisplay(s: SciState, tablet: Boolean, modifier: Modifier) {
    val c = Hud.colors
    val preview = s.preview()
    val isErr = s.result.startsWith("Error")
    val shown = when {
        s.result.isNotEmpty() -> s.result
        preview != null -> preview
        else -> "0"
    }
    val color = when {
        isErr -> c.danger
        s.result.isNotEmpty() -> c.accent
        else -> c.primary.copy(alpha = 0.75f)
    }
    val size = if (isErr) 16.sp else if (tablet) 44.sp else 36.sp
    HudPanel(modifier, "INPUT") {
        Row(Modifier.fillMaxWidth().height(30.dp), horizontalArrangement = Arrangement.End) {
            if (!tablet) {
                HudButton(
                    label = if (s.showFn) "FN▲" else "FN▼",
                    modifier = Modifier.width(64.dp).fillMaxHeight(),
                    kind = KeyKind.FN,
                    fontSize = 12.sp
                ) { s.showFn = !s.showFn }
            }
            HudButton(
                label = if (s.degrees) "DEG" else "RAD",
                modifier = Modifier.width(64.dp).fillMaxHeight(),
                kind = KeyKind.OP,
                fontSize = 12.sp
            ) { s.degrees = !s.degrees }
        }
        Spacer(Modifier.weight(1f))
        HudText(
            text = if (s.expr.isEmpty()) " " else s.expr,
            modifier = Modifier.fillMaxWidth(),
            color = c.text,
            size = if (tablet) 26.sp else 22.sp,
            align = TextAlign.End,
            maxLines = 2
        )
        HudText(
            text = shown,
            modifier = Modifier.fillMaxWidth(),
            color = color,
            size = size,
            bold = true,
            align = TextAlign.End,
            maxLines = 1
        )
    }
}

@Composable
private fun HistoryPanel(s: SciState, modifier: Modifier) {
    val c = Hud.colors
    HudPanel(modifier, "HISTORY") {
        if (s.history.isEmpty()) HudText("No calculations yet", color = c.dim, size = 12.sp)
        LazyColumn(Modifier.weight(1f)) {
            items(s.history) { h ->
                Column(
                    Modifier.fillMaxWidth().clickable { s.recall(h) }.padding(vertical = 4.dp)
                ) {
                    HudText(h.expr, color = c.primary.copy(alpha = 0.7f), size = 12.sp, maxLines = 1)
                    HudText("= ${h.result}", color = c.text, size = 17.sp, bold = true, maxLines = 1)
                }
            }
        }
    }
}

private class Plot(val ys: List<Double>, val lo: Double, val hi: Double)

private val xRegex = Regex("(?<![a-zA-Z])x(?![a-zA-Z])")

private fun buildPlot(expr: String, deg: Boolean): Plot? {
    if (!xRegex.containsMatchIn(expr)) return null
    val ys = (0..240).map { k ->
        val x = -10.0 + k * (20.0 / 240)
        runCatching { ExprEngine.evaluate(expr, deg, mapOf("x" to x, "ans" to 0.0)) }.getOrDefault(Double.NaN)
    }
    val fin = ys.filter { it.isFinite() && abs(it) < 1e6 }
    if (fin.isEmpty()) return null
    var lo = max(fin.minOrNull() ?: 0.0, -20.0)
    var hi = min(fin.maxOrNull() ?: 0.0, 20.0)
    if (lo >= hi) {
        if (fin.all { it > 20.0 || it < -20.0 }) { lo = -10.0; hi = 10.0 } else { lo -= 1; hi += 1 }
    }
    val pad = (hi - lo) * 0.05
    return Plot(ys, lo - pad, hi + pad)
}

@Composable
private fun GraphPanel(expr: String, degrees: Boolean, modifier: Modifier) {
    val c = Hud.colors
    val plot = remember(expr, degrees) { buildPlot(expr, degrees) }
    HudPanel(modifier, "GRAPH · f(x)") {
        if (plot == null) {
            HudText("Type an expression with x, e.g. sin(x)", color = c.dim, size = 12.sp)
        }
        Canvas(Modifier.weight(1f).fillMaxWidth()) {
            val w = size.width
            val h = size.height
            for (k in 0..10) {
                val gx = w * k / 10f
                drawLine(c.primary.copy(alpha = 0.08f), Offset(gx, 0f), Offset(gx, h), 1f)
            }
            for (k in 0..6) {
                val gy = h * k / 6f
                drawLine(c.primary.copy(alpha = 0.08f), Offset(0f, gy), Offset(w, gy), 1f)
            }
            if (plot == null) return@Canvas
            val span = plot.hi - plot.lo
            fun py(y: Double) = (h - ((y - plot.lo) / span * h)).toFloat()
            drawLine(c.primary.copy(alpha = 0.4f), Offset(w / 2f, 0f), Offset(w / 2f, h), 1.5f)
            if (plot.lo < 0 && plot.hi > 0) {
                drawLine(c.primary.copy(alpha = 0.4f), Offset(0f, py(0.0)), Offset(w, py(0.0)), 1.5f)
            }
            val path = Path()
            var down = false
            plot.ys.forEachIndexed { k, y ->
                val px = w * k / 240f
                if (!y.isFinite() || y < plot.lo || y > plot.hi) {
                    down = false
                } else if (!down) {
                    path.moveTo(px, py(y)); down = true
                } else {
                    path.lineTo(px, py(y))
                }
            }
            drawPath(path, c.accent, style = Stroke(width = 2.5f))
        }
    }
}
