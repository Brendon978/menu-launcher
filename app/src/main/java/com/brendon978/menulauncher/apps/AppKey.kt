package com.brendon978.menulauncher.apps

/** Identifica um app abrível: componente + perfil do usuário (Pasta Segura e perfil de trabalho geram outro serial). */
data class AppKey(val packageName: String, val activityName: String, val userSerial: Long) {
    fun encode(): String = "$packageName/$activityName#$userSerial"

    companion object {
        private val FORMAT = Regex("^([^/#]+)/([^#]+)#(-?\\d+)$")

        fun decode(s: String): AppKey? {
            val m = FORMAT.matchEntire(s) ?: return null
            val (pkg, activity, serial) = m.destructured
            return AppKey(pkg, activity, serial.toLongOrNull() ?: return null)
        }
    }
}
