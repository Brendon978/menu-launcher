package com.brendon978.menulauncher.settings

import com.brendon978.menulauncher.apps.AppKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GesturesTest {
    private val app = AppKey("com.a", "com.a.Main", 10)

    @Test fun padroesSaoOComportamentoDeAntes() {
        val s = LauncherSettings()
        assertEquals(GestureAction.NOTIFICATIONS, s.gestureDown.action)
        assertEquals(GestureAction.SEARCH, s.gestureUp.action)
        assertEquals(GestureAction.NONE, s.doubleTapChoice().action)
        assertFalse(s.listensDoubleTap())
    }

    @Test fun gestoSalvoDeApagarTelaNaoQuebra() {
        assertEquals(null, GestureChoice.decode("LOCK"))
    }

    @Test fun escolhaNosGestosValeNosDoisToques() {
        val s = LauncherSettings().withDoubleTap(GestureChoice(GestureAction.CAMERA))
        assertEquals(GestureAction.CAMERA, s.doubleTapChoice().action)
    }

    @Test fun idaEVoltaDoTexto() {
        GestureAction.entries.filter { it != GestureAction.APP }.forEach {
            assertEquals(GestureChoice(it), GestureChoice.decode(GestureChoice(it).encode()))
        }
        val c = GestureChoice(GestureAction.APP, app)
        assertEquals(c, GestureChoice.decode(c.encode()))
    }

    @Test fun textoRuimVoltaNull() {
        assertNull(GestureChoice.decode(null))
        assertNull(GestureChoice.decode("VOAR"))
        assertNull(GestureChoice.decode("APP"))
        assertNull(GestureChoice.decode("APP|nada"))
    }

    @Test fun lanternaSoEhVigiadaSeUsada() {
        assertFalse(LauncherSettings().usesTorchGesture())
        assertTrue(LauncherSettings(gestureUp = GestureChoice(GestureAction.TORCH)).usesTorchGesture())
        assertTrue(LauncherSettings().withDoubleTap(GestureChoice(GestureAction.TORCH)).usesTorchGesture())
    }
}
