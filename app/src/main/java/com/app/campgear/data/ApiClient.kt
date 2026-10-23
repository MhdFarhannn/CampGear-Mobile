package com.app.campgear.data

import com.app.campgear.BuildConfig
import com.app.campgear.core.SessionManager
import com.google.gson.GsonBuilder
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class AuthInterceptor(private val sessionManager: SessionManager) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val isLogin = chain.request().url.encodedPath.trimEnd('/').endsWith("/auth/login")
        val token = if (isLogin) null else sessionManager.getValidToken()
        val request = if (token != null) {
            chain.request().newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }

        val response = chain.proceed(request)

        if (response.code == 401 && token != null) {
            sessionManager.clearIfToken(token, com.app.campgear.core.AppStrings.get(com.app.campgear.R.string.state_unauthorized))
        }

        return response
    }
}

object ApiClient {
    fun create(sessionManager: SessionManager): ApiService {
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(sessionManager))
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .callTimeout(45, TimeUnit.SECONDS)
            .retryOnConnectionFailure(false)
            .build()

        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(
                GsonBuilder().registerTypeAdapterFactory(ResponseContracts()).create()
            ))
            .build()
            .create(ApiService::class.java)
    }
}
