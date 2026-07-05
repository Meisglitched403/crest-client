package com.crest.client.core.mixin;

import com.crest.client.core.CrestModules;
import com.crest.client.core.EntityCullingModule;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {
    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private <E extends Entity> void onShouldRender(E entity, Frustum frustum, double camX, double camY, double camZ, CallbackInfoReturnable<Boolean> cir) {
        if (!CrestModules.isEnabled("entity_culling")) return;

        double maxDist = EntityCullingModule.getMaxDistance();
        double dx = entity.getX() - camX;
        double dy = entity.getY() - camY;
        double dz = entity.getZ() - camZ;

        if (dx * dx + dy * dy + dz * dz > maxDist * maxDist) {
            cir.setReturnValue(false);
        }
    }
}
