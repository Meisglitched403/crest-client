package com.crest.client.waypoints.label.compose;

import com.google.gson.annotations.Expose;

public class IconElement implements LabelElement {
    public enum Source { BLOCK, CUSTOM, FIRST_LETTER, PRESET }

    @Expose public Source source = Source.BLOCK;
    @Expose public String customIconId = "";
    @Expose public float scale = 1f;
    @Expose public float offsetX = 0f;
    @Expose public float offsetY = 0f;
    @Expose public ElementStyle style = new ElementStyle();
    @Expose public boolean hideWhenNear = false;
    @Expose public boolean showOnlyWhenFacing = false;
    @Expose public AngleVisibility angleVisibility = AngleVisibility.ALWAYS;

    @Override public ElementStyle getStyle() { return style; }
    @Override public boolean isHideWhenNear() { return hideWhenNear; }
    @Override public boolean isShowOnlyWhenFacing() { return showOnlyWhenFacing; }
    @Override public float getOffsetX() { return offsetX; }
    @Override public float getOffsetY() { return offsetY; }
    @Override public AngleVisibility getAngleVisibility() { return angleVisibility; }
}
