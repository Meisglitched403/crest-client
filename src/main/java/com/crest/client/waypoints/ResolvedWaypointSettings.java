package com.crest.client.waypoints;

import com.crest.client.waypoints.label.compose.LabelComposition;
import com.crest.client.waypoints.label.preset.LabelGlobals;

/** Effective per-waypoint settings after merging waypoint overrides with global config. */
public class ResolvedWaypointSettings {
    public Waypoint.DisplayMode displayMode;
    public Waypoint.LabelType labelType;
    public Waypoint.IconLabelMode iconLabelMode;
    public int outlineOpacityPercent;
    public int fillOpacityPercent;
    public float outlineThickness;
    public float iconScale;
    public boolean showDistance;
    public boolean renderThroughWalls;
    public float labelFadeStartBlocks;
    public float labelFadeEndBlocks;
    public float groupCollapseDistance;
    public int color;
    public LabelGlobals labelGlobals;
    public LabelComposition labelComposition;
    public String sourcePresetId;

    public static ResolvedWaypointSettings resolve(Waypoint wp, ModConfig cfg, ModConfig.TypeDefaults typeDefaults) {
        ResolvedWaypointSettings s = new ResolvedWaypointSettings();
        s.displayMode = wp.getDisplayMode() != null ? wp.getDisplayMode() : typeDefaults.displayMode;
        s.labelType = wp.getLabelType() != null ? wp.getLabelType() : typeDefaults.labelType;
        s.iconLabelMode = wp.getIconLabelMode() != null ? wp.getIconLabelMode() : typeDefaults.iconLabelMode;
        s.outlineOpacityPercent = wp.hasOutlineOpacityPercentOverride() ? wp.getOutlineOpacityPercentOverride() : typeDefaults.outlineOpacityPercent;
        s.fillOpacityPercent = wp.hasFillOpacityPercentOverride() ? wp.getFillOpacityPercentOverride() : typeDefaults.fillOpacityPercent;
        s.outlineThickness = wp.hasOutlineThicknessOverride() ? wp.getOutlineThicknessOverride() : typeDefaults.outlineThickness;
        s.iconScale = wp.hasIconScaleOverride() ? wp.getIconScaleOverride() : typeDefaults.iconScale;
        s.showDistance = wp.hasShowDistanceOverride() ? wp.getShowDistanceOverride() : typeDefaults.showDistance;
        s.renderThroughWalls = wp.hasRenderThroughWallsOverride() ? wp.getRenderThroughWallsOverride() : typeDefaults.renderThroughWalls;
        s.labelFadeStartBlocks = wp.hasLabelFadeRangeOverride() ? wp.getLabelFadeStartBlocksOverride() : typeDefaults.labelFadeStartBlocks;
        s.labelFadeEndBlocks = wp.hasLabelFadeRangeOverride() ? wp.getLabelFadeEndBlocksOverride() : typeDefaults.labelFadeEndBlocks;
        s.groupCollapseDistance = wp.hasGroupCollapseDistanceOverride() ? wp.getGroupCollapseDistanceOverride() : typeDefaults.groupCollapseDistance;
        s.color = wp.getColor();
        s.labelGlobals = wp.hasLabelGlobals() ? wp.getLabelGlobals() : typeDefaults.labelGlobals;
        s.labelComposition = wp.hasLabelComposition() ? wp.getLabelComposition() : cfg.defaultLabelComposition;
        s.sourcePresetId = wp.hasSourcePresetId() ? wp.getSourcePresetId() : null;
        return s;
    }
}
