# Robozinho — plano de implementação

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans.

**Spec:** `docs/superpowers/specs/2026-11-03-robozinho-design.md`. **Esboço:** `docs/esboco/robozinho.html` (variante `corpo`).

**Arquitetura:** o "cérebro" (humor, irritação, felicidade pelas notificações, faixa do chão, próximo destino) é puro, em `pet/PetBrain.kt`, e testado na JVM. O desenho é feito em `pet/RobotDrawing.kt` (DrawScope, coordenadas do esboço com viewBox 120×170). O comportamento fica em `pet/RobotPet.kt` (composable que anda, reage ao toque e pausa fora da tela). As fontes de dados são a bateria (broadcast fixo), as notificações (o `MediaListener` passa a contar) e a música (`MusicState.playing`).

## Tasks
1. `PetBrain` com TDD:
   - `baseMood(hour, battery, charging, musicPlaying, happyUntil, now)` segue a prioridade da spec: dormindo, cansado, música, feliz, normal. Às 23h dorme, às 7h acorda; com bateria em 15% e sem carregar, fica cansado.
   - `Annoyance.tap(now)` dá a reação RISADINHA, SÉRIO, BRAVO ou FOGE. A irritação decai com o tempo, e depois de FOGE volta para "sério".
   - `happyUntil(prev, new, now, current)`: só quando o número de notificações vai de mais de 0 para 0, dá agora + 60 s.
   - `floorRange(...)` exclui a barra de letras e o símbolo da IA quando ele cruza o chão.
   - `nextTarget(...)`: o destino fica sempre dentro da faixa e longe do ponto atual.
2. Contagem de notificações no `MediaListener`, sem as persistentes nem as do próprio launcher, publicada num `StateFlow`.
3. Desenho do robô e das caras: normal, piscando, dormindo, equalizador, coração, cansado, escaneando, risadinha, sério e bravo.
4. `RobotPet`: anda no chão, para, escaneia, dá glitch, dança, responde ao toque e pausa quando a tela inicial não está em primeiro plano.
5. Ajuste "Robozinho" (`showPet`, ligado por padrão), ligação no modo uma mão, conferência no aparelho e merge na main.
