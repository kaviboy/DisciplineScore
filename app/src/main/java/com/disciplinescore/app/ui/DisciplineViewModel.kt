package com.disciplinescore.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.disciplinescore.app.DisciplineScoreApp
import com.disciplinescore.app.data.UserPreferences
import com.disciplinescore.app.domain.Habit
import com.disciplinescore.app.domain.HabitCategory
import com.disciplinescore.app.domain.Importance
import com.disciplinescore.app.ui.habits.HabitDraft
import com.disciplinescore.app.ui.profile.ProfileDraft
import com.disciplinescore.app.ui.habits.HabitSuggestion
import com.disciplinescore.app.ui.habits.toHabit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

sealed interface UndoAction {
    val message: String

    data class Toggle(
        val habitId: Long,
        val date: LocalDate,
        override val message: String,
    ) : UndoAction

    data class SkipChange(
        val habitId: Long,
        val date: LocalDate,
        val skipped: Boolean,
        override val message: String,
    ) : UndoAction
}

class DisciplineViewModel(app: Application) : AndroidViewModel(app) {
    private val application = app as DisciplineScoreApp
    private val repository = application.repository
    private val scheduler = application.scheduler
    private val preferencesStore = application.preferences
    val preferences = preferencesStore.state
    private val selectedDate = MutableStateFlow(LocalDate.now())
    private val clock = MutableStateFlow(LocalDate.now())
    private val visibleMonth = MutableStateFlow(YearMonth.now())
    private val calendarDay = MutableStateFlow(LocalDate.now())
    private val weekEnd = MutableStateFlow(LocalDate.now())
    private val snapshot = MutableStateFlow(
        runBlocking(Dispatchers.IO) { repository.current() },
    )
    private val _undo = MutableSharedFlow<UndoAction>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val undo: SharedFlow<UndoAction> = _undo.asSharedFlow()

    val ui: StateFlow<AppUi> = combine(
        snapshot,
        selectedDate,
        clock,
        combine(visibleMonth, calendarDay, weekEnd) { month, focused, week ->
            Triple(month, focused, week)
        },
    ) { snap, selected, today, extra ->
        present(snap, selected, today, extra.first, extra.second, extra.third)
    }
        .flowOn(Dispatchers.Default)
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            present(
                snapshot.value,
                selectedDate.value,
                clock.value,
                visibleMonth.value,
                calendarDay.value,
                weekEnd.value,
            ),
        )

    init {
        viewModelScope.launch {
            repository.observe()
                .distinctUntilChanged()
                .flowOn(Dispatchers.IO)
                .collect { snapshot.value = it }
        }
    }

    fun onResume() {
        val now = LocalDate.now()
        val previous = clock.value
        clock.value = now
        if (now == previous) return
        if (selectedDate.value == previous) selectedDate.value = now
        if (weekEnd.value == previous) weekEnd.value = now
        if (calendarDay.value == previous) {
            calendarDay.value = now
            visibleMonth.value = YearMonth.from(now)
        }
    }

    fun shiftDay(days: Int) {
        val next = selectedDate.value.plusDays(days.toLong())
        if (!next.isAfter(clock.value)) selectedDate.value = next
    }

    fun jumpToToday() {
        selectedDate.value = clock.value
        visibleMonth.value = YearMonth.from(clock.value)
        calendarDay.value = clock.value
        weekEnd.value = clock.value
    }

    fun shiftWeek(weeks: Int) {
        val next = weekEnd.value.plusDays(weeks * 7L)
        if (next.isAfter(clock.value)) return
        val earliest = snapshot.value.habits.minOfOrNull { it.createdOn } ?: return
        if (next.isBefore(earliest)) return
        weekEnd.value = next
    }

    fun jumpToThisWeek() {
        weekEnd.value = clock.value
    }

    fun jumpToThisMonth() {
        visibleMonth.value = YearMonth.from(clock.value)
        calendarDay.value = clock.value
    }

    fun shiftMonth(months: Int) {
        val next = visibleMonth.value.plusMonths(months.toLong())
        val current = YearMonth.from(clock.value)
        if (next.isAfter(current)) return
        val earliest = earliestVisibleMonth(clock.value, snapshot.value.habits.map { it.createdOn })
        if (next.isBefore(earliest)) return
        visibleMonth.value = next
        val focused = calendarDay.value
        if (YearMonth.from(focused) != next) {
            calendarDay.value = if (next == current) clock.value else next.atEndOfMonth()
        }
    }

    fun selectCalendarDay(date: LocalDate) {
        if (date.isAfter(clock.value)) return
        calendarDay.value = date
        visibleMonth.value = YearMonth.from(date)
    }

    fun showCalendarDay() {
        val date = calendarDay.value
        if (!date.isAfter(clock.value)) selectedDate.value = date
    }

    fun toggleCalendarHabit(habitId: Long) {
        toggleOn(habitId, calendarDay.value)
    }

    fun toggle(habitId: Long) {
        toggleOn(habitId, selectedDate.value)
    }

    fun toggleOn(habitId: Long, date: LocalDate, announce: Boolean = true) {
        if (date.isAfter(clock.value)) return
        val habit = snapshot.value.habits.find { it.id == habitId } ?: return
        val wasDone = snapshot.value.completions.any { it.habitId == habitId && it.date == date }
        viewModelScope.launch {
            repository.toggle(habitId, date)
            if (announce) {
                _undo.emit(
                    UndoAction.Toggle(
                        habitId = habitId,
                        date = date,
                        message = if (wasDone) "Unchecked ${habit.name}" else "Checked ${habit.name}",
                    ),
                )
            }
        }
    }

    fun skipHabit(habitId: Long) = setSkipped(habitId, selectedDate.value, skipped = true)

    fun restoreHabit(habitId: Long) = setSkipped(habitId, selectedDate.value, skipped = false)

    fun skipCalendarHabit(habitId: Long) = setSkipped(habitId, calendarDay.value, skipped = true)

    fun restoreCalendarHabit(habitId: Long) = setSkipped(habitId, calendarDay.value, skipped = false)

    fun setSkipped(habitId: Long, date: LocalDate, skipped: Boolean, announce: Boolean = true) {
        if (date.isAfter(clock.value)) return
        val habit = snapshot.value.habits.find { it.id == habitId } ?: return
        viewModelScope.launch {
            if (skipped) repository.skip(habitId, date) else repository.unskip(habitId, date)
            if (announce) {
                _undo.emit(
                    UndoAction.SkipChange(
                        habitId = habitId,
                        date = date,
                        skipped = skipped,
                        message = if (skipped) "Set ${habit.name} aside" else "Put ${habit.name} back",
                    ),
                )
            }
        }
    }

    fun performUndo(action: UndoAction) {
        when (action) {
            is UndoAction.Toggle -> toggleOn(action.habitId, action.date, announce = false)
            is UndoAction.SkipChange -> setSkipped(
                action.habitId,
                action.date,
                skipped = !action.skipped,
                announce = false,
            )
        }
    }

    fun moveHabit(id: Long, direction: Int) {
        viewModelScope.launch { repository.move(id, direction) }
    }

    fun addSuggestion(suggestion: HabitSuggestion) {
        val name = suggestion.name.trim()
        if (name.isEmpty()) return
        if (snapshot.value.habits.any { it.name.equals(name, ignoreCase = true) }) return
        viewModelScope.launch {
            repository.upsert(
                Habit(
                    id = 0,
                    name = name,
                    importance = suggestion.importance,
                    category = suggestion.category,
                    days = DayOfWeek.values().toSet(),
                    reminderMinutes = null,
                    sortOrder = 0,
                    createdOn = clock.value,
                    note = suggestion.note,
                ),
            )
            scheduler.rescheduleAll()
        }
    }

    fun newDraft(): HabitDraft = HabitDraft(
        id = 0,
        name = "",
        importance = Importance.STANDARD,
        category = HabitCategory.BODY,
        days = DayOfWeek.values().toSet(),
        reminderEnabled = false,
        reminderMinutes = preferencesStore.state.value.defaultReminderMinutes,
        createdOn = clock.value,
    )

    fun updatePreferences(transform: (UserPreferences) -> UserPreferences) {
        preferencesStore.update(transform)
    }

    fun generateNextWorkout() {
        updatePreferences {
            it.copy(
                customWorkouts = emptyMap(),
                workoutStartedOn = LocalDate.now(),
                workoutBlock = it.workoutBlock + 1,
            )
        }
    }

    fun saveProfile(draft: ProfileDraft) {
        updatePreferences {
            it.copy(
                displayName = draft.name,
                birthDate = draft.birthDate,
                sex = draft.sex,
                measureSystem = draft.measureSystem,
                heightCm = draft.heightCm,
                weightKg = draft.weightKg,
                scheduleLength = draft.scheduleLength,
                workoutStartedOn = it.workoutStartedOn ?: LocalDate.now(),
                profileComplete = true,
            )
        }
    }

    fun draftFor(id: Long): HabitDraft? =
        snapshot.value.habits.find { it.id == id }?.let { habit ->
            HabitDraft(
                id = habit.id,
                name = habit.name,
                importance = habit.importance,
                category = habit.category,
                days = habit.days,
                reminderEnabled = habit.reminderMinutes != null,
                reminderMinutes = habit.reminderMinutes ?: preferencesStore.state.value.defaultReminderMinutes,
                createdOn = habit.createdOn,
                note = habit.note,
            )
        }

    fun save(draft: HabitDraft) {
        viewModelScope.launch {
            repository.upsert(draft.toHabit())
            scheduler.rescheduleAll()
        }
    }

    fun deleteHabit(id: Long) {
        viewModelScope.launch {
            repository.delete(id)
            scheduler.rescheduleAll()
        }
    }
}
