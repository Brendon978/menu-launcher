package com.brendon978.menulauncher.music

import android.graphics.Bitmap

data class MusicState(
    val title: String,
    val artist: String?,
    val appLabel: String,
    val art: Bitmap?,
    val playing: Boolean,
)

data class SessionInfo(val packageName: String, val playing: Boolean)

/** Prefere a sessão que está tocando; empate = a primeira (a mais recente, na ordem do MediaSessionManager). */
fun pickSession(sessions: List<SessionInfo>): SessionInfo? =
    sessions.firstOrNull { it.playing } ?: sessions.firstOrNull()

const val PAUSED_GRACE_MS = 10 * 60_000L

/**
 * Tocando: aparece. Pausada: continua por [PAUSED_GRACE_MS] para dar para retomar pela tela inicial, depois some.
 * Sem saber quando pausou: não aparece.
 */
fun shouldShowMusic(playing: Boolean, pausedSince: Long?, now: Long): Boolean =
    playing || (pausedSince != null && now - pausedSince < PAUSED_GRACE_MS)
