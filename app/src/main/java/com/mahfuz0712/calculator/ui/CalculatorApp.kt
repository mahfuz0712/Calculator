package com.mahfuz0712.calculator.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class Tab(val label: String, val title: String) {
    SCI("SCI", "SCIENTIFIC"),
    BOOL("BOOL", "BOOLEAN ALGEBRA"),
    PROG("PROG", "PROGRAMMER"),
    MATRIX("MAT", "MATRIX"),
    ABOUT("INFO", "ABOUT")
}

@Composable
fun CalculatorApp(amber: Boolean, onToggleTheme: () -> Unit) {
    CalculatorTheme(if (amber) AmberPalette else CyanPalette) {
        val sci = remember { SciState() }
        val prog = remember { ProgState() }
        val bool = remember { BoolState() }
        val mat = remember { MatState() }
        var tab by remember { mutableIntStateOf(0) }
        val ctx = LocalContext.current
        var update by remember { mutableStateOf<UpdateInfo?>(null) }
        LaunchedEffect(Unit) { if (isOnline(ctx)) update = checkUpdate(ctx).second }
        val tablet = LocalConfiguration.current.screenWidthDp >= 600
        val current = Tab.values()[tab]

        val screen: @Composable () -> Unit = {
            when (current) {
                Tab.SCI -> ScientificScreen(sci, tablet)
                Tab.BOOL -> BooleanScreen(bool, tablet)
                Tab.PROG -> ProgrammerScreen(prog, tablet)
                Tab.MATRIX -> MatrixScreen(mat, tablet)
                Tab.ABOUT -> AboutScreen(tablet) { update = it }
            }
        }

        update?.let { u ->
            UpdateDialog(
                u, appVersion(ctx),
                onUpdate = { openUrl(ctx, u.apkUrl); update = null },
                onCancel = { update = null }
            )
        }

        HudBackground(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize().safeDrawingPadding().padding(8.dp)) {
                if (tablet) {
                    Row(Modifier.fillMaxSize()) {
                        Column(Modifier.width(92.dp).fillMaxHeight()) {
                            for (t in Tab.values()) {
                                HudButton(
                                    label = t.label,
                                    modifier = Modifier.fillMaxWidth().height(60.dp),
                                    kind = KeyKind.OP,
                                    selected = t == current,
                                    fontSize = 14.sp
                                ) { tab = t.ordinal }
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f).fillMaxHeight()) {
                            Header(current.title, amber, onToggleTheme, Modifier.fillMaxWidth().height(36.dp))
                            Spacer(Modifier.height(8.dp))
                            Box(Modifier.weight(1f).fillMaxWidth()) { screen() }
                        }
                    }
                } else {
                    Column(Modifier.fillMaxSize()) {
                        Header(current.title, amber, onToggleTheme, Modifier.fillMaxWidth().height(34.dp))
                        Spacer(Modifier.height(6.dp))
                        Box(Modifier.weight(1f).fillMaxWidth()) { screen() }
                        Spacer(Modifier.height(6.dp))
                        Row(Modifier.fillMaxWidth().height(46.dp)) {
                            for (t in Tab.values()) {
                                HudButton(
                                    label = t.label,
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                    kind = KeyKind.OP,
                                    selected = t == current,
                                    fontSize = 12.sp
                                ) { tab = t.ordinal }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(title: String, amber: Boolean, onToggle: () -> Unit, modifier: Modifier) {
    val c = Hud.colors
    Row(
        modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        HudText("◈ CALCULATOR", color = c.primary, size = 16.sp, bold = true)
        Row(verticalAlignment = Alignment.CenterVertically) {
            HudText(title, color = c.dim, size = 11.sp)
            Spacer(Modifier.width(10.dp))
            HudButton(
                label = if (amber) "AMBER" else "CYAN",
                modifier = Modifier.width(72.dp).fillMaxHeight(),
                kind = KeyKind.FN,
                fontSize = 11.sp
            ) { onToggle() }
        }
    }
}
