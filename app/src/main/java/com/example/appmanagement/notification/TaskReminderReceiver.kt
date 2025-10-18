package com.example.appmanagement.notification

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.os.bundleOf
import androidx.navigation.NavDeepLinkBuilder
import com.example.appmanagement.ui.main.MainActivity
import com.example.appmanagement.R
import com.example.appmanagement.util.TaskReminderScheduler

class TaskReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TaskReminderScheduler.ACTION_REMIND_TASK) return

        val taskId = intent.getLongExtra(TaskReminderScheduler.EXTRA_TASK_ID, -1L)
        val title = intent.getStringExtra(TaskReminderScheduler.EXTRA_TASK_TITLE).orEmpty()
        val taskDate = intent.getStringExtra(TaskReminderScheduler.EXTRA_TASK_DATE).orEmpty()
        val startTime = intent.getStringExtra(TaskReminderScheduler.EXTRA_TASK_TIME).orEmpty()

        TaskReminderScheduler.ensureChannel(context)

        val contentText = when {
            startTime.isNotBlank() -> context.getString(R.string.task_reminder_body_time, startTime)
            taskDate.isNotBlank() -> context.getString(R.string.task_reminder_body_date, taskDate)
            else -> context.getString(R.string.task_reminder_body_default)
        }

        val pendingIntent = NavDeepLinkBuilder(context)
            .setComponentName(ComponentName(context, MainActivity::class.java))
            .setGraph(R.navigation.nav_graph)
            .setDestination(R.id.editFragment)
            .setArguments(bundleOf("editId" to taskId))
            .createPendingIntent()

        val notification = NotificationCompat.Builder(context, TaskReminderScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_clock)
            .setContentTitle(
                context.getString(
                    R.string.task_reminder_title,
                    title.ifBlank { context.getString(R.string.app_name) }
                )
            )
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        with(NotificationManagerCompat.from(context)) {
            notify(taskId.hashCode(), notification)
        }
    }
}
