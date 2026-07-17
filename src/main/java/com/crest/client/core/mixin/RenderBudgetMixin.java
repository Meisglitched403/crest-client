package com.crest.client.core.mixin;

import com.crest.client.core.FrameBudget;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class RenderBudgetMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void crest$frameStart(DeltaTracker deltaTracker, boolean advanceGameTime, CallbackInfo ci) {
        // ponytail: publish camera position for distant-block LOD tiers (read by
        // Sodium's chunk-build worker threads via FrameBudget volatile fields).
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            FrameBudget.setCamera(mc.player.getX(), mc.player.getY(), mc.player.getZ());
        }
        FrameBudget.markFrameStart(System.nanoTime());
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void crest$frameEnd(DeltaTracker deltaTracker, boolean advanceGameTime, CallbackInfo ci) {
        FrameBudget.markFrameEnd(System.nanoTime());
    }
}
