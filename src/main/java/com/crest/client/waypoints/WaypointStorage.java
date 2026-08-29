package com.crest.client.waypoints;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import net.minecraft.core.BlockPos;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Per-world / per-dimension JSON persistence for waypoints, clumps, groups and folders. */
public final class WaypointStorage {

    public static final int CURRENT_VERSION = 10;

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(BlockPos.class, new BlockPosAdapter())
            .create();

    private static volatile WorldLocation activeLocation;

    public static final class WorldLocation {
        public final ModPaths.WorldKind kind;
        public final String id;
        public WorldLocation(ModPaths.WorldKind kind, String id) { this.kind = kind; this.id = id; }
    }

    public static final class DimensionKey {
        public final String id;
        public DimensionKey(String id) { this.id = id; }
    }

    public static final class LoadResult {
        public List<Waypoint> waypoints = new ArrayList<>();
        public Map<String, ClumpData> clumps = new HashMap<>();
        public Map<String, GroupData> groups = new HashMap<>();
        public Map<String, FolderData> folders = new HashMap<>();
        public Set<String> foldedGroups = new HashSet<>();
        public Set<String> foldedFolders = new HashSet<>();
        public List<String> navEntries = new ArrayList<>();
    }

    public static void setActiveLocation(WorldLocation loc) { activeLocation = loc; }
    public static WorldLocation getActiveLocation() { return activeLocation; }
    public static void clearActiveLocation() { activeLocation = null; }

    public static Path getPath(String worldId, DimensionKey dim) {
        return ModPaths.worldDir(ModPaths.WorldKind.MULTIPLAYER, worldId).resolve(ModPaths.dimensionFileName(dim.id));
    }

    public static Path getPathForDimensionId(String worldId, String dimId) {
        return ModPaths.worldDir(ModPaths.WorldKind.MULTIPLAYER, worldId).resolve(ModPaths.dimensionFileName(dimId));
    }

    public static boolean hasData(String worldId, DimensionKey dim) {
        return Files.exists(getPath(worldId, dim));
    }

    public static LoadResult load(String worldId, DimensionKey dim) {
        Path p = getPath(worldId, dim);
        if (!Files.exists(p)) return new LoadResult();
        try {
            String json = Files.readString(p, StandardCharsets.UTF_8);
            return tryLoad(json);
        } catch (Exception e) {
            return new LoadResult();
        }
    }

    public static LoadResult loadByDimensionId(String worldId, String dimId) {
        return load(worldId, new DimensionKey(dimId));
    }

    private static LoadResult tryLoad(String json) {
        LoadResult res = new LoadResult();
        var root = com.google.gson.JsonParser.parseString(json).getAsJsonObject();
        if (root.has("version") && root.get("version").getAsInt() != CURRENT_VERSION) {
            // still attempt best-effort read
        }
        if (root.has("waypoints") && root.get("waypoints").isJsonArray()) {
            for (var e : root.getAsJsonArray("waypoints")) {
                if (e.isJsonObject()) {
                    Waypoint w = GSON.fromJson(e, Waypoint.class);
                    if (w != null) res.waypoints.add(w);
                }
            }
        }
        res.clumps.putAll(readMap(root, "clumps", ClumpData.class));
        res.groups.putAll(readMap(root, "groups", GroupData.class));
        res.folders.putAll(readMap(root, "folders", FolderData.class));
        readCollapsedIds(root, "foldedGroups", res.foldedGroups);
        readCollapsedIds(root, "foldedFolders", res.foldedFolders);
        if (root.has("nav") && root.get("nav").isJsonArray()) {
            for (var e : root.getAsJsonArray("nav")) res.navEntries.add(e.getAsString());
        }
        return res;
    }

    private static <T> Map<String, T> readMap(com.google.gson.JsonObject root, String key, Class<T> cls) {
        Map<String, T> out = new HashMap<>();
        if (root.has(key) && root.get(key).isJsonObject()) {
            for (var entry : root.getAsJsonObject(key).entrySet()) {
                T v = GSON.fromJson(entry.getValue(), cls);
                if (v != null) out.put(entry.getKey(), v);
            }
        }
        return out;
    }

    private static void readCollapsedIds(com.google.gson.JsonObject root, String key, Set<String> set) {
        if (root.has(key) && root.get(key).isJsonArray()) {
            for (var e : root.getAsJsonArray(key)) set.add(e.getAsString());
        }
    }

    public static void save(String worldId, DimensionKey dim,
                            List<Waypoint> waypoints,
                            Map<String, ClumpData> clumps,
                            Map<String, GroupData> groups,
                            Map<String, FolderData> folders,
                            Set<String> foldedGroups,
                            Set<String> foldedFolders,
                            List<String> navEntries) {
        Path p = getPath(worldId, dim);
        try {
            Files.createDirectories(p.getParent());
            var root = buildRoot(waypoints, clumps, groups, folders, foldedGroups, foldedFolders, navEntries);
            Files.writeString(p, GSON.toJson(root), StandardCharsets.UTF_8);
        } catch (IOException ignored) {}
    }

    public static void saveByDimensionId(String worldId, String dimId,
                                         List<Waypoint> waypoints,
                                         Map<String, ClumpData> clumps,
                                         Map<String, GroupData> groups,
                                         Map<String, FolderData> folders,
                                         Set<String> foldedGroups,
                                         Set<String> foldedFolders,
                                         List<String> navEntries) {
        save(worldId, new DimensionKey(dimId), waypoints, clumps, groups, folders, foldedGroups, foldedFolders, navEntries);
    }

    private static com.google.gson.JsonObject buildRoot(List<Waypoint> waypoints,
                                                       Map<String, ClumpData> clumps,
                                                       Map<String, GroupData> groups,
                                                       Map<String, FolderData> folders,
                                                       Set<String> foldedGroups,
                                                       Set<String> foldedFolders,
                                                       List<String> navEntries) {
        var root = new com.google.gson.JsonObject();
        root.addProperty("version", CURRENT_VERSION);
        var arr = new com.google.gson.JsonArray();
        for (Waypoint w : waypoints) arr.add(GSON.toJsonTree(w));
        root.add("waypoints", arr);
        root.add("clumps", toObj(clumps));
        root.add("groups", toObj(groups));
        root.add("folders", toObj(folders));
        var fg = new com.google.gson.JsonArray();
        for (String s : foldedGroups) fg.add(s);
        root.add("foldedGroups", fg);
        var ff = new com.google.gson.JsonArray();
        for (String s : foldedFolders) ff.add(s);
        root.add("foldedFolders", ff);
        var nav = new com.google.gson.JsonArray();
        for (String s : navEntries) nav.add(s);
        root.add("nav", nav);
        return root;
    }

    private static <T> com.google.gson.JsonObject toObj(Map<String, T> map) {
        var o = new com.google.gson.JsonObject();
        for (var e : map.entrySet()) o.add(e.getKey(), GSON.toJsonTree(e.getValue()));
        return o;
    }

    private static final class BlockPosAdapter extends TypeAdapter<BlockPos> {
        @Override
        public void write(JsonWriter out, BlockPos v) throws IOException {
            out.beginArray();
            out.value(v.getX());
            out.value(v.getY());
            out.value(v.getZ());
            out.endArray();
        }

        @Override
        public BlockPos read(JsonReader in) throws IOException {
            in.beginArray();
            int x = in.nextInt();
            int y = in.nextInt();
            int z = in.nextInt();
            in.endArray();
            return new BlockPos(x, y, z);
        }
    }
}
