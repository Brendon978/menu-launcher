package com.brendon978.menulauncher.settingsui

/** Branco, areia, cinza, quase preto, verde-sálvia, caramelo. */
val COLOR_CHOICES: List<Int> = listOf(
    0xFFFFFFFF, 0xFFE6E1D6, 0xFFB8B8B8, 0xFF111111, 0xFF7FB7A4, 0xFFC9A27E,
).map { it.toInt() }

private val HEX = Regex("^#?([0-9a-fA-F]{6}|[0-9a-fA-F]{8})$")

/** "#RRGGBB" (opaco) ou "#AARRGGBB"; qualquer outra coisa é null (ainda digitando). */
fun parseHexColor(text: String): Int? {
    val m = HEX.matchEntire(text.trim()) ?: return null
    val digits = m.groupValues[1]
    val value = digits.toLong(16)
    return (if (digits.length == 6) value or 0xFF000000 else value).toInt()
}
