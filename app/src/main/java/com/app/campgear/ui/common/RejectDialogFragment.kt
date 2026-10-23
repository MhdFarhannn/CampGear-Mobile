package com.app.campgear.ui.common

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.app.campgear.R
import com.app.campgear.databinding.DialogRejectBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class RejectDialogFragment : DialogFragment() {
    private var binding: DialogRejectBinding? = null
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val ui = DialogRejectBinding.inflate(layoutInflater)
        binding = ui
        ui.inputRejectNote.setText(savedInstanceState?.getString("note").orEmpty())
        return MaterialAlertDialogBuilder(requireContext()).setTitle(R.string.dialog_reject_title)
            .setView(ui.root).setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.loan_detail_reject) { _, _ ->
                parentFragmentManager.setFragmentResult("reject", Bundle().apply { putString("note", ui.inputRejectNote.text?.toString()?.trim()) })
            }.create()
    }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("note", binding?.inputRejectNote?.text?.toString())
        super.onSaveInstanceState(outState)
    }
    override fun onDestroyView() { binding = null; super.onDestroyView() }
}
