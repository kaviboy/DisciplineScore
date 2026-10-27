package com.disciplinescore.app.data

import com.disciplinescore.app.data.local.HabitEntity
import com.disciplinescore.app.domain.Habit
import com.disciplinescore.app.domain.HabitCategory
import com.disciplinescore.app.domain.Importance
import java.time.DayOfWeek
import java.time.LocalDate

fun HabitEntity.toDomain(): Habit = Habit(
    id = id,
    name = name,
    importance = Importance.fromWeight(importance),
    category = runCatching { HabitCategory.valueOf(category) }.getOrDefault(HabitCategory.FOCUS),
    days = daysMask.toDays(),
    reminderMinutes = reminderMinutes,
    sortOrder = sortOrder,
    createdOn = LocalDate.parse(createdOn),
    note = note,
)

fun Habit.toEntity(): HabitEntity = HabitEntity(
    id = id,
    name = name,
    importance = importance.weight,
    category = category.name,
    daysMask = days.toMask(),
    reminderMinutes = reminderMinutes,
    sortOrder = sortOrder,
    createdOn = createdOn.toString(),
    note = note,
)

fun Set<DayOfWeek>.toMask(): Int =
    fold(0) { mask, day -> mask or (1 shl (day.value - 1)) }

fun Int.toDays(): Set<DayOfWeek> =
    DayOfWeek.values().filter { this and (1 shl (it.value - 1)) != 0 }.toSet()
