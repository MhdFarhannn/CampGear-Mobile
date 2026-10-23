package com.app.campgear.data.repository

import com.app.campgear.data.ApiResult
import com.app.campgear.data.ApiService
import com.app.campgear.data.LaporanPeminjamanResponse
import com.app.campgear.data.LaporanPengembalianResponse
import com.app.campgear.data.apiCall
import okhttp3.ResponseBody

class LaporanRepository(private val api: ApiService) {

    suspend fun getPeminjaman(from: String, to: String): ApiResult<LaporanPeminjamanResponse> =
        apiCall { api.getLaporanPeminjaman(from, to) }

    suspend fun getPengembalian(from: String, to: String): ApiResult<LaporanPengembalianResponse> =
        apiCall { api.getLaporanPengembalian(from, to) }

    suspend fun downloadPeminjamanCsv(from: String, to: String): ApiResult<ResponseBody> =
        apiCall { api.downloadLaporanPeminjaman(from, to) }

    suspend fun downloadPengembalianCsv(from: String, to: String): ApiResult<ResponseBody> =
        apiCall { api.downloadLaporanPengembalian(from, to) }
}
