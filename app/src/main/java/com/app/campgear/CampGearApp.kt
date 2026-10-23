package com.app.campgear

import android.app.Application
import com.app.campgear.core.AppContainer
import com.app.campgear.core.AppStrings

class CampGearApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        java.util.Locale.setDefault(java.util.Locale.forLanguageTag("id-ID"))
        AppStrings.init(this)
        container = AppContainer(this)
    }
}
