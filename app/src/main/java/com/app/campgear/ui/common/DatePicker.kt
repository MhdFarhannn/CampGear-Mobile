package com.app.campgear.ui.common

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.app.campgear.R
import com.app.campgear.core.Formatters
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.textfield.TextInputEditText
import java.util.Calendar
import java.util.TimeZone

fun TextInputEditText.prepareDateInput() {
    keyListener = null
    showSoftInputOnFocus = false
    isFocusable = true
    isFocusableInTouchMode = false
}

/** Listener dipasang kembali ke picker yang dipulihkan FragmentManager setelah rotasi. */
fun Fragment.bindIsoDatePicker(input: TextInputEditText, key: String, initial: () -> String?, onPicked: (String) -> Unit) {
    input.prepareDateInput()
    val manager = childFragmentManager
    val tag = "date_$key"
    manager.setFragmentResultListener(key, viewLifecycleOwner) { _, result ->
        result.getString("date")?.let(onPicked)
    }
    fun attach(picker: MaterialDatePicker<Long>) {
        picker.clearOnPositiveButtonClickListeners()
        picker.addOnPositiveButtonClickListener { millis ->
            manager.setFragmentResult(key, Bundle().apply { putString("date", dateIso(millis)) })
        }
    }
    @Suppress("UNCHECKED_CAST")
    (manager.findFragmentByTag(tag) as? MaterialDatePicker<Long>)?.let(::attach)
    input.setOnClickListener {
        if (manager.findFragmentByTag(tag) != null || manager.isStateSaved) return@setOnClickListener
        val picker = MaterialDatePicker.Builder.datePicker().setTitleText(R.string.date_picker_title)
            .setPositiveButtonText(R.string.action_pick_date).setNegativeButtonText(R.string.action_cancel)
            .setSelection(Formatters.parseIsoDate(initial())?.time ?: MaterialDatePicker.todayInUtcMilliseconds()).build()
        attach(picker)
        picker.show(manager, tag)
    }
}

fun Fragment.bindIsoRangePicker(button: View, initial: () -> Pair<String, String>, onPicked: (String, String) -> Unit) {
    val manager = childFragmentManager
    manager.setFragmentResultListener("report_range", viewLifecycleOwner) { _, result ->
        val from = result.getString("from") ?: return@setFragmentResultListener
        val to = result.getString("to") ?: return@setFragmentResultListener
        onPicked(from, to)
    }
    fun attach(picker: MaterialDatePicker<androidx.core.util.Pair<Long, Long>>) {
        picker.clearOnPositiveButtonClickListeners()
        picker.addOnPositiveButtonClickListener { selection ->
            val from = selection.first ?: return@addOnPositiveButtonClickListener
            val to = selection.second ?: return@addOnPositiveButtonClickListener
            manager.setFragmentResult("report_range", Bundle().apply { putString("from", dateIso(from)); putString("to", dateIso(to)) })
        }
    }
    @Suppress("UNCHECKED_CAST")
    (manager.findFragmentByTag("range_picker") as? MaterialDatePicker<androidx.core.util.Pair<Long, Long>>)?.let(::attach)
    button.setOnClickListener {
        if (manager.findFragmentByTag("range_picker") != null || manager.isStateSaved) return@setOnClickListener
        val (from, to) = initial()
        val picker = MaterialDatePicker.Builder.dateRangePicker().setTitleText(R.string.date_range_title)
            .setPositiveButtonText(R.string.action_pick_date).setNegativeButtonText(R.string.action_cancel)
            .setSelection(androidx.core.util.Pair(Formatters.parseIsoDate(from)?.time, Formatters.parseIsoDate(to)?.time)).build()
        attach(picker)
        picker.show(manager, "range_picker")
    }
}

private fun dateIso(millis: Long) = Formatters.isoFromCalendar(Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = millis })
