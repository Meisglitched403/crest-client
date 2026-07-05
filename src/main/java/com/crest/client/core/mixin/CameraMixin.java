package com.crest.client.core.mixin;

import com.crest.client.core.CrestModules;
import com.crest.client.core.ZoomModule;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public class CameraMixin {
    @Inject(method = "calculateFov", at = @At("RETURN"), cancellable = true)
    private void crest$modifyFov(float partialTicks, CallbackInfoReturnable<Float> cir) {
        float fov = cir.getReturnValue();

        if (CrestModules.isEnabled("dynamic_fov") && !ZoomModule.isZooming()) {
            fov = Minecraft.getInstance().options.fov().get().floatValue();
        }

        if (!CrestModules.isEnabled("zoom") || !ZoomModule.initialized) {
            cir.setReturnValue(fov);
            return;
        }

        double zfov = ZoomModule.currentFov;
        if (Math.abs(zfov - ZoomModule.targetFov) > 0.01) {
            zfov += (ZoomModule.targetFov - zfov) * ZoomModule.SMOOTH_SPEED;
            ZoomModule.currentFov = zfov;
        } else if (zfov != ZoomModule.targetFov) {
            ZoomModule.currentFov = ZoomModule.targetFov;
        }
        cir.setReturnValue((float) zfov);
    }
}
