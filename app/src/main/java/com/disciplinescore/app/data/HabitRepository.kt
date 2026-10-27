package com.disciplinescore.app.data

import com.disciplinescore.app.data.local.HabitDao
import com.disciplinescore.app.data.local.HabitLogEntity
import com.disciplinescore.app.data.local.HabitSkipEntity
import com.disciplinescore.app.domain.Completion
import com.disciplinescore.app.domain.DisciplineSnapshot
import com.disciplinescore.app.domain.Habit
import com.disciplinescore.app.domain.HabitCategory
import com.disciplinescore.app.domain.Importance
import com.disciplinescore.app.domain.Skip
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.DayOfWeek
import java.time.LocalDate

class HabitRepository(private val dao: HabitDao) {
    fun observe(): Flow<DisciplineSnapshot> = combine(
        dao.observeHabits(),
        dao.observeLogs(),
        dao.observeSkips(),
    ) { habits, logs, skips ->
        DisciplineSnapshot(
            habits = habits.map { it.toDomain() },
            completions = logs.map { Completion(it.habitId, LocalDate.parse(it.date)) },
            skips = skips.map { Skip(it.habitId, LocalDate.parse(it.date)) },
        )
    }

    suspend fun current(): DisciplineSnapshot = DisciplineSnapshot(
        habits = dao.getHabits().map { it.toDomain() },
        completions = dao.getLogs().map { Completion(it.habitId, LocalDate.parse(it.date)) },
        skips = dao.getSkips().map { Skip(it.habitId, LocalDate.parse(it.date)) },
    )

    suspend fun getHabit(id: Long): Habit? = dao.getHabit(id)?.toDomain()

    suspend fun isCompleted(habitId: Long, date: LocalDate): Boolean =
        dao.logCount(habitId, date.toString()) > 0

    suspend fun isSkipped(habitId: Long, date: LocalDate): Boolean =
        dao.skipCount(habitId, date.toString()) > 0

    suspend fun toggle(habitId: Long, date: LocalDate) {
        val key = date.toString()
        if (dao.logCount(habitId, key) > 0) {
            dao.deleteLog(habitId, key)
        } else {
            dao.deleteSkip(habitId, key)
            dao.insertLog(HabitLogEntity(habitId, key, System.currentTimeMillis()))
        }
    }

    suspend fun skip(habitId: Long, date: LocalDate) {
        val key = date.toString()
        dao.deleteLog(habitId, key)
        dao.insertSkip(HabitSkipEntity(habitId, key))
    }

    suspend fun unskip(habitId: Long, date: LocalDate) {
        dao.deleteSkip(habitId, date.toString())
    }

    suspend fun move(id: Long, direction: Int) {
        val habits = dao.getHabits().toMutableList()
        val index = habits.indexOfFirst { it.id == id }
        val target = index + direction
        if (index < 0 || target !in habits.indices) return
        val moved = habits.removeAt(index)
        habits.add(target, moved)
        habits.forEachIndexed { order, habit ->
            if (habit.sortOrder != order) dao.updateSortOrder(habit.id, order)
        }
    }

    suspend fun upsert(habit: Habit) {
        if (habit.id == 0L) {
            val order = dao.maxSortOrder() + 1
            dao.insertHabit(habit.copy(sortOrder = order).toEntity().copy(id = 0))
        } else {
            val existing = dao.getHabit(habit.id) ?: return
            dao.updateHabit(
                habit.copy(
                    sortOrder = existing.sortOrder,
                    createdOn = LocalDate.parse(existing.createdOn),
                ).toEntity(),
            )
        }
    }

    suspend fun delete(id: Long) {
        dao.deleteHabitAndLogs(id)
    }

    suspend fun seedIfEmpty() {
        if (dao.countHabits() > 0) return
        val today = LocalDate.now()
        val everyDay = DayOfWeek.values().toSet()
        val defaults = listOf(
            Habit(0, "Gym workout", Importance.STANDARD, HabitCategory.BODY, everyDay, 7 * 60, 0, today),
            Habit(0, "Study for 1 hour", Importance.STANDARD, HabitCategory.FOCUS, everyDay, 18 * 60, 1, today),
            Habit(0, "Read 10 pages", Importance.STANDARD, HabitCategory.MIND, everyDay, 21 * 60, 2, today),
            Habit(0, "Sleep on time", Importance.STANDARD, HabitCategory.REST, everyDay, 22 * 60 + 30, 3, today),
            Habit(0, "Drink enough water", Importance.STANDARD, HabitCategory.HEALTH, everyDay, 12 * 60, 4, today),
        )
        defaults.forEach { dao.insertHabit(it.toEntity()) }
    }
}
