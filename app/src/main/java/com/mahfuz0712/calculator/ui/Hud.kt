package com.mahfuz0712.calculator.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Rectangle with two cut corners (top-left and bottom-right) - the HUD frame look. */
class HudShape(private val cut: Dp) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val c = with(density) { cut.toPx() }.coerceAtMost(minOf(size.width, size.height) / 2f)
        val p = Path().apply {
            moveTo(c, 0f)
            lineTo(size.width, 0f)
            lineTo(size.width, size.height - c)
            lineTo(size.width - c, size.height)
            lineTo(0f, size.height)
            lineTo(0f, c)
            close()
        }
        return Outline.Generic(p)
    }
}

enum class KeyKind { NUM, OP, FN, ACCENT, DANGER }

data class Key(val label: String, val value: String = label, val kind: KeyKind = KeyKind.NUM)

@Composable
fun HudText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Hud.colors.text,
    size: TextUnit = 14.sp,
    bold: Boolean = false,
    align: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = size,
        fontFamily = FontFamily.Monospace,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
        textAlign = align,
        maxLines = maxLines
    )
}

@Composable
fun HudPanel(
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val c = Hud.colors
    val shape = HudShape(12.dp)
    Column(
        modifier
            .background(c.panel.copy(alpha = 0.82f), shape)
            .drawBehind {
                val t = 10.dp.toPx()
                val w = 2.dp.toPx()
                drawLine(c.primary, Offset(size.width - t, 0f), Offset(size.width, 0f), w)
                drawLine(c.primary, Offset(size.width, 0f), Offset(size.width, t), w)
                drawLine(c.primary, Offset(0f, size.height - t), Offset(0f, size.height), w)
                drawLine(c.primary, Offset(0f, size.height), Offset(t, size.height), w)
            }
            .border(1.dp, c.primary.copy(alpha = 0.5f), shape)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        if (title != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 4.dp)
            ) {
                Box(Modifier.size(width = 4.dp, height = 10.dp).background(c.accent))
                Spacer(Modifier.width(6.dp))
                HudText(title, color = c.primary, size = 10.sp, maxLines = 1)
            }
        }
        content()
    }
}

@Composable
fun HudButton(
    label: String,
    modifier: Modifier = Modifier,
    kind: KeyKind = KeyKind.NUM,
    enabled: Boolean = true,
    selected: Boolean = false,
    fontSize: TextUnit = 18.sp,
    onClick: () -> Unit
) {
    val c = Hud.colors
    val shape = HudShape(6.dp)
    val base = when (kind) {
        KeyKind.NUM -> c.text
        KeyKind.OP -> c.primary
        KeyKind.FN -> c.primary.copy(alpha = 0.75f)
        KeyKind.ACCENT -> c.accent
        KeyKind.DANGER -> c.danger
    }
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val fill = if (pressed || selected) base.copy(alpha = 0.28f) else base.copy(alpha = 0.06f)
    Box(
        modifier
            .padding(2.dp)
            .background(fill, shape)
            .border(1.dp, base.copy(alpha = if (enabled) 0.6f else 0.2f), shape)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        HudText(
            text = label,
            color = if (enabled) base else base.copy(alpha = 0.3f),
            size = fontSize,
            bold = true,
            maxLines = 1
        )
    }
}

@Composable
fun KeyGrid(
    rows: List<List<Key>>,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 18.sp,
    isEnabled: (Key) -> Boolean = { true },
    onKey: (Key) -> Unit
) {
    Column(modifier) {
        for (row in rows) {
            Row(Modifier.weight(1f).fillMaxWidth()) {
                for (k in row) {
                    HudButton(
                        label = k.label,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        kind = k.kind,
                        enabled = isEnabled(k),
                        fontSize = fontSize
                    ) { onKey(k) }
                }
            }
        }
    }
}

@Composable
fun HudBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val c = Hud.colors
    val transition = rememberInfiniteTransition(label = "hud-bg")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(40000, easing = LinearEasing)),
        label = "angle"
    )
    Box(
        modifier
            .background(c.bg)
            .drawBehind {
                val step = 28.dp.toPx()
                var x = 0f
                while (x < size.width) {
                    drawLine(c.primary.copy(alpha = 0.05f), Offset(x, 0f), Offset(x, size.height), 1f)
                    x += step
                }
                var y = 0f
                while (y < size.height) {
                    drawLine(c.primary.copy(alpha = 0.05f), Offset(0f, y), Offset(size.width, y), 1f)
                    y += step
                }
                val center = Offset(size.width * 0.82f, size.height * 0.18f)
                val r = minOf(size.width, size.height) * 0.35f
                drawCircle(c.primary.copy(alpha = 0.07f), r * 0.8f, center, style = Stroke(1.dp.toPx()))
                rotate(degrees = angle, pivot = center) {
                    drawArc(
                        color = c.primary.copy(alpha = 0.14f),
                        startAngle = 0f,
                        sweepAngle = 80f,
                        useCenter = false,
                        topLeft = Offset(center.x - r, center.y - r),
                        size = Size(2 * r, 2 * r),
                        style = Stroke(3.dp.toPx())
                    )
                    drawArc(
                        color = c.accent.copy(alpha = 0.12f),
                        startAngle = 140f,
                        sweepAngle = 50f,
                        useCenter = false,
                        topLeft = Offset(center.x - r, center.y - r),
                        size = Size(2 * r, 2 * r),
                        style = Stroke(3.dp.toPx())
                    )
                }
            },
        content = content
    )
}
