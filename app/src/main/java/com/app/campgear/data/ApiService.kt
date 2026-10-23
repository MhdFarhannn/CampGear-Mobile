package com.app.campgear.data

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

interface ApiService {

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @GET("auth/me")
    suspend fun me(): Response<UserResponse>

    @POST("auth/logout")
    suspend fun logout(): Response<Unit>

    @GET("users")
    suspend fun getUsers(
        @Query("page") page: Int,
        @Query("pageSize") pageSize: Int,
        @Query("search") search: String?,
        @Query("idRole") idRole: Long?
    ): Response<PagedResponse<UserResponse>>

    @GET("users/{id}")
    suspend fun getUserById(@Path("id") id: Long): Response<UserResponse>

    @POST("users")
    suspend fun createUser(@Body request: UserRequest): Response<Unit>

    @PUT("users/{id}")
    suspend fun updateUser(@Path("id") id: Long, @Body request: UserRequest): Response<Unit>

    @DELETE("users/{id}")
    suspend fun deleteUser(@Path("id") id: Long): Response<Unit>

    @GET("kategori")
    suspend fun getKategori(): Response<List<Kategori>>

    @GET("kategori/{id}")
    suspend fun getKategoriById(@Path("id") id: Long): Response<Kategori>

    @POST("kategori")
    suspend fun createKategori(@Body request: KategoriRequest): Response<Unit>

    @PUT("kategori/{id}")
    suspend fun updateKategori(@Path("id") id: Long, @Body request: KategoriRequest): Response<Unit>

    @DELETE("kategori/{id}")
    suspend fun deleteKategori(@Path("id") id: Long): Response<Unit>

    @GET("alat")
    suspend fun getAlat(
        @Query("page") page: Int,
        @Query("pageSize") pageSize: Int,
        @Query("search") search: String?,
        @Query("idKategori") idKategori: Long?
    ): Response<PagedResponse<AlatResponse>>

    @GET("alat/{id}")
    suspend fun getAlatById(@Path("id") id: Long): Response<AlatResponse>

    @Multipart
    @POST("alat")
    suspend fun createAlat(
        @Part("idKategori") idKategori: RequestBody,
        @Part("nama") nama: RequestBody,
        @Part("deskripsi") deskripsi: RequestBody?,
        @Part("hargaSewa") hargaSewa: RequestBody,
        @Part gambar: MultipartBody.Part?
    ): Response<Unit>

    @Multipart
    @PUT("alat/{id}")
    suspend fun updateAlat(
        @Path("id") id: Long,
        @Part("idKategori") idKategori: RequestBody,
        @Part("nama") nama: RequestBody,
        @Part("deskripsi") deskripsi: RequestBody?,
        @Part("hargaSewa") hargaSewa: RequestBody,
        @Part gambar: MultipartBody.Part?
    ): Response<Unit>

    @DELETE("alat/{id}")
    suspend fun deleteAlat(@Path("id") id: Long): Response<Unit>

    @GET("unit-alat")
    suspend fun getUnitAlat(
        @Query("page") page: Int,
        @Query("pageSize") pageSize: Int,
        @Query("idAlat") idAlat: Long?,
        @Query("idStatusKetersediaan") idStatusKetersediaan: Long? = null
    ): Response<PagedResponse<UnitAlatResponse>>

    @GET("unit-alat/{id}")
    suspend fun getUnitAlatById(@Path("id") id: Long): Response<UnitAlatResponse>

    @POST("unit-alat")
    suspend fun createUnitAlat(@Body request: UnitAlatRequest): Response<Unit>

    @PUT("unit-alat/{id}")
    suspend fun updateUnitAlat(
        @Path("id") id: Long,
        @Body request: UnitAlatUpdateRequest
    ): Response<Unit>

    @DELETE("unit-alat/{id}")
    suspend fun deleteUnitAlat(@Path("id") id: Long): Response<Unit>

    @GET("peminjaman")
    suspend fun getPeminjaman(
        @Query("page") page: Int,
        @Query("pageSize") pageSize: Int,
        @Query("status") status: String?,
        @Query("from") from: String?,
        @Query("to") to: String?,
        @Query("idPeminjam") idPeminjam: Long? = null
    ): Response<PagedResponse<PeminjamanResponse>>

    @GET("peminjaman/mine")
    suspend fun getMyPeminjaman(
        @Query("page") page: Int,
        @Query("pageSize") pageSize: Int,
        @Query("status") status: String?
    ): Response<PagedResponse<PeminjamanResponse>>

    @GET("peminjaman/monitor")
    suspend fun getMonitor(
        @Query("page") page: Int,
        @Query("pageSize") pageSize: Int,
        @Query("status") status: String?
    ): Response<PagedResponse<MonitorPeminjaman>>

    @GET("peminjaman/{id}")
    suspend fun getPeminjamanById(@Path("id") id: Long): Response<PeminjamanResponse>

    @POST("peminjaman")
    suspend fun createPeminjaman(@Body request: PeminjamanRequest): Response<Unit>

    @PUT("peminjaman/{id}")
    suspend fun updatePeminjaman(
        @Path("id") id: Long,
        @Body request: PeminjamanRequest
    ): Response<Unit>

    @DELETE("peminjaman/{id}")
    suspend fun deletePeminjaman(@Path("id") id: Long): Response<Unit>

    @PATCH("peminjaman/{id}/approve")
    suspend fun approvePeminjaman(@Path("id") id: Long): Response<Unit>

    @PATCH("peminjaman/{id}/reject")
    suspend fun rejectPeminjaman(@Path("id") id: Long, @Body request: RejectRequest): Response<Unit>

    @PATCH("peminjaman/{id}/borrow")
    suspend fun borrowPeminjaman(@Path("id") id: Long): Response<Unit>

    @POST("pengembalian")
    suspend fun createPengembalian(@Body request: PengembalianRequest): Response<Unit>

    @GET("pengembalian")
    suspend fun getPengembalian(
        @Query("page") page: Int,
        @Query("pageSize") pageSize: Int,
        @Query("from") from: String?,
        @Query("to") to: String?
    ): Response<PagedResponse<PengembalianResponse>>

    @GET("pengembalian/{id}")
    suspend fun getPengembalianById(@Path("id") id: Long): Response<PengembalianResponse>

    @GET("log-aktivitas")
    suspend fun getLogAktivitas(
        @Query("page") page: Int,
        @Query("pageSize") pageSize: Int,
        @Query("idUser") idUser: Long?,
        @Query("from") from: String?,
        @Query("to") to: String?
    ): Response<PagedResponse<LogAktivitas>>

    @GET("laporan/peminjaman")
    suspend fun getLaporanPeminjaman(
        @Query("from") from: String,
        @Query("to") to: String
    ): Response<LaporanPeminjamanResponse>

    @GET("laporan/pengembalian")
    suspend fun getLaporanPengembalian(
        @Query("from") from: String,
        @Query("to") to: String
    ): Response<LaporanPengembalianResponse>

    @Streaming
    @GET("laporan/peminjaman")
    suspend fun downloadLaporanPeminjaman(
        @Query("from") from: String,
        @Query("to") to: String,
        @Query("format") format: String = "csv"
    ): Response<ResponseBody>

    @Streaming
    @GET("laporan/pengembalian")
    suspend fun downloadLaporanPengembalian(
        @Query("from") from: String,
        @Query("to") to: String,
        @Query("format") format: String = "csv"
    ): Response<ResponseBody>
}
