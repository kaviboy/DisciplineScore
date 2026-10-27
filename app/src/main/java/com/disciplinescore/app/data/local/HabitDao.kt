package com.disciplinescore.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
abstract class HabitDao {
    @Query("SELECT * FROM habits ORDER BY sortOrder ASC, id ASC")
    abstract fun observeHabits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits ORDER BY sortOrder ASC, id ASC")
    abstract suspend fun getHabits(): List<HabitEntity>

    @Query("SELECT * FROM habits WHERE id = :id")
    abstract suspend fun getHabit(id: Long): HabitEntity?

    @Query("SELECT COUNT(*) FROM habits")
    abstract suspend fun countHabits(): Int

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM habits")
    abstract suspend fun maxSortOrder(): Int

    @Insert
    abstract suspend fun insertHabit(habit: HabitEntity): Long

    @Update
    abstract suspend fun updateHabit(habit: HabitEntity)

    @Query("DELETE FROM habits WHERE id = :id")
    abstract suspend fun deleteHabit(id: Long)

    @Query("SELECT * FROM habit_logs")
    abstract fun observeLogs(): Flow<List<HabitLogEntity>>

    @Query("SELECT * FROM habit_logs")
    abstract suspend fun getLogs(): List<HabitLogEntity>

    @Query("SELECT COUNT(*) FROM habit_logs WHERE habitId = :habitId AND date = :date")
    abstract suspend fun logCount(habitId: Long, date: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertLog(log: HabitLogEntity)

    @Query("DELETE FROM habit_logs WHERE habitId = :habitId AND date = :date")
    abstract suspend fun deleteLog(habitId: Long, date: String)

    @Query("DELETE FROM habit_logs WHERE habitId = :habitId")
    abstract suspend fun deleteLogsForHabit(habitId: Long)

    @Query("UPDATE habits SET sortOrder = :sortOrder WHERE id = :id")
    abstract suspend fun updateSortOrder(id: Long, sortOrder: Int)

    @Query("SELECT * FROM habit_skips")
    abstract fun observeSkips(): Flow<List<HabitSkipEntity>>

    @Query("SELECT * FROM habit_skips")
    abstract suspend fun getSkips(): List<HabitSkipEntity>

    @Query("SELECT COUNT(*) FROM habit_skips WHERE habitId = :habitId AND date = :date")
    abstract suspend fun skipCount(habitId: Long, date: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertSkip(skip: HabitSkipEntity)

    @Query("DELETE FROM habit_skips WHERE habitId = :habitId AND date = :date")
    abstract suspend fun deleteSkip(habitId: Long, date: String)

    @Query("DELETE FROM habit_skips WHERE habitId = :habitId")
    abstract suspend fun deleteSkipsForHabit(habitId: Long)

    @Transaction
    open suspend fun deleteHabitAndLogs(id: Long) {
        deleteLogsForHabit(id)
        deleteSkipsForHabit(id)
        deleteHabit(id)
    }
}
