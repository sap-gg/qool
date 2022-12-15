package gg.sap.smp.itemremover.modules;

import gg.sap.smp.itemremover.util.Format;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class TrashCommand implements CommandExecutor {

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
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
