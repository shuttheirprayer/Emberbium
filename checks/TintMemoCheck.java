import java.util.Arrays;

public class TintMemoCheck {

    static abstract class Provider {
        int calls;
        final void getColors(int pos, int tintIndex, int sprite, int[] output) {
            this.calls++;
            this.fill(pos, tintIndex, sprite, output);
        }
        abstract void fill(int pos, int tintIndex, int sprite, int[] output);
    }

    static class VanillaAdapter extends Provider {
        @Override
        void fill(int pos, int tintIndex, int sprite, int[] output) {
            Arrays.fill(output, 0x10000 * pos + tintIndex);
        }
    }

    static class Blended extends Provider {
        @Override
        void fill(int pos, int tintIndex, int sprite, int[] output) {
            for (int i = 0; i < output.length; i++) {
                output[i] = 0x10000 * pos + tintIndex + i;
            }
        }
    }

    static class SpriteKeyed extends Provider {
        @Override
        void fill(int pos, int tintIndex, int sprite, int[] output) {
            Arrays.fill(output, 0x10000 * pos + 0x100 * sprite + tintIndex);
        }
    }

    static class Renderer {
        final int[] quadColors = new int[4];

        final boolean keyOnSprite;

        boolean hasCachedTint;
        int cachedTintIndex;
        int cachedTintSprite;
        int cachedTintColor;

        Renderer() {
            this(true);
        }

        Renderer(boolean keyOnSprite) {
            this.keyOnSprite = keyOnSprite;
        }

        void beginBlock() {
            this.hasCachedTint = false;
        }

        int[] getVertexColors(int pos, Provider colorProvider, int tintIndex) {
            return this.getVertexColors(pos, colorProvider, tintIndex, 0);
        }

        int[] getVertexColors(int pos, Provider colorProvider, int tintIndex, int sprite) {
            final int[] vertexColors = this.quadColors;

            boolean cacheable = !(colorProvider instanceof Blended);

            if (cacheable && this.hasCachedTint && tintIndex == this.cachedTintIndex && (!this.keyOnSprite || sprite == this.cachedTintSprite)) {
                Arrays.fill(vertexColors, this.cachedTintColor);
            } else {
                colorProvider.getColors(pos, tintIndex, sprite, vertexColors);

                if (cacheable) {
                    this.hasCachedTint = true;
                    this.cachedTintIndex = tintIndex;
                    this.cachedTintSprite = sprite;
                    this.cachedTintColor = vertexColors[0];
                }
            }

            for (int i = 0; i < vertexColors.length; i++) {
                vertexColors[i] |= 0xFF000000;
            }

            return vertexColors;
        }
    }

    static int[] uncached(int pos, Provider p, int tintIndex) {
        return uncached(pos, p, tintIndex, 0);
    }

    static int[] uncached(int pos, Provider p, int tintIndex, int sprite) {
        int[] out = new int[4];
        p.fill(pos, tintIndex, sprite, out);
        for (int i = 0; i < out.length; i++) {
            out[i] |= 0xFF000000;
        }
        return out;
    }

    public static void main(String[] args) {
        Renderer r = new Renderer();
        VanillaAdapter vanilla = new VanillaAdapter();
        int[] expected = uncached(42, vanilla, 0);
        vanilla.calls = 0;

        r.beginBlock();
        for (int face = 0; face < 6; face++) {
            int[] got = r.getVertexColors(42, vanilla, 0);
            assert Arrays.equals(got, expected) : "memoised colour must equal the uncached one, face " + face;
        }
        assert vanilla.calls == 1 : "six quads at one tint should ask the provider once, asked " + vanilla.calls;

        vanilla.calls = 0;
        int[] tint0 = r.getVertexColors(42, vanilla, 0).clone();
        int[] tint1 = r.getVertexColors(42, vanilla, 1).clone();
        assert vanilla.calls == 1 : "tint 0 was memoised, tint 1 must be computed, calls=" + vanilla.calls;
        assert !Arrays.equals(tint0, tint1) : "different tint indices must give different colours";
        assert Arrays.equals(tint1, uncached(42, vanilla, 1)) : "tint 1 must match the uncached colour";

        r.beginBlock();
        vanilla.calls = 0;
        int[] next = r.getVertexColors(43, vanilla, 1);
        assert vanilla.calls == 1 : "a new block must ask the provider again, calls=" + vanilla.calls;
        assert Arrays.equals(next, uncached(43, vanilla, 1)) : "a new block must get its own colour, not the last block's";

        Renderer leaky = new Renderer();
        leaky.beginBlock();
        leaky.getVertexColors(42, vanilla, 1);
        int[] leaked = leaky.getVertexColors(43, vanilla, 1);
        assert !Arrays.equals(leaked, uncached(43, vanilla, 1)) : "without the per-block reset this must be the wrong colour";

        Renderer rb = new Renderer();
        Blended blended = new Blended();
        rb.beginBlock();
        for (int face = 0; face < 6; face++) {
            int[] got = rb.getVertexColors(7, blended, 0);
            assert Arrays.equals(got, uncached(7, blended, 0)) : "blended colours must pass through untouched";
            assert got[0] != got[1] : "blended providers colour each vertex differently";
        }
        assert blended.calls == 6 : "a blended provider must be asked for every quad, asked " + blended.calls;
        assert !rb.hasCachedTint : "a blended provider must never populate the memo";

        Renderer rs = new Renderer();
        SpriteKeyed copycat = new SpriteKeyed();
        rs.beginBlock();
        int[] partA = rs.getVertexColors(9, copycat, 0, 1).clone();
        int[] partB = rs.getVertexColors(9, copycat, 0, 2).clone();
        int[] partBAgain = rs.getVertexColors(9, copycat, 0, 2).clone();
        assert Arrays.equals(partA, uncached(9, copycat, 0, 1)) : "the first part must get its own colour";
        assert Arrays.equals(partB, uncached(9, copycat, 0, 2)) : "a part with another sprite at the same tint must get its own colour";
        assert Arrays.equals(partBAgain, partB) : "a repeated sprite must be served from the memo with the same colour";
        assert copycat.calls == 2 : "two sprites at one tint should ask the provider twice, asked " + copycat.calls;

        Renderer tintOnly = new Renderer(false);
        tintOnly.beginBlock();
        tintOnly.getVertexColors(9, copycat, 0, 1);
        int[] stale = tintOnly.getVertexColors(9, copycat, 0, 2);
        assert !Arrays.equals(stale, uncached(9, copycat, 0, 2)) : "keyed on the tint alone, the second part must get the first part's colour";

        System.out.println("TintMemoCheck: all assertions passed");
    }
}
