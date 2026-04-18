package dev.lewai.prefaboptimizerextended.optimization;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Exercises the 21-bit-per-axis packing in PackedPos. These routines are the
 * foundation for every neighbor-lookup and flood-fill queue operation, so a
 * sign-extension bug here would silently corrupt every result.
 */
class PackPosTest {

    @Test
    @DisplayName("positive coordinates roundtrip")
    void positiveRoundtrip() {
        assertRoundtrip(0, 0, 0);
        assertRoundtrip(1, 2, 3);
        assertRoundtrip(100, 200, 300);
        assertRoundtrip(1024, 2048, 4096);
    }

    @Test
    @DisplayName("negative coordinates sign-extend correctly")
    void negativeRoundtrip() {
        assertRoundtrip(-1, -1, -1);
        assertRoundtrip(-7, -8, -9);
        assertRoundtrip(-1000, -2000, -3000);
    }

    @Test
    @DisplayName("coordinates at the 21-bit signed range boundaries")
    void rangeBoundaries() {
        int max = (1 << 20) - 1;   // 1_048_575
        int min = -(1 << 20);      // -1_048_576
        assertRoundtrip(max, max, max);
        assertRoundtrip(min, min, min);
        assertRoundtrip(min, 0, max);
    }

    @Test
    @DisplayName("different positions produce different packed keys")
    void distinctKeys() {
        long a = PackedPos.pack(0, 0, 0);
        long b = PackedPos.pack(1, 0, 0);
        long c = PackedPos.pack(0, 1, 0);
        long d = PackedPos.pack(0, 0, 1);
        assertEquals(4, java.util.Set.of(a, b, c, d).size(), "four distinct positions should produce four distinct keys");
    }

    private static void assertRoundtrip(int x, int y, int z) {
        long key = PackedPos.pack(x, y, z);
        assertEquals(x, PackedPos.unpackX(key), "x for (" + x + "," + y + "," + z + ")");
        assertEquals(y, PackedPos.unpackY(key), "y for (" + x + "," + y + "," + z + ")");
        assertEquals(z, PackedPos.unpackZ(key), "z for (" + x + "," + y + "," + z + ")");
    }
}
