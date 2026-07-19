package com.crest.client.core;

import com.crest.client.core.setting.BooleanSetting;
import com.crest.client.core.setting.FloatSetting;
import com.crest.client.core.setting.Setting;
import java.util.List;

public class SkinLayers3dModule implements CrestModule {
    private final BooleanSetting enabled = new BooleanSetting("Enabled", true);
    private final FloatSetting headThickness = new FloatSetting("Head Thickness", 0f, 1f, 0.4f);
    private final FloatSetting bodyThickness = new FloatSetting("Body Thickness", 0f, 1f, 0.5f);
    private final FloatSetting armsThickness = new FloatSetting("Arms Thickness", 0f, 1f, 0.3f);
    private final FloatSetting legsThickness = new FloatSetting("Legs Thickness", 0f, 1f, 0.3f);

    @Override public String getId() { return "skin_layers_3d"; }
    @Override public String getName() { return "3D Skin Layers"; }
    @Override public String getDescription() { return "Gives player outer skin layers actual 3D thickness."; }
    @Override public String getCategory() { return "Visual"; }
    @Override public boolean isEnabled() { return true; }

    @Override
    public List<Setting<?>> getSettings() {
        return List.of(enabled, headThickness, bodyThickness, armsThickness, legsThickness);
    }

    public static float getHeadThickness() {
        var m = CrestModules.get("skin_layers_3d");
        if (!(m instanceof SkinLayers3dModule mod)) return 0.4f;
        return mod.headThickness.get();
    }

    public static float getBodyThickness() {
        var m = CrestModules.get("skin_layers_3d");
        if (!(m instanceof SkinLayers3dModule mod)) return 0.5f;
        return mod.bodyThickness.get();
    }

    public static float getArmsThickness() {
        var m = CrestModules.get("skin_layers_3d");
        if (!(m instanceof SkinLayers3dModule mod)) return 0.3f;
        return mod.armsThickness.get();
    }

    public static float getLegsThickness() {
        var m = CrestModules.get("skin_layers_3d");
        if (!(m instanceof SkinLayers3dModule mod)) return 0.3f;
        return mod.legsThickness.get();
    }
}
