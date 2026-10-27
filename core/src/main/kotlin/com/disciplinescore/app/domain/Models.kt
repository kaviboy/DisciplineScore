package com.disciplinescore.app.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.roundToInt

/** A day at this score or better counts as disciplined and extends the streak. */
const val DISCIPLINE_THRESHOLD = 70

enum class Importance(val weight: Int, val label: String) {
    LIGHT(1, "Light"),
    STANDARD(2, "Standard"),
    HIGH(3, "High"),
    ;

    companion object {
        fun fromWeight(weight: Int): Importance = values().firstOrNull { it.weight == weight } ?: STANDARD
    }
}

enum class HabitCategory(val label: String) {
    BODY("Body"),
    MIND("Mind"),
    REST("Rest"),
    HEALTH("Health"),
    FOCUS("Focus"),
}

data class Habit(
    val id: Long,
    val name: String,
    val importance: Importance,
    val category: HabitCategory,
    val days: Set<DayOfWeek>,
    val reminderMinutes: Int?,
    val sortOrder: Int,
    val createdOn: LocalDate,
    val note: String = "",
)

fun Habit.appliesOn(date: LocalDate): Boolean =
    !date.isBefore(createdOn) && date.dayOfWeek in days

data class Completion(
    val habitId: Long,
    val date: LocalDate,
)

/** A habit set aside for one day. It is not scheduled, so it cannot count as a miss. */
data class Skip(
    val habitId: Long,
    val date: LocalDate,
)

data class DisciplineSnapshot(
    val habits: List<Habit>,
    val completions: List<Completion>,
    val skips: List<Skip> = emptyList(),
)

data class HabitDayRow(
    val habit: Habit,
    val completed: Boolean,
)

data class DayScore(
    val date: LocalDate,
    val score: Int?,
    val scheduledCount: Int,
    val completedCount: Int,
    val scheduledWeight: Int,
    val completedWeight: Int,
    val rows: List<HabitDayRow>,
)

/** What is left between this score and a day that counts. */
data class MarkGap(
    val reached: Boolean,
    val remainingCount: Int,
    val nextHabitId: Long?,
    val nextHabitName: String?,
    /** True when checking only [nextHabitName] is enough to reach the mark. */
    val nextAloneReaches: Boolean,
)

enum class DayKind { DISCIPLINED, MISSED, OFF }

fun DayScore.kind(): DayKind = when {
    score == null -> DayKind.OFF
    score >= DISCIPLINE_THRESHOLD -> DayKind.DISCIPLINED
    else -> DayKind.MISSED
}

data class RecoveryEvent(
    val habitId: Long,
    val habitName: String,
    val missedOn: LocalDate,
    val returnedOn: LocalDate,
    val fast: Boolean,
)

data class OpenGap(
    val habitId: Long,
    val habitName: String,
    val missedOn: LocalDate,
)

data class RecoverySummary(
    val events: List<RecoveryEvent>,
    val openGaps: List<OpenGap>,
    val averageDaysAway: Double?,
)

data class HabitRate(
    val habitId: Long,
    val name: String,
    val percent: Int,
    val completed: Int,
    val scheduled: Int,
)

data class MonthDay(
    val date: LocalDate,
    val future: Boolean,
    val score: DayScore?,
)

data class MonthProgress(
    val month: YearMonth,
    val days: List<MonthDay>,
    val averageScore: Int?,
    val disciplinedDays: Int,
    val scoredDays: Int,
    val scheduledInstances: Int,
    val completedInstances: Int,
    val bestDate: LocalDate?,
    val bestScore: Int?,
    val previousAverage: Int?,
)

data class WeekReport(
    val start: LocalDate,
    val end: LocalDate,
    val days: List<DayScore>,
    val averageScore: Int?,
    val scheduledInstances: Int,
    val completedInstances: Int,
    val disciplinedDays: Int,
    val scoredDays: Int,
    val habitRates: List<HabitRate>,
) {
    val completionPercent: Int?
        get() = if (scheduledInstances == 0) {
            null
        } else {
            ((completedInstances * 100.0) / scheduledInstances).roundToInt()
        }
}

sealed interface Insight {
    data class NeedsMoreDays(val daysTracked: Int, val required: Int) : Insight
    data class WeakHabit(val name: String, val percent: Int, val scheduled: Int) : Insight
    data class WeakWeekday(val habitName: String, val day: DayOfWeek, val percent: Int) : Insight
    data class WeekdayGap(val workdayAverage: Int, val weekendAverage: Int) : Insight
    data object Holding : Insight
}

data class InsightReport(
    val daysTracked: Int,
    val ready: Boolean,
    val insights: List<Insight>,
)
