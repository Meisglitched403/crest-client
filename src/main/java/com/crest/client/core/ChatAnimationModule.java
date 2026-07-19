package com.crest.client.core;

import com.crest.client.core.setting.BooleanSetting;
import com.crest.client.core.setting.IntegerSetting;
import com.crest.client.core.setting.ModeSetting;
import com.crest.client.core.setting.Setting;

import java.util.List;

public class ChatAnimationModule implements CrestModule {
    private final BooleanSetting enabled = new BooleanSetting("Enabled", true);
    private final BooleanSetting messageAnimation = new BooleanSetting("Message Animation", true);
    private final IntegerSetting fadeTime = new IntegerSetting("Fade Time (ms)", 100, 2000, 500);
    private final BooleanSetting opacityAnimation = new BooleanSetting("Opacity Animation", true);
    private final ModeSetting easing = new ModeSetting("Easing", new String[]{"Sine", "Quad", "Cubic", "Quart", "Expo"}, 0);
    private final ModeSetting easingMode = new ModeSetting("Easing Mode", new String[]{"In", "Out", "InOut"}, 1);

    @Override public String getId() { return "chat_animation"; }
    @Override public String getName() { return "Chat Animation"; }
    @Override public String getDescription() { return "Animated chat messages: slide-up and fade-in effects"; }
    @Override public String getCategory() { return "Visual"; }
    @Override public boolean isEnabled() { return false; }

    @Override
    public List<Setting<?>> getSettings() {
        return List.of(enabled, messageAnimation, fadeTime, opacityAnimation, easing, easingMode);
    }

    public static boolean messagesAnimated() {
        var m = CrestModules.get("chat_animation");
        if (!(m instanceof ChatAnimationModule cam)) return false;
        return cam.messageAnimation.get();
    }

    public static int getFadeTime() {
        var m = CrestModules.get("chat_animation");
        if (!(m instanceof ChatAnimationModule cam)) return 500;
        return cam.fadeTime.get();
    }

    public static boolean opacityAnimated() {
        var m = CrestModules.get("chat_animation");
        if (!(m instanceof ChatAnimationModule cam)) return false;
        return cam.opacityAnimation.get();
    }

    public static String getEasing() {
        var m = CrestModules.get("chat_animation");
        if (!(m instanceof ChatAnimationModule cam)) return "Sine";
        return cam.easing.getMode();
    }

    public static Easing.Mode getEasingMode() {
        var m = CrestModules.get("chat_animation");
        if (!(m instanceof ChatAnimationModule cam)) return Easing.Mode.OUT;
        return switch (cam.easingMode.get()) {
            case 0 -> Easing.Mode.IN;
            case 1 -> Easing.Mode.OUT;
            case 2 -> Easing.Mode.IN_OUT;
            default -> Easing.Mode.OUT;
        };
    }
}
