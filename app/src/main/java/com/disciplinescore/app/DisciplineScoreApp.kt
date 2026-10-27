package com.disciplinescore.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import com.disciplinescore.app.data.HabitRepository
import com.disciplinescore.app.data.PreferencesStore
import com.disciplinescore.app.data.local.AppDatabase
import com.disciplinescore.app.reminder.CHANNEL_ID
import com.disciplinescore.app.reminder.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class DisciplineScoreApp : Application() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    lateinit var repository: HabitRepository
        private set

    lateinit var scheduler: ReminderScheduler
        private set

    lateinit var preferences: PreferencesStore
        private set

    override fun onCreate() {
        super.onCreate()
        createChannel()
        val database = AppDatabase.create(this)
        repository = HabitRepository(database.habitDao())
        preferences = PreferencesStore(this)
        scheduler = ReminderScheduler(this, repository)
        runBlocking(Dispatchers.IO) {
            repository.seedIfEmpty()
        }
        scope.launch {
            try {
                scheduler.rescheduleAll()
            } catch (error: Exception) {
                Log.e(TAG, "Could not schedule reminders", error)
            }
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Habit reminders",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Reminds you when a scheduled habit is still open."
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private companion object {
        const val TAG = "DisciplineScore"
    }
}
