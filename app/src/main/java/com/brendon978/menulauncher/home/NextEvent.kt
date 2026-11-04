package com.brendon978.menulauncher.home

import android.content.Context
import android.provider.CalendarContract
import java.util.Calendar
import java.util.TimeZone

/**
 * Texto curto do próximo compromisso: "Agora · X", "16:00 · X", "Amanhã 09:30 · X", "Hoje · X" (dia inteiro).
 * null se já acabou. Só olha hoje e amanhã.
 */
fun formatNextEvent(title: String, begin: Long, end: Long, allDay: Boolean, now: Long, tz: TimeZone = TimeZone.getDefault()): String? {
    if (end <= now) return null
    val name = title.trim().ifEmpty { "Compromisso" }
    fun dayOf(ms: Long) = Calendar.getInstance(tz).apply { timeInMillis = ms }.let { it.get(Calendar.YEAR) * 1000 + it.get(Calendar.DAY_OF_YEAR) }
    val today = dayOf(now)
    // evento de dia inteiro vem em UTC: compara a data dele em UTC
    val evDay = if (allDay) Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = begin }.let { it.get(Calendar.YEAR) * 1000 + it.get(Calendar.DAY_OF_YEAR) } else dayOf(begin)
    val dayWord = when (evDay - today) { 0 -> "Hoje"; 1 -> "Amanhã"; else -> return null }
    if (allDay) return "$dayWord · $name"
    if (begin <= now) return "Agora · $name"
    val c = Calendar.getInstance(tz).apply { timeInMillis = begin }
    val hhmm = "%02d:%02d".format(c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE))
    return if (dayWord == "Hoje") "$hhmm · $name" else "$dayWord $hhmm · $name"
}

/** Próximo compromisso da agenda (hoje ou amanhã), já formatado; null sem permissão ou sem nada. */
fun queryNextEvent(context: Context, now: Long = System.currentTimeMillis()): String? = runCatching {
    val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().also {
        android.content.ContentUris.appendId(it, now - 12 * 3_600_000L)
        android.content.ContentUris.appendId(it, now + 48 * 3_600_000L)
    }.build()
    val cols = arrayOf(CalendarContract.Instances.TITLE, CalendarContract.Instances.BEGIN, CalendarContract.Instances.END, CalendarContract.Instances.ALL_DAY, CalendarContract.Instances.STATUS)
    context.contentResolver.query(uri, cols, null, null, "${CalendarContract.Instances.BEGIN} ASC")?.use { c ->
        var best: String? = null
        while (c.moveToNext() && best == null) {
            if (c.getInt(4) == CalendarContract.Instances.STATUS_CANCELED) continue
            best = formatNextEvent(c.getString(0).orEmpty(), c.getLong(1), c.getLong(2), c.getInt(3) == 1, now)
        }
        best
    }
}.getOrNull()
