package com.app.campgear

import com.app.campgear.core.ImageUrls
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ImageUrlsTest {

    private val base = "http://10.0.2.2:5162/api/v1/"

    @Test
    fun resolve_buildsAbsoluteUrlFromRelativePath() {
        assertEquals(
            "http://10.0.2.2:5162/uploads/alat/tenda.png",
            ImageUrls.resolve("/uploads/alat/tenda.png", base)
        )
        assertEquals(
            "http://10.0.2.2:5162/uploads/alat/tenda.png",
            ImageUrls.resolve("uploads/alat/tenda.png", base)
        )
    }

    @Test
    fun resolve_keepsAbsoluteUrlUntouched() {
        val absolute = "https://cdn.example.com/alat.png"
        assertEquals(absolute, ImageUrls.resolve(absolute, base))
    }

    @Test
    fun resolve_returnsNullForBlankPath() {
        assertNull(ImageUrls.resolve(null, base))
        assertNull(ImageUrls.resolve("", base))
        assertNull(ImageUrls.resolve("   ", base))
    }
}
