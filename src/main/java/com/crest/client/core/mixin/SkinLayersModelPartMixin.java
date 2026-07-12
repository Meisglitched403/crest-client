package com.crest.client.core.mixin;

import com.crest.client.core.skinlayers3d.Mesh;
import com.crest.client.core.skinlayers3d.ModelPartInjector;
import com.crest.client.core.skinlayers3d.OffsetProvider;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(ModelPart.class)
public class SkinLayersModelPartMixin implements ModelPartInjector {
    @Shadow private boolean visible;
    @Shadow public Map<String, ModelPart> children;
    @Unique private Mesh injectedMesh;
    @Unique private OffsetProvider offsetProvider = OffsetProvider.NONE;

    @Override
    public void setInjectedMesh(Mesh mesh, OffsetProvider provider) {
        injectedMesh = mesh;
        offsetProvider = provider != null ? provider : OffsetProvider.NONE;
    }

    @Override
    public Mesh getInjectedMesh() { return injectedMesh; }

    @Override
    public OffsetProvider getOffsetProvider() { return offsetProvider; }

    @Override
    public void prepareTranslateAndRotate(PoseStack poseStack) {}

    @Override
    public boolean isVisible() { return visible; }

    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V", at = @At("HEAD"))
    private void onRenderHead(PoseStack poseStack, VertexConsumer consumer, int light, int overlay, int color, CallbackInfo ci) {
        if (injectedMesh == null || injectedMesh == Mesh.EMPTY || !visible) return;
        poseStack.pushPose();
        offsetProvider.apply(poseStack);
        injectedMesh.render((ModelPart)(Object)this, poseStack, consumer, light, overlay, color);
        poseStack.popPose();
    }
}
