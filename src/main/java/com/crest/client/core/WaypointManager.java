package com.crest.client.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class WaypointManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final String UNASSIGNED_WORLD = "__unassigned__";
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("crest-waypoints.json");

    private static boolean enabled = true;
    private static int deathLimit = 3;

    private static final List<Waypoint> all = new ArrayList<>();

    private WaypointManager() {}

    /* -------------------- Public API -------------------- */

    public static void toggle() { enabled = !enabled; save(); }
    public static boolean isEnabled() { return enabled; }

    public static int getDeathLimit() { return deathLimit; }

    public static void setDeathLimit(int limit) {
        deathLimit = Math.max(0, Math.min(50, limit));
        save();
    }

    public static void addHere(Minecraft client, String name, Integer maybeColor) {
        ensureAssigned(client);
        var p = client.player;
        if (p == null || client.level == null) return;

        String world = currentWorldId(client);
        String dim = currentDimId(client);

        int x = (int) Math.floor(p.getX());
        int y = (int) Math.floor(p.getY());
        int z = (int) Math.floor(p.getZ());

        int color = (maybeColor != null) ? maybeColor : stableColor(name);

        all.removeIf(w -> world.equals(w.world) && dim.equals(w.dimension) && w.name.equalsIgnoreCase(name));

        Waypoint w = new Waypoint(name, world, dim, x, y, z, color);
        w.kind = "normal";
        w.createdAt = System.currentTimeMillis();
        all.add(w);

        save();
    }

    public static void set(Minecraft client, String name, int x, int y, int z, Integer maybeColor) {
        ensureAssigned(client);
        if (client.level == null) return;

        String world = currentWorldId(client);
        String dim = currentDimId(client);

        int color = (maybeColor != null) ? maybeColor : stableColor(name);

        all.removeIf(w -> world.equals(w.world) && dim.equals(w.dimension) && w.name.equalsIgnoreCase(name));

        Waypoint w = new Waypoint(name, world, dim, x, y, z, color);
        w.kind = "normal";
        w.createdAt = System.currentTimeMillis();
        all.add(w);

        save();
    }

    public static boolean remove(Minecraft client, String name) {
        ensureAssigned(client);
        if (client.level == null) return false;

        String world = currentWorldId(client);
        String dim = currentDimId(client);

        boolean ok = all.removeIf(w -> world.equals(w.world) && dim.equals(w.dimension) && w.name.equalsIgnoreCase(name));
        if (ok) save();
        return ok;
    }

    /** Rename/recolor an existing waypoint in the current world/dimension, keeping its position. */
    public static void edit(Minecraft client, String oldName, String newName, Integer maybeColor) {
        ensureAssigned(client);
        if (client.level == null) return;

        String world = currentWorldId(client);
        String dim = currentDimId(client);

        Waypoint target = null;
        for (Waypoint w : all) {
            if (world.equals(w.world) && dim.equals(w.dimension) && w.name.equalsIgnoreCase(oldName)) {
                target = w;
                break;
            }
        }
        if (target == null) return;

        String finalName = (newName == null || newName.isBlank()) ? oldName : newName.trim();

        var it = all.iterator();
        while (it.hasNext()) {
            Waypoint w = it.next();
            if (w != target && world.equals(w.world) && dim.equals(w.dimension) && w.name.equalsIgnoreCase(finalName)) {
                it.remove();
            }
        }

        target.name = finalName;
        if (maybeColor != null) target.color = maybeColor & 0xFFFFFF;
        save();
    }

    public static void clear(Minecraft client) {
        ensureAssigned(client);
        if (client.level == null) return;

        String world = currentWorldId(client);
        String dim = currentDimId(client);

        all.removeIf(w -> world.equals(w.world) && dim.equals(w.dimension));
        save();
    }

    public static List<Waypoint> listForCurrent(Minecraft client) {
        ensureAssigned(client);
        if (client.level == null) return List.of();

        String world = currentWorldId(client);
        String dim = currentDimId(client);

        ArrayList<Waypoint> out = new ArrayList<>();
        for (var w : all) {
            if (world.equals(w.world) && dim.equals(w.dimension)) out.add(w);
        }
        return out;
    }

    /** All waypoints across every world/dimension (used for naming, etc.). */
    public static List<Waypoint> getAll() {
        return new ArrayList<>(all);
    }

    /* -------------------- Death waypoints -------------------- */

    public static void addDeath(Minecraft client) {
        if (deathLimit <= 0) return;
        ensureAssigned(client);
        if (client.player == null || client.level == null) return;

        String world = currentWorldId(client);
        String dim = currentDimId(client);

        int x = (int) Math.floor(client.player.getX());
        int y = (int) Math.floor(client.player.getY());
        int z = (int) Math.floor(client.player.getZ());

        Waypoint w = new Waypoint("LastDeath", world, dim, x, y, z, 0xFF5555);
        w.kind = "death";
        w.createdAt = System.currentTimeMillis();
        all.add(w);

        normalizeDeaths(world, dim);
        save();
    }

    private static void normalizeDeaths(String world, String dim) {
        var deaths = new ArrayList<Waypoint>();
        for (var w : all) {
            if (world.equals(w.world) && dim.equals(w.dimension) && "death".equals(w.kind)) {
                deaths.add(w);
            }
        }

        deaths.sort((a, b) -> Long.compare(b.createdAt, a.createdAt));

        for (int i = deathLimit; i < deaths.size(); i++) {
            all.remove(deaths.get(i));
        }

        int keep = Math.min(deathLimit, deaths.size());
        for (int i = 0; i < keep; i++) {
            Waypoint w = deaths.get(i);
            if (i == 0) w.name = "LastDeath";
            else w.name = "Death" + (i + 1);
        }
    }

    /* -------------------- World/Dim IDs -------------------- */

    public static String currentDimId(Minecraft client) {
        if (client.level == null) return "unknown";
        return client.level.dimension().identifier().toString();
    }

    public static String currentWorldId(Minecraft client) {
        var server = client.getSingleplayerServer();
        if (server != null) {
            Path root = server.getWorldPath(LevelResource.ROOT);
            return "sp:" + root.toAbsolutePath().normalize();
        }

        ServerData info = client.getCurrentServer();
        if (info != null && info.ip != null && !info.ip.isBlank()) {
            return "mp:" + info.ip.trim().toLowerCase(Locale.ROOT);
        }

        if (client.getConnection() != null && client.getConnection().getConnection() != null) {
            var addr = client.getConnection().getConnection().getRemoteAddress();
            if (addr != null) return "mp:" + addr;
        }

        return "unknown";
    }

    /* -------------------- Load/Save + migration -------------------- */

    public static void load() {
        all.clear();
        enabled = true;
        deathLimit = 3;

        if (!Files.exists(FILE)) return;

        try {
            String json = Files.readString(FILE, StandardCharsets.UTF_8);
            JsonObject root;
            if (json.isBlank()) return;

            if (json.trim().startsWith("[")) {
                // Old crest format: a bare JSON array of waypoints (no world keys).
                root = new JsonObject();
                root.add("waypoints", JsonParser.parseString(json).getAsJsonArray());
            } else {
                root = JsonParser.parseString(json).getAsJsonObject();
            }

            enabled = root.has("enabled") && root.get("enabled").getAsBoolean();
            deathLimit = root.has("deathLimit") ? root.get("deathLimit").getAsInt() : 3;

            if (root.has("waypoints") && root.get("waypoints").isJsonArray()) {
                for (JsonElement e : root.getAsJsonArray("waypoints")) {
                    if (!e.isJsonObject()) continue;
                    JsonObject o = e.getAsJsonObject();

                    String name = optString(o, "name", "wp");
                    String world = optString(o, "world", UNASSIGNED_WORLD);
                    String dim = optString(o, "dimension", "unknown");

                    int x = optInt(o, "x", 0);
                    int y = optInt(o, "y", 64);
                    int z = optInt(o, "z", 0);
                    int color = optInt(o, "color", stableColor(name)) & 0xFFFFFF;

                    String kind = optString(o, "kind", "normal");
                    long createdAt = o.has("createdAt") ? o.get("createdAt").getAsLong() : System.currentTimeMillis();

                    Waypoint w = new Waypoint(name, world, dim, x, y, z, color);
                    w.kind = kind;
                    w.createdAt = createdAt;
                    all.add(w);
                }
            }
        } catch (Exception ignored) {
            all.clear();
            enabled = true;
            deathLimit = 3;
        }
    }

    public static void save() {
        try {
            Files.createDirectories(FILE.getParent());

            JsonObject root = new JsonObject();
            root.addProperty("enabled", enabled);
            root.addProperty("deathLimit", deathLimit);

            JsonArray arr = new JsonArray();
            for (var w : all) {
                JsonObject o = new JsonObject();
                o.addProperty("name", w.name);
                o.addProperty("world", w.world);
                o.addProperty("dimension", w.dimension);
                o.addProperty("x", w.x);
                o.addProperty("y", w.y);
                o.addProperty("z", w.z);
                o.addProperty("color", w.color);
                o.addProperty("kind", w.kind == null ? "normal" : w.kind);
                o.addProperty("createdAt", w.createdAt == 0 ? System.currentTimeMillis() : w.createdAt);
                arr.add(o);
            }
            root.add("waypoints", arr);

            Files.writeString(
                    FILE,
                    GSON.toJson(root),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            );
        } catch (IOException ignored) {}
    }

    public static void clearDeaths(Minecraft client) {
        ensureAssigned(client);
        if (client.level == null) return;

        String world = currentWorldId(client);
        String dim = currentDimId(client);

        all.removeIf(w -> world.equals(w.world) && dim.equals(w.dimension) && "death".equals(w.kind));
        save();
    }

    /** Old entries saved without a world key get claimed by the world you're in on first use. */
    private static void ensureAssigned(Minecraft client) {
        String world = currentWorldId(client);
        if (world.equals("unknown")) return;

        boolean changed = false;
        for (var w : all) {
            if (UNASSIGNED_WORLD.equals(w.world)) {
                w.world = world;
                changed = true;
            }
        }
        if (changed) save();
    }

    /* -------------------- Helpers -------------------- */

    private static String optString(JsonObject o, String k, String def) {
        return o.has(k) ? o.get(k).getAsString() : def;
    }

    private static int optInt(JsonObject o, String k, int def) {
        return o.has(k) ? o.get(k).getAsInt() : def;
    }

    private static int stableColor(String name) {
        int h = name == null ? 0 : name.toLowerCase(Locale.ROOT).hashCode();
        int r = 64 + (Math.abs(h) % 192);
        int g = 64 + (Math.abs(h / 31) % 192);
        int b = 64 + (Math.abs(h / 997) % 192);
        return (r << 16) | (g << 8) | b;
    }
}
