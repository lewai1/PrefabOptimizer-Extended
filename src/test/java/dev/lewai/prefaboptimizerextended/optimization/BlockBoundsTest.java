package dev.lewai.prefaboptimizerextended.optimization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BlockBoundsTest {

    @Test
    @DisplayName("of() normalizes the min/max ordering of its inputs")
    void normalizes() {
        BlockBounds bounds = BlockBounds.of(5, 10, 15, -5, -10, -15);
        assertEquals(-5, bounds.xMin());
        assertEquals(5, bounds.xMax());
        assertEquals(-10, bounds.yMin());
        assertEquals(10, bounds.yMax());
        assertEquals(-15, bounds.zMin());
        assertEquals(15, bounds.zMax());
    }

    @Test
    @DisplayName("contains respects the inclusive bounds")
    void containsInclusive() {
        BlockBounds bounds = BlockBounds.of(-5, 0, 10, 5, 20, 30);
        assertTrue(bounds.contains(-5, 0, 10), "min corner is inclusive");
        assertTrue(bounds.contains(5, 20, 30), "max corner is inclusive");
        assertTrue(bounds.contains(0, 10, 20), "interior");
        assertFalse(bounds.contains(-6, 0, 10), "just outside x-min");
        assertFalse(bounds.contains(5, 21, 30), "just outside y-max");
        assertFalse(bounds.contains(5, 20, 31), "just outside z-max");
    }

    @Test
    @DisplayName("width/depth report the exclusive diff (max - min)")
    void sizeHelpers() {
        BlockBounds bounds = BlockBounds.of(0, 0, 0, 4, 9, 14);
        assertEquals(4, bounds.width());
        assertEquals(14, bounds.depth());
    }

    @Test
    @DisplayName("center helpers round toward the midpoint")
    void centers() {
        BlockBounds bounds = BlockBounds.of(0, 0, 0, 10, 10, 10);
        assertEquals(5, bounds.centerX());
        assertEquals(5, bounds.centerZ());
    }
}
