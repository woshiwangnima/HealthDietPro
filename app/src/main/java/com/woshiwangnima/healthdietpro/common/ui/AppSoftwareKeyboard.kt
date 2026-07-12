package com.woshiwangnima.healthdietpro.common.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.woshiwangnima.healthdietpro.R

@Composable
fun AppSoftwareKeyboard(
    onKey: (String) -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier,
    onClear: (() -> Unit)? = null,
    showSpaceKey: Boolean = true,
) {
    var mode by remember { mutableStateOf(SoftwareKeyboardMode.LETTERS) }
    var uppercase by remember { mutableStateOf(true) }
    val layout = softwareKeyboardLayout(mode, uppercase)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        layout.rows.forEach { row -> KeyboardRow(row, onKey) }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            when (mode) {
                SoftwareKeyboardMode.LETTERS -> {
                    KeyboardActionButton(
                        text = stringResource(R.string.software_keyboard_shift),
                        onClick = { uppercase = !uppercase },
                    )
                    KeyboardActionButton(
                        text = stringResource(R.string.software_keyboard_numbers),
                        onClick = { mode = SoftwareKeyboardMode.NUMBERS },
                    )
                }
                SoftwareKeyboardMode.NUMBERS -> {
                    KeyboardActionButton(
                        text = stringResource(R.string.software_keyboard_letters),
                        onClick = { mode = SoftwareKeyboardMode.LETTERS },
                    )
                    KeyboardActionButton(
                        text = stringResource(R.string.software_keyboard_symbols),
                        onClick = { mode = SoftwareKeyboardMode.SYMBOLS },
                    )
                }
                SoftwareKeyboardMode.SYMBOLS -> {
                    KeyboardActionButton(
                        text = stringResource(R.string.software_keyboard_letters),
                        onClick = { mode = SoftwareKeyboardMode.LETTERS },
                    )
                    KeyboardActionButton(
                        text = stringResource(R.string.software_keyboard_numbers),
                        onClick = { mode = SoftwareKeyboardMode.NUMBERS },
                    )
                }
            }
            KeyboardActionButton(
                text = stringResource(R.string.test_access_backspace),
                onClick = onBackspace,
            )
        }
        if (showSpaceKey) {
            OutlinedButton(
                onClick = { onKey(" ") },
                modifier = Modifier.fillMaxWidth(),
                colors = keyboardActionButtonColors(),
            ) {
                Text(stringResource(R.string.software_keyboard_space))
            }
        }
        if (onClear != null) {
            OutlinedButton(
                onClick = onClear,
                modifier = Modifier.fillMaxWidth(),
                colors = keyboardActionButtonColors(),
            ) {
                Text(stringResource(R.string.test_access_clear))
            }
        }
    }
}

@Composable
private fun KeyboardRow(keys: List<String>, onKey: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        keys.forEach { key ->
            Button(
                onClick = { onKey(key) },
                modifier = Modifier.weight(1f),
                colors = keyboardKeyButtonColors(),
                contentPadding = PaddingValues(horizontal = 0.dp),
            ) {
                Text(text = key, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun RowScope.KeyboardActionButton(
    text: String,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.weight(1f),
        colors = keyboardActionButtonColors(),
    ) {
        Text(text)
    }
}

@Composable
private fun keyboardKeyButtonColors() = ButtonDefaults.buttonColors(
    containerColor = MaterialTheme.colorScheme.secondaryContainer,
    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
)

@Composable
private fun keyboardActionButtonColors() = ButtonDefaults.outlinedButtonColors(
    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
)


