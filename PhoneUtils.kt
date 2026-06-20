package com.example.parsamessenger

object PhoneUtils {

    fun normalize(number: String): String {

        return number
            .replace(" ", "")
            .replace("-", "")
            .replace("+98", "0")
            .trim()

    }
}