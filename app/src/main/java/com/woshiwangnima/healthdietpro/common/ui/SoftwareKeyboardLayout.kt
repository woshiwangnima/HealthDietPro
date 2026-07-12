package com.woshiwangnima.healthdietpro.common.ui

internal enum class SoftwareKeyboardMode {
    LETTERS,
    NUMBERS,
    SYMBOLS,
}

internal data class SoftwareKeyboardLayout(
    val rows: List<List<String>>,
)

internal fun softwareKeyboardLayout(
    mode: SoftwareKeyboardMode,
    uppercase: Boolean,
): SoftwareKeyboardLayout = when (mode) {
    SoftwareKeyboardMode.LETTERS -> SoftwareKeyboardLayout(
        rows = listOf(
            "qwertyuiop".map { it.toString() },
            "asdfghjkl".map { it.toString() },
            "zxcvbnm".map { it.toString() },
        ).map { row -> if (uppercase) row.map(String::uppercase) else row },
    )
    SoftwareKeyboardMode.NUMBERS -> SoftwareKeyboardLayout(
        rows = listOf(
            "1234567890".map { it.toString() },
            "-/:;()$&@\"".map { it.toString() },
            listOf(".", ",", "?", "!", "'"),
        ),
    )
    SoftwareKeyboardMode.SYMBOLS -> SoftwareKeyboardLayout(
        rows = listOf(
            listOf("[", "]", "{", "}", "#", "%", "^", "*", "+", "="),
            listOf("_", "\\", "|", "~", "<", ">"),
            listOf(".", ",", "?", "!", "'"),
        ),
    )
}
