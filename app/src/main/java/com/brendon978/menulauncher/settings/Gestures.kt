package com.brendon978.menulauncher.settings

import com.brendon978.menulauncher.apps.AppKey

/** O que um gesto no fundo da tela inicial pode fazer. */
enum class GestureAction(val label: String) {
    NOTIFICATIONS("Cortina"),
    SEARCH("Busca"),
    CAMERA("Câmera"),
    TORCH("Lanterna"),
    APP("Abrir um app"),
    NONE("Nada"),
}

/** A ação de um gesto; [app] só vale para [GestureAction.APP]. */
data class GestureChoice(val action: GestureAction, val app: AppKey? = null) {
    fun encode(): String = if (action == GestureAction.APP && app != null) "${action.name}|${app.encode()}" else action.name

    companion object {
        /** null para texto desconhecido ou "abrir um app" sem o app: quem lê cai no padrão. */
        fun decode(text: String?): GestureChoice? {
            if (text == null) return null
            val action = runCatching { GestureAction.valueOf(text.substringBefore('|')) }.getOrNull() ?: return null
            if (action != GestureAction.APP) return GestureChoice(action)
            return AppKey.decode(text.substringAfter('|', ""))?.let { GestureChoice(action, it) }
        }
    }
}

/** Dois toques: enquanto ninguém escolheu nos Gestos, não fazem nada. */
fun LauncherSettings.doubleTapChoice(): GestureChoice = gestureDoubleTap ?: GestureChoice(GestureAction.NONE)

fun LauncherSettings.withDoubleTap(choice: GestureChoice): LauncherSettings = copy(gestureDoubleTap = choice)

/** Com "Nada" o toque simples nem espera pelo segundo toque. */
fun LauncherSettings.listensDoubleTap(): Boolean = doubleTapChoice().action != GestureAction.NONE

/** A lanterna só é vigiada quando algum gesto usa. */
fun LauncherSettings.usesTorchGesture(): Boolean =
    listOf(gestureDown, gestureUp, doubleTapChoice()).any { it.action == GestureAction.TORCH }
