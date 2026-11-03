package com.brendon978.menulauncher.settings

import com.brendon978.menulauncher.apps.AppKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BackupTest {
    private val a = AppKey("com.a", "A", 0)
    private val b = AppKey("com.b", "B", 10)

    @Test fun idaEVoltaGuardaTudo() {
        val s = LauncherSettings(
            mode = HomeMode.GRID, favorites = listOf(a, b), strokeColor = 0xFF7FB7A4.toInt(),
            iconOverrides = mapOf(a to "google_chrome"), showMusic = false, firstRunDone = true,
            aiPackage = "com.brendon.iris", aiActivity = "x.Main", aiX = .3f, aiY = .7f,
            showPet = false, glitchAccessory = "CAP", customLabels = mapOf(b to "Meu B"), hiddenApps = setOf(a),
        )
        assertEquals(s, Backup.decode(Backup.encode(s)))
    }

    @Test fun arquivoEstranhoNaoQuebra() {
        assertNull(Backup.decode("isso não é um backup"))
    }

    @Test fun nomesComCaracteresEspeciais() {
        val s = LauncherSettings(customLabels = mapOf(a to "Ação = é\\n#1 ✓"))
        assertEquals(s.customLabels, Backup.decode(Backup.encode(s))!!.customLabels)
    }
}
