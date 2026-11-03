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
