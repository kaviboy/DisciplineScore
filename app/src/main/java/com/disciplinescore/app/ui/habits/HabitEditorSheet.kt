package com.disciplinescore.app.ui.habits

import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.disciplinescore.app.domain.HabitCategory
import com.disciplinescore.app.domain.Importance
import com.disciplinescore.app.ui.components.ChoicePills
import com.disciplinescore.app.ui.components.ChoiceTrack
import com.disciplinescore.app.ui.components.PillButton
import com.disciplinescore.app.ui.components.PillSwitch
import com.disciplinescore.app.ui.formatMinutes
import com.disciplinescore.app.ui.shortLabel
import java.time.DayOfWeek

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitEditorSheet(
    draft: HabitDraft,
    notificationsEnabled: Boolean,
    onChange: (HabitDraft) -> Unit,
    onSave: () -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
    onRequestNotifications: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var nameError by remember(draft.id) { mutableStateOf<String?>(null) }
    var dayError by remember(draft.id) { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    var pickingTime by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
        ) {
            Text(
                text = if (draft.id == 0L) "New habit" else "Edit habit",
                style = MaterialTheme.typography.headlineMedium,
            )
            OutlinedTextField(
                value = draft.name,
                onValueChange = {
                    nameError = null
                    onChange(draft.copy(name = it.take(60)))
                },
                label = { Text("Name") },
                isError = nameError != null,
                supportingText = { nameError?.let { Text(it) } },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            )
            OutlinedTextField(
                value = draft.note,
                onValueChange = { onChange(draft.copy(note = it.take(120))) },
                label = { Text("Why it matters") },
                placeholder = { Text("Optional. Shown under the habit.") },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )

            Text(
                text = "Category",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
            )
            val categories = HabitCategory.values()
            ChoiceTrack(
                labels = categories.map { it.label },
                selectedIndex = categories.indexOf(draft.category),
                onSelect = { onChange(draft.copy(category = categories[it])) },
            )

            Text(
                text = "Importance",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
            )
            Text(
                text = "Higher importance moves the daily score more.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val ranks = Importance.values()
            ChoiceTrack(
                labels = ranks.map { it.label },
                selectedIndex = ranks.indexOf(draft.importance),
                onSelect = { onChange(draft.copy(importance = ranks[it])) },
                modifier = Modifier.padding(top = 8.dp),
            )

            Text(
                text = "Days",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
            )
            ChoiceTrack(
                labels = dayPresets.map { it.label },
                selectedIndex = dayPresets.indexOfFirst { it.days == draft.days },
                onSelect = { index ->
                    dayError = null
                    onChange(draft.copy(days = dayPresets[index].days))
                },
            )
            Text(
                text = "Or choose specific days",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 10.dp, bottom = 8.dp),
            )
            ChoicePills(
                labels = orderedDays.map { it.shortLabel() },
                isSelected = { orderedDays[it] in draft.days },
                exclusive = false,
                onSelect = { index ->
                    dayError = null
                    val day = orderedDays[index]
                    val next = if (day in draft.days) draft.days - day else draft.days + day
                    onChange(draft.copy(days = next))
                },
            )
            dayError?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(text = "Reminder", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "Sent on the days this habit is scheduled, if it is still open.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                PillSwitch(
                    checked = draft.reminderEnabled,
                    onCheckedChange = { enabled ->
                        onChange(draft.copy(reminderEnabled = enabled))
                        if (enabled && Build.VERSION.SDK_INT >= 33) onRequestNotifications()
                    },
                )
            }
            if (draft.reminderEnabled) {
                PillButton(
                    label = formatMinutes(draft.reminderMinutes),
                    onClick = { pickingTime = true },
                    modifier = Modifier.padding(top = 8.dp),
                )
                if (!notificationsEnabled) {
                    TextButton(onClick = onOpenSettings) {
                        Text("Turn on notifications")
                    }
                }
            }

            Button(
                onClick = {
                    val name = draft.name.trim()
                    nameError = if (name.isEmpty()) "Name the habit." else null
                    dayError = if (draft.days.isEmpty()) "Pick at least one day." else null
                    if (name.isNotEmpty() && draft.days.isNotEmpty()) onSave()
                },
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
            ) {
                Text("Save habit")
            }
            if (onDelete != null) {
                TextButton(
                    onClick = { confirmDelete = true },
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 4.dp),
                ) {
                    Text("Delete habit", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    if (pickingTime) {
        val timeState = rememberTimePickerState(
            initialHour = draft.reminderMinutes / 60,
            initialMinute = draft.reminderMinutes % 60,
            is24Hour = false,
        )
        AlertDialog(
            onDismissRequest = { pickingTime = false },
            confirmButton = {
                TextButton(onClick = {
                    onChange(draft.copy(reminderMinutes = timeState.hour * 60 + timeState.minute))
                    pickingTime = false
                }) { Text("Set") }
            },
            dismissButton = {
                TextButton(onClick = { pickingTime = false }) { Text("Cancel") }
            },
            text = { TimePicker(state = timeState) },
        )
    }

    if (confirmDelete && onDelete != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete habit?") },
            text = { Text("This removes the habit and its history from your score.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete()
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            },
        )
    }
}

private data class DayPreset(val label: String, val days: Set<DayOfWeek>)

private val dayPresets = listOf(
    DayPreset("Every day", DayOfWeek.values().toSet()),
    DayPreset(
        "Weekdays",
        setOf(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY,
        ),
    ),
    DayPreset("Weekends", setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)),
)

private val orderedDays = listOf(
    DayOfWeek.MONDAY,
    DayOfWeek.TUESDAY,
    DayOfWeek.WEDNESDAY,
    DayOfWeek.THURSDAY,
    DayOfWeek.FRIDAY,
    DayOfWeek.SATURDAY,
    DayOfWeek.SUNDAY,
)
