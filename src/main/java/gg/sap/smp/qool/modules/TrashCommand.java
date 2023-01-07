package gg.sap.smp.qool.modules;

import gg.sap.smp.qool.util.Format;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class TrashCommand implements CommandExecutor {

    @Override
    public boolean onCommand(
            @NotNull final CommandSender sender,
            @NotNull final Command command,
            @NotNull final String label,
            @NotNull final String[] args
    ) {
        final Format format = new Format(sender);
        if (!(sender instanceof final Player player)) {
            format.error("this command is only intended for players.");
            return true;
        }
        player.openInventory(Bukkit.createInventory(null, 54));
        format.info("opened trash can");
        return true;
    }

}
