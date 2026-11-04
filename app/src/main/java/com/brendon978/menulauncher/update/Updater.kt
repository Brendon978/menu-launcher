package com.brendon978.menulauncher.update

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** Versão publicada em updates/latest.json do repositório. */
data class Release(val versionCode: Int, val url: String, val notes: String)

private fun field(json: String, key: String): String? =
    Regex("\"" + key + "\"\\s*:\\s*(\"((?:[^\"\\\\]|\\\\.)*)\"|-?[0-9]+)").find(json)?.let { m ->
        m.groupValues[2].ifEmpty { m.groupValues[1] }
    }

fun parseRelease(json: String): Release? {
    val code = field(json, "versionCode")?.toIntOrNull() ?: return null
    val url = field(json, "url")?.takeIf { it.startsWith("https://") } ?: return null
    return Release(code, url, field(json, "notes").orEmpty())
}

fun isNewer(release: Release, installed: Int): Boolean = release.versionCode > installed

object Updater {
    private const val LATEST = "https://raw.githubusercontent.com/Brendon978/menu-launcher/main/updates/latest.json"

    suspend fun latest(): Release? = withContext(Dispatchers.IO) {
        runCatching {
            val c = URL(LATEST).openConnection() as HttpURLConnection
            c.connectTimeout = 6_000
            c.readTimeout = 6_000
            try { parseRelease(c.inputStream.bufferedReader().use { it.readText() }) } finally { c.disconnect() }
        }.getOrNull()
    }

    /** Baixa para um arquivo temporário e renomeia: nunca instala um APK pela metade. */
    suspend fun download(context: Context, release: Release): File = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        val tmp = File(dir, "update.tmp")
        val apk = File(dir, "menu-launcher.apk")
        URL(release.url).openStream().use { input -> tmp.outputStream().use { input.copyTo(it) } }
        apk.delete()
        check(tmp.renameTo(apk)) { "não consegui gravar o APK" }
        apk
    }

    /** O Android mostra a confirmação de instalar; nada instala sem você aceitar. */
    fun install(context: Context, apk: File) {
        val uri = FileProvider.getUriForFile(context, context.packageName + ".updates", apk)
        context.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}
