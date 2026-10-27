package com.disciplinescore.app.ui.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerFormatter
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import android.content.res.Configuration
import android.os.LocaleList
import androidx.compose.runtime.CompositionLocalProvider
import com.disciplinescore.app.data.UserPreferences
import com.disciplinescore.app.ui.formatBirthDate
import com.disciplinescore.app.domain.BodySex
import com.disciplinescore.app.domain.MeasureSystem
import com.disciplinescore.app.domain.ScheduleLength
import com.disciplinescore.app.domain.ageYears
import com.disciplinescore.app.domain.blockEnd
import com.disciplinescore.app.ui.components.ChoicePills
import com.disciplinescore.app.ui.components.ChoiceTrack
import com.disciplinescore.app.ui.components.SoftCard
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private val scheduleFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy")

data class ProfileDraft(
    val name: String,
    val birthDate: LocalDate,
    val sex: BodySex,
    val measureSystem: MeasureSystem,
    val heightCm: Float,
    val weightKg: Float,
    val scheduleLength: ScheduleLength,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    initial: UserPreferences,
    onboarding: Boolean,
    onSave: (ProfileDraft) -> Unit,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    var name by remember { mutableStateOf(initial.displayName) }
    var birthDate by remember { mutableStateOf(initial.birthDate) }
    var sex by remember { mutableStateOf(initial.sex) }
    var measureSystem by remember { mutableStateOf(initial.measureSystem) }
    var heightCm by remember { mutableStateOf(initial.heightCm) }
    var weightKg by remember { mutableStateOf(initial.weightKg) }
    var scheduleLength by remember { mutableStateOf(initial.scheduleLength) }
    var pickingDate by remember { mutableStateOf(false) }
    var showMissing by remember { mutableStateOf(false) }
    val today = remember { LocalDate.now() }
    val age = birthDate?.let { ageYears(it, today) }
    val draft = profileDraft(name, birthDate, sex, measureSystem, heightCm, weightKg, scheduleLength)
    val blockStart = initial.workoutStartedOn ?: today
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 8.dp, bottom = 32.dp),
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        }
        Text(
            text = if (onboarding) "Welcome" else "Profile",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(top = if (onBack == null) 12.dp else 0.dp),
        )
        Text(
            text = if (onboarding) {
                "Add your details once. You can change them later from Profile."
            } else {
                "Name, date of birth, gender, height, and weight. These stay on this device."
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
        )
        SoftCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(24) },
                    label = { Text("Name") },
                    placeholder = { Text("Shown in the greeting") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = "Date of birth",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 16.dp),
                )
                TextButton(onClick = { pickingDate = true }, modifier = Modifier.padding(top = 4.dp)) {
                    Text(birthDate?.let(::formatBirthDate) ?: "dd/mm/yyyy")
                }
                if (age != null) {
                    Text(
                        text = if (age == 1) "1 year old" else "$age years old",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = "Gender",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                )
                Text(
                    text = "Used for the calorie estimate, which has a female and a male version.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
                val genders = BodySex.values()
                ChoiceTrack(
                    labels = genders.map { it.label },
                    selectedIndex = genders.indexOfFirst { it == sex },
                    onSelect = { sex = genders[it] },
                )
                BodyMeasureFields(
                    measureSystem = measureSystem,
                    heightCm = heightCm,
                    weightKg = weightKg,
                    onMeasureSystem = { measureSystem = it },
                    onHeightCm = { heightCm = it },
                    onWeightKg = { weightKg = it },
                )
            }
        }
        ScheduleLengthCard(
            selected = scheduleLength,
            ends = blockEnd(blockStart, scheduleLength),
            onSelect = { scheduleLength = it },
        )
        if (showMissing && draft == null) {
            Text(
                text = "Add your name, date of birth, gender, height, and weight to continue.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
        Button(
            onClick = {
                val ready = draft
                if (ready == null) showMissing = true else onSave(ready)
            },
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        ) {
            Text(if (onboarding) "Continue" else "Save")
        }
    }

    if (pickingDate) {
        BirthDateDialog(
            initial = birthDate ?: today.minusYears(25),
            today = today,
            onSet = { birthDate = it },
            onDismiss = { pickingDate = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BirthDateDialog(
    initial: LocalDate,
    today: LocalDate,
    onSet: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial.toUtcMillis(),
        yearRange = (today.year - 120)..today.year,
        selectableDates = birthSelectableDates(today),
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.toUtcDate()?.let(onSet)
                onDismiss()
            }) { Text("Set") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    ) {
        val configuration = Configuration(LocalConfiguration.current)
        configuration.setLocales(LocaleList(Locale.UK))
        CompositionLocalProvider(LocalConfiguration provides configuration) {
            DatePicker(
                state = state,
                dateFormatter = BirthDatePickerFormatter,
            )
        }
    }
}

@Composable
private fun ScheduleLengthCard(
    selected: ScheduleLength,
    ends: LocalDate,
    onSelect: (ScheduleLength) -> Unit,
) {
    SoftCard(Modifier.fillMaxWidth().padding(top = 12.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text("Next schedule", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "How long this workout block lasts. When that day arrives, Workout can generate the next one.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            )
            val lengths = ScheduleLength.values()
            ChoicePills(
                labels = lengths.map { it.label },
                isSelected = { lengths[it] == selected },
                onSelect = { onSelect(lengths[it]) },
            )
            Text(
                text = "Ends ${ends.format(scheduleFormat)}.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}

private fun profileDraft(
    name: String,
    birthDate: LocalDate?,
    sex: BodySex?,
    measureSystem: MeasureSystem,
    heightCm: Float?,
    weightKg: Float?,
    scheduleLength: ScheduleLength,
): ProfileDraft? {
    val trimmed = name.trim()
    val birth = birthDate ?: return null
    val gender = sex ?: return null
    val height = heightCm ?: return null
    val weight = weightKg ?: return null
    if (trimmed.isEmpty()) return null
    if (birth.isAfter(LocalDate.now()) || birth.isBefore(LocalDate.now().minusYears(120))) return null
    return ProfileDraft(trimmed, birth, gender, measureSystem, height, weight, scheduleLength)
}

@OptIn(ExperimentalMaterial3Api::class)
private object BirthDatePickerFormatter : DatePickerFormatter {
    override fun formatMonthYear(monthMillis: Long?, locale: Locale): String? {
        if (monthMillis == null) return null
        return monthMillis.toUtcDate().format(scheduleFormat)
    }

    override fun formatDate(
        dateMillis: Long?,
        locale: Locale,
        forContentDescription: Boolean,
    ): String? {
        if (dateMillis == null) return null
        return formatBirthDate(dateMillis.toUtcDate())
    }
}

private fun LocalDate.toUtcMillis(): Long =
    atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.toUtcDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

@OptIn(ExperimentalMaterial3Api::class)
private fun birthSelectableDates(today: LocalDate): SelectableDates = object : SelectableDates {
    override fun isSelectableDate(utcTimeMillis: Long): Boolean {
        val date = utcTimeMillis.toUtcDate()
        return !date.isAfter(today) && !date.isBefore(today.minusYears(120))
    }

    override fun isSelectableYear(year: Int): Boolean = year in (today.year - 120)..today.year
}
