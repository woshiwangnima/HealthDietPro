package com.woshiwangnima.healthdietpro.ui.test

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.common.ui.AppIconTextButton
import com.woshiwangnima.healthdietpro.common.ui.AppOutlinedIconTextButton
import com.woshiwangnima.healthdietpro.common.ui.AppSoftwareKeyboard
import com.woshiwangnima.healthdietpro.common.ui.ComponentsPreviewScreen
import com.woshiwangnima.healthdietpro.common.ui.HealthDietProTheme

class TestFragment : Fragment() {
    private var previousSoftInputMode: Int? = null
    private val testAccessViewModel: TestAccessViewModel by activityViewModels()

    interface TestAccessHost {
        fun onTestAccessCancelled()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        setContent {
            HealthDietProTheme {
                val isVerified by testAccessViewModel.isVerified.collectAsStateWithLifecycle()
                if (isVerified) {
                    ComponentsPreviewScreen(onBack = { host()?.onTestAccessCancelled() })
                } else {
                    TestAccessScreen(
                        onCancel = { host()?.onTestAccessCancelled() },
                        onVerify = testAccessViewModel::verify,
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val window = requireActivity().window
        if (previousSoftInputMode == null) previousSoftInputMode = window.attributes.softInputMode
        window.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_STATE_UNSPECIFIED or
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING,
        )
        Log.d(TAG, "Applied ADJUST_NOTHING soft input mode for test access")
    }

    override fun onDestroyView() {
        previousSoftInputMode?.let { requireActivity().window.setSoftInputMode(it) }
        previousSoftInputMode = null
        super.onDestroyView()
    }

    private fun host(): TestAccessHost? = activity as? TestAccessHost

    @Composable
    private fun TestAccessScreen(
        onCancel: () -> Unit,
        onVerify: (String) -> Boolean,
    ) {
        var password by rememberSaveable { mutableStateOf("") }
        var showError by remember { mutableStateOf(false) }
        Column(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(R.string.test_access_title),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.test_access_description),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
            )
            PasswordDisplay(
                label = stringResource(R.string.test_access_password),
                value = password,
                isError = showError,
            )
            if (showError) {
                Text(
                    text = stringResource(R.string.test_access_error),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                )
            }
            AppSoftwareKeyboard(
                onKey = { key ->
                    password += key
                    showError = false
                },
                onBackspace = {
                    password = password.dropLast(1)
                    showError = false
                },
                onClear = {
                    password = ""
                    showError = false
                },
                showSpaceKey = false,
                modifier = Modifier.padding(top = 14.dp),
            )
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                AppIconTextButton(
                    text = stringResource(R.string.test_access_confirm),
                    iconRes = R.drawable.ic_check,
                    onClick = {
                        if (!onVerify(password)) {
                            Log.d(TAG, "Password rejected for test preview")
                            showError = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                AppOutlinedIconTextButton(
                    text = stringResource(R.string.test_access_cancel),
                    iconRes = R.drawable.ic_cancel,
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    @Composable
    private fun PasswordDisplay(label: String, value: String, isError: Boolean) {
        val borderColor = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, borderColor),
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "*".repeat(value.length),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }

    private companion object {
        const val TAG = "TestFragment"
    }
}
