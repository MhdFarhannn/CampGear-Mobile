package com.app.campgear.data.repository

import com.app.campgear.core.SessionManager
import com.app.campgear.data.ApiResult
import com.app.campgear.data.ApiService
import com.app.campgear.data.LoginRequest
import com.app.campgear.data.LoginResponse
import com.app.campgear.data.UserResponse
import com.app.campgear.data.actionCall
import com.app.campgear.data.apiCall
import com.app.campgear.R
import com.app.campgear.core.AppStrings
import kotlinx.coroutines.withTimeoutOrNull

class AuthRepository(
    private val api: ApiService,
    private val sessionManager: SessionManager
) {
    suspend fun login(noTelepon: String, password: String): ApiResult<LoginResponse> {
        val result = apiCall { api.login(LoginRequest(noTelepon, password)) }
        if (result is ApiResult.Success) {
            if (!sessionManager.save(result.data)) return ApiResult.Failure(AppStrings.get(R.string.state_bad_response))
        }
        return result
    }

    suspend fun me(): ApiResult<UserResponse> = apiCall { api.me() }

    suspend fun logout() {
        val token = sessionManager.session.value?.token ?: return
        try {
            withTimeoutOrNull(5_000) { actionCall { api.logout() } }
        } finally {
            sessionManager.clearIfToken(token)
        }
    }
}
