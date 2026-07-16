package com.crest.client.core.mixin;

import com.crest.client.core.SkinLayers3dModule;
import com.crest.client.core.skinlayers3d.Layers3d;
import com.crest.client.core.skinlayers3d.ModelPartInjector;
import com.crest.client.core.skinlayers3d.OffsetProvider;
import com.crest.client.core.skinlayers3d.PlayerSettings;
import com.crest.client.core.skinlayers3d.SkinLayersConfig;
import com.crest.client.core.skinlayers3d.SkinUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AvatarRenderer.class)
public abstract class SkinLayersPlayerRendererMixin extends LivingEntityRenderer<Avatar, AvatarRenderState, PlayerModel> {
    protected SkinLayersPlayerRendererMixin() { super(null, null, 0); }

    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void onExtractRenderState(Avatar entity, AvatarRenderState state, float partialTick, CallbackInfo ci) {
        Layers3d.currentRenderedEntity = entity;
    }

    @Inject(method = "renderHand", at = @At("HEAD"))
    private void onRenderHand(PoseStack poseStack, SubmitNodeCollector collector, int light,
                              Identifier texture, ModelPart arm, boolean bl, CallbackInfo ci) {
        SkinLayers3dModule.syncConfig();
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        PlayerModel model = getModel();
        ModelPart sleeve = arm == model.leftArm ? model.leftSleeve : model.rightSleeve;
        if (!(player instanceof PlayerSettings settings)) return;
        boolean slim = player.getSkin().model().equals(net.minecraft.world.entity.player.PlayerModelType.SLIM);

        ((ModelPartInjector)(Object) sleeve).setInjectedMesh(null, null);

        if (!SkinUtil.setup3dLayers(player, settings, slim)) return;

        SkinLayersConfig cfg = Layers3d.getConfig();
        if (arm == model.leftArm) {
            if (cfg.enableLeftSleeve) {
                ((ModelPartInjector)(Object) sleeve).setInjectedMesh(settings.getLeftArmMesh(),
                        slim ? OffsetProvider.FIRSTPERSON_LEFT_ARM_SLIM : OffsetProvider.FIRSTPERSON_LEFT_ARM);
            }
        } else if (cfg.enableRightSleeve) {
            ((ModelPartInjector)(Object) sleeve).setInjectedMesh(settings.getRightArmMesh(),
                    slim ? OffsetProvider.FIRSTPERSON_RIGHT_ARM_SLIM : OffsetProvider.FIRSTPERSON_RIGHT_ARM);
        }
    }
}
