package com.brendon978.menulauncher.pet

import org.junit.Assert.assertEquals
import org.junit.Test

class GlitchClockTest {
    @Test fun brincandoAndaQuadroAQuadro() = assertEquals(0L, clockGapMs(interactive = true, sleeping = true))
    @Test fun paradoAnda30VezesPorSegundo() = assertEquals(33L, clockGapMs(interactive = false, sleeping = false))
    @Test fun dormindoAnda10VezesPorSegundo() = assertEquals(100L, clockGapMs(interactive = false, sleeping = true))
}
