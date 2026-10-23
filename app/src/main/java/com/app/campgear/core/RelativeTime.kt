package com.app.campgear.core

import android.content.Context
import com.app.campgear.R

object RelativeTime {
    fun format(context: Context, raw: String): String {
        val millis = Formatters.parseApiDateTime(raw) ?: return "—"
        val age = (System.currentTimeMillis() - millis).coerceAtLeast(0)
        return when {
            age < 60_000 -> context.getString(R.string.time_just_now)
            age < 3_600_000 -> context.resources.getQuantityString(R.plurals.time_minutes, (age / 60_000).toInt(), age / 60_000)
            age < 86_400_000 -> context.resources.getQuantityString(R.plurals.time_hours, (age / 3_600_000).toInt(), age / 3_600_000)
            age < 604_800_000 -> context.resources.getQuantityString(R.plurals.time_days, (age / 86_400_000).toInt(), age / 86_400_000)
            else -> Formatters.displayDate(raw)
        }
    }
}
