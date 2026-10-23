package com.app.campgear.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.app.campgear.data.Kategori
import com.app.campgear.databinding.ItemKategoriBinding

class KategoriAdapter(
    private val onEdit: (Kategori) -> Unit,
    private val onDelete: (Kategori) -> Unit
) : ListAdapter<Kategori, KategoriAdapter.ViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemKategoriBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemKategoriBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Kategori) {
            binding.kategoriName.text = item.nama
            binding.buttonKategoriMenu.setOnClickListener {
                com.app.campgear.ui.common.showRowMenu(it, { onEdit(item) }, { onDelete(item) })
            }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Kategori>() {
            override fun areItemsTheSame(oldItem: Kategori, newItem: Kategori): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: Kategori, newItem: Kategori): Boolean =
                oldItem == newItem
        }
    }
}
