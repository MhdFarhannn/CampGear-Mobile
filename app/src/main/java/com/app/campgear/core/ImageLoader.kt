package com.app.campgear.core

import android.widget.ImageView
import coil.load
import com.app.campgear.R

object ImageLoader {
    fun load(view: ImageView, path: String?) {
        view.load(ImageUrls.resolve(path)) {
            crossfade(true)
            placeholder(R.drawable.illustration_gear)
            error(R.drawable.illustration_gear)
            fallback(R.drawable.illustration_gear)
        }
    }

    fun loadFile(view: ImageView, file: java.io.File) {
        view.load(file) {
            crossfade(true)
            error(R.drawable.illustration_gear)
        }
    }
}
