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
    /** IA conectada (null = nenhuma). aiActivity é a que abre no toque. */
    val aiPackage: String? = null,
    val aiActivity: String? = null,
    /** Posição do símbolo da IA, em fração da tela (zona do polegar, à esquerda da barra de letras). */
    val aiX: Float = 0.70f,
    val aiY: Float = 0.55f,
    /** Robozinho na tela inicial (modo uma mão). */
    val showPet: Boolean = true,
    /** Nome do acessório do Glitch (enum Accessory). */
    val glitchAccessory: String = "NONE",
    /** Nomes que você deu aos apps ("Samsung Shop" -> "Shop"). */
    val customLabels: Map<AppKey, String> = emptyMap(),
    /** Apps escondidos da lista e da grade (a busca ainda acha). */
    val hiddenApps: Set<AppKey> = emptySet(),
)
