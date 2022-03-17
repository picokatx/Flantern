package com.picobyte.flantern.types

import android.icu.util.Calendar
import java.text.SimpleDateFormat

fun getDate(milliSeconds: Long, dateFormat: String?): String? {
    val formatter = SimpleDateFormat(dateFormat)
    val calendar: Calendar = Calendar.getInstance()
    calendar.timeInMillis = milliSeconds
    return formatter.format(calendar.time)
}