package com.pvpbot;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Dificuldades do bot. NENHUM campo aqui controla alcance de ataque:
 * o alcance e a constante EntityPvPBot.REACH, igual para todas.
 * A dificuldade so altera mira, reacao, movimentacao, decisao e consistencia.
 */
public enum Difficulty {
    //            nome            reac turn  smooth noise ref cdMin cdMax atkCh strfCh sMin sMax sStr stopD  spbMin spbMax wtap hesit  chase
    INICIANTE("Iniciante",        8, 10F, 0.15F, 6.00F, 12, 7, 14, 0.55F, 0.20F, 10, 30, 0.50F, 2.3D, 20, 50, 0, 0.0100F, 0, 9, 0.000F),
    INTERMEDIARIO("Intermediário", 5, 18F, 0.25F, 3.50F, 10, 5, 9, 0.70F, 0.50F, 8, 22, 0.70F, 2.2D, 10, 25, 0, 0.0050F, 6, 6, 0.006F),
    AVANCADO("Avançado",          3, 30F, 0.40F, 2.00F, 8, 3, 6, 0.85F, 0.75F, 6, 16, 0.90F, 2.0D, 4, 10, 1, 0.0020F, 14, 4, 0.015F),
    MESTRE("Mestre",              2, 45F, 0.55F, 1.00F, 6, 2, 4, 0.93F, 0.90F, 5, 12, 1.00F, 1.9D, 2, 5, 1, 0.0005F, 20, 2, 0.025F),
    INSANO("Insano",              0, 70F, 0.75F, 0.35F, 4, 1, 3, 0.98F, 1.00F, 4, 9, 1.00F, 1.8D, 1, 3, 1, 0.0000F, 26, 1, 0.035F);

    public final String display;
    public final int reactionTicks;     // atraso de reacao (ticks) ao movimento do alvo
    public final float maxTurn;         // giro maximo de mira por tick (graus)
    public final float aimSmooth;       // fracao do erro de mira corrigida por tick
    public final float aimNoise;        // desvio aleatorio de mira (graus)
    public final int noiseRefresh;      // a cada quantos ticks o desvio muda
    public final int cdMin, cdMax;      // intervalo entre cliques (ticks)
    public final float attackChance;    // chance de clicar quando o clique esta liberado
    public final float strafeChance;    // chance de strafar a cada troca de direcao
    public final int strafeMin, strafeMax; // duracao de cada direcao de strafe (ticks)
    public final float strafeStrength;  // intensidade do strafe
    public final double stopDistance;   // distancia horizontal em que para de avancar
    public final int sprintBlockMin, sprintBlockMax; // ticks sem sprint apos acertar (sprint reset)
    public final int wtapPause;         // ticks soltando o W apos acertar
    public final float hesitateChance;  // chance por tick de hesitar
    public final int comboChaseTicks;   // ticks colado no alvo apos acertar (combo)
    public final int recoveryTicks;     // ticks sem agir depois de ser acertado
    public final float jumpChance;      // chance por tick de pular perto do alvo (critico)

    Difficulty(String display, int reactionTicks, float maxTurn, float aimSmooth, float aimNoise,
               int noiseRefresh, int cdMin, int cdMax, float attackChance, float strafeChance,
               int strafeMin, int strafeMax, float strafeStrength, double stopDistance,
               int sprintBlockMin, int sprintBlockMax, int wtapPause, float hesitateChance,
               int comboChaseTicks, int recoveryTicks, float jumpChance) {
        this.display = display;
        this.reactionTicks = reactionTicks;
        this.maxTurn = maxTurn;
        this.aimSmooth = aimSmooth;
        this.aimNoise = aimNoise;
        this.noiseRefresh = noiseRefresh;
        this.cdMin = cdMin;
        this.cdMax = cdMax;
        this.attackChance = attackChance;
        this.strafeChance = strafeChance;
        this.strafeMin = strafeMin;
        this.strafeMax = strafeMax;
        this.strafeStrength = strafeStrength;
        this.stopDistance = stopDistance;
        this.sprintBlockMin = sprintBlockMin;
        this.sprintBlockMax = sprintBlockMax;
        this.wtapPause = wtapPause;
        this.hesitateChance = hesitateChance;
        this.comboChaseTicks = comboChaseTicks;
        this.recoveryTicks = recoveryTicks;
        this.jumpChance = jumpChance;
    }

    public static Difficulty parse(String raw) {
        if (raw == null) return null;
        String s = Normalizer.normalize(raw, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT).trim();
        switch (s) {
            case "1": case "iniciante": case "beginner": case "facil": case "easy": return INICIANTE;
            case "2": case "intermediario": case "intermediate": case "medio": return INTERMEDIARIO;
            case "3": case "avancado": case "advanced": case "dificil": return AVANCADO;
            case "4": case "mestre": case "master": return MESTRE;
            case "5": case "insano": case "insane": return INSANO;
            default: return null;
        }
    }
}
