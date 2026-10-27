package com.disciplinescore.app.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.disciplinescore.app.ui.WeekDayUi

private val TrackHeight = 112.dp

@Composable
fun WeekChart(days: List<WeekDayUi>, modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    val muted = MaterialTheme.colorScheme.outline
    val miss = MaterialTheme.colorScheme.tertiary
    Column(modifier) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            days.forEach { day ->
                Text(
                    text = if (day.scheduled) day.score.toString() else "–",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Box(
            Modifier
                .padding(top = 6.dp)
                .fillMaxWidth()
                .height(TrackHeight),
        ) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = TrackHeight * 0.70f)
                    .fillMaxWidth()
                    .height(1.dp)
                    .zIndex(1f)
                    .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.85f)),
            )
            Row(
                Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                days.forEach { day ->
                    val fraction = ((day.score ?: 0) / 100f).coerceIn(0f, 1f)
                    val targetHeight = when {
                        !day.scheduled -> 4.dp
                        day.score == 0 -> 4.dp
                        else -> TrackHeight * fraction
                    }
                    val barHeight by animateDpAsState(
                        targetValue = targetHeight,
                        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow),
                        label = "week-bar",
                    )
                    val color = when {
                        !day.scheduled -> muted
                        (day.score ?: 0) >= 70 -> if (day.today) primary else primary.copy(alpha = 0.72f)
                        else -> miss
                    }
                    val description = if (day.scheduled) "${day.label}, score ${day.score}" else "${day.label}, nothing scheduled"
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .semantics { contentDescription = description },
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(barHeight)
                                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                .background(color),
                        )
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            days.forEach { day ->
                Text(
                    text = day.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (day.today) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
