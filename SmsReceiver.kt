package com.example.parsamessenger

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION == intent.action) {

            val db = AppDatabase.getDatabase(context)
            val dao = db.messageDao()

            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)

            CoroutineScope(Dispatchers.IO).launch {

                for (sms in messages) {

                    val address = sms.originatingAddress ?: "unknown"
                    val body = sms.messageBody

                    dao.insert(
                        MessageEntity(
                            address = address,
                            body = body,
                            isMine = false,
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }
            }
        }
    }
}