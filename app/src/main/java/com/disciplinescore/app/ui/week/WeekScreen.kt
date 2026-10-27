package com.disciplinescore.app.ui.week

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.disciplinescore.app.ui.HabitRateUi
import com.disciplinescore.app.ui.WeekUi
import com.disciplinescore.app.ui.components.ScreenHeader
import com.disciplinescore.app.ui.components.SoftCard
import com.disciplinescore.app.ui.components.WeekChart

@Composable
fun WeekScreen(
    state: WeekUi,
    onOpenSettings: () -> Unit,
    onOpenProfile: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onCurrent: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 8.dp, bottom = 28.dp),
    ) {
        ScreenHeader(
            title = state.title,
            subtitle = "Use the arrows to look at an earlier stretch.",
            onOpenSettings = onOpenSettings,
            onOpenProfile = onOpenProfile,
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onPrevious, enabled = state.canGoBack) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous week")
            }
            Text(
                text = state.rangeLabel,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onNext, enabled = state.canGoForward) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next week")
            }
        }
        if (!state.isCurrent) {
            TextButton(onClick = onCurrent, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Back to this week")
            }
        }
        SoftCard(Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Column(Modifier.padding(18.dp)) {
                if (state.averageScore != null) {
                    Text(
                        text = state.averageScore.toString(),
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "Average score",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = state.summary,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 12.dp),
                )
                state.completionLine?.let { line ->
                    Text(
                        text = line,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }

        if (!state.empty) {
            SoftCard(Modifier.fillMaxWidth().padding(top = 12.dp)) {
                Column(Modifier.padding(16.dp)) {
                    WeekChart(days = state.days)
                    Text(
                        text = "The line is 70. Bars below it still count, they just did not hold the day. A dash means nothing was scheduled.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(22.dp))
        Text(text = "Hardest habits", style = MaterialTheme.typography.titleLarge)
        if (state.habits.isEmpty()) {
            Text(
                text = "Habit completion shows up here once a day in this stretch has something scheduled.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        } else {
            state.habits.forEach { habit ->
                HabitRateRow(habit, Modifier.padding(top = 14.dp))
            }
        }
    }
}

@Composable
private fun HabitRateRow(habit: HabitRateUi, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        Text(text = habit.name, style = MaterialTheme.typography.titleMedium)
        Text(
            text = "${habit.detail} · ${habit.percent}%",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp, bottom = 8.dp),
        )
        LinearProgressIndicator(
            progress = { habit.percent / 100f },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(8.dp)),
            color = if (habit.weakest) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceContainer,
        )
        if (habit.weakest) {
            Text(
                text = "Lowest completion in this stretch.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}
