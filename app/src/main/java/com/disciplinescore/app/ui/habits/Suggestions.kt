package com.disciplinescore.app.ui.habits

import com.disciplinescore.app.domain.HabitCategory
import com.disciplinescore.app.domain.Importance

data class HabitSuggestion(
    val name: String,
    val category: HabitCategory,
    val importance: Importance,
    val note: String,
)

val habitSuggestions = listOf(
    HabitSuggestion("Morning walk", HabitCategory.BODY, Importance.LIGHT, "Ten easy minutes outside."),
    HabitSuggestion("Stretch", HabitCategory.BODY, Importance.LIGHT, "Five minutes is enough."),
    HabitSuggestion("Read 10 pages", HabitCategory.MIND, Importance.STANDARD, "A few pages still count."),
    HabitSuggestion("Journal one line", HabitCategory.MIND, Importance.LIGHT, "What you kept, or what you'll retry."),
    HabitSuggestion("Lights out on time", HabitCategory.REST, Importance.STANDARD, "Same bedtime, most nights."),
    HabitSuggestion("Drink enough water", HabitCategory.HEALTH, Importance.LIGHT, "A glass with each meal."),
    HabitSuggestion("Deep work block", HabitCategory.FOCUS, Importance.HIGH, "One protected hour."),
    HabitSuggestion("No phone first hour", HabitCategory.FOCUS, Importance.STANDARD, "Start the day on purpose."),
)
