# Papel de parede — plano de implementação

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Objetivo:** aplicar, a partir dos Ajustes, um papel de parede que o usuário baixou: estático (JPG, PNG, WEBP) na tela inicial, na de bloqueio ou nas duas; animado (MP4, GIF) na tela inicial, como papel de parede animado do Android.

**Arquitetura:** a lógica pura (classificar o arquivo e mapear o destino para as flags do `WallpaperManager`) é testada na JVM. O estático vai direto pelo `WallpaperManager.setStream`. O animado copia o arquivo para `filesDir/wallpaper/` e roda num `WallpaperService` próprio: vídeo por `MediaPlayer` na surface, GIF por `AnimatedImageDrawable` desenhado no canvas. Os dois pausam quando o papel de parede fica invisível.

**Spec:** `docs/superpowers/specs/2026-11-03-meu-launcher-design.md` (objetivo 4).

## Restrições globais

- As mesmas do plano 1: pacote `com.brendon978.menulauncher`, `minSdk 35`, conta git pessoal, celular `adb -s 100.95.14.17:<porta>`.
- O app não cria papéis de parede, só aplica os que o usuário escolhe.
- O animado é só para a tela inicial. Se o One UI também mostrar ele na tela de bloqueio, tudo bem.
- O animado fica parado com a tela apagada ou quando está coberto (`onVisibilityChanged(false)`).
- Vídeo sempre sem som.

## Foco da revisão

1. **Tipo de arquivo não suportado** (PDF, HEIC sem decodificador, áudio): mensagem "Formato não suportado", e nada é aplicado. Teste na Task 1.
2. **GIF chega como `image/gif`**: ele é animado, não estático, mesmo começando com `image/`. Teste na Task 1.
3. **Arquivo trocado com o papel animado já ativo**: o serviço recarrega na próxima vez que fica visível, sem precisar reaplicar. Verificação na Task 3.
4. **Arquivo apagado, ou a cópia interna sumiu**: o serviço desenha preto e não trava. Verificação na Task 3.
5. **Imagem enorme** (foto de 50 MP): a aplicação roda fora da thread principal e a tela não congela. Verificação na Task 2.

---

### Task 1: classificação e destino (lógica pura)

**Arquivos:** criar `wallpaper/WallpaperKind.kt`; teste `app/src/test/.../wallpaper/WallpaperKindTest.kt`.

**Interfaces produzidas:**
- `sealed interface WallpaperKind { data object Static; data class Animated(val ext: String /* "mp4" | "gif" */); data object Unsupported }`
- `fun classify(mime: String?, fileName: String?): WallpaperKind`. O MIME vence; sem MIME, decide pela extensão. `image/gif` e `.gif` dão `Animated("gif")`. `video/*` e `.mp4`/`.webm`/`.mkv` dão `Animated("mp4")`, porque o `MediaPlayer` toca os três. `image/jpeg|png|webp` e `.jpg/.jpeg/.png/.webp` dão `Static`. Qualquer outro caso dá `Unsupported`.
- `enum class StaticTarget { HOME, LOCK, BOTH }` e `fun StaticTarget.flags(): Int`: HOME = 1 (`FLAG_SYSTEM`), LOCK = 2 (`FLAG_LOCK`), BOTH = 3.

- [ ] Testes (falhar → implementar → passar): `classify("image/gif", null)` dá `Animated("gif")`; `classify(null, "x.GIF")` dá `Animated("gif")`; `classify("video/mp4", null)` dá `Animated("mp4")`; `classify("image/webp", null)` dá `Static`; `classify("application/pdf", "a.pdf")` dá `Unsupported`; `classify(null, null)` dá `Unsupported`; as flags dão 1, 2 e 3.
- [ ] Commit `feat: classificação do papel de parede`.

### Task 2: estático e seção nos Ajustes

**Arquivos:** criar `wallpaper/WallpaperApplier.kt` e `settingsui/WallpaperSection.kt`; modificar `SettingsScreen.kt`, `MainActivity.kt` e o manifesto (`android.permission.SET_WALLPAPER`).

- `class WallpaperApplier(context)`, com `suspend fun applyStatic(uri: Uri, target: StaticTarget)` (IO, `setStream(stream, null, true, target.flags())`) e `suspend fun prepareAnimated(uri: Uri, ext: String): File`. Esta copia para `filesDir/wallpaper/current.<ext>`, apaga a outra extensão e grava num arquivo temporário antes de renomear, para o serviço nunca ler uma cópia pela metade.
- Seção "Papel de parede" nos Ajustes, com o botão "Escolher imagem ou vídeo" (`OpenDocument`, tipos `image/*` e `video/*`). Se for `Static`, abre uma folha com "Tela inicial", "Tela de bloqueio" e "As duas". Se for `Animated`, copia e abre a prévia do sistema. Se for `Unsupported`, mostra um toast "Formato não suportado".
- [ ] No aparelho: o seletor abre e a escolha de destino aparece. Aplicar de verdade é o Brendon quem testa, porque troca o papel de parede dele.
- [ ] Commit.

### Task 3: papel de parede animado

**Arquivos:** criar `wallpaper/AnimatedWallpaperService.kt` e `res/xml/animated_wallpaper.xml`; modificar o manifesto (serviço com `BIND_WALLPAPER`).

- O engine escolhe por qual arquivo existe: `current.mp4` ou `current.gif`. Ao ficar visível, compara o `lastModified` e recarrega se o arquivo mudou.
- Vídeo: `MediaPlayer` com `setSurface`, `isLooping`, volume 0 e `VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING`. Pausa quando fica invisível e é liberado quando a surface é destruída.
- GIF: `ImageDecoder.decodeDrawable` dá um `AnimatedImageDrawable`. Um laço no `Handler` (cerca de 30 fps) só enquanto visível, desenhando com corte central.
- Sem arquivo: desenha preto.
- A prévia abre por `WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER` com `EXTRA_LIVE_WALLPAPER_COMPONENT`.
- [ ] No aparelho: copiar um MP4 e um GIF de teste (gerados com ffmpeg) para os arquivos do app por `run-as` e abrir a prévia, sem aplicar. Conferir por print que anima, que troca do MP4 para o GIF e que, sem arquivo, fica preto.
- [ ] Commit, push e merge na main.
