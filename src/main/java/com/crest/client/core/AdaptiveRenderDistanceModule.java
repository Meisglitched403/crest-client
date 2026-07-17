package com.crest.client.core;

import com.crest.client.core.event.TickEvent;
import com.crest.client.core.setting.*;
import net.minecraft.client.Minecraft;

import java.util.List;

/**
 * ponytail: Adaptive Render Distance.
 *
 * Compounds with Block LOD: instead of rendering a fixed horizon, the module
 * watches the real frame-time EMA (FrameBudget) and shrinks the render distance
 * when the game is over budget, then restores it when headroom returns. The
 * decision is debounced (at most one step per COOLDOWN ticks) so the value does
 * not oscillate every frame. Disabled by default so it never surprises the user.
 */
public class AdaptiveRenderDistanceModule implements CrestModule {
    private final BooleanSetting enabled = new BooleanSetting("Enabled", false);
    private final IntegerSetting minDistance = new IntegerSetting("Min distance (sections)", 2, 32, 6);
    private final IntegerSetting maxDistance = new IntegerSetting("Max distance (sections)", 2, 32, 32);
    private final IntegerSetting targetFps = new IntegerSetting("Target FPS", 20, 240, 60);
    private final IntegerSetting cooldownTicks = new IntegerSetting("Adjust cooldown (ticks)", 5, 120, 20);

    private int savedDistance = -1;       // user value captured on enable, restored on disable
    private int ticksSinceAdjust = 0;

    @Override public String getId() { return "adaptive_rd"; }
    @Override public String getName() { return "Adaptive Render Distance"; }
    @Override public String getDescription() { return "Shrinks render distance when frame-time is over budget and restores it when there is headroom. Compounds with Block LOD."; }
    @Override public String getCategory() { return "Performance"; }
    @Override public boolean isEnabled() { return enabled.get(); }
    @Override public void setEnabled(boolean e) { enabled.set(e); }

    @Override
    public List<Setting<?>> getSettings() {
        return List.of(enabled, minDistance, maxDistance, targetFps, cooldownTicks);
    }

    @Override
    public void onInitialize() {
        CrestModules.getEventBus().subscribe(TickEvent.class, this::onTick);
    }

    @Override
    public void onEnable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options != null) {
            savedDistance = mc.options.renderDistance().get();
            int clamped = clamp(savedDistance);
            if (clamped != savedDistance) mc.options.renderDistance().set(clamped);
        }
        ticksSinceAdjust = 0;
    }

    @Override
    public void onDisable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options != null && savedDistance >= 0) {
            mc.options.renderDistance().set(savedDistance);
        }
        savedDistance = -1;
    }

    private void onTick(TickEvent event) {
        if (!enabled.get()) return;
        Minecraft mc = event.getClient();
        if (mc.level == null || mc.options == null) return;

        ticksSinceAdjust++;
        if (ticksSinceAdjust < cooldownTicks.get()) return;
        ticksSinceAdjust = 0;

        double budgetMs = 1000.0 / Math.max(1, targetFps.get());
        double frameMs = FrameBudget.avgFrameMs();
        int current = mc.options.renderDistance().get();
        int min = minDistance.get();
        int max = Math.max(min, maxDistance.get());

        if (frameMs > budgetMs) {
            // Over budget: shrink toward min.
            if (current > min) {
                mc.options.renderDistance().set(current - 1);
            }
        } else if (frameMs < budgetMs * 0.8) {
            // Comfortably under budget: grow back toward max.
            if (current < max) {
                mc.options.renderDistance().set(current + 1);
            }
        }
    }

    private int clamp(int v) {
        int min = minDistance.get();
        int max = Math.max(min, maxDistance.get());
        return Math.max(min, Math.min(max, v));
    }
}
