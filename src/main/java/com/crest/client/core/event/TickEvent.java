package com.crest.client.core.event;

import net.minecraft.client.Minecraft;

public class TickEvent implements Event {
    private final Minecraft client;

    public TickEvent(Minecraft client) {
        this.client = client;
    }

    public Minecraft getClient() { return client; }
}
