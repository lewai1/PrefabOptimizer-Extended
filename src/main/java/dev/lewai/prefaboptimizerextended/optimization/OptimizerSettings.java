package dev.lewai.prefaboptimizerextended.optimization;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class OptimizerSettings {
    private static final Pattern TOKEN_SEPARATOR = Pattern.compile("[,;\\n]");
    private static final int MAX_SHELL_THICKNESS = 64;

    private final boolean preserveTransparentBlocks;
    private final boolean strictCubeOnly;
    private final boolean preserveFluidAdjacentBlocks;
    private final boolean floodFillInterior;
    private final int shellThickness;
    private final boolean skipBottomFace;
    private final boolean previewOnly;
    private final String excludedBlocksRaw;
    @Nullable
    private final Pattern excludedBlocksPattern;
    private final List<String> excludedBlockTokens;
    @Nullable
    private final String excludedBlocksRegexError;

    private OptimizerSettings(
        boolean preserveTransparentBlocks,
        boolean strictCubeOnly,
        boolean preserveFluidAdjacentBlocks,
        boolean floodFillInterior,
        int shellThickness,
        boolean skipBottomFace,
        boolean previewOnly,
        @Nonnull String excludedBlocksRaw,
        @Nullable Pattern excludedBlocksPattern,
        @Nonnull List<String> excludedBlockTokens,
        @Nullable String excludedBlocksRegexError
    ) {
        this.preserveTransparentBlocks = preserveTransparentBlocks;
        this.strictCubeOnly = strictCubeOnly;
        this.preserveFluidAdjacentBlocks = preserveFluidAdjacentBlocks;
        this.floodFillInterior = floodFillInterior;
        this.shellThickness = shellThickness;
        this.skipBottomFace = skipBottomFace;
        this.previewOnly = previewOnly;
        this.excludedBlocksRaw = excludedBlocksRaw;
        this.excludedBlocksPattern = excludedBlocksPattern;
        this.excludedBlockTokens = List.copyOf(excludedBlockTokens);
        this.excludedBlocksRegexError = excludedBlocksRegexError;
    }

    public static OptimizerSettings create(
        @Nullable Boolean preserveTransparentBlocks,
        @Nullable Boolean strictCubeOnly,
        @Nullable Boolean preserveFluidAdjacentBlocks,
        @Nullable Boolean floodFillInterior,
        @Nullable String shellThicknessRaw,
        @Nullable Boolean skipBottomFace,
        @Nullable Boolean previewOnly,
        @Nullable String excludedBlocksRaw
    ) {
        String raw = excludedBlocksRaw == null ? "" : excludedBlocksRaw.trim();
        ExclusionRules exclusionRules = parseExclusionRules(raw);

        return new OptimizerSettings(
            preserveTransparentBlocks == null || preserveTransparentBlocks,
            strictCubeOnly == null || strictCubeOnly,
            preserveFluidAdjacentBlocks == null || preserveFluidAdjacentBlocks,
            floodFillInterior != null && floodFillInterior,
            parseShellThickness(shellThicknessRaw),
            skipBottomFace != null && skipBottomFace,
            previewOnly != null && previewOnly,
            raw,
            exclusionRules.pattern(),
            exclusionRules.tokens(),
            exclusionRules.regexError()
        );
    }

    public boolean preserveTransparentBlocks() {
        return this.preserveTransparentBlocks;
    }

    public boolean strictCubeOnly() {
        return this.strictCubeOnly;
    }

    public boolean preserveFluidAdjacentBlocks() {
        return this.preserveFluidAdjacentBlocks;
    }

    public boolean floodFillInterior() {
        return this.floodFillInterior;
    }

    public int shellThickness() {
        return this.shellThickness;
    }

    public boolean skipBottomFace() {
        return this.skipBottomFace;
    }

    public boolean previewOnly() {
        return this.previewOnly;
    }

    @Nonnull
    public String excludedBlocksRaw() {
        return this.excludedBlocksRaw;
    }

    @Nullable
    public String excludedBlocksRegexError() {
        return this.excludedBlocksRegexError;
    }

    public boolean excludes(@Nullable String blockId) {
        if (blockId == null || blockId.isBlank()) {
            return true;
        }
        if (this.excludedBlocksPattern != null && this.excludedBlocksPattern.matcher(blockId).find()) {
            return true;
        }
        if (this.excludedBlockTokens.isEmpty()) {
            return false;
        }
        String lower = blockId.toLowerCase(Locale.ROOT);
        for (String token : this.excludedBlockTokens) {
            if (lower.equals(token) || lower.contains(token)) {
                return true;
            }
        }
        return false;
    }

    static int parseShellThickness(@Nullable String raw) {
        if (raw == null || raw.isBlank()) {
            return 1;
        }
        try {
            int value = Integer.parseInt(raw.trim());
            if (value < 1) {
                return 1;
            }
            return Math.min(value, MAX_SHELL_THICKNESS);
        } catch (NumberFormatException ignored) {
            return 1;
        }
    }

    @Nonnull
    private static ExclusionRules parseExclusionRules(@Nonnull String raw) {
        if (raw.isBlank()) {
            return new ExclusionRules(null, List.of(), null);
        }

        if (TOKEN_SEPARATOR.matcher(raw).find()) {
            return new ExclusionRules(null, parseTokens(raw), null);
        }

        try {
            return new ExclusionRules(Pattern.compile(raw), List.of(), null);
        } catch (PatternSyntaxException e) {
            return new ExclusionRules(null, parseTokens(raw), e.getDescription());
        }
    }

    @Nonnull
    private static List<String> parseTokens(@Nonnull String raw) {
        List<String> tokens = new ArrayList<>();
        for (String token : TOKEN_SEPARATOR.split(raw)) {
            String trimmed = token.trim().toLowerCase(Locale.ROOT);
            if (!trimmed.isBlank()) {
                tokens.add(trimmed);
            }
        }
        return tokens;
    }

    private record ExclusionRules(@Nullable Pattern pattern, @Nonnull List<String> tokens, @Nullable String regexError) {
    }
}
