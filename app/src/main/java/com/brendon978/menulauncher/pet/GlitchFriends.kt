package com.brendon978.menulauncher.pet

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Quem você é no servidor de visitas. Sem isto, o launcher não fala com ele. */
data class VisitAccount(val code: String, val secret: String, val name: String)

/** O que vai para o servidor: nada além disso sai do telefone. */
data class VisitProfile(val name: String, val color: String, val accessory: String, val buddy: String)

/** Amigo adicionado. [visitor] só vem quando os dois se adicionaram. */
data class VisitFriend(val code: String, val visitor: Visitor?)

class VisitFailure(val code: String) : Exception(code)

const val VISIT_NAME_MAX = 16
private const val VISIT_API = "https://glitch-visitas.glitch-visitas.workers.dev/v1"
private const val CHECK_EVERY_MS = 10 * 60 * 1000L

fun colorHex(color: Color): String = "#%06X".format(color.toArgb() and 0xFFFFFF)

fun colorFromHex(hex: String): Color? =
    hex.takeIf { Regex("#[0-9A-Fa-f]{6}").matches(it) }?.drop(1)?.toLong(16)?.let { Color(0xFF000000 or it) }

/** Campo desconhecido vira "sem nada": versão nova do amigo não pode derrubar a sua. */
fun parseVisitor(o: JSONObject): Visitor? {
    val name = o.optString("name").takeIf { it.isNotBlank() } ?: return null
    val color = colorFromHex(o.optString("color")) ?: return null
    return Visitor(
        name.take(VISIT_NAME_MAX),
        color,
        Accessory.entries.firstOrNull { it.name == o.optString("accessory") && it.choosable } ?: Accessory.NONE,
        Buddy.entries.firstOrNull { it.name == o.optString("buddy") } ?: Buddy.NONE,
    )
}

private fun JSONArray.objects() = (0 until length()).map { getJSONObject(it) }

fun parseFriends(json: String): List<VisitFriend> =
    JSONObject(json).getJSONArray("amigos").objects().map { o ->
        VisitFriend(o.getString("code"), if (o.optBoolean("mutual")) parseVisitor(o) else null)
    }

fun parseVisits(json: String): List<Visitor> =
    JSONObject(json).getJSONArray("visitas").objects().mapNotNull(::parseVisitor)

fun visitFailureText(code: String): String = when (code) {
    "nao_existe" -> "Esse código não existe."
    "codigo_invalido" -> "Código inválido. É algo como GLITCH-7K2M9X."
    "voce_mesmo" -> "Esse código é o seu."
    "nao_sao_amigos" -> "Os dois precisam adicionar o código um do outro."
    "visita_recente" -> "Seu Glitch acabou de ir. Espere uns minutos."
    "amigos_demais" -> "Amigos demais."
    "devagar" -> "Muitas tentativas. Espere um pouco."
    "perfil_invalido" -> "Nome inválido."
    "nao_autorizado" -> "O servidor não reconheceu seu Glitch."
    else -> "Sem conexão com o servidor. Tente de novo."
}

private val Context.visitStore by preferencesDataStore(name = "glitch_visitas")

/** Guardado à parte das configurações: o segredo não pode ir no arquivo de backup. */
class VisitStore(private val context: Context) {
    private object K {
        val code = stringPreferencesKey("code")
        val secret = stringPreferencesKey("secret")
        val name = stringPreferencesKey("name")
        val synced = stringPreferencesKey("synced_profile")
        val lastCheck = longPreferencesKey("last_check")
    }

    val account: Flow<VisitAccount?> = context.visitStore.data.map { p ->
        val code = p[K.code]
        val secret = p[K.secret]
        if (code == null || secret == null) null else VisitAccount(code, secret, p[K.name] ?: "Glitch")
    }

    suspend fun save(account: VisitAccount, profile: VisitProfile) = context.visitStore.edit {
        it[K.code] = account.code
        it[K.secret] = account.secret
        it[K.name] = account.name
        it[K.synced] = profile.toString()
    }

    suspend fun setName(name: String) = context.visitStore.edit { it[K.name] = name }

    suspend fun clear() = context.visitStore.edit { it.clear() }

    suspend fun synced(): String? = context.visitStore.data.first()[K.synced]
    suspend fun markSynced(profile: VisitProfile) = context.visitStore.edit { it[K.synced] = profile.toString() }
    suspend fun lastCheck(): Long = context.visitStore.data.first()[K.lastCheck] ?: 0L
    suspend fun markChecked(at: Long) = context.visitStore.edit { it[K.lastCheck] = at }
}

object GlitchVisits {
    private suspend fun call(method: String, path: String, account: VisitAccount?, body: JSONObject? = null): String =
        withContext(Dispatchers.IO) {
            val c = (URL(VISIT_API + path).openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = 8_000
                readTimeout = 8_000
                account?.let { setRequestProperty("Authorization", "Bearer ${it.code}:${it.secret}") }
                if (body != null) {
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                }
            }
            try {
                body?.let { b -> c.outputStream.use { it.write(b.toString().toByteArray()) } }
                val ok = c.responseCode in 200..299
                val text = (if (ok) c.inputStream else c.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
                if (!ok) throw VisitFailure(runCatching { JSONObject(text).getString("erro") }.getOrDefault("falha"))
                text
            } catch (e: java.io.IOException) {
                throw VisitFailure("sem_rede")
            } finally {
                c.disconnect()
            }
        }

    private fun VisitProfile.json() = JSONObject()
        .put("name", name).put("color", color).put("accessory", accessory).put("buddy", buddy)

    suspend fun create(profile: VisitProfile): VisitAccount {
        val o = JSONObject(call("POST", "/glitches", null, profile.json()))
        return VisitAccount(o.getString("code"), o.getString("secret"), profile.name)
    }

    suspend fun update(account: VisitAccount, profile: VisitProfile) { call("PUT", "/me", account, profile.json()) }
    suspend fun delete(account: VisitAccount) { call("DELETE", "/me", account) }
    suspend fun friends(account: VisitAccount) = parseFriends(call("GET", "/friends", account))
    suspend fun addFriend(account: VisitAccount, code: String) =
        parseFriends(call("POST", "/friends", account, JSONObject().put("code", code)))
    suspend fun removeFriend(account: VisitAccount, code: String) =
        parseFriends(call("DELETE", "/friends/$code", account))
    suspend fun send(account: VisitAccount, to: String) { call("POST", "/visits", account, JSONObject().put("to", to)) }

    /**
     * Roda com a tela inicial à vista: acerta o perfil se ele mudou e busca visitas a cada 10 minutos.
     * Falha de rede só espera a próxima volta.
     */
    suspend fun check(context: Context, color: Color, accessory: Accessory, buddy: Buddy, now: Long, force: Boolean = false): List<Visitor> {
        val store = VisitStore(context)
        val account = store.account.first() ?: return emptyList()
        return runCatching {
            val profile = VisitProfile(account.name, colorHex(color), accessory.name, buddy.name)
            if (store.synced() != profile.toString()) {
                update(account, profile)
                store.markSynced(profile)
            }
            if (!force && now - store.lastCheck() < CHECK_EVERY_MS) return emptyList()
            store.markChecked(now)
            parseVisits(call("POST", "/visits/claim", account))
        }.getOrDefault(emptyList())
    }
}
