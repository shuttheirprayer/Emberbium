package org.embeddedt.embeddium.gui.animation;

import net.minecraft.Util;

/**
 * A frame-rate independent eased value, plus the colour helpers the options screen animates with.
 *
 * <p>This uses exponential smoothing rather than a start/duration tween, because almost everything
 * animated in the options screen is a state that can reverse mid-flight -- the cursor leaves a
 * button before its hover fade finished, a tick box is clicked twice quickly. Smoothing handles
 * that by construction: there is nothing to restart, the value simply chases whatever the target
 * is now.
 *
 * <p>Nothing here runs outside an open screen, and a screen only renders while it is on top.
 */
public final class Animation {
    /**
     * Beyond this much time between frames, snap instead of animating. Otherwise the first frame
     * after the screen was hidden -- alt-tabbed, or behind a prompt -- plays a stale animation.
     */
    private static final long RESYNC_THRESHOLD_MILLIS = 250L;

    private static final float EPSILON = 0.001f;

    private final float halfLifeMillis;

    private float value;
    private float target;
    private long lastUpdate;
    /** Explicit, rather than a sentinel value of lastUpdate, which a real timestamp can collide with. */
    private boolean started;

    /**
     * @param halfLifeMillis time for the value to cover half the remaining distance to its target;
     *                       roughly a fifth of the perceived duration
     */
    public Animation(float halfLifeMillis, float initial) {
        this.halfLifeMillis = halfLifeMillis;
        this.value = initial;
        this.target = initial;
    }

    public void setTarget(float target) {
        this.target = target;
    }

    /** Jumps straight to a value, cancelling any animation in progress. */
    public void snapTo(float value) {
        this.value = value;
        this.target = value;
        this.started = false;
    }

    /**
     * Displaces the value and lets it ease back to the current target, for a one-shot motion.
     * Unlike {@link #snapTo(float)} this keeps the animation running.
     */
    public void restartFrom(float from) {
        this.value = from;
        this.lastUpdate = Util.getMillis();
        this.started = true;
    }

    /** Advances by the time since the last call and returns the current value. */
    public float get() {
        long now = Util.getMillis();
        long elapsed = now - this.lastUpdate;

        // First call, or the screen was not being drawn: adopt the target without animating.
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

    /** Convenience for the common case: a boolean state faded to 0 or 1. */
    public float towards(boolean on) {
        this.setTarget(on ? 1.0f : 0.0f);
        return this.get();
    }

    /** Blends two ARGB colours, including their alpha. */
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

    /**
     * The lowest alpha that text can be drawn at. Minecraft's Font forces anything below this to
     * fully opaque:
     *
     * <pre>{@code return (color & 0xFC000000) == 0 ? color | 0xFF000000 : color;}</pre>
     *
     * so a fade that runs all the way down flashes the text solid on its last frame instead of
     * disappearing. Stop drawing at this alpha rather than fading through it. Fills have no such
     * fixup and can fade to zero normally.
     */
    public static final float MIN_TEXT_ALPHA = 4.0f / 255.0f;

    /** Whether text at this alpha can be drawn without tripping Minecraft's opaque fixup. */
    public static boolean isTextVisible(float alpha) {
        return alpha >= MIN_TEXT_ALPHA;
    }

    /**
     * Scales a colour's alpha, for cross-fading two things drawn on top of each other. For text,
     * check {@link #isTextVisible(float)} first -- see {@link #MIN_TEXT_ALPHA}.
     */
    public static int scaleAlpha(int color, float factor) {
        int alpha = Math.round(((color >>> 24) & 0xFF) * Math.max(0.0f, Math.min(1.0f, factor)));
        return (alpha << 24) | (color & 0x00FFFFFF);
    }

    /** Smoothstep, for movement that should ease in and out rather than chase a target. */
    public static float smoothstep(float t) {
        float x = Math.max(0.0f, Math.min(1.0f, t));
        return x * x * (3.0f - 2.0f * x);
    }
}
