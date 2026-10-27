package com.disciplinescore.app.ui

import java.time.DayOfWeek as JavaDayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val fullDate: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE, MMM d")
private val monthDay: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d")
private val dayOfMonth: DateTimeFormatter = DateTimeFormatter.ofPattern("d")
private val monthName: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM")

private val monthYear: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")
private val birthDateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

fun formatFullDate(date: LocalDate): String = date.format(fullDate)

fun formatBirthDate(date: LocalDate): String = date.format(birthDateFormat)

fun formatMonth(month: YearMonth): String = month.format(monthYear)

fun formatRange(start: LocalDate, end: LocalDate): String =
    if (start.month == end.month && start.year == end.year) {
        "${start.format(monthName)} ${start.format(dayOfMonth)} – ${end.format(dayOfMonth)}"
    } else {
        "${start.format(monthDay)} – ${end.format(monthDay)}"
    }

fun dayHeading(date: LocalDate, today: LocalDate): String =
    if (date == today) "Today's discipline" else "${date.format(DateTimeFormatter.ofPattern("EEEE"))}'s discipline"

fun JavaDayOfWeek.shortLabel(): String = when (this) {
    JavaDayOfWeek.MONDAY -> "Mon"
    JavaDayOfWeek.TUESDAY -> "Tue"
    JavaDayOfWeek.WEDNESDAY -> "Wed"
    JavaDayOfWeek.THURSDAY -> "Thu"
    JavaDayOfWeek.FRIDAY -> "Fri"
    JavaDayOfWeek.SATURDAY -> "Sat"
    JavaDayOfWeek.SUNDAY -> "Sun"
}

fun JavaDayOfWeek.plural(): String =
    name.lowercase().replaceFirstChar { it.titlecase(Locale.getDefault()) } + "s"

fun formatDays(days: Set<JavaDayOfWeek>): String {
    val all = JavaDayOfWeek.values().toSet()
    val weekdays = setOf(
        JavaDayOfWeek.MONDAY,
        JavaDayOfWeek.TUESDAY,
        JavaDayOfWeek.WEDNESDAY,
        JavaDayOfWeek.THURSDAY,
        JavaDayOfWeek.FRIDAY,
    )
    val weekend = setOf(JavaDayOfWeek.SATURDAY, JavaDayOfWeek.SUNDAY)
    return when (days) {
        all -> "Every day"
        weekdays -> "Weekdays"
        weekend -> "Weekends"
        else -> days.sortedBy { it.value }.joinToString(" ") { it.shortLabel() }
    }
}

fun formatMinutes(minutes: Int): String {
    val normalized = ((minutes % (24 * 60)) + (24 * 60)) % (24 * 60)
    val hour24 = normalized / 60
    val minute = normalized % 60
    val suffix = if (hour24 < 12) "AM" else "PM"
    val hour12 = if (hour24 % 12 == 0) 12 else hour24 % 12
    return "%d:%02d %s".format(hour12, minute, suffix)
}

fun formatAverageDays(average: Double): String =
    if (average < 1.05) "1 day" else String.format(Locale.getDefault(), "%.1f days", average)

fun joinNames(names: List<String>): String = when (names.size) {
    0 -> ""
    1 -> names[0]
    2 -> "${names[0]} and ${names[1]}"
    else -> names.dropLast(1).joinToString(", ") + ", and " + names.last()
}
