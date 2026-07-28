package com.crest.client.core.mixin;

import com.crest.client.core.ViewModelModule;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemInHandRenderer.class)
public class ItemInHandRendererMixin {
    @Shadow private ItemStack mainHandItem;
    @Shadow private ItemStack offHandItem;
    @Shadow private float mainHandHeight;
    @Shadow private float offHandHeight;

    @Inject(method = "renderArmWithItem", at = @At("HEAD"))
    private void viewmodel$pushPose(AbstractClientPlayer player, float frameInterp, float xRot,
                                    InteractionHand hand, float attack, ItemStack itemStack,
                                    float inverseArmHeight, PoseStack poseStack,
                                    SubmitNodeCollector collector, int lightCoords, CallbackInfo ci) {
        if (!ViewModelModule.isActive()) return;
        poseStack.pushPose();
    }

    @Inject(method = "renderArmWithItem", at = @At("TAIL"))
    private void viewmodel$popPose(AbstractClientPlayer player, float frameInterp, float xRot,
                                   InteractionHand hand, float attack, ItemStack itemStack,
                                   float inverseArmHeight, PoseStack poseStack,
                                   SubmitNodeCollector collector, int lightCoords, CallbackInfo ci) {
        if (!ViewModelModule.isActive()) return;
        poseStack.popPose();
    }

    @Inject(method = "renderArmWithItem", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V"))
    private void viewmodel$transformItem(AbstractClientPlayer player, float frameInterp, float xRot,
                                         InteractionHand hand, float attack, ItemStack itemStack,
                                         float inverseArmHeight, PoseStack poseStack,
                                         SubmitNodeCollector collector, int lightCoords, CallbackInfo ci) {
        if (!ViewModelModule.isActive()) return;
        if (ViewModelModule.getTransformTarget() == 2) return;
        viewmodel$applyPosRot(hand, poseStack);
        float scale = hand == InteractionHand.MAIN_HAND ? ViewModelModule.getMainScale() : ViewModelModule.getOffScale();
        poseStack.scale(scale, scale, scale);
    }

    @Inject(method = "renderArmWithItem", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderPlayerArm(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;IFFLnet/minecraft/world/entity/HumanoidArm;)V"),
        cancellable = true)
    private void viewmodel$transformArm(AbstractClientPlayer player, float frameInterp, float xRot,
                                        InteractionHand hand, float attack, ItemStack itemStack,
                                        float inverseArmHeight, PoseStack poseStack,
                                        SubmitNodeCollector collector, int lightCoords, CallbackInfo ci) {
        if (!ViewModelModule.isActive()) return;
        if (ViewModelModule.isHideHands()) {
            ci.cancel();
            return;
        }
        if (ViewModelModule.getTransformTarget() == 1) return;
        viewmodel$applyPosRot(hand, poseStack);
    }

    @Unique
    private void viewmodel$applyPosRot(InteractionHand hand, PoseStack poseStack) {
        if (hand == InteractionHand.MAIN_HAND) {
            poseStack.mulPose(Axis.XP.rotationDegrees(ViewModelModule.getMainRotX()));
            poseStack.mulPose(Axis.YP.rotationDegrees(ViewModelModule.getMainRotY()));
            poseStack.mulPose(Axis.ZP.rotationDegrees(ViewModelModule.getMainRotZ()));
            poseStack.translate(ViewModelModule.getMainPosX() / 16f,
                ViewModelModule.getMainPosY() / 16f,
                ViewModelModule.getMainPosZ() / 16f);
        } else {
            poseStack.mulPose(Axis.XP.rotationDegrees(ViewModelModule.getOffRotX()));
            poseStack.mulPose(Axis.YP.rotationDegrees(ViewModelModule.getOffRotY()));
            poseStack.mulPose(Axis.ZP.rotationDegrees(ViewModelModule.getOffRotZ()));
            poseStack.translate(ViewModelModule.getOffPosX() / 16f,
                ViewModelModule.getOffPosY() / 16f,
                ViewModelModule.getOffPosZ() / 16f);
        }
    }

    @ModifyArg(method = "renderHandsWithItems(FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/player/LocalPlayer;I)V",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderArmWithItem(Lnet/minecraft/client/player/AbstractClientPlayer;FFLnet/minecraft/world/InteractionHand;FLnet/minecraft/world/item/ItemStack;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V",
            ordinal = 0), index = 4)
    private float viewmodel$mainSwingProgress(float swing) {
        if (!ViewModelModule.isActive()) return swing;
        LocalPlayer player = Minecraft.getInstance().player;
        if (ViewModelModule.isSwordSlash() && player != null
            && player.getMainHandItem().is(ItemTags.SWORDS)) return 0f;
        return swing + ViewModelModule.getMainSwingOffset();
    }

    @ModifyArg(method = "renderHandsWithItems(FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/player/LocalPlayer;I)V",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderArmWithItem(Lnet/minecraft/client/player/AbstractClientPlayer;FFLnet/minecraft/world/InteractionHand;FLnet/minecraft/world/item/ItemStack;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V",
            ordinal = 1), index = 4)
    private float viewmodel$offSwingProgress(float swing) {
        if (!ViewModelModule.isActive()) return swing;
        return swing + ViewModelModule.getOffSwingOffset();
    }

    @Inject(method = "shouldInstantlyReplaceVisibleItem(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)Z",
        at = @At("RETURN"), cancellable = true)
    private void viewmodel$skipSwap(ItemStack currentlyVisibleItem, ItemStack expectedItem,
                                    CallbackInfoReturnable<Boolean> cir) {
        if (ViewModelModule.isActive() && ViewModelModule.isSkipEquip()) {
            cir.setReturnValue(true);
        }
    }

    @ModifyArg(method = "tick", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/util/Mth;clamp(FFF)F", ordinal = 2), index = 0)
    private float viewmodel$mainEquipProgress(float value) {
        if (!ViewModelModule.isActive()) return value;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return value;

        ItemStack currentStack = mc.player.getMainHandItem();
        if (ViewModelModule.isOldAnimations() && !ViewModelModule.isSkipEquip()) {
            mainHandItem = currentStack;
        }

        float progress = ViewModelModule.isOldAnimations()
            ? 1f : (float) Math.pow(mc.player.getItemSwapScale(1f), 3);

        return (ItemStack.matches(mainHandItem, currentStack) ? progress : 0) - mainHandHeight;
    }

    @ModifyArg(method = "tick", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/util/Mth;clamp(FFF)F", ordinal = 3), index = 0)
    private float viewmodel$offEquipProgress(float value) {
        if (!ViewModelModule.isActive() || !ViewModelModule.isSkipEquip()) return value;
        return 1f - offHandHeight;
    }
}
