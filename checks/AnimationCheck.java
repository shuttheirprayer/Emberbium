import java.util.Arrays;

public class AnimationCheck {

    static class Anim {
        private static final long RESYNC_THRESHOLD_MILLIS = 250L;
        private static final float EPSILON = 0.001f;

        private final float halfLifeMillis;
        private float value;
        private float target;
        private long lastUpdate;
        private boolean started;

        Anim(float halfLifeMillis, float initial) {
            this.halfLifeMillis = halfLifeMillis;
            this.value = initial;
            this.target = initial;
        }

        void setTarget(float target) { this.target = target; }

        void snapTo(float value) { this.value = value; this.target = value; this.started = false; }

        void restartFrom(float from, long now) { this.value = from; this.lastUpdate = now; this.started = true; }

        float get(long now) {
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
    }

    static int lerpColor(int from, int to, float t) {
        if (t <= 0.0f) return from;
        if (t >= 1.0f) return to;
        int a = lerpChannel(from >>> 24, to >>> 24, t);
        int r = lerpChannel((from >> 16) & 0xFF, (to >> 16) & 0xFF, t);
        int g = lerpChannel((from >> 8) & 0xFF, (to >> 8) & 0xFF, t);
        int b = lerpChannel(from & 0xFF, to & 0xFF, t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    static int lerpChannel(int from, int to, float t) {
        return from + Math.round((to - from) * t);
    }

    static int scaleAlpha(int color, float factor) {
        int alpha = Math.round(((color >>> 24) & 0xFF) * Math.max(0.0f, Math.min(1.0f, factor)));
        return (alpha << 24) | (color & 0x00FFFFFF);
    }

    static final float MIN_TEXT_ALPHA = 4.0f / 255.0f;

    static boolean isTextVisible(float alpha) {
        return alpha >= MIN_TEXT_ALPHA;
    }

    static float smoothstep(float t) {
        float x = Math.max(0.0f, Math.min(1.0f, t));
        return x * x * (3.0f - 2.0f * x);
    }

    public static void main(String[] args) {
        Anim a = new Anim(50f, 0f);
        a.setTarget(1f);
        assert a.get(1000) == 1f : "the first read must adopt the target, got " + a.get(1000);

        Anim b = new Anim(50f, 0f);
        b.get(1000);
        b.setTarget(1f);
        float afterOne = b.get(1050);
        assert Math.abs(afterOne - 0.5f) < 0.01f : "one half-life should cover half the gap, got " + afterOne;
        float afterTwo = b.get(1100);
        assert Math.abs(afterTwo - 0.75f) < 0.01f : "two half-lives should cover three quarters, got " + afterTwo;

        Anim coarse = new Anim(50f, 0f);
        coarse.get(0);
        coarse.setTarget(1f);
        float oneStep = coarse.get(120);

        Anim fine = new Anim(50f, 0f);
        fine.get(0);
        fine.setTarget(1f);
        for (long t = 8; t <= 120; t += 8) {
            fine.get(t);
        }
        float manySteps = fine.get(120);

        assert Math.abs(oneStep - manySteps) < 0.02f
                : "frame rate must not change the result: 1 step gave " + oneStep + ", 15 steps gave " + manySteps;

        Anim hidden = new Anim(50f, 0f);
        hidden.get(0);
        hidden.setTarget(1f);
        assert hidden.get(5000) == 1f : "a gap longer than the resync threshold must snap";

        Anim rev = new Anim(50f, 0f);
        rev.get(0);
        rev.setTarget(1f);
        float mid = rev.get(30);
        assert mid > 0f && mid < 1f : "should be mid-flight, got " + mid;
        rev.setTarget(0f);
        float back = rev.get(60);
        assert back < mid : "after reversing, the value must head back down: " + mid + " -> " + back;
        assert back > 0f : "and must not jump straight to the target";

        Anim slide = new Anim(50f, 0f);
        slide.setTarget(0f);
        slide.restartFrom(10f, 1000);
        float sliding = slide.get(1025);
        assert sliding < 10f && sliding > 0f : "restartFrom must ease back toward the target, got " + sliding;

        Anim snapped = new Anim(50f, 0f);
        snapped.snapTo(10f);
        snapped.setTarget(0f);
        assert snapped.get(1000) == 0f : "snapTo cancels the animation, so the next read adopts the target";

        assert lerpColor(0xFF000000, 0xFFFFFFFF, 0f) == 0xFF000000 : "t=0 is the start colour";
        assert lerpColor(0xFF000000, 0xFFFFFFFF, 1f) == 0xFFFFFFFF : "t=1 is the end colour";
        assert lerpColor(0xFF000000, 0xFFFFFFFF, 0.5f) == 0xFF808080 : "midpoint should be mid grey, got "
                + Integer.toHexString(lerpColor(0xFF000000, 0xFFFFFFFF, 0.5f));
        assert (lerpColor(0x90000000, 0xE0202020, 0.5f) >>> 24) == 0xB8 : "alpha must interpolate, got "
                + Integer.toHexString(lerpColor(0x90000000, 0xE0202020, 0.5f) >>> 24);

        assert scaleAlpha(0xFFFFFFFF, 0.5f) == 0x80FFFFFF : "scaleAlpha halves alpha, keeps rgb, got "
                + Integer.toHexString(scaleAlpha(0xFFFFFFFF, 0.5f));
        assert scaleAlpha(0xFFFFFFFF, 0f) == 0x00FFFFFF : "zero alpha keeps the colour bits";
        assert scaleAlpha(0xFF123456, 2f) == 0xFF123456 : "factor above 1 is clamped, not wrapped";
        assert scaleAlpha(0xFF123456, -1f) == 0x00123456 : "negative factor is clamped to zero";

        assert !isTextVisible(0f) : "a fully faded value must not be drawn";
        assert !isTextVisible(3f / 255f) : "alpha 3 is inside the opaque-fixup range";
        assert isTextVisible(4f / 255f) : "alpha 4 is the first safe value";
        assert isTextVisible(1f) : "opaque text is obviously drawable";

        for (int alpha = 0; alpha <= 255; alpha++) {
            int color = scaleAlpha(0xFFFFFFFF, alpha / 255f);
            boolean forcedOpaque = (color & 0xFC000000) == 0;
            assert !(forcedOpaque && isTextVisible(alpha / 255f))
                    : "alpha " + alpha + " would be forced opaque by Font but is treated as drawable";
        }

        assert smoothstep(0f) == 0f && smoothstep(1f) == 1f : "smoothstep must hit its endpoints";
        assert smoothstep(-5f) == 0f && smoothstep(5f) == 1f : "smoothstep must clamp";
        float prev = -1f;
        for (int i = 0; i <= 10; i++) {
            float v = smoothstep(i / 10f);
            assert v >= prev : "smoothstep must be monotonic";
            prev = v;
        }

        System.out.println("AnimationCheck: all assertions passed");
    }
}
