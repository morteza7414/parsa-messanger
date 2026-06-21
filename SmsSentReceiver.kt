package com.example.parsamessenger

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsSentReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        val messageId = intent.getLongExtra("messageId", -1)

        if (messageId == -1L) return

        val dao = AppDatabase.getDatabase(context).messageDao()

        CoroutineScope(Dispatchers.IO).launch {

            val currentMessages = dao.getMessagesById(messageId)
            val message = currentMessages ?: return@launch

            when (resultCode) {

                Activity.RESULT_OK -> {
                    dao.updateMessage(
                        message.copy(
                            sent = true,
                            failed = false
                        )
                    )
                }

                else -> {
                    dao.updateMessage(
                        message.copy(
                            failed = true,
                            sent = false
                        )
                    )
                }
            }
        }
    }
}
