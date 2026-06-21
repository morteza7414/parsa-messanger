package com.example.parsamessenger

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action

        if (action != Telephony.Sms.Intents.SMS_DELIVER_ACTION &&
            action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION
        ) {
            return
        }

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        val dao = AppDatabase.getDatabase(context).messageDao()

        CoroutineScope(Dispatchers.IO).launch {
            messages.forEach { sms ->
                val address = sms.originatingAddress ?: return@forEach
                val body = sms.messageBody ?: ""
                val date = sms.timestampMillis

                dao.insert(
                    MessageEntity(
                        address = PhoneUtils.normalize(address),
                        body = body,
                        timestamp = date,
                        isMine = false,
                        sent = true,
                        delivered = true,
                        isRead = false,
                        failed = false
                    )
                )
                NotificationHelper.showNotification(
                    context,
                    address,
                    body
                )
            }
        }
    }
}

object NotificationHelper {
    private const val CHANNEL_ID = "messages"

    fun showNotification(context: Context, address: String, body: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Messages",
                NotificationManager.IMPORTANCE_HIGH
            )
            manager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("address", address)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // از آیکون پیش‌فرض سیستم برای پیشگیری از خطای ناموجود بودن ریسورس استفاده می‌کنیم
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.sym_action_chat)
            .setContentTitle(address)
            .setContentText(body)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        manager.notify(address.hashCode(), notification)
    }
}
