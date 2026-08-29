package com.crest.client.waypoints;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

/** Projects waypoint positions to screen space and draws labels during the HUD pass. */
public final class WaypointOverlayLabelRenderer {

    private WaypointOverlayLabelRenderer() {}

    public static void render(GuiGraphicsExtractor g, Minecraft mc, WaypointManager mgr, ModConfig cfg) {
        if (mgr == null || cfg == null) return;
        if (mc.level == null || mc.player == null || mc.getWindow() == null) return;

        int width = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();

        double maxDistSq = cfg.maxWaypointRenderDistance * cfg.maxWaypointRenderDistance;
        Font font = mc.font;

        Vec3 camPos = mc.gameRenderer.getMainCamera().position();
        Vec3 camFwd = new Vec3(
                mc.gameRenderer.getMainCamera().forwardVector().x(),
                mc.gameRenderer.getMainCamera().forwardVector().y(),
                mc.gameRenderer.getMainCamera().forwardVector().z());

        for (Waypoint wp : mgr.getWaypoints()) {
            if (mgr.isHiddenIncludingGroup(wp)) continue;
            if (cfg.activeFolderScope != null && !cfg.activeFolderScope.isEmpty()) {
                if (!cfg.activeFolderScope.equals(mgr.getFolderIdForWaypoint(wp.getId()))) continue;
            }
            ResolvedWaypointSettings s = mgr.resolveSettings(wp, cfg);

            double wx, wy, wz;
            if (mgr.getClumpsByIdInternal().containsKey(wp.getId())) {
                double[] avg = mgr.clumpAverage(wp.getId());
                wx = avg[0] + 0.5; wy = avg[1] + 0.5; wz = avg[2] + 0.5;
            } else {
                BlockPos p = wp.getPosition();
                if (p == null) continue;
                wx = p.getX() + 0.5; wy = p.getY() + 0.5; wz = p.getZ() + 0.5;
            }

            Vec3 delta = new Vec3(wx - camPos.x, wy - camPos.y, wz - camPos.z);
            if (delta.dot(camFwd) <= 0.05) continue; // behind camera
            double dSq = delta.lengthSqr();
            if (dSq > maxDistSq) continue;
            if (cfg.activeHideThreshold > 0 && dSq < cfg.activeHideThreshold * cfg.activeHideThreshold) continue;

            // Project using the real combined view-projection matrix (handles FOV, aspect, view correctly).
            Vec3 projected = mc.gameRenderer.projectPointToScreen(new Vec3(wx, wy, wz));
            if (projected == null) continue;
            if (projected.z < 0 || projected.z > 1) continue; // behind camera / outside depth range

            float screenX = (float) ((projected.x + 1.0) * 0.5 * width);
            float screenY = (float) ((1.0 - projected.y) * 0.5 * height);

            if (screenX < -50 || screenX > width + 50 || screenY < -50 || screenY > height + 50) continue;

            int dist = (int) Math.round(Math.sqrt(dSq));
            float alpha = labelAlpha(cfg, s, dist);

            int rgb = wp.getColor() & 0xFFFFFF;

            // Screen-space marker (sticks to the waypoint on screen).
            int mx = (int) screenX;
            int my = (int) screenY;
            int r = 4;
            g.fill(mx - r - 1, my - r - 1, mx + r + 1, my + r + 1, 0xFF000000);
            g.fill(mx - r, my - r, mx + r, my + r, ((int) (255 * alpha) << 24) | rgb);

            String text = wp.getName();
            if (cfg.showDistance && s.showDistance) text += " [" + dist + "m]";

            int argb = ((int) (255 * alpha) << 24) | rgb;
            int tw = font.width(text);
            int bx = mx - tw / 2 - 3;
            int by = my + r + 3;
            g.fill(bx - 1, by - 1, bx + tw + 5, by + font.lineHeight + 3,
                    ((int) (150 * alpha) << 24) | 0x000000);
            g.text(font, Component.literal(text), bx + 1, by + 1, argb);
        }
    }

    private static float labelAlpha(ModConfig cfg, ResolvedWaypointSettings s, int dist) {
        float base = cfg.labelOpacityPercent / 100f;
        if (s.labelFadeEndBlocks > s.labelFadeStartBlocks && dist > s.labelFadeStartBlocks) {
            float t = (dist - s.labelFadeStartBlocks) / (s.labelFadeEndBlocks - s.labelFadeStartBlocks);
            t = Math.max(0f, Math.min(1f, t));
            base *= (1f - t);
        }
        return Math.max(0f, Math.min(1f, base));
    }
}
