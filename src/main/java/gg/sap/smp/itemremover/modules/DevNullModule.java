package gg.sap.smp.itemremover.modules;

import gg.sap.smp.itemremover.util.Format;
import gg.sap.smp.itemremover.util.LimitedStack;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DevNullModule implements CommandExecutor, Listener {

    /**
     * Contains which items should be deleted for a specific player when picking them up
     */
    private final Map<UUID, Set<Material>> materials = new HashMap<>();

    /**
     * How many can be recovered?
     * Keep this a multiple of 9 (inventory size)
     */
    public static final int RECOVER_SIZE = 36;

    /**
     * Contains which items can be recovered for a specific player
     */
    private final Map<UUID, LimitedStack<ItemStack>> recover = new HashMap<>();

    /**
     * Checks if items should be deleted for the player with the given UUID.
     * If not, the item will be deleted from the list.
     *
     * @param uuid UUID of the player
     */
    private void cleanup(final UUID uuid) {
        this.cleanup(uuid, false);
    }

    /**
     * Checks if items should be deleted for the player with the given UUID.
     * If not, the item will be deleted from the list.
     *
     * @param uuid  UUID of the player
     * @param force Pass true to always remove the map entry for the player
     */
    private void cleanup(final UUID uuid, final boolean force) {
        if (this.materials.containsKey(uuid) && (force || this.materials.get(uuid).size() <= 0)) {
            this.materials.remove(uuid);
        }
    }

    /**
     * Checks if the list with recoverable items should be deleted for the player with the given UUID
     *
     * @param uuid UUID of the player
     */
    private void cleanupRecover(final UUID uuid) {
        this.cleanupRecover(uuid, false);
    }

    /**
     * Checks if the list with recoverable items should be deleted for the player with the given UUID
     *
     * @param uuid  UUID of the player
     * @param force Pass true to always remove the recoverable items for the player
     */
    private void cleanupRecover(final UUID uuid, final boolean force) {
        if (!this.recover.containsKey(uuid)) {
            return;
        }
        if (force || this.recover.get(uuid).size() <= 0) {
            this.recover.remove(uuid);
        }
    }

    /**
     * /dev/null command
     * <p>
     * /devnull clear - clears current items
     * /devnull +STONE -GRASS - adds stone, removes grass
     *
     * @param sender  Source of the command
     * @param command Command which was executed
     * @param label   Alias of the command which was used
     * @param args    Passed command arguments
     * @return always true, actually
     */
    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        final Format format = new Format(sender);
        // only players should be able to use the /dev/null feature
        if (!(sender instanceof Player player)) {
            format.error("this command is only intended for players.");
            return true;
        }

        // /devnull
        // show help
        if (args.length == 0) {
            player.sendMessage("/devnull clear - clears all materials");
            player.sendMessage("/devnull recover - recover deleted items");
            player.sendMessage("/devnull , - list materials");
            player.sendMessage("/devnull +COBBLESTONE,-STONE - adds cobblestone, removes stone");
            return true;
        }

        // /devnull clear
        // clears all items in devnull
        if (args.length == 1 && args[0].equalsIgnoreCase("clear")) {
            this.cleanup(player.getUniqueId(), true);
            format.info("/dev/null cleared.");
            return true;
        }

        // /devnull recover
        // recover deleted items
        if (args.length == 1 && args[0].equalsIgnoreCase("recover")) {
            final LimitedStack<ItemStack> stack = this.recover.get(player.getUniqueId());
            if (stack == null || stack.size() <= 0) {
                format.warn("no items to recover");
                return true;
            }
            final Inventory inventory = Bukkit.createInventory(null, RECOVER_SIZE);
            int i = 0;
            for (final ItemStack itemStack : stack) {
                inventory.setItem(i++, itemStack);
            }
            player.openInventory(inventory);

            // clear recoverable items
            this.cleanupRecover(player.getUniqueId(), true);

            format.info("showing &d" + (i + 1) + "&r recovered items.");
            return true;
        }

        // get or create new material set for executing player
        final Set<Material> set;
        if (this.materials.containsKey(player.getUniqueId())) {
            set = this.materials.get(player.getUniqueId());
        } else {
            set = new HashSet<>();
            this.materials.put(player.getUniqueId(), set);
        }

        // /devnull +STONE,-COBBLESTONE +GRASS -AIR
        for (final String arg : Arrays.stream(args)
                .flatMap((Function<String, Stream<String>>) s -> Arrays.stream(s.split(",\\s*")))
                .map(String::toUpperCase)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toSet())) {

            final boolean add;
            if (arg.startsWith("+")) {
                add = true;
            } else if (arg.startsWith("-")) {
                add = false;
            } else {
                format.error("'+' or '-' prefix required (+" + arg + " or -" + arg + ")");
                return true;
            }

            // get material from name
            final Material material = Material.getMaterial(arg.substring(1));
            if (material == null) {
                format.error("material '&7" + arg.substring(1) + "&r' not found.");
                return true;
            }

            // add/remove to/from list
            if (add) {
                set.add(material);
            } else {
                set.remove(material);
            }
        }

        // remove map entry if set is empty
        this.cleanup(player.getUniqueId());
        this.cleanupRecover(player.getUniqueId());

        // send player summary of all items in /dev/null
        format.info("now in /dev/null (" + set.size() + "): &a" + set.stream()
                .map(Material::name)
                .collect(Collectors.joining("&r, &a"))
        );
        return true;
    }

    /**
     * Clear Items on Pickup
     *
     * @param event EntityPickupItemEvent
     */
    @EventHandler
    public void onPickup(final EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        final Set<Material> set = this.materials.get(player.getUniqueId());
        if (set == null) {
            return;
        }
        final ItemStack stack = event.getItem().getItemStack();
        if (!set.contains(stack.getType())) {
            return;
        }
        event.setCancelled(true);
        event.getItem().remove();

        // add to recover list
        final LimitedStack<ItemStack> limitedStack;
        if (this.recover.containsKey(player.getUniqueId())) {
            limitedStack = this.recover.get(player.getUniqueId());
        } else {
            limitedStack = new LimitedStack<>(DevNullModule.RECOVER_SIZE);
            this.recover.put(player.getUniqueId(), limitedStack);
        }
        limitedStack.push(stack);
    }

    /**
     * Clear /dev/null with recoverable items on quit
     *
     * @param event PlayerQuitEvent
     */
    @EventHandler
    public void onLeave(final PlayerQuitEvent event) {
        this.cleanup(event.getPlayer().getUniqueId(), true);
        this.cleanupRecover(event.getPlayer().getUniqueId(), true);
    }

}
