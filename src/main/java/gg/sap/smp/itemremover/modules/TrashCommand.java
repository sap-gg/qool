package gg.sap.smp.itemremover.modules;

import gg.sap.smp.itemremover.util.Format;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TrashCommand implements CommandExecutor {

    private final Map<UUID, ItemStack[]> recovery = new HashMap<>();

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
