package com.app.campgear.ui.returns

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.app.campgear.R
import com.app.campgear.core.Formatters
import com.app.campgear.core.StatusUi
import com.app.campgear.data.PengembalianUnitResponse
import com.app.campgear.databinding.ItemReturnUnitDetailBinding

class ReturnDetailAdapter :
    ListAdapter<PengembalianUnitResponse, ReturnDetailAdapter.ViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemReturnUnitDetailBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemReturnUnitDetailBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PengembalianUnitResponse) {
            val context = binding.root.context
            binding.returnUnitCode.text = context.getString(
                R.string.unit_code_fmt,
                item.kodeInventaris.orEmpty()
            )
            binding.returnUnitName.text = item.alat.orEmpty()
            StatusUi.bind(binding.returnUnitStatus, item.statusKondisi)
            binding.returnUnitFine.text = context.getString(
                R.string.return_unit_fine_fmt,
                Formatters.rupiah(item.dendaKondisi)
            )
            binding.returnUnitNote.visibility =
                if (item.catatan.isNullOrBlank()) View.GONE else View.VISIBLE
            binding.returnUnitNote.text = context.getString(
                R.string.return_note_fmt,
                item.catatan.orEmpty()
            )
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<PengembalianUnitResponse>() {
            override fun areItemsTheSame(
                oldItem: PengembalianUnitResponse,
                newItem: PengembalianUnitResponse
            ): Boolean = oldItem.idUnitAlat == newItem.idUnitAlat

            override fun areContentsTheSame(
                oldItem: PengembalianUnitResponse,
                newItem: PengembalianUnitResponse
            ): Boolean = oldItem == newItem
        }
    }
}
