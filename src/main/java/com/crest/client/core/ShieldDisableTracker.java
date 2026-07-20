package com.crest.client.core;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Minecraft only syncs the LOCAL player's item cooldowns to the client, so the
 * "shield disabled" (axe-hit) state of OTHER players is not visible via
 * {@code Player.getCooldowns()}. This tracker records when the local player
 * axe-hits a blocking player (detected through AttackEntityCallback) and reports
 * that player as cooling down for the shield-disable duration, so their held
 * shield can be tinted red like the local player's.
 */
public final class ShieldDisableTracker {
    private static final long DISABLE_TICKS = 100L; // 5s, matches vanilla shield disable
    private static final Map<UUID, Long> EXPIRY = new HashMap<>();

    public static void markDisabled(Player player) {
        if (player == null) return;
        long now = gameTime();
        EXPIRY.put(player.getUUID(), now + DISABLE_TICKS);
    }

    public static boolean isDisabled(Player player) {
        if (player == null) return false;
        Long expiry = EXPIRY.get(player.getUUID());
        if (expiry == null) return false;
        if (gameTime() >= expiry) {
            EXPIRY.remove(player.getUUID());
            return false;
        }
        return true;
    }

    private static long gameTime() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level != null ? mc.level.getGameTime() : 0L;
    }
}
