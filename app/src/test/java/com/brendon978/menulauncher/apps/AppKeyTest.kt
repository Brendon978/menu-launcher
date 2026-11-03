package com.brendon978.menulauncher.apps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppKeyTest {
    @Test fun mesmoPacoteEmPerfisDiferentesSaoChavesDiferentes() {
        assertNotEquals(AppKey("com.x", "com.x.Main", 0), AppKey("com.x", "com.x.Main", 10))
    }

    @Test fun codificaEDecodifica() {
        val k = AppKey("com.x", "com.x.Main", 10)
        assertEquals(k, AppKey.decode(k.encode()))
    }

    @Test fun lixoNaoDecodifica() {
        assertNull(AppKey.decode("lixo"))
    }
}
