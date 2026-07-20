package com.crest.client.skinlayers.api;

import org.joml.Vector3f;
import org.joml.Vector4f;

import net.minecraft.client.model.geom.ModelPart.Cube;

public interface MeshTransformer {

    public void transform(Vector3f position, Vector4f[] vertexData);

    public void transform(Cube cube);

    public static final MeshTransformer EMPTY_TRANSFORMER = new MeshTransformer() {

        @Override
        public void transform(Cube cube) {
        }

        @Override
        public void transform(Vector3f position, Vector4f[] vertexData) {
        }
    };

}
