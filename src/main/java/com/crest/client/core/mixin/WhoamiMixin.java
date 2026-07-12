package com.crest.client.core.mixin;

import com.crest.client.core.CrestModules;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public class WhoamiMixin {
    @Inject(method = "shouldShowName(Lnet/minecraft/world/entity/LivingEntity;D)Z", at = @At("RETURN"), cancellable = true)
    private void crest$showOwnName(LivingEntity entity, double distance, CallbackInfoReturnable<Boolean> cir) {
        if (!CrestModules.isEnabled("whoami")) return;
        if (entity == Minecraft.getInstance().player) {
            cir.setReturnValue(true);
        }
    }
}
