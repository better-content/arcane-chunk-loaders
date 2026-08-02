package com.gerald.arcanechunkloaders;

import org.junit.jupiter.api.Test;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnchorMathTest {
    @Test
    void centeredAreaContainsExactlyNineUniqueChunks() {
        var offsets = AnchorMath.centered3x3();
        assertEquals(9, offsets.size());
        assertEquals(9, new HashSet<>(offsets).size());
        assertTrue(offsets.contains(new AnchorMath.ChunkOffset(0, 0)));
        assertTrue(offsets.stream().allMatch(offset -> Math.abs(offset.x()) <= 1 && Math.abs(offset.z()) <= 1));
    }

    @Test
    void chargeFractionIsClamped() {
        assertEquals(0.0, AnchorMath.chargeFraction(-1, 100));
        assertEquals(0.25, AnchorMath.chargeFraction(25, 100));
        assertEquals(1.0, AnchorMath.chargeFraction(125, 100));
        assertEquals(0.0, AnchorMath.chargeFraction(1, 0));
    }

    @Test
    void twoHoursAtTwentyTpsIsOneHundredFortyFourThousandTicks() {
        assertEquals(144_000, AnchorMath.serviceTicksForHours(2));
    }
}
