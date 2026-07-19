package com.crest.client.core;

import com.crest.client.core.setting.*;

import java.util.List;

public class ChatHeadsModule implements CrestModule {
    private final IntegerSetting headSize = new IntegerSetting("Head Size", 6, 20, 10);
    private final BooleanSetting showOverlay = new BooleanSetting("Show in Overlay", true);
    private final BooleanSetting showChat = new BooleanSetting("Show in Fullscreen", true);

    @Override public String getId() { return "chat_heads"; }
    @Override public String getName() { return "Chat Heads"; }
    @Override public String getDescription() { return "Shows player heads next to chat messages"; }
    @Override public String getCategory() { return "Visual"; }
    @Override public boolean isEnabled() { return true; }

    @Override
    public List<Setting<?>> getSettings() {
        return List.of(headSize, showOverlay, showChat);
    }

    public static int getHeadSize() {
        ChatHeadsModule m = getModule();
        return m != null ? m.headSize.get() : 10;
    }
    public static boolean showInOverlay() {
        ChatHeadsModule m = getModule();
        return m == null || m.showOverlay.get();
    }
    public static boolean showInChat() {
        ChatHeadsModule m = getModule();
        return m == null || m.showChat.get();
    }

    private static ChatHeadsModule getModule() {
        CrestModule m = CrestModules.get("chat_heads");
        return m instanceof ChatHeadsModule chm ? chm : null;
    }
}
