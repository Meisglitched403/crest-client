package com.crest.client.waypoints;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;

/**
 * Renders a colored, translucent cube overlay at each waypoint's block position
 * in the 3D world (the "highlight blocks" feature). Uses the same proven
 * ShapeRenderer + RenderTypes pattern as HitboxModule at the renderLevel RETURN
 * injection, where the perspective projection is still active.
 *
 * Color matches the waypoint's own color (so a red waypoint highlights red,
 * a green waypoint highlights green, etc.). Clumps are skipped (they are shown
 * as a single projected marker instead).
 */
public final class CubeHighlightRenderer {

    private CubeHighlightRenderer() {}

    public static void render(PoseStack ps, MultiBufferSource.BufferSource buffer,
                              WaypointManager mgr, ModConfig cfg) {
        if (mgr == null || cfg == null) return;
        if (!cfg.showCubeHighlights) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        double e = cfg.cubeExpand;

        // Fill phase: must fully build + end one RenderType before starting another,
        // otherwise BufferBuilder throws "Not building!".
        VertexConsumer fill = buffer.getBuffer(RenderTypes.debugFilledBox());
        for (Waypoint wp : mgr.getWaypoints()) {
            if (mgr.isHiddenIncludingGroup(wp)) continue;
            if (mgr.getClumpsByIdInternal().containsKey(wp.getId())) continue;
            BlockPos p = wp.getPosition();
            if (p == null) continue;

            int base = wp.getColor() & 0xFFFFFF;
            int fillColor = (clampAlpha(cfg.cubeFillAlpha) << 24) | base;

            AABB box = new AABB(
                    p.getX() - e, p.getY() - e, p.getZ() - e,
                    p.getX() + 1 + e, p.getY() + 1 + e, p.getZ() + 1 + e);

            ShapeRenderer.renderShape(ps, fill, Shapes.create(box), 0, 0, 0, fillColor, 1.0f);
        }
        buffer.endBatch(RenderTypes.debugFilledBox());

        // Outline phase.
        VertexConsumer lines = buffer.getBuffer(RenderTypes.lines());
        for (Waypoint wp : mgr.getWaypoints()) {
            if (mgr.isHiddenIncludingGroup(wp)) continue;
            if (mgr.getClumpsByIdInternal().containsKey(wp.getId())) continue;
            BlockPos p = wp.getPosition();
            if (p == null) continue;

            int base = wp.getColor() & 0xFFFFFF;
            int lineColor = (clampAlpha(cfg.cubeOutlineAlpha) << 24) | base;

            AABB box = new AABB(
                    p.getX() - e, p.getY() - e, p.getZ() - e,
                    p.getX() + 1 + e, p.getY() + 1 + e, p.getZ() + 1 + e);

            ShapeRenderer.renderShape(ps, lines, Shapes.create(box), 0, 0, 0, lineColor, cfg.cubeOutlineWidth);
        }
        buffer.endBatch(RenderTypes.lines());
    }

    private static int clampAlpha(float a) {
        int v = (int) (255f * Math.max(0f, Math.min(1f, a)));
        return v & 0xFF;
    }
}
