package com.crest.client.core.skinlayers3d;

import java.util.ArrayList;
import java.util.List;

public class CustomizableCubeListBuilder implements ModelBuilder {
    private final List<CustomizableCube> cubes = new ArrayList<>();
    private int u, v;
    private boolean mirror;
    private int textureWidth = 64, textureHeight = 64;

    public static ModelBuilder create() { return new CustomizableCubeListBuilder(); }

    @Override
    public ModelBuilder textureSize(int w, int h) { textureWidth = w; textureHeight = h; return this; }
    @Override
    public ModelBuilder uv(int u, int v) { this.u = u; this.v = v; return this; }
    @Override
    public ModelBuilder mirror(boolean m) { mirror = m; return this; }

    public List<CustomizableCube> getCubes() { return cubes; }

    @Override
    public ModelBuilder addBox(float x, float y, float z, float pixelSize, Direction[] hide, Direction[][] corners) {
        cubes.add(new CustomizableCube(u, v, (mirror ? -1 : 1) * x, y, z,
                pixelSize, pixelSize, pixelSize, 0, 0, 0, mirror,
                textureWidth, textureHeight, hide, corners));
        return this;
    }

    @Override
    public ModelBuilder addVanillaBox(float x, float y, float z, float w, float h, float d) {
        return this;
    }

    @Override
    public boolean isEmpty() { return cubes.isEmpty(); }
}
