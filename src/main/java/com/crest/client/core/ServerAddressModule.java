package com.crest.client.core;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;

public class ServerAddressModule extends HudModule {
    public ServerAddressModule() {
        super(-1, 80);
    }

    @Override public String getId() { return "server_address"; }
    @Override public String getName() { return "Server Address"; }
    @Override public String getDescription() { return "Shows current server IP or Singleplayer"; }
    @Override public boolean isEnabled() { return false; }

    @Override
    public int getWidth() { return 120; }

    @Override
    public int getHeight() { Minecraft mc = Minecraft.getInstance(); return mc.font.lineHeight + 4; }

    @Override
    public void render(GuiGraphicsExtractor g, Minecraft mc, DeltaTracker d) {
        if (mc.getSingleplayerServer() != null) {
            renderText(g, mc, "Singleplayer");
            return;
        }

        ServerData serverData = mc.getCurrentServer();
        if (serverData != null) {
            renderText(g, mc, serverData.ip);
        } else {
            renderText(g, mc, "Not connected");
        }
    }

    private void renderText(GuiGraphicsExtractor g, Minecraft mc, String text) {
        int rx = x < 0 ? mc.getWindow().getGuiScaledWidth() - getRenderWidth() : x;
        HudBackground.draw(g, rx, y, getRenderWidth(), mc.font.lineHeight + 4);
        g.text(mc.font, Component.literal(text), rx + 2, y + 2, 0xFFFFFFFF);
    }
}
