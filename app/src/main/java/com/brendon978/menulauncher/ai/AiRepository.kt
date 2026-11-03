package com.brendon978.menulauncher.ai

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PorterDuff
import androidx.core.graphics.createBitmap

/** Uma IA que pode ser conectada: o app e a Activity que abre no toque. */
data class AiCandidate(val packageName: String, val activityName: String, val label: String)

/** Fala com as IAs pelo contrato de AiContract (ver docs/plugue-ia.md). */
class AiRepository(private val context: Context) {
    private val pm = context.packageManager

    /** IAs que declaram a Activity ASSISTANT do contrato. */
    fun discover(): List<AiCandidate> =
        pm.queryIntentActivities(
            Intent(AiContract.ACTION_ASSISTANT).addCategory(Intent.CATEGORY_DEFAULT),
            PackageManager.MATCH_DEFAULT_ONLY,
        ).map {
            AiCandidate(it.activityInfo.packageName, it.activityInfo.name, it.loadLabel(pm).toString())
        }.filter { it.packageName != context.packageName }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }

    fun label(pkg: String): String =
        runCatching { pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString() }.getOrDefault(pkg)

    fun isInstalled(pkg: String): Boolean = runCatching { pm.getPackageInfo(pkg, 0) }.isSuccess

    /** A IA tem o receiver de comando? Sem ele, segurar só abre a IA. */
    fun acceptsCommands(pkg: String): Boolean =
        pm.queryBroadcastReceivers(Intent(AiContract.ACTION_COMMAND).setPackage(pkg), 0).isNotEmpty()

    /** Símbolo declarado no meta-data, pintado numa cor só; null se a IA não declarou. */
    fun symbol(pkg: String, activity: String, sizePx: Int, color: Int): Bitmap? = runCatching {
        val info = pm.getActivityInfo(ComponentName(pkg, activity), PackageManager.GET_META_DATA)
        val res = info.metaData?.getInt(AiContract.META_SYMBOL, 0)?.takeIf { it != 0 } ?: return null
        val drawable = pm.getResourcesForApplication(pkg).getDrawable(res, null)
        createBitmap(sizePx, sizePx).also { bmp ->
            val canvas = Canvas(bmp)
            drawable.setBounds(0, 0, sizePx, sizePx)
            drawable.draw(canvas)
            canvas.drawColor(color, PorterDuff.Mode.SRC_IN)
        }
    }.getOrNull()

    fun open(pkg: String, activity: String) {
        runCatching {
            context.startActivity(
                Intent().setComponent(ComponentName(pkg, activity))
                    .setAction(AiContract.ACTION_ASSISTANT)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }.onFailure {
            // "outro app" (sem a Activity do contrato) ou Activity renomeada: abre o app pela entrada normal.
            pm.getLaunchIntentForPackage(pkg)?.let { context.startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        }
    }

    /** Entrega o comando por broadcast explícito; a IA responde do jeito dela. */
    fun sendCommand(pkg: String, text: String): Boolean {
        val receiver = pm.queryBroadcastReceivers(Intent(AiContract.ACTION_COMMAND).setPackage(pkg), 0)
            .firstOrNull()?.activityInfo ?: return false
        context.sendBroadcast(
            Intent(AiContract.ACTION_COMMAND)
                .setComponent(ComponentName(receiver.packageName, receiver.name))
                .putExtra(Intent.EXTRA_TEXT, text)
                .addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES),
        )
        return true
    }
}
