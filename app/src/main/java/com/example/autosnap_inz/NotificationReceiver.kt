package com.example.autosnap_inz

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.autosnap_inz.R

class NotificationReceiver : BroadcastReceiver() {
    @SuppressLint("ScheduleExactAlarm")
    override fun onReceive(context: Context, intent: Intent?) {
        Log.d("NotificationReceiver", "onReceive called!")
        val channelID = "notif_channel_id"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.let {
                val channel = NotificationChannel(
                    channelID,
                    "Notification Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
                )
                it.createNotificationChannel(channel)
            }
        }

        val builder = NotificationCompat.Builder(context, channelID)
            .setSmallIcon(R.drawable.user) // Twoja ikona
            .setContentTitle("AutoSnap here!")
            .setContentText("Time to wake up and get some points!")
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.notify(System.currentTimeMillis().toInt(), builder.build())

        // ponowane ustawienie alartamu za x skeund
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val newIntent = Intent(context, NotificationReceiver::class.java)
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        else
            PendingIntent.FLAG_UPDATE_CURRENT

        val pendingIntent = PendingIntent.getBroadcast(context, 0, newIntent, pendingIntentFlags)
        val nextTrigger = System.currentTimeMillis() + 10_000L // tutaj ustawic

        Log.d("NotificationReceiver", "Scheduling next alarm in 10s at ${nextTrigger}")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextTrigger, pendingIntent)
        } else {
            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, nextTrigger, 10_000L, pendingIntent)
        }
    }
}
