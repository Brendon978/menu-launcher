package com.brendon978.menulauncher.pet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GlitchGamesTest {
    private val area = Area(0f, 0f, 100f, 100f)

    @Test fun bugRebateNaBorda() {
        val b = moveBug(Bug(1, 99f, 50f, 100f, 0f), .1f, area)
        assertEquals(100f, b.x)
        assertEquals(-100f, b.vx)
    }

    @Test fun bugAndaEmLinhaReta() {
        val b = moveBug(Bug(1, 10f, 10f, 10f, 20f), 1f, area)
        assertEquals(20f, b.x)
        assertEquals(30f, b.y)
    }

    @Test fun placarDaCaca() {
        assertTrue(bugScoreLine(5, 3).contains("venceu"))
        assertTrue(bugScoreLine(3, 5).contains("Exterminador"))
        assertTrue(bugScoreLine(4, 4).contains("Empate"))
    }

    @Test fun duelo() {
        assertEquals(DuelRound.YOU, duelRound(200, 300, false))
        assertEquals(DuelRound.HIM, duelRound(400, 300, false))
        assertEquals("sem toque, ponto dele", DuelRound.HIM, duelRound(null, 300, false))
        assertEquals("queimou a largada", DuelRound.HIM, duelRound(100, 300, true))
    }

    @Test fun recorde() {
        assertEquals(250L, bestTime(0L, 250))
        assertEquals(200L, bestTime(250L, 200))
        assertEquals(200L, bestTime(200L, 300))
    }

    @Test fun barriga() {
        assertEquals(1f, bellyAfterMeal(.9f))
        assertEquals(.5f, bellyAfterTime(.6f, 18f), .001f)
        assertEquals(0f, bellyAfterTime(.1f, 60f))
    }
}
