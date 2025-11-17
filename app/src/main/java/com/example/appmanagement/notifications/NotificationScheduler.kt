package com.example.appmanagement.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Hỗ trợ đặt Alarm tới đúng giờ bắt đầu của task để hiển thị Notification.
 */
object NotificationScheduler {
    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    fun scheduleTaskReminder(
        context: Context,
        taskId: Long,
        title: String,
        date: String,
        startTime: String
    ) {
        val triggerAtMillis = parseDateTime(date, startTime) ?: return
        if (triggerAtMillis <= System.currentTimeMillis()) return

        val intent = Intent(context, TaskReminderReceiver::class.java).apply {
            putExtra(TaskReminderReceiver.EXTRA_TASK_ID, taskId)
            putExtra(TaskReminderReceiver.EXTRA_TITLE, title)
            putExtra(TaskReminderReceiver.EXTRA_START_TIME, startTime)
        }

        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.toInt(),
            intent,
            flags
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    private fun parseDateTime(date: String, time: String): Long? = try {
        val dateObj = LocalDate.parse(date, dateFormatter)
        val timeObj = LocalTime.parse(time, timeFormatter)
        val dateTime = LocalDateTime.of(dateObj, timeObj)
        dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    } catch (_: DateTimeParseException) {
        null
    }
}
