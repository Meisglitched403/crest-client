package com.crest.client.core;

import com.crest.client.core.setting.*;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.List;

public class NameTagsModule implements CrestModule {
    private final FloatSetting scale = new FloatSetting(
        "Scale", 0.5f, 3.0f, 1.0f
    );
    private final BooleanSetting alwaysVisible = new BooleanSetting(
        "Always Visible", true
    );
    private final ColorSetting textColor = new ColorSetting(
        "Text Color", 0xFFFFFFFF
    );
    private final KeybindSetting toggleKey = new KeybindSetting(
        "Toggle Key", org.lwjgl.glfw.GLFW.GLFW_KEY_UNKNOWN
    );

    @Override
    public String getId() { return "name_tags"; }
    @Override
    public String getName() { return "Name Tags"; }
    @Override
    public String getDescription() { return "Customize entity name tags: scale, always visible, color"; }
    @Override
    public String getCategory() { return "Visual"; }
    @Override
    public boolean isEnabled() { return false; }

    @Override
    public List<Setting<?>> getSettings() {
        return List.of(scale, alwaysVisible, textColor, toggleKey);
    }

    public static float getScale() {
        CrestModule m = CrestModules.get("name_tags");
        return m instanceof NameTagsModule n ? n.scale.get() : 1.0f;
    }

    public static boolean isAlwaysVisible() {
        CrestModule m = CrestModules.get("name_tags");
        return m instanceof NameTagsModule n && n.alwaysVisible.get();
    }

    public static int getTextColor() {
        CrestModule m = CrestModules.get("name_tags");
        return m instanceof NameTagsModule n ? n.textColor.get() : 0xFFFFFFFF;
    }

    public static Component applyColor(Component component) {
        if (!CrestModules.isEnabled("name_tags")) return component;
        int color = getTextColor();
        int rgb = color & 0x00FFFFFF;
        MutableComponent colored = component.copy().withStyle(
            Style.EMPTY.withColor(rgb)
        );
        return colored;
    }
}
