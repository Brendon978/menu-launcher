package com.brendon978.menulauncher

import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherApps
import android.net.Uri
import android.os.Bundle
import android.os.Process
import android.os.UserManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.lifecycleScope
import com.brendon978.menulauncher.apps.AppEntry
import com.brendon978.menulauncher.apps.AppKey
import com.brendon978.menulauncher.apps.AppRepository
import com.brendon978.menulauncher.home.HomeScreen
import com.brendon978.menulauncher.icons.AppFilter
import com.brendon978.menulauncher.icons.IconRenderer
import com.brendon978.menulauncher.menu.AppActionsSheet
import com.brendon978.menulauncher.menu.IconPickerScreen
import com.brendon978.menulauncher.music.MusicCard
import com.brendon978.menulauncher.music.MusicControls
import com.brendon978.menulauncher.music.MusicRepository
import com.brendon978.menulauncher.settings.FavoritesOps
import com.brendon978.menulauncher.settings.LauncherSettings
import com.brendon978.menulauncher.settings.SettingsRepository
import com.brendon978.menulauncher.settingsui.SettingsScreen
import kotlinx.coroutines.launch

private sealed interface Screen {
    data object Home : Screen
    data object Settings : Screen
    data class IconPicker(val entry: AppEntry) : Screen
}

class MainActivity : ComponentActivity() {
    private lateinit var appRepo: AppRepository
    private lateinit var settingsRepo: SettingsRepository
    private lateinit var renderer: IconRenderer
    private lateinit var musicRepo: MusicRepository
    private val resetSignal = mutableIntStateOf(0)
    private val screen = mutableStateOf<Screen>(Screen.Home)
    private val sheetFor = mutableStateOf<AppEntry?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        appRepo = AppRepository(this)
        settingsRepo = SettingsRepository(this)
        musicRepo = MusicRepository(this)
        val filter = assets.open("lawnicons/appfilter.xml").use { AppFilter.parse(it) }
        renderer = IconRenderer(this, filter) { key -> appRepo.activityInfo(key)?.getIcon(0) }

        // Launcher: voltar só fecha telas internas; nunca sai da tela inicial.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                screen.value = Screen.Home
            }
        })

        setContent {
            val apps by appRepo.apps.collectAsState()
            val settings by settingsRepo.settings.collectAsState(initial = LauncherSettings())
            val music by musicRepo.state.collectAsState()
            val byKey = remember(apps) { apps.associateBy { it.key } }
            val color = Color(settings.strokeColor)
            val current = music.takeIf { settings.showMusic }

            // App desinstalado sai dos favoritos e das trocas de ícone.
            LaunchedEffect(apps) {
                if (apps.isNotEmpty()) {
                    val installed = apps.map { it.key }.toSet()
                    settingsRepo.update { FavoritesOps.prune(it, installed) }
                }
            }

            when (val s = screen.value) {
                Screen.Home -> HomeScreen(
                    apps = apps,
                    settings = settings,
                    renderer = renderer,
                    resetSignal = resetSignal.intValue,
                    onLaunch = { appRepo.launch(it.key) },
                    onAppLongPress = { sheetFor.value = it },
                    onEmptyLongPress = { screen.value = Screen.Settings },
                    musicSlotTop = { current?.let { MusicCard(it, color) } },
                    musicSlotBottom = {
                        current?.let {
                            MusicControls(it.playing, color, musicRepo::previous, musicRepo::playPause, musicRepo::next)
                        }
                    },
                )
                Screen.Settings -> SettingsScreen(
                    settings = settings,
                    apps = byKey,
                    onChange = ::updateSettings,
                    onSetDefaultLauncher = ::requestHomeRole,
                    onClose = { screen.value = Screen.Home },
                )
                is Screen.IconPicker -> IconPickerScreen(
                    appLabel = s.entry.label,
                    renderer = renderer,
                    color = 0xFFFFFFFF.toInt(), // prévia sempre legível no fundo escuro
                    assets = assets,
                    onPick = { name ->
                        updateSettings { st ->
                            val o = if (name == null) st.iconOverrides - s.entry.key else st.iconOverrides + (s.entry.key to name)
                            st.copy(iconOverrides = o)
                        }
                        screen.value = Screen.Home
                    },
                )
            }

            sheetFor.value?.let { entry ->
                AppActionsSheet(
                    label = entry.label,
                    isFavorite = entry.key in settings.favorites,
                    canUninstall = canUninstall(entry.key),
                    onToggleFavorite = { updateSettings { it.copy(favorites = FavoritesOps.toggle(it.favorites, entry.key)) } },
                    onChangeIcon = { screen.value = Screen.IconPicker(entry) },
                    onAppInfo = { openAppInfo(entry.key) },
                    onUninstall = { uninstall(entry.key) },
                    onDismiss = { sheetFor.value = null },
                )
            }
        }
    }

    private fun updateSettings(f: (LauncherSettings) -> LauncherSettings) {
        lifecycleScope.launch { settingsRepo.update(f) }
    }

    private fun isMainProfile(key: AppKey) =
        getSystemService(UserManager::class.java).getSerialNumberForUser(Process.myUserHandle()) == key.userSerial

    private fun canUninstall(key: AppKey): Boolean {
        val info = appRepo.activityInfo(key)?.applicationInfo ?: return false
        return isMainProfile(key) && info.flags and ApplicationInfo.FLAG_SYSTEM == 0
    }

    private fun openAppInfo(key: AppKey) {
        val user = appRepo.userOf(key) ?: return
        getSystemService(LauncherApps::class.java)
            .startAppDetailsActivity(android.content.ComponentName(key.packageName, key.activityName), user, null, null)
    }

    private fun uninstall(key: AppKey) {
        startActivity(Intent(Intent.ACTION_DELETE, Uri.parse("package:${key.packageName}")))
    }

    fun requestHomeRole() {
        val rm = getSystemService(RoleManager::class.java)
        if (rm.isRoleAvailable(RoleManager.ROLE_HOME) && !rm.isRoleHeld(RoleManager.ROLE_HOME)) {
            @Suppress("DEPRECATION")
            startActivityForResult(rm.createRequestRoleIntent(RoleManager.ROLE_HOME), REQUEST_HOME)
        } else {
            startActivity(Intent(android.provider.Settings.ACTION_HOME_SETTINGS))
        }
    }

    override fun onResume() {
        super.onResume()
        musicRepo.refresh()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Botão home com o launcher já aberto: fecha menus e volta para os favoritos.
        sheetFor.value = null
        screen.value = Screen.Home
        resetSignal.intValue++
    }

    private companion object {
        const val REQUEST_HOME = 1
    }
}
