package com.crest.client.core;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Vanilla replacement for WalksyLib's WalksyLibShieldStateManager. Detects the
 * shield state used by ShieldStatusModule without external dependencies.
 */
public final class ShieldStateUtil {

    private static final ItemStack SHIELD = Items.SHIELD.getDefaultInstance();

    public static boolean isCoolingDown(Player player) {
        if (player.getCooldowns().isOnCooldown(SHIELD)) return true;
        // Other players' cooldowns aren't synced to the client; use the
        // axe-disable tracker so their disabled shield still tints red.
        return ShieldDisableTracker.isDisabled(player);
    }

    public static boolean isUsingShield(Player player) {
        return player.isBlocking();
    }

    public static boolean isHoldingUsableShield(Player player) {
        return player.getMainHandItem().is(Items.SHIELD) || player.getOffhandItem().is(Items.SHIELD);
    }

    /** Holding a shield and using it, but not yet blocking and not on cooldown. */
    public static boolean isShieldRising(Player player) {
        return isHoldingUsableShield(player)
                && player.isUsingItem()
                && !isUsingShield(player)
                && !isCoolingDown(player);
    }

    public static float getCooldownProgress(Player player) {
        return player.getCooldowns().getCooldownPercent(SHIELD, 0.0f);
    }

    private static boolean holdsShield(LivingEntity e, ItemStack stack) {
        return stack.is(Items.SHIELD);
    }
}
