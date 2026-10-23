package com.app.campgear.data

import com.app.campgear.R
import com.app.campgear.core.AppStrings
import com.google.gson.JsonParser
import kotlinx.coroutines.CancellationException
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException

sealed interface ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>
    data class Failure(
        val message: String,
        val status: Int? = null,
        val fieldErrors: Map<String, String> = emptyMap()
    ) : ApiResult<Nothing>
}

suspend fun <T : Any> apiCall(block: suspend () -> Response<T>): ApiResult<T> = guardedCall {
    val response = block()
    if (response.isSuccessful) {
        response.body()?.let { ApiResult.Success(it) }
            ?: ApiResult.Failure(AppStrings.get(R.string.state_bad_response), response.code())
    } else parseFailure(response.code(), response.errorBody()?.use { it.string() })
}

suspend fun actionCall(block: suspend () -> Response<Unit>): ApiResult<Unit> = guardedCall {
    val response = block()
    if (response.isSuccessful) ApiResult.Success(Unit)
    else parseFailure(response.code(), response.errorBody()?.use { it.string() })
}

private suspend fun <T> guardedCall(block: suspend () -> ApiResult<T>): ApiResult<T> = try {
    block()
} catch (e: CancellationException) {
    throw e
} catch (e: SocketTimeoutException) {
    ApiResult.Failure(AppStrings.get(R.string.state_timeout))
} catch (e: IOException) {
    ApiResult.Failure(AppStrings.get(R.string.state_no_connection))
} catch (e: Exception) {
    ApiResult.Failure(AppStrings.get(R.string.state_bad_response))
}

fun parseFailure(status: Int, raw: String?): ApiResult.Failure {
    val json = runCatching { JsonParser.parseString(raw.orEmpty()).asJsonObject }.getOrNull()
    val detail = runCatching { json?.get("detail")?.takeUnless { it.isJsonNull }?.asString }.getOrNull()
    val fields = runCatching {
        json?.getAsJsonObject("errors")?.entrySet()?.associate { (key, value) ->
            key.replaceFirstChar { it.lowercase() } to if (value.isJsonArray) {
                value.asJsonArray.mapNotNull { it.takeUnless { item -> item.isJsonNull }?.asString }
                    .joinToString("\n")
            } else value.asString
        }.orEmpty()
    }.getOrDefault(emptyMap())
    val message = when {
        status == 401 && detail?.contains("password", ignoreCase = true) == true -> AppStrings.get(R.string.login_invalid_credentials)
        status == 401 -> AppStrings.get(R.string.state_unauthorized)
        status == 403 -> AppStrings.get(R.string.state_forbidden)
        fields.isNotEmpty() -> fields.values.joinToString("\n")
        !detail.isNullOrBlank() && status < 500 -> humanize(detail)
        status >= 500 -> AppStrings.get(R.string.state_server_error)
        status == 404 -> AppStrings.get(R.string.state_not_found)
        else -> AppStrings.get(R.string.state_generic_error)
    }
    return ApiResult.Failure(message, status, fields)
}

private fun humanize(detail: String): String = when {
    detail.contains("already registered", true) -> AppStrings.get(R.string.error_phone_duplicate)
    detail.contains("Kategori already exists", true) -> AppStrings.get(R.string.error_category_duplicate)
    detail.contains("kodeInventaris is already used", true) -> AppStrings.get(R.string.error_unit_duplicate)
    detail.contains("cannot be deleted", true) -> AppStrings.get(R.string.error_delete_referenced)
    detail.contains("currently borrowed", true) -> AppStrings.get(R.string.units_borrowed_locked)
    detail.contains("Not enough available units", true) -> AppStrings.get(R.string.error_stock_changed)
    detail.contains("Only peminjaman with status", true) || detail.contains("already been returned", true) -> AppStrings.get(R.string.error_loan_state_changed)
    detail.contains("gambar must not exceed", true) -> AppStrings.get(R.string.alat_image_too_large)
    detail.contains("gambar must be", true) -> AppStrings.get(R.string.alat_image_invalid_type)
    detail.contains("not found", true) -> AppStrings.get(R.string.state_not_found)
    else -> detail
}
