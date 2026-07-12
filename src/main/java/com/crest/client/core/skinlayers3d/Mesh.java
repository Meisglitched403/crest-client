package com.crest.client.core.skinlayers3d;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;

public interface Mesh {
    Mesh EMPTY = new Mesh() {
        @Override public void render(ModelPart vanillaModel, PoseStack poseStack, VertexConsumer vertexConsumer, int light, int overlay, int color) {}
        @Override public void setPosition(float x, float y, float z) {}
        @Override public void setRotation(float xRot, float yRot, float zRot) {}
        @Override public void loadPose(PartPose partPose) {}
        @Override public void copyFrom(ModelPart modelPart) {}
        @Override public void reset() {}
        @Override public void setVisible(boolean visible) {}
        @Override public boolean isVisible() { return false; }
    };

    default void render(PoseStack poseStack, VertexConsumer vertexConsumer, int light, int overlay) {
        render(null, poseStack, vertexConsumer, light, overlay, -1);
    }

    void render(ModelPart vanillaModel, PoseStack poseStack, VertexConsumer vertexConsumer, int light, int overlay, int color);

    default void render(ModelPart vanillaModel, PoseStack poseStack, VertexConsumer vertexConsumer, int light, int overlay, float red, float green, float blue, float alpha) {
        int a = (int)(alpha * 255.0f) << 24;
        int r = (int)(red * 255.0f) << 16;
        int g = (int)(green * 255.0f) << 8;
        int b = (int)(blue * 255.0f);
        render(vanillaModel, poseStack, vertexConsumer, light, overlay, a | r | g | b);
    }

    void setPosition(float x, float y, float z);
    void setRotation(float xRot, float yRot, float zRot);
    void loadPose(PartPose partPose);
    void copyFrom(ModelPart modelPart);
    void reset();
    void setVisible(boolean visible);
    boolean isVisible();
}
