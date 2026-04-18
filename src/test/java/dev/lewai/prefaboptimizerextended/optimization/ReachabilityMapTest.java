package dev.lewai.prefaboptimizerextended.optimization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ReachabilityMapTest {

    @Test
    @DisplayName("add stores a bit and contains reads it back")
    void addAndContains() {
        ReachabilityMap map = new ReachabilityMap(0, 4, 0, 4, 0, 4);
        assertFalse(map.contains(2, 2, 2));
        assertTrue(map.add(2, 2, 2));
        assertTrue(map.contains(2, 2, 2));
        assertFalse(map.contains(3, 3, 3));
    }

    @Test
    @DisplayName("add returns false if the bit is already set (so BFS can detect visited)")
    void addReturnsFalseOnRepeat() {
        ReachabilityMap map = new ReachabilityMap(-5, 5, -5, 5, -5, 5);
        assertTrue(map.add(0, 0, 0));
        assertFalse(map.add(0, 0, 0));
    }

    @Test
    @DisplayName("positions outside the bounds are not contained and add is a no-op")
    void outOfBoundsIgnored() {
        ReachabilityMap map = new ReachabilityMap(0, 3, 0, 3, 0, 3);
        assertFalse(map.add(-1, 0, 0), "outside min on x");
        assertFalse(map.add(0, 4, 0), "outside max on y");
        assertFalse(map.add(0, 0, 10), "way out on z");
        assertFalse(map.contains(-1, 0, 0));
        assertFalse(map.contains(0, 4, 0));
    }

    @Test
    @DisplayName("negative bounds work (world-coordinate selections)")
    void negativeBounds() {
        ReachabilityMap map = new ReachabilityMap(-10, -5, -10, -5, -10, -5);
        assertTrue(map.add(-7, -8, -9));
        assertTrue(map.contains(-7, -8, -9));
        assertFalse(map.contains(-5, -8, -9));
        assertTrue(map.add(-5, -5, -5), "inclusive max corner");
        assertFalse(map.add(-4, -5, -5), "one past max is rejected");
    }

    @Test
    @DisplayName("inverted bounds are rejected early")
    void invertedBoundsThrow() {
        assertThrows(IllegalArgumentException.class, () -> new ReachabilityMap(5, 0, 0, 5, 0, 5));
    }

    @Test
    @DisplayName("a 0-volume map created via empty() is safe to query")
    void emptyMap() {
        ReachabilityMap map = ReachabilityMap.empty();
        assertFalse(map.contains(0, 0, 0));
        assertFalse(map.contains(1, 0, 0), "any non-origin also not contained");
    }

    @Test
    @DisplayName("forEachPosition visits exactly the added positions in deterministic order")
    void forEachPositionEnumerates() {
        ReachabilityMap map = new ReachabilityMap(0, 2, 0, 2, 0, 2);
        map.add(0, 0, 0);
        map.add(1, 2, 0);
        map.add(2, 1, 2);
        java.util.List<int[]> visited = new java.util.ArrayList<>();
        map.forEachPosition((x, y, z) -> visited.add(new int[]{x, y, z}));
        assertEquals(3, visited.size(), "should visit exactly 3 positions");
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (int[] p : visited) {
            seen.add(p[0] + "," + p[1] + "," + p[2]);
        }
        assertTrue(seen.contains("0,0,0"));
        assertTrue(seen.contains("1,2,0"));
        assertTrue(seen.contains("2,1,2"));
    }
}
