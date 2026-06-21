package com.example.parsamessenger

import android.content.Context
import android.net.Uri
import android.provider.Telephony
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object SmsImporter {

    suspend fun importAll(context: Context) {

        withContext(Dispatchers.IO) {

            val dao = AppDatabase.getDatabase(context).messageDao()

            val uri = Uri.parse("content://sms")

            val projection = arrayOf(
                Telephony.Sms.ADDRESS,
                Telephony.Sms.BODY,
                Telephony.Sms.DATE,
                Telephony.Sms.TYPE,
                Telephony.Sms.READ
            )

            val cursor = context.contentResolver.query(
                uri,
                projection,
                null,
                null,
                "date ASC"
            ) ?: return@withContext

            val messages = mutableListOf<MessageEntity>()

            val addressIndex = cursor.getColumnIndex(Telephony.Sms.ADDRESS)
            val bodyIndex = cursor.getColumnIndex(Telephony.Sms.BODY)
            val dateIndex = cursor.getColumnIndex(Telephony.Sms.DATE)
            val typeIndex = cursor.getColumnIndex(Telephony.Sms.TYPE)
            val readIndex = cursor.getColumnIndex(Telephony.Sms.READ)

            while (cursor.moveToNext()) {

                val address = cursor.getString(addressIndex) ?: continue
                val body = cursor.getString(bodyIndex) ?: ""
                val date = cursor.getLong(dateIndex)
                val type = cursor.getInt(typeIndex)
                val read = cursor.getInt(readIndex) == 1

                messages.add(
                    MessageEntity(
                        address = PhoneUtils.normalize(address),
                        body = body,
                        timestamp = date,
                        isMine = type == Telephony.Sms.MESSAGE_TYPE_SENT,
                        sent = true,
                        delivered = true,
                        isRead = read,
                        failed = false
                    )
                )
            }

            cursor.close()

            dao.insertAll(messages)
        }
    }
}
