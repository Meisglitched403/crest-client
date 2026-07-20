package com.crest.client.core.mixin.skinlayers;

import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import com.crest.client.skinlayers.accessor.ModelPartInjector;
import com.crest.client.skinlayers.api.Mesh;
import com.crest.client.skinlayers.api.OffsetProvider;
import net.minecraft.client.model.geom.ModelPart;

@Mixin(value = ModelPart.class, priority = 300)
public class ModelPartMixin implements ModelPartInjector {

    @Shadow
    public boolean visible;
    @Shadow
    private Map<String, ModelPart> children;

    private Mesh injectedMesh = null;
    private OffsetProvider offsetProvider = null;

    @Inject(method = "Lnet/minecraft/client/model/geom/ModelPart;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V", at = @At(value = "HEAD"), cancellable = true)
    public void render(PoseStack poseStack, VertexConsumer vertexConsumer, int light, int overlay, int color,
            CallbackInfo ci) {
        if (visible && injectedMesh != null) {
            poseStack.pushPose();
            translateAndRotate(poseStack);
            offsetProvider.applyOffset(poseStack, injectedMesh);
            injectedMesh.render((ModelPart) (Object) this, poseStack, vertexConsumer, light, overlay, color);
            poseStack.popPose();
            ci.cancel();
        }
    }

    @Override
    public void setInjectedMesh(Mesh mesh, OffsetProvider offsetProvider) {
        this.injectedMesh = mesh;
        this.offsetProvider = offsetProvider;
    }

    @Override
    public boolean isVisible() {
        return visible;
    }

    @Override
    public Mesh getInjectedMesh() {
        return injectedMesh;
    }

    @Override
    public OffsetProvider getOffsetProvider() {
        return offsetProvider;
    }

    @Shadow
    public void translateAndRotate(PoseStack poseStack) {
    }

    @Override
    public void prepareTranslateAndRotate(PoseStack poseStack) {
        translateAndRotate(poseStack);
    }

}
