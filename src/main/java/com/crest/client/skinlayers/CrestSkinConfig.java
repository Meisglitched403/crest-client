package com.crest.client.skinlayers;

import com.crest.client.core.CrestModules;
import com.crest.client.core.SkinLayers3dModule;

public final class CrestSkinConfig {

    private CrestSkinConfig() {
    }

    // Effective thickness scalers, driven by the Crest 3D Skin Layers module.
    public static float baseVoxelSize = 1.15f;
    public static float bodyVoxelWidthSize = 1.05f;
    public static float headVoxelSize = 1.18f;
    public static float firstPersonPixelScaling = 1.1f;
    public static float skullVoxelSize = 1.1f;

    // Layer enable flags follow the module's per-layer toggles.
    public static boolean enableHat = true;
    public static boolean enableJacket = true;
    public static boolean enableLeftSleeve = true;
    public static boolean enableRightSleeve = true;
    public static boolean enableLeftPants = true;
    public static boolean enableRightPants = true;

    // Distance (in blocks) after which layers are not rendered, to save perf.
    public static int renderDistanceLOD = 14;

    // fastRender bakes one opaque box per layer for the "core", matching tr7zw.
    public static boolean fastRender = true;

    public static boolean compatibilityMode = true;

    public static boolean isEnabled(String layer) {
        SkinLayers3dModule mod = (SkinLayers3dModule) CrestModules.get("skin_layers_3d");
        return mod != null && CrestModules.isEnabled("skin_layers_3d") && mod.isLayerEnabled(layer);
    }

    public static void refresh() {
        SkinLayers3dModule mod = (SkinLayers3dModule) CrestModules.get("skin_layers_3d");
        if (mod == null)
            return;
        baseVoxelSize = 1.0f + SkinLayers3dModule.getBodyThickness() * 0.6f;
        bodyVoxelWidthSize = 1.0f + SkinLayers3dModule.getBodyThickness() * 0.25f;
        headVoxelSize = 1.0f + SkinLayers3dModule.getHeadThickness() * 0.6f;
        firstPersonPixelScaling = 1.0f + SkinLayers3dModule.getArmsThickness() * 0.4f;
        skullVoxelSize = headVoxelSize;
        enableHat = isEnabled("hat");
        enableJacket = isEnabled("jacket");
        enableLeftSleeve = isEnabled("leftSleeve");
        enableRightSleeve = isEnabled("rightSleeve");
        enableLeftPants = isEnabled("leftPants");
        enableRightPants = isEnabled("rightPants");
    }

}
