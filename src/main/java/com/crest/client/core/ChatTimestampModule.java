package com.crest.client.core;

import com.crest.client.core.setting.BooleanSetting;
import com.crest.client.core.setting.ColorSetting;
import com.crest.client.core.setting.IntegerSetting;
import com.crest.client.core.setting.Setting;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

/**
 * ponytail: Chat Timestamp. Prepends a [HH:MM] prefix to chat messages via
 * ChatTimestampMixin. No HUD; this is a passive message transform.
 */
public class ChatTimestampModule implements CrestModule {
    private final BooleanSetting enabled = new BooleanSetting("Enabled", true);
    private final ColorSetting color = new ColorSetting("Timestamp Color", 0xFF888888);
    private final IntegerSetting mode = new IntegerSetting("Format", 0, 2, 0);

    static ChatTimestampModule INSTANCE;

    @Override public String getId() { return "chat_timestamp"; }
    @Override public String getName() { return "Chat Timestamp"; }
    @Override public String getDescription() { return "Adds a [HH:MM] timestamp to chat messages."; }
    @Override public String getCategory() { return "Chat"; }
    @Override public boolean isEnabled() { return true; }

    @Override
    public void onInitialize() { INSTANCE = this; }

    @Override
    public List<Setting<?>> getSettings() {
        List<Setting<?>> s = new ArrayList<>();
        s.add(enabled);
        s.add(color);
        s.add(mode);
        return s;
    }

    public static boolean isOn() {
        var mod = INSTANCE;
        return mod != null && CrestModules.isEnabled("chat_timestamp") && mod.enabled.get();
    }

    public static int colorArgb() {
        var mod = INSTANCE;
        return mod != null ? mod.color.get() : 0xFF888888;
    }

    public static int formatMode() {
        var mod = INSTANCE;
        return mod != null ? mod.mode.get() : 0;
    }
}
