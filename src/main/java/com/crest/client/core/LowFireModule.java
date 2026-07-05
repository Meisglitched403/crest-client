package com.crest.client.core;

public class LowFireModule implements CrestModule {
    @Override public String getId() { return "low_fire"; }
    @Override public String getName() { return "Low Fire"; }
    @Override public String getDescription() { return "Shrinks the fire overlay so you can see through it"; }
    @Override public String getCategory() { return "Visual"; }
    @Override public boolean isEnabled() { return false; }
}
