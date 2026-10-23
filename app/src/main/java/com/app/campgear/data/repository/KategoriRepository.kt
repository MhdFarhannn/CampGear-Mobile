package com.app.campgear.data.repository

import com.app.campgear.data.ApiResult
import com.app.campgear.data.ApiService
import com.app.campgear.data.Kategori
import com.app.campgear.data.KategoriRequest
import com.app.campgear.data.actionCall
import com.app.campgear.data.apiCall

class KategoriRepository(private val api: ApiService) {
    suspend fun getAll(): ApiResult<List<Kategori>> = apiCall { api.getKategori() }

    suspend fun getById(id: Long): ApiResult<Kategori> = apiCall { api.getKategoriById(id) }

    suspend fun create(nama: String): ApiResult<Unit> =
        actionCall { api.createKategori(KategoriRequest(nama)) }

    suspend fun update(id: Long, nama: String): ApiResult<Unit> =
        actionCall { api.updateKategori(id, KategoriRequest(nama)) }

    suspend fun delete(id: Long): ApiResult<Unit> =
        actionCall { api.deleteKategori(id) }
}
