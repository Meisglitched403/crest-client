package com.crest.client.core;

import com.crest.client.core.setting.*;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.List;

public class BlockOutlineModule implements CrestModule {
    private final ColorSetting outlineColor = new ColorSetting(
        "Outline Color", 0xFFFFFFFF
    );
    private final FloatSetting outlineWidth = new FloatSetting(
        "Outline Width", 1.0f, 5.0f, 2.0f
    );
    private final ModeSetting mode = new ModeSetting(
        "Mode", new String[]{"Outline", "Full", "Both"}, 0
    );
    private final KeybindSetting toggleKey = new KeybindSetting(
        "Toggle Key", org.lwjgl.glfw.GLFW.GLFW_KEY_UNKNOWN
    );

    @Override
    public String getId() { return "block_outline"; }
    @Override
    public String getName() { return "Block Outline"; }
    @Override
    public String getDescription() { return "Custom block highlight with configurable color and width"; }
    @Override
    public String getCategory() { return "Visual"; }
    @Override
    public boolean isEnabled() { return false; }

    @Override
    public List<Setting<?>> getSettings() {
        return List.of(outlineColor, outlineWidth, mode, toggleKey);
    }

    public static int getColor() {
        CrestModule m = CrestModules.get("block_outline");
        return m instanceof BlockOutlineModule b ? b.outlineColor.get() : 0xFFFFFFFF;
    }

    public static float getWidth() {
        CrestModule m = CrestModules.get("block_outline");
        return m instanceof BlockOutlineModule b ? b.outlineWidth.get() : 2.0f;
    }

    public static int getMode() {
        CrestModule m = CrestModules.get("block_outline");
        return m instanceof BlockOutlineModule b ? b.mode.get() : 0;
    }
}
