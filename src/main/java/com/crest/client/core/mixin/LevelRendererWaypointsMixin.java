package com.crest.client.core.mixin;

import com.crest.client.core.CrestModules;
import com.crest.client.core.DynamicLightsModule;
import com.crest.client.core.WaypointsModule;
import org.joml.Matrix4fc;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Draws waypoint markers in the world. WorldRenderEvents does not exist in this
 * MC version, so this hooks LevelRenderer.renderLevel like HitboxModule does.
 * The projection matrix is explicitly restored so markers track the world
 * instead of floating on the (ortho) HUD projection.
 */
@Mixin(LevelRenderer.class)
public class LevelRendererWaypointsMixin {

    @Inject(method = "renderLevel", at = @At("RETURN"))
    private void crest$renderWaypoints(
        com.mojang.blaze3d.resource.GraphicsResourceAllocator allocator,
        net.minecraft.client.DeltaTracker delta,
        boolean bl,
        CameraRenderState cameraRenderState,
        Matrix4fc projection,
        com.mojang.blaze3d.buffers.GpuBufferSlice slice,
        org.joml.Vector4f vec,
        boolean bl2,
        net.minecraft.client.renderer.chunk.ChunkSectionsToRender chunks,
        CallbackInfo ci) {

        if (!CrestModules.isEnabled("waypoints")) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        WaypointsModule mod = (WaypointsModule) CrestModules.get("waypoints");
        if (mod == null) return;

        PoseStack ps = new PoseStack();
        ps.mulPose(cameraRenderState.viewRotationMatrix);
        ps.translate(-cameraRenderState.pos.x, -cameraRenderState.pos.y, -cameraRenderState.pos.z);

        MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();
        mod.renderWorld(ps, buffer, cameraRenderState);

        if (CrestModules.isEnabled("dynamic_lights")) {
            DynamicLightsModule dl = (DynamicLightsModule) CrestModules.get("dynamic_lights");
            if (dl != null) dl.renderWorld(ps, buffer, cameraRenderState);
        }
    }
}
