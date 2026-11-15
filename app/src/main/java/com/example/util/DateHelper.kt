package com.example.util

import com.example.data.model.TaskStatus
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

object DateHelper {
    private val isoFormat: SimpleDateFormat
        get() = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            isLenient = false
        }

    private val friendlyFormat: SimpleDateFormat
        get() = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

    private val shortFormat: SimpleDateFormat
        get() = SimpleDateFormat("MMM d", Locale.getDefault())

    private val timestampFormat: SimpleDateFormat
        get() = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())

    fun todayIso(): String = isoFormat.format(Date())

    fun addDaysIso(days: Int): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, days)
        return isoFormat.format(cal.time)
    }

    fun epochMillisToIso(millis: Long): String {
        val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = millis
        }
        val localCal = Calendar.getInstance().apply {
            set(
                utcCal.get(Calendar.YEAR),
                utcCal.get(Calendar.MONTH),
                utcCal.get(Calendar.DAY_OF_MONTH),
                0,
                0,
                0
            )
            set(Calendar.MILLISECOND, 0)
        }
        return isoFormat.format(localCal.time)
    }

    fun isoToUtcMillis(iso: String): Long? {
        val parsed = parseIsoDate(iso) ?: return null
        val localCal = Calendar.getInstance().apply { time = parsed }
        val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(
                localCal.get(Calendar.YEAR),
                localCal.get(Calendar.MONTH),
                localCal.get(Calendar.DAY_OF_MONTH),
                0,
                0,
                0
            )
            set(Calendar.MILLISECOND, 0)
        }
        return utcCal.timeInMillis
    }

    fun normalizeDateString(raw: String?): String {
        val trimmed = raw?.trim().orEmpty()
        if (trimmed.isEmpty()) return todayIso()
        val formats = listOf(
            "yyyy-MM-dd",
            "yyyy/MM/dd",
            "MM/dd/yyyy",
            "dd/MM/yyyy",
            "MMM d, yyyy",
            "dd-MM-yyyy"
        )
        for (pattern in formats) {
            try {
                val parser = SimpleDateFormat(pattern, Locale.US).apply { isLenient = false }
                val date = parser.parse(trimmed)
                if (date != null) {
                    return isoFormat.format(date)
                }
            } catch (_: Exception) {
                // Try next date format
            }
        }
        return trimmed
    }

    fun parseIsoDate(iso: String): Date? {
        return try {
            isoFormat.parse(iso.trim())
        } catch (_: Exception) {
            null
        }
    }

    fun daysFromToday(deadlineIso: String, referenceTodayIso: String = todayIso()): Long? {
        val target = parseIsoDate(deadlineIso) ?: return null
        val today = parseIsoDate(referenceTodayIso) ?: return null
        val diffMs = target.time - today.time
        return TimeUnit.MILLISECONDS.toDays(diffMs)
    }

    fun isOverdue(
        deadlineIso: String,
        status: TaskStatus,
        referenceTodayIso: String = todayIso()
    ): Boolean {
        if (status == TaskStatus.COMPLETED) return false
        val days = daysFromToday(deadlineIso, referenceTodayIso) ?: return false
        return days < 0
    }

    fun isDueSoon(
        deadlineIso: String,
        referenceTodayIso: String = todayIso()
    ): Boolean {
        val days = daysFromToday(deadlineIso, referenceTodayIso) ?: return false
        return days in 0..3
    }

    fun formatDeadlineLabel(deadlineIso: String, status: TaskStatus): String {
        val date = parseIsoDate(deadlineIso) ?: return deadlineIso
        val days = daysFromToday(deadlineIso) ?: return friendlyFormat.format(date)
        return when {
            status == TaskStatus.COMPLETED -> friendlyFormat.format(date)
            days < -1 -> "${shortFormat.format(date)} (${-days}d overdue)"
            days == -1L -> "${shortFormat.format(date)} (1d overdue)"
            days == 0L -> "Today (${shortFormat.format(date)})"
            days == 1L -> "Tomorrow (${shortFormat.format(date)})"
            days in 2..6 -> "In ${days}d (${shortFormat.format(date)})"
            else -> friendlyFormat.format(date)
        }
    }

    fun formatSyncTimestamp(millis: Long): String {
        if (millis <= 0L) return "Not synced yet"
        return timestampFormat.format(Date(millis))
    }

    fun extractSpreadsheetId(input: String): String {
        val trimmed = input.trim()
        val regex = Regex("/spreadsheets/d/([a-zA-Z0-9-_]+)")
        val match = regex.find(trimmed)
        return match?.groupValues?.getOrNull(1) ?: trimmed
    }

    data class WeekDayInfo(
        val isoDate: String,
        val dayShortLabel: String,
        val dayFullLabel: String,
        val dayNumber: String,
        val isToday: Boolean
    )

    fun getWeekDays(
        weekOffset: Int = 0,
        referenceTodayIso: String = todayIso()
    ): List<WeekDayInfo> {
        val refDate = parseIsoDate(referenceTodayIso) ?: Date()
        val cal = Calendar.getInstance().apply {
            time = refDate
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            val currentDow = get(Calendar.DAY_OF_WEEK)
            val diffFromSunday = currentDow - Calendar.SUNDAY
            add(Calendar.DAY_OF_YEAR, -diffFromSunday + (weekOffset * 7))
        }

        val shortDayFmt = SimpleDateFormat("EEE", Locale.getDefault())
        val fullDayFmt = SimpleDateFormat("EEEE, MMM d", Locale.getDefault())
        val dayNumFmt = SimpleDateFormat("d", Locale.getDefault())

        return (0 until 7).map {
            val iso = isoFormat.format(cal.time)
            val info = WeekDayInfo(
                isoDate = iso,
                dayShortLabel = shortDayFmt.format(cal.time),
                dayFullLabel = fullDayFmt.format(cal.time),
                dayNumber = dayNumFmt.format(cal.time),
                isToday = iso == referenceTodayIso
            )
            cal.add(Calendar.DAY_OF_YEAR, 1)
            info
        }
    }

    fun formatWeekRangeLabel(weekDays: List<WeekDayInfo>): String {
        if (weekDays.isEmpty()) return ""
        val first = parseIsoDate(weekDays.first().isoDate) ?: return ""
        val last = parseIsoDate(weekDays.last().isoDate) ?: return ""
        return "${shortFormat.format(first)} – ${friendlyFormat.format(last)}"
    }
}

