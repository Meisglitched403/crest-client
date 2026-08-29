package com.crest.client.core.mixin;

import com.crest.client.core.dynlight.DynamicLightEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Injects dynamic light into getRawBrightness, which drives the player's own
 * lightmap (so a held torch brightens the screen around you).
 */
@Mixin(LevelLightEngine.class)
public class DynamicLightRawMixin {

    @Inject(method = "getRawBrightness(Lnet/minecraft/core/BlockPos;I)I",
            at = @At("RETURN"), cancellable = true)
    private void crest$rawBrightness(BlockPos pos, int i, CallbackInfoReturnable<Integer> ci) {
        int dyn = DynamicLightEngine.get().getDynamicLight(pos);
        if (dyn > ci.getReturnValue()) ci.setReturnValue(dyn);
    }
}
