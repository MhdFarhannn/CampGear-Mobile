package com.app.campgear.ui.common

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

fun RecyclerView.onNearEnd(loadMore: () -> Unit) {
    addOnScrollListener(object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            val manager = layoutManager as? LinearLayoutManager ?: return
            if (dy > 0 && manager.findLastVisibleItemPosition() >= (adapter?.itemCount ?: 0) - 3) loadMore()
        }
    })
}
