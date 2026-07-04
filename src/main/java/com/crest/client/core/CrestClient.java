package com.crest.client.core;

import com.crest.client.bongocat.BongoCatConfig;
import com.crest.client.bongocat.BongoCatEditScreen;
import com.crest.client.bongocat.BongoCatModule;
import com.crest.client.music.MusicModule;
import com.crest.client.music.MusicScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

import static org.lwjgl.glfw.GLFW.glfwGetCurrentContext;
import static org.lwjgl.glfw.GLFW.glfwGetKey;

public class CrestClient implements ClientModInitializer {
    private static final Identifier HUD_LAYER = Identifier.fromNamespaceAndPath("crest-client", "hud_renderer");
    private static boolean wasGraveDown = false;
    private static boolean wasBDown = false;
    private static boolean wasMDown = false;
    private static boolean wasAltDown = false;

    @Override
    public void onInitializeClient() {
        CrestModules.register(new FullbrightModule());
        CrestModules.register(new ZoomModule());
        CrestModules.register(new CoordsModule());
        CrestModules.register(new FpsModule());
        CrestModules.register(new ArmorHudModule());
        CrestModules.register(new PotionHudModule());
        CrestModules.register(new CrosshairModule());
        CrestModules.register(new BongoCatModule());

        MusicModule.init();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            long window = glfwGetCurrentContext();
            if (window == 0) return;

            boolean isDown = glfwGetKey(window, GLFW.GLFW_KEY_GRAVE_ACCENT) == GLFW.GLFW_PRESS;
            if (isDown && !wasGraveDown) {
                if (client.screen instanceof CrestClickGui) {
                    client.screen.onClose();
                } else if (client.screen == null) {
                    client.setScreen(new CrestClickGui());
                }
            }
            wasGraveDown = isDown;

            boolean bDown = glfwGetKey(window, GLFW.GLFW_KEY_B) == GLFW.GLFW_PRESS;
            if (bDown && !wasBDown) {
                boolean newState = !CrestModules.isEnabled("fullbright");
                CrestModules.setEnabled("fullbright", newState);
                CrestModule fb = CrestModules.get("fullbright");
                if (client.player != null && fb instanceof FullbrightModule fbm) {
                    client.player.sendOverlayMessage(
                        Component.literal(newState
                            ? "Gamma: " + fbm.getGammaLevel() + "%"
                            : "Gamma: OFF")
                    );
                }
            }
            wasBDown = bDown;

            boolean mDown = glfwGetKey(window, GLFW.GLFW_KEY_M) == GLFW.GLFW_PRESS;
            if (mDown && !wasMDown) {
                if (client.screen instanceof MusicScreen) {
                    client.screen.onClose();
                } else if (client.screen == null) {
                    client.setScreen(new MusicScreen(MusicModule.getPlayer()));
                }
            }
            wasMDown = mDown;

            boolean altDown = glfwGetKey(window, GLFW.GLFW_KEY_LEFT_ALT) == GLFW.GLFW_PRESS;
            if (altDown && !wasAltDown) {
                if (client.screen instanceof BongoCatEditScreen) {
                    client.screen.onClose();
                } else if (client.screen == null) {
                    BongoCatConfig.reload();
                    client.setScreen(new BongoCatEditScreen(null));
                }
            }
            wasAltDown = altDown;
        });

        HudElementRegistry.attachElementBefore(
            VanillaHudElements.CHAT,
            HUD_LAYER,
            (GuiGraphicsExtractor g, DeltaTracker d) -> renderHud(g, d)
        );
    }

    private static void renderHud(GuiGraphicsExtractor g, DeltaTracker d) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getWindow() == null) return;
        if (mc.screen instanceof CrestClickGui) return;

        for (CrestModule mod : CrestModules.getAll().values()) {
            if (!CrestModules.isEnabled(mod.getId())) continue;
            if (mod instanceof RenderableModule renderable) {
                try {
                    renderable.render(g, mc, d);
                } catch (Exception e) {
                    System.err.println("[Crest] HUD render error in " + mod.getId() + ": " + e);
                }
            }
        }
    }
}
