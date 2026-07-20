package com.crest.client.core.mixin.skinlayers;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.crest.client.skinlayers.CrestSkinConfig;
import com.crest.client.skinlayers.CrestSkinUtil;
import com.crest.client.skinlayers.accessor.ModelPartInjector;
import com.crest.client.skinlayers.accessor.PlayerEntityModelAccessor;
import com.crest.client.skinlayers.accessor.PlayerSettings;
import com.crest.client.core.SkinLayers3dModule;
import com.crest.client.skinlayers.api.OffsetProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

@Mixin(PlayerModel.class)
public class PlayerModelMixin extends HumanoidModel<AvatarRenderState> implements PlayerEntityModelAccessor {

    public PlayerModelMixin(ModelPart modelPart) {
        super(modelPart);
    }

    @Shadow
    public ModelPart leftSleeve;
    @Shadow
    public ModelPart rightSleeve;
    @Shadow
    public ModelPart leftPants;
    @Shadow
    public ModelPart rightPants;
    @Shadow
    public ModelPart jacket;
    @Shadow
    private boolean slim;

    @Override
    public boolean hasThinArms() {
        return slim;
    }

    @Inject(method = "setupAnim", at = @At("TAIL"), cancellable = true)
    public void setupAnim(AvatarRenderState playerRenderState, CallbackInfo ci) {
        CrestSkinConfig.refresh();
        if (!com.crest.client.core.CrestModules.isEnabled("skin_layers_3d"))
            return;

        Avatar abstractClientPlayer = null;
        Level level = Minecraft.getInstance().level;
        if (level != null) {
            var entity = level.getEntity(playerRenderState.id);
            if (entity instanceof Avatar avatar) {
                abstractClientPlayer = avatar;
            }
        }
        if (abstractClientPlayer == null)
            return;

        Identifier skinLocation = null;
        if (playerRenderState.skin != null && playerRenderState.skin.body() instanceof ClientAsset.ResourceTexture rt) {
            skinLocation = rt.id();
        }
        if (skinLocation == null)
            return;

        PlayerSettings settings = (PlayerSettings) abstractClientPlayer;
        ((ModelPartInjector) (Object) hat).setInjectedMesh(null, null);
        ((ModelPartInjector) (Object) jacket).setInjectedMesh(null, null);
        ((ModelPartInjector) (Object) leftSleeve).setInjectedMesh(null, null);
        ((ModelPartInjector) (Object) rightSleeve).setInjectedMesh(null, null);
        ((ModelPartInjector) (Object) leftPants).setInjectedMesh(null, null);
        ((ModelPartInjector) (Object) rightPants).setInjectedMesh(null, null);
        if (Minecraft.getInstance().player == null || abstractClientPlayer.distanceToSqr(
                Minecraft.getInstance().player.position()) > CrestSkinConfig.renderDistanceLOD
                        * CrestSkinConfig.renderDistanceLOD) {
            return;
        }
        if (!CrestSkinUtil.setup3dLayers(abstractClientPlayer, settings, slim, skinLocation)) {
            return;
        }
        ItemStack itemStack = abstractClientPlayer.getItemBySlot(EquipmentSlot.HEAD);
        if (CrestSkinConfig.enableHat
                && (itemStack == null || !SkinLayers3dModule.hideHeadLayers().contains(itemStack.getItem()))) {
            ((ModelPartInjector) (Object) hat).setInjectedMesh(settings.getHeadMesh(), OffsetProvider.HEAD);
        }
        if (CrestSkinConfig.enableJacket) {
            ((ModelPartInjector) (Object) jacket).setInjectedMesh(settings.getTorsoMesh(), OffsetProvider.BODY);
        }
        if (CrestSkinConfig.enableLeftSleeve) {
            ((ModelPartInjector) (Object) leftSleeve).setInjectedMesh(settings.getLeftArmMesh(),
                    slim ? OffsetProvider.LEFT_ARM_SLIM : OffsetProvider.LEFT_ARM);
        }
        if (CrestSkinConfig.enableRightSleeve) {
            ((ModelPartInjector) (Object) rightSleeve).setInjectedMesh(settings.getRightArmMesh(),
                    slim ? OffsetProvider.RIGHT_ARM_SLIM : OffsetProvider.RIGHT_ARM);
        }
        if (CrestSkinConfig.enableLeftPants) {
            ((ModelPartInjector) (Object) leftPants).setInjectedMesh(settings.getLeftLegMesh(),
                    OffsetProvider.LEFT_LEG);
        }
        if (CrestSkinConfig.enableRightPants) {
            ((ModelPartInjector) (Object) rightPants).setInjectedMesh(settings.getRightLegMesh(),
                    OffsetProvider.RIGHT_LEG);
        }
    }

}
