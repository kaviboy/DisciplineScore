package com.disciplinescore.app.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.disciplinescore.app.domain.DISCIPLINE_THRESHOLD
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ScoreRing(score: Int?, modifier: Modifier = Modifier) {
    val animated by animateIntAsState(
        targetValue = score ?: 0,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessLow),
        label = "score",
    )
    val sweep by animateFloatAsState(
        targetValue = (score ?: 0) / 100f,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessLow),
        label = "sweep",
    )
    val metGoal = score != null && score >= DISCIPLINE_THRESHOLD
    val arc = if (metGoal) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
    val threshold = MaterialTheme.colorScheme.tertiary
    val description = if (score == null) "No daily score" else "Daily score $score out of 100"
    Box(
        modifier = modifier
            .size(188.dp)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 16.dp.toPx()
            val diameter = size.minDimension - stroke
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)
            drawArc(
                color = track,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            if (score != null && sweep > 0f) {
                drawArc(
                    color = arc.copy(alpha = 0.28f),
                    startAngle = -90f,
                    sweepAngle = 360f * sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke * 2.1f, cap = StrokeCap.Round),
                )
                drawArc(
                    color = arc,
                    startAngle = -90f,
                    sweepAngle = 360f * sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
            val markAngle = Math.toRadians(-90.0 + 360.0 * 0.70)
            val radius = diameter / 2f
            val center = Offset(
                x = size.width / 2f + (radius * cos(markAngle)).toFloat(),
                y = size.height / 2f + (radius * sin(markAngle)).toFloat(),
            )
            drawCircle(color = threshold, radius = 5.dp.toPx(), center = center)
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = if (score == null) "–" else animated.toString(),
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            if (score != null) {
                Text(
                    text = "/100",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 2.dp, bottom = 10.dp),
                )
            }
        }
    }
}
