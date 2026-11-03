package com.brendon978.menulauncher.music

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Libera a leitura das sessões de mídia e conta as notificações "de verdade" para o robozinho
 * (sem as persistentes, como música e downloads, e sem as do próprio launcher).
 */
class MediaListener : NotificationListenerService() {
    override fun onListenerConnected() = recount()
    override fun onListenerDisconnected() { _count.value = null }
    override fun onNotificationPosted(sbn: StatusBarNotification?) = recount()
    override fun onNotificationRemoved(sbn: StatusBarNotification?) = recount()

    private fun recount() {
        val active = runCatching { activeNotifications }.getOrNull() ?: return
        _count.value = active.count { it.isClearable && !it.isOngoing && it.packageName != packageName }
    }

    companion object {
        private val _count = MutableStateFlow<Int?>(null)
        /** null = sem acesso às notificações (ou ainda não conectou). */
        val count: StateFlow<Int?> = _count.asStateFlow()
    }
}
