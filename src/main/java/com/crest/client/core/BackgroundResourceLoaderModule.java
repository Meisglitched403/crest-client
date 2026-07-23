package com.crest.client.core;

import com.crest.client.core.setting.BooleanSetting;
import com.crest.client.core.setting.FloatSetting;
import com.crest.client.core.setting.ModeSetting;
import com.crest.client.core.setting.Setting;

import java.util.List;

public class BackgroundResourceLoaderModule implements CrestModule {
    private static volatile BackgroundResourceLoaderModule instance;

    private final ModeSetting progressStyle = new ModeSetting(
        "Progress Style", "How loading progress is displayed",
        new String[]{"Progress Bar", "Text", "None"}, 0
    );

    private final BooleanSetting showInBackground = new BooleanSetting(
        "Show In Background", "Render game world while resource packs load", true
    );

    private final FloatSetting animationSpeed = new FloatSetting(
        "Fade Speed", "Speed of the fade-out animation in ms",
        100f, 5000f, 500f
    );

    private final BooleanSetting rgbProgress = new BooleanSetting(
        "Rainbow Progress", "Color-cycling progress indicator", false
    );

    private volatile boolean currentlyLoading;

    @Override public String getId() { return "background_resource_loader"; }
    @Override public String getName() { return "Background Resource Loader"; }
    @Override public String getDescription() { return "Loads resource packs in the background so you can keep playing"; }
    @Override public String getCategory() { return "Performance"; }

    @Override
    public void onInitialize() {
        instance = this;
    }

    @Override
    public List<Setting<?>> getSettings() {
        return List.of(showInBackground, progressStyle, animationSpeed, rgbProgress);
    }

    public static boolean isActive() {
        var brl = instance;
        return brl != null && CrestModules.isEnabled(brl.getId()) && brl.showInBackground.get();
    }

    public static int getProgressStyle() {
        var brl = instance;
        return brl != null ? brl.progressStyle.get() : 0;
    }

    public static float getAnimationSpeed() {
        var brl = instance;
        return brl != null ? brl.animationSpeed.get() : 500f;
    }

    public static boolean isRgbProgress() {
        var brl = instance;
        return brl != null && brl.rgbProgress.get();
    }

    public static boolean isCurrentlyLoading() {
        var brl = instance;
        return brl != null && brl.currentlyLoading;
    }

    public static void setCurrentLoading(boolean loading) {
        var brl = instance;
        if (brl != null) brl.currentlyLoading = loading;
    }

    public static BackgroundResourceLoaderModule getInstance() {
        return instance;
    }
}
