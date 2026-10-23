package com.app.campgear

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.get
import androidx.core.view.size
import androidx.core.view.updatePadding
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import com.app.campgear.databinding.ActivityMainBinding
import com.app.campgear.ui.peminjam.CartViewModel
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private val appContainer get() = (application as CampGearApp).container
    private val cartViewModel: CartViewModel by viewModels { appContainer.viewModelFactory }
    private var lastUserId: Long? = null
    private var configuredRole: String? = null
    private var navigationConfigured = false

    override fun onCreate(savedInstanceState: Bundle?) {
        if (AppCompatDelegate.getApplicationLocales().toLanguageTags() != "id-ID") {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("id-ID"))
        }
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.isAppearanceLightStatusBars = resources.getBoolean(R.bool.light_system_bars)
        controller.isAppearanceLightNavigationBars = resources.getBoolean(R.bool.light_system_bars)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        lastUserId = appContainer.sessionManager.session.value?.userId
        navController = (supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment).navController
        NavigationUI.setupWithNavController(binding.bottomNavigation, navController)
        binding.navView.setNavigationItemSelectedListener { item ->
            if (item.itemId == R.id.actionLogout) confirmLogout()
            else NavigationUI.onNavDestinationSelected(item, navController)
            binding.drawerLayout.closeDrawers()
            true
        }
        binding.toolbar.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.actionProfile) { navController.navigate(R.id.profile); true } else false
        }
        applyInsets()
        supportFragmentManager.setFragmentResultListener("confirmed_action", this) { _, result ->
            if (result.getString("action") == "logout") lifecycleScope.launch { appContainer.authRepository.logout() }
        }
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val role = appContainer.sessionManager.session.value?.role
            if (role == null && destination.id != R.id.login) {
                binding.root.post { if (appContainer.sessionManager.session.value == null) resetTo(R.id.login) }
            }
            if (role != null && !canAccess(destination.id, role)) {
                Snackbar.make(binding.root, R.string.state_forbidden, Snackbar.LENGTH_LONG).show()
                binding.root.post { navController.navigate(R.id.home, null, NavOptions.Builder().setPopUpTo(R.id.home, false).setLaunchSingleTop(true).build()) }
            }
            updateChrome(destination.id, role)
            binding.navView.setCheckedItem(destination.id)
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                appContainer.sessionManager.session.collect { session ->
                    if (lastUserId != session?.userId) cartViewModel.clear()
                    lastUserId = session?.userId
                    configureNavigation(session?.role)
                    if (session == null) {
                        if (navController.currentDestination?.id != R.id.login) resetTo(R.id.login)
                    } else if (navController.currentDestination?.id == R.id.login || navController.currentDestination == null ||
                        !canAccess(navController.currentDestination!!.id, session.role)
                    ) resetTo(R.id.home)
                    updateChrome(navController.currentDestination?.id ?: R.id.login, session?.role)
                    if (session == null) appContainer.sessionManager.endMessage.value?.let { message ->
                        appContainer.sessionManager.consumeEndMessage()
                        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
                    }
                    val header = binding.navView.getHeaderView(0)
                    header.findViewById<TextView>(R.id.navUserName).text = session?.nama ?: getString(R.string.drawer_default_name)
                    header.findViewById<TextView>(R.id.navUserRole).text = session?.role ?: getString(R.string.drawer_default_role)
                }
            }
        }
    }

    private fun resetTo(destination: Int) {
        (topLevel + setOf(R.id.catalog, R.id.myLoans)).forEach { navController.clearBackStack(it) }
        navController.navigate(destination, null, NavOptions.Builder().setPopUpTo(navController.graph.id, true).setLaunchSingleTop(true).build())
    }

    private fun configureNavigation(role: String?) {
        navController.graph.setStartDestination(if (role == null) R.id.login else R.id.home)
        if (navigationConfigured && configuredRole == role) return
        navigationConfigured = true
        configuredRole = role
        binding.navView.menu.clear()
        binding.navView.inflateMenu(R.menu.drawer_menu)
        val allowed = when (role) {
            "Admin" -> setOf(R.id.home, R.id.adminLoans, R.id.adminReturns, R.id.adminAlat, R.id.adminKategori, R.id.adminUsers, R.id.adminLog, R.id.reports, R.id.profile, R.id.actionLogout)
            "Petugas" -> setOf(R.id.home, R.id.staffRequests, R.id.staffMonitor, R.id.staffReturns, R.id.reports, R.id.profile, R.id.actionLogout)
            else -> emptySet()
        }
        val menu = binding.navView.menu
        for (index in menu.size - 1 downTo 0) if (menu[index].itemId !in allowed) menu.removeItem(menu[index].itemId)
        val configuration = if (role == "Admin" || role == "Petugas") {
            AppBarConfiguration.Builder(topLevel).setOpenableLayout(binding.drawerLayout).build()
        } else AppBarConfiguration.Builder(setOf(R.id.home, R.id.catalog, R.id.myLoans, R.id.profile)).build()
        NavigationUI.setupWithNavController(binding.toolbar, navController, configuration)
    }

    private fun updateChrome(destination: Int, role: String?) {
        val loggedIn = destination != R.id.login && role != null
        binding.toolbar.visibility = if (loggedIn) View.VISIBLE else View.GONE
        binding.bottomNavigation.visibility = if (loggedIn && role == "Peminjam" && destination in setOf(R.id.home, R.id.catalog, R.id.myLoans, R.id.profile)) View.VISIBLE else View.GONE
        binding.toolbar.menu.findItem(R.id.actionProfile)?.isVisible = loggedIn && destination != R.id.profile
        binding.drawerLayout.setDrawerLockMode(if (loggedIn && role != "Peminjam") DrawerLayout.LOCK_MODE_UNLOCKED else DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        if (!loggedIn || role == "Peminjam") binding.drawerLayout.closeDrawers()
    }

    private fun applyInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.mainContent) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            view.updatePadding(top = bars.top, bottom = bars.bottom, left = bars.left, right = bars.right)
            insets
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomNavigation) { view, _ ->
            view.updatePadding(bottom = 0)
            WindowInsetsCompat.CONSUMED
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.navView) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = bars.top, bottom = bars.bottom)
            insets
        }
    }

    private fun confirmLogout() {
        if (supportFragmentManager.findFragmentByTag("logout_confirmation") != null || supportFragmentManager.isStateSaved) return
        com.app.campgear.ui.common.ConfirmationDialogFragment().apply {
            arguments = Bundle().apply {
                putString("action", "logout"); putString("title", getString(R.string.profile_logout_confirm)); putString("message", getString(R.string.profile_logout_confirm_message))
            }
        }.show(supportFragmentManager, "logout_confirmation")
    }

    private fun canAccess(destination: Int, role: String): Boolean = when (destination) {
        R.id.login, R.id.home, R.id.profile, R.id.loanDetail, R.id.returnForm, R.id.returnDetail -> role in setOf("Admin", "Petugas", "Peminjam")
        R.id.catalog, R.id.myLoans, R.id.borrowForm, R.id.equipmentDetail -> role == "Peminjam"
        R.id.staffRequests, R.id.staffMonitor, R.id.staffReturns, R.id.reports -> role == "Petugas" || role == "Admin"
        else -> role == "Admin"
    }

    override fun onResume() { super.onResume(); appContainer.sessionManager.expireIfNeeded() }
    private val topLevel = setOf(R.id.home, R.id.staffRequests, R.id.staffMonitor, R.id.staffReturns, R.id.adminLoans, R.id.adminReturns,
        R.id.adminAlat, R.id.adminKategori, R.id.adminUsers, R.id.adminLog, R.id.reports, R.id.profile)
}
