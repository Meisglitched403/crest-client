package com.crest.client.core.mixin;

import com.crest.client.core.CrestModules;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.tags.FluidTags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FogRenderer.class)
public class FogRendererMixin {
    @Inject(method = "setupFog", at = @At("RETURN"))
    private static void onSetupFog(Camera camera, int renderDistance, DeltaTracker deltaTracker, float unknownFloat, ClientLevel level, CallbackInfoReturnable<FogData> cir) {
        if (!CrestModules.isEnabled("no_fog")) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (!mc.player.isEyeInFluid(FluidTags.WATER)) return;

        FogData fog = cir.getReturnValue();
        fog.renderDistanceEnd = Float.MAX_VALUE;
        fog.environmentalEnd = Float.MAX_VALUE;
        fog.skyEnd = Float.MAX_VALUE;
        fog.cloudEnd = Float.MAX_VALUE;
        fog.color.set(fog.color.x(), fog.color.y(), fog.color.z(), 0.0F);
    }
}
