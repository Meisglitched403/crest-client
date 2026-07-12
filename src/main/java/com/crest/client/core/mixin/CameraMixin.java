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

        if (CrestModules.isEnabled("dynamic_fov") && !ZoomModule.isActive()) {
            fov = Minecraft.getInstance().options.fov().get().floatValue();
        }

        if (ZoomModule.isActive() && CrestModules.isEnabled("zoom") && ZoomModule.isInitialized()) {
            float interp = (float) (ZoomModule.prevFov + (ZoomModule.currentFov - ZoomModule.prevFov) * partialTicks);
            fov = interp;
        }

        cir.setReturnValue(fov);
    }
}
