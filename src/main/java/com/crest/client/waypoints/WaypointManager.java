package com.crest.client.waypoints;

import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** In-memory store of waypoints plus clumps, groups and folders. */
public class WaypointManager {

    private final List<Waypoint> waypoints = new ArrayList<>();
    private final Map<String, ClumpData> clumpsById = new LinkedHashMap<>();
    private final Map<String, GroupData> groupsById = new LinkedHashMap<>();
    private final Map<String, FolderData> foldersById = new LinkedHashMap<>();
    private final Map<String, String> groupIdByWaypointId = new HashMap<>();
    private final Map<String, String> folderIdByWaypointId = new HashMap<>();
    private final Map<String, String> unnamedClumpFallbackNameById = new HashMap<>();
    private boolean unnamedClumpFallbackDirty;

    public Waypoint createWaypoint(BlockPos pos, String name, int color, String iconId, boolean death, Waypoint.Type type) {
        Waypoint wp = new Waypoint(UUID.randomUUID().toString(), pos, name, color, iconId, death, type);
        waypoints.add(wp);
        return wp;
    }

    public Waypoint createWaypointWithId(String id, BlockPos pos, String name, int color, String iconId, boolean death, Waypoint.Type type) {
        Waypoint wp = new Waypoint(id, pos, name, color, iconId, death, type);
        waypoints.add(wp);
        return wp;
    }

    public List<Waypoint> getWaypoints() { return waypoints; }
    public List<Waypoint> getInternal() { return waypoints; }
    public Map<String, ClumpData> getClumpsByIdInternal() { return clumpsById; }
    public Map<String, GroupData> getGroupsByIdInternal() { return groupsById; }
    public Map<String, FolderData> getFoldersByIdInternal() { return foldersById; }

    public ClumpData getClumpData(String id) { return clumpsById.get(id); }
    public GroupData getGroupData(String id) { return groupsById.get(id); }
    public FolderData getFolderData(String id) { return foldersById.get(id); }

    public String getGroupIdForWaypoint(String wpId) { return groupIdByWaypointId.get(wpId); }
    public String getFolderIdForWaypoint(String wpId) { return folderIdByWaypointId.get(wpId); }

    public List<Waypoint> getGroupMembers(String groupId) {
        GroupData g = groupsById.get(groupId);
        List<Waypoint> out = new ArrayList<>();
        if (g != null) for (String id : g.getMemberIds()) {
            Waypoint w = getById(id);
            if (w != null) out.add(w);
        }
        return out;
    }

    public List<Waypoint> getFolderMembers(String folderId) {
        FolderData f = foldersById.get(folderId);
        List<Waypoint> out = new ArrayList<>();
        if (f != null) for (String id : f.getMemberIds()) {
            Waypoint w = getById(id);
            if (w != null) out.add(w);
        }
        return out;
    }

    public boolean moveWaypointBefore(String target, String other) {
        int ti = indexOf(target), oi = indexOf(other);
        if (ti < 0 || oi < 0 || ti == oi) return false;
        waypoints.remove(ti);
        int newOi = indexOf(other);
        waypoints.add(newOi, getById(target));
        return true;
    }

    public boolean moveWaypointAfter(String target, String other) {
        int ti = indexOf(target), oi = indexOf(other);
        if (ti < 0 || oi < 0 || ti == oi) return false;
        waypoints.remove(ti);
        int newOi = indexOf(other);
        waypoints.add(newOi + 1, getById(target));
        return true;
    }

    public boolean moveGroupMemberBefore(String groupId, String target, String other) {
        GroupData g = groupsById.get(groupId);
        return g != null && g.moveMemberBefore(target, other);
    }

    public boolean moveGroupMemberAfter(String groupId, String target, String other) {
        GroupData g = groupsById.get(groupId);
        return g != null && g.moveMemberAfter(target, other);
    }

    public Waypoint getById(String id) {
        for (Waypoint w : waypoints) if (w.getId().equals(id)) return w;
        return null;
    }

    public Waypoint getWaypointAt(BlockPos pos) {
        for (Waypoint w : waypoints) if (w.getPosition() != null && w.getPosition().equals(pos)) return w;
        return null;
    }

    public Waypoint getClumpAt(BlockPos pos) {
        for (Waypoint w : waypoints) {
            ClumpData c = clumpsById.get(w.getId());
            if (c != null && c.contains(pos)) return w;
        }
        return null;
    }

    public boolean isOccupied(BlockPos pos) {
        return getWaypointAt(pos) != null || getClumpAt(pos) != null;
    }

    public boolean hasNormalAt(BlockPos pos, String excludeId) {
        Waypoint w = getWaypointAt(pos);
        return w != null && !w.getId().equals(excludeId) && clumpsById.get(w.getId()) == null;
    }

    public int clumpSizeAt(BlockPos pos) {
        Waypoint w = getClumpAt(pos);
        return w == null ? 0 : clumpsById.get(w.getId()).size();
    }

    public Waypoint clumpFrom(BlockPos pos) { return getClumpAt(pos); }

    public Waypoint clumpSelected(List<Waypoint> list) {
        if (list == null || list.isEmpty()) return null;
        for (Waypoint w : list) if (clumpsById.containsKey(w.getId())) return w;
        return null;
    }

    public Waypoint connectWaypoints(Waypoint a, Waypoint b) {
        if (a == null || b == null) return null;
        List<Waypoint> members = new ArrayList<>();
        members.add(a); members.add(b);
        return createClumpFromMembers(members, a);
    }

    public Waypoint createClumpFromMembers(List<Waypoint> members, Waypoint anchor) {
        String clumpId = UUID.randomUUID().toString();
        ClumpData cd = new ClumpData(clumpId);
        for (Waypoint w : members) {
            if (w == null) continue;
            cd.putMember(new ClumpData.Member(w.getPosition(), w.getColor(), w.getIconId()));
            waypoints.remove(w);
            groupIdByWaypointId.remove(w.getId());
            folderIdByWaypointId.remove(w.getId());
        }
        clumpsById.put(clumpId, cd);
        Waypoint clump = new Waypoint(clumpId, anchor.getPosition(), firstNonBlankName(members), anchor.getColor(), anchor.getIconId(), false, Waypoint.Type.NORMAL);
        waypoints.add(clump);
        return clump;
    }

    public Waypoint tryAbsorbIntoAdjacentOpenClump(BlockPos pos, String iconId) {
        Waypoint existing = getWaypointAt(pos);
        if (existing == null) return null;
        // simple absorb: extend an open adjacent clump
        for (Waypoint w : waypoints) {
            ClumpData c = clumpsById.get(w.getId());
            if (c != null && c.isOpen()) {
                c.putMember(new ClumpData.Member(pos, existing.getColor(), iconId));
                waypoints.remove(existing);
                return w;
            }
        }
        return null;
    }

    public Waypoint getSingleAdjacentOpenClump(BlockPos pos) {
        for (Waypoint w : waypoints) {
            ClumpData c = clumpsById.get(w.getId());
            if (c != null && c.isOpen()) return w;
        }
        return null;
    }

    public boolean addMemberToClump(Waypoint clump, BlockPos pos, String iconId) {
        ClumpData c = clumpsById.get(clump.getId());
        if (c == null) return false;
        c.putMember(new ClumpData.Member(pos, clump.getColor(), iconId));
        return true;
    }

    public Waypoint createGroup(String name, List<Waypoint> members) {
        String id = UUID.randomUUID().toString();
        GroupData g = new GroupData(id);
        groupsById.put(id, g);
        for (Waypoint w : members) {
            if (w == null) continue;
            g.addMember(w.getId());
            groupIdByWaypointId.put(w.getId(), id);
        }
        return createWaypointWithId(UUID.randomUUID().toString(), members.isEmpty() ? new BlockPos(0,0,0) : members.get(0).getPosition(),
                name, 0xAA66FF, "block:grass", false, Waypoint.Type.NORMAL);
    }

    public Waypoint createEmptyGroup(String name, BlockPos pos, int color) {
        String id = UUID.randomUUID().toString();
        groupsById.put(id, new GroupData(id));
        return createWaypointWithId(UUID.randomUUID().toString(), pos, name, color, "block:grass", false, Waypoint.Type.NORMAL);
    }

    public boolean addMemberToGroup(Waypoint group, Waypoint member) {
        GroupData g = groupsById.get(group.getId());
        if (g == null) return false;
        boolean added = g.addMember(member.getId());
        if (added) groupIdByWaypointId.put(member.getId(), group.getId());
        return added;
    }

    public void removeWaypointFromGroup(String wpId) {
        String gid = groupIdByWaypointId.remove(wpId);
        if (gid != null) {
            GroupData g = groupsById.get(gid);
            if (g != null) g.removeMember(wpId);
        }
    }

    public void ungroup(Waypoint group) {
        String gid = group.getId();
        GroupData g = groupsById.remove(gid);
        if (g != null) {
            for (String id : g.getMemberIds()) groupIdByWaypointId.remove(id);
        }
        waypoints.remove(group);
    }

    public Waypoint createEmptyFolder(String name, int color) {
        String id = UUID.randomUUID().toString();
        foldersById.put(id, new FolderData(id));
        return createWaypointWithId(UUID.randomUUID().toString(), new BlockPos(0,0,0), name, color, "block:folder", false, Waypoint.Type.NORMAL);
    }

    public boolean addMemberToFolder(Waypoint folder, Waypoint member) {
        FolderData f = foldersById.get(folder.getId());
        if (f == null) return false;
        boolean added = f.addMember(member.getId());
        if (added) folderIdByWaypointId.put(member.getId(), folder.getId());
        return added;
    }

    public void removeWaypointFromFolder(String wpId) {
        String fid = folderIdByWaypointId.remove(wpId);
        if (fid != null) {
            FolderData f = foldersById.get(fid);
            if (f != null) f.removeMember(wpId);
        }
    }

    public boolean moveFolderMemberBefore(String folderId, String target, String other) {
        FolderData f = foldersById.get(folderId);
        return f != null && f.moveMemberBefore(target, other);
    }

    public boolean moveFolderMemberAfter(String folderId, String target, String other) {
        FolderData f = foldersById.get(folderId);
        return f != null && f.moveMemberAfter(target, other);
    }

    public Waypoint combineClumps(List<Waypoint> clumps) {
        List<Waypoint> allMembers = new ArrayList<>();
        String clumpId = UUID.randomUUID().toString();
        ClumpData cd = new ClumpData(clumpId);
        for (Waypoint c : clumps) {
            ClumpData src = clumpsById.get(c.getId());
            if (src != null) {
                for (var e : src.getMembersByPos().entrySet()) cd.putMember(e.getValue());
                clumpsById.remove(c.getId());
                waypoints.remove(c);
            }
        }
        clumpsById.put(clumpId, cd);
        Waypoint wp = createWaypointWithId(clumpId, cd.getMembersByPos().keySet().iterator().next(), "Combined", 0xFFAA66, "block:grass", false, Waypoint.Type.NORMAL);
        return wp;
    }

    public ClumpData.Member removeMemberFromClump(String clumpId, BlockPos pos) {
        ClumpData c = clumpsById.get(clumpId);
        if (c == null) return null;
        ClumpData.Member m = c.removeMember(pos);
        if (c.size() == 0) {
            clumpsById.remove(clumpId);
            waypoints.remove(getById(clumpId));
        }
        return m;
    }

    public boolean isClumpOpen(String id) {
        ClumpData c = clumpsById.get(id);
        return c != null && c.isOpen();
    }

    public void setClumpOpen(String id, boolean open) {
        ClumpData c = clumpsById.get(id);
        if (c != null) c.setOpen(open);
    }

    public boolean unclump(Waypoint clump) {
        ClumpData c = clumpsById.remove(clump.getId());
        if (c == null) return false;
        for (var e : c.getMembersByPos().entrySet()) {
            ClumpData.Member m = e.getValue();
            createWaypoint(m.pos, "", m.color, m.iconId, false, Waypoint.Type.NORMAL);
        }
        waypoints.remove(clump);
        return true;
    }

    public void syncClumpMembersAppearance(String clumpId, int color, String iconId) {
        ClumpData c = clumpsById.get(clumpId);
        if (c == null) return;
        c.setAllColor(color);
        c.setAllIcon(iconId);
        Waypoint wp = getById(clumpId);
        if (wp != null) { wp.setColor(color); wp.setIconId(iconId); }
    }

    public double[] clumpAverage(String clumpId) {
        ClumpData c = clumpsById.get(clumpId);
        double[] out = new double[] {0,0,0};
        if (c == null || c.size() == 0) return out;
        for (var p : c.getMembersByPos().keySet()) {
            out[0] += p.getX(); out[1] += p.getY(); out[2] += p.getZ();
        }
        int n = c.size();
        out[0] /= n; out[1] /= n; out[2] /= n;
        return out;
    }

    public boolean clumpAverageInto(String clumpId, double[] out) {
        double[] a = clumpAverage(clumpId);
        if (a == null) return false;
        System.arraycopy(a, 0, out, 0, 3);
        return true;
    }

    public void removeWaypoint(Waypoint wp) {
        if (wp == null) return;
        clumpsById.remove(wp.getId());
        removeWaypointFromGroup(wp.getId());
        removeWaypointFromFolder(wp.getId());
        waypoints.remove(wp);
    }

    public void clearAll() {
        waypoints.clear();
        clumpsById.clear();
        groupsById.clear();
        foldersById.clear();
        groupIdByWaypointId.clear();
        folderIdByWaypointId.clear();
    }

    public void clearUnlocked() {
        List<Waypoint> locked = new ArrayList<>();
        for (Waypoint w : waypoints) if (w.isLocked()) locked.add(w);
        clearAll();
        waypoints.addAll(locked);
    }

    public void clearUnlockedInTag(String tag) {
        List<Waypoint> keep = new ArrayList<>();
        for (Waypoint w : waypoints) {
            if (w.isLocked() || (w.getFolderNavigationKeybind() != null && w.getFolderNavigationKeybind().equals(tag))) keep.add(w);
        }
        clearAll();
        waypoints.addAll(keep);
    }

    public List<Waypoint> searchByName(String query) {
        List<Waypoint> out = new ArrayList<>();
        if (query == null || query.isEmpty()) return out;
        String q = query.toLowerCase();
        for (Waypoint w : waypoints) if (w.getName() != null && w.getName().toLowerCase().contains(q)) out.add(w);
        return out;
    }

    public void replaceAll(List<Waypoint> ws, Map<String, ClumpData> clumps, Map<String, GroupData> groups, Map<String, FolderData> folders) {
        waypoints.clear();
        clumpsById.clear();
        groupsById.clear();
        foldersById.clear();
        groupIdByWaypointId.clear();
        folderIdByWaypointId.clear();
        waypoints.addAll(ws);
        if (clumps != null) clumpsById.putAll(clumps);
        if (groups != null) groupsById.putAll(groups);
        if (folders != null) foldersById.putAll(folders);
        rebuildGroupFolderIndex();
    }

    public boolean isEmpty() { return waypoints.isEmpty(); }

    private void rebuildGroupFolderIndex() {
        for (var e : groupsById.entrySet()) {
            for (String id : e.getValue().getMemberIds()) groupIdByWaypointId.put(id, e.getKey());
        }
        for (var e : foldersById.entrySet()) {
            for (String id : e.getValue().getMemberIds()) folderIdByWaypointId.put(id, e.getKey());
        }
    }

    public ResolvedWaypointSettings resolveSettings(Waypoint wp, ModConfig cfg) {
        ModConfig.TypeDefaults typeDefaults = switch (wp.getType()) {
            case DEATH -> cfg.deathWaypointAppearance != null ? deathToDefaults(cfg) : cfg.singleWaypointDefaults;
            case SUPPLY_DROP -> cfg.hopliteSupplyDropAppearance != null ? hopliteToDefaults(cfg.hopliteSupplyDropAppearance) : cfg.singleWaypointDefaults;
            case AUTO_PICK -> cfg.hopliteAutoPickAppearance != null ? hopliteToDefaults(cfg.hopliteAutoPickAppearance) : cfg.singleWaypointDefaults;
            default -> {
                if (clumpsById.containsKey(wp.getId())) yield cfg.clumpDefaults;
                else if (groupsById.containsKey(wp.getId())) yield cfg.groupDefaults;
                else yield cfg.singleWaypointDefaults;
            }
        };
        return ResolvedWaypointSettings.resolve(wp, cfg, typeDefaults);
    }

    private ModConfig.TypeDefaults deathToDefaults(ModConfig cfg) {
        ModConfig.TypeDefaults d = cfg.singleWaypointDefaults;
        ModConfig.HopliteAppearance a = cfg.deathWaypointAppearance;
        if (a != null) { d = new ModConfig.TypeDefaults(); d.color = a.color; d.outlineOpacityPercent = a.outlineOpacityPercent; d.fillOpacityPercent = a.fillOpacityPercent; d.outlineThickness = a.outlineThickness; d.iconScale = a.iconScale; }
        return d;
    }

    private ModConfig.TypeDefaults hopliteToDefaults(ModConfig.HopliteAppearance a) {
        ModConfig.TypeDefaults d = new ModConfig.TypeDefaults();
        d.color = a.color; d.outlineOpacityPercent = a.outlineOpacityPercent; d.fillOpacityPercent = a.fillOpacityPercent;
        d.outlineThickness = a.outlineThickness; d.iconScale = a.iconScale;
        return d;
    }

    public boolean isHiddenIncludingGroup(Waypoint wp) {
        if (wp.isHidden()) return true;
        String gid = groupIdByWaypointId.get(wp.getId());
        if (gid != null) {
            GroupData g = groupsById.get(gid);
            if (g != null) {
                Waypoint gw = getById(gid);
                if (gw != null && gw.isHidden()) return true;
            }
        }
        return false;
    }

    public boolean isLockedIncludingContainers(Waypoint wp) {
        return wp.isLocked();
    }

    public String getClumpDisplayName(Waypoint wp) {
        ClumpData c = clumpsById.get(wp.getId());
        if (c == null) return wp.getName();
        return wp.getName().isEmpty() ? ("Clump (" + c.size() + ")") : wp.getName();
    }

    public String getClumpBaseName(Waypoint wp) { return wp.getName(); }

    public void markClumpDisplayNamesDirty() { unnamedClumpFallbackDirty = true; }

    private int indexOf(String id) {
        for (int i = 0; i < waypoints.size(); i++) if (waypoints.get(i).getId().equals(id)) return i;
        return -1;
    }

    private static String firstNonBlankName(List<Waypoint> members) {
        for (Waypoint w : members) if (w.getName() != null && !w.getName().isEmpty()) return w.getName();
        return "Clump";
    }
}
