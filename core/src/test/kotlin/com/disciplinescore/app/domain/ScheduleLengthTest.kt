package com.disciplinescore.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ScheduleLengthTest {
    private val start = LocalDate.of(2026, 10, 20)

    @Test
    fun blockEndsOnTheChosenLength() {
        assertEquals(LocalDate.of(2026, 11, 17), blockEnd(start, ScheduleLength.FOUR_WEEKS))
        assertEquals(LocalDate.of(2026, 12, 1), blockEnd(start, ScheduleLength.SIX_WEEKS))
        assertEquals(LocalDate.of(2026, 11, 20), blockEnd(start, ScheduleLength.ONE_MONTH))
        assertEquals(LocalDate.of(2026, 12, 20), blockEnd(start, ScheduleLength.TWO_MONTHS))
        assertEquals(LocalDate.of(2027, 1, 20), blockEnd(start, ScheduleLength.THREE_MONTHS))
    }

    @Test
    fun blockIsDueOnTheEndDateAndNotTheDayBefore() {
        val end = blockEnd(start, ScheduleLength.SIX_WEEKS)
        assertFalse(blockIsDue(start, ScheduleLength.SIX_WEEKS, end.minusDays(1)))
        assertTrue(blockIsDue(start, ScheduleLength.SIX_WEEKS, end))
    }

    @Test
    fun nextBlockUsesADifferentWeek() {
        val first = buildWorkoutWeek(TrainingGoal.CUTTING, 4, block = 0)
        val next = buildWorkoutWeek(TrainingGoal.CUTTING, 4, block = 1)
        assertEquals(first[0].exercises, next[3].exercises)
        assertEquals("Monday", weekdayOf(next[0].title))
        assertTrue(first[0].exercises != next[0].exercises)
    }
}