package com.crest.client.core.mixin;

import com.crest.client.core.ChatHeadsHelper;
import com.crest.client.core.ChatHeadsModule;
import com.crest.client.core.CrestBrandManager;
import com.crest.client.core.CrestModules;
import com.crest.client.core.CrestNametagModule;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import java.util.UUID;

@Mixin(PlayerTabOverlay.class)
public abstract class PlayerTabOverlayMixin {
    private static final String LOGO_CHAR = "\uE000";
    private static int crest$tabOffset;

    @ModifyArgs(
            method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;ILnet/minecraft/world/scores/Scoreboard;Lnet/minecraft/world/scores/Objective;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/components/PlayerFaceExtractor;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/resources/Identifier;IIIZZI)V"
            )
    )
    private void crest$addHead(Args args,
            @Local(type = PlayerInfo.class, ordinal = 0) PlayerInfo owner) {
        if (!ChatHeadsHelper.shouldRenderInTabList() || owner == null) {
            crest$tabOffset = 0;
            return;
        }

        GuiGraphicsExtractor guiGraphics = args.get(0);
        int x = args.get(2);
        int y = args.get(3);
        int headSize = ChatHeadsModule.getHeadSize();
        int offset = ChatHeadsHelper.headWidth(headSize);

        ChatHeadsHelper.renderHead(guiGraphics, x - offset, y, owner, headSize);
        crest$tabOffset = offset;
    }

    @ModifyArg(
            method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;ILnet/minecraft/world/scores/Scoreboard;Lnet/minecraft/world/scores/Objective;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)V"
            ),
            index = 2
    )
    private int crest$shiftNameX(int originalX) {
        return crest$tabOffset > 0 ? originalX + crest$tabOffset : originalX;
    }

    @ModifyReturnValue(method = "getNameForDisplay", at = @At("RETURN"))
    private Component crest$prependLogoToTabList(Component original, PlayerInfo info) {
        if (!CrestModules.isEnabled("crest_nametag")) return original;
        if (!CrestBrandManager.isInitialized()) return original;
        if (info == null || info.getProfile() == null) return original;

        UUID uuid = info.getProfile().id();
        if (CrestBrandManager.isCrestUser(uuid)) {
            if (CrestNametagModule.isLogoPositionLeft()) {
                return Component.literal(LOGO_CHAR + " ").append(original);
            } else {
                return Component.literal("").append(original).append(Component.literal(" " + LOGO_CHAR));
            }
        }
        return original;
    }
}
