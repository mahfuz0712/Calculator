package com.mahfuz0712.calculator.ui

import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class ProgState {
    var base by mutableIntStateOf(10)
    var bits by mutableIntStateOf(32)
    var value by mutableLongStateOf(0L)
    var acc by mutableStateOf<Long?>(null)
    var op by mutableStateOf<String?>(null)
    var showBits by mutableStateOf(false)
    private var fresh = true

    private fun mask(v: Long) = if (bits >= 64) v else v and ((1L shl bits) - 1)

    fun signed(v: Long): Long =
        if (bits < 64 && ((v shr (bits - 1)) and 1L) == 1L) v - (1L shl bits) else v

    fun fmt(v: Long, b: Int): String = when (b) {
        16 -> java.lang.Long.toHexString(v).uppercase()
        10 -> java.lang.Long.toUnsignedString(v)
        8 -> java.lang.Long.toOctalString(v)
        else -> java.lang.Long.toBinaryString(v).padStart(bits, '0').chunked(4).joinToString(" ")
    }

    private fun apply(o: String, a: Long, b: Long): Long = mask(
        when (o) {
            "+" -> a + b
            "-" -> a - b
            "×" -> a * b
            "÷" -> if (b == 0L) 0L else java.lang.Long.divideUnsigned(a, b)
            "MOD" -> if (b == 0L) 0L else java.lang.Long.remainderUnsigned(a, b)
            "AND" -> a and b
            "OR" -> a or b
            "XOR" -> a xor b
            "<<" -> a shl b.toInt()
            ">>" -> a ushr b.toInt()
            else -> b
        }
    )

    fun press(v: String) {
        if (v.length == 1 && Character.digit(v[0], 16) >= 0) {
            val d = Character.digit(v[0], 16)
            if (d < base) {
                value = mask(if (fresh) d.toLong() else value * base + d)
                fresh = false
            }
            return
        }
        when (v) {
            "AC" -> { value = 0L; acc = null; op = null; fresh = true }
            "DEL" -> if (!fresh) value = value / base
            "NOT" -> { value = mask(value.inv()); fresh = true }
            "=" -> {
                val a = acc
                val p = op
                if (a != null && p != null) value = apply(p, a, value)
                acc = null; op = null; fresh = true
            }
            else -> {
                val a = acc
                val p = op
                if (a != null && p != null && !fresh) value = apply(p, a, value)
                acc = value; op = v; fresh = true
            }
        }
    }

    fun toggleBit(i: Int) {
        value = mask(value xor (1L shl i))
        fresh = true
    }

    fun setSize(n: Int) {
        bits = n
        value = mask(value)
    }
}

private fun pk(l: String, kind: KeyKind = KeyKind.NUM) = Key(l, l, kind)

private val progRows = listOf(
    listOf(Key("AC", "AC", KeyKind.DANGER), Key("⌫", "DEL", KeyKind.OP), pk("NOT", KeyKind.OP), pk("AND", KeyKind.OP), pk("OR", KeyKind.OP), pk("XOR", KeyKind.OP)),
    listOf(pk("A"), pk("B"), pk("7"), pk("8"), pk("9"), Key("÷", "÷", KeyKind.OP)),
    listOf(pk("C"), pk("D"), pk("4"), pk("5"), pk("6"), Key("×", "×", KeyKind.OP)),
    listOf(pk("E"), pk("F"), pk("1"), pk("2"), pk("3"), Key("−", "-", KeyKind.OP)),
    listOf(pk("<<", KeyKind.OP), pk(">>", KeyKind.OP), pk("MOD", KeyKind.OP), pk("0"), Key("=", "=", KeyKind.ACCENT), Key("+", "+", KeyKind.OP))
)

@Composable
fun ProgrammerScreen(p: ProgState, tablet: Boolean) {
    val enabled: (Key) -> Boolean = { k ->
        val d = if (k.value.length == 1) Character.digit(k.value[0], 16) else -1
        if (d >= 0) d < p.base else true
    }
    if (tablet) {
        Row(Modifier.fillMaxSize()) {
            Column(Modifier.weight(1.3f).fillMaxHeight()) {
                ProgReadout(p, true, Modifier.fillMaxWidth().weight(1f))
                Spacer(Modifier.height(8.dp))
                KeyGrid(progRows, Modifier.fillMaxWidth().weight(1.4f), 22.sp, enabled) { p.press(it.value) }
            }
            Spacer(Modifier.width(10.dp))
            BitPanel(p, 16, Modifier.weight(1f).fillMaxHeight())
        }
    } else {
        Column(Modifier.fillMaxSize()) {
            ProgReadout(p, false, Modifier.fillMaxWidth().weight(1f))
            Spacer(Modifier.height(6.dp))
            if (p.showBits) {
                BitPanel(p, 8, Modifier.fillMaxWidth().weight(1.5f))
            } else {
                KeyGrid(progRows, Modifier.fillMaxWidth().weight(1.5f), 15.sp, enabled) { p.press(it.value) }
            }
        }
    }
}

@Composable
private fun ProgReadout(p: ProgState, tablet: Boolean, modifier: Modifier) {
    val c = Hud.colors
    HudPanel(modifier, "PROGRAMMER · ${p.bits}-BIT") {
        Row(Modifier.fillMaxWidth().height(30.dp)) {
            for ((b, n) in listOf(16 to "HEX", 10 to "DEC", 8 to "OCT", 2 to "BIN")) {
                HudButton(n, Modifier.weight(1f).fillMaxHeight(), KeyKind.OP, selected = p.base == b, fontSize = 12.sp) { p.base = b }
            }
        }
        Row(Modifier.fillMaxWidth().height(30.dp)) {
            for (n in listOf(8, 16, 32, 64)) {
                HudButton("$n", Modifier.weight(1f).fillMaxHeight(), KeyKind.FN, selected = p.bits == n, fontSize = 12.sp) { p.setSize(n) }
            }
            if (!tablet) {
                HudButton("BITS", Modifier.weight(1f).fillMaxHeight(), KeyKind.FN, selected = p.showBits, fontSize = 12.sp) { p.showBits = !p.showBits }
            }
        }
        Spacer(Modifier.height(6.dp))
        val a = p.acc
        val o = p.op
        if (a != null && o != null) {
            HudText("${p.fmt(a, p.base)} $o", color = c.accent, size = 12.sp)
        }
        ReadLine("HEX", 16, p, tablet)
        ReadLine("DEC", 10, p, tablet)
        ReadLine("OCT", 8, p, tablet)
        ReadLine("BIN", 2, p, tablet)
        HudText("SIGNED  ${p.signed(p.value)}", color = c.dim, size = 11.sp)
    }
}

@Composable
private fun ReadLine(label: String, base: Int, p: ProgState, tablet: Boolean) {
    val c = Hud.colors
    val sel = p.base == base
    val size = if (base == 2) (if (tablet) 13.sp else 11.sp) else (if (tablet) 20.sp else 16.sp)
    Row(Modifier.fillMaxWidth().clickable { p.base = base }.padding(vertical = 2.dp)) {
        HudText(label, Modifier.width(36.dp), c.dim, 11.sp)
        HudText(p.fmt(p.value, base), Modifier.weight(1f), if (sel) c.accent else c.text, size, bold = sel)
    }
}

@Composable
private fun BitPanel(p: ProgState, perRowWanted: Int, modifier: Modifier) {
    val perRow = minOf(perRowWanted, p.bits)
    val rows = p.bits / perRow
    HudPanel(modifier, "BIT FIELD · ${p.bits}-BIT") {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            for (r in 0 until rows) {
                Row(Modifier.fillMaxWidth().height(40.dp)) {
                    for (k in 0 until perRow) {
                        val idx = p.bits - 1 - (r * perRow + k)
                        val on = ((p.value shr idx) and 1L) == 1L
                        HudButton(
                            label = if (on) "1" else "0",
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            kind = if (on) KeyKind.ACCENT else KeyKind.NUM,
                            selected = on,
                            fontSize = 14.sp
                        ) { p.toggleBit(idx) }
                    }
                }
                HudText("${p.bits - 1 - r * perRow}", color = Hud.colors.dim, size = 9.sp)
            }
        }
    }
}
