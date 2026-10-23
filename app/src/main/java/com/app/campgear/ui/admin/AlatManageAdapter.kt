package com.app.campgear.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.app.campgear.R
import com.app.campgear.core.Formatters
import com.app.campgear.core.ImageLoader
import com.app.campgear.data.AlatResponse
import com.app.campgear.databinding.ItemAlatAdminBinding

class AlatManageAdapter(
    private val onEdit: (AlatResponse) -> Unit,
    private val onUnits: (AlatResponse) -> Unit,
    private val onDelete: (AlatResponse) -> Unit
) : ListAdapter<AlatResponse, AlatManageAdapter.ViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAlatAdminBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemAlatAdminBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: AlatResponse) {
            val context = binding.root.context
            ImageLoader.load(binding.adminAlatImage, item.imagePath)
            binding.adminAlatName.text = item.nama
            binding.adminAlatCategory.text = item.kategori.orEmpty()
            binding.adminAlatPrice.text = context.getString(
                R.string.alat_price_per_day_value,
                Formatters.rupiah(item.hargaSewa)
            )
            binding.adminAlatStock.text = context.getString(
                R.string.catalog_stock,
                item.stokTersedia
            )
            binding.buttonAlatMenu.setOnClickListener {
                com.app.campgear.ui.common.showRowMenu(it, { onEdit(item) }, { onDelete(item) }, { onUnits(item) })
            }
            binding.adminAlatImage.contentDescription = context.getString(R.string.equipment_image_description, item.nama)
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<AlatResponse>() {
            override fun areItemsTheSame(oldItem: AlatResponse, newItem: AlatResponse): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: AlatResponse, newItem: AlatResponse): Boolean =
                oldItem == newItem
        }
    }
}
