package com.app.campgear.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.app.campgear.R
import com.app.campgear.data.Kategori
import com.app.campgear.databinding.FragmentAlatFormBinding
import com.app.campgear.ui.common.BaseFragment
import kotlinx.coroutines.launch

class AlatFormFragment : BaseFragment<FragmentAlatFormBinding>() {
    private val viewModel: AlatFormViewModel by viewModels { viewModelFactory }
    private val alatId: Long by lazy { requireArguments().getLong("alatId") }
    private var categories: List<Kategori> = emptyList()
    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) viewModel.selectImage(requireContext(), uri)
    }

    override fun createBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentAlatFormBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.formAlatTitle.setText(if (alatId == 0L) R.string.alat_add_title else R.string.alat_edit_title)
        binding.dropdownAlatKategori.setOnItemClickListener { _, _, position, _ ->
            viewModel.setCategory(categories.getOrNull(position)?.id)
        }
        binding.inputAlatName.doAfterTextChanged { viewModel.setName(it?.toString().orEmpty()) }
        binding.inputAlatDescription.doAfterTextChanged { viewModel.setDescription(it?.toString().orEmpty()) }
        binding.inputAlatPrice.doAfterTextChanged { viewModel.setPrice(it?.toString().orEmpty()) }
        binding.buttonPickImage.setOnClickListener { pickImage.launch("image/*") }
        binding.buttonSaveAlat.setOnClickListener { save() }
        viewModel.initialize(alatId)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.state.collect(::render) }
                launch {
                    viewModel.saved.collect {
                        findNavController().previousBackStackEntry?.savedStateHandle?.set("refresh", true)
                        showMessage(getString(R.string.alat_saved))
                        findNavController().popBackStack()
                    }
                }
            }
        }
    }

    private fun render(state: AlatFormUiState) {
        when {
            state.loading -> binding.stateLayout.showLoading()
            state.loadError != null -> binding.stateLayout.showError(state.loadError) { viewModel.load(alatId) }
            state.initialized && state.categories.isEmpty() -> binding.stateLayout.showEmpty(getString(R.string.alat_no_category), {
                findNavController().navigate(R.id.adminKategori)
            }, R.string.kategori_add)
            else -> {
                binding.stateLayout.showContent()
                if (categories != state.categories) {
                    categories = state.categories
                    binding.dropdownAlatKategori.setAdapter(
                        ArrayAdapter(requireContext(), R.layout.item_dropdown, categories.map { it.nama })
                    )
                }
                if (!binding.inputAlatName.hasFocus() && binding.inputAlatName.text?.toString() != state.nama) binding.inputAlatName.setText(state.nama)
                if (!binding.inputAlatDescription.hasFocus() && binding.inputAlatDescription.text?.toString() != state.deskripsi) binding.inputAlatDescription.setText(state.deskripsi)
                if (!binding.inputAlatPrice.hasFocus() && binding.inputAlatPrice.text?.toString() != state.harga) binding.inputAlatPrice.setText(state.harga)
                val category = categories.firstOrNull { it.id == state.idKategori }
                binding.dropdownAlatKategori.setText(category?.nama.orEmpty(), false)
                binding.textAlatImage.text = when {
                    state.image != null -> getString(R.string.alat_image_selected, state.image.displayName)
                    state.alat?.imagePath != null -> getString(R.string.alat_image_existing)
                    else -> getString(R.string.image_optional)
                }
                if (state.image != null) com.app.campgear.core.ImageLoader.loadFile(binding.alatImagePreview, state.image.file)
                else com.app.campgear.core.ImageLoader.load(binding.alatImagePreview, state.alat?.imagePath)
            }
        }
        val busy = state.loading || state.saving || state.copyingImage || state.completed
        binding.buttonSaveAlat.isEnabled = !busy && state.initialized && state.categories.isNotEmpty()
        binding.buttonPickImage.isEnabled = !busy
        binding.inputAlatName.isEnabled = !state.saving
        binding.inputAlatPrice.isEnabled = !state.saving
        binding.inputAlatDescription.isEnabled = !state.saving
        binding.dropdownAlatKategori.isEnabled = !state.saving
        binding.alatFormProgress.visibility = if (state.saving || state.copyingImage) View.VISIBLE else View.GONE
        binding.textAlatFormError.visibility = if (state.saveError != null) View.VISIBLE else View.GONE
        binding.textAlatFormError.text = state.saveError
        binding.tilAlatName.error = state.fieldErrors["nama"]
        binding.tilAlatCategory.error = state.fieldErrors["idKategori"]
        binding.tilAlatPrice.error = state.fieldErrors["hargaSewa"]
        binding.buttonSaveAlat.text = getString(if (state.saving || state.copyingImage) R.string.symbol_blank else R.string.action_save)
        binding.buttonSaveAlat.icon = if (state.saving || state.copyingImage) null else androidx.appcompat.content.res.AppCompatResources.getDrawable(requireContext(), R.drawable.ic_check)
    }

    private fun save() {
        binding.tilAlatName.error = null
        binding.tilAlatCategory.error = null
        binding.tilAlatPrice.error = null
        val state = viewModel.state.value
        val name = state.nama.trim()
        var valid = true
        if (name.isEmpty() || name.length > 150) {
            binding.tilAlatName.error = getString(R.string.alat_validation_name)
            valid = false
        }
        val category = state.idKategori?.takeIf { id -> categories.any { it.id == id } }
        if (category == null) {
            binding.tilAlatCategory.error = getString(R.string.alat_validation_category)
            valid = false
        }
        val price = state.harga.replace(',', '.').toBigDecimalOrNull()
        if (price == null || price.signum() < 0 || price.scale() > 2 || price > java.math.BigDecimal("9999999999999.99")) {
            binding.tilAlatPrice.error = getString(R.string.alat_validation_price)
            valid = false
        }
        if (valid && category != null && price != null) {
            viewModel.save(alatId, category, name, state.deskripsi.trim().ifEmpty { null }, price.toPlainString())
        }
    }

    override fun onDestroyView() {
        categories = emptyList()
        super.onDestroyView()
    }

    override fun onResume() {
        super.onResume()
        if (viewModel.state.value.initialized && viewModel.state.value.categories.isEmpty() && !viewModel.state.value.loading) viewModel.load(alatId)
    }

}
