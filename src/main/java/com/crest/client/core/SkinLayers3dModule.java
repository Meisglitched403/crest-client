package com.crest.client.core;

import com.crest.client.core.setting.BooleanSetting;
import com.crest.client.core.setting.FloatSetting;
import com.crest.client.core.setting.Setting;
import java.util.List;
import java.util.Set;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public class SkinLayers3dModule implements CrestModule {
    private final BooleanSetting enabled = new BooleanSetting("Enabled", true);
    private final FloatSetting headThickness = new FloatSetting("Head Thickness", 0f, 1f, 0.4f);
    private final FloatSetting bodyThickness = new FloatSetting("Body Thickness", 0f, 1f, 0.5f);
    private final FloatSetting armsThickness = new FloatSetting("Arms Thickness", 0f, 1f, 0.3f);
    private final FloatSetting legsThickness = new FloatSetting("Legs Thickness", 0f, 1f, 0.3f);

    private final BooleanSetting hat = new BooleanSetting("Hat Layer", true);
    private final BooleanSetting jacket = new BooleanSetting("Jacket Layer", true);
    private final BooleanSetting leftSleeve = new BooleanSetting("Left Sleeve", true);
    private final BooleanSetting rightSleeve = new BooleanSetting("Right Sleeve", true);
    private final BooleanSetting leftPants = new BooleanSetting("Left Pants", true);
    private final BooleanSetting rightPants = new BooleanSetting("Right Pants", true);

    public static final Set<Item> HIDE_HEAD_LAYERS = Set.of(Items.ZOMBIE_HEAD, Items.CREEPER_HEAD,
            Items.DRAGON_HEAD, Items.SKELETON_SKULL, Items.WITHER_SKELETON_SKULL);

    @Override public String getId() { return "skin_layers_3d"; }
    @Override public String getName() { return "3D Skin Layers"; }
    @Override public String getDescription() { return "Gives player outer skin layers actual 3D thickness."; }
    @Override public String getCategory() { return "Visual"; }
    @Override public boolean isEnabled() { return true; }

    @Override
    public List<Setting<?>> getSettings() {
        return List.of(enabled, headThickness, bodyThickness, armsThickness, legsThickness,
                hat, jacket, leftSleeve, rightSleeve, leftPants, rightPants);
    }

    public boolean isLayerEnabled(String layer) {
        return switch (layer) {
            case "hat" -> hat.get();
            case "jacket" -> jacket.get();
            case "leftSleeve" -> leftSleeve.get();
            case "rightSleeve" -> rightSleeve.get();
            case "leftPants" -> leftPants.get();
            case "rightPants" -> rightPants.get();
            default -> true;
        };
    }

    public static Set<Item> hideHeadLayers() {
        return HIDE_HEAD_LAYERS;
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
