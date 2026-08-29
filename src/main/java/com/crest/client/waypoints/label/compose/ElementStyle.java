package com.crest.client.waypoints.label.compose;

import com.google.gson.annotations.Expose;

/** Per-element visual style. */
public class ElementStyle {
    @Expose public int bgOpacityPercent = 60;
    @Expose public float bgPadPx = 2f;
    @Expose public float outlinePx = 1f;
    @Expose public int outlineColor = 0xFFFFFFFF;
    @Expose public boolean textShadow = true;
    @Expose public int textShadowOpacityPercent = 50;
    @Expose public float textShadowDarkenMul = 0.5f;

    public ElementStyle copy() {
        ElementStyle s = new ElementStyle();
        s.bgOpacityPercent = bgOpacityPercent;
        s.bgPadPx = bgPadPx;
        s.outlinePx = outlinePx;
        s.outlineColor = outlineColor;
        s.textShadow = textShadow;
        s.textShadowOpacityPercent = textShadowOpacityPercent;
        s.textShadowDarkenMul = textShadowDarkenMul;
        return s;
    }
}
