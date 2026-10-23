package com.app.campgear.ui.common

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.recyclerview.widget.RecyclerView
import androidx.navigation.fragment.findNavController
import com.app.campgear.CampGearApp
import com.app.campgear.core.AppContainer
import com.app.campgear.core.PagedUiState
import com.google.android.material.snackbar.Snackbar

abstract class BaseFragment<VB : ViewBinding> : Fragment() {

    private var _binding: VB? = null
    protected val binding: VB get() = requireNotNull(_binding)

    protected val appContainer: AppContainer
        get() = (requireActivity().application as CampGearApp).container

    protected val viewModelFactory
        get() = appContainer.viewModelFactory

    private var lastPagedError: String? = null

    protected abstract fun createBinding(inflater: LayoutInflater, container: ViewGroup?): VB

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = com.google.android.material.transition.MaterialFadeThrough()
        returnTransition = com.google.android.material.transition.MaterialFadeThrough()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val savedState = findNavController().currentBackStackEntry?.savedStateHandle
        savedState?.getLiveData<Boolean>("refresh")?.observe(viewLifecycleOwner) { changed ->
            if (changed) {
                savedState.set("refresh", false)
                onDataChanged()
                showMessage(getString(com.app.campgear.R.string.data_saved))
            }
        }
    }

    protected open fun onDataChanged() = Unit

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        configureLists(binding.root)
    }

    private fun configureLists(view: View) {
        if (view is RecyclerView) view.adapter?.stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
        if (view is SwipeRefreshLayout) {
            view.setColorSchemeResources(com.app.campgear.R.color.primary)
            view.setProgressBackgroundColorSchemeResource(com.app.campgear.R.color.surface)
        }
        if (view is ViewGroup) for (index in 0 until view.childCount) configureLists(view.getChildAt(index))
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = createBinding(inflater, container)
        return binding.root
    }

    override fun onDestroyView() {
        _binding?.root?.let(::releaseAdapters)
        lastPagedError = null
        _binding = null
        super.onDestroyView()
    }

    private fun releaseAdapters(view: View) {
        if (view is RecyclerView) view.adapter = null
        if (view is ViewGroup) for (index in 0 until view.childCount) releaseAdapters(view.getChildAt(index))
    }

    protected fun showMessage(message: CharSequence) {
        designedSnackbar(message).show()
    }

    protected fun showError(message: CharSequence) {
        designedSnackbar(message).show()
    }

    private fun designedSnackbar(message: CharSequence): Snackbar {
        val snackbar = Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
        val bottom = requireActivity().findViewById<View>(com.app.campgear.R.id.bottomNavigation)
        val submit = binding.root.findViewById<View>(com.app.campgear.R.id.buttonSubmit)
            ?: binding.root.findViewById<View>(com.app.campgear.R.id.buttonSubmitReturn)
        val anchor = if (bottom?.visibility == View.VISIBLE) bottom else submit
        if (anchor != null) snackbar.anchorView = anchor
        return snackbar
    }

    protected fun renderPagedState(
        state: PagedUiState<*>,
        stateLayout: StateLayout,
        swipeRefresh: SwipeRefreshLayout?,
        emptyMessage: CharSequence,
        paginationProgress: View?,
        onRetry: () -> Unit
    ) {
        swipeRefresh?.isRefreshing = state.refreshing
        paginationProgress?.visibility = if (state.loadingMore) View.VISIBLE else View.GONE

        when {
            state.loading && state.items.isEmpty() -> stateLayout.showLoading()
            state.error != null && state.items.isEmpty() -> stateLayout.showError(state.error, onRetry)
            state.items.isEmpty() -> stateLayout.showEmpty(emptyMessage, onRetry)
            else -> stateLayout.showContent()
        }

        if (state.error != null && state.items.isNotEmpty() && state.error != lastPagedError) {
            Snackbar.make(binding.root, state.error, Snackbar.LENGTH_LONG)
                .setAction(com.app.campgear.R.string.action_retry) { onRetry() }.show()
        }
        lastPagedError = state.error
    }
}
