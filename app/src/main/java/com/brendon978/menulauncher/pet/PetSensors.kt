package com.brendon978.menulauncher.pet

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

private val HEADSET_TYPES = setOf(
    AudioDeviceInfo.TYPE_WIRED_HEADPHONES, AudioDeviceInfo.TYPE_WIRED_HEADSET,
    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_BLE_HEADSET, AudioDeviceInfo.TYPE_USB_HEADSET,
)

/** Tem fone conectado (com fio, USB ou bluetooth)? */
@Composable
fun rememberHeadset(): Boolean {
    val context = LocalContext.current
    val am = remember { context.getSystemService(AudioManager::class.java) }
    fun check() = am.getDevices(AudioManager.GET_DEVICES_OUTPUTS).any { it.type in HEADSET_TYPES }
    var on by remember { mutableStateOf(check()) }
    DisposableEffect(am) {
        val cb = object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(added: Array<out AudioDeviceInfo>?) { on = check() }
            override fun onAudioDevicesRemoved(removed: Array<out AudioDeviceInfo>?) { on = check() }
        }
        am.registerAudioDeviceCallback(cb, Handler(Looper.getMainLooper()))
        onDispose { am.unregisterAudioDeviceCallback(cb) }
    }
    return on
}

/** Tem internet? (rede padrão com acesso validado) */
@Composable
fun rememberOnline(): Boolean {
    val context = LocalContext.current
    val cm = remember { context.getSystemService(ConnectivityManager::class.java) }
    fun check(): Boolean = cm.getNetworkCapabilities(cm.activeNetwork)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
    var on by remember { mutableStateOf(check()) }
    DisposableEffect(cm) {
        val cb = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) { on = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) }
            override fun onLost(network: Network) { on = check() }
            override fun onAvailable(network: Network) { on = check() }
        }
        cm.registerDefaultNetworkCallback(cb, Handler(Looper.getMainLooper()))
        onDispose { cm.unregisterNetworkCallback(cb) }
    }
    return on
}

fun todayInt(now: Long = System.currentTimeMillis()): Int {
    val c = java.util.Calendar.getInstance().apply { timeInMillis = now }
    return c.get(java.util.Calendar.YEAR) * 10_000 + (c.get(java.util.Calendar.MONTH) + 1) * 100 + c.get(java.util.Calendar.DAY_OF_MONTH)
}
