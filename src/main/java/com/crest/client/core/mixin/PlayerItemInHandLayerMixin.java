package com.crest.client.core.mixin;

import com.crest.client.core.ShieldFocusedPlayer;
import com.crest.client.core.ShieldStatusModule;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.PlayerItemInHandLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerItemInHandLayer.class)
public class PlayerItemInHandLayerMixin {

    /**
     * Record which player owns the held-item render state BEFORE the shield
     * model is baked. The vanilla super call submits the ItemStackRenderState
     * (triggering ShieldLayerRenderStateMixin) internally, so injecting at HEAD
     * guarantees the owner map is populated first.
     */
    @Inject(method = "submitArmWithItem(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;Lnet/minecraft/client/renderer/item/ItemStackRenderState;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/HumanoidArm;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V",
            at = @At("HEAD"))
    public <S extends AvatarRenderState> void crest$setFocused(AvatarRenderState state, ItemStackRenderState item,
                                                               net.minecraft.world.item.ItemStack itemStack,
                                                               net.minecraft.world.entity.HumanoidArm arm,
                                                               PoseStack poseStack, SubmitNodeCollector collector,
                                                               int lightCoords, CallbackInfo ci) {
        if (!ShieldStatusModule.isActive()) return;
        Level level = Minecraft.getInstance().level;
        if (level == null) return;
        Entity entity = level.getEntity(state.id);
        if (entity instanceof Player player) {
            ShieldFocusedPlayer.set(item, player);
        }
    }

    /**
     * Blocking-to-eye path renders the shield from state.heldOnHead via a
     * separate method that never calls submitArmWithItem, so record the owner
     * here too (at HEAD, before the bake).
     */
    @Inject(method = "renderItemHeldToEye", at = @At("HEAD"))
    private <S extends AvatarRenderState> void crest$setFocusedHeldToEye(S state, net.minecraft.world.entity.HumanoidArm arm,
                                                                          PoseStack poseStack, SubmitNodeCollector collector,
                                                                          int lightCoords, CallbackInfo ci) {
        if (!ShieldStatusModule.isActive()) return;
        Level level = Minecraft.getInstance().level;
        if (level == null) return;
        Entity entity = level.getEntity(state.id);
        if (entity instanceof Player player) {
            ShieldFocusedPlayer.set(state.heldOnHead, player);
        }
    }
}
