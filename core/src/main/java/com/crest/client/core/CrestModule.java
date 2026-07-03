package com.crest.client.core;

public interface CrestModule {
    String getId();
    String getName();
    default void onInitialize() {}
    default void onEnable() {}
    default void onDisable() {}
}
