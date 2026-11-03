# Base do launcher — plano de implementação

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Objetivo:** um launcher que já serve para o uso diário: os dois modos (uma mão e grade), ícones de contorno para todos os apps, música com controles, ajustes no próprio celular e a primeira abertura.

**Arquitetura:** app Android de uma Activity HOME em Jetpack Compose. A lógica pura (índice de letras, favoritos, mapa do Lawnicons, ordem de resolução dos ícones, conversão para contorno) fica em classes Kotlin sem dependência de Android e é testada na JVM. O que depende do aparelho (lista de apps, sessão de mídia, papéis do sistema) fica em repositórios finos e é verificado no celular por ADB.

**Tecnologias:** Kotlin, Jetpack Compose (BOM 2026.09.00), AGP 9.5.0, Gradle 9.8.2, Kotlin 2.4.21 (plugin `org.jetbrains.kotlin.plugin.compose`), DataStore Preferences, AndroidSVG 1.4 (`com.caverock:androidsvg-aar`), JUnit 4.

**Spec:** `docs/superpowers/specs/2026-11-03-meu-launcher-design.md`

**Fora deste plano** (planos seguintes, nesta ordem): (2) papel de parede estático e animado; (3) plugue de IA, incluindo a pergunta da IA na primeira abertura e o `docs/plugue-ia.md`.

## Restrições globais

- Pacote `com.brendon978.menulauncher`; `minSdk 35`; `compileSdk`/`targetSdk 36` (plataforma instalada em `~/android-sdk`).
- Aparelhos: S25 FE (1080×2340, densidade 450) e S24 (tela menor). Layout sem medidas fixas em pixel; medidas em dp e frações da tela.
- Gradle roda no JDK 17 (`/usr/lib/jvm/java-17-openjdk`) via `gradle/gradle-daemon-jvm.properties` (`toolchainVersion=17`). O JDK 26 padrão da máquina não é usado no build.
- `local.properties` com `sdk.dir=/home/brendon/android-sdk`, fora do git.
- Ícones: só contorno, cor do traço fixa escolhida nos ajustes (padrão branco `#FFFFFFFF`), nunca derivada do papel de parede. Logo colorido nunca aparece.
- Ordem das fontes de ícone: Lawnicons → camada monocromática convertida em contorno → inicial num círculo de contorno. A troca manual do usuário vence todas.
- Lawnicons é Apache-2.0: o `LICENSE` vai junto dos assets, em `app/src/main/assets/lawnicons/LICENSE`, fixado no commit `238f782c6b3c29c3ada616ef48190fe7f86836d8`.
- Tudo o que se usa no dia a dia no modo uma mão (favoritos, letras e controles de música) fica na metade de baixo da tela.
- Git só na conta pessoal: o remote é `git@github-pessoal:Brendon978/menu-launcher.git`. Commits terminam com `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
- Celular de teste: `adb -s 100.95.14.17:34017`. Se a conexão cair, peça ao Brendon a porta nova da depuração por Wi-Fi.

## Foco da revisão

1. **Nome que não começa com letra** ("9GAG", "+Rápido", emoji) e nome acentuado ("Ícone", "Ônibus"): o acentuado entra na letra sem acento; o resto vai para o grupo `#` no fim da barra, e o app nunca some. Teste na Task 2.
2. **App desinstalado que estava nos favoritos ou tinha ícone trocado**: some da lista e dos favoritos sem travar, e a troca de ícone dele é descartada. Teste na Task 3.
3. **O mesmo app em dois perfis** (Pasta Segura ou perfil de trabalho do Samsung): aparecem os dois, com chaves diferentes, e favoritar um não favorita o outro. Teste na Task 2.
4. **Sem acesso às notificações ou sem nada tocando**: o cartão e os controles de música somem, sem erro nem espaço vazio. Teste na Task 7.
5. **Tela menor (S24) ou fonte grande do sistema**: a barra de letras não sobrepõe o topo, e a lista de favoritos rola em vez de cortar. Verificação na Task 5 com `adb shell wm size 1080x2340`/`wm size reset` e `settings put system font_scale 1.3`.

---

### Task 1: Esqueleto do projeto instalado como launcher

**Arquivos:**
- Criar: `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `gradle/libs.versions.toml`, `gradle/gradle-daemon-jvm.properties`, wrapper (`gradlew`, `gradle/wrapper/*`), `.gitignore`
- Criar: `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`, `app/src/main/java/com/brendon978/menulauncher/MainActivity.kt`, `app/src/main/java/com/brendon978/menulauncher/home/HomeScreen.kt`
- Teste: `app/src/test/java/com/brendon978/menulauncher/SanityTest.kt`

**Interfaces:**
- Produz: `MainActivity` (única Activity, `launchMode="singleTask"`, `intent-filter` com `MAIN` + `HOME` + `DEFAULT` + `LAUNCHER`); `@Composable fun HomeScreen()`, por enquanto com o relógio no topo.

- [ ] **Passo 1:** criar o wrapper. Baixar `gradle-9.8.2-bin.zip` num diretório temporário próprio e rodar `<gradle>/bin/gradle wrapper --gradle-version 9.8.2` na raiz do repositório.
- [ ] **Passo 2:** escrever os arquivos de build com as versões das Tecnologias. AGP 9 já traz o Kotlin embutido, então não se aplica `org.jetbrains.kotlin.android`, só o plugin de Compose. O `.gitignore` cobre `build/`, `.gradle/`, `local.properties` e `.idea/`. Criar `local.properties`.
- [ ] **Passo 3:** `SanityTest` com `assertEquals(4, 2 + 2)`. Rodar `./gradlew :app:testDebugUnitTest`. Esperado: `BUILD SUCCESSFUL`. Se a combinação AGP/Gradle/Kotlin falhar, suba para a versão de correção mais próxima que funcione e registre no `libs.versions.toml`.
- [ ] **Passo 4:** montar `MainActivity` (tela cheia de ponta a ponta, voltar não faz nada) e `HomeScreen`.
- [ ] **Passo 5:** instalar e abrir: `./gradlew :app:installDebug && adb -s 100.95.14.17:34017 shell am start -n com.brendon978.menulauncher/.MainActivity`. Depois rodar `adb ... shell cmd package query-activities -a android.intent.action.MAIN -c android.intent.category.HOME | grep menulauncher`. Esperado: a Activity aparece como candidata a HOME.
- [ ] **Passo 6:** commit `feat: esqueleto do launcher`.

### Task 2: Catálogo de apps e índice de letras

**Arquivos:**
- Criar: `apps/AppKey.kt`, `apps/AppEntry.kt`, `apps/LetterIndex.kt`, `apps/AppRepository.kt` (todos em `app/src/main/java/com/brendon978/menulauncher/`)
- Teste: `app/src/test/java/com/brendon978/menulauncher/apps/LetterIndexTest.kt`, `AppKeyTest.kt`

**Interfaces:**
- Produz:
  - `data class AppKey(val packageName: String, val activityName: String, val userSerial: Long)` com `fun encode(): String` (`"pkg/activity#serial"`) e `companion fun decode(s: String): AppKey?`.
  - `data class AppEntry(val key: AppKey, val label: String)` (sem tipos do Android, para os testes rodarem na JVM; o `UserHandle` sai de `UserManager.getUserForSerialNumber(key.userSerial)` quando precisar).
  - `object LetterIndex { const val STAR = '★'; const val OTHER = '#'; val BAR: List<Char> /* ★, A–Z, # */; fun letterOf(label: String): Char; fun group(apps: List<AppEntry>): Map<Char, List<AppEntry>> }`. Os grupos ficam ordenados por rótulo, sem diferenciar maiúsculas nem acentos (`Collator` pt-BR).
  - `class AppRepository(context: Context) { val apps: StateFlow<List<AppEntry>>; fun launch(key: AppKey, sourceBounds: Rect?) }`. Usa `LauncherApps.getActivityList(null, user)` em todos os `UserManager.userProfiles` e se atualiza por `LauncherApps.Callback` (instalar, remover, mudar).

- [ ] **Passo 1:** testes que falham:
  - `letterOf("Ícone") == 'I'`, `letterOf("ônibus") == 'O'`, `letterOf("9GAG") == '#'`, `letterOf("+Rápido") == '#'`, `letterOf("😀 Emoji") == '#'`, `letterOf("") == '#'`.
  - `group` com `["banana","Abacate","árvore","9x"]` dá `A -> [Abacate, árvore]`, `B -> [banana]` e `# -> [9x]`.
  - `BAR.first() == '★'`, `BAR.last() == '#'`, `BAR.size == 28`.
  - `AppKey` com `userSerial` 0 e 10 para o mesmo pacote são diferentes; `decode(encode(k)) == k`; `decode("lixo") == null`.
- [ ] **Passo 2:** rodar `./gradlew :app:testDebugUnitTest --tests '*LetterIndexTest*' --tests '*AppKeyTest*'`. Esperado: FAIL.
- [ ] **Passo 3:** implementar. Em `letterOf`, normalizar em NFD, tirar as marcas diacríticas e usar a primeira letra A–Z em maiúscula. Qualquer outro caso vira `#`.
- [ ] **Passo 4:** rodar de novo. Esperado: PASS.
- [ ] **Passo 5:** implementar o `AppRepository`. Usar um `AppKey` por `LauncherActivityInfo` com `userSerial = userManager.getSerialNumberForUser(user)` e abrir pelo `LauncherApps.startMainActivity`.
- [ ] **Passo 6:** commit `feat: catálogo de apps e índice de letras`.

### Task 3: Ajustes persistentes e favoritos

**Arquivos:**
- Criar: `settings/LauncherSettings.kt`, `settings/FavoritesOps.kt`, `settings/SettingsRepository.kt`
- Teste: `app/src/test/java/com/brendon978/menulauncher/settings/FavoritesOpsTest.kt`

**Interfaces:**
- Consome: `AppKey` (Task 2).
- Produz:
  - `enum class HomeMode { ONE_HAND, GRID }`.
  - `data class LauncherSettings(val mode: HomeMode = ONE_HAND, val favorites: List<AppKey> = emptyList(), val strokeColor: Int = 0xFFFFFFFF.toInt(), val iconOverrides: Map<AppKey, String> = emptyMap() /* valor = nome do drawable do Lawnicons */, val showMusic: Boolean = true, val firstRunDone: Boolean = false)`.
  - `object FavoritesOps { fun toggle(favs: List<AppKey>, key: AppKey): List<AppKey>; fun move(favs: List<AppKey>, from: Int, to: Int): List<AppKey>; fun prune(s: LauncherSettings, installed: Set<AppKey>): LauncherSettings }`.
  - `class SettingsRepository(context: Context) { val settings: Flow<LauncherSettings>; suspend fun update(f: (LauncherSettings) -> LauncherSettings) }`, com DataStore Preferences e chaves codificadas por `AppKey.encode()`. Entradas que não decodificam são ignoradas.
  - Favoritos iniciais, aplicados quando `firstRunDone == false` e a lista está vazia: os apps padrão de Telefone, Mensagens e Câmera, resolvidos pelas intents `ACTION_DIAL`, `Intent.makeMainSelectorActivity(ACTION_MAIN, CATEGORY_APP_MESSAGING)` e `MediaStore.ACTION_IMAGE_CAPTURE`. O que não resolver fica de fora.

- [ ] **Passo 1:** testes que falham:
  - `toggle` adiciona no fim e, chamado de novo, remove;
  - `move(listOf(a,b,c), 0, 2) == listOf(b,c,a)`;
  - `prune` com `favorites=[a,b]`, `iconOverrides={b:"x", c:"y"}` e `installed={a}` devolve `favorites=[a]` e `iconOverrides={}`.
- [ ] **Passo 2:** rodar os testes. Esperado: FAIL.
- [ ] **Passo 3:** implementar `FavoritesOps` e `SettingsRepository`. Chamar `prune` sempre que `AppRepository.apps` emitir.
- [ ] **Passo 4:** rodar os testes. Esperado: PASS.
- [ ] **Passo 5:** commit `feat: ajustes persistentes e favoritos`.

### Task 4: Ícones de contorno

**Arquivos:**
- Criar: `tools/fetch-lawnicons.sh`, `app/src/main/assets/lawnicons/` (`svgs/*.svg`, `appfilter.xml`, `LICENSE`)
- Criar: `icons/AppFilter.kt`, `icons/IconResolver.kt`, `icons/OutlineTracer.kt`, `icons/IconRenderer.kt`
- Teste: `app/src/test/java/com/brendon978/menulauncher/icons/AppFilterTest.kt`, `IconResolverTest.kt`, `OutlineTracerTest.kt`

**Interfaces:**
- Consome: `AppKey` (Task 2) e `LauncherSettings.iconOverrides`/`strokeColor` (Task 3).
- Produz:
  - `class AppFilter(byComponent: Map<String, String>, byPackage: Map<String, String>) { fun drawableFor(pkg: String, activity: String): String?; companion fun parse(xml: java.io.InputStream): AppFilter }`. Lê os `<item component="ComponentInfo{pkg/activity}" drawable="nome"/>`. Busca primeiro o componente exato e depois o primeiro item com o mesmo pacote.
  - `sealed interface IconSource { data class Lawnicon(val name: String); data class Monochrome(val key: AppKey); data class Initial(val letter: Char) }`.
  - `fun resolveIcon(key: AppKey, label: String, override: String?, filter: AppFilter, hasMonochrome: (AppKey) -> Boolean): IconSource`, uma função pura na ordem: override → Lawnicons → monocromático → inicial (`LetterIndex.letterOf`, e se der `#` usa o primeiro caractere visível do rótulo).
  - `object OutlineTracer { fun trace(alpha: IntArray, size: Int, thickness: Int): BooleanArray }`, também pura. Retorna `true` nos pixels de borda: `mask = alpha >= 128`, `borda = dilate(mask, thickness/2) && !erode(mask, thickness/2)`, com vizinhança quadrada.
  - `class IconRenderer(context: Context, filter: AppFilter) { fun render(source: IconSource, sizePx: Int, color: Int): ImageBitmap }`. Lawnicon: SVG de `assets/lawnicons/svgs/<nome>.svg` desenhado pelo AndroidSVG com `RenderOptions.css("* { stroke: #RRGGBB !important; }")`. Monochrome: `AdaptiveIconDrawable.monochrome` do ícone (`LauncherApps.getActivityList(...)...getIcon(0)`), rasterizado em 192 px, passado pelo `trace(..., 192, 14)` e pintado na cor. Initial: círculo de contorno com a letra, traço `sizePx * 14 / 192`. Cache LRU por `(source, sizePx, color)`.

- [ ] **Passo 1:** escrever `tools/fetch-lawnicons.sh`. Ele baixa o tarball do commit fixado num diretório temporário novo e copia só `svgs/`, `app/assets/appfilter.xml` e `LICENSE` para `app/src/main/assets/lawnicons/`. Rodar e commitar os assets.
- [ ] **Passo 2:** testes que falham:
  - `AppFilter.parse` sobre um XML de teste com `ComponentInfo{com.spotify.music/com.spotify.music.MainActivity}` → `spotify` dá `drawableFor("com.spotify.music","com.spotify.music.MainActivity") == "spotify"` e `drawableFor("com.spotify.music","Outra") == "spotify"`; um pacote ausente dá `null`.
  - `resolveIcon`: override `"x"` vence tudo; sem override e com o pacote no filtro dá `Lawnicon`; fora do filtro e com `hasMonochrome=true` dá `Monochrome`; senão `Initial('S')` para "Spotify" e `Initial('9')` para "9GAG".
  - `OutlineTracer`: quadrado cheio de 100×100 centralizado em 192 com `thickness=14` deixa o pixel (96,96) `false` e o (46,96) `true` (borda); imagem toda transparente dá tudo `false`.
- [ ] **Passo 3:** rodar os testes. Esperado: FAIL.
- [ ] **Passo 4:** implementar `AppFilter`, `resolveIcon` e `OutlineTracer`.
- [ ] **Passo 5:** rodar os testes. Esperado: PASS.
- [ ] **Passo 6:** implementar o `IconRenderer`.
- [ ] **Passo 7:** commit `feat: ícones de contorno (Lawnicons, monocromático e inicial)`.

### Task 5: Modo uma mão

**Arquivos:**
- Criar: `home/TopInfo.kt`, `home/OneHandMode.kt`, `home/AlphabetBar.kt`, `home/AppRow.kt`
- Modificar: `home/HomeScreen.kt`

**Interfaces:**
- Consome: `AppRepository.apps`, `LetterIndex` (Task 2), `SettingsRepository.settings` (Task 3), `IconRenderer` e `resolveIcon` (Task 4).
- Produz: `@Composable fun AppRow(entry: AppEntry, icon: ImageBitmap, onClick: () -> Unit, onLongClick: () -> Unit)`, reaproveitada na Task 6 e na Task 8. `HomeScreen` recebe os parâmetros `musicSlotTop: @Composable () -> Unit = {}` e `musicSlotBottom: @Composable () -> Unit = {}`, preenchidos na Task 7, e `onEmptyLongPress: () -> Unit` e `onAppLongPress: (AppEntry) -> Unit`, ligados na Task 8.

Layout, conforme o `docs/esboco/tela-inicial.html`:
- Topo: relógio de 52sp em peso leve (atualiza a cada minuto) e data em pt-BR (`ter., 3 de novembro`); abaixo, o `musicSlotTop`. Tocar no relógio abre o app de alarmes (`AlarmClock.ACTION_SHOW_ALARMS`).
- Metade de baixo: cabeçalho ("Favoritos" ou a letra), lista de `AppRow` (ícone de 24dp, texto de 15sp, linha de 42dp), rolável, ancorada embaixo; abaixo da lista, o `musicSlotBottom`.
- Barra de letras na borda direita: `LetterIndex.BAR` (28 itens), curva para dentro no meio com deslocamento `sin(i/(n-1)·π)·22dp` e altura de cada item `min(15.5dp, alturaDisponível/28)`. Ela ocupa no máximo a metade de baixo da tela. Tocar ou arrastar seleciona a letra com uma vibração curta (`HapticFeedbackType.TextHandleMove`). Letra sem apps fica com 35% de opacidade, e selecioná-la mostra "nenhum app". `★` volta aos favoritos. Ao voltar para a tela inicial (`onResume`/botão home), a seleção volta para `★`.

- [ ] **Passo 1:** implementar `TopInfo`, `AppRow`, `AlphabetBar` e `OneHandMode`, e ligar no `HomeScreen`.
- [ ] **Passo 2:** instalar e definir como padrão no celular (Configurações → Apps → Apps padrão → Tela inicial). Tirar o print: `adb -s 100.95.14.17:34017 exec-out screencap -p > /tmp/menu-launcher/onehand.png`. Comparar com o esboço: ícones de contorno brancos, barra curva e favoritos embaixo.
- [ ] **Passo 3:** foco da revisão 5: `adb ... shell wm size 1080x2200` e `settings put system font_scale 1.3`, tirar o print e conferir que a barra não sobrepõe o topo e a lista rola. Depois desfazer com `wm size reset` e `settings put system font_scale 1.0`.
- [ ] **Passo 4:** foco da revisão 1, no aparelho: tocar em `#` lista os apps que começam com número ou símbolo.
- [ ] **Passo 5:** commit `feat: modo uma mão`.

### Task 6: Modo grade

**Arquivos:**
- Criar: `home/GridMode.kt`
- Modificar: `home/HomeScreen.kt`

**Interfaces:**
- Consome: `AppRow`/`IconRenderer` (Tasks 4 e 5) e `LauncherSettings.mode`.
- Produz: `@Composable fun GridMode(apps: List<AppEntry>, ...)`. `HomeScreen` escolhe o modo por `settings.mode`.

Grade de todos os apps em ordem alfabética (mesmo `Collator` da Task 2), ícone de 28dp com o nome embaixo (11sp, uma linha com reticências), colunas `GridCells.Adaptive(76.dp)` e rolagem vertical. O topo mostra só relógio e data. Os controles de música desta tela também ficam embaixo (o mesmo `musicSlotBottom`).

- [ ] **Passo 1:** implementar e ligar a troca pelo ajuste (a tela de ajustes vem na Task 8; até lá, testar mudando o valor padrão).
- [ ] **Passo 2:** instalar, tirar o print do modo grade e conferir.
- [ ] **Passo 3:** commit `feat: modo grade`.

### Task 7: Música

**Arquivos:**
- Criar: `music/MediaListener.kt` (um `NotificationListenerService` vazio, só para liberar o acesso), `music/MusicState.kt`, `music/MusicRepository.kt`, `music/MusicCard.kt`, `music/MusicControls.kt`
- Modificar: `AndroidManifest.xml` (serviço com `BIND_NOTIFICATION_LISTENER_SERVICE`) e `home/HomeScreen.kt`
- Teste: `app/src/test/java/com/brendon978/menulauncher/music/MusicStateTest.kt`

**Interfaces:**
- Produz:
  - `data class MusicState(val title: String, val artist: String?, val appLabel: String, val art: Bitmap?, val playing: Boolean)`.
  - `fun pickSession(sessions: List<SessionInfo>): SessionInfo?`, pura: prefere a que está tocando e, empatando, a primeira da lista (a mais recente, na ordem do `MediaSessionManager`). `SessionInfo(val packageName: String, val playing: Boolean)`.
  - `class MusicRepository(context: Context) { val state: StateFlow<MusicState?>; fun hasAccess(): Boolean; fun playPause(); fun next(); fun previous() }`. Usa `MediaSessionManager.getActiveSessions(ComponentName(ctx, MediaListener::class.java))` e o `addOnActiveSessionsChangedListener`. Sem acesso ou sem sessão, o estado fica `null`.
- Consome: os slots `musicSlotTop`/`musicSlotBottom` da Task 5 e `LauncherSettings.showMusic`.

- [ ] **Passo 1:** testes de `pickSession` que falham: lista vazia dá `null`; `[pausada A, tocando B]` dá B; `[pausada A, pausada B]` dá A.
- [ ] **Passo 2:** rodar. Esperado: FAIL. Implementar. Rodar. Esperado: PASS.
- [ ] **Passo 3:** implementar o repositório e ligar: `MusicCard` (capa de 36dp, título e artista, contorno sutil) no slot de cima; `MusicControls` (⏮ ⏯ ⏭ em contorno, alvos de 48dp) no slot de baixo. Os dois só aparecem com `state != null && showMusic`.
- [ ] **Passo 4:** no aparelho, foco da revisão 4: sem acesso às notificações, nada de música aparece. Liberar o acesso (`adb ... shell cmd notification allow_listener com.brendon978.menulauncher/com.brendon978.menulauncher.music.MediaListener`), tocar algo no YT Music, conferir o cartão e os controles por print e apertar ⏯ para ver pausar.
- [ ] **Passo 5:** commit `feat: música com cartão e controles`.

### Task 8: Menu do app, troca de ícone e tela de ajustes

**Arquivos:**
- Criar: `menu/AppActionsSheet.kt`, `menu/IconPickerScreen.kt`, `settingsui/SettingsScreen.kt`, `settingsui/ColorChoices.kt`
- Modificar: `home/HomeScreen.kt`, `MainActivity.kt` (navegação simples por estado: Home, Settings, IconPicker(AppKey))

**Interfaces:**
- Consome: `FavoritesOps`/`SettingsRepository` (Task 3), `AppFilter`/`IconRenderer` (Task 4) e os callbacks `onAppLongPress`/`onEmptyLongPress` (Task 5).
- Produz: `fun lawniconNames(assets: AssetManager): List<String>` (nomes dos SVGs, para a busca do seletor).

Comportamento:
- Toque longo num app abre uma folha que sobe de baixo com: Favoritar ou Remover dos favoritos, Trocar ícone, Informações do app (`ACTION_APPLICATION_DETAILS_SETTINGS`) e Desinstalar (`ACTION_DELETE` com `package:` URI). Desinstalar não aparece para app de sistema.
- Trocar ícone: grade de Lawnicons com busca por nome, com o primeiro item "Automático" (remove a troca). Escolher grava em `iconOverrides`.
- Toque longo numa área vazia abre os Ajustes: modo (Uma mão/Grade); cor do traço, com 6 opções fixas (`#FFFFFF`, `#E6E1D6`, `#B8B8B8`, `#111111`, `#7FB7A4`, `#C9A27E`) mais a opção de digitar um hex; mostrar música; favoritos (lista com arrastar para reordenar, por `FavoritesOps.move`, e remover); e "Definir como tela inicial padrão" (ver Task 9).

- [ ] **Passo 1:** implementar a folha e as ações, e conferir no aparelho que favoritar aparece na lista de favoritos.
- [ ] **Passo 2:** implementar o seletor de ícone. No aparelho, trocar o ícone de um app, conferir, voltar para "Automático" e conferir de novo.
- [ ] **Passo 3:** implementar os Ajustes. No aparelho, trocar a cor do traço e conferir que todos os ícones mudam e continuam iguais ao trocar o papel de parede do sistema. Trocar o modo e reordenar os favoritos.
- [ ] **Passo 4:** foco da revisão 2, no aparelho: favoritar e trocar o ícone de um app de teste, desinstalar pelo próprio menu e conferir que ele some dos favoritos sem travar.
- [ ] **Passo 5:** commit `feat: menu do app, troca de ícone e ajustes`.

### Task 9: Primeira abertura

**Arquivos:**
- Criar: `onboarding/FirstRunScreen.kt`
- Modificar: `MainActivity.kt`

**Interfaces:**
- Consome: `LauncherSettings.firstRunDone` (Task 3) e `MusicRepository.hasAccess()` (Task 7).
- Produz: `FirstRunScreen(onDone: () -> Unit)`, com o passo de IA a ser inserido pelo plano 3 entre os passos 2 e 3.

Passos, um por tela, cada um pulável:
1. "Usar como tela inicial": `RoleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)`. Se o papel já for do app, o passo é pulado.
2. "Mostrar a música que está tocando": abre `Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS`. Se já houver acesso, o passo é pulado.
3. Fim: grava `firstRunDone = true` e aplica os favoritos iniciais (Task 3).

Os Ajustes ganham "Definir como tela inicial padrão", que abre o mesmo pedido do passo 1.

- [ ] **Passo 1:** implementar.
- [ ] **Passo 2:** no aparelho: `adb ... shell pm clear com.brendon978.menulauncher`, abrir o app, passar pelos passos e conferir que ele vira a tela inicial padrão (`adb ... shell cmd role get-role-holders android.app.role.HOME` lista o pacote) e que os favoritos iniciais aparecem.
- [ ] **Passo 3:** rodar `./gradlew :app:testDebugUnitTest :app:assembleDebug`. Esperado: `BUILD SUCCESSFUL`.
- [ ] **Passo 4:** commit `feat: primeira abertura` e push (`git push`).
