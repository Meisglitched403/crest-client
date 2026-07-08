package com.crest.client.core;

import com.crest.client.core.event.TickEvent;
import com.crest.client.core.setting.*;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.function.Consumer;

import static org.lwjgl.glfw.GLFW.glfwGetCurrentContext;
import static org.lwjgl.glfw.GLFW.glfwGetKey;

public class WaypointsModule implements CrestModule, RenderableModule {
    private final IntegerSetting maxDistance = new IntegerSetting(
        "Max Distance", 100, 10000, 1000
    );
    private final BooleanSetting showLabels = new BooleanSetting(
        "Show Labels", true
    );
    private final BooleanSetting showDistance = new BooleanSetting(
        "Show Distance", true
    );
    private final KeybindSetting toggleKey = new KeybindSetting(
        "Toggle Key", org.lwjgl.glfw.GLFW.GLFW_KEY_UNKNOWN
    );

    private static boolean wasPeriodDown;

    @Override
    public String getId() { return "waypoints"; }
    @Override
    public String getName() { return "Waypoints"; }
    @Override
    public String getDescription() { return "Mark and display waypoints in the world"; }
    @Override
    public String getCategory() { return "Visual"; }
    @Override
    public boolean isEnabled() { return CrestModules.isEnabled(getId()); }

    @Override
    public List<Setting<?>> getSettings() {
        return List.of(maxDistance, showLabels, showDistance, toggleKey);
    }

    @Override
    public void onInitialize() {
        WaypointManager.init();
        CrestModules.getEventBus().subscribe(TickEvent.class, this::onTick);
    }

    private void onTick(TickEvent event) {
        Minecraft mc = event.getClient();
        long window = glfwGetCurrentContext();
        if (window == 0 || mc.player == null) return;

        boolean isDown = glfwGetKey(window, GLFW.GLFW_KEY_PERIOD) == GLFW.GLFW_PRESS;
        if (isDown && !wasPeriodDown && CrestModules.isEnabled("waypoints")) {
            Vec3 pos = mc.player.position();
            int count = WaypointManager.getAll().size();
            String name = "Waypoint " + (count + 1);
            String dim = mc.level != null ? mc.level.dimension().identifier().toString() : "minecraft:overworld";
            WaypointManager.add(new Waypoint(name, pos.x, pos.y, pos.z, dim, 0xFFFF5555));
            mc.player.sendSystemMessage(Component.literal("Waypoint added: " + name));
        }
        wasPeriodDown = isDown;
    }

    public static int getMaxDistance() {
        CrestModule m = CrestModules.get("waypoints");
        return m instanceof WaypointsModule w ? w.maxDistance.get() : 1000;
    }

    @Override
    public void render(GuiGraphicsExtractor g, Minecraft mc, DeltaTracker d) {
        if (mc.level == null || mc.player == null) return;
        if (mc.screen instanceof WaypointScreen) return;

        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 cameraPos = camera.position();
        Quaternionf cameraRot = new Quaternionf(camera.rotation());
        cameraRot.conjugate();

        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();
        double fov = Math.toRadians(camera.getFov());
        float focalLen = (float) (screenH / 2.0 / Math.tan(fov / 2.0));

        int maxDist = getMaxDistance();
        int maxDistSq = maxDist * maxDist;

        List<Waypoint> waypoints = WaypointManager.getVisible();

        for (Waypoint wp : waypoints) {
            double dx = wp.getX() - cameraPos.x;
            double dy = wp.getY() - cameraPos.y;
            double dz = wp.getZ() - cameraPos.z;
            double distSq = dx * dx + dy * dy + dz * dz;

            if (distSq > maxDistSq) continue;
            if (distSq < 1) continue;

            Vector3f viewSpace = new Vector3f((float) dx, (float) dy, (float) dz);
            cameraRot.transform(viewSpace);

            float screenX, screenY;

            if (viewSpace.z > 0.1f) {
                screenX = (viewSpace.x / viewSpace.z) * focalLen + screenW / 2.0f;
                screenY = -(viewSpace.y / viewSpace.z) * focalLen + screenH / 2.0f;
            } else {
                Vector3f dir = new Vector3f(viewSpace).normalize();
                float angle = (float) Math.atan2(-viewSpace.x, -viewSpace.z);
                screenX = screenW / 2.0f + (float) Math.cos(angle) * screenW * 0.4f;
                screenY = screenH / 2.0f + (float) Math.sin(angle) * screenH * 0.4f;
            }

            screenX = Math.max(8, Math.min(screenW - 8, screenX));
            screenY = Math.max(8, Math.min(screenH - 8, screenY));

            int color = wp.getColor() | 0xFF000000;
            g.fill((int) screenX - 3, (int) screenY - 3, (int) screenX + 3, (int) screenY + 3, color);

            if (showLabels.get()) {
                String label = wp.getName();
                if (showDistance.get()) {
                    double dist = Math.sqrt(distSq);
                    label += " (" + String.format("%.0f", dist) + "m)";
                }
                int labelW = mc.font.width(label);
                int lx = (int) screenX - labelW / 2;
                int ly = (int) screenY + 5;
                g.fill(lx - 1, ly - 1, lx + labelW + 1, ly + mc.font.lineHeight + 1, 0x66000000);
                g.text(mc.font, Component.literal(label), lx, ly, 0xFFFFFF);
            }
        }
    }
}
