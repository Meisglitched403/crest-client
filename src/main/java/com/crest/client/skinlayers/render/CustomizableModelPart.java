package com.crest.client.skinlayers.render;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import com.crest.client.skinlayers.api.Mesh;
import com.crest.client.skinlayers.api.MeshTransformer;
import com.crest.client.skinlayers.api.SkinLayersAPI;
import com.crest.client.skinlayers.versionless.render.CustomModelPart;
import com.crest.client.skinlayers.versionless.render.CustomizableCube;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.ModelPart.Cube;
import net.minecraft.client.model.geom.PartPose;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class CustomizableModelPart extends CustomModelPart implements Mesh {

    private final List<Cube> cubes;
    private final java.util.Map<String, ModelPart> children;

    public CustomizableModelPart(List<Cube> list, List<CustomizableCube> customCubes, java.util.Map<String, ModelPart> map) {
        super(customCubes);
        this.cubes = list;
        this.children = map;
    }

    public void loadPose(PartPose partPose) {
        this.x = partPose.x();
        this.y = partPose.y();
        this.z = partPose.z();
        this.xRot = partPose.xRot();
        this.yRot = partPose.yRot();
        this.zRot = partPose.zRot();
    }

    public void copyFrom(ModelPart modelPart) {
        this.xRot = modelPart.xRot;
        this.yRot = modelPart.yRot;
        this.zRot = modelPart.zRot;
        this.x = modelPart.x;
        this.y = modelPart.y;
        this.z = modelPart.z;
    }

    public void render(PoseStack poseStack, VertexConsumer vertexConsumer, int i, int j) {
        render(null, poseStack, vertexConsumer, i, j, 1.0F, 1.0F, 1.0F, 1.0F);
    }

    private int convertFloatColorToInteger(float color) {
        return color > 1F ? 255 : Math.round(color * 255F);
    }

    @Deprecated(forRemoval = true)
    public void render(ModelPart vanillaModel, PoseStack poseStack, VertexConsumer vertexConsumer, int light,
            int overlay, float red, float green, float blue, float alpha) {
        var color = (convertFloatColorToInteger(alpha) & 0xFF) << 24 | (convertFloatColorToInteger(red) & 0xFF) << 16
                | (convertFloatColorToInteger(green) & 0xFF) << 8 | convertFloatColorToInteger(blue) & 0xFF;

        render(vanillaModel, poseStack, vertexConsumer, light, overlay, color);
    }

    public void render(ModelPart vanillaModel, PoseStack poseStack, VertexConsumer vertexConsumer, int light,
            int overlay, int color) {
        if (!this.visible)
            return;
        poseStack.pushPose();
        translateAndRotate(poseStack);
        try {
            compile(vanillaModel, poseStack.last(), vertexConsumer, light, overlay, color);
        } catch (IllegalStateException | IllegalArgumentException ex) {
            throw new IllegalStateException(
                    "Crash in 3d Skin Layer function from overflow. PolygonAmount=" + polygonAmount
                            + " PolygonDataLength=" + polygonData.length,
                    ex);
        }

        for (ModelPart modelPart : this.children.values()) {
            modelPart.render(poseStack, vertexConsumer, light, overlay, color);
        }
        poseStack.popPose();
    }

    public void translateAndRotate(PoseStack poseStack) {
        if (x != 0 || y != 0 || z != 0)
            poseStack.translate(this.x / 16.0F, this.y / 16.0F, this.z / 16.0F);
        if (this.xRot != 0.0F || this.yRot != 0.0F || this.zRot != 0.0F)
            poseStack.mulPose((new Quaternionf()).rotationZYX(this.zRot, this.yRot, this.xRot));
    }

    private final Vector4f[] vector4f = new Vector4f[] { new Vector4f(), new Vector4f(), new Vector4f(), new Vector4f() };

    private void compile(ModelPart vanillaModel, PoseStack.Pose pose, VertexConsumer vertexConsumer, int light,
            int overlay, int color) {
        MeshTransformer transformer = SkinLayersAPI.getMeshTransformerProvider().prepareTransformer(vanillaModel);
        Matrix4f matrix4f = pose.pose();
        Matrix3f matrix3f = pose.normal();

        for (int id = 0; id < polygonData.length; id += polyDataSize) {
            Vector3f vector3f = new Vector3f(polygonData[id + 0], polygonData[id + 1], polygonData[id + 2]);
            for (int o = 0; o < 4; o++) {
                vector4f[o].set(polygonData[id + 3 + (o * 5) + 0], polygonData[id + 3 + (o * 5) + 1],
                        polygonData[id + 3 + (o * 5) + 2], 1.0F);
            }
            transformer.transform(vector3f, vector4f);

            vector3f = matrix3f.transform(vector3f);
            for (int o = 0; o < 4; o++) {
                matrix4f.transform(vector4f[o]);
                vertexConsumer.addVertex(vector4f[o].x(), vector4f[o].y(), vector4f[o].z());
                vertexConsumer.setColor(color);
                vertexConsumer.setUv(polygonData[id + 3 + (o * 5) + 3], polygonData[id + 3 + (o * 5) + 4]);
                vertexConsumer.setOverlay(overlay);
                vertexConsumer.setLight(light);
                vertexConsumer.setNormal(vector3f.x(), vector3f.y(), vector3f.z());
            }
        }

        for (Cube cube : this.cubes) {
            transformer.transform(cube);
            cube.compile(pose, vertexConsumer, light, overlay, color);
        }
    }

    @Override
    public void reset() {
        this.x = 0;
        this.y = 0;
        this.z = 0;
        this.xRot = 0;
        this.yRot = 0;
        this.zRot = 0;
    }

}
