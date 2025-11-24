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

// Đặt alarm đúng giờ bắt đầu task để bắn Broadcast → hiện notification
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
        // Ghép date + time → millis, nếu lỗi hoặc < hiện tại thì bỏ
        val triggerAtMillis = parseDateTime(date, startTime) ?: return
        if (triggerAtMillis <= System.currentTimeMillis()) return

        // Intent gửi tới TaskReminderReceiver khi tới giờ
        val intent = Intent(context, TaskReminderReceiver::class.java).apply {
            putExtra(TaskReminderReceiver.EXTRA_TASK_ID, taskId)
            putExtra(TaskReminderReceiver.EXTRA_TITLE, title)
            putExtra(TaskReminderReceiver.EXTRA_START_TIME, startTime)
        }

        // PendingIntent broadcast (1 task = 1 requestCode riêng)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.toInt(),
            intent,
            flags
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Android 12+ cần có đặc quyền SCHEDULE_EXACT_ALARM
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                // Không có quyền đặt exact alarm → thoát, tránh SecurityException
                return
            }
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } catch (_: SecurityException) {
            // Thiếu quyền / bị ROM chặn → đơn giản là không đặt alarm
        }
    }

    // Chuyển "dd/MM/yyyy" + "HH:mm" → millis (Long)
    private fun parseDateTime(date: String, time: String): Long? = try {
        val dateObj = LocalDate.parse(date, dateFormatter)
        val timeObj = LocalTime.parse(time, timeFormatter)
        val dateTime = LocalDateTime.of(dateObj, timeObj)
        dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    } catch (_: DateTimeParseException) {
        null
    }
}
