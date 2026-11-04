package com.brendon978.menulauncher.settings

import com.brendon978.menulauncher.apps.AppKey
import com.brendon978.menulauncher.wallpaper.WallpaperInterval
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
            folders = mapOf("Jogos" to listOf(a, b)), doubleTapLock = true,
            showMostUsed = true, showQuickActions = true, showNextEvent = true,
        )
        assertEquals(s, Backup.decode(Backup.encode(s)))
    }

    @Test fun rodadaTresVaiNoBackup() {
        val s = LauncherSettings(
            wallpaperFolder = "content://x/tree/primary%3AFundos", wallpaperInterval = WallpaperInterval.SIX_HOURS,
            wallpaperLastChange = 1_800_000_000_000L, wallpaperLastImage = "praia.jpg",
            strokeAuto = StrokeAuto.BY_TIME, strokeDay = 0xFF7FB7A4.toInt(), strokeNight = 0xFFC9A27E.toInt(),
        )
        assertEquals(s, Backup.decode(Backup.encode(s)))
    }

    @Test fun backupAntigoCaiNosPadroes() {
        assertEquals(LauncherSettings(), Backup.decode("menu-launcher-backup=1"))
    }

    @Test fun arquivoEstranhoNaoQuebra() {
        assertNull(Backup.decode("isso não é um backup"))
    }

    @Test fun nomesComCaracteresEspeciais() {
        val s = LauncherSettings(customLabels = mapOf(a to "Ação = é\\n#1 ✓"))
        assertEquals(s.customLabels, Backup.decode(Backup.encode(s))!!.customLabels)
    }

    @Test fun usoVaiNoBackup() {
        val s = LauncherSettings(usage = mapOf(a to 3.5f, b to .25f))
        assertEquals(s.usage, Backup.decode(Backup.encode(s))!!.usage)
    }
}
