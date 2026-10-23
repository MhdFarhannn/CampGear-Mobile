@file:Suppress("DEPRECATION")

package com.app.campgear.core

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.app.campgear.data.LoginResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.security.KeyStore

data class Session(val userId: Long, val nama: String, val role: String, val token: String, val expiresAtMillis: Long)

class SessionManager(context: Context) {
    private val prefs = createPreferences(context.applicationContext)
    private val _session = MutableStateFlow(readValidSession())
    val session: StateFlow<Session?> = _session.asStateFlow()
    private val _endMessage = MutableStateFlow<String?>(null)
    val endMessage = _endMessage.asStateFlow()
    private val expiryScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var expiryJob: Job? = null

    init { scheduleExpiry() }

    @Synchronized
    fun save(login: LoginResponse): Boolean {
        val expiresAt = Formatters.parseApiDateTime(login.expiresAt) ?: return false
        if (login.token.isBlank() || login.user.id <= 0 || login.user.nama.isBlank() ||
            login.user.role !in ROLES || expiresAt <= System.currentTimeMillis()
        ) return false
        prefs?.edit {
            putLong(KEY_USER_ID, login.user.id)
            putString(KEY_NAME, login.user.nama)
            putString(KEY_ROLE, login.user.role)
            putString(KEY_TOKEN, login.token)
            putLong(KEY_EXPIRES_AT, expiresAt)
        }
        _session.value = Session(login.user.id, login.user.nama, login.user.role, login.token, expiresAt)
        _endMessage.value = null
        scheduleExpiry()
        return true
    }

    fun getValidToken(): String? {
        expireIfNeeded()
        return _session.value?.token
    }

    fun expireIfNeeded() {
        _session.value?.let { if (it.expiresAtMillis <= System.currentTimeMillis()) clearIfToken(it.token, AppStrings.get(com.app.campgear.R.string.state_unauthorized)) }
    }

    @Synchronized
    fun clearIfToken(token: String, message: String? = null) {
        if (_session.value?.token == token) {
            _endMessage.value = message
            clear()
        }
    }

    @Synchronized
    fun clear() {
        expiryJob?.cancel()
        prefs?.edit { clear() }
        _session.value = null
    }

    private fun scheduleExpiry() {
        expiryJob?.cancel()
        val current = _session.value ?: return
        expiryJob = expiryScope.launch {
            delay((current.expiresAtMillis - System.currentTimeMillis()).coerceAtLeast(0))
            clearIfToken(current.token, AppStrings.get(com.app.campgear.R.string.state_unauthorized))
        }
    }

    fun consumeEndMessage() { _endMessage.value = null }

    private fun readValidSession(): Session? = runCatching {
        val storage = prefs ?: return null
        val token = storage.getString(KEY_TOKEN, null)?.takeIf { it.isNotBlank() } ?: return null
        val expiry = storage.getLong(KEY_EXPIRES_AT, 0)
        val id = storage.getLong(KEY_USER_ID, 0)
        val role = storage.getString(KEY_ROLE, "").orEmpty()
        val name = storage.getString(KEY_NAME, "").orEmpty()
        if (expiry <= System.currentTimeMillis() || id <= 0 || role !in ROLES || name.isBlank()) {
            storage.edit { clear() }
            null
        } else Session(id, name, role, token, expiry)
    }.getOrNull()

    private fun createPreferences(context: Context): SharedPreferences? {
        fun create(): SharedPreferences = EncryptedSharedPreferences.create(
            context, PREFS, MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        return runCatching { create() }.getOrElse {
            // Backup/kunci rusak dipulihkan sebagai sesi kosong, bukan menyebabkan crash saat start.
            context.deleteSharedPreferences(PREFS)
            runCatching {
                KeyStore.getInstance("AndroidKeyStore").apply {
                    load(null)
                    deleteEntry(MasterKey.DEFAULT_MASTER_KEY_ALIAS)
                }
                create()
            }.getOrNull()
        }
    }

    private companion object {
        const val PREFS = "campgear_session"
        const val KEY_USER_ID = "user_id"
        const val KEY_NAME = "name"
        const val KEY_ROLE = "role"
        const val KEY_TOKEN = "token"
        const val KEY_EXPIRES_AT = "expires_at"
        val ROLES = setOf("Admin", "Petugas", "Peminjam")
    }
}
