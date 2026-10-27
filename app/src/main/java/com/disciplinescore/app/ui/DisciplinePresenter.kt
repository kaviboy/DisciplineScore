package com.disciplinescore.app.ui

import com.disciplinescore.app.domain.DISCIPLINE_THRESHOLD
import com.disciplinescore.app.domain.DayKind
import com.disciplinescore.app.domain.DayKindCache
import com.disciplinescore.app.domain.DayLookup
import com.disciplinescore.app.domain.DisciplineSnapshot
import com.disciplinescore.app.domain.Habit
import com.disciplinescore.app.domain.HabitCategory
import com.disciplinescore.app.domain.Insight
import com.disciplinescore.app.domain.MonthDay
import com.disciplinescore.app.domain.MonthProgress
import com.disciplinescore.app.domain.RecoverySummary
import com.disciplinescore.app.domain.WeekReport
import com.disciplinescore.app.domain.appliesOn
import com.disciplinescore.app.domain.bestStreak
import com.disciplinescore.app.domain.buildInsights
import com.disciplinescore.app.domain.currentStreak
import com.disciplinescore.app.domain.groupedByDate
import com.disciplinescore.app.domain.kind
import com.disciplinescore.app.domain.markGap
import com.disciplinescore.app.domain.monthProgress
import com.disciplinescore.app.domain.recoverySummary
import com.disciplinescore.app.domain.skipsByDate
import com.disciplinescore.app.domain.weekReport
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.roundToInt

data class AppUi(
    val today: TodayUi,
    val calendar: CalendarUi,
    val week: WeekUi,
    val insights: InsightsUi,
    val habitList: List<HabitListItem>,
)

data class TodayUi(
    val heading: String,
    val dateLabel: String,
    val isToday: Boolean,
    val canGoForward: Boolean,
    val score: Int?,
    val progressLine: String,
    val markHint: String?,
    val listHeading: String,
    val rows: List<TodayHabitUi>,
    val skipped: List<SkippedHabitUi>,
    val streakValue: String,
    val streakCaption: String,
    val recoveryValue: String,
    val recoveryCaption: String,
    val banner: String?,
    val principle: String,
    val emptyMessage: String?,
    val weekdayIndex: Int,
)

data class TodayHabitUi(
    val id: Long,
    val name: String,
    val meta: String,
    val note: String,
    val completed: Boolean,
    val category: HabitCategory,
    val focusLabel: String?,
)

data class SkippedHabitUi(
    val id: Long,
    val name: String,
    val category: HabitCategory,
)

data class WeekUi(
    val title: String,
    val rangeLabel: String,
    val canGoBack: Boolean,
    val canGoForward: Boolean,
    val isCurrent: Boolean,
    val averageScore: Int?,
    val summary: String,
    val completionLine: String?,
    val days: List<WeekDayUi>,
    val habits: List<HabitRateUi>,
    val empty: Boolean,
)

data class WeekDayUi(
    val label: String,
    val score: Int?,
    val scheduled: Boolean,
    val today: Boolean,
)

data class HabitRateUi(
    val name: String,
    val percent: Int,
    val detail: String,
    val weakest: Boolean,
)

data class InsightsUi(
    val cards: List<InsightCardUi>,
)

enum class InsightTone { CALM, FOCUS, STEADY }

data class InsightCardUi(
    val title: String,
    val body: String,
    val tone: InsightTone,
)

enum class CalendarKind { BLANK, FUTURE, OFF, OPEN, MET, MISSED }

data class CalendarCellUi(
    val date: LocalDate?,
    val dayNumber: String,
    val scoreLabel: String?,
    val kind: CalendarKind,
    val selected: Boolean,
    val today: Boolean,
)

data class CalendarDayUi(
    val heading: String,
    val scoreLabel: String,
    val progress: String,
    val rows: List<TodayHabitUi>,
    val skipped: List<SkippedHabitUi>,
    val emptyMessage: String?,
)

data class CalendarUi(
    val title: String,
    val canGoBack: Boolean,
    val canGoForward: Boolean,
    val weekdayLabels: List<String>,
    val cells: List<CalendarCellUi>,
    val averageLabel: String,
    val disciplinedLine: String,
    val completionLine: String?,
    val bestLine: String?,
    val bestDate: LocalDate?,
    val previousLine: String?,
    val selected: CalendarDayUi?,
)

data class HabitListItem(
    val id: Long,
    val name: String,
    val meta: String,
    val note: String,
    val category: HabitCategory,
)

fun present(
    snapshot: DisciplineSnapshot,
    selected: LocalDate,
    today: LocalDate,
    month: YearMonth = YearMonth.from(today),
    calendarDay: LocalDate = today,
    weekEnd: LocalDate = today,
): AppUi {
    val habits = snapshot.habits.sortedBy { it.sortOrder }
    val byDate = snapshot.completions.groupedByDate()
    val skippedByDate = snapshot.skips.skipsByDate()
    val windowEnd = if (weekEnd.isAfter(today)) today else weekEnd
    val lookup = DayLookup(habits, byDate, skippedByDate)
    val kinds = DayKindCache(habits, byDate, skippedByDate)
    val day = lookup.score(selected)
    val recovery = recoverySummary(habits, today, byDate, skippedByDate)
    val streak = currentStreak(
        selected,
        habits,
        byDate,
        inProgress = selected == today,
        skippedByDate = skippedByDate,
        kinds = kinds,
    )
    val best = bestStreak(today, habits, byDate, skippedByDate, kinds)
    val week = weekReport(windowEnd, habits, byDate, skippedByDate, lookup)
    val insights = buildInsights(habits, byDate, today, skippedByDate, lookup = lookup)
    val monthReport = monthProgress(month, today, habits, byDate, skippedByDate, lookup)
    return AppUi(
        today = todayUi(selected, today, habits, day, streak, best, recovery, skippedByDate[selected].orEmpty()),
        calendar = calendarUi(monthReport, today, habits, calendarDay, skippedByDate),
        week = weekUi(week, today, habits),
        insights = insightsUi(insights.insights),
        habitList = habits.map { it.toListItem() },
    )
}

private fun todayUi(
    selected: LocalDate,
    today: LocalDate,
    habits: List<Habit>,
    day: com.disciplinescore.app.domain.DayScore,
    streak: Int,
    best: Int,
    recovery: RecoverySummary,
    skippedIds: Set<Long>,
): TodayUi {
    val weighted = day.rows.map { it.habit.importance }.distinct().size > 1
    val progressLine = when {
        day.score == null -> "Nothing scheduled"
        weighted -> "${day.completedCount} of ${day.scheduledCount} done · weighted by importance"
        else -> "${day.completedCount} of ${day.scheduledCount} done"
    }
    val gap = markGap(day)
    val skipped = habits
        .filter { it.appliesOn(selected) && it.id in skippedIds }
        .sortedBy { it.sortOrder }
        .map { SkippedHabitUi(it.id, it.name, it.category) }
    val emptyMessage = when {
        habits.isEmpty() -> "Add a habit to start scoring the day."
        day.rows.isEmpty() && skipped.isNotEmpty() ->
            "Everything scheduled is set aside, so this day stays quiet."
        day.rows.isEmpty() && habits.all { selected.isBefore(it.createdOn) } ->
            "These habits start later, so this day is not scored."
        day.rows.isEmpty() -> "Nothing is scheduled for this day."
        else -> null
    }
    val averageReturn = recovery.averageDaysAway
    val recoveryValue = when {
        averageReturn != null -> formatAverageDays(averageReturn)
        recovery.openGaps.isNotEmpty() -> "Open"
        else -> "None"
    }
    val recoveryCaption = when {
        averageReturn != null -> "average return"
        recovery.openGaps.isNotEmpty() -> "waiting on you"
        else -> "no misses yet"
    }
    val streakCaption = buildString {
        append(if (streak == 1) "day at $DISCIPLINE_THRESHOLD+" else "days at $DISCIPLINE_THRESHOLD+")
        when {
            streak > 0 && best == streak -> append(" · personal best")
            best > streak -> append(" · best $best")
        }
    }
    return TodayUi(
        heading = dayHeading(selected, today),
        dateLabel = formatFullDate(selected),
        isToday = selected == today,
        canGoForward = selected.isBefore(today),
        score = day.score,
        progressLine = progressLine,
        markHint = markHint(gap),
        listHeading = if (selected == today) {
            "Today's habits"
        } else {
            "${selected.dayOfWeek.plural().dropLast(1)}'s habits"
        },
        rows = day.rows.map { row ->
            TodayHabitUi(
                id = row.habit.id,
                name = row.habit.name,
                meta = "${row.habit.category.label} · ${row.habit.importance.label}",
                note = row.habit.note,
                completed = row.completed,
                category = row.habit.category,
                focusLabel = focusLabel(row.habit.id, gap),
            )
        },
        skipped = skipped,
        streakValue = streak.toString(),
        streakCaption = streakCaption,
        recoveryValue = recoveryValue,
        recoveryCaption = recoveryCaption,
        banner = if (selected == today) recoveryBanner(recovery, today) else null,
        principle = "A miss pauses the streak. Coming back is a recovery, not a reset.",
        emptyMessage = emptyMessage,
        weekdayIndex = selected.dayOfWeek.value - 1,
    )
}

private fun markHint(gap: com.disciplinescore.app.domain.MarkGap?): String? {
    gap ?: return null
    return when {
        gap.reached && gap.remainingCount == 0 -> "Every habit is checked. This day counts."
        gap.reached -> "You made the mark. Anything still open is a bonus."
        gap.nextAloneReaches && gap.nextHabitName != null ->
            "Check ${gap.nextHabitName} and this day counts."
        gap.nextHabitName != null && gap.remainingCount == 1 ->
            "${gap.nextHabitName} is still open."
        gap.nextHabitName != null ->
            "${gap.remainingCount} still open. ${gap.nextHabitName} moves the score the most."
        else -> "This day is still under $DISCIPLINE_THRESHOLD."
    }
}

private fun focusLabel(habitId: Long, gap: com.disciplinescore.app.domain.MarkGap?): String? {
    if (gap == null || gap.reached || gap.nextHabitId != habitId) return null
    return if (gap.nextAloneReaches) "Gets you to $DISCIPLINE_THRESHOLD" else "Biggest lift"
}

private fun recoveryBanner(summary: RecoverySummary, today: LocalDate): String? {
    val returned = summary.events.filter { it.returnedOn == today }
    if (returned.isNotEmpty()) {
        val names = joinNames(returned.map { it.habitName }.distinct())
        return "Back today on $names. That return is recorded, and the miss does not reset your discipline."
    }
    if (summary.openGaps.isNotEmpty()) {
        val names = joinNames(summary.openGaps.map { it.habitName }.distinct())
        return "$names can still be a recovery. Come back and the miss stays a pause."
    }
    return null
}

private val weekdayLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

fun earliestVisibleMonth(today: LocalDate, habitStarts: List<LocalDate>): YearMonth {
    val window = YearMonth.from(today).minusYears(5)
    val created = habitStarts.minOfOrNull { YearMonth.from(it) }
    return if (created != null && created.isBefore(window)) created else window
}

private fun calendarUi(
    month: MonthProgress,
    today: LocalDate,
    habits: List<Habit>,
    calendarDay: LocalDate,
    skippedByDate: Map<LocalDate, Set<Long>>,
): CalendarUi {
    val focus = focusDay(month, today, calendarDay)
    val lead = month.month.atDay(1).dayOfWeek.value - 1
    val tail = (7 - ((lead + month.days.size) % 7)) % 7
    val cells = List(lead) { blankCell() } +
        month.days.map { day -> day.toCell(today, focus) } +
        List(tail) { blankCell() }
    val completion = if (month.scheduledInstances == 0) {
        null
    } else {
        val percent = ((month.completedInstances * 100.0) / month.scheduledInstances).roundToInt()
        "${month.completedInstances} of ${month.scheduledInstances} habits checked off · $percent%"
    }
    val earliest = earliestVisibleMonth(today, habits.map { it.createdOn })
    return CalendarUi(
        title = formatMonth(month.month),
        canGoBack = month.month.isAfter(earliest),
        canGoForward = month.month.isBefore(YearMonth.from(today)),
        weekdayLabels = weekdayLabels,
        cells = cells,
        averageLabel = month.averageScore?.toString() ?: "–",
        disciplinedLine = if (month.scoredDays == 0) {
            "No scored days yet this month."
        } else {
            "${month.disciplinedDays} of ${month.scoredDays} scored days held $DISCIPLINE_THRESHOLD or better."
        },
        completionLine = completion,
        bestLine = month.bestDate?.let { date ->
            month.bestScore?.let { score -> "Best day ${formatFullDate(date)} · $score" }
        },
        bestDate = month.bestDate,
        previousLine = month.previousAverage?.let { average ->
            "${formatMonth(month.month.minusMonths(1))} averaged $average."
        },
        selected = focus?.let { date ->
            val day = month.days.first { it.date == date }
            val scored = day.score
            val rows = scored?.rows.orEmpty()
            val skippedIds = skippedByDate[date].orEmpty()
            val skipped = habits
                .filter { it.appliesOn(date) && it.id in skippedIds }
                .sortedBy { it.sortOrder }
                .map { SkippedHabitUi(it.id, it.name, it.category) }
            val gap = scored?.let { markGap(it) }
            CalendarDayUi(
                heading = formatFullDate(date),
                scoreLabel = scored?.score?.toString() ?: "–",
                progress = when {
                    scored == null || scored.score == null -> "Nothing scheduled"
                    else -> "${scored.completedCount} of ${scored.scheduledCount} done"
                },
                rows = rows.map { row ->
                    TodayHabitUi(
                        id = row.habit.id,
                        name = row.habit.name,
                        meta = "${row.habit.category.label} · ${row.habit.importance.label}",
                        note = row.habit.note,
                        completed = row.completed,
                        category = row.habit.category,
                        focusLabel = focusLabel(row.habit.id, gap),
                    )
                },
                skipped = skipped,
                emptyMessage = when {
                    habits.isEmpty() -> "Add a habit to start filling the month."
                    rows.isEmpty() && skipped.isNotEmpty() ->
                        "Everything that day was set aside, so it stays quiet."
                    rows.isEmpty() -> "Nothing was scheduled, so this day stays quiet."
                    else -> null
                },
            )
        },
    )
}

private fun focusDay(month: MonthProgress, today: LocalDate, calendarDay: LocalDate): LocalDate? {
    val inMonth = !calendarDay.isAfter(today) && YearMonth.from(calendarDay) == month.month
    if (inMonth) return calendarDay
    return when {
        month.month == YearMonth.from(today) -> today
        month.month.isAfter(YearMonth.from(today)) -> null
        else -> month.month.atEndOfMonth()
    }
}

private fun blankCell() = CalendarCellUi(
    date = null,
    dayNumber = "",
    scoreLabel = null,
    kind = CalendarKind.BLANK,
    selected = false,
    today = false,
)

private fun MonthDay.toCell(today: LocalDate, focus: LocalDate?): CalendarCellUi {
    val value = score?.score
    val kind = when {
        future -> CalendarKind.FUTURE
        value == null -> CalendarKind.OFF
        date == today && value < DISCIPLINE_THRESHOLD -> CalendarKind.OPEN
        score?.kind() == DayKind.DISCIPLINED -> CalendarKind.MET
        else -> CalendarKind.MISSED
    }
    return CalendarCellUi(
        date = date,
        dayNumber = date.dayOfMonth.toString(),
        scoreLabel = value?.toString(),
        kind = kind,
        selected = date == focus,
        today = date == today,
    )
}

private fun weekUi(week: WeekReport, today: LocalDate, habits: List<Habit>): WeekUi {
    val percents = week.habitRates.map { it.percent }.distinct()
    val weakestPercent = week.habitRates.minOfOrNull { it.percent }
    val flagWeakest = percents.size > 1
    val earliest = habits.minOfOrNull { it.createdOn }
    return WeekUi(
        title = if (week.end == today) "This week" else "Earlier week",
        rangeLabel = formatRange(week.start, week.end),
        canGoBack = earliest != null && week.end.isAfter(earliest),
        canGoForward = week.end.isBefore(today),
        isCurrent = week.end == today,
        averageScore = week.averageScore,
        summary = if (week.scoredDays == 0) {
            "This stretch has no scored days yet. Scores start the day a habit is scheduled."
        } else {
            "${week.disciplinedDays} of ${week.scoredDays} scored days held $DISCIPLINE_THRESHOLD or better."
        },
        completionLine = week.completionPercent?.let { percent ->
            "${week.completedInstances} of ${week.scheduledInstances} habits checked off · $percent%"
        },
        days = week.days.map { day ->
            WeekDayUi(
                label = day.date.dayOfWeek.shortLabel(),
                score = day.score,
                scheduled = day.score != null,
                today = day.date == today,
            )
        },
        habits = week.habitRates.map { rate ->
            HabitRateUi(
                name = rate.name,
                percent = rate.percent,
                detail = "${rate.completed} of ${rate.scheduled}",
                weakest = flagWeakest && rate.percent < 100 && rate.percent == weakestPercent,
            )
        },
        empty = week.scoredDays == 0,
    )
}

private fun insightsUi(insights: List<Insight>): InsightsUi =
    InsightsUi(cards = insights.map { it.toCard() })

private fun Insight.toCard(): InsightCardUi = when (this) {
    is Insight.NeedsMoreDays -> InsightCardUi(
        title = "Patterns need a week",
        body = "$daysTracked of $required scored days so far. Insights look for the habits and weekdays that slip, including busy workdays. Today stays out of this, so an unfinished day does not look like a miss.",
        tone = InsightTone.CALM,
    )
    is Insight.WeakHabit -> InsightCardUi(
        title = "$name slips the most",
        body = "$percent% complete across $scheduled scheduled days. The daily score weighs this every time it comes due.",
        tone = InsightTone.FOCUS,
    )
    is Insight.WeakWeekday -> InsightCardUi(
        title = "${day.plural()} are the weak spot",
        body = "You most often miss $habitName on ${day.plural()} — $percent% those days.",
        tone = InsightTone.FOCUS,
    )
    is Insight.WeekdayGap -> InsightCardUi(
        title = if (workdayAverage < weekendAverage) "Workdays run lower" else "Weekends run lower",
        body = "Workdays average $workdayAverage. Weekends average $weekendAverage. The gap is the schedule, not a ruined streak.",
        tone = InsightTone.FOCUS,
    )
    Insight.Holding -> InsightCardUi(
        title = "Nothing is slipping",
        body = "Completion is holding across the last eight weeks. Recovery still matters the next time a day goes sideways.",
        tone = InsightTone.STEADY,
    )
}

private fun Habit.toListItem(): HabitListItem {
    val time = reminderMinutes?.let { formatMinutes(it) } ?: "No reminder"
    return HabitListItem(
        id = id,
        name = name,
        meta = "${formatDays(days)} · $time · ${importance.label}",
        note = note,
        category = category,
    )
}
