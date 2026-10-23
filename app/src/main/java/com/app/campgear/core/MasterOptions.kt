package com.app.campgear.core

import androidx.annotation.StringRes
import com.app.campgear.R

data class Option(val id: Long, @param:StringRes val labelRes: Int)

object MasterOptions {
    val roles = listOf(
        Option(1, R.string.role_admin),
        Option(2, R.string.role_petugas),
        Option(3, R.string.role_peminjam)
    )

    val genders = listOf(
        Option(1, R.string.gender_male),
        Option(2, R.string.gender_female)
    )

    val statusBarang = listOf(
        Option(1, R.string.condition_baik),
        Option(2, R.string.condition_rusak_ringan),
        Option(3, R.string.condition_rusak_berat)
    )

    val statusKetersediaan = listOf(
        Option(1, R.string.availability_tersedia),
        Option(2, R.string.availability_dipinjam),
        Option(3, R.string.availability_tidak_tersedia)
    )

    val kondisiKembali = listOf(
        Option(1, R.string.condition_baik),
        Option(2, R.string.condition_rusak_ringan),
        Option(3, R.string.condition_rusak_berat),
        Option(4, R.string.condition_hilang)
    )

    const val LATE_FINE_PER_DAY = 10_000.0
    const val FINE_RUSAK_RINGAN = 25_000.0
    const val FINE_RUSAK_BERAT = 100_000.0
    const val FINE_HILANG = 250_000.0

    fun conditionFine(status: String?): Double = when (status) {
        "rusak_ringan" -> FINE_RUSAK_RINGAN
        "rusak_berat" -> FINE_RUSAK_BERAT
        "hilang" -> FINE_HILANG
        else -> 0.0
    }

    fun statusIdByName(name: String?): Long = when (name) {
        "baik" -> 1L
        "rusak_ringan" -> 2L
        "rusak_berat" -> 3L
        "hilang" -> 4L
        else -> 1L
    }
}
