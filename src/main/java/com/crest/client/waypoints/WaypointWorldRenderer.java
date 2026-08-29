package com.crest.client.waypoints;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.state.level.CameraRenderState;

import java.util.function.Supplier;

/**
 * Draws waypoint markers in the 3D world. The in-world cube highlight is the
 * "highlight blocks" feature (see {@link CubeHighlightRenderer}); screen-space
 * labels/markers are drawn separately by {@link WaypointOverlayLabelRenderer}
 * during the HUD pass.
 */
public final class WaypointWorldRenderer {

    private static volatile WaypointManager REGISTERED_MANAGER;
    private static volatile Supplier<ModConfig> REGISTERED_CONFIG;

    private WaypointWorldRenderer() {}

    public static void register(WaypointManager manager, Supplier<ModConfig> configSupplier) {
        REGISTERED_MANAGER = manager;
        REGISTERED_CONFIG = configSupplier;
    }

    public static void renderWorld(PoseStack ps, MultiBufferSource.BufferSource buffer, CameraRenderState cam) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        if (REGISTERED_MANAGER == null || REGISTERED_CONFIG == null) return;
        CubeHighlightRenderer.render(ps, buffer, REGISTERED_MANAGER, REGISTERED_CONFIG.get());
    }
}
