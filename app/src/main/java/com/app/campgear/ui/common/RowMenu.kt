package com.app.campgear.ui.common

import android.view.View
import androidx.appcompat.widget.PopupMenu
import com.app.campgear.R

fun showRowMenu(anchor: View, onEdit: () -> Unit, onDelete: () -> Unit, onUnits: (() -> Unit)? = null) {
    val popup = PopupMenu(anchor.context, anchor)
    popup.menu.add(0, 1, 0, R.string.action_edit).setIcon(R.drawable.ic_edit)
    if (onUnits != null) popup.menu.add(0, 3, 1, R.string.action_manage_units).setIcon(R.drawable.ic_gear)
    popup.menu.add(0, 2, 2, R.string.action_delete).setIcon(R.drawable.ic_delete)
    popup.setOnMenuItemClickListener {
        when (it.itemId) { 1 -> onEdit(); 2 -> onDelete(); 3 -> onUnits?.invoke() }
        true
    }
    popup.show()
}
