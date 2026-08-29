package com.crest.client.waypoints;

import com.crest.client.waypoints.label.preset.LabelPreset;
import com.crest.client.waypoints.label.preset.LabelPresetStore;

/** Binds a label preset id into the active config. */
public final class WaypointLabelPresetService {
    private WaypointLabelPresetService() {}

    public static void apply(ModConfig cfg, String presetId) {
        if (presetId == null || presetId.isEmpty()) { cfg.activeLabelPresetId = ""; return; }
        LabelPreset preset = LabelPresetStore.getById(presetId);
        if (preset != null) {
            cfg.labelGlobals = preset.globals.copy();
            cfg.activeLabelPresetId = preset.id;
        }
    }

    public static String current(ModConfig cfg) {
        return cfg.activeLabelPresetId != null ? cfg.activeLabelPresetId : "";
    }
}
