package com.example

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.data.repository.HakeemRepository
import com.example.data.remote.SupabaseService
import io.github.jan.supabase.auth.auth

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val title = remoteMessage.notification?.title ?: "New Notification"
        val body = remoteMessage.notification?.body ?: ""

        sendNotification(title, body)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        android.util.Log.d("FCM_DEBUG", "onNewToken triggered by Firebase.")
        
        val repository = HakeemRepository()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. Ensure Supabase Auth has finished loading from storage
                SupabaseService.client.auth.awaitInitialization()
                
                val user = SupabaseService.client.auth.currentUserOrNull()
                if (user == null) {
                    android.util.Log.d("FCM_DEBUG", "No active user in onNewToken, skipping save.")
                    return@launch
                }
                
                // 2. Refresh the session if the access token is missing/expired
                var accessToken = SupabaseService.client.auth.currentAccessTokenOrNull()
                if (accessToken == null) {
                    try {
                        SupabaseService.client.auth.refreshCurrentSession()
                        accessToken = SupabaseService.client.auth.currentAccessTokenOrNull()
                    } catch (e: Exception) {
                        android.util.Log.e("FCM_DEBUG", "Session refresh failed in onNewToken: ${e.message}")
                        return@launch
                    }
                }
                
                if (accessToken != null) {
                    repository.saveFcmToken(user.id, token)
                } else {
                    android.util.Log.d("FCM_DEBUG", "Could not obtain valid access token, skipping save.")
                }
            } catch (e: Exception) {
                android.util.Log.e("FCM_DEBUG", "Error in onNewToken: ${e.message}", e)
            }
        }
    }

    private fun sendNotification(title: String, messageBody: String) {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_ONE_SHOT
        )

        val channelId = "hakeem_cafeteria_notifications"
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Hakeem Cafeteria Notifications",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }
}
