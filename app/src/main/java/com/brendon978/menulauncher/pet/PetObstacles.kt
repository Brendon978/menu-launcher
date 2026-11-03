package com.brendon978.menulauncher.pet

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned

/** O que está na tela inicial e o robozinho não pode pisar (posições na janela). */
class PetObstacles {
    val rects: SnapshotStateMap<String, Rect> = mutableStateMapOf()
}

val LocalPetObstacles = compositionLocalOf<PetObstacles?> { null }

/** Marca este elemento como obstáculo para o robozinho; some do mapa quando o elemento sai da tela. */
fun Modifier.petObstacle(key: String): Modifier = composed {
    val reg = LocalPetObstacles.current ?: return@composed this
    DisposableEffect(key) { onDispose { reg.rects.remove(key) } }
    onGloballyPositioned { reg.rects[key] = it.boundsInRoot() }
}
