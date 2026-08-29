package com.crest.client.waypoints;

import net.minecraft.core.BlockPos;

import java.util.UUID;

/**
 * Rich waypoint model adapted from wWaypoints. Holds position, appearance,
 * per-waypoint overrides, grouping information and label composition.
 */
public class Waypoint {

    public enum Type {
        NORMAL,
        DEATH,
        SUPPLY_DROP,
        AUTO_PICK
    }

    public enum DisplayMode {
        OUTLINE,
        FILL,
        BOTH,
        NONE
    }

    public enum LabelType {
        TEXT,
        ICON,
        BOTH
    }

    public enum IconLabelMode {
        BLOCK,
        CUSTOM,
        FIRST_LETTER,
        PRESET
    }

    private final String id;
    private BlockPos position;
    private String name;
    private int color;
    private float hueDeg;
    private float saturation;
    private float value;
    private String iconId;
    private DisplayMode displayMode;
    private LabelType labelType;
    private IconLabelMode iconLabelMode;
    private boolean hidden;
    private boolean locked;
    private final Type type;

    private int outlineOpacityPercentOverride = -1;
    private int fillOpacityPercentOverride = -1;
    private float outlineThicknessOverride = -1f;
    private float iconScaleOverride = -1f;
    private Boolean showDistanceOverride;
    private Boolean renderThroughWallsOverride;
    private float labelFadeStartBlocksOverride = -1f;
    private float labelFadeEndBlocksOverride = -1f;
    private float groupCollapseDistanceOverride = -1f;

    private boolean autoSupplyDrop;
    private boolean autoPickWaypoint;
    private boolean deathWaypoint;
    private Boolean deleteOnArrival;
    private String sourcePresetId;

    private com.crest.client.waypoints.label.compose.LabelComposition labelComposition;
    private com.crest.client.waypoints.label.preset.LabelGlobals labelGlobals;

    private boolean pendingBlockIconResolution;
    private boolean dynamicY;
    private double dynamicYOffset;
    private BlockModelShape blockModelShape;
    private String folderNavigationKeybind;

    // transient render caches
    private transient String cachedName;
    private transient String cachedTrimmedName;
    private transient String cachedBoxText;
    private transient String cachedFirstLetter;
    private transient int cachedPosX;
    private transient int cachedPosY;
    private transient int cachedPosZ;
    private transient boolean cachedDynamicY;
    private transient double cachedDynamicYOffset;
    private transient String cachedPosLabel;
    private transient int cachedDistanceBlocks;
    private transient String cachedDistanceLabel;

    public Waypoint(String id, BlockPos position, String name, int color, String iconId, boolean deathWaypoint, Type type) {
        this.id = id;
        this.position = position;
        this.name = name;
        this.color = color;
        float[] hsv = ColorUtil.rgbToHsv(color);
        this.hueDeg = hsv[0];
        this.saturation = hsv[1];
        this.value = hsv[2];
        this.iconId = iconId;
        this.displayMode = DisplayMode.BOTH;
        this.labelType = LabelType.TEXT;
        this.iconLabelMode = IconLabelMode.BLOCK;
        this.hidden = false;
        this.locked = false;
        this.type = type;
        this.deathWaypoint = deathWaypoint;
        invalidateNameCaches();
        invalidatePositionCaches();
    }

    public String getId() { return id; }
    public BlockPos getPosition() { return position; }
    public String getName() { return name; }
    public int getColor() { return color; }
    public float getHueDeg() { return hueDeg; }
    public float getSaturation() { return saturation; }
    public float getValue() { return value; }
    public String getIconId() { return iconId; }
    public DisplayMode getDisplayMode() { return displayMode; }
    public LabelType getLabelType() { return labelType; }
    public IconLabelMode getIconLabelMode() { return iconLabelMode; }
    public boolean isHidden() { return hidden; }
    public boolean isLocked() { return locked; }
    public Type getType() { return type; }

    public Waypoint copyTo(BlockPos newPos) {
        Waypoint w = new Waypoint(UUID.randomUUID().toString(), newPos, name, color, iconId, deathWaypoint, type);
        w.displayMode = displayMode;
        w.labelType = labelType;
        w.iconLabelMode = iconLabelMode;
        w.hidden = hidden;
        w.locked = locked;
        w.outlineOpacityPercentOverride = outlineOpacityPercentOverride;
        w.fillOpacityPercentOverride = fillOpacityPercentOverride;
        w.outlineThicknessOverride = outlineThicknessOverride;
        w.iconScaleOverride = iconScaleOverride;
        w.showDistanceOverride = showDistanceOverride;
        w.renderThroughWallsOverride = renderThroughWallsOverride;
        w.labelFadeStartBlocksOverride = labelFadeStartBlocksOverride;
        w.labelFadeEndBlocksOverride = labelFadeEndBlocksOverride;
        w.groupCollapseDistanceOverride = groupCollapseDistanceOverride;
        if (hasLabelGlobals()) w.setLabelGlobals(labelGlobals);
        if (hasLabelComposition()) w.setLabelComposition(labelComposition);
        if (hasSourcePresetId()) w.setSourcePresetId(sourcePresetId);
        w.invalidateNameCaches();
        w.invalidatePositionCaches();
        return w;
    }

    public void setName(String name) { this.name = name; invalidateNameCaches(); }
    public void setPosition(BlockPos position) { this.position = position; invalidatePositionCaches(); }
    public void setColor(int color) {
        this.color = color;
        float[] hsv = ColorUtil.rgbToHsv(color);
        this.hueDeg = hsv[0];
        this.saturation = hsv[1];
        this.value = hsv[2];
    }
    public void setColorHsv(float h, float s, float v) {
        this.hueDeg = h; this.saturation = s; this.value = v;
        this.color = ColorUtil.hsvToRgb(h, s, v);
    }
    public void setIconId(String iconId) { this.iconId = iconId; }
    public void setDisplayMode(DisplayMode m) { this.displayMode = m; }
    public void setLabelType(LabelType t) { this.labelType = t; }
    public void setIconLabelMode(IconLabelMode m) { this.iconLabelMode = m; }
    public void setHidden(boolean hidden) { this.hidden = hidden; }
    public void setLocked(boolean locked) { this.locked = locked; }

    public boolean hasOutlineOpacityPercentOverride() { return outlineOpacityPercentOverride >= 0; }
    public int getOutlineOpacityPercentOverride() { return outlineOpacityPercentOverride; }
    public void setOutlineOpacityPercentOverride(int v) { this.outlineOpacityPercentOverride = v; }
    public void clearOutlineOpacityPercentOverride() { this.outlineOpacityPercentOverride = -1; }

    public boolean hasFillOpacityPercentOverride() { return fillOpacityPercentOverride >= 0; }
    public int getFillOpacityPercentOverride() { return fillOpacityPercentOverride; }
    public void setFillOpacityPercentOverride(int v) { this.fillOpacityPercentOverride = v; }
    public void clearFillOpacityPercentOverride() { this.fillOpacityPercentOverride = -1; }

    public boolean hasOutlineThicknessOverride() { return outlineThicknessOverride >= 0; }
    public float getOutlineThicknessOverride() { return outlineThicknessOverride; }
    public void setOutlineThicknessOverride(float v) { this.outlineThicknessOverride = v; }
    public void clearOutlineThicknessOverride() { this.outlineThicknessOverride = -1f; }

    public boolean hasIconScaleOverride() { return iconScaleOverride >= 0; }
    public float getIconScaleOverride() { return iconScaleOverride; }
    public void setIconScaleOverride(float v) { this.iconScaleOverride = v; }
    public void clearIconScaleOverride() { this.iconScaleOverride = -1f; }

    public boolean hasShowDistanceOverride() { return showDistanceOverride != null; }
    public Boolean getShowDistanceOverride() { return showDistanceOverride; }
    public void setShowDistanceOverride(Boolean v) { this.showDistanceOverride = v; }
    public void clearShowDistanceOverride() { this.showDistanceOverride = null; }

    public boolean hasRenderThroughWallsOverride() { return renderThroughWallsOverride != null; }
    public Boolean getRenderThroughWallsOverride() { return renderThroughWallsOverride; }
    public void setRenderThroughWallsOverride(Boolean v) { this.renderThroughWallsOverride = v; }
    public void clearRenderThroughWallsOverride() { this.renderThroughWallsOverride = null; }

    public boolean hasLabelFadeRangeOverride() { return labelFadeStartBlocksOverride >= 0; }
    public float getLabelFadeStartBlocksOverride() { return labelFadeStartBlocksOverride; }
    public float getLabelFadeEndBlocksOverride() { return labelFadeEndBlocksOverride; }
    public void setLabelFadeRangeOverride(float s, float e) { this.labelFadeStartBlocksOverride = s; this.labelFadeEndBlocksOverride = e; }
    public void clearLabelFadeRangeOverride() { this.labelFadeStartBlocksOverride = -1f; this.labelFadeEndBlocksOverride = -1f; }

    public boolean hasGroupCollapseDistanceOverride() { return groupCollapseDistanceOverride >= 0; }
    public float getGroupCollapseDistanceOverride() { return groupCollapseDistanceOverride; }
    public void setGroupCollapseDistanceOverride(float v) { this.groupCollapseDistanceOverride = v; }
    public void clearGroupCollapseDistanceOverride() { this.groupCollapseDistanceOverride = -1f; }

    public boolean isAutoSupplyDrop() { return autoSupplyDrop; }
    public void setAutoSupplyDrop(boolean b) { this.autoSupplyDrop = b; }
    public boolean isAutoPickWaypoint() { return autoPickWaypoint; }
    public void setAutoPickWaypoint(boolean b) { this.autoPickWaypoint = b; }
    public boolean isDeathWaypoint() { return deathWaypoint; }
    public void setDeathWaypoint(boolean b) { this.deathWaypoint = b; }

    public boolean hasDeleteOnArrivalOverride() { return deleteOnArrival != null; }
    public Boolean getDeleteOnArrivalOverride() { return deleteOnArrival; }
    public void setDeleteOnArrivalOverride(Boolean v) { this.deleteOnArrival = v; }
    public void clearDeleteOnArrivalOverride() { this.deleteOnArrival = null; }
    public boolean isDeleteOnArrival() { return deleteOnArrival != null && deleteOnArrival; }

    public String getSourcePresetId() { return sourcePresetId; }
    public void setSourcePresetId(String id) { this.sourcePresetId = id; }
    public boolean hasSourcePresetId() { return sourcePresetId != null && !sourcePresetId.isEmpty(); }
    public void clearSourcePresetId() { this.sourcePresetId = null; }

    public com.crest.client.waypoints.label.compose.LabelComposition getLabelComposition() { return labelComposition; }
    public void setLabelComposition(com.crest.client.waypoints.label.compose.LabelComposition c) { this.labelComposition = c; }
    public boolean hasLabelComposition() { return labelComposition != null; }
    public void clearLabelComposition() { this.labelComposition = null; }

    public com.crest.client.waypoints.label.preset.LabelGlobals getLabelGlobals() { return labelGlobals; }
    public void setLabelGlobals(com.crest.client.waypoints.label.preset.LabelGlobals g) { this.labelGlobals = g; }
    public boolean hasLabelGlobals() { return labelGlobals != null; }
    public void clearLabelGlobals() { this.labelGlobals = null; }

    public boolean hasPendingBlockIconResolution() { return pendingBlockIconResolution; }
    public void setPendingBlockIconResolution(boolean b) { this.pendingBlockIconResolution = b; }
    public void clearPendingBlockIconResolution() { this.pendingBlockIconResolution = false; }

    public boolean isDynamicY() { return dynamicY; }
    public void setDynamicY(boolean b) { this.dynamicY = b; }
    public double getDynamicYOffset() { return dynamicYOffset; }
    public void setDynamicYOffset(double d) { this.dynamicYOffset = d; }

    public BlockModelShape getBlockModelShape() { return blockModelShape; }
    public void setBlockModelShape(BlockModelShape s) { this.blockModelShape = s; }
    public void clearBlockModelShape() { this.blockModelShape = null; }

    public String getFolderNavigationKeybind() { return folderNavigationKeybind; }
    public boolean hasFolderNavigationKeybind() { return folderNavigationKeybind != null && !folderNavigationKeybind.isEmpty(); }
    public void setFolderNavigationKeybind(String k) { this.folderNavigationKeybind = k; }
    public void clearFolderNavigationKeybind() { this.folderNavigationKeybind = null; }

    public int getX() { ensurePositionCache(); return cachedPosX; }
    public int getY() { ensurePositionCache(); return cachedPosY; }
    public int getZ() { ensurePositionCache(); return cachedPosZ; }

    public String getTrimmedNameOrEmpty() { ensureNameCaches(); return cachedTrimmedName; }
    public String getBoxTextOrPositionLabel() { ensureNameCaches(); ensurePositionCache(); return cachedBoxText; }
    public String getFirstLetterOrW() { ensureNameCaches(); return cachedFirstLetter; }
    public String getPositionLabel() { ensurePositionCache(); return cachedPosLabel; }
    public String getDistanceLabel(int dist) { ensurePositionCache(); return cachedDistanceLabel != null ? cachedDistanceLabel : formatDistanceCompact(dist); }

    public String getDynamicYLabel() { return formatDynamicYLabel(dynamicYOffset); }

    private void ensureNameCaches() {
        if (cachedName != null && cachedName.equals(name)) return;
        invalidateNameCaches();
        cachedName = name;
        cachedTrimmedName = shortenForBox(name, 24);
        cachedFirstLetter = firstLetterOrW(name);
        cachedBoxText = (name == null || name.isEmpty()) ? getPositionLabel() : name;
    }

    private void ensurePositionCache() {
        if (position == null) return;
        if (cachedPosX == position.getX() && cachedPosY == position.getY() && cachedPosZ == position.getZ()
                && cachedDynamicY == dynamicY && cachedDynamicYOffset == dynamicYOffset && cachedPosLabel != null) return;
        cachedPosX = position.getX();
        cachedPosY = position.getY();
        cachedPosZ = position.getZ();
        cachedDynamicY = dynamicY;
        cachedDynamicYOffset = dynamicYOffset;
        int y = dynamicY ? (int) Math.round(position.getY() + dynamicYOffset) : position.getY();
        cachedPosLabel = position.getX() + " " + y + " " + position.getZ();
        cachedDistanceLabel = formatDistanceCompact(cachedDistanceBlocks);
    }

    private void invalidateNameCaches() { cachedName = null; cachedTrimmedName = null; cachedFirstLetter = null; cachedBoxText = null; }
    private void invalidatePositionCaches() { cachedPosX = 0; cachedPosY = 0; cachedPosZ = 0; cachedPosLabel = null; cachedDistanceLabel = null; }

    private static String formatDynamicYLabel(double off) {
        if (off == 0) return "y";
        String sign = off > 0 ? "+" : "-";
        return "y" + sign + Math.abs(Math.round(off));
    }

    private static String firstLetterOrW(String s) {
        if (s == null || s.isEmpty()) return "W";
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isLetterOrDigit(c)) return String.valueOf(Character.toUpperCase(c));
        }
        return "W";
    }

    private static String shortenForBox(String s, int max) {
        if (s == null) return "";
        if (s.length() <= max) return s;
        return s.substring(0, max - 1) + "…";
    }

    private static String formatDistanceCompact(int dist) {
        if (dist < 1000) return dist + "m";
        double k = dist / 1000.0;
        if (k < 10) return String.format("%.1fkm", k);
        if (k < 100) return String.format("%.0fkm", k);
        if (k < 1000) return String.format("%.1fMm", k / 1000.0);
        if (k < 10000) return String.format("%.0fMm", k / 1000.0);
        return "≥10Mm";
    }
}
