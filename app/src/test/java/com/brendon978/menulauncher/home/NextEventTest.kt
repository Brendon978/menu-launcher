package com.brendon978.menulauncher.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class NextEventTest {
    private val tz = TimeZone.getTimeZone("America/Sao_Paulo")
    private fun at(day: Int, h: Int, m: Int = 0) = Calendar.getInstance(tz).apply { clear(); set(2026, 10, day, h, m) }.timeInMillis

    @Test fun hojeComHora() {
        assertEquals("16:00 · Dentista", formatNextEvent("Dentista", at(3, 16), at(3, 17), false, now = at(3, 10), tz = tz))
    }

    @Test fun acontecendoAgora() {
        assertEquals("Agora · Reunião", formatNextEvent("Reunião", at(3, 9), at(3, 11), false, now = at(3, 10), tz = tz))
    }

    @Test fun amanha() {
        assertEquals("Amanhã 09:30 · Academia", formatNextEvent("Academia", at(4, 9, 30), at(4, 10), false, now = at(3, 22), tz = tz))
    }

    @Test fun diaInteiro() {
        assertEquals("Hoje · Aniversário da Ana", formatNextEvent("Aniversário da Ana", at(3, 0), at(4, 0), true, now = at(3, 10), tz = tz))
    }

    @Test fun jaAcabouNaoMostra() {
        assertNull(formatNextEvent("Antigo", at(3, 8), at(3, 9), false, now = at(3, 10), tz = tz))
    }

    @Test fun semTituloVira() {
        assertEquals("16:00 · Compromisso", formatNextEvent(" ", at(3, 16), at(3, 17), false, now = at(3, 10), tz = tz))
    }
}
