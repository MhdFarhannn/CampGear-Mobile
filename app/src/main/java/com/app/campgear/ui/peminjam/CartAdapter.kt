package com.app.campgear.ui.peminjam

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.app.campgear.R
import com.app.campgear.core.Formatters
import com.app.campgear.databinding.ItemCartBinding

class CartAdapter(
    private val onIncrease: (CartItem) -> Unit,
    private val onDecrease: (CartItem) -> Unit,
    private val onRemove: (CartItem) -> Unit
) : ListAdapter<CartItem, CartAdapter.ViewHolder>(DIFF) {
    private var busy = false
    fun setBusy(value: Boolean) {
        if (busy == value) return
        busy = value
        notifyItemRangeChanged(0, itemCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCartBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemCartBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CartItem) {
            val context = binding.root.context
            binding.cartName.text = item.nama
            binding.cartPrice.text = context.getString(
                R.string.alat_price_per_day_value,
                Formatters.rupiah(item.hargaSewa)
            )
            binding.cartQuantity.text = context.getString(R.string.quantity_fmt, item.jumlah)
            binding.cartSubtotal.text = Formatters.rupiah(item.hargaSewa * item.jumlah)
            binding.buttonMinus.setOnClickListener { onDecrease(item) }
            binding.buttonPlus.setOnClickListener { onIncrease(item) }
            binding.buttonRemove.setOnClickListener { onRemove(item) }
            binding.buttonPlus.isEnabled = !busy && item.jumlah < item.stok.coerceAtMost(1000)
            binding.buttonMinus.isEnabled = !busy
            binding.buttonRemove.isEnabled = !busy
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<CartItem>() {
            override fun areItemsTheSame(oldItem: CartItem, newItem: CartItem): Boolean =
                oldItem.alatId == newItem.alatId

            override fun areContentsTheSame(oldItem: CartItem, newItem: CartItem): Boolean =
                oldItem == newItem
        }
    }
}
