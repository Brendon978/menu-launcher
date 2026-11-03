package com.brendon978.menulauncher.wallpaper

import org.junit.Assert.assertEquals
import org.junit.Test

class WallpaperKindTest {
    @Test fun gifEAnimadoMesmoSendoImagem() {
        assertEquals(WallpaperKind.Animated("gif"), classify("image/gif", null))
        assertEquals(WallpaperKind.Animated("gif"), classify(null, "x.GIF"))
    }

    @Test fun videoEAnimado() {
        assertEquals(WallpaperKind.Animated("mp4"), classify("video/mp4", null))
        assertEquals(WallpaperKind.Animated("mp4"), classify(null, "clip.webm"))
    }

    @Test fun imagemComumEEstatica() {
        assertEquals(WallpaperKind.Static, classify("image/webp", null))
        assertEquals(WallpaperKind.Static, classify("image/jpeg", "a.jpg"))
        assertEquals(WallpaperKind.Static, classify(null, "foto.PNG"))
    }

    @Test fun mimeVenceExtensao() {
        assertEquals(WallpaperKind.Static, classify("image/png", "estranho.gif"))
    }

    @Test fun naoSuportado() {
        assertEquals(WallpaperKind.Unsupported, classify("application/pdf", "a.pdf"))
        assertEquals(WallpaperKind.Unsupported, classify(null, null))
        assertEquals(WallpaperKind.Unsupported, classify("audio/mpeg", "m.mp3"))
    }

    @Test fun flagsDoDestino() {
        assertEquals(1, StaticTarget.HOME.flags())
        assertEquals(2, StaticTarget.LOCK.flags())
        assertEquals(3, StaticTarget.BOTH.flags())
    }
}
