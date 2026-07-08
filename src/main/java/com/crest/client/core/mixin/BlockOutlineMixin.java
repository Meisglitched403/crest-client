package com.crest.client.core.mixin;

import com.crest.client.core.BlockOutlineModule;
import com.crest.client.core.CrestModules;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.state.level.BlockOutlineRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class BlockOutlineMixin {

    @Inject(
        method = "renderBlockOutline",
        at = @At("HEAD"),
        cancellable = true
    )
    private void crest$customOutline(
        MultiBufferSource.BufferSource buffer,
        PoseStack poseStack,
        boolean translucent,
        LevelRenderState renderState,
        CallbackInfo ci
    ) {
        if (!CrestModules.isEnabled("block_outline")) return;

        BlockOutlineRenderState outline = renderState.blockOutlineRenderState;
        if (outline == null) return;
        if (outline.isTranslucent() != translucent) return;

        ci.cancel();

        BlockPos pos = outline.pos();
        int color = BlockOutlineModule.getColor();
        float width = BlockOutlineModule.getWidth();
        int mode = BlockOutlineModule.getMode();
        int alpha = (color >> 24) & 0xFF;
        int rgb = color & 0x00FFFFFF;
        int edgeColor = (alpha << 24) | rgb;

        double camX = renderState.cameraRenderState.pos.x;
        double camY = renderState.cameraRenderState.pos.y;
        double camZ = renderState.cameraRenderState.pos.z;

        poseStack.pushPose();

        VoxelShape shape = outline.shape();

        if (mode == 1 || mode == 2) {
            // Full / Both: filled translucent box.
            AABB aabb = shape.bounds().move(pos);
            VertexConsumer fill = buffer.getBuffer(net.minecraft.client.renderer.rendertype.RenderTypes.debugFilledBox());
            int fillAlpha = Math.max(40, alpha);
            renderFilledBox(poseStack.last(), fill,
                (float) (aabb.minX - camX), (float) (aabb.minY - camY), (float) (aabb.minZ - camZ),
                (float) (aabb.maxX - camX), (float) (aabb.maxY - camY), (float) (aabb.maxZ - camZ),
                (rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, fillAlpha);
        }

        if (mode == 0 || mode == 2) {
            // Outline / Both: edges via ShapeRenderer (block-minus-camera offset,
            // matching vanilla renderHitOutline's coordinate convention).
            VertexConsumer lines = buffer.getBuffer(net.minecraft.client.renderer.rendertype.RenderTypes.lines());
            ShapeRenderer.renderShape(poseStack, lines, shape,
                pos.getX() - camX, pos.getY() - camY, pos.getZ() - camZ, edgeColor, width);
        }

        poseStack.popPose();
        buffer.endBatch(net.minecraft.client.renderer.rendertype.RenderTypes.lines());
        if (mode == 1 || mode == 2) {
            buffer.endBatch(net.minecraft.client.renderer.rendertype.RenderTypes.debugFilledBox());
        }
    }

    private static void renderFilledBox(com.mojang.blaze3d.vertex.PoseStack.Pose pose, VertexConsumer consumer,
                                        float x1, float y1, float z1, float x2, float y2, float z2,
                                        int r, int g, int b, int a) {
        // Bottom face (y1)
        face(pose, consumer, x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2, 0, -1, 0, r, g, b, a);
        // Top face (y2)
        face(pose, consumer, x1, y2, z1, x1, y2, z2, x2, y2, z2, x2, y2, z1, 0, 1, 0, r, g, b, a);
        // North face (z1)
        face(pose, consumer, x1, y1, z1, x1, y2, z1, x2, y2, z1, x2, y1, z1, 0, 0, -1, r, g, b, a);
        // South face (z2)
        face(pose, consumer, x2, y1, z2, x2, y2, z2, x1, y2, z2, x1, y1, z2, 0, 0, 1, r, g, b, a);
        // West face (x1)
        face(pose, consumer, x1, y1, z2, x1, y2, z2, x1, y2, z1, x1, y1, z1, -1, 0, 0, r, g, b, a);
        // East face (x2)
        face(pose, consumer, x2, y1, z1, x2, y2, z1, x2, y2, z2, x2, y1, z2, 1, 0, 0, r, g, b, a);
    }

    private static void face(com.mojang.blaze3d.vertex.PoseStack.Pose pose, VertexConsumer c,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz,
                             float nx, float ny, float nz, int r, int g, int b, int a) {
        vert(pose, c, ax, ay, az, nx, ny, nz, r, g, b, a);
        vert(pose, c, bx, by, bz, nx, ny, nz, r, g, b, a);
        vert(pose, c, cx, cy, cz, nx, ny, nz, r, g, b, a);
        vert(pose, c, cx, cy, cz, nx, ny, nz, r, g, b, a);
        vert(pose, c, dx, dy, dz, nx, ny, nz, r, g, b, a);
        vert(pose, c, ax, ay, az, nx, ny, nz, r, g, b, a);
    }

    private static void vert(com.mojang.blaze3d.vertex.PoseStack.Pose pose, VertexConsumer c,
                             float x, float y, float z, float nx, float ny, float nz,
                             int r, int g, int b, int a) {
        c.addVertex(pose, x, y, z).setColor(r, g, b, a).setNormal(pose, nx, ny, nz);
    }
}
