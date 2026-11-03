package com.brendon978.menulauncher.home

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
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
) {
    val color = Color(settings.strokeColor)
    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures(onLongPress = { onEmptyLongPress() }) }
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Column(Modifier.padding(start = 22.dp, end = 22.dp, top = 24.dp)) {
            TopInfo(color)
            if (settings.mode == HomeMode.ONE_HAND) musicSlotTop()
        }
        when (settings.mode) {
            HomeMode.ONE_HAND -> OneHandMode(
                apps = apps,
                settings = settings,
                renderer = renderer,
                resetSignal = resetSignal,
                onLaunch = onLaunch,
                onAppLongPress = onAppLongPress,
                musicSlotBottom = musicSlotBottom,
            )
            HomeMode.GRID -> Unit // Task 6
        }
    }
}
