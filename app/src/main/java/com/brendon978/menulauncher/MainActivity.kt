package com.brendon978.menulauncher

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.lifecycleScope
import com.brendon978.menulauncher.apps.AppRepository
import com.brendon978.menulauncher.home.HomeScreen
import com.brendon978.menulauncher.icons.AppFilter
import com.brendon978.menulauncher.icons.IconRenderer
import com.brendon978.menulauncher.music.MusicCard
import com.brendon978.menulauncher.music.MusicControls
import com.brendon978.menulauncher.music.MusicRepository
import com.brendon978.menulauncher.settings.FavoritesOps
import com.brendon978.menulauncher.settings.LauncherSettings
import com.brendon978.menulauncher.settings.SettingsRepository
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var appRepo: AppRepository
    private lateinit var settingsRepo: SettingsRepository
    private lateinit var renderer: IconRenderer
    private lateinit var musicRepo: MusicRepository
    private val resetSignal = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        appRepo = AppRepository(this)
        settingsRepo = SettingsRepository(this)
        val filter = assets.open("lawnicons/appfilter.xml").use { AppFilter.parse(it) }
        musicRepo = MusicRepository(this)
        renderer = IconRenderer(this, filter) { key -> appRepo.activityInfo(key)?.getIcon(0) }

        // Launcher: voltar não sai da tela inicial.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = Unit
        })

        setContent {
            val apps by appRepo.apps.collectAsState()
            val settings by settingsRepo.settings.collectAsState(initial = LauncherSettings())
            val music by musicRepo.state.collectAsState()
            val color = Color(settings.strokeColor)
            val current = music.takeIf { settings.showMusic }

            // App desinstalado sai dos favoritos e das trocas de ícone.
            LaunchedEffect(apps) {
                if (apps.isNotEmpty()) {
                    val installed = apps.map { it.key }.toSet()
                    settingsRepo.update { FavoritesOps.prune(it, installed) }
                }
            }

            HomeScreen(
                apps = apps,
                settings = settings,
                renderer = renderer,
                resetSignal = resetSignal.intValue,
                onLaunch = { appRepo.launch(it.key) },
                onAppLongPress = { entry ->
                    // Task 8: menu do app. Por enquanto, favoritar/desfavoritar.
                    lifecycleScope.launch {
                        settingsRepo.update { it.copy(favorites = FavoritesOps.toggle(it.favorites, entry.key)) }
                    }
                },
                onEmptyLongPress = {},
                musicSlotTop = { current?.let { MusicCard(it, color) } },
                musicSlotBottom = {
                    current?.let {
                        MusicControls(it.playing, color, musicRepo::previous, musicRepo::playPause, musicRepo::next)
                    }
                },
            )
        }
    }

    override fun onResume() {
        super.onResume()
        musicRepo.refresh()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Botão home com o launcher já aberto: volta para os favoritos.
        resetSignal.intValue++
    }
}
