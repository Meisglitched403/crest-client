package com.crest.client.core;

import com.crest.client.core.setting.IntegerSetting;
import com.crest.client.core.setting.BooleanSetting;
import com.crest.client.core.setting.Setting;
import com.mojang.authlib.GameProfile;

import java.util.List;

public class ChatHeadsModule implements CrestModule {
    private final BooleanSetting showHat = new BooleanSetting("Show Hat", true);
    private final IntegerSetting threeDeeNess = new IntegerSetting("3D Depth", 0, 5, 2);

    private static GameProfile lastProfile;

    @Override public String getId() { return "chat_heads"; }
    @Override public String getName() { return "Chat Heads"; }
    @Override public String getDescription() { return "Shows player face icons next to chat messages"; }
    @Override public String getCategory() { return "Visual"; }
    @Override public boolean isEnabled() { return false; }

    @Override
    public List<Setting<?>> getSettings() {
        return List.of(showHat, threeDeeNess);
    }

    public static void setLastProfile(GameProfile profile) { lastProfile = profile; }
    public static GameProfile getLastProfile() { return lastProfile; }

    public static boolean showHat() {
        var m = CrestModules.get("chat_heads");
        return !(m instanceof ChatHeadsModule ch) || ch.showHat.get();
    }

    public static float getNess() {
        var m = CrestModules.get("chat_heads");
        if (!(m instanceof ChatHeadsModule ch)) return 0;
        return ch.threeDeeNess.get() * 2f;
    }
}
