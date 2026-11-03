package com.brendon978.menulauncher.pet

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import kotlin.math.sin

/** Rosto na telinha quadrada. */
enum class Face { NORMAL, SLEEP, MUSIC, HEART, TIRED, SCAN, GIGGLE, SERIOUS, ANGRY }

/**
 * Pose de um quadro. Ângulos em graus; [phase] é um relógio em segundos para as animações do rosto.
 * [blink] 1 = olho aberto, ~0 = fechado.
 */
data class RobotPose(
    val face: Face = Face.NORMAL,
    val facingLeft: Boolean = false,
    val legL: Float = 0f,
    val legR: Float = 0f,
    val armL: Float = 0f,
    val armR: Float = 0f,
    val tilt: Float = 0f,
    val lift: Float = 0f,
    val blink: Float = 1f,
    val antennaOn: Boolean = true,
    val phase: Float = 0f,
)

/** Proporção do desenho (viewBox do esboço: 120 × 170). */
const val ROBOT_ASPECT = 120f / 170f

/** Desenha o robozinho do esboço (variante "corpo") em contorno, numa cor só, ocupando o DrawScope inteiro. */
fun DrawScope.drawRobot(color: Color, pose: RobotPose) {
    val u = size.height / 170f
    val line = Stroke(width = 2.2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    scale(u, u, pivot = Offset.Zero) {
        scale(if (pose.facingLeft) -1f else 1f, 1f, pivot = Offset(60f, 0f)) {
            translate(top = -pose.lift) {
                rotate(pose.tilt, pivot = Offset(60f, 153f)) {
                    // pernas sanfonadas com pé em cúpula (giram no quadril)
                    leg(52f, pose.legL, color, line)
                    leg(68f, pose.legR, color, line)
                    // corpo
                    drawRoundRect(color, Offset(44f, 80f), Size(32f, 44f), CornerRadius(8f), style = line)
                    drawCircle(color, 1.6f, Offset(53f, 94f))
                    drawCircle(color.copy(alpha = color.alpha * .5f), 1.6f, Offset(59f, 94f))
                    drawLine(color.copy(alpha = color.alpha * .5f), Offset(52f, 106f), Offset(68f, 106f), 2.2f, StrokeCap.Round)
                    // braços sanfonados (iguais às pernas) com mão de 3 garrinhas; abertos 10° para fora
                    arm(40f, 10f + pose.armL, color, line)
                    arm(80f, -10f + pose.armR, color, line)
                    // cabeça (no esboço: translate(9 2) scale(.85) sobre o desenho da cabeça)
                    translate(9f, 2f) {
                        scale(.85f, .85f, pivot = Offset.Zero) { head(color, pose, line) }
                    }
                }
            }
        }
    }
}

private fun DrawScope.leg(x: Float, angle: Float, color: Color, line: Stroke) {
    rotate(angle, pivot = Offset(x, 124f)) {
        drawLine(color, Offset(x - 4, 124f), Offset(x - 4, 146f), 2.2f, StrokeCap.Round)
        drawLine(color, Offset(x + 4, 124f), Offset(x + 4, 146f), 2.2f, StrokeCap.Round)
        val ring = color.copy(alpha = color.alpha * .8f)
        var y = 129f
        while (y <= 145f) {
            drawLine(ring, Offset(x - 4, y), Offset(x + 4, y), 1.2f, StrokeCap.Round)
            y += 4f
        }
        val foot = Path().apply {
            moveTo(x - 8, 153f)
            quadraticTo(x - 8, 145f, x, 145f)
            quadraticTo(x + 8, 145f, x + 8, 153f)
            close()
        }
        drawPath(foot, color, style = line)
    }
}

/** Braço de tubo com anéis, girando no ombro (topo), e mão de três garrinhas. */
private fun DrawScope.arm(x: Float, angle: Float, color: Color, line: Stroke) {
    rotate(angle, pivot = Offset(x, 90f)) {
        drawLine(color, Offset(x - 3, 90f), Offset(x - 3, 112f), 2f, StrokeCap.Round)
        drawLine(color, Offset(x + 3, 90f), Offset(x + 3, 112f), 2f, StrokeCap.Round)
        val ring = color.copy(alpha = color.alpha * .8f)
        var y = 94f
        while (y <= 110f) {
            drawLine(ring, Offset(x - 3, y), Offset(x + 3, y), 1.1f, StrokeCap.Round)
            y += 4f
        }
        // palma + 3 garrinhas abertas
        drawRoundRect(color, Offset(x - 4.5f, 112f), Size(9f, 5f), CornerRadius(2f), style = Stroke(1.8f))
        val claw = Stroke(1.8f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawPath(Path().apply { moveTo(x - 3.5f, 117f); lineTo(x - 5.5f, 121.5f) }, color, style = claw)
        drawPath(Path().apply { moveTo(x, 117f); lineTo(x, 122.5f) }, color, style = claw)
        drawPath(Path().apply { moveTo(x + 3.5f, 117f); lineTo(x + 5.5f, 121.5f) }, color, style = claw)
    }
}

private fun DrawScope.head(color: Color, pose: RobotPose, line: Stroke) {
    drawLine(color, Offset(60f, 26f), Offset(60f, 16f), 2.2f, StrokeCap.Round)
    drawCircle(if (pose.antennaOn) color else color.copy(alpha = color.alpha * .2f), 3f, Offset(60f, 13f), style = line)
    drawRoundRect(color, Offset(30f, 26f), Size(60f, 62f), CornerRadius(14f), style = line)
    drawRoundRect(color, Offset(38f, 36f), Size(44f, 44f), CornerRadius(4f), style = line)
    drawLine(color, Offset(30f, 52f), Offset(25f, 52f), 2.2f, StrokeCap.Round)
    drawLine(color, Offset(90f, 52f), Offset(95f, 52f), 2.2f, StrokeCap.Round)
    // o rosto não sai da telinha
    clipRect(39f, 37f, 81f, 79f) { face(color, pose) }
    if (pose.face == Face.SLEEP) zzz(color, pose.phase)
}

private fun DrawScope.face(c: Color, pose: RobotPose) {
    val t = pose.phase
    fun px(x: Float, y: Float, w: Float, h: Float, a: Float = 1f) =
        drawRoundRect(c.copy(alpha = c.alpha * a), Offset(x, y), Size(w, h), CornerRadius(1.2f))
    fun ln(x1: Float, y1: Float, x2: Float, y2: Float, w: Float = 2f) =
        drawLine(c, Offset(x1, y1), Offset(x2, y2), w, StrokeCap.Round)

    when (pose.face) {
        Face.NORMAL -> {
            val h = 9f * pose.blink.coerceIn(.1f, 1f)
            px(47f, 56.5f - h / 2, 8f, h); px(65f, 56.5f - h / 2, 8f, h)
            px(56f, 69f, 8f, 2f, .7f)
        }
        Face.SLEEP -> { px(45f, 57f, 10f, 2.6f); px(65f, 57f, 10f, 2.6f) }
        Face.MUSIC -> listOf(44f, 51f, 58f, 65f, 72f).forEachIndexed { i, x ->
            val k = .25f + .75f * ((sin(t * 9f + i * 1.7f) + 1f) / 2f)
            val h = 26f * k
            px(x, 74f - h, 4.5f, h)
        }
        Face.HEART -> {
            drawCircle(c, 5.6f, Offset(54.5f, 56.5f)); drawCircle(c, 5.6f, Offset(65.5f, 56.5f))
            drawPath(Path().apply { moveTo(49.1f, 58f); lineTo(70.9f, 58f); lineTo(60f, 70f); close() }, c)
        }
        Face.TIRED -> {
            px(46f, 58f, 8f, 5f); px(66f, 58f, 8f, 5f)
            ln(44f, 54f, 56f, 55.5f, 1.6f); ln(76f, 54f, 64f, 55.5f, 1.6f)
            drawRect(c, Offset(55f, 70f), Size(10f, 5f), style = Stroke(1.4f))
            px(56.5f, 71.5f, 2f, 2f)
        }
        Face.SCAN -> {
            px(47f, 53f, 7f, 7f); px(66f, 53f, 7f, 7f)
            val y = 57f + 14f * sin(t * 3.5f)
            px(40f, y, 40f, 1.6f, .8f)
        }
        Face.GIGGLE -> {
            drawPath(Path().apply { moveTo(46f, 60f); lineTo(50.5f, 54f); lineTo(55f, 60f) }, c, style = Stroke(2.4f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawPath(Path().apply { moveTo(65f, 60f); lineTo(69.5f, 54f); lineTo(74f, 60f) }, c, style = Stroke(2.4f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawArc(c, 0f, 180f, false, Offset(55f, 64f), Size(10f, 8f), style = Stroke(2f, cap = StrokeCap.Round))
        }
        Face.SERIOUS -> { px(46f, 56f, 9f, 3f); px(65f, 56f, 9f, 3f); ln(55f, 70f, 65f, 70f) }
        Face.ANGRY -> {
            px(47f, 56f, 8f, 6f); px(65f, 56f, 8f, 6f)
            ln(45f, 50f, 56f, 54f, 2.4f); ln(75f, 50f, 64f, 54f, 2.4f)
            drawPath(Path().apply { moveTo(53f, 72f); lineTo(56.5f, 69f); lineTo(60f, 72f); lineTo(63.5f, 69f); lineTo(67f, 72f) }, c, style = Stroke(1.8f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

/** Três "z" subindo da cabeça (únicos traços fora da telinha). */
private fun DrawScope.zzz(c: Color, t: Float) {
    for (i in 0 until 3) {
        val p = ((t / 2.4f) + i / 3f) % 1f
        val a = when { p < .3f -> p / .3f; else -> 1f - (p - .3f) / .7f }.coerceIn(0f, 1f)
        val s = 4f + i * 1.2f
        val x = 84f + p * 16f + i * 2f
        val y = 30f - p * 24f
        drawPath(
            Path().apply { moveTo(x, y); lineTo(x + s, y); lineTo(x, y + s); lineTo(x + s, y + s) },
            c.copy(alpha = c.alpha * a * .9f),
            style = Stroke(1.6f, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}
