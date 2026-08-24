package com.crest.client.core;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;

import java.util.List;

/**
 * Renders waypoint markers in the world: a colored outline box at the waypoint,
 * a full-height beam, and a billboarded name label. Called from the
 * LevelRenderer.renderLevel mixin (WorldRenderEvents does not exist in this MC).
 */
public final class WaypointWorldRenderer {
    private WaypointWorldRenderer() {}

    private static final ByteBufferBuilder BUFFER = new ByteBufferBuilder(8192);
    private static final MultiBufferSource.BufferSource SOURCE = MultiBufferSource.immediate(BUFFER);

    public static void render(PoseStack ps, MultiBufferSource.BufferSource buffer, CameraRenderState cam) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        if (!WaypointManager.isEnabled()) return;

        List<Waypoint> waypoints = WaypointManager.listForCurrent(mc);
        if (waypoints.isEmpty()) return;

        double camX = cam.pos.x;
        double camY = cam.pos.y;
        double camZ = cam.pos.z;

        int maxDist = WaypointsModule.getMaxDistance();
        int maxDistSq = maxDist * maxDist;

        int minY = mc.level.getMinY();
        int maxY = mc.level.getMaxY();

        // ---------- PASS 1: marker boxes ----------
        VertexConsumer lines = SOURCE.getBuffer(RenderTypes.lines());
        for (Waypoint wp : waypoints) {
            double wx = wp.x + 0.5;
            double wy = wp.y;
            double wz = wp.z + 0.5;

            double dx = wx - camX;
            double dy = wy - camY;
            double dz = wz - camZ;
            if (dx * dx + dy * dy + dz * dz > maxDistSq) continue;

            ShapeRenderer.renderShape(ps, lines,
                Shapes.create(new AABB(wx - 0.30, wy - 0.30, wz - 0.30, wx + 0.30, wy + 0.30, wz + 0.30)),
                0, 0, 0, 0xFF000000 | (wp.color & 0xFFFFFF), 1.0f);
        }
        SOURCE.endBatch(RenderTypes.lines());

        // ---------- PASS 2: beams ----------
        if (WaypointsModule.showBeams()) {
            VertexConsumer beam = SOURCE.getBuffer(RenderTypes.debugFilledBox());
            for (Waypoint wp : waypoints) {
                double wx = wp.x + 0.5;
                double wy = wp.y;
                double wz = wp.z + 0.5;

                double dx = wx - camX;
                double dy = wy - camY;
                double dz = wz - camZ;
                if (dx * dx + dy * dy + dz * dz > maxDistSq) continue;

                ShapeRenderer.renderShape(ps, beam,
                    Shapes.create(new AABB(wx - 0.06, minY, wz - 0.06, wx + 0.06, maxY, wz + 0.06)),
                    0, 0, 0, 0xFF000000 | (wp.color & 0xFFFFFF), 1.0f);
            }
            SOURCE.endBatch(RenderTypes.debugFilledBox());
        }

        // ---------- PASS 3: labels ----------
        if (!WaypointsModule.showLabels()) return;

        Font font = mc.font;
        for (Waypoint wp : waypoints) {
            double wx = wp.x + 0.5;
            double wy = wp.y;
            double wz = wp.z + 0.5;

            double dx = wx - camX;
            double dy = wy - camY;
            double dz = wz - camZ;
            if (dx * dx + dy * dy + dz * dz > maxDistSq) continue;

            String label = wp.name;
            if (WaypointsModule.showDistance()) {
                int dist = (int) Math.sqrt(dx * dx + dy * dy + dz * dz);
                label = wp.name + " (" + dist + "m)";
            }

            int argb = 0xFF000000 | (wp.color & 0xFFFFFF);

            ps.pushPose();
            ps.translate(dx, dy + 1.8, dz);
            ps.scale(-0.03f, -0.03f, 0.03f);

            float w = font.width(label);
            font.drawInBatch(
                Component.literal(label),
                -w / 2.0f,
                0.0f,
                argb,
                false,
                ps.last().pose(),
                SOURCE,
                Font.DisplayMode.SEE_THROUGH,
                0x55000000,
                0xF000F0
            );

            ps.popPose();
        }
        SOURCE.endBatch();
    }
}
