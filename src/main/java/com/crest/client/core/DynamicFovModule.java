package com.crest.client.core;

public class DynamicFovModule implements CrestModule {
    @Override public String getId() { return "dynamic_fov"; }
    @Override public String getName() { return "Lock FOV"; }
    @Override public String getDescription() { return "Prevents FOV changes from sprinting and speed effects"; }
    @Override public String getCategory() { return "Visual"; }
    @Override public boolean isEnabled() { return false; }
}
