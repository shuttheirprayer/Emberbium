import java.util.Arrays;

/**
 * Standalone check of the per-block tint memo in BlockRenderer.getVertexColors().
 *
 * The memo is only sound because of three things the real code relies on and this check pins down:
 * a blended provider is never memoised, a differing tint index is never served from the memo, and
 * the memo is dropped between blocks -- otherwise one block's colour bleeds into the next, which is
 * the kind of thing that only shows up as a wrong tint on someone else's screen.
 *
 *   javac checks/TintMemoCheck.java -d /tmp && java -ea -cp /tmp TintMemoCheck
 */
public class TintMemoCheck {

    /** Stand-in for ColorProvider. Counts calls so we can see what the memo skipped. */
    static abstract class Provider {
        int calls;
        final void getColors(int pos, int tintIndex, int[] output) {
            this.calls++;
            this.fill(pos, tintIndex, output);
        }
        abstract void fill(int pos, int tintIndex, int[] output);
    }

    /** DefaultColorProviders.VanillaAdapter: one colour for the whole quad, from (pos, tintIndex). */
    static class VanillaAdapter extends Provider {
        @Override
        void fill(int pos, int tintIndex, int[] output) {
            Arrays.fill(output, 0x10000 * pos + tintIndex);
        }
    }

    /** BlendedColorProvider: a different colour per vertex, so it can never be memoised. */
    static class Blended extends Provider {
        @Override
        void fill(int pos, int tintIndex, int[] output) {
            for (int i = 0; i < output.length; i++) {
                output[i] = 0x10000 * pos + tintIndex + i;
            }
        }
    }

    /** The patched BlockRenderer, transcribed down to the parts the memo touches. */
    static class Renderer {
        final int[] quadColors = new int[4];

        boolean hasCachedTint;
        int cachedTintIndex;
        int cachedTintColor;

        /** renderModel(): the memo is dropped for every block. */
        void beginBlock() {
            this.hasCachedTint = false;
        }

        /** getVertexColors(), for a quad that has a colour. */
        int[] getVertexColors(int pos, Provider colorProvider, int tintIndex) {
            final int[] vertexColors = this.quadColors;

            boolean cacheable = !(colorProvider instanceof Blended);

            if (cacheable && this.hasCachedTint && tintIndex == this.cachedTintIndex) {
                Arrays.fill(vertexColors, this.cachedTintColor);
            } else {
                colorProvider.getColors(pos, tintIndex, vertexColors);

                if (cacheable) {
                    this.hasCachedTint = true;
                    this.cachedTintIndex = tintIndex;
                    this.cachedTintColor = vertexColors[0];
                }
            }

            for (int i = 0; i < vertexColors.length; i++) {
                vertexColors[i] |= 0xFF000000;
            }

            return vertexColors;
        }
    }

    /** What upstream produced, for every case: always ask the provider. */
    static int[] uncached(int pos, Provider p, int tintIndex) {
        int[] out = new int[4];
        p.fill(pos, tintIndex, out);
        for (int i = 0; i < out.length; i++) {
            out[i] |= 0xFF000000;
        }
        return out;
    }

    public static void main(String[] args) {
        // --- the win: one block, six quads, same tint -> one provider call, same colours ---
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

        // --- a different tint index within the same block must not reuse the memo ---
        vanilla.calls = 0;
        int[] tint0 = r.getVertexColors(42, vanilla, 0).clone();
        int[] tint1 = r.getVertexColors(42, vanilla, 1).clone();
        assert vanilla.calls == 1 : "tint 0 was memoised, tint 1 must be computed, calls=" + vanilla.calls;
        assert !Arrays.equals(tint0, tint1) : "different tint indices must give different colours";
        assert Arrays.equals(tint1, uncached(42, vanilla, 1)) : "tint 1 must match the uncached colour";

        // --- the regression that matters: the memo must not survive into the next block ---
        r.beginBlock();
        vanilla.calls = 0;
        int[] next = r.getVertexColors(43, vanilla, 1);
        assert vanilla.calls == 1 : "a new block must ask the provider again, calls=" + vanilla.calls;
        assert Arrays.equals(next, uncached(43, vanilla, 1)) : "a new block must get its own colour, not the last block's";

        // and with the reset removed, it would be wrong -- proving the assertion above can fail
        Renderer leaky = new Renderer();
        leaky.beginBlock();
        leaky.getVertexColors(42, vanilla, 1);
        int[] leaked = leaky.getVertexColors(43, vanilla, 1); // no beginBlock()
        assert !Arrays.equals(leaked, uncached(43, vanilla, 1)) : "without the per-block reset this must be the wrong colour";

        // --- blended providers are never memoised, and keep their per-vertex values ---
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

        System.out.println("TintMemoCheck: all assertions passed");
    }
}
