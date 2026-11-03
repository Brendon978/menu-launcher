package com.brendon978.menulauncher.wallpaper

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Aplica os papéis de parede que o usuário escolhe. Nada aqui roda na thread principal. */
class WallpaperApplier(private val context: Context) {

    fun kindOf(uri: Uri): WallpaperKind = classify(context.contentResolver.getType(uri), displayName(uri))

    suspend fun applyStatic(uri: Uri, target: StaticTarget) = withContext(Dispatchers.IO) {
        context.contentResolver.openInputStream(uri)!!.use { input ->
            WallpaperManager.getInstance(context).setStream(input, null, true, target.flags())
        }
    }

    /** Copia para a pasta do serviço animado; grava num temporário e renomeia (o serviço nunca lê pela metade). */
    suspend fun prepareAnimated(uri: Uri, ext: String): File = withContext(Dispatchers.IO) {
        val dir = AnimatedFiles.dir(context).apply { mkdirs() }
        val tmp = File(dir, "incoming.tmp")
        context.contentResolver.openInputStream(uri)!!.use { input ->
            tmp.outputStream().use { input.copyTo(it) }
        }
        AnimatedFiles.all(context).forEach { it.delete() }
        val target = File(dir, "current.$ext")
        check(tmp.renameTo(target)) { "não consegui gravar $target" }
        target
    }

    /** Prévia do sistema do papel de parede animado; o usuário confirma lá. */
    fun livePreviewIntent(): Intent =
        Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
            .putExtra(
                WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                ComponentName(context, AnimatedWallpaperService::class.java),
            )

    private fun displayName(uri: Uri): String? = runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
    }.getOrNull()
}

/** Onde mora o arquivo do papel de parede animado (o serviço lê daqui). */
object AnimatedFiles {
    fun dir(context: Context) = File(context.filesDir, "wallpaper")
    fun all(context: Context) = listOf("mp4", "gif").map { File(dir(context), "current.$it") }
    fun current(context: Context): File? = all(context).firstOrNull { it.exists() }
}
