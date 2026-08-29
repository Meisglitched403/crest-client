package com.crest.client.waypoints;

/** Tracks which faces of a waypoint's marker box have neighbors, used by the block-model highlight renderer. */
public class BlockModelShape {
    private boolean fullCube;

    public boolean isFullCube() { return fullCube; }
    public void setFullCube(boolean b) { this.fullCube = b; }
}
