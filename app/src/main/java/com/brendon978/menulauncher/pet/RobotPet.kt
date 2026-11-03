package com.brendon978.menulauncher.pet

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.brendon978.menulauncher.music.MediaListener
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/** Altura do robozinho na tela. */
val ROBOT_HEIGHT = 72.dp
private val TOUCH_PAD = 14.dp

/**
 * O robozinho no chão invisível: anda por [range] (x do canto esquerdo, em px) com os pés em [floorY] (px).
 * Humor pelo celular (horário, bateria, [musicPlaying], notificações) e reação ao toque, tudo só visual.
 * As animações vivem do relógio de quadros do Compose, que para quando a tela inicial não está visível.
 */
@Composable
fun RobotPet(
    color: Color,
    musicPlaying: Boolean,
    floorY: Float,
    range: ClosedFloatingPointRange<Float>,
) {
    val density = LocalDensity.current
    val heightPx = with(density) { ROBOT_HEIGHT.toPx() }
    val widthPx = heightPx * ROBOT_ASPECT

    // ---- entradas do celular ----
    val battery = rememberBattery()
    val notifications by MediaListener.count.collectAsState()
    var prevNotif by remember { mutableStateOf<Int?>(null) }
    var happyUntil by remember { mutableLongStateOf(0L) }
    LaunchedEffect(notifications) {
        val n = notifications ?: return@LaunchedEffect
        happyUntil = happyUntil(prevNotif, n, System.currentTimeMillis(), happyUntil)
        prevNotif = n
    }

    // ---- relógio de animação (só corre com a tela inicial visível) ----
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var t by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        val start = withFrameMillis { it }
        while (true) withFrameMillis { frame ->
            t = (frame - start) / 1000f
            now = System.currentTimeMillis()
        }
    }
    val hour = remember(now / 60_000) { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val mood = baseMood(hour, battery.level, battery.charging, musicPlaying, happyUntil, now)

    // ---- toque ----
    var annoyance by remember { mutableStateOf(Annoyance()) }
    var reaction by remember { mutableStateOf<Reaction?>(null) }
    var reactionUntil by remember { mutableLongStateOf(0L) }
    var fleeSignal by remember { mutableIntStateOf(0) }

    // ---- andar ----
    val x = remember { Animatable(range.start) }
    var walking by remember { mutableStateOf(false) }
    var facingLeft by remember { mutableStateOf(false) }
    var scanning by remember { mutableStateOf(false) }
    var visible by remember { mutableFloatStateOf(1f) }
    val currentRange by rememberUpdatedState(range)
    val stillMood = mood == Mood.SLEEPING || mood == Mood.TIRED || mood == Mood.DANCING
    val random = remember { Random(System.nanoTime()) }

    suspend fun glitchTo(target: Float) {
        // pisca, some, reaparece no outro lugar
        repeat(3) { visible = .3f; delay(50); visible = 1f; delay(60) }
        visible = 0f
        delay(220)
        x.snapTo(target)
        visible = .5f; delay(60); visible = 1f
    }

    // faixa mudou (girou a tela, IA mudou de lugar): fica dentro dela
    LaunchedEffect(range) {
        if (x.value !in range) x.snapTo(x.value.coerceIn(range.start, range.endInclusive))
    }

    LaunchedEffect(stillMood) {
        if (stillMood) {
            walking = false
            return@LaunchedEffect
        }
        while (true) {
            val r = currentRange
            val target = nextTarget(random, r, x.value, minDistance = widthPx)
            facingLeft = target < x.value
            walking = true
            val speed = with(density) { 38.dp.toPx() } // px por segundo
            val ms = (abs(target - x.value) / speed * 1000).roundToInt().coerceAtLeast(1)
            x.animateTo(target, tween(ms, easing = LinearEasing))
            walking = false
            delay(random.nextLong(1_500, 4_000))
            when (random.nextInt(100)) {
                in 0..11 -> { scanning = true; delay(2_500); scanning = false }
                in 12..15 -> glitchTo(nextTarget(random, currentRange, x.value, widthPx * 2))
            }
        }
    }

    // fugir de quem fica importunando: vai para a ponta oposta
    LaunchedEffect(fleeSignal) {
        if (fleeSignal == 0) return@LaunchedEffect
        val r = currentRange
        val mid = (r.start + r.endInclusive) / 2
        glitchTo(if (x.value < mid) r.endInclusive else r.start)
    }

    // ---- rosto e pose ----
    val reacting = reaction != null && now < reactionUntil
    val calming = annoyance.levelReaction(now)
    val face = when {
        reacting && reaction == Reaction.GIGGLE -> Face.GIGGLE
        reacting && reaction == Reaction.ANGRY -> Face.ANGRY
        reacting -> Face.SERIOUS
        calming == Reaction.ANGRY || calming == Reaction.FLEE -> Face.ANGRY
        calming == Reaction.SERIOUS -> Face.SERIOUS
        mood == Mood.SLEEPING -> Face.SLEEP
        mood == Mood.TIRED -> Face.TIRED
        mood == Mood.DANCING -> Face.MUSIC
        mood == Mood.HAPPY -> Face.HEART
        scanning -> Face.SCAN
        else -> Face.NORMAL
    }
    val blinkCycle = t % 4f
    val blink = if (blinkCycle in 3.75f..3.9f) .1f else 1f
    val pose = when {
        reacting && reaction == Reaction.GIGGLE -> {
            val k = ((reactionUntil - now) / 900f).coerceIn(0f, 1f)
            RobotPose(face, facingLeft, armL = -30f, armR = 30f, lift = 10f * sin(k * Math.PI.toFloat()), blink = blink, phase = t)
        }
        face == Face.ANGRY -> {
            val stomp = sin(t * 18f)
            RobotPose(face, facingLeft, legL = if (stomp > 0) -14f else 0f, armL = 25f, armR = -25f, antennaOn = (t * 6).toInt() % 2 == 0, phase = t)
        }
        mood == Mood.DANCING && !reacting -> {
            val s = sin(t * 7f)
            RobotPose(face, facingLeft, legL = 8f * s, legR = -8f * s, armL = -35f + 30f * s, armR = 35f + 30f * s, tilt = 6f * s, lift = 3f * abs(s), phase = t)
        }
        mood == Mood.HAPPY && !walking -> {
            val hop = sin(t * 5f).coerceAtLeast(0f)
            RobotPose(face, facingLeft, armL = -20f, armR = 20f, lift = 8f * hop, blink = blink, phase = t)
        }
        walking -> {
            val s = sin(t * 12f)
            RobotPose(face, facingLeft, legL = 16f * s, legR = -16f * s, armL = -16f * s, armR = 16f * s, lift = 2.5f * abs(s), blink = blink, phase = t)
        }
        mood == Mood.TIRED -> RobotPose(face, facingLeft, armL = 8f, armR = -8f, tilt = if (facingLeft) -4f else 4f, phase = t)
        else -> RobotPose(face, facingLeft, blink = blink, antennaOn = !scanning || (t * 3).toInt() % 2 == 0, phase = t)
    }

    val scope = androidx.compose.runtime.rememberCoroutineScope()
    // área de toque maior que o desenho: ele é pequeno e está andando
    val pad = with(density) { TOUCH_PAD.toPx() }
    Box(
        contentAlignment = androidx.compose.ui.Alignment.Center,
        modifier = Modifier
            .offset { IntOffset((x.value - pad).roundToInt(), (floorY - heightPx - pad).roundToInt()) }
            .size(with(density) { widthPx.toDp() } + TOUCH_PAD * 2, ROBOT_HEIGHT + TOUCH_PAD * 2)
            .alpha(visible)
            .pointerInput(Unit) {
                detectTapGestures {
                    val tapNow = System.currentTimeMillis()
                    val (next, r) = annoyance.tap(tapNow)
                    annoyance = next
                    reaction = r
                    reactionUntil = tapNow + if (r == Reaction.GIGGLE) 900 else 1_200
                    if (r == Reaction.FLEE) scope.launch { fleeSignal++ }
                }
            },
    ) {
        Canvas(Modifier.size(with(density) { widthPx.toDp() }, ROBOT_HEIGHT)) { drawRobot(color, pose) }
    }
}

private data class BatteryState(val level: Int?, val charging: Boolean)

@Composable
private fun rememberBattery(): BatteryState {
    val context = LocalContext.current
    var state by remember { mutableStateOf(BatteryState(null, false)) }
    DisposableEffect(context) {
        fun read(i: Intent?) {
            if (i == null) return
            val level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
            val status = i.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            state = BatteryState(
                level = if (level >= 0 && scale > 0) level * 100 / scale else null,
                charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL,
            )
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, i: Intent?) = read(i)
        }
        read(context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED)))
        onDispose { context.unregisterReceiver(receiver) }
    }
    return state
}
