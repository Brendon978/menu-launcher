package com.brendon978.menulauncher.settingsui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brendon978.menulauncher.pet.GlitchVisits
import com.brendon978.menulauncher.pet.VISIT_NAME_MAX
import com.brendon978.menulauncher.pet.VisitFailure
import com.brendon978.menulauncher.pet.VisitFriend
import com.brendon978.menulauncher.pet.VisitProfile
import com.brendon978.menulauncher.pet.VisitStore
import com.brendon978.menulauncher.pet.colorHex
import com.brendon978.menulauncher.pet.visitFailureText
import kotlinx.coroutines.launch

/** Código de amigo, lista de amigos e o botão de mandar o Glitch. Nada sai do telefone antes de ativar. */
@Composable
fun VisitsSection(color: Color, accessory: String, buddy: String) {
    val context = LocalContext.current
    val store = remember { VisitStore(context) }
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    val account by store.account.collectAsState(null)
    var friends by remember { mutableStateOf<List<VisitFriend>?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var codeText by remember { mutableStateOf("") }

    /** Um pedido por vez: toque repetido não duplica nada. */
    fun run(work: suspend () -> Unit) {
        if (busy) return
        busy = true
        message = null
        scope.launch {
            try { work() } catch (e: VisitFailure) { message = visitFailureText(e.code) } finally { busy = false }
        }
    }

    val me = account
    if (me == null) {
        Text(
            "Seu Glitch visita o de um amigo. Só o nome, a cor, o acessório e o bichinho vão para o servidor.",
            color = Color.White.copy(alpha = .6f),
            fontSize = 13.sp,
        )
        ActionText(if (busy) "Ativando…" else "Ativar visitas") {
            run {
                val profile = VisitProfile("Glitch", colorHex(color), accessory, buddy)
                store.save(GlitchVisits.create(profile), profile)
            }
        }
        message?.let { Text(it, color = Color.White.copy(alpha = .75f), fontSize = 13.sp) }
        return
    }

    LaunchedEffect(me.code) {
        runCatching { friends = GlitchVisits.friends(me) }.onFailure { message = visitFailureText((it as? VisitFailure)?.code.orEmpty()) }
    }

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.weight(1f)) {
            Text("Seu código", color = Color.White.copy(alpha = .6f), fontSize = 12.sp)
            Text(me.code, color = Color.White, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium)
        }
        ActionText("Copiar") { clipboard.setText(AnnotatedString(me.code)); message = "Código copiado." }
    }
    var nameText by remember(me.code) { mutableStateOf(me.name) }
    OutlinedTextField(
        value = nameText,
        onValueChange = { v ->
            val cut = v.take(VISIT_NAME_MAX)
            nameText = cut
            if (cut.isNotBlank()) scope.launch { store.setName(cut.trim()) }
        },
        singleLine = true,
        label = { Text("Nome do seu Glitch nas visitas") },
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    )
    OutlinedTextField(
        value = codeText,
        onValueChange = { codeText = it },
        singleLine = true,
        label = { Text("Código do amigo") },
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    )
    ActionText("Adicionar amigo") {
        run {
            friends = GlitchVisits.addFriend(me, codeText)
            codeText = ""
        }
    }
    friends?.forEach { f ->
        val name = f.visitor?.name
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text(name ?: f.code, color = Color.White)
                if (name == null) Text("Falta ele adicionar o seu código", color = Color.White.copy(alpha = .5f), fontSize = 12.sp)
            }
            if (name != null) ActionText("Visitar") {
                run {
                    GlitchVisits.send(me, f.code)
                    message = "Seu Glitch foi visitar $name."
                }
            }
            ActionText("✕") { run { friends = GlitchVisits.removeFriend(me, f.code) } }
        }
    }
    message?.let { Text(it, color = Color.White.copy(alpha = .75f), fontSize = 13.sp) }
    ActionText("Desativar visitas e apagar meu Glitch do servidor") {
        run {
            GlitchVisits.delete(me)
            store.clear()
            friends = null
        }
    }
}

@Composable
private fun ActionText(text: String, onClick: () -> Unit) {
    Text(text, color = Color.White.copy(alpha = .85f), modifier = Modifier.clickable(onClick = onClick).padding(vertical = 10.dp, horizontal = 4.dp))
}
