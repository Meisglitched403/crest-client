package com.crest.client.core;

import net.minecraft.client.gui.screens.Screen;

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
    default Screen createConfigScreen(Screen parent) { return null; }
}
