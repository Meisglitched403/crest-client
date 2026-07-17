package com.crest.client.core;

public class Waypoint {
    private String name;
    private double x;
    private double y;
    private double z;
    private String dimension;
    private int color;
    private boolean enabled;

    // ponytail: cache the rendered label string + measured width so per-frame
    // render doesn't reallocate a String + re-measure font width every frame.
    private String cachedLabelKey;
    private String cachedLabelText;
    private int cachedLabelWidth;

    public Waypoint() {}

    public Waypoint(String name, double x, double y, double z, String dimension, int color) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.z = z;
        this.dimension = dimension;
        this.color = color;
        this.enabled = true;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public String getDimension() { return dimension; }
    public int getColor() { return color; }
    public void setColor(int color) { this.color = color; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getCachedLabel() { return cachedLabelKey; }
    public String getCachedLabelText() { return cachedLabelText; }
    public int getCachedLabelWidth() { return cachedLabelWidth; }
    public void setCachedLabel(String key, String text, int width) {
        this.cachedLabelKey = key;
        this.cachedLabelText = text;
        this.cachedLabelWidth = width;
    }
}
