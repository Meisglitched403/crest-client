package com.crest.client.waypoints;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** An ordered named collection of member IDs (waypoints or other groups). */
public final class FolderData {
    private final String id;
    private final List<String> memberIds = new ArrayList<>();
    private final List<String> memberIdsView = Collections.unmodifiableList(memberIds);

    public FolderData(String id) { this.id = id; }

    public String getId() { return id; }
    public List<String> getMemberIds() { return memberIdsView; }

    public boolean setMemberOrder(List<String> order) {
        if (!memberIds.containsAll(order) || order.size() != memberIds.size()) return false;
        memberIds.clear();
        memberIds.addAll(order);
        return true;
    }

    public boolean addMember(String id) {
        if (memberIds.contains(id)) return false;
        return memberIds.add(id);
    }

    public boolean removeMember(String id) { return memberIds.remove(id); }

    public boolean moveMemberBefore(String target, String other) {
        int ti = memberIds.indexOf(target);
        int oi = memberIds.indexOf(other);
        if (ti < 0 || oi < 0 || ti == oi) return false;
        memberIds.remove(ti);
        int newOi = memberIds.indexOf(other);
        memberIds.add(newOi, target);
        return true;
    }

    public boolean moveMemberAfter(String target, String other) {
        int ti = memberIds.indexOf(target);
        int oi = memberIds.indexOf(other);
        if (ti < 0 || oi < 0 || ti == oi) return false;
        memberIds.remove(ti);
        int newOi = memberIds.indexOf(other);
        memberIds.add(newOi + 1, target);
        return true;
    }

    public int size() { return memberIds.size(); }
}
