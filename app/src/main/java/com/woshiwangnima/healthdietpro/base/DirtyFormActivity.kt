package com.woshiwangnima.healthdietpro.base

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.woshiwangnima.healthdietpro.common.ui.DiscardChangesDialog

abstract class DirtyFormActivity : BaseBackActivity() {
    protected var showDiscardChangesDialog by mutableStateOf(false)

    protected abstract fun hasUnsavedChanges(): Boolean

    protected abstract fun saveFormChanges()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = requestFormExit()
        })
    }

    protected fun requestFormExit() {
        if (hasUnsavedChanges()) showDiscardChangesDialog = true else finish()
    }

    @Composable
    protected fun DiscardChangesConfirmation() {
        if (showDiscardChangesDialog) {
            DiscardChangesDialog(
                onDiscard = { showDiscardChangesDialog = false; finish() },
                onSave = { showDiscardChangesDialog = false; saveFormChanges() },
                onDismiss = { showDiscardChangesDialog = false },
            )
        }
    }
}
