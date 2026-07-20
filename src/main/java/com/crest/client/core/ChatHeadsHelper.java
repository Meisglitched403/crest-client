package com.crest.client.core;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.regex.Pattern;

public final class ChatHeadsHelper {
    private static final Pattern FORMAT_CODE = Pattern.compile("(?i)§[0-9a-fk-or]");

    public static GuiGraphicsExtractor guiGraphics;
    public static ChatComponent.ChatGraphicsAccess chatGraphicsAccess;

    public static boolean shouldRenderHeads() {
        if (!CrestModules.isEnabled("chat_heads")) return false;
        boolean focused = Minecraft.getInstance().screen instanceof net.minecraft.client.gui.screens.ChatScreen;
        return focused ? ChatHeadsModule.showInChat() : ChatHeadsModule.showInOverlay();
    }

    public static boolean shouldRenderInTabList() {
        return CrestModules.isEnabled("chat_heads") && ChatHeadsModule.showInTabList();
    }

    @Nullable
    public static PlayerInfo findOwner(Component content) {
        Minecraft mc = Minecraft.getInstance();
        ClientPacketListener conn = mc.getConnection();
        if (conn == null) return null;

        String plain = stripFormatting(content.getString());
        if (plain.isEmpty()) return null;

        Map<String, PlayerInfo> nameToPlayer = new HashMap<>();
        Map<Integer, List<String>> namesByFirstChar = new HashMap<>();

        for (PlayerInfo info : conn.getOnlinePlayers()) {
            addNameEntry(info, stripFormatting(info.getProfile().name()), nameToPlayer, namesByFirstChar);
            Component dn = info.getTabListDisplayName();
            if (dn != null) {
                String display = stripFormatting(dn.getString());
                addNameEntry(info, display, nameToPlayer, namesByFirstChar);
            }
        }

        return scanForPlayerName(plain, namesByFirstChar, nameToPlayer);
    }

    private static void addNameEntry(PlayerInfo info, String name, Map<String, PlayerInfo> nameToPlayer, Map<Integer, List<String>> namesByFirstChar) {
        if (name.isEmpty() || nameToPlayer.containsKey(name)) return;
        nameToPlayer.put(name, info);
        namesByFirstChar.computeIfAbsent(name.codePointAt(0), k -> new ArrayList<>()).add(name);
    }

    @Nullable
    private static PlayerInfo scanForPlayerName(String message, Map<Integer, List<String>> namesByFirstChar, Map<String, PlayerInfo> nameToPlayer) {
        int[] chars = message.codePoints().toArray();
        boolean insideWord = false;
        PlayerInfo best = null;
        int bestLen = 0;

        for (int i = 0; i < chars.length; i++) {
            int c = chars[i];

            if (insideWord && isWordCharacter(c)) continue;

            List<String> nameList = namesByFirstChar.get(c);
            if (nameList != null) {
                for (String name : nameList) {
                    int[] nameChars = name.codePoints().toArray();
                    if (nameChars.length <= bestLen) continue;
                    if (i + nameChars.length > chars.length) continue;

                    boolean nameEndsAsWord = isWordCharacter(nameChars[nameChars.length - 1]);
                    boolean nameFollowedByWord = i + nameChars.length < chars.length && isWordCharacter(chars[i + nameChars.length]);
                    if (nameEndsAsWord && nameFollowedByWord) continue;

                    boolean match = true;
                    for (int j = 0; j < nameChars.length; j++) {
                        if (Character.toLowerCase(chars[i + j]) != Character.toLowerCase(nameChars[j])) {
                            match = false;
                            break;
                        }
                    }

                    if (match) {
                        PlayerInfo info = nameToPlayer.get(name);
                        if (info != null && nameChars.length > bestLen) {
                            best = info;
                            bestLen = nameChars.length;
                        }
                    }
                }
            }

            insideWord = isWordCharacter(c);
        }

        return best;
    }

    public static void renderHead(GuiGraphicsExtractor g, int x, int y, PlayerInfo owner, int headSize) {
        Identifier skin = owner.getSkin().body().texturePath();
        boolean upsideDown = false;
        if (Minecraft.getInstance().level != null) {
            Player p = Minecraft.getInstance().level.getPlayerByUUID(owner.getProfile().id());
            if (p != null) upsideDown = AvatarRenderer.isPlayerUpsideDown(p);
        }
        PlayerFaceExtractor.extractRenderState(g, skin, x, y, headSize, owner.showHat(), upsideDown, -1);
    }

    public static int headWidth(int headSize) {
        return headSize + 2;
    }

    static String stripFormatting(String s) {
        return FORMAT_CODE.matcher(s).replaceAll("").trim();
    }

    static boolean isWordCharacter(int cp) {
        return Character.isLetterOrDigit(cp) || cp == '_' || Character.getNumericValue(cp) != -1;
    }

    private ChatHeadsHelper() {}
}
