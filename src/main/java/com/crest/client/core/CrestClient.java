package com.crest.client.core;

import com.crest.client.bongocat.BongoCatConfig;
import com.crest.client.bongocat.BongoCatEditScreen;
import com.crest.client.bongocat.BongoCatModule;
import com.crest.client.core.event.TickEvent;
import com.crest.client.music.MusicModule;
import com.crest.client.music.MusicScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class CrestClient implements ClientModInitializer {
    private static final Identifier HUD_LAYER = Identifier.fromNamespaceAndPath("crest-client", "hud_renderer");

    @Override
    public void onInitializeClient() {
        CrestModules.init();

        CrestModules.register(new FullbrightModule());
        CrestModules.register(new ZoomModule());
        CrestModules.register(new CoordsModule());
        CrestModules.register(new FpsModule());
        CrestModules.register(new ArmorHudModule());
        CrestModules.register(new PotionHudModule());
        CrestModules.register(new CrosshairModule());
        CrestModules.register(new NoFogModule());
        CrestModules.register(new NoHurtCamModule());
        CrestModules.register(new DynamicFovModule());
        CrestModules.register(new LowFireModule());
        CrestModules.register(new EntityCullingModule());
        CrestModules.register(new MotionBlurModule());
        CrestModules.register(new BongoCatModule());
        CrestModules.register(new TimeChangerModule());
        CrestModules.register(new WeatherChangerModule());
        CrestModules.register(new NoOverlayModule());
        CrestModules.register(new ItemCounterModule());
        CrestModules.register(new ServerAddressModule());
        CrestModules.register(new ToggleNotificationsModule());
        CrestModules.register(new SpeedometerModule());
        CrestModules.register(new BlockOutlineModule());
        CrestModules.register(new NameTagsModule());
        CrestModules.register(new FreelookModule());
        CrestModules.register(new WaypointsModule());
        CrestModules.register(new AntiAfkModule());

        MusicModule.init();

        KeybindManager.registerAction(GLFW.GLFW_KEY_M, () -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen instanceof MusicScreen) {
                mc.screen.onClose();
            } else if (mc.screen == null) {
                mc.setScreen(new MusicScreen(MusicModule.getPlayer()));
            }
        });

        KeybindManager.registerAction(GLFW.GLFW_KEY_LEFT_ALT, () -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen instanceof BongoCatEditScreen) {
                mc.screen.onClose();
            } else if (mc.screen == null) {
                BongoCatConfig.reload();
                mc.setScreen(new BongoCatEditScreen(null));
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            CrestModules.getEventBus().post(new TickEvent(client));
            KeybindManager.processTick();
        });

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            CrestModules.getConfigManager().save();
            HudSettings.save();
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
        System.out.println("[CrestHud] renderHud called, modules: " + CrestModules.getAll().size());

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
