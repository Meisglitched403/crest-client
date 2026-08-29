package com.crest.client.waypoints.label.compose;

import com.google.gson.annotations.Expose;

public class TextElement implements LabelElement {
    public enum Source { NAME, POSITION, DISTANCE, CUSTOM, ICON_LABEL }

    @Expose public Source source = Source.NAME;
    @Expose public String customText = "";
    @Expose public ElementStyle style = new ElementStyle();
    @Expose public boolean hideWhenNear = false;
    @Expose public boolean showOnlyWhenFacing = false;
    @Expose public float offsetX = 0f;
    @Expose public float offsetY = 0f;
    @Expose public AngleVisibility angleVisibility = AngleVisibility.ALWAYS;

    @Override public ElementStyle getStyle() { return style; }
    @Override public boolean isHideWhenNear() { return hideWhenNear; }
    @Override public boolean isShowOnlyWhenFacing() { return showOnlyWhenFacing; }
    @Override public float getOffsetX() { return offsetX; }
    @Override public float getOffsetY() { return offsetY; }
    @Override public AngleVisibility getAngleVisibility() { return angleVisibility; }
}
