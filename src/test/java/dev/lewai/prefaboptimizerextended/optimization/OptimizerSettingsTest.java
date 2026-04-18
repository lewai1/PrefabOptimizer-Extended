package dev.lewai.prefaboptimizerextended.optimization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OptimizerSettingsTest {

    @Test
    @DisplayName("null inputs resolve to the documented defaults")
    void defaults() {
        OptimizerSettings settings = OptimizerSettings.create(null, null, null, null, null, null, null, null);
        assertTrue(settings.preserveTransparentBlocks(), "preserveTransparent default should be true");
        assertTrue(settings.strictCubeOnly(), "strictCubeOnly default should be true");
        assertTrue(settings.preserveFluidAdjacentBlocks(), "preserveFluidAdjacent default should be true");
        assertFalse(settings.floodFillInterior(), "floodFillInterior default should be false");
        assertEquals(1, settings.shellThickness(), "shellThickness default should be 1");
        assertFalse(settings.skipBottomFace(), "skipBottomFace default should be false");
        assertFalse(settings.previewOnly(), "previewOnly default should be false");
        assertEquals("", settings.excludedBlocksRaw());
        assertNull(settings.excludedBlocksRegexError());
    }

    @Test
    @DisplayName("comma-separated exclusions are treated as tokens")
    void tokenListExclusion() {
        OptimizerSettings settings = OptimizerSettings.create(null, null, null, null, null, null, null, "glass,water,leaves");
        assertTrue(settings.excludes("Glass"), "token match is case-insensitive (exact)");
        assertTrue(settings.excludes("Minecraft:water"), "token match uses contains() by design");
        assertFalse(settings.excludes("Stone"));
        assertNull(settings.excludedBlocksRegexError());
    }

    @Test
    @DisplayName("semicolon and newline separators also produce token lists")
    void tokenListAlternateSeparators() {
        OptimizerSettings withSemicolons = OptimizerSettings.create(null, null, null, null, null, null, null, "glass;water;leaves");
        assertTrue(withSemicolons.excludes("glass"));

        OptimizerSettings withNewlines = OptimizerSettings.create(null, null, null, null, null, null, null, "glass\nwater");
        assertTrue(withNewlines.excludes("water"));
    }

    @Test
    @DisplayName("a single expression without list separators is a regex")
    void regexExclusionMatches() {
        OptimizerSettings settings = OptimizerSettings.create(null, null, null, null, null, null, null, "^Minecraft:(Glass|Water)$");
        assertTrue(settings.excludes("Minecraft:Glass"));
        assertTrue(settings.excludes("Minecraft:Water"));
        assertFalse(settings.excludes("Minecraft:Stone"));
        assertNull(settings.excludedBlocksRegexError());
    }

    @Test
    @DisplayName("invalid regex falls back to tokens AND records the compile error")
    void invalidRegexSurfaces() {
        OptimizerSettings settings = OptimizerSettings.create(null, null, null, null, null, null, null, "[InvalidRegex");
        assertNotNull(settings.excludedBlocksRegexError(), "regex error should be captured");
        assertFalse(settings.excludedBlocksRegexError().isBlank());
        assertTrue(settings.excludes("[InvalidRegex"));
        assertFalse(settings.excludes("Stone"));
    }

    @Test
    @DisplayName("blank block id is always excluded (defensive)")
    void blankBlockIdIsExcluded() {
        OptimizerSettings settings = OptimizerSettings.create(null, null, null, null, null, null, null, null);
        assertTrue(settings.excludes(""));
        assertTrue(settings.excludes(null));
    }

    @Test
    @DisplayName("shell thickness parses positive integers and clamps non-positives to 1")
    void shellThicknessParsing() {
        assertEquals(1, OptimizerSettings.parseShellThickness(null));
        assertEquals(1, OptimizerSettings.parseShellThickness(""));
        assertEquals(1, OptimizerSettings.parseShellThickness("   "));
        assertEquals(1, OptimizerSettings.parseShellThickness("0"));
        assertEquals(1, OptimizerSettings.parseShellThickness("-5"));
        assertEquals(1, OptimizerSettings.parseShellThickness("not a number"));
        assertEquals(3, OptimizerSettings.parseShellThickness("3"));
        assertEquals(5, OptimizerSettings.parseShellThickness("  5  "));
    }

    @Test
    @DisplayName("shell thickness is clamped to a safe upper bound")
    void shellThicknessUpperBound() {
        int clamped = OptimizerSettings.parseShellThickness("99999");
        assertTrue(clamped <= 64, "unbounded large values must be clamped");
        assertTrue(clamped >= 1);
    }

    @Test
    @DisplayName("flood-fill, preview, shell and skip-bottom flags can be toggled independently")
    void modeFlagsIndependent() {
        OptimizerSettings floodOnly = OptimizerSettings.create(null, null, null, true, null, false, false, null);
        assertTrue(floodOnly.floodFillInterior());
        assertFalse(floodOnly.previewOnly());
        assertFalse(floodOnly.skipBottomFace());

        OptimizerSettings aggressive = OptimizerSettings.create(null, null, null, true, "3", true, false, null);
        assertTrue(aggressive.floodFillInterior());
        assertEquals(3, aggressive.shellThickness());
        assertTrue(aggressive.skipBottomFace());

        OptimizerSettings previewDryRun = OptimizerSettings.create(null, null, null, false, null, false, true, null);
        assertTrue(previewDryRun.previewOnly());
        assertFalse(previewDryRun.floodFillInterior());
    }
}
