package com.disciplinescore.app.domain

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

/**
 * Daily score is completed importance divided by scheduled importance, from 0 to 100.
 * Days with nothing scheduled are left unscored so they do not look like a miss.
 */
fun scoreDay(
    date: LocalDate,
    habits: List<Habit>,
    completedIds: Set<Long>,
    skippedIds: Set<Long> = emptySet(),
): DayScore {
    val rows = habits
        .filter { it.appliesOn(date) && it.id !in skippedIds }
        .sortedBy { it.sortOrder }
        .map { HabitDayRow(it, it.id in completedIds) }
    val scheduledWeight = rows.sumOf { it.habit.importance.weight }
    val completedWeight = rows.filter { it.completed }.sumOf { it.habit.importance.weight }
    val score = if (scheduledWeight == 0) {
        null
    } else {
        ((completedWeight * 100.0) / scheduledWeight).roundToInt()
    }
    return DayScore(
        date = date,
        score = score,
        scheduledCount = rows.size,
        completedCount = rows.count { it.completed },
        scheduledWeight = scheduledWeight,
        completedWeight = completedWeight,
        rows = rows,
    )
}

/**
 * Same result as [scoreDay] then [DayScore.kind], without allocating the day's rows.
 * Streaks only need the kind, and they walk every day since the first habit.
 */
fun dayKind(
    date: LocalDate,
    habits: List<Habit>,
    completedIds: Set<Long>,
    skippedIds: Set<Long> = emptySet(),
): DayKind {
    var scheduledWeight = 0
    var completedWeight = 0
    for (habit in habits) {
        if (!habit.appliesOn(date) || habit.id in skippedIds) continue
        val weight = habit.importance.weight
        scheduledWeight += weight
        if (habit.id in completedIds) completedWeight += weight
    }
    if (scheduledWeight == 0) return DayKind.OFF
    val score = ((completedWeight * 100.0) / scheduledWeight).roundToInt()
    return if (score >= DISCIPLINE_THRESHOLD) DayKind.DISCIPLINED else DayKind.MISSED
}

/** One score per date, shared by today, the week, the month, and insights. */
class DayLookup(
    private val habits: List<Habit>,
    private val byDate: Map<LocalDate, Set<Long>>,
    private val skippedByDate: Map<LocalDate, Set<Long>>,
) {
    private val cache = HashMap<LocalDate, DayScore>()

    fun score(date: LocalDate): DayScore = cache.getOrPut(date) {
        scoreDay(date, habits, byDate[date].orEmpty(), skippedByDate[date].orEmpty())
    }
}

/** One kind per date, shared by the current streak and the best streak. */
class DayKindCache(
    private val habits: List<Habit>,
    private val byDate: Map<LocalDate, Set<Long>>,
    private val skippedByDate: Map<LocalDate, Set<Long>>,
) {
    private val cache = HashMap<LocalDate, DayKind>()

    fun kind(date: LocalDate): DayKind = cache.getOrPut(date) {
        dayKind(date, habits, byDate[date].orEmpty(), skippedByDate[date].orEmpty())
    }
}

fun List<Completion>.groupedByDate(): Map<LocalDate, Set<Long>> =
    groupBy { it.date }.mapValues { (_, items) -> items.map { it.habitId }.toSet() }

fun List<Skip>.skipsByDate(): Map<LocalDate, Set<Long>> =
    groupBy { it.date }.mapValues { (_, items) -> items.map { it.habitId }.toSet() }

/**
 * The lightest habit that would cross 70, or the one that moves the score the most
 * when no single habit is enough.
 */
fun markGap(day: DayScore): MarkGap? {
    val score = day.score ?: return null
    val open = day.rows.filter { !it.completed }
    if (score >= DISCIPLINE_THRESHOLD) {
        return MarkGap(
            reached = true,
            remainingCount = open.size,
            nextHabitId = null,
            nextHabitName = null,
            nextAloneReaches = false,
        )
    }
    fun projected(row: HabitDayRow): Int {
        if (day.scheduledWeight == 0) return 0
        return ((day.completedWeight + row.habit.importance.weight) * 100.0 / day.scheduledWeight).roundToInt()
    }
    val easiest = open
        .filter { projected(it) >= DISCIPLINE_THRESHOLD }
        .minWithOrNull(compareBy({ it.habit.importance.weight }, { it.habit.sortOrder }))
    val next = easiest ?: open.maxWithOrNull(
        compareBy({ it.habit.importance.weight }, { -it.habit.sortOrder }),
    )
    return MarkGap(
        reached = false,
        remainingCount = open.size,
        nextHabitId = next?.habit?.id,
        nextHabitName = next?.habit?.name,
        nextAloneReaches = easiest != null,
    )
}

/**
 * Consecutive disciplined days ending at [asOf].
 * When [inProgress] is true, a day that is not yet at 70 does not break the streak —
 * the day is still open. Days with nothing scheduled are skipped.
 */
fun currentStreak(
    asOf: LocalDate,
    habits: List<Habit>,
    byDate: Map<LocalDate, Set<Long>>,
    inProgress: Boolean,
    skippedByDate: Map<LocalDate, Set<Long>> = emptyMap(),
    kinds: DayKindCache? = null,
): Int {
    val earliest = habits.minOfOrNull { it.createdOn } ?: return 0
    fun kind(date: LocalDate): DayKind = kinds?.kind(date) ?: dayKind(
        date,
        habits,
        byDate[date].orEmpty(),
        skippedByDate[date].orEmpty(),
    )

    var streak = 0
    var cursor = asOf
    if (inProgress) {
        if (kind(asOf) == DayKind.DISCIPLINED) streak = 1
        cursor = asOf.minusDays(1)
    }
    while (!cursor.isBefore(earliest)) {
        when (kind(cursor)) {
            DayKind.DISCIPLINED -> streak += 1
            DayKind.OFF -> Unit
            DayKind.MISSED -> return streak
        }
        cursor = cursor.minusDays(1)
    }
    return streak
}

/**
 * Longest run of disciplined days up to [asOf].
 * [asOf] is treated as still open: a score under 70 today does not erase a longer run.
 */
fun bestStreak(
    asOf: LocalDate,
    habits: List<Habit>,
    byDate: Map<LocalDate, Set<Long>>,
    skippedByDate: Map<LocalDate, Set<Long>> = emptyMap(),
    kinds: DayKindCache? = null,
): Int {
    val earliest = habits.minOfOrNull { it.createdOn } ?: return 0
    fun kind(date: LocalDate): DayKind = kinds?.kind(date) ?: dayKind(
        date,
        habits,
        byDate[date].orEmpty(),
        skippedByDate[date].orEmpty(),
    )

    var best = 0
    var run = 0
    var cursor = earliest
    val lastClosed = asOf.minusDays(1)
    while (!cursor.isAfter(lastClosed)) {
        when (kind(cursor)) {
            DayKind.DISCIPLINED -> {
                run += 1
                if (run > best) best = run
            }
            DayKind.OFF -> Unit
            DayKind.MISSED -> run = 0
        }
        cursor = cursor.plusDays(1)
    }
    if (!asOf.isBefore(earliest) && kind(asOf) == DayKind.DISCIPLINED) {
        run += 1
        if (run > best) best = run
    }
    return best
}

fun recoverySummary(
    habits: List<Habit>,
    today: LocalDate,
    byDate: Map<LocalDate, Set<Long>>,
    skippedByDate: Map<LocalDate, Set<Long>> = emptyMap(),
): RecoverySummary {
    val doneByHabit = HashMap<Long, HashSet<LocalDate>>()
    for ((date, ids) in byDate) {
        for (id in ids) {
            val dates = doneByHabit[id]
            if (dates == null) doneByHabit[id] = hashSetOf(date) else dates.add(date)
        }
    }
    val events = mutableListOf<RecoveryEvent>()
    val gaps = mutableListOf<OpenGap>()
    habits.forEach { habit ->
        val done = doneByHabit[habit.id].orEmpty()
        var gapStart: LocalDate? = null
        var date = habit.createdOn
        while (!date.isAfter(today)) {
            val skipped = habit.id in skippedByDate[date].orEmpty()
            if (habit.appliesOn(date) && !skipped) {
                if (date in done) {
                    val start = gapStart
                    if (start != null) {
                        events += RecoveryEvent(
                            habitId = habit.id,
                            habitName = habit.name,
                            missedOn = start,
                            returnedOn = date,
                            fast = isFastReturn(habit, start, date, skippedByDate),
                        )
                        gapStart = null
                    }
                } else if (date.isBefore(today) && gapStart == null) {
                    gapStart = date
                }
            }
            date = date.plusDays(1)
        }
        val openFrom = gapStart
        if (openFrom != null) {
            gaps += OpenGap(habit.id, habit.name, openFrom)
        }
    }
    val average = if (events.isEmpty()) {
        null
    } else {
        events.map { ChronoUnit.DAYS.between(it.missedOn, it.returnedOn).toDouble() }.average()
    }
    return RecoverySummary(events, gaps, average)
}

/** True when the return was the next scheduled chance, not merely the next calendar day. */
fun isFastReturn(
    habit: Habit,
    missedOn: LocalDate,
    returnedOn: LocalDate,
    skippedByDate: Map<LocalDate, Set<Long>> = emptyMap(),
): Boolean {
    var date = missedOn.plusDays(1)
    while (date.isBefore(returnedOn)) {
        val skipped = habit.id in skippedByDate[date].orEmpty()
        if (habit.appliesOn(date) && !skipped) return false
        date = date.plusDays(1)
    }
    return true
}

fun weekReport(
    end: LocalDate,
    habits: List<Habit>,
    byDate: Map<LocalDate, Set<Long>>,
    skippedByDate: Map<LocalDate, Set<Long>> = emptyMap(),
    lookup: DayLookup? = null,
): WeekReport {
    val start = end.minusDays(6)
    val days = (0L..6L).map { offset ->
        val date = start.plusDays(offset)
        lookup?.score(date) ?: scoreDay(date, habits, byDate[date].orEmpty(), skippedByDate[date].orEmpty())
    }
    val scores = days.mapNotNull { it.score }
    val rates = habits.mapNotNull { habit ->
        val instances = days.filter { day -> day.rows.any { it.habit.id == habit.id } }
        if (instances.isEmpty()) {
            null
        } else {
            val completed = instances.count { day ->
                day.rows.any { it.habit.id == habit.id && it.completed }
            }
            HabitRate(
                habitId = habit.id,
                name = habit.name,
                percent = ((completed * 100.0) / instances.size).roundToInt(),
                completed = completed,
                scheduled = instances.size,
            )
        }
    }.sortedWith(compareBy({ it.percent }, { it.name }))
    return WeekReport(
        start = start,
        end = end,
        days = days,
        averageScore = if (scores.isEmpty()) null else scores.average().roundToInt(),
        scheduledInstances = days.sumOf { it.scheduledCount },
        completedInstances = days.sumOf { it.completedCount },
        disciplinedDays = days.count { it.kind() == DayKind.DISCIPLINED },
        scoredDays = days.count { it.score != null },
        habitRates = rates,
    )
}

/**
 * One cell per day in [month]. Days after [today] stay unmarked so an unfinished
 * future does not look like a miss. Days before a habit exists stay unscored.
 */
fun monthProgress(
    month: YearMonth,
    today: LocalDate,
    habits: List<Habit>,
    byDate: Map<LocalDate, Set<Long>>,
    skippedByDate: Map<LocalDate, Set<Long>> = emptyMap(),
    lookup: DayLookup? = null,
): MonthProgress {
    val days = (1..month.lengthOfMonth()).map { dayNumber ->
        val date = month.atDay(dayNumber)
        if (date.isAfter(today)) {
            MonthDay(date, future = true, score = null)
        } else {
            MonthDay(
                date,
                future = false,
                score = lookup?.score(date)
                    ?: scoreDay(date, habits, byDate[date].orEmpty(), skippedByDate[date].orEmpty()),
            )
        }
    }
    val scored = days.mapNotNull { day -> day.score?.takeIf { it.score != null } }
    val best = scored.maxWithOrNull(compareBy({ it.score }, { it.date }))
    return MonthProgress(
        month = month,
        days = days,
        averageScore = if (scored.isEmpty()) null else scored.map { it.score!! }.average().roundToInt(),
        disciplinedDays = scored.count { it.kind() == DayKind.DISCIPLINED },
        scoredDays = scored.size,
        scheduledInstances = days.sumOf { it.score?.scheduledCount ?: 0 },
        completedInstances = days.sumOf { it.score?.completedCount ?: 0 },
        bestDate = best?.date,
        bestScore = best?.score,
        previousAverage = monthAverage(month.minusMonths(1), today, habits, byDate, skippedByDate),
    )
}

private fun monthAverage(
    month: YearMonth,
    today: LocalDate,
    habits: List<Habit>,
    byDate: Map<LocalDate, Set<Long>>,
    skippedByDate: Map<LocalDate, Set<Long>>,
): Int? {
    if (month.atDay(1).isAfter(today)) return null
    val scores = (1..month.lengthOfMonth()).mapNotNull { dayNumber ->
        val date = month.atDay(dayNumber)
        if (date.isAfter(today)) {
            null
        } else {
            scoreDay(date, habits, byDate[date].orEmpty(), skippedByDate[date].orEmpty()).score
        }
    }
    return if (scores.isEmpty()) null else scores.average().roundToInt()
}

/** Next wall-clock reminder strictly after [after], within the next three weeks. */
fun nextReminder(habit: Habit, after: LocalDateTime): LocalDateTime? {
    val minutes = habit.reminderMinutes ?: return null
    if (habit.days.isEmpty()) return null
    val clamped = minutes.coerceIn(0, 23 * 60 + 59)
    val time = LocalTime.of(clamped / 60, clamped % 60)
    var date = after.toLocalDate()
    if (date.isBefore(habit.createdOn)) date = habit.createdOn
    repeat(21) {
        if (!date.isBefore(habit.createdOn) && date.dayOfWeek in habit.days) {
            val trigger = LocalDateTime.of(date, time)
            if (trigger.isAfter(after)) return trigger
        }
        date = date.plusDays(1)
    }
    return null
}
