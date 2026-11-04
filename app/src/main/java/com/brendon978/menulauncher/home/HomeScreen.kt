package com.brendon978.menulauncher.home

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import com.brendon978.menulauncher.pet.Area
import com.brendon978.menulauncher.pet.LocalPetObstacles
import com.brendon978.menulauncher.pet.PetObstacles
import com.brendon978.menulauncher.pet.RobotPet
import com.brendon978.menulauncher.pet.petObstacle
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.brendon978.menulauncher.apps.AppEntry
import com.brendon978.menulauncher.icons.IconRenderer
import com.brendon978.menulauncher.settings.HomeMode
import com.brendon978.menulauncher.settings.LauncherSettings

@Composable
fun HomeScreen(
    apps: List<AppEntry>,
    settings: LauncherSettings,
    renderer: IconRenderer,
    resetSignal: Int,
    onLaunch: (AppEntry) -> Unit,
    onAppLongPress: (AppEntry) -> Unit,
    onEmptyLongPress: () -> Unit,
    musicSlotTop: @Composable () -> Unit = {},
    musicSlotBottom: @Composable () -> Unit = {},
    onSearch: () -> Unit = {},
    onSwipeDown: () -> Unit = {},
    onSwipeUp: () -> Unit = {},
    onEmptyDoubleTap: () -> Unit = {},
    musicPlaying: Boolean = false,
    /** Desenhado por cima de tudo (símbolo flutuante da IA). */
    overlay: @Composable () -> Unit = {},
) {
    val color = Color(settings.strokeColor)
    val obstacles = remember { PetObstacles() }
    CompositionLocalProvider(LocalPetObstacles provides obstacles) {
    Box(
        Modifier
            .fillMaxSize()
            // só observa os toques (não consome): os olhos do Glitch seguem o seu dedo
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val ev = awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                        ev.changes.firstOrNull()?.let {
                            com.brendon978.menulauncher.pet.GlitchSignals.lastTouch = Triple(it.position.x, it.position.y, System.currentTimeMillis())
                        }
                    }
                }
            }
            .pointerInput(settings.doubleTapLock) {
                detectTapGestures(
                    onLongPress = { onEmptyLongPress() },
                    // só escuta toque duplo com a opção ligada (senão o toque simples nem espera)
                    onDoubleTap = if (settings.doubleTapLock) ({ onEmptyDoubleTap() }) else null,
                )
            }
            // deslizar para baixo: notificações; para cima: busca
            .pointerInput(Unit) {
                var dy = 0f
                detectVerticalDragGestures(
                    onDragStart = { dy = 0f },
                    onDragEnd = {
                        val min = 90.dp.toPx()
                        if (dy > min) onSwipeDown() else if (dy < -min) onSwipeUp()
                    },
                ) { _, d -> dy += d }
            }
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        when (settings.mode) {
            HomeMode.ONE_HAND -> {
                Column(Modifier.padding(start = 22.dp, end = 22.dp, top = 24.dp).petObstacle("top")) {
                    TopInfo(color, showNextEvent = settings.showNextEvent)
                    if (settings.showQuickActions) QuickActions(color)
                    musicSlotTop()
                }
                OneHandMode(
                    apps = apps,
                    settings = settings,
                    renderer = renderer,
                    resetSignal = resetSignal,
                    onLaunch = onLaunch,
                    onAppLongPress = onAppLongPress,
                    musicSlotBottom = musicSlotBottom,
                    onSearch = onSearch,
                )
            }
            // Grade: player ao lado do relógio; a grade começa logo abaixo do que for mais alto.
            HomeMode.GRID -> Column(Modifier.fillMaxSize()) {
                Row(Modifier.padding(start = 22.dp, end = 22.dp, top = 24.dp, bottom = 12.dp)) {
                    Column {
                        TopInfo(color, showNextEvent = settings.showNextEvent)
                        if (settings.showQuickActions) QuickActions(color)
                    }
                    Box(Modifier.weight(1f).padding(start = 16.dp)) { musicSlotTop() }
                }
                GridMode(
                    apps = apps,
                    settings = settings,
                    renderer = renderer,
                    onLaunch = onLaunch,
                    onAppLongPress = onAppLongPress,
                    musicSlotBottom = musicSlotBottom,
                    onSearch = onSearch,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        if (settings.mode == HomeMode.ONE_HAND && settings.showPet) PetLayer(color, musicPlaying, settings.glitchAccessory)
        overlay()
    }
    }
}

/** Camada do robozinho: a tela inteira, menos o que está marcado como obstáculo (com folga). */
@Composable
private fun PetLayer(color: Color, musicPlaying: Boolean, accessory: String) {
    val reg = LocalPetObstacles.current ?: return
    val density = LocalDensity.current
    var origin by remember { mutableStateOf(Offset.Zero) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    Box(
        Modifier
            .fillMaxSize()
            .onGloballyPositioned { origin = it.positionInRoot(); size = it.size },
    ) {
        if (size == IntSize.Zero) return@Box
        val gap = with(density) { 8.dp.toPx() }
        val margin = with(density) { 12.dp.toPx() }
        val obstacles = reg.rects.values.map {
            Area(it.left - origin.x - gap, it.top - origin.y - gap, it.right - origin.x + gap, it.bottom - origin.y + gap)
        }
        RobotPet(
            color = color,
            musicPlaying = musicPlaying,
            area = Area(margin, margin, size.width - margin, size.height - margin),
            obstacles = obstacles,
            accessory = runCatching { com.brendon978.menulauncher.pet.Accessory.valueOf(accessory) }.getOrDefault(com.brendon978.menulauncher.pet.Accessory.NONE),
        )
    }
}
