package me.jellysquid.mods.sodium.client.util.sorting;

public class RadixSort extends AbstractSort {
    private static final int RADIX_BITS = 8;
    private static final int BUCKETS = 1 << RADIX_BITS;
    private static final int BUCKET_MASK = BUCKETS - 1;
    private static final int PASSES = Integer.SIZE / RADIX_BITS;

    public static int[] radixSortDescending(final float[] keys) {
        final int length = keys.length;
        final int[] sortable = new int[length];
        final int[] counts = new int[PASSES * BUCKETS];

        for (int index = 0; index < length; index++) {
            int bits = Float.floatToRawIntBits(keys[index]);
            int key = ~(bits ^ ((bits >> 31) | Integer.MIN_VALUE));
            sortable[index] = key;

            for (int pass = 0; pass < PASSES; pass++) {
                counts[(pass * BUCKETS) + ((key >>> (pass * RADIX_BITS)) & BUCKET_MASK)]++;
            }
        }

        int[] indices = createIndexBuffer(length);
        int[] scratch = new int[length];

        for (int pass = 0; pass < PASSES; pass++) {
            final int offset = pass * BUCKETS;
            final int shift = pass * RADIX_BITS;

            if (counts[offset + ((sortable[0] >>> shift) & BUCKET_MASK)] == length) {
                continue;
            }

            int sum = 0;

            for (int bucket = 0; bucket < BUCKETS; bucket++) {
                int count = counts[offset + bucket];
                counts[offset + bucket] = sum;
                sum += count;
            }

            for (int i = 0; i < length; i++) {
                int index = indices[i];
                scratch[counts[offset + ((sortable[index] >>> shift) & BUCKET_MASK)]++] = index;
            }

            int[] swap = indices;
            indices = scratch;
            scratch = swap;
        }

        return indices;
    }
}
