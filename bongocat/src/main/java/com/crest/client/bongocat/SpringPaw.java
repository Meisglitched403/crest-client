package com.crest.client.bongocat;

public class SpringPaw {
    private static final float K = 1800.0f;
    private static final float DAMPING = 38.0f;

    private float value = 0.0f;
    private float velocity = 0.0f;
    private float target = 0.0f;

    public void setPressed(boolean pressed) {
        target = pressed ? 1.0f : 0.0f;
    }

    public void update(float dt) {
        float displacement = value - target;
        float a = -K * displacement - DAMPING * velocity;
        velocity += a * dt;
        value += velocity * dt;
    }

    public float getValue() {
        return value;
    }

    public boolean isRaised() {
        return value > 0.5f;
    }
}
