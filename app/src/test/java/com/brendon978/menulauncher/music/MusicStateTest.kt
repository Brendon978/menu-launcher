package com.brendon978.menulauncher.music

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MusicStateTest {
    @Test fun semSessaoNadaAparece() {
        assertNull(pickSession(emptyList()))
    }

    @Test fun prefereAQueEstaTocando() {
        val b = SessionInfo("b", playing = true)
        assertEquals(b, pickSession(listOf(SessionInfo("a", playing = false), b)))
    }

    @Test fun todasPausadasPegaAMaisRecente() {
        val a = SessionInfo("a", playing = false)
        assertEquals(a, pickSession(listOf(a, SessionInfo("b", playing = false))))
    }

    @Test fun tocandoSempreAparece() {
        assertEquals(true, shouldShowMusic(playing = true, pausedSince = null, now = 0))
    }

    @Test fun pausadaApareceAteDezMinutos() {
        val t0 = 1_000_000L
        assertEquals(true, shouldShowMusic(playing = false, pausedSince = t0, now = t0 + 9 * 60_000))
        assertEquals(false, shouldShowMusic(playing = false, pausedSince = t0, now = t0 + 10 * 60_000))
    }

    @Test fun pausadaSemMarcaNaoAparece() {
        assertEquals(false, shouldShowMusic(playing = false, pausedSince = null, now = 0))
    }
}
