package com.example.appmanagement.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.widget.RemoteViews
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.appmanagement.R
import com.example.appmanagement.ui.main.MainActivity

class TaskReminderReceiver : BroadcastReceiver() {

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, 0L)
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        val startTime = intent.getStringExtra(EXTRA_START_TIME).orEmpty()

        if (taskId <= 0L) return

        createChannelIfNeeded(context)

        // Bấm thông báo → mở MainActivity (HomeFragment là startDestination)
        val clickIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val contentIntent = PendingIntent.getActivity(
            context,
            taskId.toInt(),
            clickIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Layout custom: icon + title + content (notification_task_reminder.xml)
        val customView = RemoteViews(context.packageName, R.layout.notification_task_reminder).apply {
            setTextViewText(
                R.id.txtTitle,
                "${context.getString(R.string.notify_task_start_prefix)} $title"
            )
            setTextViewText(R.id.txtContent, startTime)
            setImageViewResource(R.id.imgIcon, R.drawable.ic_clock)
            // Nếu sau này bạn muốn, có thể set thêm tvTimeRange / tvCreatedDate ở đây
        }

        // Âm thanh hệ thống
        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_clock)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText("${context.getString(R.string.notify_task_start_prefix)} $title ($startTime)")
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setSound(soundUri)
            .setContentIntent(contentIntent)
            .setCustomContentView(customView)
            .setCustomBigContentView(customView)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .build()

        NotificationManagerCompat.from(context).notify(taskId.toInt(), notification)
    }

    private fun createChannelIfNeeded(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val existing = notificationManager.getNotificationChannel(CHANNEL_ID)
        if (existing != null) return

        // Âm thanh hệ thống cho channel
        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notify_task_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            enableVibration(true)
            enableLights(true)
            setSound(
                soundUri,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build()
            )
            description = context.getString(R.string.notify_task_channel_desc)
        }

        notificationManager.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "task_start_time_channel"
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_START_TIME = "extra_start_time"
    }
}
