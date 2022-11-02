package gg.sap.smp.itemremover.modules;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import static gg.sap.smp.itemremover.util.Util.error;
import static gg.sap.smp.itemremover.util.Util.light;

public class TrashCommand implements CommandExecutor {

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        if (!(sender instanceof Player player)) {
            error(sender, "this command is only intended for players.");
            return true;
        }
        player.openInventory(Bukkit.createInventory(null, 54));
        light(player, "ok", "opened trash can");
        return true;
    }

}
