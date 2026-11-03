package com.brendon978.menulauncher.icons

import java.io.InputStream

/** Mapa do Lawnicons (appfilter.xml): componente/pacote -> nome do SVG. */
class AppFilter(
    private val byComponent: Map<String, String>,
    private val byPackage: Map<String, String>,
) {
    fun drawableFor(pkg: String, activity: String): String? =
        byComponent["$pkg/$activity"] ?: byPackage[pkg]

    companion object {
        private val ITEM = Regex("<item\\s[^>]*>")
        private val COMPONENT = Regex("component=\"ComponentInfo\\{([^/}]+)/([^}]+)\\}\"")
        private val DRAWABLE = Regex("drawable=\"([^\"]+)\"")

        fun parse(xml: InputStream): AppFilter {
            val text = xml.bufferedReader().use { it.readText() }
            val byComponent = HashMap<String, String>()
            val counts = HashMap<String, MutableMap<String, Int>>()
            for (tag in ITEM.findAll(text)) {
                val c = COMPONENT.find(tag.value) ?: continue
                val d = DRAWABLE.find(tag.value)?.groupValues?.get(1) ?: continue
                val (pkg, activity) = c.destructured
                byComponent.putIfAbsent("$pkg/$activity", d)
                val c2 = counts.getOrPut(pkg) { LinkedHashMap() }
                c2[d] = (c2[d] ?: 0) + 1
            }
            // Pacote sem o componente exato: o desenho mais usado pelo pacote (empate: o primeiro visto).
            val byPackage = counts.mapValues { (_, c2) -> c2.maxBy { it.value }.key }
            return AppFilter(byComponent, byPackage)
        }
    }
}
