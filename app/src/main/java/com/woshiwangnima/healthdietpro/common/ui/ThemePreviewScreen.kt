package com.woshiwangnima.healthdietpro.common.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.woshiwangnima.healthdietpro.R

@Composable
fun ThemePreviewScreen(
    onBack: (() -> Unit)? = null,
    onOpenSettings: (() -> Unit)? = null,
    onOpenComponents: (() -> Unit)? = null,
) {
    BaseScreen(
        title = stringResource(R.string.compose_theme_preview_title),
        onBack = onBack,
    ) { padding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = stringResource(R.string.compose_theme_preview_body),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Spacer(modifier = Modifier.height(24.dp))
                if (onOpenSettings != null) {
                    Button(onClick = onOpenSettings) {
                        Text(stringResource(R.string.compose_theme_preview_open_settings))
                    }
                }
                if (onOpenComponents != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = onOpenComponents) {
                        Text(stringResource(R.string.compose_components_preview_open))
                    }
                }
            }
        }
    }
}
