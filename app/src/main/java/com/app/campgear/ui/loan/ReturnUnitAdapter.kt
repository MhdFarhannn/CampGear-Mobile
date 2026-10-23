package com.app.campgear.ui.loan

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.app.campgear.R
import com.app.campgear.core.MasterOptions
import com.app.campgear.databinding.ItemReturnUnitBinding

class ReturnUnitAdapter(
    private val onCondition: (Long, String) -> Unit,
    private val onNote: (Long, String) -> Unit
) : ListAdapter<ReturnUnitInput, ReturnUnitAdapter.ViewHolder>(DIFF) {
    private var busy = false
    fun setBusy(value: Boolean) {
        if (busy == value) return
        busy = value
        notifyItemRangeChanged(0, itemCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemReturnUnitBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isNotEmpty() && payloads.all { it == "note" }) holder.bindNote(getItem(position))
        else holder.bind(getItem(position))
    }

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        holder.clearWatcher()
    }

    inner class ViewHolder(
        private val binding: ItemReturnUnitBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private var watcher: TextWatcher? = null
        private var unitId: Long? = null

        fun bind(item: ReturnUnitInput) {
            val sameUnit = unitId == item.idUnitAlat
            unitId = item.idUnitAlat
            val context = binding.root.context
            binding.dropdownCondition.isEnabled = !busy
            binding.inputUnitNote.isEnabled = !busy
            binding.returnUnitCode.text = context.getString(
                R.string.unit_code_fmt,
                item.kodeInventaris.ifBlank { context.getString(R.string.symbol_dash) }
            )
            binding.returnUnitName.text = item.alat
            binding.unitEstimatedFine.text = context.getString(R.string.return_unit_fine_fmt, com.app.campgear.core.Formatters.rupiah(MasterOptions.conditionFine(item.kondisi)))

            val labels = MasterOptions.kondisiKembali.map { context.getString(it.labelRes) }
            binding.dropdownCondition.setAdapter(
                ArrayAdapter(context, R.layout.item_dropdown, labels)
            )
            val selectedIndex = CONDITION_KEYS.indexOf(item.kondisi).coerceAtLeast(0)
            binding.dropdownCondition.setText(labels[selectedIndex], false)
            binding.dropdownCondition.setOnItemClickListener { _, _, position, _ ->
                onCondition(item.idUnitAlat, CONDITION_KEYS[position])
            }

            clearWatcher()
            if ((!sameUnit || !binding.inputUnitNote.hasFocus()) && binding.inputUnitNote.text?.toString() != item.catatan) {
                binding.inputUnitNote.setText(item.catatan)
            }
            val newWatcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
                override fun afterTextChanged(s: Editable?) {
                    onNote(item.idUnitAlat, s?.toString().orEmpty())
                }
            }
            binding.inputUnitNote.addTextChangedListener(newWatcher)
            watcher = newWatcher
        }

        fun bindNote(item: ReturnUnitInput) {
            if (!binding.inputUnitNote.hasFocus() && binding.inputUnitNote.text?.toString() != item.catatan) {
                clearWatcher()
                bind(item)
            }
        }

        fun clearWatcher() {
            watcher?.let { binding.inputUnitNote.removeTextChangedListener(it) }
            watcher = null
        }
    }

    companion object {
        private val CONDITION_KEYS = listOf("baik", "rusak_ringan", "rusak_berat", "hilang")

        private val DIFF = object : DiffUtil.ItemCallback<ReturnUnitInput>() {
            override fun areItemsTheSame(oldItem: ReturnUnitInput, newItem: ReturnUnitInput): Boolean =
                oldItem.idUnitAlat == newItem.idUnitAlat

            override fun areContentsTheSame(oldItem: ReturnUnitInput, newItem: ReturnUnitInput): Boolean =
                oldItem == newItem

            override fun getChangePayload(oldItem: ReturnUnitInput, newItem: ReturnUnitInput): Any? =
                if (oldItem.copy(catatan = "") == newItem.copy(catatan = "")) "note" else null
        }
    }
}
