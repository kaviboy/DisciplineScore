package com.disciplinescore.app.domain

import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.roundToInt

private const val WINDOW_DAYS = 56
private const val MIN_SCORED_DAYS = 7
private const val WEAK_HABIT_PERCENT = 75
private const val WEEKDAY_GAP = 25
private const val SCHEDULE_GAP = 12

/**
 * Patterns from finished days only. Today is excluded so an open day
 * does not look like a skip.
 */
fun buildInsights(
    habits: List<Habit>,
    byDate: Map<LocalDate, Set<Long>>,
    today: LocalDate,
    skippedByDate: Map<LocalDate, Set<Long>> = emptyMap(),
    windowDays: Int = WINDOW_DAYS,
    lookup: DayLookup? = null,
): InsightReport {
    val end = today.minusDays(1)
    val start = end.minusDays((windowDays - 1).toLong())
    if (habits.isEmpty() || end.isBefore(start)) {
        return InsightReport(0, false, listOf(Insight.NeedsMoreDays(0, MIN_SCORED_DAYS)))
    }

    val scored = mutableListOf<DayScore>()
    var date = start
    while (!date.isAfter(end)) {
        val day = lookup?.score(date)
            ?: scoreDay(date, habits, byDate[date].orEmpty(), skippedByDate[date].orEmpty())
        if (day.score != null) scored += day
        date = date.plusDays(1)
    }
    if (scored.size < MIN_SCORED_DAYS) {
        return InsightReport(
            daysTracked = scored.size,
            ready = false,
            insights = listOf(Insight.NeedsMoreDays(scored.size, MIN_SCORED_DAYS)),
        )
    }

    val cards = mutableListOf<Insight>()
    val rates = habits.mapNotNull { habit ->
        val instances = scored.filter { day -> day.rows.any { it.habit.id == habit.id } }
        if (instances.isEmpty()) {
            null
        } else {
            val completed = instances.count { day ->
                day.rows.any { it.habit.id == habit.id && it.completed }
            }
            HabitWindow(
                habit = habit,
                scheduled = instances.size,
                percent = ((completed * 100.0) / instances.size).roundToInt(),
            )
        }
    }
    val weakest = rates
        .filter { it.scheduled >= 4 && it.percent <= WEAK_HABIT_PERCENT }
        .minWithOrNull(compareBy({ it.percent }, { it.habit.name }))
    if (weakest != null) {
        cards += Insight.WeakHabit(weakest.habit.name, weakest.percent, weakest.scheduled)
        val dayRates = DayOfWeek.values().mapNotNull { dow ->
            val instances = scored.filter { day ->
                day.date.dayOfWeek == dow && day.rows.any { it.habit.id == weakest.habit.id }
            }
            if (instances.size < 2) {
                null
            } else {
                val completed = instances.count { day ->
                    day.rows.any { it.habit.id == weakest.habit.id && it.completed }
                }
                DayRate(dow, ((completed * 100.0) / instances.size).roundToInt())
            }
        }
        val worstDay = dayRates.minWithOrNull(compareBy({ it.percent }, { it.day.value }))
        if (worstDay != null && worstDay.percent <= weakest.percent - WEEKDAY_GAP) {
            cards += Insight.WeakWeekday(weakest.habit.name, worstDay.day, worstDay.percent)
        }
    }

    val work = scored.filter { it.date.dayOfWeek.value in 1..5 }.mapNotNull { it.score }
    val weekend = scored.filter { it.date.dayOfWeek.value >= 6 }.mapNotNull { it.score }
    if (work.size >= 3 && weekend.size >= 2) {
        val workAvg = work.average().roundToInt()
        val weekendAvg = weekend.average().roundToInt()
        if (abs(workAvg - weekendAvg) >= SCHEDULE_GAP) {
            cards += Insight.WeekdayGap(workAvg, weekendAvg)
        }
    }
    if (cards.isEmpty()) cards += Insight.Holding
    return InsightReport(scored.size, true, cards)
}

private data class HabitWindow(
    val habit: Habit,
    val scheduled: Int,
    val percent: Int,
)

private data class DayRate(
    val day: DayOfWeek,
    val percent: Int,
)
