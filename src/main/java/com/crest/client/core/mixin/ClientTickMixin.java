package com.crest.client.core.mixin;

import com.crest.client.core.StateRecorder;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class ClientTickMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void crest$onTick(CallbackInfo ci) {
        if (!StateRecorder.isActive()) return;
        StateRecorder.onTick();
    }
}