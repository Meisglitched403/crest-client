package com.crest.client.core.skinlayers3d;

import com.mojang.blaze3d.vertex.PoseStack;

public interface ModelPartInjector {
    void setInjectedMesh(Mesh mesh, OffsetProvider offsetProvider);
    boolean isVisible();
    Mesh getInjectedMesh();
    OffsetProvider getOffsetProvider();
    void prepareTranslateAndRotate(PoseStack poseStack);
}
