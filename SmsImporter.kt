package com.example.parsamessenger

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun loadExistingSms(context: Context, dao: MessageDao) {

    val prefs = context.getSharedPreferences("sms_import", Context.MODE_PRIVATE)

    if (prefs.getBoolean("imported", false)) return

    withContext(Dispatchers.IO) {

        val cursor = context.contentResolver.query(
            Uri.parse("content://sms/"),
            null,
            null,
            null,
            "date ASC"
        )

        cursor?.use {

            val bodyIndex = it.getColumnIndex("body")
            val addressIndex = it.getColumnIndex("address")
            val dateIndex = it.getColumnIndex("date")
            val typeIndex = it.getColumnIndex("type")

            while (it.moveToNext()) {

                val body = it.getString(bodyIndex)
                val address = it.getString(addressIndex)
                val date = it.getLong(dateIndex)
                val type = it.getInt(typeIndex)

                val isMine = type == 2

                dao.insert(
                    MessageEntity(
                        address = PhoneUtils.normalize(address),
                        body = body,
                        timestamp = date,
                        isMine = isMine
                    )
                )
            }
        }

        prefs.edit().putBoolean("imported", true).apply()
    }
}
