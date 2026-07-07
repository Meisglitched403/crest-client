package com.crest.client.core;

public class NoOverlayModule implements CrestModule {
    @Override public String getId() { return "no_overlay"; }
    @Override public String getName() { return "No Overlay"; }
    @Override public String getDescription() { return "Removes water overlay"; }
    @Override public String getCategory() { return "Visual"; }
    @Override public boolean isEnabled() { return false; }
}
