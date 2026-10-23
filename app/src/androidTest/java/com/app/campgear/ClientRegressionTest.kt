package com.app.campgear

import android.graphics.Bitmap
import androidx.core.content.FileProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.navigation.fragment.NavHostFragment
import com.app.campgear.core.AppStrings
import com.app.campgear.ui.admin.copySelectedImage
import com.app.campgear.data.ApiResult
import com.app.campgear.data.LoginResponse
import com.app.campgear.data.LoginUser
import com.app.campgear.data.apiCall
import com.app.campgear.data.parseFailure
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Response
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.io.File

@RunWith(AndroidJUnit4::class)
class ClientRegressionTest {
    private val app get() = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as CampGearApp
    private val sessions get() = app.container.sessionManager

    @Before fun clearSession() { sessions.clear() }

    @Test fun loginEmptyFields_showInlineErrors() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.findViewById<android.view.View>(R.id.buttonLogin).performClick()
                assertNotNull(activity.findViewById<TextInputLayout>(R.id.tilPhone).error)
                assertNotNull(activity.findViewById<TextInputLayout>(R.id.tilPassword).error)
            }
        }
    }

    @Test fun nullSuccessfulGet_isFailureInsteadOfClassCast() = runBlocking {
        val result = apiCall { Response.success<String>(null) }
        assertTrue(result is ApiResult.Failure)
        assertEquals(AppStrings.get(R.string.state_bad_response), (result as ApiResult.Failure).message)
    }

    @Test fun noConnection_andForbiddenHaveClearIndonesianMessages() = runBlocking {
        val result = apiCall<String> { throw IOException("offline") }
        assertEquals(AppStrings.get(R.string.state_no_connection), (result as ApiResult.Failure).message)
        assertEquals(AppStrings.get(R.string.state_forbidden), parseFailure(403, "").message)
        val validation = parseFailure(400, "{\"errors\":{\"Nama\":[\"Nama wajib diisi\"]}}")
        assertEquals("Nama wajib diisi", validation.fieldErrors["nama"])
    }

    @Test fun stale401_doesNotClearNewSession() {
        assertTrue(sessions.save(login("old", System.currentTimeMillis() + 60_000)))
        assertTrue(sessions.save(login("new", System.currentTimeMillis() + 60_000)))
        sessions.clearIfToken("old")
        assertEquals("new", sessions.session.value?.token)
        sessions.clear()
    }

    @Test fun expiredOrMalformedLogin_isRejected() {
        assertFalse(sessions.save(login("token", System.currentTimeMillis() - 1_000)))
        assertFalse(sessions.save(login("", System.currentTimeMillis() + 60_000)))
        assertFalse(sessions.save(login("token", System.currentTimeMillis() + 60_000).copy(user = LoginUser(3, "Test", "Owner"))))
        assertNull(sessions.session.value)
    }

    @Test fun expiryTimer_clearsTokenWithoutAnotherRequest() {
        assertTrue(sessions.save(login("expiry", System.currentTimeMillis() + 500)))
        Thread.sleep(900)
        assertNull(sessions.session.value)
    }

    @Test fun realLoginPerRole_routeAndRotationKeepRole() = runBlocking {
        for ((phone, password, role) in listOf(
            Triple("081200000001", "admin123", "Admin"),
            Triple("081200000002", "petugas123", "Petugas"),
            Triple("081200000003", "peminjam123", "Peminjam")
        )) {
            val result = app.container.authRepository.login(phone, password)
            assertTrue("Backend uji harus aktif: $result", result is ApiResult.Success)
            assertEquals(role, sessions.session.value?.role)
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                Thread.sleep(500)
                scenario.onActivity { activity ->
                    val nav = (activity.supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment).navController
                    assertEquals(R.id.home, nav.currentDestination?.id)
                }
                scenario.recreate()
                scenario.onActivity { assertEquals(role, sessions.session.value?.role) }
            }
            app.container.authRepository.logout()
        }
    }

    @Test fun invalidToken401_clearsSessionAndReturnsLogin() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { assertTrue(sessions.save(login("invalid-token", System.currentTimeMillis() + 60_000))) }
            val deadline = System.currentTimeMillis() + 15_000
            while (sessions.session.value != null && System.currentTimeMillis() < deadline) Thread.sleep(100)
            assertNull("Backend uji harus mengembalikan 401", sessions.session.value)
            Thread.sleep(300)
            scenario.onActivity { activity ->
                val nav = (activity.supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment).navController
                assertEquals(R.id.login, nav.currentDestination?.id)
            }
        }
    }

    @Test fun realRoleRestriction_preservesSession() = runBlocking {
        assertTrue(app.container.authRepository.login("081200000003", "peminjam123") is ApiResult.Success)
        val result = app.container.userRepository.getUsers(1, null)
        assertTrue(result is ApiResult.Failure)
        assertEquals(403, (result as ApiResult.Failure).status)
        assertNotNull(sessions.session.value)
        app.container.authRepository.logout()
    }

    @Test fun selectedPng_isCopiedAndReadable(): Unit = runBlocking {
        val directory = File(app.cacheDir, "images").apply { mkdirs() }
        val source = File(directory, "image-regression.png")
        Bitmap.createBitmap(24, 24, Bitmap.Config.ARGB_8888).let { bitmap ->
            source.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
        try {
            val uri = FileProvider.getUriForFile(app, "${app.packageName}.fileprovider", source)
            val result = copySelectedImage(app, uri)
            assertTrue(result is ApiResult.Success)
            val image = (result as ApiResult.Success).data
            assertEquals("image/png", image.mime)
            assertTrue(image.file.length() > 0)
            assertNotEquals(source.path, image.file.path)
            image.file.delete()
            Unit
        } finally { source.delete() }
    }

    @Test fun selectedImageOverTwoMb_isRejected() = runBlocking {
        val directory = File(app.cacheDir, "images").apply { mkdirs() }
        val source = File(directory, "large-image-regression.png")
        source.outputStream().use { it.write(ByteArray(2 * 1024 * 1024 + 1)) }
        try {
            val uri = FileProvider.getUriForFile(app, "${app.packageName}.fileprovider", source)
            val result = copySelectedImage(app, uri)
            assertTrue(result is ApiResult.Failure)
            assertEquals(app.getString(R.string.alat_image_too_large), (result as ApiResult.Failure).message)
        } finally { source.delete() }
    }

    private fun login(token: String, expiry: Long) = LoginResponse(
        token = token,
        expiresAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date(expiry)),
        user = LoginUser(3, "Peminjam Test", "Peminjam")
    )
}
