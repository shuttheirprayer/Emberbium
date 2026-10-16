import java.util.BitSet;
import java.util.SplittableRandom;

public class FacingOrderCheck {
    static final int COUNT = 7;
    static final int ALL = (1 << COUNT) - 1;
    static final int POS_X = 0, POS_Y = 1, POS_Z = 2, NEG_X = 3, NEG_Y = 4, NEG_Z = 5, UNASSIGNED = 6;

    static int greaterThan(int a, int b) {
        return (b - a) >>> 31;
    }

    static int lessThan(int a, int b) {
        return (a - b) >>> 31;
    }

    static int visibleFaces(int originX, int originY, int originZ, int chunkX, int chunkY, int chunkZ) {
        int boundsMinX = chunkX << 4, boundsMaxX = boundsMinX + 16;
        int boundsMinY = chunkY << 4, boundsMaxY = boundsMinY + 16;
        int boundsMinZ = chunkZ << 4, boundsMaxZ = boundsMinZ + 16;

        int planes = 1 << UNASSIGNED;
        planes |= greaterThan(originX, boundsMinX - 3) << POS_X;
        planes |= greaterThan(originY, boundsMinY - 3) << POS_Y;
        planes |= greaterThan(originZ, boundsMinZ - 3) << POS_Z;
        planes |= lessThan(originX, boundsMaxX + 3) << NEG_X;
        planes |= lessThan(originY, boundsMaxY + 3) << NEG_Y;
        planes |= lessThan(originZ, boundsMaxZ + 3) << NEG_Z;
        return planes;
    }

    static int[] visibleFirst(int visible) {
        int[] order = new int[COUNT];
        int next = 0;

        for (int facing = 0; facing < COUNT; facing++) {
            if ((visible & (1 << facing)) != 0) {
                order[next++] = facing;
            }
        }

        for (int facing = 0; facing < COUNT; facing++) {
            if ((visible & (1 << facing)) == 0) {
                order[next++] = facing;
            }
        }

        return order;
    }

    static int[][] layout(int[] counts, int[] uploadOrder) {
        int[][] ranges = new int[COUNT][];
        int start = 0;

        for (int facing : uploadOrder) {
            if (counts[facing] == 0) {
                continue;
            }

            ranges[facing] = new int[] {start, counts[facing]};
            start += counts[facing];
        }

        return ranges;
    }

    static int packOrder(int[][] ranges) {
        int[] facings = new int[COUNT];
        int count = 0;

        for (int facing = 0; facing < COUNT; facing++) {
            int[] range = ranges[facing];

            if (range == null || range[1] == 0) {
                continue;
            }

            int position = count++;

            while (position > 0 && ranges[facings[position - 1]][0] > range[0]) {
                facings[position] = facings[position - 1];
                position--;
            }

            facings[position] = facing;
        }

        for (int facing = 0; facing < COUNT; facing++) {
            int[] range = ranges[facing];

            if (range == null || range[1] == 0) {
                facings[count++] = facing;
            }
        }

        int order = 0;

        for (int i = 0; i < COUNT; i++) {
            order |= facings[i] << (i * 3);
        }

        return order;
    }

    static int draw(int[] offsets, int[] elements, int order, int mask, BitSet drawn) {
        int commands = 0;
        int end = -1;
        int commandStart = -1;
        int commandElements = 0;

        for (int i = 0; i < COUNT; i++) {
            int facing = (order >>> (i * 3)) & 7;

            if (((mask >> facing) & 1) == 0) {
                continue;
            }

            if (offsets[facing] == end) {
                commandElements += elements[facing];
            } else {
                if (commandStart >= 0) {
                    drawn.set(commandStart, commandStart + (commandElements / 6) * 4);
                }

                commandStart = offsets[facing];
                commandElements = elements[facing];
                commands++;
            }

            end = offsets[facing] + (elements[facing] / 6) * 4;
        }

        if (commandStart >= 0) {
            drawn.set(commandStart, commandStart + (commandElements / 6) * 4);
        }

        return commands;
    }

    public static void main(String[] args) {
        SplittableRandom random = new SplittableRandom(7);
        long oldCommands = 0, newCommands = 0, movedCommands = 0;
        int sections = 200_000;

        for (int n = 0; n < sections; n++) {
            int[] counts = new int[COUNT];

            for (int facing = 0; facing < COUNT; facing++) {
                counts[facing] = random.nextInt(4) == 0 ? 0 : 4 * (1 + random.nextInt(300));
            }

            int chunkX = random.nextInt(-12, 13), chunkY = random.nextInt(-4, 5), chunkZ = random.nextInt(-12, 13);
            int camX = random.nextInt(-8, 24), camY = random.nextInt(-8, 24), camZ = random.nextInt(-8, 24);
            int buildVisible = visibleFaces(camX, camY, camZ, chunkX, chunkY, chunkZ);

            int base = random.nextInt(1 << 20);
            int[][] ranges = layout(counts, visibleFirst(buildVisible));
            int order = packOrder(ranges);

            int[] offsets = new int[COUNT];
            int[] elements = new int[COUNT];
            int sliceMask = 0;

            for (int facing = 0; facing < COUNT; facing++) {
                int[] range = ranges[facing];
                offsets[facing] = base + (range != null ? range[0] : 0);
                elements[facing] = range != null ? (range[1] >> 2) * 6 : 0;
                sliceMask |= range != null ? 1 << facing : 0;
            }

            int moved = random.nextInt(8);
            int drawVisible = moved == 0
                    ? visibleFaces(camX + random.nextInt(-40, 41), camY + random.nextInt(-40, 41), camZ + random.nextInt(-40, 41), chunkX, chunkY, chunkZ)
                    : buildVisible;
            int mask = drawVisible & sliceMask;

            BitSet drawn = new BitSet();
            int commands = draw(offsets, elements, order, mask, drawn);

            BitSet expected = new BitSet();

            for (int facing = 0; facing < COUNT; facing++) {
                if ((mask & (1 << facing)) != 0) {
                    expected.set(offsets[facing], offsets[facing] + counts[facing]);
                }
            }

            if (!drawn.equals(expected)) {
                throw new AssertionError("section " + n + " drew the wrong vertices");
            }

            int newBase = random.nextInt(1 << 20);
            int vertexOffset = newBase;
            int[] rebased = new int[COUNT];

            for (int i = 0; i < COUNT; i++) {
                int facing = (order >>> (i * 3)) & 7;
                rebased[facing] = vertexOffset;
                vertexOffset += (elements[facing] / 6) * 4;
            }

            for (int facing = 0; facing < COUNT; facing++) {
                if (counts[facing] != 0 && rebased[facing] != offsets[facing] - base + newBase) {
                    throw new AssertionError("section " + n + " lost its layout when the arena moved it");
                }
            }

            int[][] oldRanges = layout(counts, visibleFirst(ALL));
            int[] oldOffsets = new int[COUNT];

            for (int facing = 0; facing < COUNT; facing++) {
                oldOffsets[facing] = base + (oldRanges[facing] != null ? oldRanges[facing][0] : 0);
            }

            int oldCount = draw(oldOffsets, elements, packOrder(oldRanges), mask, new BitSet());

            if (moved == 0) {
                movedCommands += commands;
            } else {
                oldCommands += oldCount;
                newCommands += commands;
            }
        }

        System.out.printf("steady camera: %.3f draw commands per section before, %.3f after%n",
                oldCommands / (double) (sections * 7 / 8), newCommands / (double) (sections * 7 / 8));
        System.out.println("FacingOrderCheck: all assertions passed");
    }
}
