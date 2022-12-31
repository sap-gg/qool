package gg.sap.smp.itemremover.modules;

import gg.sap.smp.itemremover.util.Format;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class ExchangeCommand implements CommandExecutor, Listener {

    private final Map<UUID, UUID> requests = new HashMap<>();

    public class ExchangeInventoryHolder implements InventoryHolder {
        public static final Inventory DUMMY = Bukkit.createInventory(null, 9);
        private Inventory inventory = DUMMY;

        // players which view the exchange inventory
        private final Set<UUID> viewers;

        public ExchangeInventoryHolder(final UUID ... viewers) {
            this.viewers = new HashSet<>(Arrays.asList(viewers));
        }

        @NotNull
        @Override
        public Inventory getInventory() {
            return this.inventory;
        }

        public void setInventory(Inventory inventory) {
            this.inventory = inventory;
        }

        public Set<UUID> getViewers() {
            return viewers;
        }
    }

    @Override
    public boolean onCommand(
            @NotNull final CommandSender sender,
            @NotNull final Command command,
            @NotNull final String label,
            @NotNull final String[] args
    ) {
        if (!(sender instanceof Player player)) {
            Format.error(sender, "this command is only intended for players");
            return true;
        }

        // get target
        final UUID target;
        if (args.length == 0) {
            if (Bukkit.getOnlinePlayers().size() == 2) {
                // get other player
                target = Bukkit.getOnlinePlayers().stream()
                        .filter(other -> other != player)
                        .findFirst()
                        .orElseThrow()
                        .getUniqueId();
            } else {
                Format.error(sender, "/exchange <player>");
                return true;
            }
        } else {
            final Player targetPlayer = Bukkit.getPlayer(args[0]);
            if (targetPlayer == null || !targetPlayer.isOnline()) {
                Format.error(sender, "player not found");
                return true;
            }
            target = targetPlayer.getUniqueId();
        }

        // check if target is self
        if (target.equals(player.getUniqueId())) {
            Format.error(player, "no lol");
            return true;
        }

        // check if target _really_ online
        final Player targetPlayer = Bukkit.getPlayer(target);
        if (targetPlayer == null || !targetPlayer.isOnline()) {
            Format.error(sender, "target not found");
            return true;
        }

        // check if requested and execute trade
        if (target.equals(this.requests.get(player.getUniqueId()))) {
            // execute trade
            final ExchangeInventoryHolder holder = new ExchangeInventoryHolder(
                    target, player.getUniqueId()
            );
            final Inventory inventory = Bukkit.createInventory(holder, 54);
            holder.setInventory(inventory);

            player.openInventory(inventory);
            targetPlayer.openInventory(inventory);
            return true;
        }

        // notify players
        if (!player.getUniqueId().equals(this.requests.get(target))) {
            Format.info(targetPlayer, String.format("&9%s&r wants to trade. &a/exchange %s",
                    player.getName(), player.getName()));
            Format.info(player, "sent request to &d" + targetPlayer.getName());
        }
        this.requests.put(target, player.getUniqueId());

        return true;
    }

    // remove ongoing requests

    @EventHandler
    public void onQuit(final PlayerQuitEvent event) {
        requests.remove(event.getPlayer().getUniqueId());
        requests.entrySet().removeIf(e -> e.getValue().equals(event.getPlayer().getUniqueId()));
    }

    @EventHandler
    public void onInventoryClose(final InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof ExchangeInventoryHolder eih)) {
            return;
        }
        // remove player from viewers
        final HumanEntity player = event.getPlayer();
        eih.getViewers().remove(player.getUniqueId());

        // if there's more players, ignore
        if (eih.getViewers().size() > 0) {
            return;
        }

        // drop all items in inventory
        boolean added = false;
        for (final ItemStack stack : event.getInventory().getContents()) {
            if (stack == null) {
                continue;
            }
            added = true;
            // give item or drop
            player.getInventory().addItem(stack).values()
                    .forEach(item -> player.getWorld().dropItem(player.getLocation(), item));
        }

        // nag player
        if (added) {
            Format.warn(player, "there were still items in the exchange");
            Format.warn(player, "&7&othese items were added to your inventory");
        }
    }

}
