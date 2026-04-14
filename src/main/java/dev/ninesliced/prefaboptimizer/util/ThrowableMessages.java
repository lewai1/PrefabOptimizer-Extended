package dev.ninesliced.prefaboptimizer.util;

import javax.annotation.Nonnull;

public final class ThrowableMessages {
    private ThrowableMessages() {
    }

    @Nonnull
    public static Throwable rootCause(@Nonnull Throwable throwable) {
        Throwable result = throwable;
        while (result.getCause() != null && result.getCause() != result) {
            result = result.getCause();
        }
        return result;
    }

    @Nonnull
    public static String readableMessage(@Nonnull Throwable throwable) {
        String message = throwable.getMessage();
        return message == null || message.isBlank() ? throwable.getClass().getSimpleName() : message;
    }
}
