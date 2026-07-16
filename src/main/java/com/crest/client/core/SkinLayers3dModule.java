package com.crest.client.core;

import com.crest.client.core.setting.BooleanSetting;
import com.crest.client.core.setting.FloatSetting;
import com.crest.client.core.setting.IntegerSetting;
import com.crest.client.core.setting.Setting;
import com.crest.client.core.skinlayers3d.Layers3d;
import com.crest.client.core.skinlayers3d.SkinLayersConfig;

import java.util.List;

public class SkinLayers3dModule implements CrestModule {
    private final BooleanSetting enableHat = new BooleanSetting("3D Hat", true);
    private final BooleanSetting enableJacket = new BooleanSetting("3D Jacket", true);
    private final BooleanSetting enableLeftSleeve = new BooleanSetting("3D Left Sleeve", true);
    private final BooleanSetting enableRightSleeve = new BooleanSetting("3D Right Sleeve", true);
    private final BooleanSetting enableLeftPants = new BooleanSetting("3D Left Pants", true);
    private final BooleanSetting enableRightPants = new BooleanSetting("3D Right Pants", true);
    private final IntegerSetting renderDistance = new IntegerSetting("Render Distance", 4, 64, 16);
    private final BooleanSetting fastRender = new BooleanSetting("Fast Render", true);

    @Override
    public String getId() { return "skin_layers_3d"; }
    @Override
    public String getName() { return "3D Skin Layers"; }
    @Override
    public String getDescription() { return "Renders the second skin layer in 3D"; }
    @Override
    public String getCategory() { return "Visual"; }
    @Override
    public boolean isEnabled() { return false; }

    @Override
    public List<Setting<?>> getSettings() {
        return List.of(enableHat, enableJacket, enableLeftSleeve, enableRightSleeve,
                enableLeftPants, enableRightPants, renderDistance, fastRender);
    }

    public static void syncConfig() {
        var m = CrestModules.get("skin_layers_3d");
        if (!(m instanceof SkinLayers3dModule mod)) return;
        SkinLayersConfig c = Layers3d.getConfig();
        c.enableHat = mod.enableHat.get();
        c.enableJacket = mod.enableJacket.get();
        c.enableLeftSleeve = mod.enableLeftSleeve.get();
        c.enableRightSleeve = mod.enableRightSleeve.get();
        c.enableLeftPants = mod.enableLeftPants.get();
        c.enableRightPants = mod.enableRightPants.get();
        c.renderDistanceLOD = mod.renderDistance.get();
        c.fastRender = mod.fastRender.get();
    }

    @Override
    public void onEnable() {
        syncConfig();
    }

    @Override
    public void onDisable() {
        SkinLayersConfig c = Layers3d.getConfig();
        c.enableHat = c.enableJacket = c.enableLeftSleeve = c.enableRightSleeve = false;
        c.enableLeftPants = c.enableRightPants = false;
    }
}
