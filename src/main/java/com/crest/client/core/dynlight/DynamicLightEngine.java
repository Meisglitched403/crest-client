package com.crest.client.core.dynlight;

import com.crest.client.core.CrestModules;
import com.crest.client.core.DynamicLightsModule;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Client-side dynamic lighting bookkeeping. Maintains a map of extra block-light
 * levels used by the getRawBrightness hook (brightens held items / the player's
 * own view on any server). The visible world glow is drawn by DynamicLightsModule
 * as a Sodium-independent overlay, so this engine does not re-mesh chunks.
 *
 * Visual only: never mutates stored light data, so it does not affect mob
 * spawning or server lighting.
 */
public final class DynamicLightEngine {
    private static final DynamicLightEngine INSTANCE = new DynamicLightEngine();
    public static DynamicLightEngine get() { return INSTANCE; }

    private final Long2IntOpenHashMap dynamic = new Long2IntOpenHashMap();
    private int tick = 0;

    public int getDynamicLight(BlockPos pos) {
        return dynamic.get(pos.asLong());
    }

    public int getDynamicLight(long key) {
        return dynamic.get(key);
    }

    public void tick(Minecraft mc) {
        tick++;
        if (tick % 3 != 0) return;

        DynamicLightsModule mod = (DynamicLightsModule) CrestModules.get("dynamic_lights");
        if (mod == null || !CrestModules.isEnabled("dynamic_lights") || mc.level == null || mc.player == null) {
            if (!dynamic.isEmpty()) dynamic.clear();
            return;
        }

        int range = mod.getRange();
        double r2 = (double) range * range;

        Long2IntOpenHashMap next = new Long2IntOpenHashMap();

        collectHand(mc.player, next, mod);
        if (mod.getOtherPlayers()) {
            for (Player p : mc.level.players()) {
                if (p == mc.player) continue;
                if (mc.player.distanceToSqr(p) > r2) continue;
                collectHand(p, next, mod);
            }
        }
        if (mod.getMobLights()) {
            for (Entity e : mc.level.getEntities(mc.player, mc.player.getBoundingBox().inflate(range), x -> true)) {
                if (mc.player.distanceToSqr(e) > r2) continue;
                int lvl = LightCatalog.entityLevel(e.getType());
                if (lvl == 0 && e instanceof ItemEntity ie) {
                    lvl = LightCatalog.itemLevel(ie.getItem(), mod.getEnchantedGlow());
                }
                if (lvl == 0 && e.isOnFire() && !(e instanceof Player)) lvl = 15;
                if (lvl > 0) addLight(next, e.blockPosition(), effectiveLevel(lvl, mod));
            }
        }

        dynamic.clear();
        dynamic.putAll(next);
    }

    private static void collectHand(Player p, Long2IntOpenHashMap next, DynamicLightsModule mod) {
        BlockPos base = p.blockPosition().above(1);
        int m = effectiveLevel(LightCatalog.itemLevel(p.getMainHandItem(), mod.getEnchantedGlow()), mod);
        if (m > 0) addLight(next, base, m);
        int o = effectiveLevel(LightCatalog.itemLevel(p.getOffhandItem(), mod.getEnchantedGlow()), mod);
        if (o > 0) addLight(next, base, o);
    }

    private static int effectiveLevel(int level, DynamicLightsModule mod) {
        if (level <= 0) return 0;
        int v = Math.round(level * (0.6f + 0.4f * mod.getIntensity() / 10f));
        return Math.min(15, v);
    }

    /** Adds a square-falloff light volume (matches Minecraft block-light decay). */
    private static void addLight(Long2IntOpenHashMap map, BlockPos pos, int L) {
        if (L <= 0) return;
        int bx = pos.getX(), by = pos.getY(), bz = pos.getZ();
        for (int dx = -L; dx <= L; dx++) {
            for (int dy = -L; dy <= L; dy++) {
                for (int dz = -L; dz <= L; dz++) {
                    int dist = Math.max(Math.max(Math.abs(dx), Math.abs(dy)), Math.abs(dz));
                    if (dist > L) continue;
                    int l = L - dist;
                    if (l <= 0) continue;
                    long k = BlockPos.asLong(bx + dx, by + dy, bz + dz);
                    int cur = map.get(k);
                    if (l > cur) map.put(k, l);
                }
            }
        }
    }
}
