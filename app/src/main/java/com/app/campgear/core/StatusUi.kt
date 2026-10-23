package com.app.campgear.core

import android.content.Context
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import com.app.campgear.R

object StatusUi {

    @StringRes
    fun labelRes(status: String?): Int = when (status) {
        "diajukan" -> R.string.status_diajukan
        "disetujui" -> R.string.status_disetujui
        "ditolak" -> R.string.status_ditolak
        "dipinjam" -> R.string.status_dipinjam
        "selesai" -> R.string.status_selesai
        "baik" -> R.string.condition_baik
        "rusak_ringan" -> R.string.condition_rusak_ringan
        "rusak_berat" -> R.string.condition_rusak_berat
        "hilang" -> R.string.condition_hilang
        "tersedia" -> R.string.availability_tersedia
        "tidak_tersedia" -> R.string.availability_tidak_tersedia
        else -> R.string.label_status
    }

    @ColorRes
    private fun textColorRes(status: String?): Int = when (status) {
        "selesai", "baik", "tersedia" -> R.color.status_green
        "disetujui" -> R.color.status_blue
        "diajukan", "rusak_ringan" -> R.color.status_orange
        "ditolak", "rusak_berat", "tidak_tersedia" -> R.color.status_red
        "hilang" -> R.color.status_gray
        "dipinjam" -> R.color.status_teal
        else -> R.color.status_gray
    }

    @ColorRes
    private fun backgroundColorRes(status: String?): Int = when (status) {
        "selesai", "baik", "tersedia" -> R.color.status_green_bg
        "disetujui" -> R.color.status_blue_bg
        "diajukan", "rusak_ringan" -> R.color.status_orange_bg
        "ditolak", "rusak_berat", "tidak_tersedia" -> R.color.status_red_bg
        "hilang" -> R.color.status_gray_bg
        "dipinjam" -> R.color.status_teal_bg
        else -> R.color.status_gray_bg
    }

    fun textColor(context: Context, status: String?): Int =
        ContextCompat.getColor(context, textColorRes(status))

    fun backgroundColor(context: Context, status: String?): Int =
        ContextCompat.getColor(context, backgroundColorRes(status))

    fun bind(view: TextView, status: String?) {
        view.text = view.context.getString(labelRes(status))
        view.setTextColor(textColor(view.context, status))
        view.setBackgroundResource(R.drawable.bg_status_chip)
        view.backgroundTintList = android.content.res.ColorStateList.valueOf(
            backgroundColor(view.context, status)
        )
    }

    fun bindConditionLabel(view: TextView, status: String?, prefixRes: Int? = null) {
        val label = view.context.getString(labelRes(status))
        view.text = if (prefixRes != null) {
            view.context.getString(prefixRes, label)
        } else {
            label
        }
    }
}
