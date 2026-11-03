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
}
