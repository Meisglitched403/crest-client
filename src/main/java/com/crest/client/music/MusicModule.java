package com.crest.client.music;

import net.minecraft.client.Minecraft;

public class MusicModule {
    private static MusicPlayer player;

    public static void init() {
        player = new MusicPlayer();
        player.setOnTrackStart(mp -> {
            var info = mp.getCurrentTrack().getInfo();
            String label = (info.title != null ? info.title : "Unknown")
                + " - " + (info.author != null ? info.author : "Unknown");
            String msg = "Playing: " + label;
            System.out.println("[Crest Music] " + msg);
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                String finalMsg = msg;
                mc.execute(() -> {
                    mc.player.sendSystemMessage(
                        net.minecraft.network.chat.Component.literal("[" + finalMsg + "]")
                    );
                });
            }
            MusicScreen screen = currentScreen();
            if (screen != null) screen.setStatus(msg);
        });

        player.setOnTrackEnd(mp -> {
            MusicScreen screen = currentScreen();
            if (screen != null) screen.setStatus("Track ended");
        });

        player.setOnError(msg -> {
            MusicScreen screen = currentScreen();
            if (screen != null) screen.setStatus(msg);
        });
    }

    public static MusicPlayer getPlayer() {
        return player;
    }

    private static MusicScreen currentScreen() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof MusicScreen ms) return ms;
        return null;
    }
}
