package com.avantgardelabs.healthyhabitstracker.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.avantgardelabs.healthyhabitstracker.data.DataManager
import com.avantgardelabs.healthyhabitstracker.data.ReminderScheduler

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED || action == "android.intent.action.QUICKBOOT_POWERON") {
            val dataManager = DataManager(context)
            val reminder = dataManager.habitData.reminder
            if (reminder.enabled) {
                ReminderScheduler.scheduleReminder(context, reminder.hour, reminder.minute, true)
            }
        }
    }
}
