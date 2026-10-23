package com.app.campgear.ui.reports

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.app.campgear.R
import com.app.campgear.core.Formatters
import com.app.campgear.data.ReportPengembalian
import com.app.campgear.databinding.ItemReportReturnBinding

class ReportReturnAdapter :
    ListAdapter<ReportPengembalian, ReportReturnAdapter.ViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemReportReturnBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemReportReturnBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ReportPengembalian) {
            val context = binding.root.context
            binding.reportReturnNumber.text = context.getString(R.string.return_number, item.id)
            binding.reportReturnBorrower.text = context.getString(
                R.string.loan_peminjam_fmt,
                item.peminjam.orEmpty()
            )
            binding.reportReturnDate.text = context.getString(
                R.string.return_date_fmt,
                Formatters.displayDate(item.tanggalKembali)
            )
            binding.reportReturnLate.text = context.resources.getQuantityString(
                R.plurals.return_late_days_fmt,
                item.keterlambatanPengembalian,
                item.keterlambatanPengembalian
            )
            binding.reportReturnFine.text = Formatters.rupiah(item.denda)
            binding.reportReturnBreakdown.text = context.getString(R.string.report_return_breakdown,
                Formatters.rupiah((item.denda - item.dendaKondisi).coerceAtLeast(0.0)), Formatters.rupiah(item.dendaKondisi))
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<ReportPengembalian>() {
            override fun areItemsTheSame(
                oldItem: ReportPengembalian,
                newItem: ReportPengembalian
            ): Boolean = oldItem.id == newItem.id

            override fun areContentsTheSame(
                oldItem: ReportPengembalian,
                newItem: ReportPengembalian
            ): Boolean = oldItem == newItem
        }
    }
}
