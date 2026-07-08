package com.crest.client.core;

public class CrestThemeModule implements CrestModule {
    @Override public String getId() { return "ui_theme"; }
    @Override public String getName() { return "UI Theme"; }
    @Override public String getDescription() { return "Restyles vanilla Minecraft GUI with the client's glassmorphic theme"; }
    @Override public String getCategory() { return "Visual"; }
    @Override public boolean isEnabled() { return true; }
}
