package com.crest.client.waypoints;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.function.BiConsumer;

/** Makes chat coordinates clickable to create waypoints. */
public final class ChatWaypointIntegration {
    public static final String ADD_COMMAND = "/wp add ";

    private ChatWaypointIntegration() {}

    public static Component withClickableCoords(Component original, Runnable onCreate) {
        String plain = original.getString();
        ChatCoordinateService.Coordinates c = ChatCoordinateService.parse(plain);
        if (c == null) return original;
        MutableComponent copy = Component.literal(plain);
        copy.setStyle(original.getStyle().withClickEvent(
                new net.minecraft.network.chat.ClickEvent.RunCommand(
                        ADD_COMMAND + ChatCoordinateService.format(c))));
        return copy;
    }

    public static boolean handleIncomingMessage(Component msg, BiConsumer<ChatCoordinateService.Coordinates, String> creator) {
        String plain = msg.getString();
        ChatCoordinateService.Coordinates c = ChatCoordinateService.parse(plain);
        if (c == null) return false;
        creator.accept(c, plain);
        return true;
    }
}
