package com.disciplinescore.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun CutlineMark(modifier: Modifier = Modifier, size: Dp = 22.dp) {
    val ring = MaterialTheme.colorScheme.primary
    val line = MaterialTheme.colorScheme.onBackground
    Canvas(modifier.size(size)) {
        val stroke = this.size.minDimension * 0.14f
        val diameter = this.size.minDimension - stroke
        val topLeft = Offset((this.size.width - diameter) / 2f, (this.size.height - diameter) / 2f)
        drawArc(
            color = ring,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = Size(diameter, diameter),
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
        val lineWidth = stroke * 0.85f
        val y = this.size.height * 0.42f
        drawLine(
            color = line,
            start = Offset(this.size.width * 0.12f, y),
            end = Offset(this.size.width * 0.88f, y),
            strokeWidth = lineWidth,
            cap = StrokeCap.Round,
        )
    }
}
