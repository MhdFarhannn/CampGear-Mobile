package com.app.campgear.ui.loan

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.app.campgear.R
import com.app.campgear.core.Formatters
import com.app.campgear.data.PeminjamanDetailResponse
import com.app.campgear.databinding.ItemLoanDetailBinding

class LoanDetailAdapter :
    ListAdapter<PeminjamanDetailResponse, LoanDetailAdapter.ViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemLoanDetailBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemLoanDetailBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PeminjamanDetailResponse) {
            val context = binding.root.context
            binding.loanItemName.text = item.alat.orEmpty()
            binding.loanItemQuantity.text = context.getString(R.string.quantity_fmt, item.jumlah)
            binding.loanItemPrice.text = Formatters.rupiah(item.hargaSewaSaatTransaksi)
            if (item.kodeInventaris.isEmpty()) {
                binding.loanItemUnits.visibility = View.GONE
            } else {
                binding.loanItemUnits.visibility = View.VISIBLE
                binding.loanItemUnits.text = context.getString(
                    R.string.unit_codes_fmt,
                    item.kodeInventaris.joinToString(", ")
                )
            }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<PeminjamanDetailResponse>() {
            override fun areItemsTheSame(
                oldItem: PeminjamanDetailResponse,
                newItem: PeminjamanDetailResponse
            ): Boolean = oldItem.id == newItem.id

            override fun areContentsTheSame(
                oldItem: PeminjamanDetailResponse,
                newItem: PeminjamanDetailResponse
            ): Boolean = oldItem == newItem
        }
    }
}
