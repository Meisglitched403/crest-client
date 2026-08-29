package com.crest.client.core;

import com.crest.client.core.event.TickEvent;
import com.crest.client.waypoints.ChatCoordinateService;
import com.crest.client.waypoints.ClientCommandRegistrar;
import com.crest.client.waypoints.ConfigStorage;
import com.crest.client.waypoints.DeathWaypointService;
import com.crest.client.waypoints.label.preset.LabelPresetStore;
import com.crest.client.waypoints.ModConfig;
import com.crest.client.waypoints.ModKeybindings;
import com.crest.client.waypoints.Waypoint;
import com.crest.client.waypoints.WaypointColorService;
import com.crest.client.waypoints.WaypointManager;
import com.crest.client.waypoints.WaypointOverlayLabelRenderer;
import com.crest.client.waypoints.FolderNavigationKeybindService;
import com.crest.client.waypoints.ToggleSneakService;
import com.crest.client.waypoints.WaypointSaveQueue;
import com.crest.client.waypoints.WaypointStorage;
import com.crest.client.waypoints.WaypointWorldRenderer;
import com.crest.client.waypoints.WorldContext;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class WaypointsModule extends HudModule {
    private static WaypointsModule INSTANCE;

    private final WaypointManager mgr = new WaypointManager();
    private ModConfig cfg = new ModConfig();
    private final ModKeybindings keybinds = ModKeybindings.createDefault();

    private int page;
    private boolean wasAlive = true;
    private String lastWorldId = "";
    private String lastDimId = "";

    public WaypointsModule() {
        super(-1, 4);
    }

    @Override public String getId() { return "waypoints"; }
    @Override public String getName() { return "Waypoints"; }
    @Override public String getDescription() { return "World markers ported from wWaypoints: 3D markers, labels, death waypoints, chat coords and /wp commands."; }
    @Override public String getCategory() { return "Visual"; }
    @Override public boolean isEnabled() { return true; }
    @Override public boolean selfHandlesKeybinds() { return false; }

    public static WaypointsModule get() { return INSTANCE; }
    public WaypointManager getManager() { return mgr; }
    public ModConfig getConfig() { return cfg; }

    @Override
    public void onInitialize() {
        INSTANCE = this;
        LabelPresetStore.initDefaults();
        cfg = ConfigStorage.load();
        WaypointWorldRenderer.register(mgr, () -> cfg);
        ClientCommandRegistrar.register(() -> mgr, () -> cfg);
        keybinds.registerActions(mgr, cfg, this::openMenu, this::openSettings);
        CrestModules.getEventBus().subscribe(TickEvent.class, this::onTick);
        refreshForWorld();
    }

    private void openMenu() {}
    private void openSettings() {}

    private void refreshForWorld() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        String worldId = WorldContext.currentWorldId(mc);
        String dimId = WorldContext.currentDimensionId(mc);
        if (!worldId.equals(lastWorldId) || !dimId.equals(lastDimId)) {
            if (!lastWorldId.isEmpty()) save();
            lastWorldId = worldId;
            lastDimId = dimId;
            WaypointStorage.LoadResult r = WaypointStorage.loadByDimensionId(worldId, dimId);
            mgr.replaceAll(r.waypoints, r.clumps, r.groups, r.folders);
        }
    }

    private void onTick(TickEvent event) {
        Minecraft mc = event.getClient();
        if (mc.level == null) return;

        refreshForWorld();

        boolean alive = mc.player != null && mc.player.isAlive();
        if (cfg.deathWaypoints && wasAlive && !alive && mc.player != null) {
            DeathWaypointService.maybeCreateOnDeath(mc, mgr, cfg);
            WaypointSaveQueue.markDirty();
        }
        wasAlive = alive;

        if (WaypointSaveQueue.consume()) save();
    }

    private void save() {
        if (lastWorldId.isEmpty()) return;
        WaypointStorage.saveByDimensionId(lastWorldId, lastDimId,
                mgr.getWaypoints(),
                mgr.getClumpsByIdInternal(),
                mgr.getGroupsByIdInternal(),
                mgr.getFoldersByIdInternal(),
                java.util.Set.of(), java.util.Set.of(), java.util.List.of());
    }

    public void renderWorld(PoseStack ps, MultiBufferSource.BufferSource buffer, CameraRenderState cam) {
        if (!CrestModules.isEnabled(getId())) return;
        WaypointWorldRenderer.renderWorld(ps, buffer, cam);
    }

    public static void handleChat(Component msg) {
        WaypointsModule mod = INSTANCE;
        if (mod == null) return;
        if (!mod.cfg.chatCoordinatesEnabled) return;
        ChatCoordinateService.Coordinates c = ChatCoordinateService.parseStrict(msg.getString());
        if (c == null) return;
        int color = WaypointColorService.pickNewWaypointColor(mod.cfg, Waypoint.Type.NORMAL);
        mod.mgr.createWaypoint(new BlockPos(c.x, c.y, c.z), "Chat " + c.x + " " + c.z, color, "block:grass", false, Waypoint.Type.NORMAL);
        WaypointSaveQueue.markDirty();
    }

    /* -------------------- HUD: directional waypoint list -------------------- */

    private List<Waypoint> sortedForHud() {
        Minecraft mc = Minecraft.getInstance();
        List<Waypoint> list = new ArrayList<>(mgr.getWaypoints());
        list.sort(Comparator.comparingDouble(this::distSq));
        return list;
    }

    private double distSq(Waypoint wp) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || wp.getPosition() == null) return Double.MAX_VALUE;
        double dx = (wp.getX() + 0.5) - mc.player.getX();
        double dy = wp.getY() - mc.player.getY();
        double dz = (wp.getZ() + 0.5) - mc.player.getZ();
        return dx * dx + dy * dy + dz * dz;
    }

    private String lineFor(Waypoint wp, int dist) {
        return arrowTo(wp) + " " + wp.getName() + " [" + dist + "m]";
    }

    private String arrowTo(Waypoint wp) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || wp.getPosition() == null) return "";
        double dx = (wp.getX() + 0.5) - mc.player.getX();
        double dz = (wp.getZ() + 0.5) - mc.player.getZ();
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
        if (!cfg.showLabels) return 0;
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
        if (!cfg.showLabels) return 0;
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

    private static final int PAGE_SIZE = 5;

    @Override
    public void render(GuiGraphicsExtractor g, Minecraft mc, DeltaTracker d) {
        if (mc.level == null || mc.player == null) return;
        if (!CrestModules.isEnabled(getId())) return;

        WaypointOverlayLabelRenderer.render(g, mc, mgr, cfg);

        if (!cfg.showLabels) return;
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
            int argb = 0xFF000000 | (wp.getColor() & 0xFFFFFF);
            g.text(mc.font, Component.literal(lineFor(wp, dist)), rx + 4, ty, argb);
            ty += 10;
        }

        if (cfg.activeHideThreshold > 0) {
            g.text(mc.font, Component.literal("Hidden < " + cfg.activeHideThreshold + "m"),
                    rx + 4, ry + getHeight() - 10, 0xFF888888);
        }
        if (cfg.activeFolderScope != null && !cfg.activeFolderScope.isEmpty()) {
            g.text(mc.font, Component.literal("Folder: " + FolderNavigationKeybindService.currentLabel(mgr, cfg)),
                    rx + 4, ry + getHeight() - (cfg.activeHideThreshold > 0 ? 21 : 10), 0xFF88CCFF);
        }

        ToggleSneakService.renderIndicator(g, cfg,
                mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
    }

    @Override
    public Screen createConfigScreen(Screen parent) { return null; }
}
