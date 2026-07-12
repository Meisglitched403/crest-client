package com.crest.client.core.mixin;

import com.crest.client.core.FreelookModule;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public class FreelookCameraMixin {
    @Shadow private Entity entity;
    @Unique private float savedYaw;
    @Unique private float savedPitch;

    @Inject(method = "update", at = @At("HEAD"))
    private void onUpdateHead(CallbackInfo ci) {
        if (!FreelookModule.isActive() || entity == null) return;
        savedYaw = entity.getYRot();
        savedPitch = entity.getXRot();
        entity.setYRot(FreelookModule.getYaw());
        entity.setXRot(FreelookModule.getPitch());
    }

    @Inject(method = "update", at = @At("TAIL"))
    private void onUpdateTail(CallbackInfo ci) {
        if (!FreelookModule.isActive() || entity == null) return;
        entity.setYRot(savedYaw);
        entity.setXRot(savedPitch);
    }
}
