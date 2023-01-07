package gg.sap.smp.qool.util;

import org.bukkit.Location;
import org.bukkit.block.BlockState;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// fight me over that class name.
public class Util {

    public static String simpleLocation(final BlockState state) {
        return simpleLocation(state.getLocation());
    }

    public static String simpleLocation(final Location location) {
        return String.format("%d %d %d (%s)",
                location.getBlockX(), location.getBlockY(), location.getBlockZ(), location.getWorld().getName());
    }

    /**
     * Get enum value for input string or null if it doesn't exist. This compares with the <code>name()</code> method.
     *
     * @param values Enum values
     * @param search Name to search for (case-insensitive)
     * @param <E>    Enum
     * @return Enum value if found, otherwise nu;;
     */
    public static <E extends Enum<E>> E enumGet(final E[] values, final String search) {
        for (final E value : values) {
            if (value.name().equalsIgnoreCase(search)) {
                return value;
            }
        }
        return null;
    }

    /**
     * Returns a joined version of all enum values
     *
     * @param values Enum values
     * @param <E>    Enum
     * @return Enum values joined by <code>, </code>
     */
    public static <E extends Enum<E>> String enumJoin(final E[] values) {
        return enumJoin(values, 0);
    }


    /**
     * Returns a joined version of all enum values
     *
     * @param values Enum values
     * @param limit  Max. number of values to join
     * @param <E>    Enum
     * @return Enum values joined by <code>, </code>
     */
    public static <E extends Enum<E>> String enumJoin(final E[] values, final int limit) {
        return enumJoin(values, ", ", limit);
    }

    /**
     * Returns a joined version of all enum values
     *
     * @param values Enum values
     * @param join   Join <code>Enum.name()</code> with {join} separator, e. g. <code>,</code>
     * @param <E>    Enum
     * @return Enum values joined by {join}
     */
    public static <E extends Enum<E>> String enumJoin(final E[] values, final String join) {
        return enumJoin(values, join, 0);
    }

    /**
     * Returns a joined version of max {limit} enum values
     *
     * @param values Enum values
     * @param join   Join <code>Enum.name()</code> with {join} separator, e. g. <code>,</code>
     * @param limit  Max. number of values to join
     * @param <E>    Enum
     * @return Enum values joined by {join}
     */
    public static <E extends Enum<E>> String enumJoin(final E[] values, final String join, final int limit) {
        Stream<String> stream = Arrays.stream(values).map(Enum::name);
        if (limit > 0) {
            stream = stream.limit(limit);
        }
        return stream.collect(Collectors.joining(join));
    }


}
