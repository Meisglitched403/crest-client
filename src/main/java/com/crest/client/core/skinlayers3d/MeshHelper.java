package com.crest.client.core.skinlayers3d;

import com.mojang.blaze3d.platform.NativeImage;

public interface MeshHelper {
    Mesh create3DMesh(NativeImage nativeImage, int width, int height, int depth, int textureU, int textureV, boolean topPivot, float rotationOffset);
    Mesh create3DMesh(NativeImage nativeImage, int width, int height, int depth, int textureU, int textureV, boolean topPivot, float rotationOffset, boolean mirror);
}
