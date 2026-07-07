package com.crest.client.core;

import com.crest.client.core.event.TickEvent;
import com.crest.client.core.setting.*;
import net.minecraft.world.entity.player.Input;

import java.util.List;
import java.util.function.Consumer;

public class AntiAfkModule implements CrestModule {
    private final IntegerSetting interval = new IntegerSetting(
        "Interval (seconds)", 10, 300, 30
    );
    private final ModeSetting action = new ModeSetting(
        "Action", new String[]{"Jump", "Sneak", "Turn"}, 0
    );
    private final KeybindSetting toggleKey = new KeybindSetting(
        "Toggle Key", org.lwjgl.glfw.GLFW.GLFW_KEY_UNKNOWN
    );

    private int tickCounter;
    private Consumer<TickEvent> tickListener;

    @Override
    public String getId() { return "anti_afk"; }
    @Override
    public String getName() { return "Anti AFK"; }
    @Override
    public String getDescription() { return "Prevents being kicked for inactivity"; }
    @Override
    public String getCategory() { return "Misc"; }
    @Override
    public boolean isEnabled() { return false; }

    @Override
    public List<Setting<?>> getSettings() {
        return List.of(interval, action, toggleKey);
    }

    @Override
    public void onInitialize() {
        tickListener = this::onTick;
        CrestModules.getEventBus().subscribe(TickEvent.class, tickListener);
    }

    private void onTick(TickEvent event) {
        if (!CrestModules.isEnabled("anti_afk")) return;
        if (event.getClient().player == null) return;

        tickCounter++;
        if (tickCounter < interval.get() * 20) return;
        tickCounter = 0;

        var player = event.getClient().player;
        switch (action.get()) {
            case 0 -> player.input.makeJump();
            case 1 -> player.input.keyPresses = new Input(false, false, false, false, false, true, false);
            case 2 -> player.turn(10, 0);
        }
    }
}
