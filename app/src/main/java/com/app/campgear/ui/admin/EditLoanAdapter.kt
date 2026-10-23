package com.app.campgear.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.app.campgear.R
import com.app.campgear.core.Formatters
import com.app.campgear.databinding.ItemEditLoanBinding

class EditLoanAdapter(
    private val onIncrease: (EditLoanItem) -> Unit,
    private val onDecrease: (EditLoanItem) -> Unit,
    private val onRemove: (EditLoanItem) -> Unit
) : ListAdapter<EditLoanItem, EditLoanAdapter.ViewHolder>(DIFF) {
    private var busy = false
    fun setBusy(value: Boolean) {
        if (busy == value) return
        busy = value
        notifyItemRangeChanged(0, itemCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemEditLoanBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemEditLoanBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: EditLoanItem) {
            val context = binding.root.context
            binding.editItemName.text = item.nama
            binding.editItemPrice.text = context.getString(
                R.string.alat_price_per_day_value,
                Formatters.rupiah(item.hargaSewa)
            )
            binding.editItemQuantity.text = context.getString(R.string.quantity_fmt, item.jumlah)
            binding.buttonMinus.setOnClickListener { onDecrease(item) }
            binding.buttonPlus.setOnClickListener { onIncrease(item) }
            binding.buttonRemoveEdit.setOnClickListener { onRemove(item) }
            binding.buttonPlus.isEnabled = !busy && item.jumlah < 1000
            binding.buttonMinus.isEnabled = !busy && item.jumlah > 1
            binding.buttonRemoveEdit.isEnabled = !busy
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<EditLoanItem>() {
            override fun areItemsTheSame(oldItem: EditLoanItem, newItem: EditLoanItem): Boolean =
                oldItem.idAlat == newItem.idAlat

            override fun areContentsTheSame(oldItem: EditLoanItem, newItem: EditLoanItem): Boolean =
                oldItem == newItem
        }
    }
}
