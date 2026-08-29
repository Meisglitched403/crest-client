package com.crest.client.waypoints.label.preset;

import com.google.gson.annotations.Expose;

/** Global label appearance for a single waypoint. */
public class LabelGlobals {
    @Expose public float scale = 1f;
    @Expose public int opacityPercent = 100;
    @Expose public float eyeOffset = 0f;
    @Expose public LabelBillboardMode billboardMode = LabelBillboardMode.FULL;

    public LabelGlobals copy() {
        LabelGlobals g = new LabelGlobals();
        g.scale = scale;
        g.opacityPercent = opacityPercent;
        g.eyeOffset = eyeOffset;
        g.billboardMode = billboardMode;
        return g;
    }
}
