package com.crest.client.waypoints;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

/** Quick-create a waypoint at the player's current position. */
public final class QuickWaypointController {
    private QuickWaypointController() {}

    public static Waypoint createHere(Minecraft mc, WaypointManager mgr, ModConfig cfg, String presetId) {
        if (mc.player == null || mc.level == null) return null;
        int color = WaypointColorService.pickNewWaypointColor(cfg, Waypoint.Type.NORMAL);
        Waypoint wp = mgr.createWaypoint(mc.player.blockPosition(), "Waypoint", color, "block:grass", false, Waypoint.Type.NORMAL);
        if (presetId != null && !presetId.isEmpty()) {
            wp.setSourcePresetId(presetId);
            com.crest.client.waypoints.label.preset.LabelPreset preset =
                    com.crest.client.waypoints.label.preset.LabelPresetStore.getById(presetId);
            if (preset != null) {
                wp.setLabelComposition(preset.composition.copy());
                wp.setLabelGlobals(preset.globals.copy());
            }
        }
        return wp;
    }
}
