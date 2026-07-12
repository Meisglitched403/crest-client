package com.crest.client.core.mixin;

import com.crest.client.core.CameraOverriddenEntity;
import com.crest.client.core.FreelookModule;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public class FreelookMixin implements CameraOverriddenEntity {
    @Unique private float freelook$cameraPitch;
    @Unique private float freelook$cameraYaw;

    @Override
    public float freelook$getCameraPitch() { return freelook$cameraPitch; }
    @Override
    public void freelook$setCameraPitch(float pitch) { freelook$cameraPitch = pitch; }
    @Override
    public float freelook$getCameraYaw() { return freelook$cameraYaw; }
    @Override
    public void freelook$setCameraYaw(float yaw) { freelook$cameraYaw = yaw; }

    @Inject(method = "turn", at = @At("HEAD"), cancellable = true)
    private void onTurn(double yaw, double pitch, CallbackInfo ci) {
        if (!FreelookModule.isActive()) return;
        Entity self = (Entity) (Object) this;
        if (self != Minecraft.getInstance().player) return;
        FreelookModule.addAngles((float) yaw, (float) pitch);
        ci.cancel();
    }
}
