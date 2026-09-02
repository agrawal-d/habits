package com.avantgardelabs.healthyhabitstracker.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.avantgardelabs.healthyhabitstracker.MainActivity
import com.avantgardelabs.healthyhabitstracker.data.DataManager
import com.avantgardelabs.healthyhabitstracker.data.ReminderScheduler
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class HabitReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        showNotification(context)
        
        // Reschedule alarm for tomorrow
        val dataManager = DataManager(context)
        val reminder = dataManager.habitData.reminder
        if (reminder.enabled) {
            ReminderScheduler.scheduleReminder(context, reminder.hour, reminder.minute, true)
        }
    }

    companion object {
        fun showNotification(context: Context) {
            val channelId = "habit_reminder_channel"
            val notificationId = 1002

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val channel = NotificationChannel(
                    channelId,
                    "Habit Reminders",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Daily reminders to complete habit questionnaire"
                }
                notificationManager.createNotificationChannel(channel)

            val todayStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            val contentIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("extra_launch_questionnaire", true)
                putExtra("extra_date", todayStr)
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                2002,
                contentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm) // System standard fallback icon
                .setContentTitle("Daily Habit Review")
                .setContentText("Tap to record your habits for today.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            try {
                notificationManager.notify(notificationId, notification)
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
        }
    }
}
