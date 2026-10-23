package com.app.campgear.ui.common

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import com.app.campgear.R
import com.google.android.material.button.MaterialButton

class StateLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val overlay: View
    private val progress: View
    private val empty: View
    private val emptyMessage: TextView
    private val error: View
    private val errorMessage: TextView
    private val retry: MaterialButton
    private val emptyAction: MaterialButton

    init {
        LayoutInflater.from(context).inflate(R.layout.view_state, this, true)
        overlay = findViewById(R.id.stateOverlay)
        progress = findViewById(R.id.stateProgress)
        empty = findViewById(R.id.stateEmpty)
        emptyMessage = findViewById(R.id.stateEmptyMessage)
        error = findViewById(R.id.stateError)
        errorMessage = findViewById(R.id.stateErrorMessage)
        retry = findViewById(R.id.stateRetry)
        emptyAction = findViewById(R.id.stateEmptyAction)
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        overlay.bringToFront()
    }

    fun showContent() {
        overlay.visibility = View.GONE
    }

    fun showLoading() {
        overlay.visibility = View.VISIBLE
        progress.visibility = View.VISIBLE
        empty.visibility = View.GONE
        error.visibility = View.GONE
    }

    fun showEmpty(message: CharSequence, onRetry: (() -> Unit)? = null) {
        showEmpty(message, onRetry, R.string.action_retry)
    }

    fun showEmpty(message: CharSequence, onRetry: (() -> Unit)?, actionLabel: Int) {
        overlay.visibility = View.VISIBLE
        progress.visibility = View.GONE
        empty.visibility = View.VISIBLE
        error.visibility = View.GONE
        emptyMessage.text = message
        emptyAction.visibility = if (onRetry != null) View.VISIBLE else View.GONE
        emptyAction.setText(actionLabel)
        emptyAction.setOnClickListener { onRetry?.invoke() }
    }

    fun showError(message: CharSequence, onRetry: (() -> Unit)? = null) {
        overlay.visibility = View.VISIBLE
        progress.visibility = View.GONE
        empty.visibility = View.GONE
        error.visibility = View.VISIBLE
        errorMessage.text = message
        bindRetry(onRetry)
    }

    private fun bindRetry(onRetry: (() -> Unit)?) {
        retry.visibility = if (onRetry != null) View.VISIBLE else View.GONE
        retry.setOnClickListener { onRetry?.invoke() }
    }
}
