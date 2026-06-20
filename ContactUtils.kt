package com.example.parsamessenger

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.content.ContextCompat

data class PhoneContact(
    val name: String,
    val number: String
)

object ContactsUtils {

    fun getContacts(
        context: Context
    ): List<PhoneContact> {

        if (
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CONTACTS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return emptyList()
        }

        val contacts = mutableListOf<PhoneContact>()

        val cursor =
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null,
                null,
                null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
            )

        cursor?.use {

            while (it.moveToNext()) {

                val name =
                    it.getString(
                        it.getColumnIndexOrThrow(
                            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
                        )
                    )

                val number =
                    it.getString(
                        it.getColumnIndexOrThrow(
                            ContactsContract.CommonDataKinds.Phone.NUMBER
                        )
                    )

                contacts.add(
                    PhoneContact(
                        name,
                        number
                    )
                )
            }
        }

        return contacts
    }

    fun getContactPhoto(
        context: Context,
        number: String
    ): Uri? {

        if (
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CONTACTS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return null
        }

        return try {

            val uri =
                Uri.withAppendedPath(
                    ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                    Uri.encode(number)
                )

            val cursor =
                context.contentResolver.query(
                    uri,
                    arrayOf(
                        ContactsContract.PhoneLookup.PHOTO_URI
                    ),
                    null,
                    null,
                    null
                )

            cursor?.use {

                if (it.moveToFirst()) {

                    val photo =
                        it.getString(0)

                    if (photo != null) {

                        return Uri.parse(photo)

                    }
                }
            }

            null

        } catch (_: Exception) {

            null

        }
    }
}