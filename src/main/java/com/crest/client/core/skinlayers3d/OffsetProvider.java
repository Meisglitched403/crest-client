package com.crest.client.core.skinlayers3d;

import com.mojang.blaze3d.vertex.PoseStack;

@FunctionalInterface
public interface OffsetProvider {
    void apply(PoseStack poseStack);

    OffsetProvider NONE = poseStack -> {};

    OffsetProvider HEAD = poseStack -> {};
    OffsetProvider BODY = poseStack -> {};
    OffsetProvider LEFT_ARM = poseStack -> {};
    OffsetProvider RIGHT_ARM = poseStack -> {};
    OffsetProvider LEFT_ARM_SLIM = poseStack -> {};
    OffsetProvider RIGHT_ARM_SLIM = poseStack -> {};
    OffsetProvider LEFT_LEG = poseStack -> {};
    OffsetProvider RIGHT_LEG = poseStack -> {};
    OffsetProvider FIRSTPERSON_LEFT_ARM = poseStack -> poseStack.translate(0, 0.4, -0.2);
    OffsetProvider FIRSTPERSON_LEFT_ARM_SLIM = poseStack -> poseStack.translate(0, 0.4, -0.2);
    OffsetProvider FIRSTPERSON_RIGHT_ARM = poseStack -> poseStack.translate(0, 0.4, -0.2);
    OffsetProvider FIRSTPERSON_RIGHT_ARM_SLIM = poseStack -> poseStack.translate(0, 0.4, -0.2);
}
