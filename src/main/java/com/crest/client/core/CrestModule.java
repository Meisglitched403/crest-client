package com.crest.client.core;

import com.crest.client.core.setting.Setting;
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
}
