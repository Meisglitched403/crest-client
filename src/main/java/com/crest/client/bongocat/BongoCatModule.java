package com.crest.client.bongocat;

import com.crest.client.core.CrestModule;
import com.crest.client.core.CrestModules;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.DeltaTracker;
import net.minecraft.resources.Identifier;

public class BongoCatModule implements CrestModule {
    private static final Identifier CAT_LAYER = Identifier.fromNamespaceAndPath("crest-client", "cat_overlay");

    @Override
    public String getId() { return "bongocat"; }

    @Override
    public String getName() { return "Bongo Cat"; }

    @Override
    public void onInitialize() {
        HudElementRegistry.attachElementBefore(
            VanillaHudElements.CHAT,
            CAT_LAYER,
            this::render
        );
    }

    private void render(GuiGraphicsExtractor g, DeltaTracker d) {
        if (!CrestModules.isEnabled(getId())) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.getWindow() == null) return;
        if (mc.screen instanceof BongoCatEditScreen) return;

        InputTracker input = InputTracker.getInstance();
        if (!input.tryInit()) return;

        input.update();

        BongoCatOverlay.render(g, mc, input, BongoCatConfig.getInstance());
    }
}
