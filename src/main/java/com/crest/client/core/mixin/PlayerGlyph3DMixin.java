package com.crest.client.core.mixin;

import com.crest.client.core.ChatHeadsModule;
import com.crest.client.core.CrestModules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(targets = "net.minecraft.client.gui.font.PlayerGlyphProvider$Instance")
public class PlayerGlyph3DMixin {
    private static float ness() {
        return CrestModules.isEnabled("chat_heads") ? Math.min(ChatHeadsModule.getNess(), 2f) : 0;
    }

    @ModifyArg(method = "renderSprite", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/font/PlayerGlyphProvider$Instance;renderQuad(Lorg/joml/Matrix4fc;Lcom/mojang/blaze3d/vertex/VertexConsumer;IFFFFFIFFIIII)V", ordinal = 0), index = 3)
    private float crest$faceLeft(float left) { return left - ness(); }

    @ModifyArg(method = "renderSprite", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/font/PlayerGlyphProvider$Instance;renderQuad(Lorg/joml/Matrix4fc;Lcom/mojang/blaze3d/vertex/VertexConsumer;IFFFFFIFFIIII)V", ordinal = 0), index = 4)
    private float crest$faceRight(float right) { return right + ness(); }

    @ModifyArg(method = "renderSprite", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/font/PlayerGlyphProvider$Instance;renderQuad(Lorg/joml/Matrix4fc;Lcom/mojang/blaze3d/vertex/VertexConsumer;IFFFFFIFFIIII)V", ordinal = 1), index = 3)
    private float crest$hatLeft(float left) { return left - ness(); }

    @ModifyArg(method = "renderSprite", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/font/PlayerGlyphProvider$Instance;renderQuad(Lorg/joml/Matrix4fc;Lcom/mojang/blaze3d/vertex/VertexConsumer;IFFFFFIFFIIII)V", ordinal = 1), index = 4)
    private float crest$hatRight(float right) { return right + ness(); }

    @ModifyArg(method = "renderSprite", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/font/PlayerGlyphProvider$Instance;renderQuad(Lorg/joml/Matrix4fc;Lcom/mojang/blaze3d/vertex/VertexConsumer;IFFFFFIFFIIII)V", ordinal = 1), index = 5)
    private float crest$hatTop(float top) { return top + 2; }

    @ModifyArg(method = "renderSprite", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/font/PlayerGlyphProvider$Instance;renderQuad(Lorg/joml/Matrix4fc;Lcom/mojang/blaze3d/vertex/VertexConsumer;IFFFFFIFFIIII)V", ordinal = 1), index = 6)
    private float crest$hatBottom(float bottom) { return bottom + 2; }
}
