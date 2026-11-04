package com.brendon978.menulauncher.settings

import com.brendon978.menulauncher.apps.AppKey
import com.brendon978.menulauncher.wallpaper.WallpaperInterval

enum class HomeMode { ONE_HAND, GRID }

data class LauncherSettings(
    val mode: HomeMode = HomeMode.ONE_HAND,
    val favorites: List<AppKey> = emptyList(),
    val strokeColor: Int = 0xFFFFFFFF.toInt(),
    /** Cor do traço automática; desligada vale [strokeColor]. */
    val strokeAuto: StrokeAuto = StrokeAuto.OFF,
    /** Cor de dia (7h às 19h) no modo por horário. */
    val strokeDay: Int = 0xFFFFFFFF.toInt(),
    /** Cor de noite no modo por horário. */
    val strokeNight: Int = 0xFFE6E1D6.toInt(),
    /** valor = nome do drawable do Lawnicons */
    val iconOverrides: Map<AppKey, String> = emptyMap(),
    val showMusic: Boolean = true,
    val firstRunDone: Boolean = false,
    /** IA conectada (null = nenhuma). aiActivity é a que abre no toque. */
    val aiPackage: String? = null,
    val aiActivity: String? = null,
    /** Posição do símbolo da IA, em fração da tela (zona do polegar, à esquerda da barra de letras). */
    val aiX: Float = 0.70f,
    val aiY: Float = 0.55f,
    /** Robozinho na tela inicial (modo uma mão). */
    val showPet: Boolean = true,
    /** Nome do acessório do Glitch (enum Accessory). */
    val glitchAccessory: String = "NONE",
    /** Nomes que você deu aos apps ("Samsung Shop" -> "Shop"). */
    val customLabels: Map<AppKey, String> = emptyMap(),
    /** Apps escondidos da lista e da grade (a busca ainda acha). */
    val hiddenApps: Set<AppKey> = emptySet(),
    /** Pastas do modo grade: nome -> apps (na ordem em que entraram). */
    val folders: Map<String, List<AppKey>> = emptyMap(),
    /** Tocar duas vezes no fundo apaga a tela (precisa do serviço de acessibilidade ligado). */
    val doubleTapLock: Boolean = false,
    /** Deslizar para baixo no fundo. */
    val gestureDown: GestureChoice = GestureChoice(GestureAction.NOTIFICATIONS),
    /** Deslizar para cima no fundo. */
    val gestureUp: GestureChoice = GestureChoice(GestureAction.SEARCH),
    /** Dois toques no fundo; null = ainda não escolhido (vale [doubleTapLock], ver doubleTapChoice). */
    val gestureDoubleTap: GestureChoice? = null,
    /** Quanto você usa cada app (para a linha de mais usados). */
    val usage: Map<AppKey, Float> = emptyMap(),
    /** Linha com os apps mais usados em cima dos favoritos. */
    val showMostUsed: Boolean = false,
    /** Deslizar um app da lista para o lado abre as mesmas ações do toque longo. */
    val swipeAppActions: Boolean = false,
    /** Atalhos rápidos (lanterna, Wi-Fi, internet, não perturbe) embaixo da data. */
    val showQuickActions: Boolean = false,
    /** Próximo compromisso da agenda embaixo da data (pede acesso à agenda). */
    val showNextEvent: Boolean = false,
    /** Favoritos diferentes de dia, no trabalho e à noite. */
    val timeProfiles: Boolean = false,
    /** Lista de cada horário (nome de TimeProfile -> apps); vazia usa [favorites]. */
    val profileFavorites: Map<String, List<AppKey>> = emptyMap(),
    /** Contatos nos resultados da busca (pede acesso aos contatos). */
    val searchContacts: Boolean = false,
    /** O Glitch comenta o app de onde você voltou. */
    val glitchComments: Boolean = true,
    /** O Glitch apronta com o relógio e os favoritos. */
    val glitchMischief: Boolean = true,
    /** Seu aniversário, "dia/mês" (vazio = sem festa). */
    val birthday: String = "",
    /** Cidade para o clima do Glitch (vazia = ele não consulta a internet). */
    val weatherCity: String = "",
    /** Pasta do papel de parede que muda (URI da árvore; vazia = nenhuma). */
    val wallpaperFolder: String = "",
    /** Intervalo da troca; OFF não mexe no papel de parede. */
    val wallpaperInterval: WallpaperInterval = WallpaperInterval.OFF,
    /** Quando a última troca foi tentada, em milissegundos. */
    val wallpaperLastChange: Long = 0,
    /** Nome da última imagem aplicada (a próxima vem depois dela). */
    val wallpaperLastImage: String = "",
)
