package com.crest.client.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

/**
 * Plays subtle UI sounds for Crest buttons: a click on press and a soft
 * tick on hover-enter. Uses the vanilla UI button sound (this MC version has
 * no dedicated hover sound, so the same event is pitched down for hover).
 */
public final class UiSounds {
    private UiSounds() {}

    public static void click() {
        play(SoundEvents.UI_BUTTON_CLICK, 1.0F);
    }

    public static void hover() {
        play(SoundEvents.UI_BUTTON_CLICK, 0.7F);
    }

    private static void play(Holder<SoundEvent> event, float pitch) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getSoundManager() == null) return;
        mc.getSoundManager().play(SimpleSoundInstance.forUI(event, pitch));
    }
}
