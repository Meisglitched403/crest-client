package com.crest.client.core;

import com.crest.client.core.setting.KeybindSetting;
import com.crest.client.core.setting.Setting;
import com.crest.client.core.setting.StringSetting;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

/**
 * ponytail: AutoTextHotkey. Sends a configured chat message (or command when the
 * text starts with "/") when its keybind is pressed. Reads keybinds live each
 * client tick so keybind changes apply immediately.
 */
public class AutoTextHotkeyModule implements CrestModule {
    private static final int SLOTS = 5;

    private final List<KeybindSetting> keys = new ArrayList<>();
    private final List<StringSetting> messages = new ArrayList<>();

    public AutoTextHotkeyModule() {
        for (int i = 0; i < SLOTS; i++) {
            keys.add(new KeybindSetting("Hotkey " + (i + 1), org.lwjgl.glfw.GLFW.GLFW_KEY_UNKNOWN));
            messages.add(new StringSetting("Message " + (i + 1), "", "/help"));
        }
    }

    @Override public String getId() { return "autotext"; }
    @Override public String getName() { return "Auto Text Hotkey"; }
    @Override public String getDescription() { return "Sends a chat message or command when a key is pressed."; }
    @Override public String getCategory() { return "Utility"; }
    @Override public boolean isEnabled() { return true; }

    @Override
    public List<Setting<?>> getSettings() {
        List<Setting<?>> s = new ArrayList<>();
        for (int i = 0; i < SLOTS; i++) {
            s.add(keys.get(i));
            s.add(messages.get(i));
        }
        return s;
    }

    @Override
    public void onInitialize() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            for (int i = 0; i < SLOTS; i++) {
                if (keys.get(i).wasPressed()) send(i);
            }
        });
    }

    private void send(int idx) {
        String msg = messages.get(idx).get();
        if (msg == null || msg.isBlank()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() == null || mc.player == null) return;
        if (msg.startsWith("/")) {
            mc.getConnection().sendCommand(msg.substring(1));
        } else {
            mc.getConnection().sendChat(msg);
        }
    }
}
