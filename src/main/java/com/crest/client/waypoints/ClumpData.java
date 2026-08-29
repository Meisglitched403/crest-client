package com.crest.client.waypoints;

import net.minecraft.core.BlockPos;

/** A clump is a waypoint that joins several adjacent blocks into one marker. */
public final class ClumpData {
    private final String id;
    private boolean open;
    private final java.util.Map<BlockPos, ClumpData.Member> membersByPos = new java.util.LinkedHashMap<>();

    public static final class Member {
        public final BlockPos pos;
        public final int color;
        public final String iconId;
        public Member(BlockPos pos, int color, String iconId) {
            this.pos = pos; this.color = color; this.iconId = iconId;
        }
    }

    public ClumpData(String id) { this.id = id; }

    public String getId() { return id; }
    public boolean isOpen() { return open; }
    public void setOpen(boolean b) { this.open = b; }
    public java.util.Map<BlockPos, Member> getMembersByPos() { return membersByPos; }
    public boolean contains(BlockPos pos) { return membersByPos.containsKey(pos); }
    public int size() { return membersByPos.size(); }

    public void putMember(Member m) { membersByPos.put(m.pos, m); }
    public Member removeMember(BlockPos pos) { return membersByPos.remove(pos); }
    public void clear() { membersByPos.clear(); }

    public void setAllColor(int color) {
        for (Member m : membersByPos.values()) {
            // color is final; replace member
            membersByPos.put(m.pos, new Member(m.pos, color, m.iconId));
        }
    }

    public void setAllIcon(String iconId) {
        for (Member m : membersByPos.values()) {
            membersByPos.put(m.pos, new Member(m.pos, m.color, iconId));
        }
    }
}
