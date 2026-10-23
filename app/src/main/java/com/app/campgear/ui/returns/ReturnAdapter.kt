package com.app.campgear.ui.returns

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.app.campgear.R
import com.app.campgear.core.Formatters
import com.app.campgear.data.PengembalianResponse
import com.app.campgear.databinding.ItemReturnBinding

class ReturnAdapter(
    private val onOpen: (PengembalianResponse) -> Unit
) : ListAdapter<PengembalianResponse, ReturnAdapter.ViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemReturnBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemReturnBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PengembalianResponse) {
            val context = binding.root.context
            binding.returnListNumber.text = context.getString(R.string.return_number, item.id)
            binding.returnListBorrower.text = context.getString(
                R.string.loan_peminjam_fmt,
                item.peminjam.orEmpty()
            )
            binding.returnListDate.text = Formatters.displayDate(item.tanggalKembali)
            binding.returnListLate.text = context.resources.getQuantityString(
                R.plurals.return_late_days_fmt,
                item.keterlambatanPengembalian,
                item.keterlambatanPengembalian
            )
            binding.returnListFine.text = Formatters.rupiah(item.denda)
            binding.root.setOnClickListener { onOpen(item) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<PengembalianResponse>() {
            override fun areItemsTheSame(
                oldItem: PengembalianResponse,
                newItem: PengembalianResponse
            ): Boolean = oldItem.id == newItem.id

            override fun areContentsTheSame(
                oldItem: PengembalianResponse,
                newItem: PengembalianResponse
            ): Boolean = oldItem == newItem
        }
    }
}
