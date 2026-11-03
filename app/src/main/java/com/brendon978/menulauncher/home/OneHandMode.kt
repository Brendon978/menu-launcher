package com.brendon978.menulauncher.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.brendon978.menulauncher.ai.clampFraction
import com.brendon978.menulauncher.pet.ROBOT_ASPECT
import com.brendon978.menulauncher.pet.ROBOT_HEIGHT
import com.brendon978.menulauncher.pet.RobotPet
import com.brendon978.menulauncher.pet.floorRange
import androidx.compose.ui.unit.sp
import com.brendon978.menulauncher.apps.AppEntry
import com.brendon978.menulauncher.apps.LetterIndex
import com.brendon978.menulauncher.icons.IconRenderer
import com.brendon978.menulauncher.settings.LauncherSettings

/**
 * Metade de baixo: favoritos (ou os apps da letra escolhida) + controles de música; barra de letras à direita.
 * [resetSignal] muda quando o usuário volta para a tela inicial: a seleção volta para ★.
 */
@Composable
fun OneHandMode(
    apps: List<AppEntry>,
    settings: LauncherSettings,
    renderer: IconRenderer,
    resetSignal: Int,
    onLaunch: (AppEntry) -> Unit,
    onAppLongPress: (AppEntry) -> Unit,
    musicSlotBottom: @Composable () -> Unit,
    onSearch: () -> Unit,
    musicPlaying: Boolean = false,
    modifier: Modifier = Modifier,
) {
    var selected by rememberSaveable { mutableStateOf(LetterIndex.STAR) }
    LaunchedEffect(resetSignal) { selected = LetterIndex.STAR }

    val groups = remember(apps) { LetterIndex.group(apps) }
    val byKey = remember(apps) { apps.associateBy { it.key } }
    val favorites = remember(settings.favorites, byKey) { settings.favorites.mapNotNull { byKey[it] } }
    val color = Color(settings.strokeColor)

    val shown = if (selected == LetterIndex.STAR) favorites else groups[selected].orEmpty()
    val header = when {
        selected == LetterIndex.STAR -> "Favoritos"
        shown.isEmpty() -> "$selected · nenhum app"
        else -> selected.toString()
    }

    BoxWithConstraints(modifier.fillMaxSize()) {
        val half = maxHeight / 2
        Column(
            verticalArrangement = Arrangement.Bottom,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .heightIn(max = half)
                .padding(start = 22.dp, end = 52.dp),
        ) {
            Text(header, color = color.copy(alpha = 0.6f), fontSize = 12.sp, modifier = Modifier.padding(bottom = 4.dp))
            LazyColumn(
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier.weight(1f, fill = false),
            ) {
                items(shown, key = { it.key.encode() }) { entry ->
                    AppRow(
                        entry = entry,
                        override = settings.iconOverrides[entry.key],
                        color = settings.strokeColor,
                        renderer = renderer,
                        onClick = { onLaunch(entry) },
                        onLongClick = { onAppLongPress(entry) },
                    )
                }
            }
            musicSlotBottom()
            Box(Modifier.height(8.dp))
        }
        if (settings.showPet) {
            val density = LocalDensity.current
            val widthPx = with(density) { maxWidth.toPx() }
            val heightPx = with(density) { maxHeight.toPx() }
            val floorY = with(density) { (half - 14.dp).toPx() }
            val robotW = with(density) { ROBOT_HEIGHT.toPx() } * ROBOT_ASPECT
            val robotTop = floorY - with(density) { ROBOT_HEIGHT.toPx() }
            // símbolo da IA (56dp), se cruzar o chão: o robô não passa por ele
            val aiSize = with(density) { 56.dp.toPx() }
            val aiLeft = settings.aiPackage?.let { clampFraction(settings.aiX) * (widthPx - aiSize) }
            val aiTop = settings.aiPackage?.let { clampFraction(settings.aiY) * (heightPx - aiSize) }
            val aiCrosses = aiLeft != null && aiTop != null && aiTop < floorY && aiTop + aiSize > robotTop
            val margin = with(density) { 16.dp.toPx() }
            val barLeft = widthPx - with(density) { 64.dp.toPx() }
            var range = floorRange(widthPx, robotW, margin, barLeft, aiLeft.takeIf { aiCrosses && it!! > widthPx / 3 })
            if (aiCrosses && aiLeft!! <= widthPx / 3) {
                // IA à esquerda: o chão começa depois dela
                val start = (aiLeft + aiSize + margin / 2).coerceAtMost(range.endInclusive)
                range = start..range.endInclusive
            }
            RobotPet(color = color, musicPlaying = musicPlaying, floorY = floorY, range = range)
        }
        AlphabetBar(
            selected = selected,
            available = groups.keys,
            color = color,
            onSelect = { selected = it },
            onSearch = onSearch,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .heightIn(max = half)
                .padding(end = 4.dp, bottom = 8.dp),
        )
    }
}
