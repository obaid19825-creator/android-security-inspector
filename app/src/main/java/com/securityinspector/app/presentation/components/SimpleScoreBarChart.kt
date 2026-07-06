package com.securityinspector.app.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * A minimal, dependency-free bar chart used to show recent scan score history.
 * Each entry is (label, value 0-100, color).
 */
@Composable
fun SimpleScoreBarChart(
    entries: List<Triple<String, Int, Color>>,
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 140.dp
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
        ) {
            if (entries.isEmpty()) return@Canvas

            val barCount = entries.size
            val gap = size.width * 0.04f
            val totalGap = gap * (barCount + 1)
            val barWidth = (size.width - totalGap) / barCount

            entries.forEachIndexed { index, (_, value, color) ->
                val barHeight = (value / 100f) * size.height
                val left = gap + index * (barWidth + gap)
                val top = size.height - barHeight

                drawRoundRect(
                    color = color,
                    topLeft = Offset(left, top),
                    size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
                )
            }

            // Baseline
            drawLine(
                color = Color.Gray.copy(alpha = 0.3f),
                start = Offset(0f, size.height),
                end = Offset(size.width, size.height),
                strokeWidth = 2f,
                cap = StrokeCap.Round
            )
        }

        androidx.compose.foundation.layout.Row(modifier = Modifier.fillMaxWidth()) {
            entries.forEach { (label, _, _) ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}
