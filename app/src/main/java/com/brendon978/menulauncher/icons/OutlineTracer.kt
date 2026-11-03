package com.brendon978.menulauncher.icons

/**
 * Converte uma silhueta (canal alfa) em contorno: borda = dilate(mask, r) && !erode(mask, r),
 * com r = thickness/2 e vizinhança quadrada.
 */
object OutlineTracer {
    fun trace(alpha: IntArray, size: Int, thickness: Int): BooleanArray {
        val mask = BooleanArray(size * size) { alpha[it] >= 128 }
        val r = thickness / 2
        val dilated = morph(mask, size, r, dilate = true)
        val eroded = morph(mask, size, r, dilate = false)
        return BooleanArray(size * size) { dilated[it] && !eroded[it] }
    }

    // Separável: passa em linhas e depois em colunas. Fora da imagem conta como vazio.
    private fun morph(src: BooleanArray, size: Int, r: Int, dilate: Boolean): BooleanArray {
        val tmp = BooleanArray(size * size)
        val out = BooleanArray(size * size)
        for (y in 0 until size) for (x in 0 until size) {
            tmp[y * size + x] = window(dilate, x - r, x + r, size) { src[y * size + it] }
        }
        for (y in 0 until size) for (x in 0 until size) {
            out[y * size + x] = window(dilate, y - r, y + r, size) { tmp[it * size + x] }
        }
        return out
    }

    private inline fun window(dilate: Boolean, from: Int, to: Int, size: Int, get: (Int) -> Boolean): Boolean {
        if (!dilate && (from < 0 || to >= size)) return false
        for (i in maxOf(0, from)..minOf(size - 1, to)) {
            val v = get(i)
            if (dilate && v) return true
            if (!dilate && !v) return false
        }
        return !dilate
    }
}
