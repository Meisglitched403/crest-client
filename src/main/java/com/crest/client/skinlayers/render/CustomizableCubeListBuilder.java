package com.crest.client.skinlayers.render;

import java.util.List;

import com.google.common.collect.Lists;

import com.crest.client.skinlayers.api.BoxBuilder;
import com.crest.client.skinlayers.api.Mesh;
import com.crest.client.skinlayers.api.SkinLayersAPI;
import com.crest.client.skinlayers.versionless.render.CustomizableCube;
import com.crest.client.skinlayers.versionless.util.Direction;
import com.crest.client.skinlayers.versionless.util.wrapper.ModelBuilder;
import net.minecraft.client.model.geom.ModelPart.Cube;
import net.minecraft.client.model.geom.builders.CubeListBuilder;

public class CustomizableCubeListBuilder implements ModelBuilder {

    private final List<CustomizableCube> cubes = Lists.newArrayList();
    private final List<Cube> vanillaCubes = Lists.newArrayList();
    private int u;
    private int v;
    private boolean mirror;
    private int textureWidth = 64;
    private int textureHeight = 64;

    public static ModelBuilder create() {
        return new CustomizableCubeListBuilder();
    }

    public ModelBuilder textureSize(int width, int height) {
        this.textureWidth = width;
        this.textureHeight = height;
        return this;
    }

    @Override
    public ModelBuilder uv(int u, int v) {
        this.u = u;
        this.v = v;
        return this;
    }

    @Override
    public ModelBuilder mirror(boolean bl) {
        this.mirror = bl;
        return this;
    }

    public List<CustomizableCube> getCubes() {
        return cubes;
    }

    public List<Cube> getVanillaCubes() {
        return vanillaCubes;
    }

    @Override
    public ModelBuilder addBox(float x, float y, float z, float pixelSize, Direction[] hide, Direction[][] corners) {
        this.cubes.add(new CustomizableCube(this.u, this.v, (mirror ? -1 : 1) * x, y, z, pixelSize, pixelSize,
                pixelSize, 0, 0, 0, this.mirror, textureWidth, textureHeight, hide, corners));
        return this;
    }

    @Override
    public ModelBuilder addVanillaBox(float x, float y, float z, float width, float height, float depth) {
        if (mirror) {
            x = -1;
        }
        this.vanillaCubes.add(SkinLayersAPI.boxBuilder.build(new BoxBuilder.BoxDefinition(u, v, mirror, x, y, z,
                width, height, depth, textureWidth, textureHeight)));
        return this;
    }

    @Override
    public boolean isEmpty() {
        return getCubes().isEmpty() && getVanillaCubes().isEmpty();
    }

}
