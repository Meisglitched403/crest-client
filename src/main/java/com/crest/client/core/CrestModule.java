package com.crest.client.core;

import com.crest.client.core.setting.Setting;
import com.crest.client.core.setting.SettingGroup;
import net.minecraft.client.gui.screens.Screen;

import java.util.List;

public interface CrestModule {
    String getId();
    String getName();
    default String getDescription() { return ""; }
    default String getCategory() { return "Misc"; }
    default boolean isEnabled() { return true; }
    default void setEnabled(boolean enabled) {}
    default void onInitialize() {}
    default void onEnable() {}
    default void onDisable() {}
    default void loadSettings() {}
    default Screen createConfigScreen(Screen parent) { return null; }
    default List<Setting<?>> getSettings() { return List.of(); }
    default List<SettingGroup> getSettingGroups() { return List.of(); }
    /**
     * Return true when the module polls its own keybinds (e.g. action hotkeys like
     * record/stream/save). KeybindManager will then skip auto-binding those settings
     * to a module toggle, preventing one press from triggering both the action and
     * the module toggle.
     */
    default boolean selfHandlesKeybinds() { return false; }
}
