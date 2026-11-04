package com.brendon978.menulauncher.home

import android.content.Intent
import android.provider.AlarmClock
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val PT_BR = Locale.forLanguageTag("pt-BR")

/** Relógio e data no topo: só para olhar (tocar no relógio abre os alarmes). */
@Composable
fun TopInfo(color: Color, modifier: Modifier = Modifier, showNextEvent: Boolean = false) {
    val context = LocalContext.current
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(60_000 - now % 60_000)
        }
    }
    val is24h = android.text.format.DateFormat.is24HourFormat(context)
    val time = remember(now, is24h) { SimpleDateFormat(if (is24h) "HH:mm" else "h:mm", PT_BR).format(Date(now)) }
    val date = remember(now) { SimpleDateFormat("EEE, d 'de' MMMM", PT_BR).format(Date(now)) }
    // travessura do Glitch: entorta o relógio até você cutucar ele
    val clockTilt by androidx.compose.animation.core.animateFloatAsState(
        if (com.brendon978.menulauncher.pet.GlitchMischief.current.value == com.brendon978.menulauncher.pet.Mischief.CLOCK) -8f else 0f,
        label = "relogio",
    )
    Column(modifier) {
        Text(
            time,
            color = color,
            fontSize = 52.sp,
            fontWeight = FontWeight.ExtraLight,
            modifier = Modifier.graphicsLayer { rotationZ = clockTilt }.clickable(remember { MutableInteractionSource() }, indication = null) {
                runCatching {
                    context.startActivity(Intent(AlarmClock.ACTION_SHOW_ALARMS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
            },
        )
        Text(date, color = color.copy(alpha = 0.7f), fontSize = 13.sp)
        if (showNextEvent) {
            // próximo compromisso, relido a cada minuto (fora da thread principal)
            val event by androidx.compose.runtime.produceState<String?>(null, now / 60_000) {
                value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { queryNextEvent(context) }
            }
            event?.let {
                Text(
                    it, color = color.copy(alpha = 0.85f), fontSize = 13.sp, maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 200.dp),
                )
            }
        }
    }
}
