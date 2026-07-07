package com.crest.client.core;

import com.crest.client.core.event.TickEvent;
import com.crest.client.core.setting.*;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

public class SpeedometerModule extends HudModule {
    private static final double BLOCKS_TO_KMH = 3.6;

    private final ModeSetting unitMode = new ModeSetting(
        "Unit", "Blocks/s or km/h", new String[]{"Blocks/s", "km/h"}, 0
    );
    private final ColorSetting textColor = new ColorSetting(
        "Text Color", 0xFFFFFFFF
    );
    private final IntegerSetting smoothing = new IntegerSetting(
        "Smoothing", 1, 20, 5
    );

    private final LinkedList<Double> speedHistory = new LinkedList<>();
    private double currentSpeed;
    private double prevX;
    private double prevZ;
    private boolean hasPrevPos;
    private Consumer<TickEvent> tickListener;

    public SpeedometerModule() {
        super(-1, 24);
    }

    @Override
    public String getId() { return "speedometer"; }
    @Override
    public String getName() { return "Speedometer"; }
    @Override
    public String getDescription() { return "Displays your current movement speed"; }
    @Override
    public boolean isEnabled() { return false; }

    @Override
    public List<Setting<?>> getSettings() {
        return List.of(unitMode, textColor, smoothing);
    }

    @Override
    public void onInitialize() {
        tickListener = this::onTick;
        CrestModules.getEventBus().subscribe(TickEvent.class, tickListener);
    }

    private void onTick(TickEvent event) {
        Minecraft mc = event.getClient();
        Player player = mc.player;
        if (player == null) return;

        double x = player.getX();
        double z = player.getZ();

        if (hasPrevPos) {
            double dx = x - prevX;
            double dz = z - prevZ;
            double dist = Math.sqrt(dx * dx + dz * dz);
            double speed = dist * 20.0;

            speedHistory.addLast(speed);
            while (speedHistory.size() > smoothing.get()) {
                speedHistory.removeFirst();
            }

            currentSpeed = speedHistory.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        }

        prevX = x;
        prevZ = z;
        hasPrevPos = true;
    }

    @Override
    public int getWidth() {
        String text = formatSpeed();
        return Minecraft.getInstance().font.width(text) + 4;
    }

    @Override
    public int getHeight() {
        return Minecraft.getInstance().font.lineHeight + 4;
    }

    @Override
    public void render(GuiGraphicsExtractor g, Minecraft mc, DeltaTracker d) {
        String text = formatSpeed();
        int w = mc.font.width(text);
        int rx = x < 0 ? mc.getWindow().getGuiScaledWidth() - w - 4 - 2 : x;
        int ry = y;

        g.fill(rx, ry, rx + w + 4, ry + mc.font.lineHeight + 4, 0x66000000);
        g.text(mc.font, Component.literal(text), rx + 2, ry + 2, textColor.get());
    }

    private String formatSpeed() {
        double display = currentSpeed;
        String suffix = " bps";
        if (unitMode.get() == 1) {
            display *= BLOCKS_TO_KMH;
            suffix = " km/h";
        }
        return String.format("%.1f%s", display, suffix);
    }
}
