package com.crest.client.core.mixin;

import com.crest.client.core.CrestModules;
import com.crest.client.core.ShieldStatusModule;
import com.crest.client.core.accessor.ShieldTintAccessor;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AvatarRenderer.class)
public class AvatarRendererMixin {

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V", at = @At("TAIL"))
    private void crest$extractShieldState(Avatar entity, AvatarRenderState state, float tickDelta, CallbackInfo ci) {
        if (!CrestModules.isEnabled("shield_status")) return;

        ShieldTintAccessor accessor = (ShieldTintAccessor) state;
        if (entity instanceof Player player && player.getCooldowns().isOnCooldown(Items.SHIELD.getDefaultInstance())) {
            accessor.crest$setShieldTintColor(ShieldStatusModule.getCooldownColor());
        } else {
            accessor.crest$setShieldTintColor(ShieldStatusModule.getBlockingColor());
        }
    }
}
