package com.crest.client.core;

import com.crest.client.core.setting.*;

import java.util.List;

public class ChatHeadsModule implements CrestModule {
    private final IntegerSetting headSize = new IntegerSetting("Head Size", 6, 16, 10);
    private final BooleanSetting showOverlay = new BooleanSetting("Show in Overlay", true);
    private final BooleanSetting showChat = new BooleanSetting("Show in Fullscreen", true);
    private final BooleanSetting showTabList = new BooleanSetting("Show in Tab List", true);

    static ChatHeadsModule INSTANCE;

    @Override public String getId() { return "chat_heads"; }
    @Override public String getName() { return "Chat Heads"; }
    @Override public String getDescription() { return "Shows player heads next to chat messages"; }
    @Override public String getCategory() { return "Visual"; }
    @Override public boolean isEnabled() { return true; }

    @Override
    public void onInitialize() { INSTANCE = this; }

    @Override
    public List<Setting<?>> getSettings() {
        return List.of(headSize, showOverlay, showChat, showTabList);
    }

    public static int getHeadSize() {
        ChatHeadsModule m = INSTANCE;
        return m != null ? m.headSize.get() : 10;
    }
    public static boolean showInOverlay() {
        ChatHeadsModule m = INSTANCE;
        return m == null || m.showOverlay.get();
    }
    public static boolean showInChat() {
        ChatHeadsModule m = INSTANCE;
        return m == null || m.showChat.get();
    }
    public static boolean showInTabList() {
        ChatHeadsModule m = INSTANCE;
        return m == null || m.showTabList.get();
    }
}
