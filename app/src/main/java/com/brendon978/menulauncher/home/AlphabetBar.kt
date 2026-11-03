package com.brendon978.menulauncher.home

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.brendon978.menulauncher.apps.LetterIndex
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Barra ★ A–Z # na borda direita, curva para dentro no meio (como no esboço).
 * Tocar ou arrastar seleciona; letra sem apps fica apagada.
 */
@Composable
fun AlphabetBar(
    selected: Char,
    available: Set<Char>,
    color: Color,
    onSelect: (Char) -> Unit,
    modifier: Modifier = Modifier,
) {
    val letters = LetterIndex.BAR
    val haptic = LocalHapticFeedback.current
    val currentSelected = rememberUpdatedState(selected)
    val currentOnSelect = rememberUpdatedState(onSelect)

    BoxWithConstraints(modifier) {
        val itemHeight = min(15.5.dp, maxHeight / letters.size)
        val itemPx = with(LocalDensity.current) { itemHeight.toPx() }

        fun pick(y: Float) {
            val i = (y / itemPx).toInt().coerceIn(0, letters.lastIndex)
            if (letters[i] != currentSelected.value) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                currentOnSelect.value(letters[i])
            }
        }

        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .pointerInput(itemPx) { detectTapGestures { pick(it.y) } }
                .pointerInput(itemPx) {
                    detectVerticalDragGestures(onDragStart = { pick(it.y) }) { change, _ -> pick(change.position.y) }
                },
        ) {
            letters.forEachIndexed { i, letter ->
                val curve = (sin(i / (letters.size - 1).toFloat() * PI) * 22).roundToInt().dp
                val isSel = letter == selected
                val enabled = letter == LetterIndex.STAR || letter in available
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .height(itemHeight)
                        .padding(end = curve)
                        .background(if (isSel) color else Color.Transparent, RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp),
                ) {
                    Text(
                        letter.toString(),
                        fontSize = 10.sp,
                        lineHeight = 10.sp,
                        color = when {
                            isSel -> if (color.luminance() > 0.5f) Color.Black else Color.White
                            enabled -> color.copy(alpha = 0.8f)
                            else -> color.copy(alpha = 0.35f)
                        },
                    )
                }
            }
        }
    }
}
