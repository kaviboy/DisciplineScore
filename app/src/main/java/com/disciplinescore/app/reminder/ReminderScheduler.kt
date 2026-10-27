package com.disciplinescore.app.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.disciplinescore.app.data.HabitRepository
import com.disciplinescore.app.domain.Habit
import com.disciplinescore.app.domain.nextReminder
import java.time.LocalDateTime
import java.time.ZoneId

const val EXTRA_HABIT_ID = "habit_id"
const val CHANNEL_ID = "habit-reminders"

class ReminderScheduler(
    private val context: Context,
    private val repository: HabitRepository,
) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val lock = Any()

    suspend fun rescheduleAll() {
        val habits = repository.current().habits
        synchronized(lock) {
            val alarmManager = context.getSystemService(AlarmManager::class.java)
            val previous = prefs.getStringSet(KEY_IDS, emptySet()).orEmpty()
            val current = habits.map { it.id.toString() }.toSet()
            (previous - current).forEach { id -> alarmManager.cancel(broadcast(id.toLong())) }
            habits.forEach { habit -> arm(alarmManager, habit, LocalDateTime.now()) }
            prefs.edit().putStringSet(KEY_IDS, current).apply()
        }
    }

    fun schedule(habit: Habit, after: LocalDateTime) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        synchronized(lock) { arm(alarmManager, habit, after) }
    }

    fun cancel(habitId: Long) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        synchronized(lock) { alarmManager.cancel(broadcast(habitId)) }
    }

    private fun arm(alarmManager: AlarmManager, habit: Habit, after: LocalDateTime) {
        val pending = broadcast(habit.id)
        val trigger = nextReminder(habit, after)
        if (habit.reminderMinutes == null || trigger == null) {
            alarmManager.cancel(pending)
            return
        }
        val at = trigger.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
    }

    private fun broadcast(habitId: Long): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).putExtra(EXTRA_HABIT_ID, habitId)
        return PendingIntent.getBroadcast(
            context,
            habitId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private companion object {
        const val PREFS = "reminders"
        const val KEY_IDS = "ids"
    }
}
