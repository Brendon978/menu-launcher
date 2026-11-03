package com.brendon978.menulauncher.apps

import org.junit.Assert.assertEquals
import org.junit.Test

class AppSearchTest {
    private fun app(label: String) = AppEntry(AppKey("p.$label", "A", 0), label)
    private val apps = listOf(app("Câmera"), app("Calculadora"), app("Spotify"), app("YT Music"), app("Música"))

    @Test fun vazioNaoMostraNada() {
        assertEquals(emptyList<AppEntry>(), searchApps(apps, "  "))
    }

    @Test fun ignoraAcentoECaixa() {
        assertEquals(listOf("Câmera"), searchApps(apps, "CAME").map { it.label })
        assertEquals(listOf("Música", "YT Music"), searchApps(apps, "musi").map { it.label })
    }

    @Test fun comecoDoNomeVemAntesDoMeio() {
        assertEquals(listOf("Calculadora", "Câmera", "Música"), searchApps(apps, "ca").map { it.label })
        assertEquals(listOf("Música", "YT Music"), searchApps(apps, "mus").map { it.label })
    }

    @Test fun comecoDePalavraTambemConta() {
        assertEquals(listOf("YT Music"), searchApps(listOf(app("Amusement"), app("YT Music")), "mu").map { it.label }.take(1))
    }
}
