package com.disciplinescore.app.ui.plan

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.disciplinescore.app.domain.CustomDay
import com.disciplinescore.app.domain.ScheduleLength
import com.disciplinescore.app.domain.blockEnd
import com.disciplinescore.app.domain.blockIsDue
import com.disciplinescore.app.domain.MAX_EXERCISES_PER_DAY
import com.disciplinescore.app.domain.MAX_TRAINING_DAYS
import com.disciplinescore.app.domain.MIN_TRAINING_DAYS
import com.disciplinescore.app.domain.TrainingGoal
import com.disciplinescore.app.domain.addExercise
import com.disciplinescore.app.domain.applyCustomExercises
import com.disciplinescore.app.domain.buildWorkoutWeek
import com.disciplinescore.app.domain.exerciseName
import com.disciplinescore.app.domain.filledWeek
import com.disciplinescore.app.domain.removeExercise
import com.disciplinescore.app.domain.sanitizeSessionLabel
import com.disciplinescore.app.domain.setSessionLabel
import com.disciplinescore.app.domain.snapshotWeek
import com.disciplinescore.app.domain.weekdayOf
import com.disciplinescore.app.domain.workoutNote
import com.disciplinescore.app.ui.components.ChoiceTrack
import com.disciplinescore.app.ui.components.ScreenHeader
import com.disciplinescore.app.ui.components.SoftCard
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun WorkoutScreen(
    goal: TrainingGoal,
    trainingDays: Int,
    customDays: List<CustomDay>?,
    blockStart: LocalDate?,
    scheduleLength: ScheduleLength,
    block: Int,
    onGenerate: () -> Unit,
    onTrainingDays: (Int) -> Unit,
    onTrainingGoal: (TrainingGoal) -> Unit,
    onCustomDays: (List<CustomDay>?) -> Unit,
    onOpenProfile: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val template = remember(goal, trainingDays, block) {
        buildWorkoutWeek(goal, trainingDays, block)
    }
    val suggested = snapshotWeek(template)
    var editingDay by remember { mutableStateOf<Int?>(null) }
    var sessionName by remember { mutableStateOf("") }
    var draft by remember { mutableStateOf(filledWeek(template, customDays)) }
    LaunchedEffect(goal, trainingDays, block) {
        editingDay = null
        draft = filledWeek(template, customDays)
    }
    val shown = applyCustomExercises(template, if (editingDay != null) draft else customDays)
    val lifting = shown.count { it.training }
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 8.dp, bottom = 32.dp),
    ) {
        ScreenHeader(
            title = "Workout",
            subtitle = "${goal.label}. Use the pencil on a day, then Update to keep that day.",
            onOpenSettings = onOpenSettings,
            onOpenProfile = onOpenProfile,
        )
        GoalToggle(
            goal = goal,
            onGoal = onTrainingGoal,
            enabled = editingDay == null,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        Text(
            text = "Schedule",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
        )
        ChoiceTrack(
            labels = (MIN_TRAINING_DAYS..MAX_TRAINING_DAYS).map { it.toString() },
            captions = (MIN_TRAINING_DAYS..MAX_TRAINING_DAYS).map { if (it == 1) "day" else "days" },
            selectedIndex = trainingDays - MIN_TRAINING_DAYS,
            enabled = editingDay == null,
            onSelect = { onTrainingDays(it + MIN_TRAINING_DAYS) },
        )
        Text(
            text = workoutNote(goal, trainingDays),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp),
        )
        NextBlockCard(
            start = blockStart,
            length = scheduleLength,
            enabled = editingDay == null,
            onGenerate = onGenerate,
        )
        Text(
            text = if (lifting == 1) "1 lifting day" else "$lifting lifting days",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(top = 18.dp),
        )
        shown.forEachIndexed { dayIndex, day ->
            if (!day.training) return@forEachIndexed
            val editingThisDay = editingDay == dayIndex
            SoftCard(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Column(Modifier.padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (editingThisDay) {
                            OutlinedTextField(
                                value = sessionName,
                                onValueChange = { value ->
                                    if (value.length <= 24 && '\n' !in value) sessionName = value
                                },
                                label = { Text(weekdayOf(day.title)) },
                                singleLine = true,
                                modifier = Modifier.weight(1f).padding(top = 8.dp, end = 8.dp),
                            )
                        } else {
                            Text(
                                text = day.title,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (editingThisDay) {
                            TextButton(onClick = {
                                editingDay = null
                                draft = filledWeek(template, customDays)
                            }) { Text("Cancel") }
                            Button(onClick = {
                                if (sanitizeSessionLabel(sessionName) == null) return@Button
                                val named = setSessionLabel(draft, dayIndex, sessionName)
                                onCustomDays(if (named == suggested) null else named)
                                editingDay = null
                            }) { Text("Update") }
                        } else if (editingDay == null) {
                            IconButton(onClick = {
                                val filled = filledWeek(template, customDays)
                                draft = filled
                                sessionName = filled[dayIndex].label
                                editingDay = dayIndex
                            }) {
                                Icon(Icons.Filled.Edit, contentDescription = "Edit ${day.title}")
                            }
                        }
                    }
                    if (day.exercises.isEmpty() && day.training) {
                        Text(
                            text = "No exercises yet.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    day.exercises.forEachIndexed { exerciseIndex, exercise ->
                        if (!day.training || !editingThisDay) {
                            Text(
                                text = exercise,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        } else {
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = exercise,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f).padding(top = 4.dp),
                                )
                                IconButton(
                                    onClick = {
                                        draft = removeExercise(draft, dayIndex, exerciseIndex)
                                    },
                                ) {
                                    Icon(
                                        Icons.Filled.Close,
                                        contentDescription = "Remove ${exerciseName(exercise)}",
                                    )
                                }
                            }
                        }
                    }
                    if (editingThisDay) {
                        if (day.exercises.size >= MAX_EXERCISES_PER_DAY) {
                            Text(
                                text = "This day already has $MAX_EXERCISES_PER_DAY exercises.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        } else {
                            ExerciseDropdown(
                                goal = goal,
                                exclude = day.exercises.map(::exerciseName).toSet(),
                                onSelect = { line ->
                                    draft = addExercise(draft, dayIndex, line)
                                },
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, end = 12.dp),
                            )
                        }
                    }
                }
            }
        }
        Text(
            text = "A starting week for a healthy adult. Stop if a joint hurts, and use a coach if the bar is new.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

private val blockDateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy")

@Composable
private fun NextBlockCard(
    start: LocalDate?,
    length: ScheduleLength,
    enabled: Boolean,
    onGenerate: () -> Unit,
) {
    val today = remember { LocalDate.now() }
    val end = start?.let { blockEnd(it, length) }
    val due = start != null && blockIsDue(start, length, today)
    SoftCard(Modifier.fillMaxWidth().padding(top = 16.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text("Next workout", style = MaterialTheme.typography.titleMedium)
            Text(
                text = when {
                    end == null -> "Save a schedule length on your profile to start this block."
                    due -> "This ${length.label} block ended on ${end.format(blockDateFormat)}."
                    else -> "This ${length.label} block runs until ${end.format(blockDateFormat)}."
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )
            Button(
                onClick = onGenerate,
                enabled = due && enabled,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            ) {
                Text("Generate new workout")
            }
        }
    }
}

