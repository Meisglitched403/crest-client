package com.crest.client.core.mixin;

import com.crest.client.core.skinlayers3d.Layers3d;
import com.crest.client.core.skinlayers3d.ModelPartInjector;
import com.crest.client.core.skinlayers3d.OffsetProvider;
import com.crest.client.core.skinlayers3d.PlayerEntityModelAccessor;
import com.crest.client.core.skinlayers3d.PlayerSettings;
import com.crest.client.core.skinlayers3d.SkinLayersConfig;
import com.crest.client.core.skinlayers3d.SkinUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerModel.class)
public abstract class SkinLayersPlayerModelMixin extends HumanoidModel<AvatarRenderState> implements PlayerEntityModelAccessor {
    @Shadow public ModelPart leftSleeve;
    @Shadow public ModelPart rightSleeve;
    @Shadow public ModelPart leftPants;
    @Shadow public ModelPart rightPants;
    @Shadow public ModelPart jacket;
    @Shadow private boolean slim;
    @Unique private boolean ignored;

    protected SkinLayersPlayerModelMixin(ModelPart part) {
        super(part);
    }

    @Override
    public boolean hasThinArms() { return slim; }

    @Override
    public void setIgnored(boolean ignored) { this.ignored = ignored; }

    @Inject(method = "setupAnim", at = @At("TAIL"))
    private void onSetupAnim(AvatarRenderState state, CallbackInfo ci) {
        if (ignored) return;
        Avatar entity = Layers3d.currentRenderedEntity;
        if (entity == null) return;
        if (!(entity instanceof PlayerSettings settings)) return;

        ((ModelPartInjector)(Object) hat).setInjectedMesh(null, null);
        ((ModelPartInjector)(Object) jacket).setInjectedMesh(null, null);
        ((ModelPartInjector)(Object) leftSleeve).setInjectedMesh(null, null);
        ((ModelPartInjector)(Object) rightSleeve).setInjectedMesh(null, null);
        ((ModelPartInjector)(Object) leftPants).setInjectedMesh(null, null);
        ((ModelPartInjector)(Object) rightPants).setInjectedMesh(null, null);

        if (Minecraft.getInstance().player == null ||
                entity.distanceToSqr(Minecraft.getInstance().getCameraEntity().position()) >
                        Layers3d.getConfig().renderDistanceLOD * Layers3d.getConfig().renderDistanceLOD)
            return;

        if (!SkinUtil.setup3dLayers(entity, settings, slim)) return;

        SkinLayersConfig cfg = Layers3d.getConfig();
        ItemStack headItem = entity.getItemBySlot(EquipmentSlot.HEAD);

        if (cfg.enableHat && (headItem == null || !headItem.isEmpty())) {
            ((ModelPartInjector)(Object) hat).setInjectedMesh(settings.getHeadMesh(), OffsetProvider.HEAD);
        }
        if (cfg.enableJacket) {
            ((ModelPartInjector)(Object) jacket).setInjectedMesh(settings.getTorsoMesh(), OffsetProvider.BODY);
        }
        if (cfg.enableLeftSleeve) {
            ((ModelPartInjector)(Object) leftSleeve).setInjectedMesh(settings.getLeftArmMesh(),
                    slim ? OffsetProvider.LEFT_ARM_SLIM : OffsetProvider.LEFT_ARM);
        }
        if (cfg.enableRightSleeve) {
            ((ModelPartInjector)(Object) rightSleeve).setInjectedMesh(settings.getRightArmMesh(),
                    slim ? OffsetProvider.RIGHT_ARM_SLIM : OffsetProvider.RIGHT_ARM);
        }
        if (cfg.enableLeftPants) {
            ((ModelPartInjector)(Object) leftPants).setInjectedMesh(settings.getLeftLegMesh(), OffsetProvider.LEFT_LEG);
        }
        if (cfg.enableRightPants) {
            ((ModelPartInjector)(Object) rightPants).setInjectedMesh(settings.getRightLegMesh(), OffsetProvider.RIGHT_LEG);
        }
    }
}
