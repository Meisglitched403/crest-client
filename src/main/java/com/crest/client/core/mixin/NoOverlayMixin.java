package com.crest.client.core.mixin;

import com.crest.client.core.CrestModules;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenEffectRenderer.class)
public class NoOverlayMixin {
    @Inject(method = "renderWater", at = @At("HEAD"), cancellable = true)
    private static void crest$cancelWater(Minecraft minecraft, PoseStack poseStack, MultiBufferSource bufferSource, CallbackInfo ci) {
        if (CrestModules.isEnabled("no_overlay")) {
            ci.cancel();
        }
    }
}
