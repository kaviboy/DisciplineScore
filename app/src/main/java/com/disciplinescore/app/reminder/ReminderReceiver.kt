package com.disciplinescore.app.reminder

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.disciplinescore.app.DisciplineScoreApp
import com.disciplinescore.app.MainActivity
import com.disciplinescore.app.R
import com.disciplinescore.app.domain.appliesOn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.LocalTime

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val habitId = intent.getLongExtra(EXTRA_HABIT_ID, -1L)
        if (habitId < 0L) return
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as DisciplineScoreApp
                val habit = app.repository.getHabit(habitId)
                val minutes = habit?.reminderMinutes
                if (habit == null || minutes == null) {
                    app.scheduler.cancel(habitId)
                    return@launch
                }
                val now = LocalDateTime.now()
                val today = now.toLocalDate()
                val start = today.atTime(LocalTime.of(minutes.coerceIn(0, 23 * 60 + 59) / 60, minutes.coerceIn(0, 23 * 60 + 59) % 60))
                val end = start.plusHours(3)
                val completed = app.repository.isCompleted(habitId, today)
                val skipped = app.repository.isSkipped(habitId, today)
                if (habit.appliesOn(today) && !completed && !skipped && !now.isBefore(start) && now.isBefore(end)) {
                    showNotification(context, habit.id, habit.name)
                }
                app.scheduler.schedule(habit, now.plusSeconds(30))
            } finally {
                pending.finish()
            }
        }
    }

    private fun showNotification(context: Context, habitId: Long, name: String) {
        val notifications = NotificationManagerCompat.from(context)
        val allowed = if (android.os.Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED && notifications.areNotificationsEnabled()
        } else {
            notifications.areNotificationsEnabled()
        }
        if (!allowed) return

        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(name)
            .setContentText("Still open today. Checking it off updates your discipline score.")
            .setContentIntent(open)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        notifications.notify(habitId.toInt(), notification)
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                (context.applicationContext as DisciplineScoreApp).scheduler.rescheduleAll()
            } finally {
                pending.finish()
            }
        }
    }
}
