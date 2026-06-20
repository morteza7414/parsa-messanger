package com.example.parsamessenger

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.core.content.ContextCompat

object ContactNameUtils {

    fun getName(

        context: Context,

        number: String

    ): String {

        if (

            ContextCompat.checkSelfPermission(

                context,

                Manifest.permission.READ_CONTACTS

            )

            !=

            PackageManager.PERMISSION_GRANTED

        ) {

            return number

        }

        return try {

            val normalized =

                PhoneUtils.normalize(
                    number
                )

            val uri =

                android.net.Uri.withAppendedPath(

                    ContactsContract.PhoneLookup.CONTENT_FILTER_URI,

                    android.net.Uri.encode(
                        normalized
                    )

                )

            val cursor =

                context.contentResolver.query(

                    uri,

                    arrayOf(

                        ContactsContract
                            .PhoneLookup
                            .DISPLAY_NAME

                    ),

                    null,

                    null,

                    null

                )

            cursor?.use {

                if (

                    it.moveToFirst()

                ) {

                    return it.getString(0)
                        ?: number

                }

            }

            number

        }

        catch (

            e: Exception

        ) {

            number

        }

    }

}