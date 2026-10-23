package com.app.campgear.core

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import com.app.campgear.ui.admin.MasterFormViewModel
import com.app.campgear.ui.admin.AlatFormViewModel
import com.app.campgear.ui.admin.AlatManageViewModel
import com.app.campgear.ui.admin.EditLoanViewModel
import com.app.campgear.ui.admin.KategoriViewModel
import com.app.campgear.ui.admin.LogAktivitasViewModel
import com.app.campgear.ui.admin.UnitAlatViewModel
import com.app.campgear.ui.admin.UsersViewModel
import com.app.campgear.ui.auth.LoginViewModel
import com.app.campgear.ui.loan.LoanDetailViewModel
import com.app.campgear.ui.loan.ReturnFormViewModel
import com.app.campgear.ui.peminjam.BorrowFormViewModel
import com.app.campgear.ui.peminjam.CartViewModel
import com.app.campgear.ui.peminjam.CatalogViewModel
import com.app.campgear.ui.peminjam.MyLoansViewModel
import com.app.campgear.ui.peminjam.EquipmentDetailViewModel
import com.app.campgear.ui.profile.ProfileViewModel
import com.app.campgear.ui.reports.ReportViewModel
import com.app.campgear.ui.returns.ReturnDetailViewModel
import com.app.campgear.ui.returns.ReturnsViewModel
import com.app.campgear.ui.staff.StaffMonitorViewModel
import com.app.campgear.ui.staff.StaffRequestsViewModel
import com.app.campgear.ui.home.DashboardViewModel

class ViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T =
        createViewModel(modelClass, extras.createSavedStateHandle())

    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        error("Gunakan factory dengan CreationExtras untuk ${modelClass.simpleName}")

    @Suppress("UNCHECKED_CAST")
    private fun <T : ViewModel> createViewModel(modelClass: Class<T>, savedState: SavedStateHandle): T {
        val viewModel: ViewModel = when {
            modelClass.isAssignableFrom(DashboardViewModel::class.java) -> DashboardViewModel(container.dashboardRepository, container.sessionManager)
            modelClass.isAssignableFrom(EquipmentDetailViewModel::class.java) -> EquipmentDetailViewModel(container.alatRepository)
            modelClass.isAssignableFrom(MasterFormViewModel::class.java) ->
                MasterFormViewModel(container.userRepository, container.kategoriRepository, container.unitAlatRepository, savedState)
            modelClass.isAssignableFrom(LoginViewModel::class.java) ->
                LoginViewModel(container.authRepository)

            modelClass.isAssignableFrom(ProfileViewModel::class.java) ->
                ProfileViewModel(container.authRepository)

            modelClass.isAssignableFrom(CatalogViewModel::class.java) ->
                CatalogViewModel(container.alatRepository, container.kategoriRepository)

            modelClass.isAssignableFrom(CartViewModel::class.java) ->
                CartViewModel(container.alatRepository)

            modelClass.isAssignableFrom(BorrowFormViewModel::class.java) ->
                BorrowFormViewModel(container.peminjamanRepository)

            modelClass.isAssignableFrom(MyLoansViewModel::class.java) ->
                MyLoansViewModel(container.peminjamanRepository)

            modelClass.isAssignableFrom(LoanDetailViewModel::class.java) ->
                LoanDetailViewModel(container.peminjamanRepository)

            modelClass.isAssignableFrom(ReturnFormViewModel::class.java) ->
                ReturnFormViewModel(
                        container.peminjamanRepository,
                    container.pengembalianRepository,
                    container.sessionManager
                )

            modelClass.isAssignableFrom(EditLoanViewModel::class.java) ->
                EditLoanViewModel(container.peminjamanRepository, container.alatRepository)

            modelClass.isAssignableFrom(StaffRequestsViewModel::class.java) ->
                StaffRequestsViewModel(container.peminjamanRepository)

            modelClass.isAssignableFrom(StaffMonitorViewModel::class.java) ->
                StaffMonitorViewModel(container.peminjamanRepository)

            modelClass.isAssignableFrom(ReturnsViewModel::class.java) ->
                ReturnsViewModel(container.pengembalianRepository)

            modelClass.isAssignableFrom(ReturnDetailViewModel::class.java) ->
                ReturnDetailViewModel(container.pengembalianRepository)

            modelClass.isAssignableFrom(ReportViewModel::class.java) ->
                ReportViewModel(container.laporanRepository, container.applicationContext)

            modelClass.isAssignableFrom(AlatManageViewModel::class.java) ->
                AlatManageViewModel(container.alatRepository)

            modelClass.isAssignableFrom(AlatFormViewModel::class.java) ->
                AlatFormViewModel(container.alatRepository, container.kategoriRepository)

            modelClass.isAssignableFrom(UnitAlatViewModel::class.java) ->
                UnitAlatViewModel(container.unitAlatRepository)

            modelClass.isAssignableFrom(KategoriViewModel::class.java) ->
                KategoriViewModel(container.kategoriRepository)

            modelClass.isAssignableFrom(UsersViewModel::class.java) ->
                UsersViewModel(container.userRepository)

            modelClass.isAssignableFrom(LogAktivitasViewModel::class.java) ->
                LogAktivitasViewModel(container.logRepository, container.userRepository)

            else -> error("ViewModel tidak dikenal: ${modelClass.name}")
        }
        return viewModel as T
    }
}
