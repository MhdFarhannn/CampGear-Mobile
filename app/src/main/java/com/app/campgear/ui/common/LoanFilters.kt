package com.app.campgear.ui.common

import com.app.campgear.R

fun loanStatusChip(status: String?): Int = when (status) {
    "diajukan" -> R.id.chipDiajukan
    "disetujui" -> R.id.chipDisetujui
    "ditolak" -> R.id.chipDitolak
    "dipinjam" -> R.id.chipDipinjam
    "selesai" -> R.id.chipSelesai
    else -> R.id.chipAll
}
