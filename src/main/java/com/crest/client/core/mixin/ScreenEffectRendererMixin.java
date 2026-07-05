package com.crest.client.core.mixin;

import com.crest.client.core.CrestModules;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenEffectRenderer.class)
public class ScreenEffectRendererMixin {
    @Inject(method = "renderFire", at = @At("HEAD"))
    private static void onRenderFire(PoseStack poseStack, MultiBufferSource bufferSource, TextureAtlasSprite fireSprite, CallbackInfo ci) {
        if (!CrestModules.isEnabled("low_fire")) return;
        poseStack.pushPose();
        poseStack.translate(0.0, 0.6, 0.0);
        poseStack.scale(0.6F, 0.15F, 1.0F);
    }

    @Inject(method = "renderFire", at = @At("RETURN"))
    private static void onRenderFireEnd(PoseStack poseStack, MultiBufferSource bufferSource, TextureAtlasSprite fireSprite, CallbackInfo ci) {
        if (!CrestModules.isEnabled("low_fire")) return;
        poseStack.popPose();
    }
}
