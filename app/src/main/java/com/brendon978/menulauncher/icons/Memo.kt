package com.brendon978.menulauncher.icons

import java.util.concurrent.ConcurrentHashMap

/** Cache seguro entre threads que calcula cada chave uma vez só (inclusive quando o valor é null). */
class Memo<K : Any, V>(private val compute: (K) -> V) {
    private class Box<V>(val value: V)

    private val map = ConcurrentHashMap<K, Box<V>>()

    fun get(key: K): V = map.computeIfAbsent(key) { Box(compute(it)) }.value

    fun clear() = map.clear()
}
