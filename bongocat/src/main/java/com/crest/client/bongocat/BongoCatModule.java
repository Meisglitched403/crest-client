package com.crest.client.bongocat;

import com.crest.client.core.CrestModule;
import com.crest.client.core.CrestModules;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.DeltaTracker;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

import static org.lwjgl.glfw.GLFW.glfwGetCurrentContext;
import static org.lwjgl.glfw.GLFW.glfwGetKey;

public class BongoCatModule implements ClientModInitializer, CrestModule {
    private static final Identifier CAT_LAYER = Identifier.fromNamespaceAndPath("crest-bongocat", "cat_overlay");
    private static boolean wasAltDown = false;

    @Override
    public String getId() { return "bongocat"; }

    @Override
    public String getName() { return "Bongo Cat"; }

    @Override
    public void onInitializeClient() {
        CrestModules.register(this);
    }

    @Override
    public void onInitialize() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            long window = glfwGetCurrentContext();
            if (window == 0) return;

            boolean isDown = glfwGetKey(window, GLFW.GLFW_KEY_LEFT_ALT) == GLFW.GLFW_PRESS;
            if (isDown && !wasAltDown) {
                if (client.screen instanceof BongoCatEditScreen) {
                    client.screen.onClose();
                } else if (client.screen == null) {
                    BongoCatConfig.reload();
                    client.setScreen(new BongoCatEditScreen(null));
                }
            }
            wasAltDown = isDown;
        });

        HudElementRegistry.attachElementBefore(
            VanillaHudElements.CHAT,
            CAT_LAYER,
            this::render
        );
    }

    private void render(GuiGraphicsExtractor g, DeltaTracker d) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getWindow() == null) return;
        if (mc.screen instanceof BongoCatEditScreen) return;

        InputTracker input = InputTracker.getInstance();
        if (!input.tryInit()) return;

        input.update();

        BongoCatOverlay.render(g, mc, input, BongoCatConfig.getInstance());
    }
}
