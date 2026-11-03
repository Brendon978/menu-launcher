package com.brendon978.menulauncher.apps

import java.text.Collator
import java.text.Normalizer
import java.util.Locale

object LetterIndex {
    const val STAR = '★'
    const val OTHER = '#'

    val BAR: List<Char> = listOf(STAR) + ('A'..'Z') + OTHER

    private val DIACRITICS = Regex("\\p{M}+")

    val collator: Collator = Collator.getInstance(Locale.forLanguageTag("pt-BR")).apply {
        strength = Collator.PRIMARY
    }

    fun letterOf(label: String): Char {
        val first = label.trimStart().firstOrNull() ?: return OTHER
        val base = Normalizer.normalize(first.toString(), Normalizer.Form.NFD).replace(DIACRITICS, "")
        val c = base.firstOrNull()?.uppercaseChar() ?: return OTHER
        return if (c in 'A'..'Z') c else OTHER
    }

    fun sorted(apps: List<AppEntry>): List<AppEntry> = apps.sortedWith { a, b -> collator.compare(a.label, b.label) }

    fun group(apps: List<AppEntry>): Map<Char, List<AppEntry>> = sorted(apps).groupBy { letterOf(it.label) }
}
