package com.crest.client.core;

import com.crest.client.core.event.TickEvent;
import com.crest.client.core.setting.BooleanSetting;
import com.crest.client.core.setting.IntegerSetting;
import com.crest.client.core.setting.KeybindSetting;
import com.crest.client.core.setting.Setting;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class WaypointsModule extends HudModule {
    private static final int PAGE_SIZE = 5;

    private final IntegerSetting maxDistance = new IntegerSetting("Max Distance", 100, 10000, 1000);
    private final BooleanSetting showBeams = new BooleanSetting("Show Beams", true);
    private final BooleanSetting showLabels = new BooleanSetting("Show Labels", true);
    private final BooleanSetting showDistance = new BooleanSetting("Show Distance", true);
    private final BooleanSetting showHud = new BooleanSetting("Show Waypoint List", true);
    private final BooleanSetting autoDeath = new BooleanSetting("Auto Death Waypoints", true);
    private final IntegerSetting deathLimit = new IntegerSetting("Death Waypoint Limit", 0, 50, 3);
    private final KeybindSetting addWaypointKey = new KeybindSetting("Add Waypoint Key",
        org.lwjgl.glfw.GLFW.GLFW_KEY_PERIOD);
    private final KeybindSetting nextPageKey = new KeybindSetting("Next Page Key",
        org.lwjgl.glfw.GLFW.GLFW_KEY_PAGE_DOWN);
    private final KeybindSetting prevPageKey = new KeybindSetting("Prev Page Key",
        org.lwjgl.glfw.GLFW.GLFW_KEY_PAGE_UP);

    private int page;
    private boolean wasAlive = true;

    public WaypointsModule() {
        super(-1, 4);
    }

    @Override
    public String getId() { return "waypoints"; }
    @Override
    public String getName() { return "Waypoints"; }
    @Override
    public String getDescription() { return "World markers, directional waypoint list and /wp commands."; }
    @Override
    public String getCategory() { return "Visual"; }
    @Override
    public boolean isEnabled() { return true; }

    @Override
    public boolean selfHandlesKeybinds() { return true; }

    @Override
    public List<Setting<?>> getSettings() {
        return List.of(maxDistance, showBeams, showLabels, showDistance, showHud,
            autoDeath, deathLimit, addWaypointKey, nextPageKey, prevPageKey);
    }

    @Override
    public void onInitialize() {
        WaypointManager.load();
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
            WaypointCommands.register(dispatcher));
        CrestModules.getEventBus().subscribe(TickEvent.class, this::onTick);
    }

    @Override
    public void loadSettings() {
        WaypointManager.setDeathLimit(deathLimit.get());
    }

    private void onTick(TickEvent event) {
        Minecraft mc = event.getClient();
        if (mc.level == null) return;

        if (WaypointManager.getDeathLimit() != deathLimit.get()) {
            WaypointManager.setDeathLimit(deathLimit.get());
        }

        if (addWaypointKey.wasPressed() && mc.player != null && mc.screen == null) {
            mc.setScreen(new WaypointAddScreen(null));
        }
        if (nextPageKey.wasPressed()) page++;
        if (prevPageKey.wasPressed()) page--;

        boolean alive = mc.player != null && mc.player.isAlive();
        if (autoDeath.get() && wasAlive && !alive && mc.player != null) {
            WaypointManager.addDeath(mc);
            mc.player.sendSystemMessage(Component.literal("[Crest] Death waypoint added. Use /wp clear death to remove."));
        }
        wasAlive = alive;
    }

    /* -------------------- Settings read by the world renderer -------------------- */

    public static int getMaxDistance() {
        CrestModule m = CrestModules.get("waypoints");
        return m instanceof WaypointsModule w ? w.maxDistance.get() : 1000;
    }

    public static boolean showBeams() {
        CrestModule m = CrestModules.get("waypoints");
        return m instanceof WaypointsModule w && w.showBeams.get();
    }

    public static boolean showLabels() {
        CrestModule m = CrestModules.get("waypoints");
        return m instanceof WaypointsModule w && w.showLabels.get();
    }

    public static boolean showDistance() {
        CrestModule m = CrestModules.get("waypoints");
        return m instanceof WaypointsModule w && w.showDistance.get();
    }

    public static boolean showHud() {
        CrestModule m = CrestModules.get("waypoints");
        return m instanceof WaypointsModule w && w.showHud.get();
    }

    public void renderWorld(PoseStack ps, MultiBufferSource.BufferSource buffer, CameraRenderState cam) {
        if (!CrestModules.isEnabled(getId())) return;
        WaypointWorldRenderer.render(ps, buffer, cam);
    }

    /* -------------------- HUD: directional waypoint list -------------------- */

    private List<Waypoint> sortedForHud() {
        Minecraft mc = Minecraft.getInstance();
        List<Waypoint> list = new ArrayList<>(WaypointManager.listForCurrent(mc));
        list.sort(Comparator.comparingDouble(this::distSq));
        return list;
    }

    private double distSq(Waypoint wp) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return Double.MAX_VALUE;
        double dx = (wp.x + 0.5) - mc.player.getX();
        double dy = wp.y - mc.player.getY();
        double dz = (wp.z + 0.5) - mc.player.getZ();
        return dx * dx + dy * dy + dz * dz;
    }

    private String lineFor(Waypoint wp, int dist) {
        return arrowTo(wp) + " " + wp.name + " [" + dist + "m]";
    }

    private String arrowTo(Waypoint wp) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return "";
        double dx = (wp.x + 0.5) - mc.player.getX();
        double dz = (wp.z + 0.5) - mc.player.getZ();

        float targetYaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float playerYaw = mc.player.getYRot();
        float delta = Mth.wrapDegrees(targetYaw - playerYaw);

        if (delta >= -22.5 && delta < 22.5) return "\u2191";
        if (delta >= 22.5 && delta < 67.5) return "\u2197";
        if (delta >= 67.5 && delta < 112.5) return "\u2192";
        if (delta >= 112.5 && delta < 157.5) return "\u2198";
        if (delta >= 157.5 || delta < -157.5) return "\u2193";
        if (delta >= -157.5 && delta < -112.5) return "\u2199";
        if (delta >= -112.5 && delta < -67.5) return "\u2190";
        return "\u2196";
    }

    @Override
    public int getWidth() {
        if (!showHud()) return 0;
        Minecraft mc = Minecraft.getInstance();
        int maxW = 90;
        for (Waypoint wp : sortedForHud()) {
            int dist = (int) Math.round(Math.sqrt(distSq(wp)));
            maxW = Math.max(maxW, mc.font.width(lineFor(wp, dist)) + 6);
        }
        return maxW;
    }

    @Override
    public int getHeight() {
        if (!showHud()) return 0;
        List<Waypoint> list = sortedForHud();
        if (list.isEmpty()) return 0;
        int total = list.size();
        int pages = Math.max(1, (total + PAGE_SIZE - 1) / PAGE_SIZE);
        if (page > pages - 1) page = pages - 1;
        if (page < 0) page = 0;
        int from = page * PAGE_SIZE;
        int to = Math.min(from + PAGE_SIZE, total);
        int rows = Math.max(1, to - from);
        return 14 + rows * 10;
    }

    @Override
    public void render(GuiGraphicsExtractor g, Minecraft mc, DeltaTracker d) {
        if (mc.level == null || mc.player == null) return;
        if (!WaypointManager.isEnabled()) return;
        if (!showHud()) return;

        List<Waypoint> list = sortedForHud();
        if (list.isEmpty()) return;

        int total = list.size();
        int pages = Math.max(1, (total + PAGE_SIZE - 1) / PAGE_SIZE);
        if (page < 0) page = 0;
        if (page > pages - 1) page = pages - 1;

        int from = page * PAGE_SIZE;
        int to = Math.min(from + PAGE_SIZE, total);

        int rx = x < 0 ? mc.getWindow().getGuiScaledWidth() - getWidth() - 4 : x;
        int ry = y;

        HudBackground.draw(g, rx, ry, getWidth(), getHeight());

        int ty = ry + 2;
        String header = "Waypoints " + (page + 1) + "/" + pages;
        g.text(mc.font, Component.literal(header), rx + 4, ty, 0xFFFFFFFF);
        ty += 12;

        for (int i = from; i < to; i++) {
            Waypoint wp = list.get(i);
            int dist = (int) Math.round(Math.sqrt(distSq(wp)));
            int argb = 0xFF000000 | (wp.color & 0xFFFFFF);
            g.text(mc.font, Component.literal(lineFor(wp, dist)), rx + 4, ty, argb);
            ty += 10;
        }
    }

    @Override
    public Screen createConfigScreen(Screen parent) {
        return new WaypointScreen(this, parent);
    }
}
