package dev.lewai.prefaboptimizerextended.optimization;

import com.hypixel.hytale.server.core.prefab.selection.standard.BlockSelection;
import javax.annotation.Nonnull;

final class SelectionBoundsReader {
    private SelectionBoundsReader() {
    }

    @Nonnull
    static BlockBounds read(@Nonnull BlockSelection selection) {
        try {
            Object min = selection.getClass().getMethod("getSelectionMin").invoke(selection);
            Object max = selection.getClass().getMethod("getSelectionMax").invoke(selection);
            return BlockBounds.of(
                vectorCoordinate(min, "x"),
                vectorCoordinate(min, "y"),
                vectorCoordinate(min, "z"),
                vectorCoordinate(max, "x"),
                vectorCoordinate(max, "y"),
                vectorCoordinate(max, "z")
            );
        } catch (ReflectiveOperationException | LinkageError | ClassCastException e) {
            throw new IllegalStateException("Could not read the BuilderTools selection bounds", e);
        }
    }

    private static int vectorCoordinate(@Nonnull Object vector, @Nonnull String component) throws ReflectiveOperationException {
        try {
            return vector.getClass().getField(component).getInt(vector);
        } catch (NoSuchFieldException ignored) {
            Object value = vector.getClass().getMethod(component).invoke(vector);
            if (value instanceof Number number) {
                return number.intValue();
            }
            throw new IllegalStateException("Vector coordinate " + component + " is not numeric.");
        }
    }
}
