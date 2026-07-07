package com.crest.client.core.mixin;

import com.crest.client.core.Recorder;
import com.mojang.blaze3d.systems.RenderSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderSystem.class)
public class RenderSystemMixin {
    @Inject(method = "flipFrame", at = @At("HEAD"))
    private static void crest$onFlipFrame(CallbackInfo ci) {
        Recorder.onFrame();
    }
}
