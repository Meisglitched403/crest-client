package com.crest.client.ui;

/** A single animated float value that eases toward a target each frame. */
public final class Animated {
    private float value;
    private float target;
    private final float speed;

    public Animated(float initial, float speed) {
        this.value = initial;
        this.target = initial;
        this.speed = speed;
    }

    public Animated(float initial) {
        this(initial, 14f);
    }

    public void set(float target) {
        this.target = target;
    }

    public void setImmediate(float v) {
        this.value = v;
        this.target = v;
    }

    public void tick(float dt) {
        value += (target - value) * Anim.smooth(dt, speed);
        if (Math.abs(target - value) < 0.001f) value = target;
    }

    public float get() {
        return value;
    }

    public float getTarget() {
        return target;
    }
}
