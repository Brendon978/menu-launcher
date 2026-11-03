package com.brendon978.menulauncher.wallpaper

sealed interface WallpaperKind {
    data object Static : WallpaperKind
    /** [ext] é "mp4" (qualquer vídeo que o MediaPlayer toca) ou "gif". */
    data class Animated(val ext: String) : WallpaperKind
    data object Unsupported : WallpaperKind
}

private val STATIC_MIME = setOf("image/jpeg", "image/png", "image/webp")
private val STATIC_EXT = setOf("jpg", "jpeg", "png", "webp")
private val VIDEO_EXT = setOf("mp4", "webm", "mkv")

/** O MIME vence; sem MIME, decide pela extensão do nome. */
fun classify(mime: String?, fileName: String?): WallpaperKind {
    val m = mime?.lowercase()
    if (m != null) return when {
        m == "image/gif" -> WallpaperKind.Animated("gif")
        m.startsWith("video/") -> WallpaperKind.Animated("mp4")
        m in STATIC_MIME -> WallpaperKind.Static
        else -> WallpaperKind.Unsupported
    }
    val ext = fileName?.substringAfterLast('.', "")?.lowercase().orEmpty()
    return when (ext) {
        "gif" -> WallpaperKind.Animated("gif")
        in VIDEO_EXT -> WallpaperKind.Animated("mp4")
        in STATIC_EXT -> WallpaperKind.Static
        else -> WallpaperKind.Unsupported
    }
}

enum class StaticTarget { HOME, LOCK, BOTH }

/** Flags do WallpaperManager: FLAG_SYSTEM = 1, FLAG_LOCK = 2. */
fun StaticTarget.flags(): Int = when (this) {
    StaticTarget.HOME -> 1
    StaticTarget.LOCK -> 2
    StaticTarget.BOTH -> 3
}
