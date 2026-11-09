package com.brendon978.menulauncher.pet

import androidx.compose.ui.graphics.Color
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GlitchFriendsTest {
    @Test fun corIdaEVolta() {
        val c = Color(0xFF7FB7A4)
        assertEquals("#7FB7A4", colorHex(c))
        assertEquals(c, colorFromHex("#7FB7A4"))
        assertNull(colorFromHex("vermelho"))
    }

    @Test fun visitanteCompleto() {
        val v = parseVisitor(JSONObject("""{"name":"Bit","color":"#7FB7A4","accessory":"CAP","buddy":"CAT"}"""))!!
        assertEquals("Bit", v.name)
        assertEquals(Accessory.CAP, v.accessory)
        assertEquals(Buddy.CAT, v.buddy)
    }

    @Test fun acessorioDesconhecidoNaoDerruba() {
        val v = parseVisitor(JSONObject("""{"name":"Bit","color":"#7FB7A4","accessory":"CAPA_NOVA","buddy":"DRAGAO"}"""))!!
        assertEquals(Accessory.NONE, v.accessory)
        assertEquals(Buddy.NONE, v.buddy)
    }

    @Test fun visitanteSemNomeOuCorEhIgnorado() {
        assertEquals(emptyList<Visitor>(), parseVisits("""{"visitas":[{"name":"","color":"#7FB7A4"},{"name":"Bit","color":"x"}]}"""))
    }

    @Test fun amigoSoTemPerfilQuandoMutuo() {
        val l = parseFriends("""{"amigos":[{"code":"GLITCH-222222","mutual":false},
            {"code":"GLITCH-333333","mutual":true,"name":"Eloy","color":"#112233","accessory":"NONE","buddy":"NONE"}]}""")
        assertNull(l[0].visitor)
        assertEquals("Eloy", l[1].visitor?.name)
    }

    @Test fun erroDesconhecidoViraSemConexao() {
        assertEquals("Sem conexão com o servidor. Tente de novo.", visitFailureText("qualquer"))
    }
}
