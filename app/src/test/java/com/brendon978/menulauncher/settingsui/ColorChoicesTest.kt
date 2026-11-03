package com.brendon978.menulauncher.settingsui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ColorChoicesTest {
    @Test fun hexDeSeisDigitosViraOpaco() {
        assertEquals(0xFFA0C4FF.toInt(), parseHexColor("#A0C4FF"))
        assertEquals(0xFFA0C4FF.toInt(), parseHexColor(" a0c4ff "))
    }

    @Test fun hexDeOitoDigitosMantemAlfa() {
        assertEquals(0x80FFFFFF.toInt(), parseHexColor("#80FFFFFF"))
    }

    @Test fun incompletoOuInvalidoNaoMuda() {
        assertNull(parseHexColor("#A0C"))
        assertNull(parseHexColor("#GGGGGG"))
        assertNull(parseHexColor(""))
    }

    @Test fun seisOpcoesFixas() {
        assertEquals(6, COLOR_CHOICES.size)
        assertEquals(0xFFFFFFFF.toInt(), COLOR_CHOICES.first())
    }
}
