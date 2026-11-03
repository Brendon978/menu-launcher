package com.brendon978.menulauncher.icons

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OutlineTracerTest {
    private val size = 192

    private fun square(from: Int, to: Int) = IntArray(size * size) { i ->
        val x = i % size; val y = i / size
        if (x in from until to && y in from until to) 255 else 0
    }

    @Test fun quadradoCheioViraBorda() {
        val out = OutlineTracer.trace(square(46, 146), size, 14)
        assertFalse("centro deve ficar vazio", out[96 * size + 96])
        assertTrue("borda esquerda deve ser traço", out[96 * size + 46])
        assertFalse("longe do quadrado fica vazio", out[96 * size + 10])
    }

    @Test fun transparenteDaVazio() {
        assertTrue(OutlineTracer.trace(IntArray(size * size), size, 14).none { it })
    }
}
