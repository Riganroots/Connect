package com.example.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class ConnectMessagingService : FirebaseMessagingService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        serviceScope.launch {
            PushTokenRegistrar(this@ConnectMessagingService).registerToken(userId, token)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val intendedUserId = message.data["targetUserId"]

        if (!intendedUserId.isNullOrBlank() && intendedUserId != currentUserId) {
            return
        }

        val title = message.notification?.title
            ?: message.data["title"]
            ?: "Connect"
        val body = message.notification?.body
            ?: message.data["body"]
            ?: return

        ConnectNotificationCenter.show(
            context = this,
            title = title.take(120),
            body = body.take(500),
            type = message.data["type"],
            targetId = message.data["targetId"]
        )
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}

object ConnectNotificationCenter {
    private const val CHANNEL_ID = "connect_activity_updates"
    private const val CHANNEL_NAME = "Activity updates"
    private const val CHANNEL_DESCRIPTION = "Activity, chat and community updates from Connect."

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = CHANNEL_DESCRIPTION
        }
        manager.createNotificationChannel(channel)
    }

    fun show(
        context: Context,
        title: String,
        body: String,
        type: String?,
        targetId: String?
    ) {
        createChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("notification_type", type)
            putExtra("notification_target_id", targetId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            (type.orEmpty() + targetId.orEmpty()).hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        try {
            NotificationManagerCompat.from(context)
                .notify((title + body + targetId.orEmpty()).hashCode(), notification)
        } catch (_: SecurityException) {
            // Android 13+ permission may be denied; the app remains fully usable without push alerts.
        }
    }
}
