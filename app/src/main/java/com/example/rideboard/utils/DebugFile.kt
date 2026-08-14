package com.example.rideboard.utils

import android.content.Context
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun appendDebugLog(
    context: Context,
    line: String,
) {
    try {
        val file = File(context.filesDir, "gps_debug.txt")
        file.appendText(line + "\n")
    } catch (e: Exception) {
        Log.e("DEBUG_LOG", "Erreur appendDebugLog", e)
    }
}