package com.crest.client.core;

import com.crest.client.core.setting.*;

import java.util.ArrayList;
import java.util.List;

public class ViewModelModule implements CrestModule {
    private static ViewModelModule instance;

    // Transform Target
    private final ModeSetting transformTarget = new ModeSetting("Transform Target",
        new String[]{"Both", "Hands Only", "Arms Only"}, 0);

    // Main Hand
    private final FloatSetting mainScale = new FloatSetting("Main Hand Scale", 0.1f, 5.0f, 1.0f);
    private final FloatSetting mainPosX = new FloatSetting("Main Position X", -45f, 45f, 0f);
    private final FloatSetting mainPosY = new FloatSetting("Main Position Y", -45f, 45f, 0f);
    private final FloatSetting mainPosZ = new FloatSetting("Main Position Z", -30f, 30f, 0f);
    private final FloatSetting mainRotX = new FloatSetting("Main Rotation X (Pitch)", -70f, 70f, 0f);
    private final FloatSetting mainRotY = new FloatSetting("Main Rotation Y (Yaw)", -60f, 60f, 0f);
    private final FloatSetting mainRotZ = new FloatSetting("Main Rotation Z (Roll)", -60f, 60f, 0f);

    // Off Hand
    private final FloatSetting offScale = new FloatSetting("Off Hand Scale", 0.1f, 5.0f, 1.0f);
    private final FloatSetting offPosX = new FloatSetting("Off Position X", -45f, 45f, 0f);
    private final FloatSetting offPosY = new FloatSetting("Off Position Y", -45f, 45f, 0f);
    private final FloatSetting offPosZ = new FloatSetting("Off Position Z", -30f, 30f, 0f);
    private final FloatSetting offRotX = new FloatSetting("Off Rotation X (Pitch)", -70f, 70f, 0f);
    private final FloatSetting offRotY = new FloatSetting("Off Rotation Y (Yaw)", -60f, 60f, 0f);
    private final FloatSetting offRotZ = new FloatSetting("Off Rotation Z (Roll)", -60f, 60f, 0f);

    // Swing
    private final ModeSetting swingMode = new ModeSetting("Swing Mode",
        new String[]{"Default", "Main Hand", "Off Hand"}, 0);
    private final IntegerSetting swingSpeed = new IntegerSetting("Swing Speed Modifier", -5, 5, 0);
    private final FloatSetting mainSwingOffset = new FloatSetting("Main Swing Offset", 0f, 1f, 0f);
    private final FloatSetting offSwingOffset = new FloatSetting("Off Swing Offset", 0f, 1f, 0f);
    private final BooleanSetting noSwing = new BooleanSetting("No Swing Animation", false);

    // Misc
    private final BooleanSetting oldAnimations = new BooleanSetting("1.8 Animations", false);
    private final BooleanSetting swordSlash = new BooleanSetting("Sword Slash Pose", false);
    private final BooleanSetting skipEquip = new BooleanSetting("Skip Equip Animation", false);
    private final BooleanSetting hideHands = new BooleanSetting("Hide Hands", false);

    @Override
    public String getId() { return "view_model"; }

    @Override
    public String getName() { return "View Model"; }

    @Override
    public String getDescription() { return "Customize held item position, rotation, and scale"; }

    @Override
    public String getCategory() { return "Visual"; }

    @Override
    public boolean isEnabled() { return false; }

    @Override
    public void onInitialize() {
        instance = this;
    }

    @Override
    public List<Setting<?>> getSettings() {
        List<Setting<?>> s = new ArrayList<>();
        s.add(transformTarget);
        s.add(mainScale);
        s.add(mainPosX);
        s.add(mainPosY);
        s.add(mainPosZ);
        s.add(mainRotX);
        s.add(mainRotY);
        s.add(mainRotZ);
        s.add(offScale);
        s.add(offPosX);
        s.add(offPosY);
        s.add(offPosZ);
        s.add(offRotX);
        s.add(offRotY);
        s.add(offRotZ);
        s.add(swingMode);
        s.add(swingSpeed);
        s.add(mainSwingOffset);
        s.add(offSwingOffset);
        s.add(noSwing);
        s.add(oldAnimations);
        s.add(swordSlash);
        s.add(skipEquip);
        s.add(hideHands);
        return s;
    }

    // ──────────────────────────────────────────────
    // Static accessors for mixins
    // ──────────────────────────────────────────────

    public static boolean isActive() {
        return instance != null && CrestModules.isEnabled("view_model");
    }

    public static boolean isHideHands() {
        return instance != null && instance.hideHands.get();
    }

    public static boolean isSkipEquip() {
        return instance != null && instance.skipEquip.get();
    }

    public static boolean isOldAnimations() {
        return instance != null && instance.oldAnimations.get();
    }

    public static boolean isSwordSlash() {
        return instance != null && instance.swordSlash.get();
    }

    public static boolean isNoSwing() {
        return instance != null && instance.noSwing.get();
    }

    public static int getSwingSpeed() {
        return instance != null ? instance.swingSpeed.get() : 0;
    }

    public static float getMainSwingOffset() {
        return instance != null ? instance.mainSwingOffset.get() : 0f;
    }

    public static float getOffSwingOffset() {
        return instance != null ? instance.offSwingOffset.get() : 0f;
    }

    public static int getTransformTarget() {
        return instance != null ? instance.transformTarget.get() : 0;
    }

    public static int getSwingMode() {
        return instance != null ? instance.swingMode.get() : 0;
    }

    public static float getMainScale() {
        return instance != null ? instance.mainScale.get() : 1f;
    }

    public static float getMainPosX() { return instance != null ? instance.mainPosX.get() : 0f; }
    public static float getMainPosY() { return instance != null ? instance.mainPosY.get() : 0f; }
    public static float getMainPosZ() { return instance != null ? instance.mainPosZ.get() : 0f; }
    public static float getMainRotX() { return instance != null ? instance.mainRotX.get() : 0f; }
    public static float getMainRotY() { return instance != null ? instance.mainRotY.get() : 0f; }
    public static float getMainRotZ() { return instance != null ? instance.mainRotZ.get() : 0f; }

    public static float getOffScale() {
        return instance != null ? instance.offScale.get() : 1f;
    }

    public static float getOffPosX() { return instance != null ? instance.offPosX.get() : 0f; }
    public static float getOffPosY() { return instance != null ? instance.offPosY.get() : 0f; }
    public static float getOffPosZ() { return instance != null ? instance.offPosZ.get() : 0f; }
    public static float getOffRotX() { return instance != null ? instance.offRotX.get() : 0f; }
    public static float getOffRotY() { return instance != null ? instance.offRotY.get() : 0f; }
    public static float getOffRotZ() { return instance != null ? instance.offRotZ.get() : 0f; }
}
