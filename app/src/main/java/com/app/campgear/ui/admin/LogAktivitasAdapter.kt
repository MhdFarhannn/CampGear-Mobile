package com.app.campgear.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.app.campgear.R
import com.app.campgear.core.Formatters
import com.app.campgear.data.LogAktivitas
import com.app.campgear.databinding.ItemLogBinding

class LogAktivitasAdapter : ListAdapter<LogAktivitas, LogAktivitasAdapter.ViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemLogBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemLogBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: LogAktivitas) {
            binding.logActivity.text = item.aktivitas
            binding.logMeta.text = binding.root.context.getString(
                R.string.log_meta_fmt,
                item.namaUser ?: "-",
                com.app.campgear.core.RelativeTime.format(binding.root.context, item.waktu)
            )
            binding.logMeta.contentDescription = binding.root.context.getString(R.string.log_meta_fmt, item.namaUser.orEmpty(), Formatters.displayDateTime(item.waktu))
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<LogAktivitas>() {
            override fun areItemsTheSame(oldItem: LogAktivitas, newItem: LogAktivitas): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: LogAktivitas, newItem: LogAktivitas): Boolean =
                oldItem == newItem
        }
    }
}
