package com.disciplinescore.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.temporal.ChronoUnit

class DisciplineCalculatorTest {
    private val friday = LocalDate.of(2026, 10, 16)

    @Test
    fun fourOfFiveEqualWeightsScoresEighty() {
        val habits = sampleHabits(friday)
        val completed = setOf(1L, 2L, 3L, 5L)
        val score = scoreDay(friday, habits, completed)

        assertEquals(80, score.score)
        assertEquals(4, score.completedCount)
        assertEquals(5, score.scheduledCount)
        assertEquals(DayKind.DISCIPLINED, score.kind())
        assertFalse(score.rows.first { it.habit.name == "Sleep on time" }.completed)
        assertTrue(score.rows.first { it.habit.name == "Gym workout" }.completed)
    }

    @Test
    fun higherImportanceMovesTheScoreMore() {
        val habits = listOf(
            habit(1, importance = Importance.HIGH, createdOn = friday),
            habit(2, importance = Importance.STANDARD, createdOn = friday),
        )
        assertEquals(60, scoreDay(friday, habits, setOf(1L)).score)
        assertEquals(40, scoreDay(friday, habits, setOf(2L)).score)
    }

    @Test
    fun daysBeforeAHabitStartsAreOffNotMissed() {
        val habits = listOf(habit(1, createdOn = friday))
        val score = scoreDay(friday.minusDays(1), habits, emptySet())
        assertNull(score.score)
        assertEquals(DayKind.OFF, score.kind())
    }

    @Test
    fun monthLeavesTheFutureBlankAndAveragesOnlyScoredDays() {
        val today = LocalDate.of(2026, 10, 16)
        val started = LocalDate.of(2026, 10, 15)
        val habit = habit(1, createdOn = started)
        val report = monthProgress(
            month = YearMonth.of(2026, 10),
            today = today,
            habits = listOf(habit),
            byDate = mapOf(today to setOf(1L)),
        )

        val beforeStart = report.days.first { it.date.dayOfMonth == 14 }
        val missed = report.days.first { it.date.dayOfMonth == 15 }
        val held = report.days.first { it.date.dayOfMonth == 16 }
        val ahead = report.days.first { it.date.dayOfMonth == 17 }

        assertFalse(beforeStart.future)
        assertNull(beforeStart.score?.score)
        assertEquals(0, missed.score?.score)
        assertEquals(100, held.score?.score)
        assertTrue(ahead.future)
        assertNull(ahead.score)
        assertEquals(50, report.averageScore)
        assertEquals(1, report.disciplinedDays)
        assertEquals(2, report.scoredDays)
        assertEquals(today, report.bestDate)
        assertEquals(100, report.bestScore)
        assertNull(report.previousAverage)
    }

    @Test
    fun unfinishedTodayDoesNotBreakTheStreak() {
        val habit = habit(1, createdOn = friday.minusDays(10))
        val wednesday = friday.minusDays(2)
        val thursday = friday.minusDays(1)
        val byDate = mapOf(wednesday to setOf(1L), thursday to setOf(1L))

        assertEquals(2, currentStreak(friday, listOf(habit), byDate, inProgress = true))
    }

    @Test
    fun aClosedMissBreaksTheStreak() {
        val habit = habit(1, createdOn = friday.minusDays(10))
        val wednesday = friday.minusDays(2)
        val byDate = mapOf(wednesday to setOf(1L), friday to setOf(1L))

        assertEquals(1, currentStreak(friday, listOf(habit), byDate, inProgress = true))
        assertEquals(
            0,
            currentStreak(friday.minusDays(1), listOf(habit), byDate, inProgress = false),
        )
    }

    @Test
    fun unscheduledWeekendDoesNotBreakAWeekdayStreak() {
        val tuesday = LocalDate.of(2026, 10, 13)
        val weekdays = setOf(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY,
        )
        val habit = habit(
            id = 1,
            createdOn = LocalDate.of(2026, 10, 9),
            days = weekdays,
        )
        val byDate = mapOf(
            LocalDate.of(2026, 10, 9) to setOf(1L),
            LocalDate.of(2026, 10, 12) to setOf(1L),
        )

        assertEquals(2, currentStreak(tuesday, listOf(habit), byDate, inProgress = true))
        assertEquals(
            0,
            currentStreak(tuesday, listOf(habit), mapOf(LocalDate.of(2026, 10, 9) to setOf(1L)), inProgress = true),
        )
    }

    @Test
    fun returningTheNextScheduledDayIsAFastRecovery() {
        val habit = habit(1, name = "Gym workout", createdOn = friday.minusDays(1))
        val summary = recoverySummary(listOf(habit), friday, mapOf(friday to setOf(1L)))
        val event = summary.events.single()

        assertTrue(event.fast)
        assertEquals(1, ChronoUnit.DAYS.between(event.missedOn, event.returnedOn))
        assertTrue(summary.openGaps.isEmpty())
        assertEquals(1.0, summary.averageDaysAway!!, 0.001)
    }

    @Test
    fun stayingAwayAnExtraScheduledDayIsNotAFastRecovery() {
        val habit = habit(1, createdOn = friday.minusDays(2))
        val summary = recoverySummary(listOf(habit), friday, mapOf(friday to setOf(1L)))
        val event = summary.events.single()

        assertFalse(event.fast)
        assertEquals(2, ChronoUnit.DAYS.between(event.missedOn, event.returnedOn))
    }

    @Test
    fun todayStillPendingDoesNotOpenAGapByItself() {
        val startedToday = recoverySummary(
            listOf(habit(1, createdOn = friday)),
            friday,
            emptyMap(),
        )
        assertTrue(startedToday.events.isEmpty())
        assertTrue(startedToday.openGaps.isEmpty())

        val missedYesterday = recoverySummary(
            listOf(habit(1, createdOn = friday.minusDays(1))),
            friday,
            emptyMap(),
        )
        assertTrue(missedYesterday.events.isEmpty())
        assertEquals(friday.minusDays(1), missedYesterday.openGaps.single().missedOn)
    }

    @Test
    fun weekAverageIgnoresDaysWithNothingScheduled() {
        val habit = habit(1, createdOn = friday)
        val report = weekReport(friday, listOf(habit), mapOf(friday to setOf(1L)))

        assertEquals(1, report.scoredDays)
        assertEquals(100, report.averageScore)
        assertEquals(1, report.disciplinedDays)
        assertEquals(6, report.days.count { it.score == null })
    }

    @Test
    fun nextReminderSkipsToTheNextMatchingDay() {
        val weekdays = setOf(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY,
        )
        val habit = habit(
            id = 1,
            createdOn = LocalDate.of(2026, 10, 1),
            days = weekdays,
            reminderMinutes = 7 * 60,
        )

        assertEquals(
            LocalDateTime.of(2026, 10, 16, 7, 0),
            nextReminder(habit, LocalDateTime.of(2026, 10, 16, 6, 0)),
        )
        assertEquals(
            LocalDateTime.of(2026, 10, 19, 7, 0),
            nextReminder(habit, LocalDateTime.of(2026, 10, 16, 20, 0)),
        )
    }

    @Test
    fun skippingAHabitTakesItOutOfTheScore() {
        val habits = listOf(
            habit(1, name = "Deep work", importance = Importance.HIGH, createdOn = friday),
            habit(2, name = "Water", importance = Importance.LIGHT, createdOn = friday),
        )
        val skipped = scoreDay(friday, habits, setOf(2L), skippedIds = setOf(1L))

        assertEquals(1, skipped.scheduledCount)
        assertEquals(100, skipped.score)
        assertEquals(DayKind.DISCIPLINED, skipped.kind())
    }

    @Test
    fun aSkippedMissDoesNotOpenARecoveryGap() {
        val habit = habit(1, createdOn = friday.minusDays(1))
        val summary = recoverySummary(
            habits = listOf(habit),
            today = friday,
            byDate = emptyMap(),
            skippedByDate = mapOf(friday.minusDays(1) to setOf(1L)),
        )

        assertTrue(summary.events.isEmpty())
        assertTrue(summary.openGaps.isEmpty())
    }

    @Test
    fun bestStreakKeepsAnOpenTodayFromErasingTheRun() {
        val habit = habit(1, createdOn = friday.minusDays(10))
        val byDate = mapOf(
            friday.minusDays(4) to setOf(1L),
            friday.minusDays(3) to setOf(1L),
            friday.minusDays(2) to setOf(1L),
            friday.minusDays(1) to setOf(1L),
        )

        assertEquals(4, currentStreak(friday, listOf(habit), byDate, inProgress = true))
        assertEquals(4, bestStreak(friday, listOf(habit), byDate))
    }

    @Test
    fun bestStreakRemembersALongerPastRun() {
        val habit = habit(1, createdOn = friday.minusDays(20))
        val byDate = mutableMapOf<LocalDate, Set<Long>>()
        for (daysAgo in 10..15) {
            byDate[friday.minusDays(daysAgo.toLong())] = setOf(1L)
        }
        byDate[friday.minusDays(2)] = setOf(1L)
        byDate[friday.minusDays(1)] = setOf(1L)

        assertEquals(2, currentStreak(friday, listOf(habit), byDate, inProgress = true))
        assertEquals(6, bestStreak(friday, listOf(habit), byDate))
    }

    @Test
    fun markGapPrefersTheLightestHabitThatCrossesTheMark() {
        val habits = listOf(
            habit(1, name = "Lift", importance = Importance.HIGH, createdOn = friday),
            habit(2, name = "Run", importance = Importance.HIGH, createdOn = friday),
            habit(3, name = "Swim", importance = Importance.HIGH, createdOn = friday),
            habit(4, name = "Water", importance = Importance.LIGHT, createdOn = friday),
        )
        val day = scoreDay(friday, habits, setOf(1L, 2L))
        val gap = markGap(day)

        assertEquals(60, day.score)
        assertFalse(gap!!.reached)
        assertEquals(4L, gap.nextHabitId)
        assertEquals("Water", gap.nextHabitName)
        assertTrue(gap.nextAloneReaches)
        assertTrue(markGap(scoreDay(friday, habits, setOf(1L, 2L, 4L)))!!.reached)
    }

    private fun sampleHabits(createdOn: LocalDate) = listOf(
        habit(1, "Gym workout", HabitCategory.BODY, createdOn),
        habit(2, "Study for 1 hour", HabitCategory.FOCUS, createdOn),
        habit(3, "Read 10 pages", HabitCategory.MIND, createdOn),
        habit(4, "Sleep on time", HabitCategory.REST, createdOn),
        habit(5, "Drink enough water", HabitCategory.HEALTH, createdOn),
    )

    private fun habit(
        id: Long,
        name: String = "Gym workout",
        category: HabitCategory = HabitCategory.BODY,
        createdOn: LocalDate,
        importance: Importance = Importance.STANDARD,
        days: Set<DayOfWeek> = DayOfWeek.values().toSet(),
        reminderMinutes: Int? = null,
    ) = Habit(
        id = id,
        name = name,
        importance = importance,
        category = category,
        days = days,
        reminderMinutes = reminderMinutes,
        sortOrder = id.toInt(),
        createdOn = createdOn,
    )
}
