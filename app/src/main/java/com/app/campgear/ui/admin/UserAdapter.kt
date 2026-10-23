package com.app.campgear.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.app.campgear.R
import com.app.campgear.data.UserResponse
import com.app.campgear.databinding.ItemUserBinding

class UserAdapter(
    private val onEdit: (UserResponse) -> Unit,
    private val onDelete: (UserResponse) -> Unit
) : ListAdapter<UserResponse, UserAdapter.ViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemUserBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: UserResponse) {
            binding.userName.text = item.nama
            binding.userMeta.text = binding.root.context.getString(
                R.string.users_meta_fmt,
                item.role.orEmpty(),
                item.noTelepon.orEmpty()
            )
            binding.userAddress.text = item.alamat.orEmpty()
            binding.userAddress.visibility =
                if (item.alamat.isNullOrBlank()) android.view.View.GONE else android.view.View.VISIBLE
            binding.buttonUserMenu.setOnClickListener {
                com.app.campgear.ui.common.showRowMenu(it, { onEdit(item) }, { onDelete(item) })
            }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<UserResponse>() {
            override fun areItemsTheSame(oldItem: UserResponse, newItem: UserResponse): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: UserResponse, newItem: UserResponse): Boolean =
                oldItem == newItem
        }
    }
}
