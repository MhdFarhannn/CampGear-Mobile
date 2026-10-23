package com.app.campgear.ui.reports

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.app.campgear.R
import com.app.campgear.core.Formatters
import com.app.campgear.data.ReportPeminjaman
import com.app.campgear.databinding.ItemReportLoanBinding

class ReportLoanAdapter :
    ListAdapter<ReportPeminjaman, ReportLoanAdapter.ViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemReportLoanBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemReportLoanBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ReportPeminjaman) {
            val context = binding.root.context
            binding.reportLoanNumber.text = context.getString(R.string.loan_number, item.id)
            com.app.campgear.core.StatusUi.bind(binding.reportLoanStatus, item.status)
            binding.reportLoanBorrower.text = context.getString(
                R.string.loan_peminjam_fmt,
                item.peminjam.orEmpty()
            )
            binding.reportLoanDates.text = context.getString(
                R.string.date_range_fmt,
                Formatters.displayDate(item.tanggalPeminjaman),
                Formatters.displayDate(item.tanggalPengembalian)
            )
            binding.reportLoanItems.text = context.resources.getQuantityString(
                R.plurals.item_count_fmt,
                item.jumlahAlat,
                item.jumlahAlat
            )
            binding.reportLoanTotal.text = Formatters.rupiah(item.totalBiaya)
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<ReportPeminjaman>() {
            override fun areItemsTheSame(
                oldItem: ReportPeminjaman,
                newItem: ReportPeminjaman
            ): Boolean = oldItem.id == newItem.id

            override fun areContentsTheSame(
                oldItem: ReportPeminjaman,
                newItem: ReportPeminjaman
            ): Boolean = oldItem == newItem
        }
    }
}
