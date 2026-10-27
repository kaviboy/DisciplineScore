package com.disciplinescore.app.domain

import java.time.LocalDate

enum class ScheduleLength(val label: String) {
    FOUR_WEEKS("4 weeks"),
    SIX_WEEKS("6 weeks"),
    ONE_MONTH("1 month"),
    TWO_MONTHS("2 months"),
    THREE_MONTHS("3 months"),
}

fun blockEnd(start: LocalDate, length: ScheduleLength): LocalDate = when (length) {
    ScheduleLength.FOUR_WEEKS -> start.plusWeeks(4)
    ScheduleLength.SIX_WEEKS -> start.plusWeeks(6)
    ScheduleLength.ONE_MONTH -> start.plusMonths(1)
    ScheduleLength.TWO_MONTHS -> start.plusMonths(2)
    ScheduleLength.THREE_MONTHS -> start.plusMonths(3)
}

fun blockIsDue(start: LocalDate, length: ScheduleLength, today: LocalDate): Boolean =
    !today.isBefore(blockEnd(start, length))
