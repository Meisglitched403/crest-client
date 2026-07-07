package com.crest.client.core.event;

public class KeyPressEvent implements Event {
    private final int key;
    private final int action;

    public KeyPressEvent(int key, int action) {
        this.key = key;
        this.action = action;
    }

    public int getKey() { return key; }
    public int getAction() { return action; }
}
