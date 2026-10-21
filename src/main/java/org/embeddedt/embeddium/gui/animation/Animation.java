package org.embeddedt.embeddium.gui.animation;

import net.minecraft.Util;

public final class Animation {
    private static final long RESYNC_THRESHOLD_MILLIS = 250L;

    private static final float EPSILON = 0.001f;

    private final float halfLifeMillis;

    private float value;
    private float target;
    private long lastUpdate;
    private boolean started;

    public Animation(float halfLifeMillis, float initial) {
        this.halfLifeMillis = halfLifeMillis;
        this.value = initial;
        this.target = initial;
    }

    public void setTarget(float target) {
        this.target = target;
    }

    public void snapTo(float value) {
        this.value = value;
        this.target = value;
        this.started = false;
    }

    public void restartFrom(float from) {
        this.value = from;
        this.lastUpdate = Util.getMillis();
        this.started = true;
    }

    public float get() {
        long now = Util.getMillis();
        long elapsed = now - this.lastUpdate;

        if (!this.started || elapsed > RESYNC_THRESHOLD_MILLIS) {
            this.lastUpdate = now;
            this.started = true;
            this.value = this.target;
            return this.value;
        }

        this.lastUpdate = now;

        if (elapsed > 0L && this.value != this.target) {
            this.value += (this.target - this.value) * (float) (1.0 - Math.pow(0.5, elapsed / this.halfLifeMillis));

            if (Math.abs(this.target - this.value) < EPSILON) {
                this.value = this.target;
            }
        }

        return this.value;
    }

    public float towards(boolean on) {
        this.setTarget(on ? 1.0f : 0.0f);
        return this.get();
    }

    public static int lerpColor(int from, int to, float t) {
        if (t <= 0.0f) {
            return from;
        } else if (t >= 1.0f) {
            return to;
        }

        int a = lerpChannel(from >>> 24, to >>> 24, t);
        int r = lerpChannel((from >> 16) & 0xFF, (to >> 16) & 0xFF, t);
        int g = lerpChannel((from >> 8) & 0xFF, (to >> 8) & 0xFF, t);
        int b = lerpChannel(from & 0xFF, to & 0xFF, t);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int lerpChannel(int from, int to, float t) {
        return from + Math.round((to - from) * t);
    }

    public static final float MIN_TEXT_ALPHA = 4.0f / 255.0f;

    public static boolean isTextVisible(float alpha) {
        return alpha >= MIN_TEXT_ALPHA;
    }

    public static int scaleAlpha(int color, float factor) {
        int alpha = Math.round(((color >>> 24) & 0xFF) * Math.max(0.0f, Math.min(1.0f, factor)));
        return (alpha << 24) | (color & 0x00FFFFFF);
    }

    public static float smoothstep(float t) {
        float x = Math.max(0.0f, Math.min(1.0f, t));
        return x * x * (3.0f - 2.0f * x);
    }
}
