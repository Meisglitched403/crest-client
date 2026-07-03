package com.crest.client.core.mixin;

import com.crest.client.core.CrestModules;
import com.crest.client.core.ZoomModule;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void crest$modifyFov(Camera camera, float tickDelta, boolean changingFov, CallbackInfoReturnable<Double> cir) {
        if (!CrestModules.isEnabled("zoom") || !ZoomModule.initialized) return;

        double fov = ZoomModule.currentFov;
        if (Math.abs(fov - ZoomModule.targetFov) > 0.01) {
            fov += (ZoomModule.targetFov - fov) * ZoomModule.SMOOTH_SPEED;
            ZoomModule.currentFov = fov;
        } else if (fov != ZoomModule.targetFov) {
            ZoomModule.currentFov = ZoomModule.targetFov;
        }
        cir.setReturnValue(fov);
    }
}
