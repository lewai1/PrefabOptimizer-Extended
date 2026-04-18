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

    private final boolean preserveTransparentBlocks;
    private final boolean strictCubeOnly;
    private final boolean preserveFluidAdjacentBlocks;
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
        @Nonnull String excludedBlocksRaw,
        @Nullable Pattern excludedBlocksPattern,
        @Nonnull List<String> excludedBlockTokens,
        @Nullable String excludedBlocksRegexError
    ) {
        this.preserveTransparentBlocks = preserveTransparentBlocks;
        this.strictCubeOnly = strictCubeOnly;
        this.preserveFluidAdjacentBlocks = preserveFluidAdjacentBlocks;
        this.excludedBlocksRaw = excludedBlocksRaw;
        this.excludedBlocksPattern = excludedBlocksPattern;
        this.excludedBlockTokens = List.copyOf(excludedBlockTokens);
        this.excludedBlocksRegexError = excludedBlocksRegexError;
    }

    public static OptimizerSettings create(
        @Nullable Boolean preserveTransparentBlocks,
        @Nullable Boolean strictCubeOnly,
        @Nullable Boolean preserveFluidAdjacentBlocks,
        @Nullable String excludedBlocksRaw
    ) {
        String raw = excludedBlocksRaw == null ? "" : excludedBlocksRaw.trim();
        ExclusionRules exclusionRules = parseExclusionRules(raw);

        return new OptimizerSettings(
            preserveTransparentBlocks == null || preserveTransparentBlocks,
            strictCubeOnly == null || strictCubeOnly,
            preserveFluidAdjacentBlocks == null || preserveFluidAdjacentBlocks,
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
