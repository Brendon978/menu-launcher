package com.brendon978.menulauncher.pet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class PetBrainTest {
    private fun mood(hour: Int = 12, battery: Int? = 80, charging: Boolean = false, music: Boolean = false, happyUntil: Long = 0, now: Long = 1000) =
        baseMood(hour, battery, charging, music, happyUntil, now)

    @Test fun dormeDas23As7() {
        assertEquals(Mood.SLEEPING, mood(hour = 23))
        assertEquals(Mood.SLEEPING, mood(hour = 3))
        assertEquals(Mood.NORMAL, mood(hour = 7))
        assertEquals(Mood.NORMAL, mood(hour = 22))
    }

    @Test fun cansadoComBateriaBaixaSemCarregar() {
        assertEquals(Mood.TIRED, mood(battery = 15))
        assertEquals(Mood.NORMAL, mood(battery = 16))
        assertEquals(Mood.NORMAL, mood(battery = 10, charging = true))
        assertEquals(Mood.NORMAL, mood(battery = null))
    }

    @Test fun prioridadeDormindoCansadoMusicaFeliz() {
        assertEquals(Mood.SLEEPING, mood(hour = 2, battery = 5, music = true))
        assertEquals(Mood.TIRED, mood(battery = 5, music = true))
        assertEquals(Mood.DANCING, mood(music = true, happyUntil = 5000))
        assertEquals(Mood.HAPPY, mood(happyUntil = 5000, now = 1000))
        assertEquals(Mood.NORMAL, mood(happyUntil = 5000, now = 5000))
    }

    @Test fun felizSoQuandoNotificacoesZeram() {
        assertEquals(61_000L, happyUntil(prev = 3, new = 0, now = 1_000, current = 0))
        assertEquals(0L, happyUntil(prev = 0, new = 0, now = 1_000, current = 0))
        assertEquals(0L, happyUntil(prev = null, new = 0, now = 1_000, current = 0))
        assertEquals(500L, happyUntil(prev = 2, new = 1, now = 1_000, current = 500))
    }

    @Test fun toqueDeVezEmQuandoESempreRisadinha() {
        var a = Annoyance()
        repeat(5) { i ->
            val (next, r) = a.tap(now = i * 10_000L)
            assertEquals(Reaction.GIGGLE, r)
            a = next
        }
    }

    @Test fun importunarSobeAteFugirEDepoisFicaSerio() {
        var a = Annoyance()
        var afterFlee: Annoyance? = null
        val reactions = (0 until 8).map { i ->
            val (next, r) = a.tap(now = i * 200L)
            if (r == Reaction.FLEE && afterFlee == null) afterFlee = next
            a = next
            r
        }
        assertEquals(Reaction.GIGGLE, reactions.first())
        assertTrue(Reaction.SERIOUS in reactions)
        assertTrue(Reaction.ANGRY in reactions)
        val flee = reactions.indexOf(Reaction.FLEE)
        assertTrue("deve fugir", flee > 0)
        assertTrue(reactions.indexOf(Reaction.ANGRY) < flee)
        assertEquals(Reaction.SERIOUS, afterFlee!!.levelReaction(afterFlee!!.at))
    }

    @Test fun irritacaoBaixaSozinha() {
        var a = Annoyance()
        repeat(4) { i -> a = a.tap(i * 200L).first }
        assertTrue(a.levelAt(600) > 2f)
        assertEquals(0f, a.levelAt(60_000), 0.001f)
    }

    @Test fun chaoNaoPassaNaBarraNemNaIa() {
        // tela 400 de largura, robô 60, margem 16, barra ocupa 330..400
        assertEquals(16f..270f, floorRange(width = 400f, robotWidth = 60f, margin = 16f, barLeft = 330f, aiLeft = null))
        // IA cruzando o chão em x=250: o robô para antes dela
        assertEquals(16f..190f, floorRange(width = 400f, robotWidth = 60f, margin = 16f, barLeft = 330f, aiLeft = 250f))
        // espaço impossível: fica parado na margem
        assertEquals(16f..16f, floorRange(width = 100f, robotWidth = 60f, margin = 16f, barLeft = 60f, aiLeft = null))
    }

    @Test fun destinoDentroDaFaixaELongeDoAtual() {
        val r = Random(42)
        repeat(200) {
            val t = nextTarget(r, 16f..270f, current = 100f, minDistance = 40f)
            assertTrue(t in 16f..270f)
            assertTrue(kotlin.math.abs(t - 100f) >= 40f)
        }
        assertEquals(16f, nextTarget(r, 16f..16f, current = 16f, minDistance = 40f))
    }
}
