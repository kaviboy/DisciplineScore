package com.disciplinescore.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.disciplinescore.app.ui.SkippedHabitUi
import com.disciplinescore.app.ui.icon
import com.disciplinescore.app.ui.tint

@Composable
fun SkippedHabitsCard(
    skipped: List<SkippedHabitUi>,
    onRestore: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (skipped.isEmpty()) return
    SoftCard(modifier.fillMaxWidth()) {
        Column(Modifier.padding(top = 14.dp)) {
            Text(
                text = "Set aside",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Text(
                text = "These do not count against the day. Put one back if you want it on the score.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
            skipped.forEachIndexed { index, habit ->
                if (index == 0) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                SkippedRow(habit, onRestore = { onRestore(habit.id) })
                if (index != skipped.lastIndex) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f))
                }
            }
        }
    }
}

@Composable
private fun SkippedRow(habit: SkippedHabitUi, onRestore: () -> Unit) {
    val tint = habit.category.tint()
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = habit.category.icon(),
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(18.dp),
            )
        }
        Text(
            text = habit.name,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
        )
        TextButton(onClick = onRestore) {
            Text("Put back")
        }
    }
}
