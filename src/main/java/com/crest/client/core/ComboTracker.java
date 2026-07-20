package com.crest.client.core;

import net.minecraft.client.Minecraft;

/**
 * ponytail: Tracks the current and max hit-combo. Incremented by MinecraftAttackMixin
 * when the player lands an attack on an entity; decays to 0 if no hit lands within
 * {@link #TIMEOUT_TICKS}.
 */
public final class ComboTracker {
    public static final int TIMEOUT_TICKS = 60; // 3s at 20tps

    private static int combo;
    private static int maxCombo;
    private static int sinceHit = TIMEOUT_TICKS;

    public static void onHit() {
        combo++;
        if (combo > maxCombo) maxCombo = combo;
        sinceHit = 0;
    }

    public static void tick() {
        sinceHit++;
        if (sinceHit >= TIMEOUT_TICKS) {
            combo = 0;
        }
    }

    public static int combo() { return combo; }
    public static int maxCombo() { return maxCombo; }

    public static void resetMax() { maxCombo = 0; }
}
