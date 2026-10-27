package com.mahfuz0712.calculator.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mahfuz0712.calculator.engine.BoolEngine
import com.mahfuz0712.calculator.engine.BoolResult
import com.mahfuz0712.calculator.engine.KMap

class BoolState {
    var expr by mutableStateOf("A'B + AC")

    fun press(v: String) {
        expr = when (v) {
            "AC" -> ""
            "DEL" -> expr.dropLast(1)
            else -> expr + v
        }
    }
}

private val boolRows = listOf(
    listOf(Key("A"), Key("B"), Key("C"), Key("D"), Key("E"), Key("F")),
    listOf(Key("0"), Key("1"), Key("("), Key(")"), Key("'", "'", KeyKind.OP), Key("!", "!", KeyKind.OP)),
    listOf(
        Key("+", "+", KeyKind.OP), Key("·", "·", KeyKind.OP), Key("⊕", "⊕", KeyKind.OP),
        Key("AC", "AC", KeyKind.DANGER), Key("⌫", "DEL", KeyKind.OP)
    )
)

@Composable
fun BooleanScreen(s: BoolState, tablet: Boolean) {
    val res = remember(s.expr) { runCatching { BoolEngine.analyze(s.expr) } }
    if (tablet) BoolTablet(s, res) else BoolPhone(s, res)
}

@Composable
private fun BoolPhone(s: BoolState, res: Result<BoolResult>) {
    val r = res.getOrNull()
    Column(Modifier.fillMaxSize()) {
        BoolInput(s, res, Modifier.fillMaxWidth(), false)
        Spacer(Modifier.height(6.dp))
        KeyGrid(boolRows, Modifier.fillMaxWidth().height(170.dp), 18.sp) { s.press(it.value) }
        Spacer(Modifier.height(6.dp))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            if (r != null) {
                SimplifiedPanel(r, Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                val km = r.kmap
                if (km != null) {
                    KMapPanel(km, Modifier.fillMaxWidth())
                    Spacer(Modifier.height(6.dp))
                }
                TruthPanel(r, Modifier.fillMaxWidth(), false)
            }
        }
    }
}

@Composable
private fun BoolTablet(s: BoolState, res: Result<BoolResult>) {
    val r = res.getOrNull()
    Row(Modifier.fillMaxSize()) {
        Column(Modifier.weight(0.9f).fillMaxHeight()) {
            BoolInput(s, res, Modifier.fillMaxWidth().weight(0.45f), true)
            Spacer(Modifier.height(8.dp))
            KeyGrid(boolRows, Modifier.fillMaxWidth().weight(1f), 24.sp) { s.press(it.value) }
        }
        Spacer(Modifier.width(10.dp))
        Row(Modifier.weight(1.3f).fillMaxHeight()) {
            if (r != null) {
                TruthPanel(r, Modifier.weight(1f).fillMaxHeight(), true)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f).fillMaxHeight()) {
                    SimplifiedPanel(r, Modifier.fillMaxWidth())
                    Spacer(Modifier.height(10.dp))
                    val km = r.kmap
                    if (km != null) KMapPanel(km, Modifier.fillMaxWidth())
                }
            } else {
                HudPanel(Modifier.fillMaxSize(), "RESULT") {
                    HudText("Waiting for a valid expression", color = Hud.colors.dim, size = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun BoolInput(s: BoolState, res: Result<BoolResult>, modifier: Modifier, tablet: Boolean) {
    val c = Hud.colors
    val bad = res.isFailure
    val status = res.fold(
        { r -> "VARIABLES: ${r.vars.joinToString(" ")}   ROWS: ${r.table.size}" },
        { e -> e.message ?: "Invalid expression" }
    )
    HudPanel(modifier, "BOOLEAN EXPRESSION") {
        HudText(s.expr + "▌", color = c.text, size = 24.sp, bold = true)
        if (tablet) Spacer(Modifier.weight(1f)) else Spacer(Modifier.height(8.dp))
        HudText(status, color = if (bad) c.danger else c.dim, size = 11.sp)
    }
}

@Composable
private fun SimplifiedPanel(r: BoolResult, modifier: Modifier) {
    val c = Hud.colors
    HudPanel(modifier, "SIMPLIFIED") {
        HudText("SOP", color = c.dim, size = 10.sp)
        HudText("F = ${r.sop}", color = c.accent, size = 18.sp, bold = true)
        Spacer(Modifier.height(6.dp))
        HudText("POS", color = c.dim, size = 10.sp)
        HudText("F = ${r.pos}", color = c.primary, size = 16.sp, bold = true)
        Spacer(Modifier.height(6.dp))
        HudText("Σm(${r.minterms.joinToString(",")})", color = c.text.copy(alpha = 0.7f), size = 12.sp)
    }
}

@Composable
private fun TruthPanel(r: BoolResult, modifier: Modifier, scroll: Boolean) {
    val c = Hud.colors
    val n = r.vars.size
    HudPanel(modifier, "TRUTH TABLE") {
        Column(if (scroll) Modifier.weight(1f).verticalScroll(rememberScrollState()) else Modifier) {
            HudText(
                text = r.vars.joinToString(" ") + (if (n > 0) " │ " else "") + "F",
                color = c.primary,
                size = 14.sp,
                bold = true
            )
            for ((idx, out) in r.table) {
                val bits = (0 until n).joinToString(" ") { k -> ((idx shr (n - 1 - k)) and 1).toString() }
                HudText(
                    text = bits + (if (n > 0) " │ " else "") + (if (out) "1" else "0"),
                    color = if (out) c.accent else c.text.copy(alpha = 0.55f),
                    size = 14.sp
                )
            }
        }
    }
}

@Composable
private fun KMapPanel(km: KMap, modifier: Modifier) {
    val c = Hud.colors
    HudPanel(modifier, "K-MAP  ${km.corner}") {
        Row(Modifier.fillMaxWidth()) {
            Spacer(Modifier.weight(1f))
            for (cl in km.colLabels) {
                HudText(cl, Modifier.weight(1f), c.primary, 12.sp, align = TextAlign.Center)
            }
        }
        km.cells.forEachIndexed { ri, row ->
            Row(Modifier.fillMaxWidth().height(38.dp)) {
                Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    HudText(km.rowLabels[ri], color = c.primary, size = 12.sp)
                }
                for (v in row) {
                    Box(
                        Modifier.weight(1f).fillMaxHeight().padding(1.dp)
                            .background(if (v) c.primary.copy(alpha = 0.25f) else Color.Transparent)
                            .border(1.dp, c.primary.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        HudText(if (v) "1" else "0", color = if (v) c.accent else c.text.copy(alpha = 0.5f), size = 16.sp, bold = true)
                    }
                }
            }
        }
    }
}
