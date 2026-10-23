package com.app.campgear.data.repository

import com.app.campgear.data.AlatResponse
import com.app.campgear.data.ApiResult
import com.app.campgear.data.ApiService
import com.app.campgear.data.PagedResponse
import com.app.campgear.data.actionCall
import com.app.campgear.data.apiCall
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class AlatRepository(private val api: ApiService) {

    suspend fun getAlat(
        page: Int,
        search: String?,
        idKategori: Long?
    ): ApiResult<PagedResponse<AlatResponse>> =
        apiCall { api.getAlat(page, PAGE_SIZE, search, idKategori) }

    suspend fun getAlatById(id: Long): ApiResult<AlatResponse> =
        apiCall { api.getAlatById(id) }

    suspend fun create(
        idKategori: Long,
        nama: String,
        deskripsi: String?,
        hargaSewa: String,
        gambar: File?,
        gambarMime: String?
    ): ApiResult<Unit> = actionCall {
        api.createAlat(
            idKategori = idKategori.toRequestBody(),
            nama = nama.toRequestBody(),
            deskripsi = deskripsi?.toRequestBody(),
            hargaSewa = hargaSewa.toRequestBody(),
            gambar = gambar.toPart(gambarMime)
        )
    }

    suspend fun update(
        id: Long,
        idKategori: Long,
        nama: String,
        deskripsi: String?,
        hargaSewa: String,
        gambar: File?,
        gambarMime: String?
    ): ApiResult<Unit> = actionCall {
        api.updateAlat(
            id = id,
            idKategori = idKategori.toRequestBody(),
            nama = nama.toRequestBody(),
            deskripsi = deskripsi?.toRequestBody(),
            hargaSewa = hargaSewa.toRequestBody(),
            gambar = gambar.toPart(gambarMime)
        )
    }

    suspend fun delete(id: Long): ApiResult<Unit> = actionCall { api.deleteAlat(id) }

    private fun String.toRequestBody(): RequestBody =
        toPlainRequestBody()

    private fun Long.toRequestBody(): RequestBody =
        toString().toPlainRequestBody()

    private fun String.toPlainRequestBody(): RequestBody =
        toRequestBody("text/plain".toMediaType())

    private fun File?.toPart(mime: String?): MultipartBody.Part? {
        if (this == null) return null
        val mediaType = (mime ?: "image/jpeg").toMediaType()
        return MultipartBody.Part.createFormData("gambar", name, asRequestBody(mediaType))
    }

    companion object {
        const val PAGE_SIZE = 20
    }
}
