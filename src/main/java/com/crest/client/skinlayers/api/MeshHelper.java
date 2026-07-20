package com.crest.client.skinlayers.api;

import com.mojang.blaze3d.platform.NativeImage;

public interface MeshHelper {

    public Mesh create3DMesh(NativeImage natImage, int width, int height, int depth, int textureU, int textureV,
            boolean topPivot, float rotationOffset);

    public Mesh create3DMesh(NativeImage natImage, int width, int height, int depth, int textureU, int textureV,
            boolean topPivot, float rotationOffset, boolean mirror);

}
