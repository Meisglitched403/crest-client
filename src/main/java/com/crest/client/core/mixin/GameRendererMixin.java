package com.crest.client.core.mixin;

import com.crest.client.core.CrestModules;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true)
    private void onBobHurt(CameraRenderState cameraState, PoseStack poseStack, CallbackInfo ci) {
        if (CrestModules.isEnabled("no_hurt_cam")) {
            ci.cancel();
        }
    }
}
