package com.crest.client.core.mixin;

import com.crest.client.core.CrestModules;
import com.crest.client.core.NameTagsModule;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public class NameTagsMixin {
    @Inject(method = "extractRenderState", at = @At("RETURN"))
    private void crest$modifyNameTagColor(Entity entity, EntityRenderState state, float partialTick, CallbackInfo ci) {
        if (!CrestModules.isEnabled("name_tags")) return;
        if (state.nameTag != null) {
            state.nameTag = NameTagsModule.applyColor(state.nameTag);
        }
    }

    @Inject(
        method = "submitNameDisplay(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;I)V",
        at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V", shift = At.Shift.AFTER)
    )
    private void crest$modifyNameTagScale(EntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraRenderState, int i, CallbackInfo ci) {
        if (!CrestModules.isEnabled("name_tags")) return;
        float scale = NameTagsModule.getScale();
        if (scale != 1.0f) {
            poseStack.scale(scale, scale, scale);
        }
    }
}
