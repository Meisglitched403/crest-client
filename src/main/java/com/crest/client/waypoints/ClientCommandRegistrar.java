package com.crest.client.waypoints;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;

import com.crest.client.waypoints.WaypointTemplateService;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import java.util.function.Supplier;

/** Registers the /wp client command tree. */
public final class ClientCommandRegistrar {
    private ClientCommandRegistrar() {}

    public static void register(Supplier<WaypointManager> mgrSup, Supplier<ModConfig> cfgSup) {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registry) ->
                register(dispatcher, mgrSup, cfgSup));
    }

    private static void register(CommandDispatcher<FabricClientCommandSource> dispatcher,
                                 Supplier<WaypointManager> mgrSup, Supplier<ModConfig> cfgSup) {
        LiteralCommandNode<FabricClientCommandSource> root = dispatcher.register(ClientCommands.literal("wp"));

        root.addChild(ClientCommands.literal("add")
                .then(ClientCommands.argument("name", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            Minecraft mc = Minecraft.getInstance();
                            WaypointManager mgr = mgrSup.get();
                            if (mgr == null || mc.player == null || mc.level == null) return 0;
                            String name = StringArgumentType.getString(ctx, "name");
                            int color = WaypointColorService.pickNewWaypointColor(cfgSup.get(), Waypoint.Type.NORMAL);
                            mgr.createWaypoint(mc.player.blockPosition(), name, color, "block:grass", false, Waypoint.Type.NORMAL);
                            ctx.getSource().sendFeedback(Component.literal("Added waypoint: " + name));
                            return 1;
                        })).build());

        root.addChild(ClientCommands.literal("set")
                .then(ClientCommands.argument("name", StringArgumentType.word())
                        .then(ClientCommands.argument("x", IntegerArgumentType.integer())
                                .then(ClientCommands.argument("y", IntegerArgumentType.integer())
                                        .then(ClientCommands.argument("z", IntegerArgumentType.integer())
                                                .executes(ctx -> {
                                                    WaypointManager mgr = mgrSup.get();
                                                    if (mgr == null) return 0;
                                                    String name = StringArgumentType.getString(ctx, "name");
                                                    int x = IntegerArgumentType.getInteger(ctx, "x");
                                                    int y = IntegerArgumentType.getInteger(ctx, "y");
                                                    int z = IntegerArgumentType.getInteger(ctx, "z");
                                                    int color = WaypointColorService.pickNewWaypointColor(cfgSup.get(), Waypoint.Type.NORMAL);
                                                    mgr.createWaypoint(new BlockPos(x, y, z), name, color, "block:grass", false, Waypoint.Type.NORMAL);
                                                    ctx.getSource().sendFeedback(Component.literal("Set waypoint: " + name));
                                                    return 1;
                                                })))))
                .build());

        root.addChild(ClientCommands.literal("list").executes(ctx -> {
            WaypointManager mgr = mgrSup.get();
            if (mgr == null) return 0;
            var list = mgr.getWaypoints();
            if (list.isEmpty()) { ctx.getSource().sendFeedback(Component.literal("No waypoints.")); return 1; }
            ctx.getSource().sendFeedback(Component.literal("Waypoints (" + list.size() + "):"));
            for (var w : list) ctx.getSource().sendFeedback(Component.literal("- " + w.getName() + " @ " + w.getX() + " " + w.getY() + " " + w.getZ()));
            return 1;
        }).build());

        root.addChild(ClientCommands.literal("remove")
                .then(ClientCommands.argument("name", StringArgumentType.word())
                        .executes(ctx -> {
                            WaypointManager mgr = mgrSup.get();
                            if (mgr == null) return 0;
                            String name = StringArgumentType.getString(ctx, "name");
                            Waypoint w = findByName(mgr, name);
                            if (w != null) { mgr.removeWaypoint(w); ctx.getSource().sendFeedback(Component.literal("Removed: " + name)); }
                            else ctx.getSource().sendFeedback(Component.literal("Not found: " + name));
                            return 1;
                        })).build());

        root.addChild(ClientCommands.literal("clear").executes(ctx -> {
            WaypointManager mgr = mgrSup.get();
            if (mgr == null) return 0;
            mgr.clearAll();
            ctx.getSource().sendFeedback(Component.literal("Cleared all waypoints."));
            return 1;
        }).build());

        root.addChild(ClientCommands.literal("toggle").executes(ctx -> {
            ctx.getSource().sendFeedback(Component.literal("Toggle handled by HUD visibility."));
            return 1;
        }).build());

        root.addChild(ClientCommands.literal("template")
                .then(ClientCommands.literal("save")
                        .then(ClientCommands.argument("name", StringArgumentType.word())
                                .executes(ctx -> {
                                    WaypointManager mgr = mgrSup.get();
                                    if (mgr == null) return 0;
                                    Minecraft mc = Minecraft.getInstance();
                                    BlockPos p = mc.player != null ? mc.player.blockPosition() : BlockPos.ZERO;
                                    String name = StringArgumentType.getString(ctx, "name");
                                    Waypoint sample = mgr.createWaypoint(p, name, 0xFFFFFF00, "block:grass", false, Waypoint.Type.NORMAL);
                                    WaypointTemplateService.put(name, sample);
                                    mgr.removeWaypoint(sample);
                                    ctx.getSource().sendFeedback(Component.literal("Saved template: " + name));
                                    return 1;
                                })))
                .then(ClientCommands.literal("load")
                        .then(ClientCommands.argument("name", StringArgumentType.word())
                                .executes(ctx -> {
                                    WaypointManager mgr = mgrSup.get();
                                    if (mgr == null) return 0;
                                    Minecraft mc = Minecraft.getInstance();
                                    BlockPos p = mc.player != null ? mc.player.blockPosition() : BlockPos.ZERO;
                                    String name = StringArgumentType.getString(ctx, "name");
                                    Waypoint w = WaypointTemplateService.instantiate(name, p);
                                    if (w != null) {
                                        mgr.getWaypoints().add(w);
                                        ctx.getSource().sendFeedback(Component.literal("Created from template: " + name));
                                    } else {
                                        ctx.getSource().sendFeedback(Component.literal("No such template: " + name));
                                    }
                                    return 1;
                                })))
                .executes(ctx -> {
                    ctx.getSource().sendFeedback(Component.literal("Usage: /wp template save <name> | /wp template load <name>"));
                    return 1;
                })
                .build());
    }

    private static Waypoint findByName(WaypointManager mgr, String name) {
        for (Waypoint w : mgr.getWaypoints()) if (w.getName().equalsIgnoreCase(name)) return w;
        return null;
    }
}
