package com.app.campgear.ui.loan

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.app.campgear.R
import com.app.campgear.core.Formatters
import com.app.campgear.core.StatusUi
import com.app.campgear.data.PeminjamanResponse
import com.app.campgear.databinding.ItemLoanBinding

class LoanAdapter(
    private val showBorrower: Boolean,
    private val onOpen: (PeminjamanResponse) -> Unit
) : ListAdapter<PeminjamanResponse, LoanAdapter.ViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemLoanBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemLoanBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(loan: PeminjamanResponse) {
            val context = binding.root.context
            binding.loanNumber.text = context.getString(R.string.loan_number, loan.id)
            StatusUi.bind(binding.loanStatus, loan.status)
            binding.loanBorrower.visibility = if (showBorrower) View.VISIBLE else View.GONE
            binding.loanBorrower.text = loan.peminjam.orEmpty()
            binding.loanDates.text = context.getString(
                R.string.date_range_fmt,
                Formatters.displayDate(loan.tanggalPeminjaman),
                Formatters.displayDate(loan.tanggalPengembalian)
            )
            binding.loanTotal.text = Formatters.rupiah(loan.totalBiaya)
            binding.root.setOnClickListener { onOpen(loan) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<PeminjamanResponse>() {
            override fun areItemsTheSame(
                oldItem: PeminjamanResponse,
                newItem: PeminjamanResponse
            ): Boolean = oldItem.id == newItem.id

            override fun areContentsTheSame(
                oldItem: PeminjamanResponse,
                newItem: PeminjamanResponse
            ): Boolean = oldItem == newItem
        }
    }
}
