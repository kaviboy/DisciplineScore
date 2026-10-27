package com.disciplinescore.app.ui.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.disciplinescore.app.data.UserPreferences
import com.disciplinescore.app.domain.ageYears
import com.disciplinescore.app.domain.bodySummary
import com.disciplinescore.app.ui.components.ChoiceTrack
import com.disciplinescore.app.ui.components.PillButton
import com.disciplinescore.app.ui.components.PillSwitch
import com.disciplinescore.app.ui.components.SoftCard
import com.disciplinescore.app.ui.formatBirthDate
import com.disciplinescore.app.ui.formatMinutes
import com.disciplinescore.app.ui.theme.AccentTheme
import com.disciplinescore.app.ui.theme.ThemeMode
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    preferences: UserPreferences,
    onTheme: (ThemeMode) -> Unit,
    onAccent: (AccentTheme) -> Unit,
    onOpenProfile: () -> Unit,
    onHaptics: (Boolean) -> Unit,
    onSinkCompleted: (Boolean) -> Unit,
    onDefaultReminder: (Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var pickingTime by remember { mutableStateOf(false) }
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 8.dp, bottom = 32.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = "Customize",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
        Text(
            text = "Appearance and how the day feels. Your name and body details are on Profile.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp, bottom = 18.dp),
        )

        SectionLabel("Appearance")
        SoftCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text(text = "Theme", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "Dark keeps the same layout with a quieter palette.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
                )
                val modes = ThemeMode.values()
                ChoiceTrack(
                    labels = modes.map { it.label },
                    selectedIndex = modes.indexOf(preferences.theme),
                    onSelect = { onTheme(modes[it]) },
                )
            }
        }

        SectionLabel("Color", Modifier.padding(top = 22.dp))
        SoftCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text(text = preferences.accent.label, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "Used for the score, buttons, and navigation.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 14.dp),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AccentTheme.values().forEach { accent ->
                        AccentSwatch(
                            accent = accent,
                            selected = preferences.accent == accent,
                            onClick = { onAccent(accent) },
                        )
                    }
                }
            }
        }

        SectionLabel("Profile", Modifier.padding(top = 22.dp))
        SoftCard(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenProfile),
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    text = preferences.displayName.ifBlank { "Your profile" },
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = profileLine(preferences),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Text(
                    text = "Edit profile",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }

        SectionLabel("Reminders", Modifier.padding(top = 22.dp))
        SoftCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text(text = "Default reminder", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "New habits use this time when you turn a reminder on.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
                PillButton(
                    label = formatMinutes(preferences.defaultReminderMinutes),
                    onClick = { pickingTime = true },
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }

        SectionLabel("Day list", Modifier.padding(top = 22.dp))
        SoftCard(Modifier.fillMaxWidth()) {
            Column {
                SettingSwitch(
                    title = "Haptic tick",
                    body = "A short buzz when you check a habit.",
                    checked = preferences.haptics,
                    onCheckedChange = onHaptics,
                )
                SettingSwitch(
                    title = "Finished habits sink",
                    body = "Checked habits move to the bottom so the open ones stay in reach.",
                    checked = preferences.sinkCompleted,
                    onCheckedChange = onSinkCompleted,
                )
            }
        }
    }

    if (pickingTime) {
        val timeState = rememberTimePickerState(
            initialHour = preferences.defaultReminderMinutes / 60,
            initialMinute = preferences.defaultReminderMinutes % 60,
            is24Hour = false,
        )
        AlertDialog(
            onDismissRequest = { pickingTime = false },
            confirmButton = {
                TextButton(onClick = {
                    onDefaultReminder(timeState.hour * 60 + timeState.minute)
                    pickingTime = false
                }) { Text("Set") }
            },
            dismissButton = {
                TextButton(onClick = { pickingTime = false }) { Text("Cancel") }
            },
            text = { TimePicker(state = timeState) },
        )
    }
}

private fun profileLine(preferences: UserPreferences): String {
    val today = LocalDate.now()
    val birth = preferences.birthDate
    val age = birth?.let { date ->
        val years = ageYears(date, today)
        if (years == 1) "1 year old" else "$years years old"
    }
    val parts = listOfNotNull(
        birth?.let(::formatBirthDate),
        age,
        preferences.sex?.label,
        bodySummary(preferences.heightCm, preferences.weightKg, preferences.measureSystem),
        preferences.scheduleLength.label,
    )
    return parts.joinToString(" · ").ifBlank { "Name, date of birth, gender, height, and weight." }
}

@Composable
private fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(bottom = 8.dp, start = 4.dp),
    )
}

@Composable
private fun SettingSwitch(
    title: String,
    body: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        PillSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun AccentSwatch(
    accent: AccentTheme,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val ring by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        label = "accent-ring",
    )
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .border(width = 2.dp, color = ring, shape = CircleShape)
            .clickable(onClick = onClick)
            .semantics {
                role = Role.RadioButton
                contentDescription = accent.label
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(accent.swatch),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}
