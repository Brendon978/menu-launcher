package com.brendon978.menulauncher.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brendon978.menulauncher.apps.AppEntry
import com.brendon978.menulauncher.apps.searchApps
import com.brendon978.menulauncher.icons.IconRenderer
import com.brendon978.menulauncher.settings.LauncherSettings

/**
 * Buscador de apps: campo embaixo (zona do polegar, sobe com o teclado) e resultados logo acima,
 * o melhor mais perto do campo. "Ir" no teclado abre o primeiro resultado.
 */
@Composable
fun SearchScreen(
    apps: List<AppEntry>,
    settings: LauncherSettings,
    renderer: IconRenderer,
    onLaunch: (AppEntry) -> Unit,
    onAppLongPress: (AppEntry) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val results = remember(apps, query) { searchApps(apps, query) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    Column(
        verticalArrangement = Arrangement.Bottom,
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xE60C0C0E))
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 22.dp, vertical = 12.dp),
    ) {
        LazyColumn(
            reverseLayout = true, // o primeiro resultado fica colado no campo
            modifier = Modifier.weight(1f, fill = false),
        ) {
            items(results, key = { it.key.encode() }) { entry ->
                AppRow(
                    entry = entry,
                    override = settings.iconOverrides[entry.key],
                    color = settings.strokeColor,
                    renderer = renderer,
                    onClick = { onLaunch(entry) },
                    onLongClick = { onAppLongPress(entry) },
                )
            }
        }
        if (query.isNotBlank() && results.isEmpty()) {
            Text("Nenhum app com “$query”", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp, modifier = Modifier.padding(vertical = 8.dp))
        }
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            placeholder = { Text("Buscar app") },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { results.firstOrNull()?.let(onLaunch) }),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .focusRequester(focus),
        )
    }
}
