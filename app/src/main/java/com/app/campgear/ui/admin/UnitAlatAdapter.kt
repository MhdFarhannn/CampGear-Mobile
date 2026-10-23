package com.app.campgear.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.app.campgear.R
import com.app.campgear.core.StatusUi
import com.app.campgear.data.UnitAlatResponse
import com.app.campgear.databinding.ItemUnitBinding

class UnitAlatAdapter(
    private val onEdit: (UnitAlatResponse) -> Unit,
    private val onDelete: (UnitAlatResponse) -> Unit
) : ListAdapter<UnitAlatResponse, UnitAlatAdapter.ViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemUnitBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemUnitBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: UnitAlatResponse) {
            val context = binding.root.context
            binding.unitCode.text = context.getString(
                R.string.unit_code_fmt,
                item.kodeInventaris
            )
            binding.unitAlatName.text = item.namaAlat.orEmpty()
            StatusUi.bind(binding.unitItemStatus, item.statusBarang)
            StatusUi.bind(binding.unitAvailabilityStatus, item.statusKetersediaan)
            binding.buttonUnitMenu.setOnClickListener {
                com.app.campgear.ui.common.showRowMenu(it, { onEdit(item) }, { onDelete(item) })
            }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<UnitAlatResponse>() {
            override fun areItemsTheSame(
                oldItem: UnitAlatResponse,
                newItem: UnitAlatResponse
            ): Boolean = oldItem.id == newItem.id

            override fun areContentsTheSame(
                oldItem: UnitAlatResponse,
                newItem: UnitAlatResponse
            ): Boolean = oldItem == newItem
        }
    }
}
