package com.app.campgear

import com.app.campgear.data.PeminjamanResponse
import com.app.campgear.ui.admin.EditLoanItem
import com.app.campgear.ui.admin.EditLoanUiState
import com.app.campgear.ui.loan.ReturnFormUiState
import com.app.campgear.ui.loan.ReturnUnitInput
import org.junit.Assert.assertEquals
import org.junit.Test

class PricingTest {

    @Test
    fun returnEstimate_combinesLateAndConditionFines() {
        val state = ReturnFormUiState(
            loan = PeminjamanResponse(tanggalPengembalian = "2026-10-20"),
            returnDate = "2026-10-22",
            units = listOf(
                ReturnUnitInput(1, "TND-001", "Tenda", kondisi = "baik"),
                ReturnUnitInput(2, "TND-002", "Tenda", kondisi = "rusak_ringan")
            )
        )

        assertEquals(2, state.lateDays)
        assertEquals(20_000.0, state.lateFine, 0.0)
        assertEquals(25_000.0, state.conditionFine, 0.0)
        assertEquals(45_000.0, state.estimatedFine, 0.0)
    }

    @Test
    fun returnEstimate_isZeroWhenOnTimeAndGoodCondition() {
        val state = ReturnFormUiState(
            loan = PeminjamanResponse(tanggalPengembalian = "2026-10-22"),
            returnDate = "2026-10-22",
            units = listOf(ReturnUnitInput(1, "TND-001", "Tenda"))
        )

        assertEquals(0, state.lateDays)
        assertEquals(0.0, state.estimatedFine, 0.0)
    }

    @Test
    fun editLoanTotal_usesCurrentPriceAndDuration() {
        val state = EditLoanUiState(
            items = listOf(EditLoanItem(1, "Tenda Dome 4P", 50_000.0, 2)),
            loanDate = "2026-10-22",
            returnDate = "2026-10-25"
        )

        assertEquals(3, state.days)
        assertEquals(300_000.0, state.total, 0.0)
    }
}
