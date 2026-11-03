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
        private const val COMPONENT = "component=\"ComponentInfo{"
        private const val DRAWABLE = "drawable=\""

        /** Leitura por linha com indexOf: o arquivo real tem ~3 MB e regex no texto todo trava a abertura. */
        fun parse(xml: InputStream): AppFilter {
            val byComponent = HashMap<String, String>(16384)
            val counts = HashMap<String, MutableMap<String, Int>>(16384)
            xml.bufferedReader().useLines { lines ->
                for (raw in lines) {
                    var i = raw.indexOf("<item")
                    while (i >= 0) {
                        val end = raw.indexOf('>', i).let { if (it < 0) raw.length else it }
                        val tag = raw.substring(i, end)
                        i = raw.indexOf("<item", end)
                        val c = tag.indexOf(COMPONENT)
                        val d = tag.indexOf(DRAWABLE)
                        if (c < 0 || d < 0) continue
                        val cStart = c + COMPONENT.length
                        val cEnd = tag.indexOf('}', cStart)
                        val dStart = d + DRAWABLE.length
                        val dEnd = tag.indexOf('"', dStart)
                        if (cEnd < 0 || dEnd < 0) continue
                        val component = tag.substring(cStart, cEnd)
                        val slash = component.indexOf('/')
                        if (slash <= 0) continue
                        val drawable = tag.substring(dStart, dEnd)
                        byComponent.putIfAbsent(component, drawable)
                        val perPkg = counts.getOrPut(component.substring(0, slash)) { LinkedHashMap() }
                        perPkg[drawable] = (perPkg[drawable] ?: 0) + 1
                    }
                }
            }
            // Pacote sem o componente exato: o desenho mais usado pelo pacote (empate: o primeiro visto).
            val byPackage = counts.mapValues { (_, c) -> c.maxBy { it.value }.key }
            return AppFilter(byComponent, byPackage)
        }
    }
}
