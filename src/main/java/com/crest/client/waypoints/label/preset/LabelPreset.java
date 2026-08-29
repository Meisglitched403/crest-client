package com.crest.client.waypoints.label.preset;

import com.crest.client.waypoints.label.compose.LabelComposition;
import com.google.gson.annotations.Expose;

/** A named, reusable label style. */
public class LabelPreset {
    @Expose public String id;
    @Expose public String name;
    @Expose public LabelComposition composition = new LabelComposition();
    @Expose public LabelGlobals globals = new LabelGlobals();
    @Expose public boolean builtIn;

    public LabelPreset(String id, String name, boolean builtIn) {
        this.id = id;
        this.name = name;
        this.builtIn = builtIn;
    }

    public LabelPreset copy() {
        LabelPreset p = new LabelPreset(id, name, builtIn);
        p.composition = composition.copy();
        p.globals = globals.copy();
        return p;
    }
}
