package com.crest.client.core.skinlayers3d;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.List;
import java.util.Map;

public class CustomizableModelPart extends CustomModelPart implements Mesh {
    private final List<ModelPart.Cube> cubes;
    private final Map<String, ModelPart> children;
    private final Vector4f[] vec4 = {new Vector4f(), new Vector4f(), new Vector4f(), new Vector4f()};

    public CustomizableModelPart(List<ModelPart.Cube> cubes, List<CustomizableCube> customCubes, Map<String, ModelPart> children) {
        super(customCubes);
        this.cubes = cubes;
        this.children = children;
    }

    @Override
    public void loadPose(PartPose pose) {
        x = pose.x();
        y = pose.y();
        z = pose.z();
        xRot = pose.xRot();
        yRot = pose.yRot();
        zRot = pose.zRot();
    }

    @Override
    public void copyFrom(ModelPart part) {
        xRot = part.xRot; yRot = part.yRot; zRot = part.zRot;
        x = part.x; y = part.y; z = part.z;
    }

    @Override
    public void render(PoseStack poseStack, VertexConsumer consumer, int light, int overlay) {
        render(null, poseStack, consumer, light, overlay, -1);
    }

    @Override
    public void render(ModelPart vanillaModel, PoseStack poseStack, VertexConsumer consumer, int light, int overlay, int color) {
        if (!visible) return;
        poseStack.pushPose();
        translateAndRotate(poseStack);
        try {
            compile(vanillaModel, poseStack.last(), consumer, light, overlay, color);
        } catch (Exception e) {
            throw new RuntimeException("Crash in 3D Skin Layer", e);
        }
        for (ModelPart child : children.values())
            child.render(poseStack, consumer, light, overlay, color);
        poseStack.popPose();
    }

    public void translateAndRotate(PoseStack poseStack) {
        if (x != 0 || y != 0 || z != 0)
            poseStack.translate(x / 16f, y / 16f, z / 16f);
        if (xRot != 0 || yRot != 0 || zRot != 0)
            poseStack.mulPose(new Quaternionf().rotationZYX(zRot, yRot, xRot));
    }

    private void compile(ModelPart vanillaModel, PoseStack.Pose pose, VertexConsumer consumer, int light, int overlay, int color) {
        // ponytail: no mesh transformer — use EMPTY behavior (no transformation)
        Matrix4f mat4 = pose.pose();
        Matrix3f mat3 = pose.normal();

        for (int id = 0; id < polygonData.length; id += 23) {
            Vector3f normal = new Vector3f(polygonData[id], polygonData[id + 1], polygonData[id + 2]);
            for (int o = 0; o < 4; o++) {
                vec4[o].set(polygonData[id + 3 + o * 5], polygonData[id + 3 + o * 5 + 1],
                        polygonData[id + 3 + o * 5 + 2], 1f);
            }
            normal = mat3.transform(normal);
            for (int o = 0; o < 4; o++) {
                mat4.transform(vec4[o]);
                consumer.addVertex(vec4[o].x(), vec4[o].y(), vec4[o].z());
                consumer.setColor(color);
                consumer.setUv(polygonData[id + 3 + o * 5 + 3], polygonData[id + 3 + o * 5 + 4]);
                consumer.setOverlay(overlay);
                consumer.setLight(light);
                consumer.setNormal(normal.x(), normal.y(), normal.z());
            }
        }

        for (ModelPart.Cube cube : cubes)
            cube.compile(pose, consumer, light, overlay, color);
    }

    @Override
    public void reset() {
        x = y = z = 0;
        xRot = yRot = zRot = 0;
    }
}
