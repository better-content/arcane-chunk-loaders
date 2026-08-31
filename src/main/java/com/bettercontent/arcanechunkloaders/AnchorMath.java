package com.bettercontent.arcanechunkloaders;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class AnchorMath {
    private static final List<ChunkOffset> CENTERED_3X3;

    static {
        List<ChunkOffset> offsets = new ArrayList<>(9);
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) offsets.add(new ChunkOffset(x, z));
        }
        CENTERED_3X3 = Collections.unmodifiableList(offsets);
    }

    public static List<ChunkOffset> centered3x3() { return CENTERED_3X3; }

    public static double chargeFraction(int stored, int capacity) {
        if (capacity <= 0) return 0.0;
        return Math.max(0.0, Math.min(1.0, stored / (double) capacity));
    }

    public static int serviceTicksForHours(int hours) {
        return Math.multiplyExact(hours, 72_000);
    }

    public static boolean canSatisfySingleSourceRequest(int stored, int requested) {
        return requested >= 0 && stored >= requested;
    }

    public record ChunkOffset(int x, int z) {}

    private AnchorMath() {}
}
