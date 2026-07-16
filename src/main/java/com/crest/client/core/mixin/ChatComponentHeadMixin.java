package com.crest.client.core.mixin;

import com.crest.client.core.ChatHeadsModule;
import com.crest.client.core.CrestModules;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.objects.PlayerSprite;
import net.minecraft.world.item.component.ResolvableProfile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(ChatComponent.class)
public class ChatComponentHeadMixin {
    @ModifyArg(
        method = "addPlayerMessage",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/ChatComponent;addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageSource;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V"),
        index = 0
    )
    private Component crest$prependHead(Component message) {
        if (!CrestModules.isEnabled("chat_heads")) return message;
        GameProfile profile = ChatHeadsModule.getLastProfile();
        if (profile == null) return message;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && profile.id().equals(mc.player.getUUID())) return message;
        ResolvableProfile resolvable = ResolvableProfile.createResolved(profile);
        PlayerSprite sprite = new PlayerSprite(resolvable, ChatHeadsModule.showHat());
        Component head = Component.object(sprite).withStyle(Style.EMPTY.withColor(0xFFFFFF));
        ChatHeadsModule.setLastProfile(null);
        return Component.literal("").append(head).append(Component.literal(" ")).append(message);
    }
}
