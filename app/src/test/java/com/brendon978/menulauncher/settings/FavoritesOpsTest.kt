package com.brendon978.menulauncher.settings

import com.brendon978.menulauncher.apps.AppKey
import org.junit.Assert.assertEquals
import org.junit.Test

class FavoritesOpsTest {
    private val a = AppKey("a", "A", 0)
    private val b = AppKey("b", "B", 0)
    private val c = AppKey("c", "C", 0)

    @Test fun toggleAdicionaNoFimERemove() {
        val one = FavoritesOps.toggle(listOf(a), b)
        assertEquals(listOf(a, b), one)
        assertEquals(listOf(a), FavoritesOps.toggle(one, b))
    }

    @Test fun moveReordena() {
        assertEquals(listOf(b, c, a), FavoritesOps.move(listOf(a, b, c), 0, 2))
    }

    @Test fun pruneTiraDesinstaladosDosFavoritosEDasTrocas() {
        val s = LauncherSettings(favorites = listOf(a, b), iconOverrides = mapOf(b to "x", c to "y"))
        val r = FavoritesOps.prune(s, setOf(a))
        assertEquals(listOf(a), r.favorites)
        assertEquals(emptyMap<AppKey, String>(), r.iconOverrides)
    }

    @Test fun pruneSemMudancaDevolveIgual() {
        val s = LauncherSettings(favorites = listOf(a))
        assertEquals(s, FavoritesOps.prune(s, setOf(a, b)))
    }
}
