package com.crest.client.waypoints.label.compose;

import com.google.gson.annotations.Expose;

/** Common interface for label elements (text / icon / shape). */
public interface LabelElement {
    ElementStyle getStyle();
    boolean isHideWhenNear();
    boolean isShowOnlyWhenFacing();
    float getOffsetX();
    float getOffsetY();
    AngleVisibility getAngleVisibility();
}
