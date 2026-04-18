package dev.lewai.prefaboptimizerextended.optimization;

import com.hypixel.hytale.server.core.prefab.selection.standard.BlockSelection;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;

final class SelectionBoundsReader {
    private static final Method SELECTION_MIN = resolveSelectionMethod("getSelectionMin");
    private static final Method SELECTION_MAX = resolveSelectionMethod("getSelectionMax");

    // Vector types differ across Hytale builds (com.hypixel.hytale.math.vector.Vector3i vs org.joml.Vector3i).
    // Rather than hard-code the class, we cache the field/method handle keyed by the actual runtime class.
    private static final ConcurrentHashMap<Class<?>, VectorAccessor> VECTOR_ACCESSORS = new ConcurrentHashMap<>();

    private SelectionBoundsReader() {
    }

    @Nonnull
    static BlockBounds read(@Nonnull BlockSelection selection) {
        try {
            Object min = SELECTION_MIN.invoke(selection);
            Object max = SELECTION_MAX.invoke(selection);
            VectorAccessor minAccessor = accessorFor(min);
            VectorAccessor maxAccessor = accessorFor(max);
            return BlockBounds.of(
                minAccessor.x(min),
                minAccessor.y(min),
                minAccessor.z(min),
                maxAccessor.x(max),
                maxAccessor.y(max),
                maxAccessor.z(max)
            );
        } catch (ReflectiveOperationException | LinkageError | ClassCastException e) {
            throw new IllegalStateException("Could not read the BuilderTools selection bounds", e);
        }
    }

    @Nonnull
    private static Method resolveSelectionMethod(@Nonnull String name) {
        try {
            Method method = BlockSelection.class.getMethod(name);
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException | SecurityException e) {
            throw new ExceptionInInitializerError("BlockSelection." + name + " not available: " + e.getMessage());
        }
    }

    @Nonnull
    private static VectorAccessor accessorFor(@Nonnull Object vector) {
        return VECTOR_ACCESSORS.computeIfAbsent(vector.getClass(), SelectionBoundsReader::buildAccessor);
    }

    @Nonnull
    private static VectorAccessor buildAccessor(@Nonnull Class<?> type) {
        ComponentHandle x = componentHandle(type, "x");
        ComponentHandle y = componentHandle(type, "y");
        ComponentHandle z = componentHandle(type, "z");
        return new VectorAccessor(x, y, z);
    }

    @Nonnull
    private static ComponentHandle componentHandle(@Nonnull Class<?> type, @Nonnull String component) {
        try {
            Field field = type.getField(component);
            field.setAccessible(true);
            return new ComponentHandle.ViaField(field);
        } catch (NoSuchFieldException ignored) {
            try {
                Method method = type.getMethod(component);
                method.setAccessible(true);
                return new ComponentHandle.ViaMethod(method);
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException("Vector type " + type + " has no " + component + " component", e);
            }
        }
    }

    private record VectorAccessor(
        @Nonnull ComponentHandle x,
        @Nonnull ComponentHandle y,
        @Nonnull ComponentHandle z
    ) {
        int x(@Nonnull Object vector) throws ReflectiveOperationException {
            return this.x.read(vector);
        }

        int y(@Nonnull Object vector) throws ReflectiveOperationException {
            return this.y.read(vector);
        }

        int z(@Nonnull Object vector) throws ReflectiveOperationException {
            return this.z.read(vector);
        }
    }

    private sealed interface ComponentHandle {
        int read(@Nonnull Object vector) throws ReflectiveOperationException;

        record ViaField(@Nonnull Field field) implements ComponentHandle {
            @Override
            public int read(@Nonnull Object vector) throws ReflectiveOperationException {
                return this.field.getInt(vector);
            }
        }

        record ViaMethod(@Nonnull Method method) implements ComponentHandle {
            @Override
            public int read(@Nonnull Object vector) throws ReflectiveOperationException {
                Object value = this.method.invoke(vector);
                if (value instanceof Number number) {
                    return number.intValue();
                }
                throw new IllegalStateException("Vector component " + this.method.getName() + " did not return a number");
            }
        }
    }
}
