package com.app.campgear.ui.peminjam

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.app.campgear.R
import com.app.campgear.core.Formatters
import com.app.campgear.core.ImageLoader
import com.app.campgear.databinding.FragmentEquipmentDetailBinding
import com.app.campgear.ui.common.BaseFragment
import kotlinx.coroutines.launch

class EquipmentDetailFragment : BaseFragment<FragmentEquipmentDetailBinding>() {
    private val viewModel: EquipmentDetailViewModel by viewModels { viewModelFactory }
    private val cartViewModel: CartViewModel by activityViewModels { viewModelFactory }
    override fun createBinding(inflater: LayoutInflater, container: ViewGroup?) = FragmentEquipmentDetailBinding.inflate(inflater, container, false)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.initialize(requireArguments().getLong("alatId"))
        binding.buttonBorrowEquipment.setOnClickListener {
            viewModel.state.value.alat?.let { alat ->
                if (cartViewModel.add(alat)) {
                    showMessage(getString(R.string.catalog_added, alat.nama))
                    findNavController().navigate(R.id.borrowForm)
                } else showError(getString(R.string.catalog_stock_limit))
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    val alat = state.alat
                    when {
                        alat == null && state.loading -> binding.stateLayout.showLoading()
                        alat == null && state.error != null -> binding.stateLayout.showError(state.error) { viewModel.load() }
                        alat != null -> {
                            binding.stateLayout.showContent()
                            binding.equipmentName.text = alat.nama
                            binding.equipmentCategory.text = alat.kategori.orEmpty()
                            binding.equipmentDescription.text = alat.deskripsi?.ifBlank { null } ?: getString(R.string.equipment_no_description)
                            binding.equipmentPrice.text = getString(R.string.alat_price_per_day_value, Formatters.rupiah(alat.hargaSewa))
                            binding.equipmentStock.text = getString(R.string.catalog_stock, alat.stokTersedia)
                            binding.equipmentImage.contentDescription = getString(R.string.equipment_image_description, alat.nama)
                            ImageLoader.load(binding.equipmentImage, alat.imagePath)
                            state.error?.let(::showError)
                        }
                    }
                    binding.buttonBorrowEquipment.isEnabled = !state.loading && (alat?.stokTersedia ?: 0) > 0
                }
            }
        }
    }
}
