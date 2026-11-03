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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brendon978.menulauncher.apps.AppEntry
import com.brendon978.menulauncher.apps.LetterIndex
import com.brendon978.menulauncher.pet.petObstacle
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
                .padding(start = 22.dp, end = 52.dp)
                .petObstacle("list"),
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
        AlphabetBar(
            selected = selected,
            available = groups.keys,
            color = color,
            onSelect = { selected = it },
            onSearch = onSearch,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .heightIn(max = half)
                .padding(end = 4.dp, bottom = 8.dp)
                .petObstacle("bar"),
        )
    }
}
