package com.example.parsamessenger

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsDeliveredReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        val messageId = intent.getLongExtra("messageId", -1)
        if (messageId == -1L) return

        val dao = AppDatabase.getDatabase(context).messageDao()

        CoroutineScope(Dispatchers.IO).launch {

            val message = dao.getMessagesById(messageId) ?: return@launch

            dao.updateMessage(
                message.copy(
                    delivered = true
                )
            )
        }
    }
}

