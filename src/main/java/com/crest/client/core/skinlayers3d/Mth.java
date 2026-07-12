package com.crest.client.core.skinlayers3d;

public class Mth {
    public static float lerp(float delta, float start, float end) {
        return start + delta * (end - start);
    }
}
