package com.crest.client.skinlayers.api;

import net.minecraft.client.model.geom.ModelPart.Cube;
import net.minecraft.client.model.geom.builders.CubeListBuilder;

public interface BoxBuilder {

    Cube build(BoxDefinition box);

    record BoxDefinition(int u, int v, boolean mirror, float x, float y, float z, float width, float height,
            float depth, int textureWidth, int textureHeight) {
    }

    public static final BoxBuilder DEFAULT = new BoxBuilder() {
        @Override
        public Cube build(BoxDefinition box) {
            CubeListBuilder cubeList = CubeListBuilder.create();
            cubeList.texOffs(box.u(), box.v()).mirror(box.mirror()).addBox(box.x(), box.y(), box.z(), box.width(),
                    box.height(), box.depth());
            return cubeList.getCubes().get(0).bake(box.textureWidth(), box.textureHeight());
        }
    };

}
