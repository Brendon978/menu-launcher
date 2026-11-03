package com.brendon978.menulauncher.pet

import kotlin.math.abs
import kotlin.math.max
import kotlin.random.Random

/** Humor de fundo, pelo estado do celular. A reação ao toque aparece por cima por um instante. */
enum class Mood { SLEEPING, TIRED, DANCING, HAPPY, NORMAL }

enum class Reaction { GIGGLE, SERIOUS, ANGRY, FLEE }

const val SLEEP_FROM_HOUR = 23
const val SLEEP_UNTIL_HOUR = 7
const val TIRED_BATTERY = 15
const val HAPPY_MS = 60_000L

/** Prioridade da spec: dormindo > cansado > música > feliz > normal (o "bravo" vem do toque, por cima). */
fun baseMood(hour: Int, battery: Int?, charging: Boolean, musicPlaying: Boolean, happyUntil: Long, now: Long): Mood = when {
    hour >= SLEEP_FROM_HOUR || hour < SLEEP_UNTIL_HOUR -> Mood.SLEEPING
    battery != null && battery <= TIRED_BATTERY && !charging -> Mood.TIRED
    musicPlaying -> Mood.DANCING
    now < happyUntil -> Mood.HAPPY
    else -> Mood.NORMAL
}

/** Fica feliz só quando as notificações vão de "alguma" para zero. [prev] null = ainda não sabia quantas eram. */
fun happyUntil(prev: Int?, new: Int, now: Long, current: Long): Long =
    if (prev != null && prev > 0 && new == 0) now + HAPPY_MS else current

/**
 * Irritação: cada toque soma 1, e ela cai [DECAY_PER_S] por segundo.
 * Faixas: < 2 risadinha, < 3.5 sério, < 5 bravo, a partir de 5 foge (e volta para "sério").
 */
data class Annoyance(val level: Float = 0f, val at: Long = 0L) {
    fun levelAt(now: Long): Float = max(0f, level - (now - at).coerceAtLeast(0) / 1000f * DECAY_PER_S)

    fun tap(now: Long): Pair<Annoyance, Reaction> {
        val l = levelAt(now) + 1f
        val r = reactionFor(l)
        val next = if (r == Reaction.FLEE) Annoyance(AFTER_FLEE, now) else Annoyance(l, now)
        return next to r
    }

    /** Reação que corresponde ao nível atual, sem novo toque (para o rosto enquanto ele se acalma). */
    fun levelReaction(now: Long): Reaction = reactionFor(levelAt(now))

    private fun reactionFor(l: Float) = when {
        l < 2f -> Reaction.GIGGLE
        l < 3.5f -> Reaction.SERIOUS
        l < 5f -> Reaction.ANGRY
        else -> Reaction.FLEE
    }

    companion object {
        const val DECAY_PER_S = 0.5f
        const val AFTER_FLEE = 2.5f
    }
}

/** Faixa de x (canto esquerdo do robô) no chão: da margem até antes da barra de letras e do símbolo da IA. */
fun floorRange(width: Float, robotWidth: Float, margin: Float, barLeft: Float, aiLeft: Float?): ClosedFloatingPointRange<Float> {
    val rightLimit = minOf(barLeft, aiLeft ?: Float.MAX_VALUE, width)
    val maxX = rightLimit - robotWidth
    return if (maxX <= margin) margin..margin else margin..maxX
}

/** Próximo ponto do chão, a pelo menos [minDistance] do atual quando couber. */
fun nextTarget(random: Random, range: ClosedFloatingPointRange<Float>, current: Float, minDistance: Float): Float {
    val span = range.endInclusive - range.start
    if (span <= 0f) return range.start
    repeat(20) {
        val t = range.start + random.nextFloat() * span
        if (abs(t - current) >= minDistance) return t
    }
    // faixa estreita: vai para a ponta mais longe
    return if (current - range.start > range.endInclusive - current) range.start else range.endInclusive
}
