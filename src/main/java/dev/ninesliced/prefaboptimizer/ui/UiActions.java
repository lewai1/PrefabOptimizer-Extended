package dev.ninesliced.prefaboptimizer.ui;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

final class UiActions {
    private UiActions() {
    }

    @Nullable
    static <T extends Enum<T>> T parse(@Nonnull Class<T> actionType, @Nullable String value) {
        if (value == null) {
            return null;
        }

        try {
            return Enum.valueOf(actionType, value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
