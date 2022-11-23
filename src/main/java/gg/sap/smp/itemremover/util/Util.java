package gg.sap.smp.itemremover.util;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// fight me over that class name.
public class Util {

    public static class SSIRException extends Exception {
        public SSIRException(final String message) {
            super(message);
        }
    }

    public static int parseInt(final String[] arg, final int index) throws SSIRException {
        return parseInt(arg, index, null);
    }

    public static int parseInt(final String[] arg, final int index, final Integer def) throws SSIRException {
        if (arg.length <= index) {
            if (def == null) {
                throw new SSIRException("index out of bounds. " + index + " (idx) < " + arg.length + " (len)");
            }
            return def;
        }
        try {
            return Integer.parseInt(arg[index]);
        } catch (final NumberFormatException nfex) {
            throw new SSIRException("cannot parse '" + arg[index] + "' to a number");
        }
    }

    public static void max(final int what, final int max, final String reason) throws SSIRException {
        if (what > max) {
            throw new SSIRException(reason + ". max: " + max + ", given: " + what);
        }
    }

    public static String simpleLocation(final BlockState state) {
        return simpleLocation(state.getLocation());
    }

    public static String simpleLocation(final Location location) {
        return String.format("%d %d %d", location.getBlockX(), location.getBlockY(), location.getBlockZ());
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


    public static String color(final String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public static void color(final CommandSender sender, final String message) {
        sender.sendMessage(color(message));
    }

    public static String warn(final String message) {
        return color("&ewarn:&r " + message);
    }

    public static void warn(final CommandSender sender, final String message) {
        sender.sendMessage(warn(message));
    }

    public static String error(final String message) {
        return color("&cerror:&r " + message);
    }

    public static void error(final CommandSender sender, final String message) {
        sender.sendMessage(error(message));
    }

    public static String light(final String prefix, final String message) {
        return color("&7" + prefix + ":&r " + message);
    }

    public static void light(final CommandSender sender, final String prefix, final String message) {
        sender.sendMessage(light(prefix, message));
    }

}
