package com.woshiwangnima.healthdietpro.common.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BaseScreen(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, style = androidx.compose.material3.MaterialTheme.typography.headlineLarge) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                            )
                        }
                    }
                },
                actions = actions,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
                    titleContentColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
        modifier = Modifier,
    ) { innerPadding ->
        content(innerPadding)
    }
}

@Composable
fun BaseScreen(
    title: String,
    onBack: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    BaseScreen(title = title, onBack = onBack) { _ ->
        content()
    }
}
