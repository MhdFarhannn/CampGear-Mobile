package com.app.campgear.core

import android.annotation.SuppressLint
import android.content.Context
import androidx.annotation.StringRes

@SuppressLint("StaticFieldLeak")
object AppStrings {

    private var context: Context? = null

    fun init(context: Context) {
        this.context = context.applicationContext
    }

    fun get(@StringRes resourceId: Int): String {
        val current = checkNotNull(context) {
            "AppStrings belum diinisialisasi."
        }
        return current.getString(resourceId)
    }
}
