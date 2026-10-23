package com.app.campgear.ui.peminjam

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.app.campgear.R
import com.app.campgear.core.Formatters
import com.app.campgear.core.ImageLoader
import com.app.campgear.data.AlatResponse
import com.app.campgear.databinding.ItemAlatBinding

class AlatAdapter(
    private val onOpen: (AlatResponse) -> Unit,
    private val onAdd: (AlatResponse) -> Unit
) : ListAdapter<AlatResponse, AlatAdapter.ViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAlatBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemAlatBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(alat: AlatResponse) {
            val context = binding.root.context
            ImageLoader.load(binding.alatImage, alat.imagePath)
            binding.alatImage.contentDescription = context.getString(R.string.equipment_image_description, alat.nama)
            binding.alatName.text = alat.nama
            binding.alatCategory.text = alat.kategori ?: "-"
            binding.alatDescription.text = alat.deskripsi.orEmpty()
            binding.alatDescription.visibility =
                if (alat.deskripsi.isNullOrBlank()) android.view.View.GONE else android.view.View.VISIBLE
            binding.alatPrice.text = context.getString(
                R.string.alat_price_per_day_value,
                Formatters.rupiah(alat.hargaSewa)
            )
            binding.alatStock.text = context.getString(R.string.catalog_stock, alat.stokTersedia)
            binding.alatStock.setTextColor(com.app.campgear.core.StatusUi.textColor(context, if (alat.stokTersedia > 0) "tersedia" else "tidak_tersedia"))
            binding.alatStock.backgroundTintList = android.content.res.ColorStateList.valueOf(com.app.campgear.core.StatusUi.backgroundColor(context, if (alat.stokTersedia > 0) "tersedia" else "tidak_tersedia"))
            binding.buttonAdd.isEnabled = alat.stokTersedia > 0
            binding.buttonAdd.text = context.getString(
                if (alat.stokTersedia > 0) R.string.catalog_add else R.string.catalog_no_stock
            )
            binding.buttonAdd.setOnClickListener { onAdd(alat) }
            binding.root.setOnClickListener { onOpen(alat) }
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
