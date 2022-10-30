package gg.sap.smp.itemremover;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DevNull implements CommandExecutor, Listener {

    private static final Set<Material> IGNORED_ITEMS = new HashSet<>(List.of(
            Material.AIR
    ));

    /**
     * Contains which items should be deleted for a player when picking them up
     */
    private final Map<UUID, Set<Material>> materials = new HashMap<>();

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
        if (!this.materials.containsKey(uuid)) {
            return;
        }
        if (force || this.materials.get(uuid).size() <= 0) {
            this.materials.remove(uuid);
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
        // only players should be able to use the /dev/null feature
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command is intended for players.");
            return true;
        }

        // /devnull
        // show help
        if (args.length == 0) {
            player.sendMessage("/devnull clear - clears all materials");
            player.sendMessage("/devnull , - list materials");
            player.sendMessage("/devnull +COBBLESTONE,-STONE - adds cobblestone, removes stone");
            return true;
        }

        // /devnull clear
        // clears all items in devnull
        if (args.length == 1 && args[0].equalsIgnoreCase("clear")) {
            this.cleanup(player.getUniqueId(), true);
            player.sendMessage("ok: /dev/null cleared.");
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
                player.sendMessage("error: '+' or '-' prefix required (+" + arg + " or -" + arg + ")");
                return true;
            }

            // get material from name
            final Material material = Material.getMaterial(arg.substring(1));
            if (material == null) {
                player.sendMessage("error: Material " + arg.substring(1) + " not found.");
                return true;
            }

            if (IGNORED_ITEMS.contains(material)) {
                player.sendMessage("warn: Material " + arg.substring(1) + " is ignored.");
                continue;
            }

            // ignore AIR

            // add/remove to/from list
            if (add) {
                set.add(material);
            } else {
                set.remove(material);
            }
        }

        // remove map entry if set is empty
        this.cleanup(player.getUniqueId());

        // send player summary of all items in /dev/null
        player.sendMessage("Now in /dev/null (" + set.size() + "): " + set.stream()
                .map(Material::name)
                .collect(Collectors.joining(", "))
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
        if (set.contains(event.getItem().getItemStack().getType())) {
            event.setCancelled(true);
            event.getItem().remove();
        }
    }

    /**
     * Make sure a player doesn't forget about any active /dev/null things
     *
     * @param event PlayerChangedWorldEvent
     */
    @EventHandler
    public void onWorldChange(final PlayerChangedWorldEvent event) {
        if (this.materials.containsKey(event.getPlayer().getUniqueId())) {
            this.cleanup(event.getPlayer().getUniqueId(), true);
            event.getPlayer().sendMessage("Your /dev/null has been cleared because you changed the world.");
        }
    }

    /**
     * Clear /dev/null on quit
     *
     * @param event PlayerQuitEvent
     */
    @EventHandler
    public void onLeave(final PlayerQuitEvent event) {
        this.cleanup(event.getPlayer().getUniqueId(), true);
    }

}
