package com.crest.client.core.mixin;

import com.crest.client.core.CrestModules;
import com.crest.client.core.NameTagsModule;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public class NameTagsAlwaysVisibleMixin {
    @Inject(method = "shouldShowName", at = @At("RETURN"), cancellable = true)
    private void crest$alwaysShowName(LivingEntity entity, double distance, CallbackInfoReturnable<Boolean> cir) {
        if (!CrestModules.isEnabled("name_tags")) return;
        if (NameTagsModule.isAlwaysVisible() && entity.hasCustomName()) {
            cir.setReturnValue(true);
        }
    }
}
