package com.brendon978.menulauncher

import org.junit.Assert.assertFalse
import org.junit.Test
import java.io.File

/** O Nubank bloqueou a conta com o launcher usando acessibilidade: ela não volta sem decisão explícita. */
class ManifestTest {
    @Test fun launcherNaoDeclaraAcessibilidadeNemSobreposicao() {
        val manifest = File("src/main/AndroidManifest.xml").readText()
        assertFalse(manifest.contains("BIND_ACCESSIBILITY_SERVICE"))
        assertFalse(manifest.contains("SYSTEM_ALERT_WINDOW"))
    }
}
