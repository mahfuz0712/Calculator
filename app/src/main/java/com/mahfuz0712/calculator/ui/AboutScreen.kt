package com.mahfuz0712.calculator.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mahfuz0712.calculator.AppInfo
import kotlinx.coroutines.launch

@Composable
fun AboutScreen(tablet: Boolean, onUpdateFound: (UpdateInfo) -> Unit) {
    val uri = LocalUriHandler.current
    val open: (String) -> Unit = { link -> runCatching { uri.openUri(link) } }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        if (tablet) {
            Row(Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    AppPanel(Modifier.fillMaxWidth())
                    Spacer(Modifier.height(10.dp))
                    UpdatePanel(onUpdateFound, Modifier.fillMaxWidth())
                    Spacer(Modifier.height(10.dp))
                    MoreAppsPanel(open, Modifier.fillMaxWidth())
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) { AuthorPanel(open, Modifier.fillMaxWidth()) }
            }
        } else {
            AppPanel(Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            UpdatePanel(onUpdateFound, Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            AuthorPanel(open, Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            MoreAppsPanel(open, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun AppPanel(modifier: Modifier) {
    val c = Hud.colors
    val version = appVersion(LocalContext.current)
    HudPanel(modifier, "SYSTEM") {
        HudText("◈ ${AppInfo.NAME.uppercase()}", color = c.primary, size = 28.sp, bold = true)
        HudText("VERSION $version", color = c.accent, size = 12.sp)
        Spacer(Modifier.height(8.dp))
        HudText(
            "Scientific calculation, boolean algebra, programmer tools and matrix operations in a sci-fi HUD interface. Adaptive layout for phones and tablets.",
            color = c.text,
            size = 13.sp
        )
    }
}

@Composable
private fun UpdatePanel(onUpdateFound: (UpdateInfo) -> Unit, modifier: Modifier) {
    val c = Hud.colors
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf("") }
    HudPanel(modifier, "UPDATES") {
        HudButton(
            label = "CHECK FOR UPDATES",
            modifier = Modifier.fillMaxWidth().height(46.dp),
            kind = KeyKind.OP,
            fontSize = 13.sp
        ) {
            scope.launch {
                status = "CHECKING..."
                if (!isOnline(ctx)) {
                    status = "NO INTERNET CONNECTION"
                    return@launch
                }
                val (ok, info) = checkUpdate(ctx)
                status = when {
                    !ok -> "CHECK FAILED"
                    info == null -> "UP TO DATE"
                    else -> "NEW VERSION AVAILABLE: v${info.version}"
                }
                if (info != null) onUpdateFound(info)
            }
        }
        if (status.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            HudText(status, color = c.dim, size = 12.sp)
        }
    }
}

@Composable
private fun AuthorPanel(open: (String) -> Unit, modifier: Modifier) {
    HudPanel(modifier, "AUTHOR") {
        LinkRow("NAME", AppInfo.AUTHOR, null, open)
        LinkRow("EMAIL", AppInfo.EMAIL, "mailto:${AppInfo.EMAIL}", open)
        LinkRow("GITHUB", "github.com/mahfuz0712", AppInfo.GITHUB, open)
        LinkRow("PORTFOLIO", "portfolio-mahfuz0712.vercel.app", AppInfo.PORTFOLIO, open)
    }
}

@Composable
private fun MoreAppsPanel(open: (String) -> Unit, modifier: Modifier) {
    val c = Hud.colors
    HudPanel(modifier, "MORE APPS") {
        HudText("Discover more apps by the developer on zApps Store.", color = c.text, size = 13.sp)
        Spacer(Modifier.height(8.dp))
        HudButton(
            label = "OPEN zApps STORE ▸",
            modifier = Modifier.fillMaxWidth().height(48.dp),
            kind = KeyKind.ACCENT,
            fontSize = 14.sp
        ) { open(AppInfo.MORE_APPS) }
    }
}

@Composable
private fun LinkRow(label: String, value: String, link: String?, open: (String) -> Unit) {
    val c = Hud.colors
    Column(
        Modifier
            .fillMaxWidth()
            .then(if (link != null) Modifier.clickable { open(link) } else Modifier)
            .padding(vertical = 6.dp)
    ) {
        HudText(label, color = c.dim, size = 10.sp)
        HudText(value, color = if (link != null) c.primary else c.text, size = 16.sp)
    }
}
