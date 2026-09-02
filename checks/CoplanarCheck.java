import java.util.Arrays;

/**
 * Standalone check of the TranslucentQuadAnalyzer.getSortState() allocation change.
 *
 * The plane test now reads the FloatArrayList's backing array in place instead of a copy, so it
 * has to be told the logical length: elements() is longer than size(), and clear() only resets
 * size, leaving the previous section's centers sitting past the end. Reading them would flip a
 * coplanar section to STATIC (or worse, keep centers for a section that does not need them).
 *
 *   javac checks/CoplanarCheck.java -d /tmp && java -ea -cp /tmp CoplanarCheck
 */
public class CoplanarCheck {

    /** net.minecraft.util.Mth.equal, transcribed. */
    static boolean equal(float a, float b) {
        return Math.abs(b - a) < 1.0E-5F;
    }

    static float nX, nY, nZ; // stand-in for globalNormal

    /** Upstream: the array is exactly the centers, so its length is the length. */
    static boolean allOnSamePlaneUpstream(float[] centers) {
        return allOnSamePlane(centers, centers.length);
    }

    /** The patched version, transcribed. */
    static boolean allOnSamePlane(float[] centerArray, int length) {
        float a = nX, b = nY, c = nZ;
        float d = a * centerArray[0] + b * centerArray[1] + c * centerArray[2];
        int nQuads = length / 3;
        for (int quadIdx = 1; quadIdx < nQuads; quadIdx++) {
            int centerOff = quadIdx * 3;
            float candidateD = a * centerArray[centerOff + 0] + b * centerArray[centerOff + 1] + c * centerArray[centerOff + 2];
            if (!equal(candidateD, d)) {
                return false;
            }
        }
        return true;
    }

    /** Stand-in for FloatArrayList: backing array outlives the logical size, clear() keeps it. */
    static class Centers {
        float[] elements = new float[16];
        int size;

        void add(float x, float y, float z) {
            if (size + 3 > elements.length) {
                elements = Arrays.copyOf(elements, Math.max(elements.length * 2, size + 3));
            }
            elements[size++] = x;
            elements[size++] = y;
            elements[size++] = z;
        }

        void clear() { this.size = 0; } // exactly fastutil's: stale floats stay in elements

        boolean isEmpty() { return size == 0; }

        float[] toArray() { return Arrays.copyOf(elements, size); }
    }

    static int copies; // counts toArray() calls, i.e. the allocation we are removing

    enum Level { NONE, STATIC, DYNAMIC }

    record SortState(Level level, float[] centers) {
        static final SortState NONE = new SortState(Level.NONE, null);
    }

    /** The patched getSortState(), transcribed. */
    static SortState getSortState(Centers quadCenters, boolean hasDistinctNormals) {
        if (quadCenters.isEmpty()) {
            quadCenters.clear();
            return SortState.NONE;
        } else {
            Level sortLevel;

            if (hasDistinctNormals) {
                sortLevel = Level.DYNAMIC;
            } else {
                sortLevel = allOnSamePlane(quadCenters.elements, quadCenters.size) ? Level.NONE : Level.STATIC;
            }

            SortState finalState;
            if (sortLevel == Level.NONE) {
                finalState = SortState.NONE;
            } else {
                copies++;
                finalState = new SortState(sortLevel, quadCenters.toArray());
            }
            quadCenters.clear();
            return finalState;
        }
    }

    public static void main(String[] args) {
        // A flat horizontal surface -- water, the common case. Normal is +Y, all centers y=64.
        nX = 0; nY = 1; nZ = 0;

        Centers flat = new Centers();
        flat.add(0.5f, 64.0f, 0.5f);
        flat.add(1.5f, 64.0f, 0.5f);
        flat.add(2.5f, 64.0f, 3.5f);

        // --- both versions agree when the array is exactly sized ---
        assert allOnSamePlaneUpstream(flat.toArray()) : "flat surface must read as coplanar";
        assert allOnSamePlane(flat.elements, flat.size) : "patched version must agree on the same data";

        // --- the regression this change could introduce: stale data past size() ---
        // Poison every slot past the logical size with a center on a *different* plane, which is
        // what the previous section leaves behind after clear().
        Arrays.fill(flat.elements, flat.size, flat.elements.length, 99.0f);
        assert allOnSamePlane(flat.elements, flat.size) : "stale centers past size() must not be read";
        assert !allOnSamePlaneUpstream(flat.elements) : "the poison must actually be able to flip the verdict";

        // --- a genuinely non-coplanar section still reads as such ---
        Centers stepped = new Centers();
        stepped.add(0.5f, 64.0f, 0.5f);
        stepped.add(1.5f, 65.0f, 0.5f);
        assert !allOnSamePlane(stepped.elements, stepped.size) : "two planes must require a static sort";

        // --- one quad is trivially coplanar, and must not read the slot after it ---
        Centers single = new Centers();
        single.add(0.5f, 64.0f, 0.5f);
        Arrays.fill(single.elements, single.size, single.elements.length, 99.0f);
        assert allOnSamePlane(single.elements, single.size) : "a lone quad is always on its own plane";

        // --- the point of the change: NONE no longer copies the centers ---
        copies = 0;
        Centers reused = new Centers();
        reused.add(0.5f, 64.0f, 0.5f);
        reused.add(1.5f, 64.0f, 0.5f);
        SortState none = getSortState(reused, false);
        assert none.level() == Level.NONE : "coplanar section must be NONE, got " + none.level();
        assert none.centers() == null : "NONE keeps no centers";
        assert copies == 0 : "NONE must not allocate a copy of the centers, allocated " + copies;

        // --- the states that keep the centers still get their own copy, not the backing array ---
        copies = 0;
        reused.add(0.5f, 64.0f, 0.5f);
        reused.add(1.5f, 65.0f, 0.5f);
        SortState statik = getSortState(reused, false);
        assert statik.level() == Level.STATIC : "two planes must be STATIC, got " + statik.level();
        assert copies == 1 : "STATIC must copy the centers once, allocated " + copies;
        assert statik.centers().length == 6 : "the copy must be exactly the centers, got " + statik.centers().length;
        assert statik.centers() != reused.elements : "the copy must not alias the reused backing array";

        copies = 0;
        reused.add(0.5f, 64.0f, 0.5f);
        SortState dynamic = getSortState(reused, true);
        assert dynamic.level() == Level.DYNAMIC : "distinct normals must be DYNAMIC";
        assert copies == 1 : "DYNAMIC must copy the centers once, allocated " + copies;
        assert dynamic.centers().length == 3 : "the copy must be exactly the centers";

        // --- the copy survives the list being reused for the next section ---
        float[] kept = dynamic.centers();
        float first = kept[0];
        reused.add(-7.0f, -7.0f, -7.0f);
        getSortState(reused, true);
        assert kept[0] == first : "a stored state must not see later sections' writes";

        System.out.println("CoplanarCheck: all assertions passed");
    }
}
