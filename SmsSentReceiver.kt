package com.example.parsamessenger

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class SmsSentReceiver : BroadcastReceiver() {

    override fun onReceive(

        context: Context,

        intent: Intent

    ) {

        val messageId =

            intent.getLongExtra(

                "messageId",

                -1

            )

        if (

            resultCode ==

            android.app.Activity.RESULT_OK

        ) {

            // مرحله بعد
            // sent = true

        }

    }

}