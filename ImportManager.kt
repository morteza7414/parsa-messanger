package com.example.parsamessenger

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ImportManager {

    private const val PREF = "sms_import"
    private const val KEY_IMPORTED = "imported"

    suspend fun runIfNeeded(context: Context) {

        val prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)

        val alreadyImported = prefs.getBoolean(KEY_IMPORTED, false)

        if (alreadyImported) return

        withContext(Dispatchers.IO) {
            SmsImporter.importAll(context)
        }

        prefs.edit {
            putBoolean(KEY_IMPORTED, true)
        }
    }
}
