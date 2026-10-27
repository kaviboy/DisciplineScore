package com.disciplinescore.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class InsightsTest {
    private val today = LocalDate.of(2026, 10, 16)

    @Test
    fun insightsWaitUntilAWeekOfScoredDays() {
        val report = buildInsights(
            habits = listOf(habit(createdOn = today)),
            byDate = emptyMap(),
            today = today,
        )

        assertFalse(report.ready)
        val waiting = report.insights.single() as Insight.NeedsMoreDays
        assertEquals(0, waiting.daysTracked)
        assertEquals(7, waiting.required)
    }

    @Test
    fun mondayMissesSurfaceAsTheWeakWeekday() {
        val habit = habit(createdOn = today.minusDays(80))
        val byDate = mutableMapOf<LocalDate, Set<Long>>()
        var date = today.minusDays(56)
        while (date.isBefore(today)) {
            val missed = date.dayOfWeek == DayOfWeek.MONDAY || date.dayOfWeek == DayOfWeek.TUESDAY
            if (!missed) byDate[date] = setOf(habit.id)
            date = date.plusDays(1)
        }

        val report = buildInsights(listOf(habit), byDate, today)
        assertTrue(report.ready)
        val weak = report.insights.filterIsInstance<Insight.WeakHabit>().single()
        assertEquals("Gym workout", weak.name)
        assertTrue(weak.percent <= 75)
        val weekday = report.insights.filterIsInstance<Insight.WeakWeekday>().single()
        assertEquals(DayOfWeek.MONDAY, weekday.day)
        assertEquals(0, weekday.percent)
    }

    @Test
    fun workdaysLowerThanWeekendsAreCalledOut() {
        val habit = habit(createdOn = today.minusDays(80))
        val byDate = mutableMapOf<LocalDate, Set<Long>>()
        var date = today.minusDays(56)
        while (date.isBefore(today)) {
            if (date.dayOfWeek.value >= 6) byDate[date] = setOf(habit.id)
            date = date.plusDays(1)
        }

        val report = buildInsights(listOf(habit), byDate, today)
        val gap = report.insights.filterIsInstance<Insight.WeekdayGap>().single()
        assertEquals(0, gap.workdayAverage)
        assertEquals(100, gap.weekendAverage)
    }

    private fun habit(createdOn: LocalDate) = Habit(
        id = 1,
        name = "Gym workout",
        importance = Importance.STANDARD,
        category = HabitCategory.BODY,
        days = DayOfWeek.values().toSet(),
        reminderMinutes = null,
        sortOrder = 0,
        createdOn = createdOn,
    )
}
