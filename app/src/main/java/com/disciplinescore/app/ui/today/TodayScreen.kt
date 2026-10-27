package com.disciplinescore.app.ui.today

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.disciplinescore.app.domain.DISCIPLINE_THRESHOLD
import com.disciplinescore.app.domain.GymDay
import com.disciplinescore.app.domain.TrainingGoal
import com.disciplinescore.app.domain.buildWorkoutWeek
import com.disciplinescore.app.ui.TodayHabitUi
import com.disciplinescore.app.ui.TodayUi
import com.disciplinescore.app.ui.components.CutlineMark
import com.disciplinescore.app.ui.components.ScoreRing
import com.disciplinescore.app.ui.components.SkippedHabitsCard
import com.disciplinescore.app.ui.components.SoftCard
import com.disciplinescore.app.ui.icon
import com.disciplinescore.app.ui.tint
import java.time.LocalTime

@Composable
fun TodayScreen(
    state: TodayUi,
    displayName: String,
    bodyLine: String?,
    haptics: Boolean,
    sinkCompleted: Boolean,
    onToggle: (Long) -> Unit,
    onSkip: (Long) -> Unit,
    onRestore: (Long) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenSettings: () -> Unit,
    trainingGoal: TrainingGoal,
    workoutDays: Int,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val rows = if (sinkCompleted) state.rows.sortedBy { it.completed } else state.rows
    val status = scoreStatus(state.score)
    val progress = if (state.score == null) 0f else state.score / 100f
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 8.dp, bottom = 28.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CutlineMark()
                    Text(
                        text = "Cutline",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
                Text(
                    text = greeting(displayName),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(top = 4.dp),
                )
                if (!bodyLine.isNullOrBlank()) {
                    Text(
                        text = bodyLine,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
            IconButton(onClick = onOpenProfile) {
                Icon(Icons.Filled.Person, contentDescription = "Profile")
            }
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Filled.Settings, contentDescription = "Customize")
            }
        }
        Text(
            text = state.heading,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onPrevious) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous day")
            }
            Text(
                text = state.dateLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onNext, enabled = state.canGoForward) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next day")
            }
        }
        if (!state.isToday) {
            TextButton(onClick = onToday, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Back to today")
            }
        }

        RestToday(
            isToday = state.isToday,
            weekdayIndex = state.weekdayIndex,
            days = buildWorkoutWeek(trainingGoal, workoutDays),
        )

        SoftCard(Modifier.fillMaxWidth().padding(top = 12.dp)) {
            Column(
                Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ScoreRing(score = state.score)
                StatusPill(label = status.label, color = status.color())
                Text(
                    text = state.progressLine,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = 10.dp),
                )
                state.markHint?.let { hint ->
                    Text(
                        text = hint,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (state.score != null && state.score >= DISCIPLINE_THRESHOLD) {
                            MaterialTheme.colorScheme.secondary
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                    )
                }
                if (state.score != null) {
                    val animatedProgress by animateFloatAsState(
                        targetValue = progress,
                        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow),
                        label = "progress",
                    )
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 28.dp, vertical = 10.dp)
                            .height(8.dp)
                            .clip(CircleShape),
                        color = status.color(),
                        trackColor = MaterialTheme.colorScheme.surfaceContainer,
                    )
                }
                Text(
                    text = "The mark on the ring is $DISCIPLINE_THRESHOLD. Reach it and the day counts.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatCard(
                value = state.streakValue,
                caption = state.streakCaption,
                label = "Streak",
                icon = Icons.Filled.LocalFireDepartment,
                modifier = Modifier.weight(1f),
            )
            StatCard(
                value = state.recoveryValue,
                caption = state.recoveryCaption,
                label = "Recovery",
                icon = Icons.Filled.Sync,
                modifier = Modifier.weight(1f),
            )
        }

        state.banner?.let { banner ->
            Surface(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f),
            ) {
                Text(
                    text = banner,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }

        Text(
            text = state.listHeading,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(
                top = 18.dp,
                bottom = if (state.rows.isNotEmpty()) 0.dp else 10.dp,
            ),
        )
        if (state.rows.isNotEmpty()) {
            Text(
                text = "Tap a habit to check it off. Not today leaves it out of the score.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 10.dp),
            )
        }
        if (state.emptyMessage != null) {
            SoftCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    Text(text = "A quiet day", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = state.emptyMessage,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        } else {
            SoftCard(Modifier.fillMaxWidth()) {
                rows.forEachIndexed { index, row ->
                    HabitCheckRow(
                        row = row,
                        onToggle = {
                            if (haptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onToggle(row.id)
                        },
                        onSkip = {
                            if (haptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onSkip(row.id)
                        },
                    )
                    if (index != rows.lastIndex) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f))
                    }
                }
            }
        }

        if (state.skipped.isNotEmpty()) {
            SkippedHabitsCard(
                skipped = state.skipped,
                onRestore = { id ->
                    if (haptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onRestore(id)
                },
                modifier = Modifier.padding(top = 12.dp),
            )
        }

        SoftCard(Modifier.fillMaxWidth().padding(top = 12.dp)) {
            Text(
                text = state.principle,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@Composable
private fun RestToday(isToday: Boolean, weekdayIndex: Int, days: List<GymDay>) {
    val day = days.getOrNull(weekdayIndex) ?: return
    if (day.training) return
    SoftCard(Modifier.fillMaxWidth().padding(top = 12.dp)) {
        Text(
            text = if (isToday) "Today is a rest day." else "This day is a rest day.",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(16.dp),
        )
    }
}

private data class ScoreStatus(val label: String, val tone: StatusTone)

private enum class StatusTone { REST, OPEN, MET }

private fun scoreStatus(score: Int?): ScoreStatus = when {
    score == null -> ScoreStatus("Rest day", StatusTone.REST)
    score >= DISCIPLINE_THRESHOLD -> ScoreStatus("On track", StatusTone.MET)
    score == 0 -> ScoreStatus("Ready when you are", StatusTone.OPEN)
    else -> ScoreStatus("Still open", StatusTone.OPEN)
}

@Composable
private fun ScoreStatus.color(): Color = when (tone) {
    StatusTone.REST -> MaterialTheme.colorScheme.onSurfaceVariant
    StatusTone.OPEN -> MaterialTheme.colorScheme.primary
    StatusTone.MET -> MaterialTheme.colorScheme.secondary
}

@Composable
private fun StatusPill(label: String, color: Color) {
    Surface(
        shape = CircleShape,
        color = color.copy(alpha = 0.14f),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = color,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun StatCard(
    value: String,
    caption: String,
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    SoftCard(modifier) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                text = caption,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun HabitCheckRow(row: TodayHabitUi, onToggle: () -> Unit, onSkip: () -> Unit) {
    val status = if (row.completed) "Done" else "To do"
    val tint = row.category.tint()
    val fill by animateColorAsState(
        targetValue = if (row.completed) tint else Color.Transparent,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "check",
    )
    val checkScale by animateFloatAsState(
        targetValue = if (row.completed) 1f else 0.88f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "check-scale",
    )
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .semantics {
                    role = Role.Checkbox
                    stateDescription = status
                }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = row.category.icon(),
                    contentDescription = null,
                    tint = tint,
                )
            }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(text = row.name, style = MaterialTheme.typography.titleMedium)
                if (row.note.isNotBlank()) {
                    Text(
                        text = row.note,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                Text(
                    text = row.meta,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
                row.focusLabel?.let { label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = status,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (row.completed) tint else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Box(
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .size(30.dp)
                        .scale(checkScale)
                        .clip(CircleShape)
                        .background(fill)
                        .border(
                            width = 1.5.dp,
                            color = tint,
                            shape = CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (row.completed) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = null,
                            tint = if (tint.luminance() > 0.65f) Color(0xFF1C1915) else Color.White,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }
        if (!row.completed) {
            TextButton(
                onClick = onSkip,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                modifier = Modifier.padding(start = 54.dp, bottom = 4.dp),
            ) {
                Text("Not today")
            }
        }
    }
}

private fun greeting(name: String): String {
    val hello = when (LocalTime.now().hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..21 -> "Good evening"
        else -> "Good night"
    }
    val trimmed = name.trim()
    return if (trimmed.isEmpty()) hello else "$hello, $trimmed"
}
