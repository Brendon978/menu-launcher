package com.brendon978.menulauncher.settings

import com.brendon978.menulauncher.apps.AppKey

enum class HomeMode { ONE_HAND, GRID }

data class LauncherSettings(
    val mode: HomeMode = HomeMode.ONE_HAND,
    val favorites: List<AppKey> = emptyList(),
    val strokeColor: Int = 0xFFFFFFFF.toInt(),
    /** valor = nome do drawable do Lawnicons */
    val iconOverrides: Map<AppKey, String> = emptyMap(),
    val showMusic: Boolean = true,
    val firstRunDone: Boolean = false,
)
