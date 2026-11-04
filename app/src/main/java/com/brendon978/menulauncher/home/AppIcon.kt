package com.brendon978.menulauncher.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.brendon978.menulauncher.music.MediaListener
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Dp
import com.brendon978.menulauncher.pet.GlitchSignals
import com.brendon978.menulauncher.apps.AppEntry
import com.brendon978.menulauncher.icons.IconRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Ícone de contorno desenhado fora da thread principal (a conversão do monocromático custa um pouco). */
@Composable
fun AppIcon(entry: AppEntry, override: String?, color: Int, size: Dp, renderer: IconRenderer) {
    val px = with(LocalDensity.current) { size.roundToPx() }
    val bmp by produceState<ImageBitmap?>(null, entry.key, override, color, px) {
        value = withContext(Dispatchers.Default) { renderer.iconFor(entry, override, px, color) }
    }
    val b = bmp
    // bolinha de notificação: só aparece se o app tem notificação "de verdade"
    val badges by MediaListener.badges.collectAsState()
    val pkg = entry.key.packageName
    val badged = pkg in badges && pkg !in GlitchSignals.eaten
    if (badged) DisposableEffect(pkg) { onDispose { GlitchSignals.badgeSpots.remove(pkg) } }
    Box(Modifier.size(size).then(if (badged) Modifier.onGloballyPositioned { GlitchSignals.badgeSpots[pkg] = it.boundsInRoot() } else Modifier)) {
        if (b != null) Image(b, contentDescription = null, modifier = Modifier.size(size))
        if (badged) {
            Canvas(Modifier.size(size)) {
                val r = this.size.minDimension * .17f
                val c = Offset(this.size.width - r * .6f, r * .6f)
                drawCircle(Color(0xFF0C0C0E), r + 1.6f, c)
                drawCircle(Color(color), r, c)
            }
        }
    }
}
