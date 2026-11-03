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
import androidx.compose.animation.core.VectorConverter
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.layout.layout
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
 * O robozinho andando pela tela inicial inteira, sem pisar no que é tocável ([obstacles]) e dentro de [area]
 * (coordenadas do próprio container, em px). Humor pelo celular e reação ao toque, tudo só visual.
 * Muito bravo, ele para e aponta a AK de brinquedo para você; guarda quando se acalma. Nunca atira.
 * As animações vivem do relógio de quadros do Compose, que para quando a tela inicial não está visível.
 */
@Composable
fun RobotPet(
    color: Color,
    musicPlaying: Boolean,
    area: Area,
    obstacles: List<Area>,
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
    val reacting = reaction != null && now < reactionUntil
    val calming = annoyance.levelReaction(now)
    val menacing = (reacting && reaction == Reaction.MENACE) || calming == Reaction.MENACE

    // depois de guardar a mira, sai marchando com a arma por um tempo
    var marchUntil by remember { mutableLongStateOf(0L) }
    var wasMenacing by remember { mutableStateOf(false) }
    LaunchedEffect(menacing) {
        marchUntil = marchUntil(wasMenacing, menacing, System.currentTimeMillis(), marchUntil)
        wasMenacing = menacing
    }
    val marching = !menacing && now < marchUntil

    // balão de fala (só raiva e marcha)
    var speech by remember { mutableStateOf<String?>(null) }
    var speechUntil by remember { mutableLongStateOf(0L) }
    val speechRandom = remember { Random(System.nanoTime() + 1) }
    LaunchedEffect(marching) {
        if (!marching) return@LaunchedEffect
        while (true) {
            delay(speechRandom.nextLong(2_500, 5_000))
            speech = MARCH_LINES.random(speechRandom)
            speechUntil = System.currentTimeMillis() + 2_000
        }
    }

    // ---- dança: troca de passo a cada DANCE_MOVE_MS ----
    var danceMove by remember { mutableStateOf(DanceMove.SWAY) }
    val danceRandom = remember { Random(System.nanoTime() + 2) }
    LaunchedEffect(mood == Mood.DANCING) {
        if (mood != Mood.DANCING) return@LaunchedEffect
        while (true) {
            delay(DANCE_MOVE_MS)
            danceMove = nextDanceMove(danceRandom, danceMove)
        }
    }

    // ---- andar ----
    val pos = remember { Animatable(Offset.Unspecified, Offset.VectorConverter) }
    var placed by remember { mutableStateOf(false) }
    var walking by remember { mutableStateOf(false) }
    var facingLeft by remember { mutableStateOf(false) }
    var scanning by remember { mutableStateOf(false) }
    var visible by remember { mutableFloatStateOf(1f) }
    val currentArea by rememberUpdatedState(area)
    val currentObstacles by rememberUpdatedState(obstacles)
    // marchando, ele anda mesmo que estivesse dormindo, cansado ou dançando
    val stillMood = menacing || (!marching && (mood == Mood.SLEEPING || mood == Mood.TIRED || mood == Mood.DANCING))
    val random = remember { Random(System.nanoTime()) }

    fun spot(from: Offset?, minDistance: Float): Offset? =
        freeSpot(random, currentArea, widthPx, heightPx, currentObstacles, from?.let { it.x to it.y }, minDistance)
            ?.let { Offset(it.first, it.second) }

    suspend fun glitchTo(target: Offset) {
        // pisca, some, reaparece no outro lugar. Se for cancelado no meio (a tela mudou), ele não pode ficar invisível.
        try {
            repeat(3) { visible = .3f; delay(50); visible = 1f; delay(60) }
            visible = 0f
            delay(220)
            pos.snapTo(target)
            visible = .5f; delay(60)
        } finally {
            visible = 1f
        }
    }

    // moonwalk: atravessa a tela de verdade, deslizando para trás (olha para um lado e vai para o outro)
    var moonFacingLeft by remember { mutableStateOf(false) }
    val moonwalking = mood == Mood.DANCING && danceMove == DanceMove.MOONWALK && !menacing
    LaunchedEffect(moonwalking) {
        if (!moonwalking || !pos.value.isSpecified) return@LaunchedEffect
        val here = pos.value
        val a = currentArea
        val maxX = a.r - widthPx
        val target = (0 until 15).map { a.l + random.nextFloat() * (maxX - a.l) }
            .filter { abs(it - here.x) > widthPx * 1.5f && pathClear(here.x to here.y, it to here.y, widthPx, heightPx, currentObstacles) }
            .maxByOrNull { abs(it - here.x) }
            ?: return@LaunchedEffect
        moonFacingLeft = target > here.x // olha para o lado contrário de onde vai
        pos.animateTo(Offset(target, here.y), tween((DANCE_MOVE_MS - 300).toInt(), easing = LinearEasing))
    }

    // primeira posição, e fugir de onde a tela mudou (lista cresceu, IA veio para cá)
    LaunchedEffect(area, obstacles) {
        val p = pos.value
        val me = if (p.isSpecified) Area(p.x, p.y, p.x + widthPx, p.y + heightPx) else null
        val inside = me != null && me.l >= area.l && me.t >= area.t && me.r <= area.r && me.b <= area.b
        if (me == null || !inside || obstacles.any { it.intersects(me) }) {
            val target = spot(null, 0f) ?: return@LaunchedEffect
            if (!placed) { pos.snapTo(target); placed = true } else glitchTo(target)
        }
    }

    LaunchedEffect(stillMood, placed, marching) {
        if (stillMood || !placed) {
            walking = false
            return@LaunchedEffect
        }
        while (true) {
            val target = spot(pos.value, minDistance = widthPx * 1.5f)
            if (target == null) { delay(1_500); continue }
            facingLeft = target.x < pos.value.x
            walking = true
            val speed = with(density) { (if (marching) 48.dp else 38.dp).toPx() } // px por segundo
            val ms = ((target - pos.value).getDistance() / speed * 1000).roundToInt().coerceAtLeast(1)
            pos.animateTo(target, tween(ms, easing = LinearEasing))
            walking = false
            delay(if (marching) random.nextLong(300, 700) else random.nextLong(1_500, 4_000))
            when (random.nextInt(100)) {
                in 0..11 -> { scanning = true; delay(2_500); scanning = false }
                in 12..15 -> spot(null, widthPx * 3)?.let { glitchTo(it) }
            }
        }
    }

    // fugir de quem fica importunando: o lugar livre mais longe que achar
    LaunchedEffect(fleeSignal) {
        if (fleeSignal == 0) return@LaunchedEffect
        val here = pos.value
        val far = (0 until 12).mapNotNull { spot(null, 0f) }.maxByOrNull { (it - here).getDistance() }
        if (far != null) glitchTo(far)
    }

    if (!placed) return

    // ---- rosto e pose ----
    val face = when {
        menacing -> Face.ANGRY
        marching -> Face.SERIOUS
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
        // de frente, braços fechados no meio segurando a arma apontada para você
        menacing -> RobotPose(face, false, armL = -47f, armR = 47f, tilt = 1.5f * sin(t * 3f), antennaOn = (t * 6).toInt() % 2 == 0, phase = t, gun = GunHold.AIM)
        // marcha: joelho alto, passo duro, arma atravessada no peito segura pelas duas mãos
        marching -> {
            val s = sin(t * 9f)
            RobotPose(face, facingLeft, legL = 24f * s, legR = -24f * s, armL = -38f, armR = 22f, lift = 3.5f * abs(s), phase = t, gun = GunHold.MARCH)
        }
        reacting && reaction == Reaction.GIGGLE -> {
            val k = ((reactionUntil - now) / 900f).coerceIn(0f, 1f)
            RobotPose(face, facingLeft, armL = -30f, armR = 30f, lift = 10f * sin(k * Math.PI.toFloat()), blink = blink, phase = t)
        }
        face == Face.ANGRY -> {
            val stomp = sin(t * 18f)
            RobotPose(face, facingLeft, legL = if (stomp > 0) -14f else 0f, armL = 25f, armR = -25f, antennaOn = (t * 6).toInt() % 2 == 0, phase = t)
        }
        mood == Mood.DANCING && !reacting -> dancePose(danceMove, face, if (danceMove == DanceMove.MOONWALK) moonFacingLeft else facingLeft, t)
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
            .offset { IntOffset((pos.value.x - pad).roundToInt(), (pos.value.y - pad).roundToInt()) }
            .size(with(density) { widthPx.toDp() } + TOUCH_PAD * 2, ROBOT_HEIGHT + TOUCH_PAD * 2)
            .alpha(visible)
            .pointerInput(Unit) {
                detectTapGestures {
                    val tapNow = System.currentTimeMillis()
                    val (next, r) = annoyance.tap(tapNow)
                    annoyance = next
                    reaction = r
                    reactionUntil = tapNow + if (r == Reaction.GIGGLE) 900 else 1_200
                    angerLine(r, speechRandom)?.let { speech = it; speechUntil = tapNow + 1_800 }
                    if (r == Reaction.FLEE) scope.launch { fleeSignal++ }
                }
            },
    ) {
        Canvas(Modifier.size(with(density) { widthPx.toDp() }, ROBOT_HEIGHT)) { drawRobot(color, pose) }
    }

    val line = speech
    if (line != null && now < speechUntil && visible > 0f) {
        val cx = pos.value.x + widthPx / 2
        val top = pos.value.y - with(density) { 2.dp.toPx() }
        SpeechBubble(
            text = line,
            color = color,
            modifier = Modifier.layout { m, c ->
                val p = m.measure(c.copy(minWidth = 0, minHeight = 0))
                layout(p.width, p.height) {
                    // centralizado em cima da cabeça, sem sair da tela
                    val x = (cx - p.width / 2).coerceIn(area.l, (area.r - p.width).coerceAtLeast(area.l))
                    val y = (top - p.height).coerceAtLeast(0f)
                    p.place(x.roundToInt(), y.roundToInt())
                }
            },
        )
    }
}

/** Balãozinho com rabinho apontando para a cabeça dele; na cor do traço, fundo escuro para dar leitura. */
@Composable
private fun SpeechBubble(text: String, color: Color, modifier: Modifier = Modifier) {
    androidx.compose.foundation.layout.Column(modifier, horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
        androidx.compose.material3.Text(
            text,
            color = color,
            fontSize = 13.sp,
            maxLines = 1,
            modifier = Modifier
                .background(Color(0xE60C0C0E), RoundedCornerShape(12.dp))
                .border(1.dp, color.copy(alpha = .6f), RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 5.dp),
        )
        Canvas(Modifier.size(12.dp, 6.dp)) {
            val tail = androidx.compose.ui.graphics.Path().apply {
                moveTo(0f, 0f); lineTo(size.width, 0f); lineTo(size.width / 2, size.height); close()
            }
            drawPath(tail, Color(0xE60C0C0E))
            drawLine(color.copy(alpha = .6f), Offset(0f, 0f), Offset(size.width / 2, size.height), 1.dp.toPx())
            drawLine(color.copy(alpha = .6f), Offset(size.width, 0f), Offset(size.width / 2, size.height), 1.dp.toPx())
        }
    }
}

/** Uma pose por passo de dança, no tempo [t] (segundos). */
private fun dancePose(move: DanceMove, face: Face, facingLeft: Boolean, t: Float): RobotPose {
    val beat = sin(t * 7f)
    return when (move) {
        // o de sempre: balança de um lado pro outro
        DanceMove.SWAY -> RobotPose(face, facingLeft, legL = 8f * beat, legR = -8f * beat, armL = -35f + 30f * beat, armR = 35f + 30f * beat, tilt = 6f * beat, lift = 3f * abs(beat), phase = t)
        // robô clássico: poses travadas que trocam em "tique"
        DanceMove.ROBOT -> {
            val k = (t * 3f).toInt() % 4
            val (aL, aR, head) = when (k) { 0 -> Triple(-90f, 0f, -12f); 1 -> Triple(0f, 90f, 12f); 2 -> Triple(-90f, 90f, 0f); else -> Triple(0f, 0f, -12f) }
            RobotPose(face, facingLeft, armL = aL, armR = aR, headTilt = head, phase = t)
        }
        // pulinho: um tempinho no chão, sobe abrindo os braços em V (para fora, sem passar na cabeça)
        DanceMove.HOP -> {
            val p = (t * 1.6f) % 1f
            val air = if (p < .55f) sin(p / .55f * Math.PI.toFloat()) else 0f
            RobotPose(face, facingLeft, legL = -8f * air, legR = 8f * air, armL = -25f - 105f * air, armR = 25f + 105f * air, lift = 16f * air, phase = t)
        }
        // dois passos pra um lado e volta, estalando as garrinhas
        DanceMove.SIDESTEP -> {
            val side = sin(t * 2.5f)
            val snap = if ((t * 4).toInt() % 2 == 0) 20f else -5f
            RobotPose(face, side < 0, legL = 14f * beat, legR = -14f * beat, armL = -20f - snap, armR = 20f + snap, offsetX = 26f * side, lift = 2f * abs(beat), phase = t)
        }
        // moonwalk: o deslize é a posição de verdade (RobotPet); aqui só as pernas "andando" para a frente
        // e os braços soltos, como se fosse para o lado que ele olha
        DanceMove.MOONWALK -> {
            val step = sin(t * 8f)
            RobotPose(face, facingLeft, legL = 20f * step, legR = -20f * step, armL = -14f * step, armR = 14f * step, lift = 1.5f * abs(step), phase = t)
        }
        // giro: vira, fica um instante de costas e volta (frente 0.6s, vira 0.3s, costas 0.6s, vira 0.3s)
        DanceMove.SPIN -> {
            val p = (t % 1.8f) / 1.8f
            val (width, back) = when {
                p < .33f -> 1f to false
                p < .5f -> kotlin.math.cos((p - .33f) / .17f * Math.PI.toFloat()).let { kotlin.math.abs(it) to (it < 0f) }
                p < .83f -> 1f to true
                else -> kotlin.math.cos((p - .83f) / .17f * Math.PI.toFloat()).let { kotlin.math.abs(it) to (it > 0f) }
            }
            RobotPose(face, facingLeft, armL = -60f, armR = 60f, spin = width, back = back, lift = 3f * abs(beat), antennaOn = (t * 6).toInt() % 2 == 0, phase = t)
        }
        // headbang: só a cabeça vai e volta forte; equalizador a mil
        DanceMove.HEADBANG -> {
            val nod = sin(t * 13f)
            RobotPose(face, facingLeft, legL = 4f * nod, armL = -30f + 10f * nod, armR = 30f - 10f * nod, headTilt = 18f * nod, lift = 2f * abs(nod), phase = t * 2f)
        }
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
