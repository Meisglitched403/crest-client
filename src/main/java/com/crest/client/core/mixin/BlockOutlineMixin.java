package com.crest.client.core.mixin;

import com.crest.client.core.BlockOutlineModule;
import com.crest.client.core.CrestModules;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.state.level.BlockOutlineRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(LevelRenderer.class)
public class BlockOutlineMixin {
    @ModifyArg(
        method = "renderBlockOutline",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/LevelRenderer;renderHitOutline(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;DDDLnet/minecraft/client/renderer/state/level/BlockOutlineRenderState;IF)V"
        ),
        index = 6
    )
    private int crest$modifyOutlineColor(int color) {
        if (!CrestModules.isEnabled("block_outline")) return color;
        int customColor = BlockOutlineModule.getColor();
        return customColor & 0x00FFFFFF;
    }
}
