package com.crest.client.waypoints.label.compose;

import com.google.gson.annotations.Expose;

import java.util.ArrayList;
import java.util.List;

/** Ordered list of label layers drawn behind/over the waypoint name. */
public class LabelComposition {
    @Expose public List<ShapeElement> shapes = new ArrayList<>();
    @Expose public List<IconElement> icons = new ArrayList<>();
    @Expose public List<TextElement> texts = new ArrayList<>();

    public LabelComposition copy() {
        LabelComposition c = new LabelComposition();
        for (ShapeElement e : shapes) c.shapes.add(e);
        for (IconElement e : icons) c.icons.add(e);
        for (TextElement e : texts) c.texts.add(e);
        return c;
    }

    public boolean isEmpty() {
        return shapes.isEmpty() && icons.isEmpty() && texts.isEmpty();
    }

    public static LabelComposition defaultComposition() {
        LabelComposition c = new LabelComposition();
        c.shapes.add(new ShapeElement());
        c.icons.add(new IconElement());
        c.texts.add(new TextElement());
        return c;
    }
}
