package com.example.closenest.features.notifications.model

import java.util.Calendar

fun startOfDayMillis(timestampMillis: Long = System.currentTimeMillis()): Long =
    Calendar.getInstance().apply {
        timeInMillis = timestampMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

fun NotificationItem.isCreatedToday(nowMillis: Long = System.currentTimeMillis()): Boolean =
    createdAtMillis >= startOfDayMillis(nowMillis)
