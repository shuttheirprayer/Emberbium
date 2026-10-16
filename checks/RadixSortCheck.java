import me.jellysquid.mods.sodium.client.util.sorting.MergeSort;
import me.jellysquid.mods.sodium.client.util.sorting.RadixSort;

import java.util.Arrays;
import java.util.SplittableRandom;

public class RadixSortCheck {
    static final SplittableRandom RANDOM = new SplittableRandom(0x5EED);

    public static void main(String[] args) {
        int[] sizes = {1, 2, 3, 15, 16, 17, 255, 256, 257, 1000, 4096, 65536};
        int cases = 0;

        for (int size : sizes) {
            cases += checkAll(size);
        }

        for (int i = 0; i < 2000; i++) {
            cases += checkAll(1 + RANDOM.nextInt(5000));
        }

        checkNaNIsPermutation();

        System.out.println("RadixSortCheck passed, " + cases + " cases");
    }

    static int checkAll(int size) {
        check("camera", cameraKeys(size));
        check("uniform", uniformKeys(size));
        check("few distinct", fewDistinctKeys(size));
        check("all equal", constantKeys(size));
        check("ascending", ascendingKeys(size));
        check("descending", descendingKeys(size));
        check("extremes", extremeKeys(size));
        check("signed", signedKeys(size));
        return 8;
    }

    static void check(String name, float[] keys) {
        int[] expected = MergeSort.mergeSort(keys);
        int[] actual = RadixSort.radixSortDescending(keys);

        if (!Arrays.equals(expected, actual)) {
            throw new AssertionError(name + " differs at size " + keys.length);
        }
    }

    static float[] cameraKeys(int size) {
        float cx = (float) (RANDOM.nextDouble() * 16.0), cy = (float) (RANDOM.nextDouble() * 16.0), cz = (float) (RANDOM.nextDouble() * 16.0);
        float[] keys = new float[size];

        for (int i = 0; i < size; i++) {
            float x = RANDOM.nextInt(32) * 0.5f, y = RANDOM.nextInt(32) * 0.5f, z = RANDOM.nextInt(32) * 0.5f;
            float dx = x - cx, dy = y - cy, dz = z - cz;
            keys[i] = dx * dx + dy * dy + dz * dz;
        }

        return keys;
    }

    static float[] uniformKeys(int size) {
        float[] keys = new float[size];

        for (int i = 0; i < size; i++) {
            keys[i] = (float) (RANDOM.nextDouble() * 1.0e6);
        }

        return keys;
    }

    static float[] fewDistinctKeys(int size) {
        float[] keys = new float[size];

        for (int i = 0; i < size; i++) {
            keys[i] = RANDOM.nextInt(8);
        }

        return keys;
    }

    static float[] constantKeys(int size) {
        float[] keys = new float[size];
        Arrays.fill(keys, 42.5f);
        return keys;
    }

    static float[] ascendingKeys(int size) {
        float[] keys = new float[size];

        for (int i = 0; i < size; i++) {
            keys[i] = i * 0.25f;
        }

        return keys;
    }

    static float[] descendingKeys(int size) {
        float[] keys = new float[size];

        for (int i = 0; i < size; i++) {
            keys[i] = (size - i) * 0.25f;
        }

        return keys;
    }

    static float[] extremeKeys(int size) {
        float[] pool = {0.0f, Float.MIN_VALUE, Float.MIN_NORMAL, 1.0f, 1.0000001f, 65536.0f, Float.MAX_VALUE, Float.POSITIVE_INFINITY};
        float[] keys = new float[size];

        for (int i = 0; i < size; i++) {
            keys[i] = pool[RANDOM.nextInt(pool.length)];
        }

        return keys;
    }

    static float[] signedKeys(int size) {
        float[] keys = new float[size];

        for (int i = 0; i < size; i++) {
            keys[i] = (float) ((RANDOM.nextDouble() - 0.5) * 2.0e4);
        }

        return keys;
    }

    static void checkNaNIsPermutation() {
        float[] keys = uniformKeys(4096);

        for (int i = 0; i < keys.length; i += 7) {
            keys[i] = Float.NaN;
        }

        int[] actual = RadixSort.radixSortDescending(keys);
        boolean[] seen = new boolean[keys.length];

        for (int index : actual) {
            if (seen[index]) {
                throw new AssertionError("NaN input produced a duplicate index " + index);
            }

            seen[index] = true;
        }
    }
}
