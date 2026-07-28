package com.crest.client.core.mixin;

import com.crest.client.core.CrestBrandManager;
import com.crest.client.core.CrestModules;
import com.crest.client.core.CrestNametagModule;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.UUID;

@Mixin(EntityRenderer.class)
public class CrestNametagMixin {
    private static final String LOGO_CHAR = "\uE000";

    @ModifyReturnValue(method = "getNameTag", at = @At("RETURN"))
    private Component crest$prependLogoToNameTag(Component original, Entity entity) {
        if (original == null) return null;
        if (!CrestModules.isEnabled("crest_nametag")) return original;
        if (!CrestBrandManager.isInitialized()) return original;
        if (!(entity instanceof Player)) return original;

        UUID uuid = entity.getUUID();
        if (CrestBrandManager.isCrestUser(uuid)) {
            if (CrestNametagModule.isLogoPositionLeft()) {
                return Component.literal(LOGO_CHAR + " ").append(original);
            } else {
                return Component.literal("").append(original).append(Component.literal(" " + LOGO_CHAR));
            }
        }
        return original;
    }
}
