package com.brendon978.menulauncher.system

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

/**
 * Serviço de acessibilidade só para apagar a tela (bloquear) com dois toques no fundo da tela inicial.
 * Não lê nada da tela: não pede conteúdo de janelas nem recebe eventos úteis.
 * Fica desligado até você ligar em Configurações → Acessibilidade → menu-launcher.
 */
class LockService : AccessibilityService() {
    override fun onServiceConnected() { instance = this }
    override fun onUnbind(intent: android.content.Intent?): Boolean { instance = null; return super.onUnbind(intent) }
    override fun onDestroy() { instance = null; super.onDestroy() }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() = Unit

    companion object {
        @Volatile private var instance: LockService? = null
        val enabled: Boolean get() = instance != null
        /** Apaga a tela; false se o serviço não estiver ligado. */
        fun lock(): Boolean = instance?.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN) ?: false
    }
}
