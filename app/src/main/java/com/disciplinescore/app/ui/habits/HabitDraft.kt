package com.disciplinescore.app.ui.habits

import com.disciplinescore.app.domain.Habit
import com.disciplinescore.app.domain.HabitCategory
import com.disciplinescore.app.domain.Importance
import java.time.DayOfWeek
import java.time.LocalDate

data class HabitDraft(
    val id: Long,
    val name: String,
    val importance: Importance,
    val category: HabitCategory,
    val days: Set<DayOfWeek>,
    val reminderEnabled: Boolean,
    val reminderMinutes: Int,
    val createdOn: LocalDate,
    val note: String = "",
)

fun HabitDraft.toHabit(): Habit = Habit(
    id = id,
    name = name.trim(),
    importance = importance,
    category = category,
    days = days,
    reminderMinutes = if (reminderEnabled) reminderMinutes else null,
    sortOrder = 0,
    createdOn = createdOn,
    note = note.trim(),
)
