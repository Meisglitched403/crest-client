package com.crest.client.core.mixin;

import com.crest.client.core.CrestModules;
import com.crest.client.core.ZoomModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {
    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void crest$cancelHotbarScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (ZoomModule.initialized && CrestModules.isEnabled("zoom") && ZoomModule.isZooming()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen == null && mc.getOverlay() == null && mc.player != null) {
                ci.cancel();
            }
        }
    }
}
