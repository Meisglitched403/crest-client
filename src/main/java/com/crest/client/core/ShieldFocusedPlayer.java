package com.crest.client.core;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.player.Player;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Tracks which player owns a given held-item {@link ItemStackRenderState}, so the
 * tint submitter can resolve the correct colour/texture per shield instance.
 *
 * The map is keyed by the render-state instance (not a global) because the
 * {@code ItemStackRenderState.submit} bake runs during entity render-state
 * extraction — before {@code PlayerItemInHandLayer.submitArmWithItem} (which sets
 * the owning player) runs in the later draw phase. Keying by instance lets the
 * submit mixin read the correct player via {@code get(ItemStackRenderState)}
 * instead of a stale global.
 *
 * GUI shields always belong to the local player ({@link #setLocal()}).
 */
public final class ShieldFocusedPlayer {
    private static final Map<ItemStackRenderState, Player> OWNERS = new IdentityHashMap<>();
    private static Player localFallback;

    /** Associate a held-item render state with the player that owns it. */
    public static void set(ItemStackRenderState state, Player player) {
        if (state != null && player != null) OWNERS.put(state, player);
    }

    /** Resolve the player that owns the given render state, or the local player. */
    public static Player get(ItemStackRenderState state) {
        Player p = state != null ? OWNERS.get(state) : null;
        if (p != null) return p;
        return localFallback != null ? localFallback : Minecraft.getInstance().player;
    }

    /** GUI shields always belong to the local player. */
    public static void setLocal() {
        localFallback = Minecraft.getInstance().player;
    }
}
