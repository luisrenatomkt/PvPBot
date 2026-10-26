# PvP Bot Trainer (Minecraft 1.8.9 + Forge)

Projeto-fonte do mod. NAO foi compilado nem testado no ambiente em que foi escrito
(sem JDK 8, sem Forge e sem internet). Compile com uma das opcoes abaixo.

## Opcao A - GitHub Actions (sem instalar nada)
1. Crie um repositorio no GitHub e envie TODO o conteudo desta pasta (use `git push`
   ou o upload do site; confira se `.github/workflows/build.yml` foi enviado. Se nao foi:
   "Add file > Create new file", nome `.github/workflows/build.yml`, cole o conteudo de `build-workflow.yml`).
2. Abra a aba **Actions**, escolha o ultimo "Build PvPBot" (ou clique em "Run workflow").
3. Ao terminar, baixe o artefato **PvPBot-jar**. Dentro do zip use `PvPBot-1.1.0.jar`
   (ignore o `-sources.jar`, se aparecer).
4. Se o build falhar, copie o log do passo "Compilar" e envie na conversa para corrigir.

## Opcao B - local
Requisitos: JDK 8 e Gradle 2.14.1.
    gradle setupDecompWorkspace build
O jar fica em `build/libs/PvPBot-1.1.0.jar`.

## Instalar
1. Instale o Forge 1.8.9 (11.15.1.2318).
2. Copie `PvPBot-1.1.0.jar` para `.minecraft/mods`.
3. Abra o perfil Forge 1.8.9.

## Comandos (singleplayer, sem precisar ativar cheats)
- `/pvpbot spawn [iniciante|intermediario|avancado|mestre|insano]`
- `/pvpbot difficulty <dificuldade>`
- `/pvpbot armor <none|leather|gold|chain|iron|diamond>`
- `/pvpbot sword <wood|stone|gold|iron|diamond>`
- `/pvpbot pause` (pausa/retoma)  |  `/pvpbot list`  |  `/pvpbot remove`

## Alcance
Constante unica `EntityPvPBot.REACH = 3.0` (alcance do jogador vanilla 1.8.9), usada por
todas as dificuldades. O ataque so acontece se o olhar do bot aponta para a hitbox do alvo,
a menos de 3.0 blocos do olho, sem bloco no caminho.
