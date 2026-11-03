package com.brendon978.menulauncher.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
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
    if (b == null) Box(Modifier.size(size)) else Image(b, contentDescription = null, modifier = Modifier.size(size))
}
