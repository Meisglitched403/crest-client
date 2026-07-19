package com.crest.client.core.mixin;

import com.crest.client.core.ChatHeadsModule;
import com.crest.client.core.CrestModules;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.client.multiplayer.chat.GuiMessageSource;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.SkinManager;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mixin(ChatComponent.class)
public class ChatHeadsMixin {
    @Shadow
    private List<GuiMessage.Line> trimmedMessages;

    @Shadow
    private int chatScrollbarPos;

    // Cache of name -> resolved skin, so we don't hit the session service every frame.
    private static final Map<String, PlayerSkin> skinCache = new ConcurrentHashMap<>();
    private static final Map<String, CompletableFuture<Void>> skinFutures = new ConcurrentHashMap<>();

    private static final Pattern PLAYER_PATTERN = Pattern.compile("^(?:<([^>]+)>|([\\w\\p{L}._\\-]+)[\\s:»\\-]+)");

    /**
     * Inject at RETURN of the public extractRenderState (after popMatrix, so the pose
     * is the clean HUD base). Draw heads in plain gui coordinates using vanilla's exact
     * line-Y formula, matching ChatComponent's own layout.
     */
    @Inject(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;IIILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;Z)V",
            at = @At("RETURN"))
    private void crest$renderHeads(GuiGraphicsExtractor graphics, Font font, int tickCount, int mouseX, int mouseY, ChatComponent.DisplayMode mode, boolean focused, CallbackInfo ci) {
        if (!CrestModules.isEnabled("chat_heads")) return;
        if (!mode.foreground && !ChatHeadsModule.showInOverlay()) return;
        if (mode.foreground && !ChatHeadsModule.showInChat()) return;
        if (trimmedMessages == null || trimmedMessages.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        ClientPacketListener conn = mc.getConnection();
        if (conn == null) return;

        double chatScale = mc.options.chatScale().get();
        float scale = (float) (chatScale > 0 ? chatScale : 1.0);
        int guiHeight = graphics.guiHeight();
        int lineH = Math.round(9f * (1f + mc.options.chatLineSpacing().get().floatValue()));
        if (lineH <= 0) lineH = 9;

        int chatBottomY = Mth.floor((guiHeight - 40) / scale);

        int linesPerPage = Mth.floor((guiHeight - 40) / (lineH * scale));
        if (linesPerPage <= 0) linesPerPage = 1;
        int visibleLines = Math.min(trimmedMessages.size() - chatScrollbarPos, linesPerPage);
        if (visibleLines <= 0) return;

        int headSize = ChatHeadsModule.getHeadSize();

        for (int v = 0; v < visibleLines; v++) {
            int trimmedIdx = chatScrollbarPos + v;
            if (trimmedIdx >= trimmedMessages.size()) break;

            GuiMessage.Line line = trimmedMessages.get(trimmedIdx);
            GuiMessage msg = line.parent();
            if (msg == null || msg.source() != GuiMessageSource.PLAYER) continue;

            String plain = stripCodes(msg.content().getString());
            Matcher m = PLAYER_PATTERN.matcher(plain);
            if (!m.find()) continue;
            String name = m.group(1) != null ? m.group(1) : m.group(2);
            if (name == null || name.isEmpty()) continue;

            Identifier skinId = resolveSkin(mc, conn, name);
            if (skinId == null) continue;

            int lineTop = chatBottomY - (v + 1) * lineH;
            int headY = lineTop + (lineH - headSize) / 2;
            int headX = 4;

            graphics.blit(RenderPipelines.GUI_TEXTURED, skinId, headX, headY, 8f, 8f, headSize, headSize, 64, 64);
            graphics.blit(RenderPipelines.GUI_TEXTURED, skinId, headX, headY, 40f, 8f, headSize, headSize, 64, 64);
        }
    }

    /** Players in the tab list resolve instantly; otherwise fall back to the skin service by name. */
    private static Identifier resolveSkin(Minecraft mc, ClientPacketListener conn, String name) {
        PlayerInfo info = conn.getPlayerInfoIgnoreCase(name);
        if (info != null) {
            Identifier id = info.getSkin().body().texturePath();
            if (id != null) return id;
        }
        PlayerSkin cached = skinCache.get(name);
        if (cached != null) {
            Identifier id = cached.body().texturePath();
            return id != null ? id : null;
        }
        // Kick off an async resolution if not already pending.
        skinFutures.computeIfAbsent(name, n -> {
            SkinManager sm = mc.getSkinManager();
            return sm.get(new GameProfile(null, n)).thenAccept(opt -> opt.ifPresent(skin -> skinCache.put(n, skin)));
        });
        return null;
    }

    private static String stripCodes(String s) {
        // Remove legacy §x color codes.
        return s.replaceAll("(?i)§[0-9a-fk-or]", "");
    }
}
