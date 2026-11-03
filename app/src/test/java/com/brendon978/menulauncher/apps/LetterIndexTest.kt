package com.brendon978.menulauncher.apps

import org.junit.Assert.assertEquals
import org.junit.Test

class LetterIndexTest {
    private fun app(label: String) = AppEntry(AppKey("p.$label", "A", 0), label)

    @Test fun acentoVaiParaLetraSemAcento() {
        assertEquals('I', LetterIndex.letterOf("Ícone"))
        assertEquals('O', LetterIndex.letterOf("ônibus"))
    }

    @Test fun naoLetraVaiParaOutros() {
        assertEquals('#', LetterIndex.letterOf("9GAG"))
        assertEquals('#', LetterIndex.letterOf("+Rápido"))
        assertEquals('#', LetterIndex.letterOf("😀 Emoji"))
        assertEquals('#', LetterIndex.letterOf(""))
    }

    @Test fun agrupaOrdenadoSemAcentoNemCaixa() {
        val g = LetterIndex.group(listOf(app("banana"), app("árvore"), app("Abacate"), app("9x")))
        assertEquals(listOf("Abacate", "árvore"), g['A']!!.map { it.label })
        assertEquals(listOf("banana"), g['B']!!.map { it.label })
        assertEquals(listOf("9x"), g['#']!!.map { it.label })
    }

    @Test fun barraTemEstrelaLetrasEOutros() {
        assertEquals('★', LetterIndex.BAR.first())
        assertEquals('#', LetterIndex.BAR.last())
        assertEquals(28, LetterIndex.BAR.size)
    }
}
