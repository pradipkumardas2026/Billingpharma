package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {
    private val standardDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)
    private val monthYearFormat = SimpleDateFormat("MM/yyyy", Locale.ENGLISH)
    private val isoDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)

    fun currentDateString(): String {
        return standardDateFormat.format(Date())
    }

    fun getTodayDateString(): String {
        return currentDateString()
    }

    fun formatDate(timestamp: Long): String {
        return formatTimestamp(timestamp)
    }

    fun parseDateToMillis(dateStr: String): Long? {
        return try {
            standardDateFormat.parse(dateStr.trim())?.time
        } catch (e: Exception) {
            null
        }
    }

    fun isToday(timestamp: Long): Boolean {
        val cal1 = Calendar.getInstance()
        cal1.timeInMillis = timestamp
        val cal2 = Calendar.getInstance()
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    fun formatTimestamp(timestamp: Long): String {
        return standardDateFormat.format(Date(timestamp))
    }

    fun parseDateToTimestamp(dateStr: String): Long {
        return parseStartOfDayTimestamp(dateStr)
    }

    fun parseStartOfDayTimestamp(dateStr: String): Long {
        return try {
            val date = standardDateFormat.parse(dateStr.trim()) ?: return System.currentTimeMillis()
            val cal = Calendar.getInstance()
            cal.time = date
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }

    fun parseEndOfDayTimestamp(dateStr: String): Long {
        return try {
            val date = standardDateFormat.parse(dateStr.trim()) ?: return System.currentTimeMillis()
            val cal = Calendar.getInstance()
            cal.time = date
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
            cal.timeInMillis
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }

    fun isExpired(expiryStr: String): Boolean {
        val exp = parseExpiry(expiryStr) ?: return false
        return exp < System.currentTimeMillis()
    }

    fun isNearExpiry(expiryStr: String, withinDays: Int = 90): Boolean {
        val exp = parseExpiry(expiryStr) ?: return false
        val now = System.currentTimeMillis()
        val threshold = now + (withinDays * 86400000L)
        return now <= exp && exp <= threshold
    }

    private fun parseExpiry(expiryStr: String): Long? {
        val cleaned = expiryStr.trim()
        val calendar = Calendar.getInstance()
        return try {
            if (cleaned.contains("/")) {
                val parts = cleaned.split("/")
                if (parts.size == 2) {
                    val month = parts[0].toInt() - 1
                    val year = parts[1].toInt().let { if (it < 100) 2000 + it else it }
                    calendar.set(year, month, calendar.getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59)
                    calendar.timeInMillis
                } else if (parts.size == 3) {
                    standardDateFormat.parse(cleaned)?.time
                } else null
            } else if (cleaned.contains("-")) {
                val parts = cleaned.split("-")
                if (parts.size == 2) {
                    val year = parts[0].toInt().let { if (it < 100) 2000 + it else it }
                    val month = parts[1].toInt() - 1
                    calendar.set(year, month, calendar.getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59)
                    calendar.timeInMillis
                } else if (parts.size == 3) {
                    isoDateFormat.parse(cleaned)?.time
                } else null
            } else null
        } catch (e: Exception) {
            null
        }
    }

    fun daysSince(timestamp: Long): Int {
        val diff = System.currentTimeMillis() - timestamp
        return (diff / 86400000L).toInt().coerceAtLeast(0)
    }

    fun is30DaysDue(timestamp: Long, dueAmount: Double): Boolean {
        return dueAmount > 0.0 && daysSince(timestamp) >= 30
    }
}
