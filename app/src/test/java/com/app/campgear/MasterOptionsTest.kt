package com.app.campgear

import com.app.campgear.core.MasterOptions
import org.junit.Assert.assertEquals
import org.junit.Test

class MasterOptionsTest {

    @Test
    fun statusIdByName_mapsBackendStatuses() {
        assertEquals(1L, MasterOptions.statusIdByName("baik"))
        assertEquals(2L, MasterOptions.statusIdByName("rusak_ringan"))
        assertEquals(3L, MasterOptions.statusIdByName("rusak_berat"))
        assertEquals(4L, MasterOptions.statusIdByName("hilang"))
        assertEquals(1L, MasterOptions.statusIdByName(null))
    }

    @Test
    fun conditionFine_matchesBackendSettings() {
        assertEquals(0.0, MasterOptions.conditionFine("baik"), 0.0)
        assertEquals(25_000.0, MasterOptions.conditionFine("rusak_ringan"), 0.0)
        assertEquals(100_000.0, MasterOptions.conditionFine("rusak_berat"), 0.0)
        assertEquals(250_000.0, MasterOptions.conditionFine("hilang"), 0.0)
    }
}
