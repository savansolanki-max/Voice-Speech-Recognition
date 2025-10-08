package com.nsv.voice_demo.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.nsv.voice_demo.R

object NotificationHelper {
    fun createVoiceServiceNotification(context: Context): Notification {
        val channelId = "voice_service_channel"

        val manager = context.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Voice Assistant",
                NotificationManager.IMPORTANCE_LOW
            )
            manager?.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(context, channelId)
            .setContentTitle("Voice Assistant Running")
            .setContentText("Listening for commands...")
            .setSmallIcon(R.drawable.ic_mic)
            .build()
    }
}
