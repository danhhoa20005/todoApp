package com.example.appmanagement.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat
import com.example.appmanagement.notification.TaskReminderReceiver
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object TaskReminderScheduler {

    const val CHANNEL_ID = "task_reminder_channel"
    const val ACTION_REMIND_TASK = "com.example.appmanagement.action.REMIND_TASK"
    const val EXTRA_TASK_ID = "extra_task_id"
    const val EXTRA_TASK_TITLE = "extra_task_title"
    const val EXTRA_TASK_DATE = "extra_task_date"
    const val EXTRA_TASK_TIME = "extra_task_time"

    private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = NotificationManagerCompat.from(context)
        val existing = manager.getNotificationChannel(CHANNEL_ID)
        if (existing != null) return

        val channel = NotificationChannelCompat.Builder(
            CHANNEL_ID,
            NotificationManagerCompat.IMPORTANCE_HIGH
        )
            .setName(context.getString(com.example.appmanagement.R.string.task_reminder_channel_name))
            .setDescription(context.getString(com.example.appmanagement.R.string.task_reminder_channel_desc))
            .build()
        manager.createNotificationChannel(channel)
    }

    fun schedule(
        context: Context,
        taskId: Long,
        title: String,
        taskDate: String,
        startTime: String
    ) {
        val triggerAtMillis = computeTriggerAtMillis(taskDate, startTime) ?: return
        val appContext = context.applicationContext
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        ensureChannel(appContext)

        val intent = buildIntent(appContext, taskId, title, taskDate, startTime)
        val flags = pendingIntentFlags(PendingIntent.FLAG_UPDATE_CURRENT)
        val pendingIntent = PendingIntent.getBroadcast(
            appContext,
            taskId.hashCode(),
            intent,
            flags
        )

        // Cancel any previous alarm with the same request code to avoid duplicates
        alarmManager.cancel(pendingIntent)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    fun cancel(context: Context, taskId: Long) {
        val appContext = context.applicationContext
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        ensureChannel(appContext)

        val intent = buildIntent(appContext, taskId, title = "", taskDate = "", startTime = "")
        val flags = pendingIntentFlags(PendingIntent.FLAG_NO_CREATE)
        val pendingIntent = PendingIntent.getBroadcast(
            appContext,
            taskId.hashCode(),
            intent,
            flags
        ) ?: return

        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    private fun buildIntent(
        context: Context,
        taskId: Long,
        title: String,
        taskDate: String,
        startTime: String
    ): Intent {
        return Intent(context, TaskReminderReceiver::class.java).apply {
            action = ACTION_REMIND_TASK
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_TASK_TITLE, title)
            putExtra(EXTRA_TASK_DATE, taskDate)
            putExtra(EXTRA_TASK_TIME, startTime)
        }
    }

    private fun computeTriggerAtMillis(date: String, time: String): Long? {
        if (date.isBlank() || time.isBlank()) return null
        val datePart = runCatching { LocalDate.parse(date, dateFormatter) }.getOrNull() ?: return null
        val timePart = runCatching { LocalTime.parse(time, timeFormatter) }.getOrNull() ?: return null

        val zonedDateTime = datePart.atTime(timePart).atZone(ZoneId.systemDefault())
        val triggerAt = zonedDateTime.toInstant().toEpochMilli()
        return if (triggerAt > System.currentTimeMillis()) triggerAt else null
    }

    private fun pendingIntentFlags(base: Int): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            base or PendingIntent.FLAG_IMMUTABLE
        } else {
            base
        }
    }
}
