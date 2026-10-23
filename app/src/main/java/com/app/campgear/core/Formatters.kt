package com.app.campgear.core

import java.text.NumberFormat
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Currency
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

object Formatters {
    private val indonesian = Locale.forLanguageTag("id-ID")
    private val utc = TimeZone.getTimeZone("UTC")
    private val datePattern = Regex("\\d{4}-\\d{2}-\\d{2}")
    private val timestampPattern = Regex(
        "^(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2})(?:\\.(\\d{1,9}))?(Z|[+-]\\d{2}:\\d{2})?$"
    )

    fun rupiah(value: Double): String = NumberFormat.getCurrencyInstance(indonesian).apply {
        currency = Currency.getInstance("IDR")
        maximumFractionDigits = 0
        minimumFractionDigits = 0
    }.format(if (value.isFinite()) value else 0.0)

    fun displayDate(raw: String?): String {
        val date = parseIsoDate(raw?.take(10)) ?: return "-"
        return SimpleDateFormat("dd MMM yyyy", indonesian).apply { timeZone = utc }.format(date)
    }

    fun displayDateTime(raw: String?): String {
        val millis = parseApiDateTime(raw) ?: return "-"
        return SimpleDateFormat("dd MMM yyyy, HH:mm", indonesian).format(Date(millis))
    }

    fun parseIsoDate(raw: String?): Date? {
        val value = raw?.takeIf { datePattern.matches(it) } ?: return null
        return parseStrict(value, "yyyy-MM-dd", utc)
    }

    fun isValidDate(raw: String?): Boolean = parseIsoDate(raw) != null

    fun isoFromCalendar(calendar: Calendar): String = String.format(
        Locale.US, "%04d-%02d-%02d", calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH)
    )

    fun calendarFromIso(raw: String?): Calendar = Calendar.getInstance(utc).apply {
        parseIsoDate(raw)?.let { time = it }
    }

    fun todayIso(): String = isoFromCalendar(Calendar.getInstance())

    fun addDaysIso(baseIso: String, days: Int): String = calendarFromIso(baseIso).apply {
        add(Calendar.DAY_OF_MONTH, days)
    }.let(::isoFromCalendar)

    fun firstDayOfMonthIso(): String = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
    }.let(::isoFromCalendar)

    fun daysBetween(startIso: String?, endIso: String?): Int =
        dateDifference(startIso, endIso).coerceAtLeast(1)

    fun lateDays(dueIso: String?, returnIso: String?): Int =
        dateDifference(dueIso, returnIso).coerceAtLeast(0)

    private fun dateDifference(startIso: String?, endIso: String?): Int {
        val start = parseIsoDate(startIso) ?: return 0
        val end = parseIsoDate(endIso) ?: return 0
        return TimeUnit.MILLISECONDS.toDays(end.time - start.time).toInt()
    }

    fun parseApiDateTime(raw: String?): Long? {
        val match = raw?.let(timestampPattern::matchEntire) ?: return null
        val date = match.groupValues[1]
        // .NET mengirim hingga tujuh digit pecahan; SimpleDateFormat hanya memahami milidetik.
        val fraction = match.groupValues[2].take(3).padEnd(3, '0')
        val offset = match.groupValues[3]
        val normalized = "$date.$fraction$offset"
        val pattern = if (offset.isEmpty()) "yyyy-MM-dd'T'HH:mm:ss.SSS" else "yyyy-MM-dd'T'HH:mm:ss.SSSXXX"
        return parseStrict(normalized, pattern, TimeZone.getDefault())?.time
    }

    private fun parseStrict(value: String, pattern: String, zone: TimeZone): Date? {
        val formatter = SimpleDateFormat(pattern, Locale.US).apply {
            isLenient = false
            timeZone = zone
        }
        val position = ParsePosition(0)
        val parsed = formatter.parse(value, position)
        return parsed?.takeIf { position.index == value.length }
    }
}
