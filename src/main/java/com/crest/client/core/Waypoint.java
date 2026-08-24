package com.crest.client.core;

public class Waypoint {
    public String name;
    public String world;
    public String dimension;
    public int x, y, z;
    public int color;
    public String kind;
    public long createdAt;
    public boolean enabled;

    public Waypoint() {}

    public Waypoint(String name, String world, String dimension, int x, int y, int z, int color) {
        this.name = name;
        this.world = world;
        this.dimension = dimension;
        this.x = x;
        this.y = y;
        this.z = z;
        this.color = color;
        this.kind = "normal";
        this.createdAt = System.currentTimeMillis();
        this.enabled = true;
    }
}
