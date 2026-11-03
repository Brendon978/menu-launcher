package com.brendon978.menulauncher.settings

import com.brendon978.menulauncher.apps.AppKey

object FavoritesOps {
    fun toggle(favs: List<AppKey>, key: AppKey): List<AppKey> =
        if (key in favs) favs - key else favs + key

    fun move(favs: List<AppKey>, from: Int, to: Int): List<AppKey> {
        if (from !in favs.indices || to !in favs.indices) return favs
        return favs.toMutableList().apply { add(to, removeAt(from)) }
    }

    fun prune(s: LauncherSettings, installed: Set<AppKey>): LauncherSettings {
        val favs = s.favorites.filter { it in installed }
        val overrides = s.iconOverrides.filterKeys { it in installed }
        return if (favs == s.favorites && overrides == s.iconOverrides) s
        else s.copy(favorites = favs, iconOverrides = overrides)
    }
}
