package com.example.closenest.features.notifications.model

import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationDateUtilsTest {

    @Test
    fun `startOfDayMillis uses local timezone boundary`() {
        withDefaultTimeZone("Asia/Ho_Chi_Minh") {
            val nowMillis = calendarOf(2026, Calendar.JUNE, 7, 5, 30).timeInMillis
            val expectedStartOfDay = calendarOf(2026, Calendar.JUNE, 7, 0, 0).timeInMillis

            assertEquals(expectedStartOfDay, startOfDayMillis(nowMillis))
        }
    }

    @Test
    fun `isCreatedToday keeps early morning notifications in today`() {
        withDefaultTimeZone("Asia/Ho_Chi_Minh") {
            val nowMillis = calendarOf(2026, Calendar.JUNE, 7, 5, 30).timeInMillis
            val createdAtToday = calendarOf(2026, Calendar.JUNE, 7, 0, 15).timeInMillis
            val createdAtYesterday = calendarOf(2026, Calendar.JUNE, 6, 23, 50).timeInMillis

            val todayNotification = notification(createdAtToday)
            val yesterdayNotification = notification(createdAtYesterday)

            assertTrue(todayNotification.isCreatedToday(nowMillis))
            assertFalse(yesterdayNotification.isCreatedToday(nowMillis))
        }
    }

    private fun calendarOf(
        year: Int,
        month: Int,
        dayOfMonth: Int,
        hourOfDay: Int,
        minute: Int
    ): Calendar = Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month)
        set(Calendar.DAY_OF_MONTH, dayOfMonth)
        set(Calendar.HOUR_OF_DAY, hourOfDay)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    private fun notification(createdAtMillis: Long) = NotificationItem(
        id = "id-$createdAtMillis",
        type = NotificationType.CHECK_IN,
        status = NotificationStatus.READ,
        relationshipId = null,
        relationshipName = null,
        relationshipAvatarUrl = null,
        title = "title",
        description = "description",
        createdAtMillis = createdAtMillis,
        expiresAtMillis = null,
        actionLabel = null,
        actionType = null
    )

    private fun withDefaultTimeZone(id: String, block: () -> Unit) {
        val original = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone(id))
        try {
            block()
        } finally {
            TimeZone.setDefault(original)
        }
    }
}
