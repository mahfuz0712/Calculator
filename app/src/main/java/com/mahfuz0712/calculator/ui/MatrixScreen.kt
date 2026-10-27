package com.mahfuz0712.calculator.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mahfuz0712.calculator.engine.MatrixEngine

class MatState {
    var a by mutableStateOf("1 2\n3 4")
    var b by mutableStateOf("0 1\n1 0")
    var op by mutableStateOf("")
    var result by mutableStateOf("")
    var error by mutableStateOf(false)

    fun run(o: String) {
        op = o
        try {
            result = MatrixEngine.run(o, a, b)
            error = false
        } catch (e: Exception) {
            result = e.message ?: "Error"
            error = true
        }
    }
}

private val matOps = listOf("A+B", "A−B", "A×B", "det A", "A⁻¹", "Aᵀ", "det B", "B⁻¹", "Bᵀ")

@Composable
fun MatrixScreen(s: MatState, tablet: Boolean) {
    if (tablet) {
        Row(Modifier.fillMaxSize()) {
            Column(Modifier.weight(1f).fillMaxHeight()) {
                MatField("MATRIX A", s.a, { s.a = it }, Modifier.fillMaxWidth().weight(1f))
                Spacer(Modifier.height(10.dp))
                MatField("MATRIX B", s.b, { s.b = it }, Modifier.fillMaxWidth().weight(1f))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f).fillMaxHeight()) {
                OpsGrid(s, matOps, Modifier.fillMaxWidth().height(170.dp))
                Spacer(Modifier.height(10.dp))
                ResultPanel(s, Modifier.fillMaxWidth().weight(1f))
            }
        }
    } else {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            MatField("MATRIX A", s.a, { s.a = it }, Modifier.fillMaxWidth())
            Spacer(Modifier.height(6.dp))
            MatField("MATRIX B", s.b, { s.b = it }, Modifier.fillMaxWidth())
            Spacer(Modifier.height(6.dp))
            OpsGrid(s, matOps.take(6), Modifier.fillMaxWidth().height(110.dp))
            Spacer(Modifier.height(6.dp))
            ResultPanel(s, Modifier.fillMaxWidth().heightIn(min = 130.dp))
        }
    }
}

@Composable
private fun MatField(title: String, value: String, onChange: (String) -> Unit, modifier: Modifier) {
    val c = Hud.colors
    HudPanel(modifier, "$title  (rows: new line or ;)") {
        BasicTextField(
            value = value,
            onValueChange = onChange,
            textStyle = TextStyle(color = c.text, fontFamily = FontFamily.Monospace, fontSize = 18.sp),
            cursorBrush = SolidColor(c.primary),
            modifier = Modifier.fillMaxWidth().heightIn(min = 70.dp)
        )
    }
}

@Composable
private fun OpsGrid(s: MatState, ops: List<String>, modifier: Modifier) {
    Column(modifier) {
        for (row in ops.chunked(3)) {
            Row(Modifier.weight(1f).fillMaxWidth()) {
                for (o in row) {
                    HudButton(o, Modifier.weight(1f).fillMaxHeight(), KeyKind.OP, fontSize = 15.sp) { s.run(o) }
                }
            }
        }
    }
}

@Composable
private fun ResultPanel(s: MatState, modifier: Modifier) {
    val c = Hud.colors
    HudPanel(modifier, "RESULT  ${s.op}") {
        Box(Modifier.horizontalScroll(rememberScrollState())) {
            HudText(
                text = if (s.result.isEmpty()) "Pick an operation" else s.result,
                color = if (s.error) c.danger else if (s.result.isEmpty()) c.dim else c.accent,
                size = 18.sp,
                bold = !s.error
            )
        }
    }
}
