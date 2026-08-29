package com.crest.client.waypoints;

import com.crest.client.waypoints.label.compose.LabelComposition;
import com.crest.client.waypoints.label.preset.LabelGlobals;

import java.util.ArrayList;
import java.util.List;

/** Global wWaypoints-style configuration. Saved as mod-config.json. */
public class ModConfig {
    public int version = 10;

    /* ---- visibility / toggles ---- */
    public boolean hideToggleEnabled = false;
    public boolean hideBeam = false;
    public boolean hideIconRender = false;
    public boolean chatCoordinatesEnabled = true;
    public boolean deathWaypoints = true;
    public int deathWaypointLimit = 3;
    public boolean hopliteEnabled = true;
    public boolean hopliteAutoPickCreatesWaypoint = true;
    public boolean hopliteAutoSupplyDropWaypoints = true;
    public boolean hideWaypoints = false;
    public boolean showThroughWalls = false;
    public boolean sortByName = false;
    public boolean showDistance = true;
    public boolean showBeam = true;
    public boolean showMarkers = true;
    public boolean showLabels = true;

    /* ---- cube block highlights (cubewaypoints-style) ---- */
    public boolean showCubeHighlights = true;
    public float cubeFillAlpha = 0.25f;
    public float cubeOutlineAlpha = 1.0f;
    public float cubeOutlineWidth = 1.0f;
    public float cubeExpand = 0.005f;

    public int maxWaypointRenderDistance = 1000;
    public int labelOpacityPercent = 100;

    public RandomColorsMode randomColorsMode = RandomColorsMode.OFF;
    public List<String> deathWaypointsAutoHiddenIds = new ArrayList<>();
    public int[] hideWaypointCycleThresholds = new int[] { 0, 100, 500, 2000 };

    /* ---- appearance defaults ---- */
    public TypeDefaults singleWaypointDefaults = new TypeDefaults();
    public TypeDefaults clumpDefaults = new TypeDefaults();
    public TypeDefaults groupDefaults = new TypeDefaults();

    public HopliteAppearance deathWaypointAppearance = new HopliteAppearance();
    public HopliteAppearance hopliteSupplyDropAppearance = new HopliteAppearance();
    public HopliteAppearance hopliteAutoPickAppearance = new HopliteAppearance();

    /* ---- labels ---- */
    public LabelGlobals labelGlobals = new LabelGlobals();
    public LabelComposition defaultLabelComposition = LabelComposition.defaultComposition();
    public String activeLabelPresetId = "";

    /* ---- transient runtime state (not saved) ---- */
    public transient int activeHideThreshold = 0;
    public transient int hideThresholdIndex = 0;
    public transient String activeFolderScope = "";

    public ModConfig() {
        singleWaypointDefaults.color = 0xFF55FFFF;
        singleWaypointDefaults.iconId = "block:grass";
        singleWaypointDefaults.showDistance = true;
        singleWaypointDefaults.labelGlobals = new LabelGlobals();

        clumpDefaults.color = 0xFFFFAA33;
        clumpDefaults.iconId = "block:grass";
        groupDefaults.color = 0xFFAA66FF;
        groupDefaults.iconId = "block:grass";
    }

    public enum RandomColorsMode {
        OFF,
        PER_WAYPOINT,
        PER_GROUP
    }

    public enum WaypointMenuSortOrder {
        DISTANCE,
        NAME,
        DATE
    }

    public static class TypeDefaults {
        public Waypoint.DisplayMode displayMode = Waypoint.DisplayMode.BOTH;
        public Waypoint.LabelType labelType = Waypoint.LabelType.ICON;
        public Waypoint.IconLabelMode iconLabelMode = Waypoint.IconLabelMode.BLOCK;

        public int color = 0xFF55FFFF;
        public String iconId = "block:grass";
        public int outlineOpacityPercent = 60;
        public int fillOpacityPercent = 20;
        public float outlineThickness = 3f;
        public float iconScale = 100f;

        public boolean showDistance = true;
        public boolean renderThroughWalls = false;
        public float labelFadeStartBlocks = 64f;
        public float labelFadeEndBlocks = 200f;
        public float groupCollapseDistance = 64f;

        public boolean beamEnabled = true;
        public int beamColor = 0xFF55FFFF;
        public float beamHeight = 64f;

        public LabelGlobals labelGlobals = new LabelGlobals();
    }

    public static class HopliteAppearance {
        public int color = 0xFF55FFFF;
        public int outlineOpacityPercent = 60;
        public int fillOpacityPercent = 20;
        public float outlineThickness = 3f;
        public float iconScale = 100f;
    }
}
