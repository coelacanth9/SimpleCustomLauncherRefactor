package com.coelacanth9.simplecustomlauncher.data

import android.content.ContentUris
import android.content.Context
import android.provider.CalendarContract
import java.time.LocalDate
import java.time.ZoneId

class CalendarRepository(private val context: Context) {

    private val holidayTranslations = mapOf(
        "New Year's Day" to "元日",
        "Coming of Age Day" to "成人の日",
        "National Foundation Day" to "建国記念の日",
        "The Emperor's Birthday" to "天皇誕生日",
        "Vernal Equinox Day" to "春分の日",
        "Showa Day" to "昭和の日",
        "Constitution Memorial Day" to "憲法記念日",
        "Greenery Day" to "みどりの日",
        "Children's Day" to "こどもの日",
        "Marine Day" to "海の日",
        "Mountain Day" to "山の日",
        "Respect for the Aged Day" to "敬老の日",
        "Autumnal Equinox Day" to "秋分の日",
        "Sports Day" to "スポーツの日",
        "Culture Day" to "文化の日",
        "Labour Thanksgiving Day" to "勤労感謝の日",
        "Labor Thanksgiving Day" to "勤労感謝の日",
        "Holiday in lieu" to "振替休日",
        "Substitute Holiday" to "振替休日"
    )

    private fun translateHolidayName(name: String): String {
        holidayTranslations[name]?.let { return it }
        for ((english, japanese) in holidayTranslations) {
            if (name.contains(english, ignoreCase = true)) {
                return if (name.contains("observed", ignoreCase = true) ||
                           name.contains("lieu", ignoreCase = true)) "振替休日"
                       else japanese
            }
        }
        return name
    }

    fun getHolidaysForMonth(year: Int, month: Int): Map<Int, String> {
        val holidayMap = mutableMapOf<Int, String>()
        val startLocalDate = LocalDate.of(year, month, 1)
        val endLocalDate = startLocalDate.withDayOfMonth(startLocalDate.lengthOfMonth())
        val startMillis = startLocalDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endMillis = endLocalDate.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val projection = arrayOf(
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.TITLE,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME
        )
        val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(builder, startMillis)
        ContentUris.appendId(builder, endMillis)

        try {
            val cursor = context.contentResolver.query(
                builder.build(), projection, null, null,
                "${CalendarContract.Instances.BEGIN} ASC"
            )
            cursor?.use {
                val beginCol = it.getColumnIndex(CalendarContract.Instances.BEGIN)
                val titleCol = it.getColumnIndex(CalendarContract.Instances.TITLE)
                val nameCol = it.getColumnIndex(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
                while (it.moveToNext()) {
                    val title = it.getString(titleCol) ?: ""
                    val calName = it.getString(nameCol) ?: ""
                    val begin = it.getLong(beginCol)
                    val date = java.time.Instant.ofEpochMilli(begin)
                        .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                    val isJapaneseHolidayCalendar = calName.contains("Japan", ignoreCase = true) ||
                            calName.contains("日本", ignoreCase = true)
                    val isJapaneseHolidayTitle = title.contains("祝日") || title.contains("の日") ||
                            title.contains("振替") || title.contains("元日") ||
                            holidayTranslations.containsKey(title)
                    if ((isJapaneseHolidayCalendar || isJapaneseHolidayTitle) &&
                        date.year == year && date.monthValue == month) {
                        holidayMap[date.dayOfMonth] = translateHolidayName(title)
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("CalendarDebug", "クエリエラー", e)
        }
        return holidayMap
    }

    fun getHolidayName(date: LocalDate): String? {
        val holidays = getHolidaysForMonth(date.year, date.monthValue)
        return holidays[date.dayOfMonth]
    }
}
