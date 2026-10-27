package com.disciplinescore.app.ui.calendar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.disciplinescore.app.domain.DISCIPLINE_THRESHOLD
import com.disciplinescore.app.ui.CalendarCellUi
import com.disciplinescore.app.ui.CalendarDayUi
import com.disciplinescore.app.ui.CalendarKind
import com.disciplinescore.app.ui.CalendarUi
import com.disciplinescore.app.ui.TodayHabitUi
import com.disciplinescore.app.ui.components.ScreenHeader
import com.disciplinescore.app.ui.components.SkippedHabitsCard
import com.disciplinescore.app.ui.components.SoftCard
import com.disciplinescore.app.ui.icon
import com.disciplinescore.app.ui.tint
import java.time.LocalDate

@Composable
fun CalendarScreen(
    state: CalendarUi,
    haptics: Boolean,
    onOpenSettings: () -> Unit,
    onOpenProfile: () -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onThisMonth: () -> Unit,
    onSelect: (LocalDate) -> Unit,
    onToggle: (Long) -> Unit,
    onSkip: (Long) -> Unit,
    onRestore: (Long) -> Unit,
    onOpenDay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 8.dp, bottom = 28.dp),
    ) {
        ScreenHeader(
            title = "Calendar",
            subtitle = "Each day keeps its score. $DISCIPLINE_THRESHOLD or better holds the day.",
            onOpenSettings = onOpenSettings,
            onOpenProfile = onOpenProfile,
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onPreviousMonth, enabled = state.canGoBack) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous month")
            }
            Text(
                text = state.title,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onNextMonth, enabled = state.canGoForward) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next month")
            }
        }
        if (state.canGoForward) {
            TextButton(
                onClick = onThisMonth,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Text("This month")
            }
        }

        SoftCard(Modifier.fillMaxWidth().padding(top = 4.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    text = state.averageLabel,
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "Month average",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = state.disciplinedLine,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 8.dp),
                )
                state.completionLine?.let { line ->
                    Text(
                        text = line,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                state.bestLine?.let { line ->
                    Text(
                        text = line,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .clickable(enabled = state.bestDate != null) {
                                state.bestDate?.let(onSelect)
                            },
                    )
                }
                state.previousLine?.let { line ->
                    Text(
                        text = line,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
        }

        SoftCard(Modifier.fillMaxWidth().padding(top = 12.dp)) {
            Column(Modifier.padding(horizontal = 8.dp, vertical = 12.dp)) {
                Row(Modifier.fillMaxWidth()) {
                    state.weekdayLabels.forEach { label ->
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                state.cells.chunked(7).forEach { week ->
                    Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                        week.forEach { cell ->
                            DayCell(
                                cell = cell,
                                onClick = {
                                    val date = cell.date
                                    if (date != null && cell.kind != CalendarKind.FUTURE) onSelect(date)
                                },
                                modifier = Modifier.weight(1f).padding(horizontal = 2.dp),
                            )
                        }
                    }
                }
                Legend(Modifier.padding(top = 12.dp, start = 4.dp, end = 4.dp))
            }
        }

        state.selected?.let { day ->
            Text(
                text = day.heading,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 20.dp),
            )
            Text(
                text = "${day.scoreLabel} · ${day.progress}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp, bottom = 10.dp),
            )
            if (day.emptyMessage != null) {
                SoftCard(Modifier.fillMaxWidth()) {
                    Text(
                        text = day.emptyMessage,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            } else {
                SoftCard(Modifier.fillMaxWidth()) {
                    day.rows.forEachIndexed { index, row ->
                        DayHabitRow(
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
                        if (index != day.rows.lastIndex) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f))
                        }
                    }
                }
            }
            if (day.skipped.isNotEmpty()) {
                SkippedHabitsCard(
                    skipped = day.skipped,
                    onRestore = { id ->
                        if (haptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onRestore(id)
                    },
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
            TextButton(onClick = onOpenDay, modifier = Modifier.padding(top = 4.dp)) {
                Text("Open this day")
            }
        }
    }
}

@Composable
private fun DayCell(
    cell: CalendarCellUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = cellColors(cell.kind)
    val description = when {
        cell.date == null -> ""
        cell.kind == CalendarKind.FUTURE -> "${cell.dayNumber}, later this month"
        cell.scoreLabel == null -> "${cell.dayNumber}, nothing scheduled"
        else -> "${cell.dayNumber}, score ${cell.scoreLabel}"
    }
    val clickable = cell.date != null && cell.kind != CalendarKind.FUTURE && cell.kind != CalendarKind.BLANK
    Surface(
        modifier = modifier
            .height(52.dp)
            .semantics { if (description.isNotEmpty()) contentDescription = description }
            .then(if (clickable) Modifier.clickable(onClick = onClick) else Modifier),
        shape = MaterialTheme.shapes.small,
        color = colors.background,
        border = if (cell.selected) {
            BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
        } else {
            null
        },
    ) {
        if (cell.kind != CalendarKind.BLANK) {
            Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = cell.dayNumber,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (cell.today) MaterialTheme.colorScheme.primary else colors.day,
                )
                Text(
                    text = cell.scoreLabel ?: if (cell.kind == CalendarKind.OFF) "–" else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.score,
                )
            }
        }
    }
}

private data class CellColors(val background: Color, val day: Color, val score: Color)

@Composable
private fun cellColors(kind: CalendarKind): CellColors {
    val scheme = MaterialTheme.colorScheme
    return when (kind) {
        CalendarKind.MET -> CellColors(
            background = scheme.secondary.copy(alpha = 0.22f),
            day = scheme.onSurface,
            score = scheme.secondary,
        )
        CalendarKind.MISSED -> CellColors(
            background = scheme.tertiary.copy(alpha = 0.18f),
            day = scheme.onSurface,
            score = scheme.tertiary,
        )
        CalendarKind.OPEN -> CellColors(
            background = scheme.primary.copy(alpha = 0.16f),
            day = scheme.onSurface,
            score = scheme.primary,
        )
        CalendarKind.OFF -> CellColors(
            background = scheme.surface,
            day = scheme.onSurfaceVariant,
            score = scheme.onSurfaceVariant,
        )
        CalendarKind.FUTURE -> CellColors(
            background = Color.Transparent,
            day = scheme.onSurfaceVariant.copy(alpha = 0.45f),
            score = scheme.onSurfaceVariant,
        )
        CalendarKind.BLANK -> CellColors(Color.Transparent, Color.Transparent, Color.Transparent)
    }
}

@Composable
private fun Legend(modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        LegendItem("On track", MaterialTheme.colorScheme.secondary)
        LegendItem("Under 70", MaterialTheme.colorScheme.tertiary)
        LegendItem("Open", MaterialTheme.colorScheme.primary)
        LegendItem("Quiet", MaterialTheme.colorScheme.outline)
    }
}

@Composable
private fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}

@Composable
private fun DayHabitRow(row: TodayHabitUi, onToggle: () -> Unit, onSkip: () -> Unit) {
    val tint = row.category.tint()
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .semantics { role = Role.Checkbox }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(row.category.icon(), contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
            }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(text = row.name, style = MaterialTheme.typography.titleMedium)
                if (row.note.isNotBlank()) {
                    Text(text = row.note, style = MaterialTheme.typography.bodyMedium)
                }
                row.focusLabel?.let { label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
            if (row.completed) {
                Icon(Icons.Filled.Check, contentDescription = "Done", tint = tint)
            } else {
                Text(
                    text = "To do",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (!row.completed) {
            TextButton(
                onClick = onSkip,
                modifier = Modifier.padding(start = 48.dp, bottom = 4.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
            ) {
                Text("Not today")
            }
        }
    }
}
