package com.app.campgear.core

import android.content.Context
import com.app.campgear.data.ApiClient
import com.app.campgear.data.repository.AlatRepository
import com.app.campgear.data.repository.AuthRepository
import com.app.campgear.data.repository.KategoriRepository
import com.app.campgear.data.repository.LaporanRepository
import com.app.campgear.data.repository.LogRepository
import com.app.campgear.data.repository.PeminjamanRepository
import com.app.campgear.data.repository.PengembalianRepository
import com.app.campgear.data.repository.UnitAlatRepository
import com.app.campgear.data.repository.UserRepository
import com.app.campgear.data.repository.DashboardRepository

class AppContainer(context: Context) {
    val applicationContext: Context = context.applicationContext

    val sessionManager = SessionManager(context)
    private val api = ApiClient.create(sessionManager)

    val authRepository = AuthRepository(api, sessionManager)
    val userRepository = UserRepository(api)
    val kategoriRepository = KategoriRepository(api)
    val alatRepository = AlatRepository(api)
    val unitAlatRepository = UnitAlatRepository(api)
    val peminjamanRepository = PeminjamanRepository(api)
    val pengembalianRepository = PengembalianRepository(api)
    val logRepository = LogRepository(api)
    val laporanRepository = LaporanRepository(api)
    val dashboardRepository = DashboardRepository(api)

    val viewModelFactory = ViewModelFactory(this)
}
