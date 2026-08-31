package com.bettercontent.arcanechunkloaders;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AnchorSourcePolicyTest {
    @Test
    void oneFullSourceAnchorCanPayImpossibleMatter() {
        assertTrue(AnchorMath.canSatisfySingleSourceRequest(144_000, 100_000));
        assertFalse(AnchorMath.canSatisfySingleSourceRequest(99_999, 100_000));
    }
}
