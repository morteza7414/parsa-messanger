package com.example.parsamessenger

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class MmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // فعلاً برای واجد شرایط شدن Default SMS کافی است.
        // بعداً اگر خواستی MMS واقعی را هندل کنیم اینجا تکمیلش می‌کنیم.
    }
}
