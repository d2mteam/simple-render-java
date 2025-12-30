package com.simplerender.app;

public final class Time {
    private long lastTimeNanos;
    private float deltaSeconds;

    public Time() {
        this.lastTimeNanos = System.nanoTime();
        this.deltaSeconds = 0.0f;
    }

    public void update() {
        long now = System.nanoTime();
        long elapsed = now - lastTimeNanos;
        lastTimeNanos = now;
        deltaSeconds = elapsed / 1_000_000_000.0f;
    }

    public float deltaSeconds() {
        return deltaSeconds;
    }
}
