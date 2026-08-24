package com.crest.client.core;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.Locale;

public final class WaypointCommands {
    private WaypointCommands() {}

    private static int parseRgb(String s) {
        String t = s.startsWith("#") ? s.substring(1) : s;
        if (t.length() != 6) throw new IllegalArgumentException("hex must be RRGGBB");
        return Integer.parseInt(t, 16) & 0xFFFFFF;
    }

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        LiteralCommandNode<FabricClientCommandSource> root = dispatcher.register(ClientCommands.literal("wp"));

        // /wp add <name>
        root.addChild(
            ClientCommands.literal("add")
                .then(ClientCommands.argument("name", StringArgumentType.word())
                    .executes(ctx -> {
                        Minecraft client = Minecraft.getInstance();
                        if (client.player == null || client.level == null) return 0;

                        String name = StringArgumentType.getString(ctx, "name");
                        WaypointManager.addHere(client, name, null);

                        ctx.getSource().sendFeedback(Component.literal("Added waypoint: " + name));
                        return 1;
                    })
                ).build()
        );

        // /wp set <name> <x> <y> <z>
        root.addChild(
            ClientCommands.literal("set")
                .then(ClientCommands.argument("name", StringArgumentType.word())
                    .then(ClientCommands.argument("x", IntegerArgumentType.integer())
                        .then(ClientCommands.argument("y", IntegerArgumentType.integer())
                            .then(ClientCommands.argument("z", IntegerArgumentType.integer())
                                .executes(ctx -> {
                                    Minecraft client = Minecraft.getInstance();
                                    if (client.level == null) return 0;

                                    String name = StringArgumentType.getString(ctx, "name");
                                    int x = IntegerArgumentType.getInteger(ctx, "x");
                                    int y = IntegerArgumentType.getInteger(ctx, "y");
                                    int z = IntegerArgumentType.getInteger(ctx, "z");

                                    WaypointManager.set(client, name, x, y, z, null);

                                    ctx.getSource().sendFeedback(Component.literal("Set waypoint: " + name));
                                    return 1;
                                })
                            )
                        )
                    )
                ).build()
        );

        // /wp list
        root.addChild(
            ClientCommands.literal("list").executes(ctx -> {
                Minecraft client = Minecraft.getInstance();
                if (client.level == null) return 0;

                String worldKey = WaypointManager.currentWorldId(client);
                String dim = WaypointManager.currentDimId(client);

                var list = WaypointManager.listForCurrent(client);
                if (list.isEmpty()) {
                    ctx.getSource().sendFeedback(Component.literal("No waypoints here."));
                    return 1;
                }

                ctx.getSource().sendFeedback(Component.literal("Waypoints (" + worldKey + ", " + dim + "):"));
                for (var wp : list) {
                    ctx.getSource().sendFeedback(
                        Component.literal("- " + wp.name + " @ " + wp.x + " " + wp.y + " " + wp.z +
                            " #" + String.format("%06X", wp.color & 0xFFFFFF))
                    );
                }
                return 1;
            }).build()
        );

        // /wp remove <name>
        root.addChild(
            ClientCommands.literal("remove")
                .then(ClientCommands.argument("name", StringArgumentType.word())
                    .executes(ctx -> {
                        Minecraft client = Minecraft.getInstance();
                        if (client.level == null) return 0;

                        String name = StringArgumentType.getString(ctx, "name");
                        boolean ok = WaypointManager.remove(client, name);

                        ctx.getSource().sendFeedback(Component.literal(ok ? "Removed: " + name : "Not found: " + name));
                        return 1;
                    })
                ).build()
        );

        // /wp clear [/wp clear death]
        root.addChild(
            ClientCommands.literal("clear")
                .executes(ctx -> {
                    Minecraft client = Minecraft.getInstance();
                    if (client.level == null) return 0;

                    WaypointManager.clear(client);
                    ctx.getSource().sendFeedback(Component.literal("Cleared waypoints in this world+dimension."));
                    return 1;
                })
                .then(ClientCommands.literal("death")
                    .executes(ctx -> {
                        Minecraft client = Minecraft.getInstance();
                        if (client.level == null) return 0;

                        WaypointManager.clearDeaths(client);
                        ctx.getSource().sendFeedback(Component.literal("Cleared death waypoints in this world+dimension."));
                        return 1;
                    })
                )
                .build()
        );

        // /wp toggle
        root.addChild(
            ClientCommands.literal("toggle").executes(ctx -> {
                WaypointManager.toggle();
                ctx.getSource().sendFeedback(Component.literal(
                    "Waypoints rendering: " + (WaypointManager.isEnabled() ? "ON" : "OFF")
                ));
                return 1;
            }).build()
        );

        // /wp color <name> <hex>
        root.addChild(
            ClientCommands.literal("color")
                .then(ClientCommands.argument("name", StringArgumentType.word())
                    .then(ClientCommands.argument("hex", StringArgumentType.word())
                        .executes(ctx -> {
                            Minecraft client = Minecraft.getInstance();
                            if (client.level == null) return 0;

                            String name = StringArgumentType.getString(ctx, "name");
                            String hex = StringArgumentType.getString(ctx, "hex");

                            int rgb;
                            try {
                                rgb = parseRgb(hex);
                            } catch (Exception e) {
                                ctx.getSource().sendFeedback(Component.literal("Bad hex. Use RRGGBB or #RRGGBB"));
                                return 0;
                            }

                            var list = WaypointManager.listForCurrent(client);
                            var found = list.stream().filter(w -> w.name.equalsIgnoreCase(name)).findFirst();
                            if (found.isEmpty()) {
                                ctx.getSource().sendFeedback(Component.literal("Not found: " + name));
                                return 0;
                            }
                            var wp = found.get();
                            WaypointManager.set(client, wp.name, wp.x, wp.y, wp.z, rgb);

                            ctx.getSource().sendFeedback(Component.literal("Color set for " + name));
                            return 1;
                        })
                    )
                ).build()
        );

        // /wp deathlimit <0..50>
        root.addChild(
            ClientCommands.literal("deathlimit")
                .then(ClientCommands.argument("count", IntegerArgumentType.integer(0, 50))
                    .executes(ctx -> {
                        int n = IntegerArgumentType.getInteger(ctx, "count");
                        WaypointManager.setDeathLimit(n);
                        ctx.getSource().sendFeedback(Component.literal("Death waypoints limit: " + WaypointManager.getDeathLimit()));
                        return 1;
                    })
                ).build()
        );
    }
}
