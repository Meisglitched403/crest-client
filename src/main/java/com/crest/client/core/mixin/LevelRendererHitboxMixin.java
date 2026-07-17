package com.crest.client.core.mixin;

import com.crest.client.core.HitboxModule;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class LevelRendererHitboxMixin {

    @Inject(method = "renderLevel", at = @At("RETURN"))
    private void crest$drawHitboxes(
        com.mojang.blaze3d.resource.GraphicsResourceAllocator allocator,
        net.minecraft.client.DeltaTracker delta,
        boolean bl,
        CameraRenderState cameraRenderState,
        org.joml.Matrix4fc projection,
        com.mojang.blaze3d.buffers.GpuBufferSlice slice,
        org.joml.Vector4f vec,
        boolean bl2,
        net.minecraft.client.renderer.chunk.ChunkSectionsToRender chunks,
        CallbackInfo ci) {

        com.crest.client.core.HitboxModule mod =
            (com.crest.client.core.HitboxModule) com.crest.client.core.CrestModules.get("hitbox");
        if (mod == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        PoseStack ps = new PoseStack();
        ps.mulPose(cameraRenderState.viewRotationMatrix);
        ps.translate(-cameraRenderState.pos.x, -cameraRenderState.pos.y, -cameraRenderState.pos.z);

        MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();
        mod.drawWorldBoxes(ps, buffer, cameraRenderState);
    }
}
