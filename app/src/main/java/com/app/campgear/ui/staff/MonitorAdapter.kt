package com.app.campgear.ui.staff

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.app.campgear.R
import com.app.campgear.core.Formatters
import com.app.campgear.data.MonitorPeminjaman
import com.app.campgear.databinding.ItemMonitorBinding

class MonitorAdapter(
    private val onOpen: (MonitorPeminjaman) -> Unit
) : ListAdapter<MonitorPeminjaman, MonitorAdapter.ViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMonitorBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemMonitorBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: MonitorPeminjaman) {
            val context = binding.root.context
            binding.monitorName.text = item.peminjam.orEmpty()
            binding.monitorPhone.text = context.getString(
                R.string.phone_value,
                item.noTelepon.orEmpty()
            )
            binding.monitorDates.text = context.getString(
                R.string.date_range_fmt,
                Formatters.displayDate(item.tanggalPeminjaman),
                Formatters.displayDate(item.tanggalPengembalian)
            )
            binding.monitorLate.text = if (item.hariTerlambat > 0) {
                context.resources.getQuantityString(
                    R.plurals.monitor_late,
                    item.hariTerlambat,
                    item.hariTerlambat
                )
            } else {
                context.getString(if (item.tanggalPengembalian == Formatters.todayIso()) R.string.monitor_due_today else R.string.monitor_on_time)
            }
            binding.monitorLate.setTextColor(
                androidx.core.content.ContextCompat.getColor(
                    context,
                    if (item.hariTerlambat > 0) R.color.status_red else R.color.status_green
                )
            )
            binding.monitorLate.backgroundTintList = android.content.res.ColorStateList.valueOf(
                com.app.campgear.core.StatusUi.backgroundColor(context, if (item.hariTerlambat > 0) "ditolak" else "diajukan")
            )
            binding.monitorTotal.text = Formatters.rupiah(item.totalBiaya)
            binding.root.setOnClickListener { onOpen(item) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<MonitorPeminjaman>() {
            override fun areItemsTheSame(
                oldItem: MonitorPeminjaman,
                newItem: MonitorPeminjaman
            ): Boolean = oldItem.id == newItem.id

            override fun areContentsTheSame(
                oldItem: MonitorPeminjaman,
                newItem: MonitorPeminjaman
            ): Boolean = oldItem == newItem
        }
    }
}
