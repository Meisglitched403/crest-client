package com.crest.client.core;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public interface RenderableModule {
    void render(GuiGraphicsExtractor g, Minecraft mc, DeltaTracker d);
}
