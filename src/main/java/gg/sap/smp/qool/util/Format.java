package gg.sap.smp.qool.util;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.Nullable;

public class Format {

    public enum MessageType {
        ERROR(0, "&cerror"),
        WARN(10, "&ewarn"),
        INFO(20, "&binfo"),
        VERBOSE(30, "&7verbose"),
        CUSTOM(80, "");

        final int level;
        final String prefix;

        MessageType(final int level, final String prefix) {
            this.level = level;
            this.prefix = prefix;
        }

        public static String of(final String prefix, final String message) {
            return prefix + ": &r" + message;
        }

        public String format(final String message) {
            return color(MessageType.of(this.prefix, message));
        }

        public void send(@Nullable final CommandSender sender, final String message) {
            if (sender != null) {
                sender.sendMessage(this.format(message));
            }
        }
    }

    private final CommandSender sender;
    private MessageType level;

    public Format(final CommandSender sender) {
        this.sender = sender;
        this.level = MessageType.INFO;
    }

    public void setLevel(MessageType level) {
        this.level = level;
    }

    public void send(final MessageType type, final String message) {
        if (this.level.level < type.level) {
            return;
        }
        type.send(this.sender, message);
    }

    public void error(final String message) {
        this.send(MessageType.ERROR, message);
    }

    public void warn(final String message) {
        this.send(MessageType.WARN, message);
    }

    public void info(final String message) {
        this.send(MessageType.INFO, message);
    }

    public void verbose(final String message) {
        this.send(MessageType.VERBOSE, message);
    }

    public void custom(final String prefix, final String message) {
        if (this.level.level < MessageType.CUSTOM.level) {
            return;
        }
        Format.light(this.sender, prefix, message);
    }

    ///

    public static String color(final String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public static void color(final CommandSender sender, final String message) {
        sender.sendMessage(color(message));
    }

    ///

    public static void warn(@Nullable final CommandSender sender, final String message) {
        MessageType.WARN.send(sender, message);
    }

    public static void error(@Nullable final CommandSender sender, final String message) {
        MessageType.ERROR.send(sender, message);
    }

    public static void info(@Nullable final CommandSender sender, final String message) {
        MessageType.INFO.send(sender, message);
    }

    public static void verbose(@Nullable final CommandSender sender, final String message) {
        MessageType.VERBOSE.send(sender, message);
    }

    public static void light(final CommandSender sender, final String prefix, final String message) {
        sender.sendMessage(MessageType.of("&7" + prefix, message));
    }

}
