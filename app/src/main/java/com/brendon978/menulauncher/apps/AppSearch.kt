package com.brendon978.menulauncher.apps

import java.text.Normalizer

private val MARKS = Regex("\\p{M}+")

/** Minúsculas e sem acento: "Câmera" -> "camera". */
fun normalizeForSearch(s: String): String =
    Normalizer.normalize(s, Normalizer.Form.NFD).replace(MARKS, "").lowercase()

/**
 * Apps cujo nome contém a busca (sem acento nem caixa).
 * Ordem: começa com a busca; depois alguma palavra começa com ela; depois contém no meio. Empate: alfabética.
 */
fun searchApps(apps: List<AppEntry>, query: String): List<AppEntry> {
    val q = normalizeForSearch(query.trim())
    if (q.isEmpty()) return emptyList()
    return LetterIndex.sorted(apps)
        .mapNotNull { app ->
            val name = normalizeForSearch(app.label)
            val rank = when {
                name.startsWith(q) -> 0
                name.split(' ', '-', '.', '_').any { it.startsWith(q) } -> 1
                q in name -> 2
                else -> return@mapNotNull null
            }
            rank to app
        }
        .sortedBy { it.first } // estável: mantém a ordem alfabética dentro do mesmo nível
        .map { it.second }
}
