package com.crest.client.core;

import com.crest.client.core.setting.*;

public class BlockLodModule implements CrestModule {
    // ponytail: distance thresholds in SECTIONS (16 blocks) from the camera.
    // A block's section distance determines its LOD tier:
    //   tier 0 (near):   full quality — unchanged vanilla/Sodium path
    //   tier 1:          disable AO + collapse cutout/translucent to solid
    //   tier 2:          tier 1 + aggressive interior-face culling
    //   tier 3 (far):    tier 2 (translucency fully collapsed at distance)
    private final IntegerSetting tier1Sections = new IntegerSetting("Tier 1 Start (sections)", 1, 32, 2);
    private final IntegerSetting tier2Sections = new IntegerSetting("Tier 2 Start (sections)", 1, 32, 4);
    private final IntegerSetting tier3Sections = new IntegerSetting("Tier 3 Start (sections)", 1, 32, 8);

    @Override public String getId() { return "block_lod"; }
    @Override public String getName() { return "Block LOD"; }
    @Override public String getDescription() { return "Quality-preserving block LOD: distant blocks use cheaper meshing (no AO, solid layers, aggressive culling) while near blocks stay full quality."; }
    @Override public String getCategory() { return "Performance"; }
    @Override public boolean isEnabled() { return false; }

    @Override
    public java.util.List<Setting<?>> getSettings() {
        return java.util.List.of(tier1Sections, tier2Sections, tier3Sections);
    }

    // ponytail: these are read from Sodium's chunk-build worker threads, so they
    // must be cheap + volatile-safe. IntegerSetting.get() is a plain field read.
    public static int tier1() { return INSTANCE != null ? INSTANCE.tier1Sections.get() : 2; }
    public static int tier2() { return INSTANCE != null ? INSTANCE.tier2Sections.get() : 4; }
    public static int tier3() { return INSTANCE != null ? INSTANCE.tier3Sections.get() : 8; }
    public static boolean active() { return INSTANCE != null && CrestModules.isEnabled("block_lod"); }

    // set in onInitialize so the mixin can read thresholds without a Map lookup
    static BlockLodModule INSTANCE;
    @Override
    public void onInitialize() { INSTANCE = this; }
}
