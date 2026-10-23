package com.app.campgear.core

import com.app.campgear.BuildConfig

object ImageUrls {

    fun resolve(path: String?, apiBaseUrl: String = BuildConfig.API_BASE_URL): String? {
        if (path.isNullOrBlank()) return null
        if (path.startsWith("http://") || path.startsWith("https://")) return path
        val origin = apiBaseUrl
            .trimEnd('/')
            .removeSuffix("/api/v1")
            .trimEnd('/')
        return "$origin/${path.trimStart('/')}"
    }
}
