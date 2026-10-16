public class MeshGrowthCheck {
    static final class Builder {
        final int stride;
        final boolean fixed;
        long allocatedBytes;
        int capacity;
        int count;

        Builder(int stride, int initialCapacity, boolean fixed) {
            this.stride = stride;
            this.fixed = fixed;
            this.setBufferSize(initialCapacity);
        }

        void push(int vertexCount) {
            if (this.count + vertexCount >= this.capacity) {
                this.grow(this.fixed ? vertexCount : this.stride * vertexCount);
            }

            this.count += vertexCount;

            if ((long) this.count * this.stride > this.allocatedBytes) {
                throw new AssertionError("wrote past the end of the buffer");
            }
        }

        void grow(int len) {
            int cap = Math.max(this.capacity * 2, this.capacity + len);
            this.setBufferSize(this.fixed ? cap : cap * this.stride);
        }

        void setBufferSize(int capacity) {
            this.allocatedBytes = (long) capacity * this.stride;
            this.capacity = capacity;
        }
    }

    public static void main(String[] args) {
        int initial = 128 * 1024;

        for (int stride : new int[] {20, 40}) {
            Builder fixed = new Builder(stride, initial, true);
            Builder old = new Builder(stride, initial, false);

            for (int quad = 0; quad < 40_000; quad++) {
                fixed.push(4);
                old.push(4);
            }

            long written = 40_000L * 4 * stride;

            assert fixed.allocatedBytes >= written : "the fixed buffer must hold every vertex";
            assert fixed.allocatedBytes <= 2L * initial * stride : "one overflow must at most double the buffer";
            assert old.allocatedBytes >= (long) stride * fixed.allocatedBytes : "the old growth must over-allocate by the stride";

            System.out.printf("stride %d: 40,000 quads need %.1f MiB, fixed growth allocates %.1f MiB, old growth allocated %.1f MiB%n",
                    stride, written / 1048576.0, fixed.allocatedBytes / 1048576.0, old.allocatedBytes / 1048576.0);
        }

        System.out.println("MeshGrowthCheck: all assertions passed");
    }
}
