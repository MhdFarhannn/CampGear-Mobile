package com.app.campgear.ui.common

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.app.campgear.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ConfirmationDialogFragment : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val args = requireArguments()
        return MaterialAlertDialogBuilder(requireContext()).setTitle(args.getString("title"))
            .setMessage(args.getString("message")).setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_confirm) { _, _ ->
                parentFragmentManager.setFragmentResult("confirmed_action", Bundle().apply {
                    putString("action", args.getString("action")); putLong("id", args.getLong("id"))
                })
            }.create()
    }
}

fun androidx.fragment.app.Fragment.confirmAction(action: String, title: String, message: String, id: Long = 0) {
    if (childFragmentManager.findFragmentByTag("confirmation") != null || childFragmentManager.isStateSaved) return
    ConfirmationDialogFragment().apply {
        arguments = Bundle().apply { putString("action", action); putString("title", title); putString("message", message); putLong("id", id) }
    }.show(childFragmentManager, "confirmation")
}
