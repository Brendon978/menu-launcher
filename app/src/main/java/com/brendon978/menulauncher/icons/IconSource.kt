package com.brendon978.menulauncher.icons

import com.brendon978.menulauncher.apps.AppKey
import com.brendon978.menulauncher.apps.LetterIndex

sealed interface IconSource {
    data class Lawnicon(val name: String) : IconSource
    data class Monochrome(val key: AppKey) : IconSource
    data class Initial(val letter: Char) : IconSource
}

/** Ordem do design: troca manual -> Lawnicons -> monocromático em contorno -> inicial. */
fun resolveIcon(
    key: AppKey,
    label: String,
    override: String?,
    filter: AppFilter,
    hasMonochrome: (AppKey) -> Boolean,
): IconSource {
    override?.let { return IconSource.Lawnicon(it) }
    filter.drawableFor(key.packageName, key.activityName)?.let { return IconSource.Lawnicon(it) }
    if (hasMonochrome(key)) return IconSource.Monochrome(key)
    return IconSource.Initial(initialOf(label))
}

internal fun initialOf(label: String): Char {
    val letter = LetterIndex.letterOf(label)
    if (letter != LetterIndex.OTHER) return letter
    val c = label.trimStart().firstOrNull() ?: return '?'
    return if (c.isSurrogate()) '?' else c.uppercaseChar()
}
