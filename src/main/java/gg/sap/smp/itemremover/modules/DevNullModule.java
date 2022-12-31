package gg.sap.smp.itemremover.modules;

import gg.sap.smp.itemremover.util.Format;
import gg.sap.smp.itemremover.util.LimitedStack;
import gg.sap.smp.itemremover.util.Util;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.block.Sign;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.BlockIterator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
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
    public static final int RECOVER_SIZE = 54;

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

    private @NotNull Set<Material> getSetCreate(final Player player) {
        Set<Material> set = this.getSet(player);
        if (set == null) {
            set = new HashSet<>();
            this.materials.put(player.getUniqueId(), set);
        }
        return set;
    }

    private @Nullable Set<Material> getSet(final Player player) {
        return this.materials.get(player.getUniqueId());
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
            @NotNull final CommandSender sender,
            @NotNull final Command command,
            @NotNull final String label,
            @NotNull final String[] args
    ) {
        final Format format = new Format(sender);
        // only players should be able to use the /dev/null feature
        if (!(sender instanceof final Player player)) {
            format.error("this command is only intended for players.");
            return true;
        }

        // /devnull
        // show help
        if (args.length == 0) {
            player.sendMessage("/devnull clear - clears all materials");
            player.sendMessage("/devnull recover - recover deleted items");
            player.sendMessage("/devnull run <inv|range|container> - execute on a target");
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

        // /devnull inventory
        // execute cleanup in inventory
        if (args[0].equalsIgnoreCase("run")) {
            this.runCommand(format, player, Arrays.copyOfRange(args, 1, args.length));
            return true;
        }

        // get or create new material set for executing player
        final Set<Material> set = this.getSetCreate(player);

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

    private long runOnContent(
            @NotNull final Player player,
            @NotNull final Inventory inventory,
            @NotNull final Consumer<ItemStack[]> setFunction
    ) {
        final ItemStack[] contents = inventory.getContents();
        long removed = 0;
        for (int i = 0; i < contents.length; i++) {
            final ItemStack stack = contents[i];
            if (stack == null) {
                continue;
            }
            if (this.destroy(player, stack, null)) {
                contents[i] = null;
                removed += stack.getAmount();
            }
        }
        setFunction.accept(contents);
        return removed;
    }

    private void runCommand(
            @NotNull final Format format,
            @NotNull final Player player,
            @NotNull final String[] args
    ) {
        // run /dev/null on range
        final String where = args[0].toUpperCase();
        if (where.startsWith("RANGE=")) {
            final String[] spl = where.split("=");
            if (spl.length != 2) {
                format.error("invalid range");
                return;
            }
            final int range;
            try {
                range = Integer.parseInt(spl[1]);
            } catch (final NumberFormatException nfex) {
                format.error("cannot parse range to an int");
                return;
            }
            long removed = 0;
            // get items in range
            for (final Item item : player.getWorld().getNearbyEntitiesByType(Item.class, player.getLocation(), range)) {
                if (this.destroy(player, item.getItemStack(), item)) {
                    removed += item.getItemStack().getAmount();
                }
            }
            format.info("&rdeleted &e" + removed + "&r items from &dthe ground: range " + range);
            return;
        }

        if (where.equals("INV")) {
            final long removed = this.runOnContent(
                    player,
                    player.getInventory(),
                    content -> player.getInventory().setContents(content)
            );
            format.info("&rdeleted &e" + removed + "&r items from &dyour inventory");
            return;
        }

        if (where.equals("CONTAINER") || where.equals("CONTAINER->")) {
            final boolean processMultiple = where.equals("CONTAINER->");
            final BlockIterator it = new BlockIterator(player, 10);
            while (it.hasNext()) {
                final Block block = it.next();
                if (!(block.getState() instanceof final Container container)) {
                    continue;
                }
                final long removed = this.runOnContent(
                        player,
                        container.getInventory(),
                        content -> container.getInventory().setContents(content)
                );
                format.info("&rdeleted &e" + removed + "&r items from &dcontainer: " + Util.simpleLocation(container));
                if (!processMultiple) {
                    return;
                }
            }
            if (!processMultiple) {
                format.error("no container in range");
            }
            return;
        }

        format.error("/devnull run <inv|range=[...]>");
    }

    public boolean destroy(
            @NotNull final Player player,
            @NotNull final ItemStack stack,
            @Nullable final Item item
    ) {
        final Set<Material> set = this.getSet(player);
        if (set == null) {
            return false;
        }
        if (!set.contains(stack.getType())) {
            return false;
        }
        // remove dropped item
        if (item != null) {
            item.remove();
        }
        // add to recover list
        final LimitedStack<ItemStack> limitedStack;
        if (this.recover.containsKey(player.getUniqueId())) {
            limitedStack = this.recover.get(player.getUniqueId());
        } else {
            limitedStack = new LimitedStack<>(DevNullModule.RECOVER_SIZE);
            this.recover.put(player.getUniqueId(), limitedStack);
        }
        limitedStack.push(stack);
        return true;
    }

    /**
     * Clear Items on Pickup
     *
     * @param event EntityPickupItemEvent
     */
    @EventHandler
    public void onPickup(final EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof final Player player)) {
            return;
        }
        final ItemStack stack = event.getItem().getItemStack();
        if (this.destroy(player, stack, event.getItem())) {
            event.setCancelled(true);
        }
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


    ////////////////////////////////////////////////////////////////////////////////////////////////

    @EventHandler
    public void onSignEdit(final SignChangeEvent event) {
        if (event.line(1) instanceof final TextComponent text) {
            if (!text.content().equalsIgnoreCase("[devnull]")) {
                return;
            }
            event.line(1, Component.text("/dev/null", NamedTextColor.GREEN));

            if (!(event.line(2) instanceof final TextComponent what) || what.content().isBlank()) {
                Format.warn(event.getPlayer(), "what to do? o.0");
                event.getBlock().breakNaturally();
                return;
            }

            event.line(2, Component.text(what.content().toUpperCase()));
            event.getBlock().getWorld().spawnParticle(Particle.VILLAGER_HAPPY, event.getBlock().getLocation(), 2);
        }
    }

    @EventHandler
    public void onInteract(final PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK
                || event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        final Block target = event.getClickedBlock();
        if (target == null) {
            return;
        }
        if (!(target.getState() instanceof final Sign sign)) {
            return;
        }
        if (!(sign.line(1) instanceof final TextComponent header)
                || !NamedTextColor.GREEN.equals(header.color())
                || !header.content().equals("/dev/null")) {
            return;
        }
        if (!(sign.line(2) instanceof final TextComponent text)) {
            return;
        }
        final Player player = event.getPlayer();
        switch (text.content().toUpperCase()) {
            // clears the /dev/null
            case "CLEAR" -> player.performCommand("devnull clear");

            // adds items to a preset
            case "PRESET" -> {
                if (!(sign.getBlockData() instanceof final WallSign wall)) {
                    return;
                }
                final Block facedBlock = target.getRelative(wall.getFacing().getOppositeFace());
                if (!(facedBlock.getState() instanceof final Container container)) {
                    return;
                }
                // add all items from container to devnull
                final Set<Material> set = this.getSetCreate(player);
                for (final ItemStack stack : container.getInventory().getContents()) {
                    if (stack == null) {
                        continue;
                    }
                    set.add(stack.getType());
                }
                player.performCommand("devnull ,");
            }

            default -> {
                Format.info(player, "&rExecuting &e/dev/null " + text.content() + "&r...");
                player.performCommand("devnull " + text.content());
            }
        }
    }

    private void checkContents(final Player player, final ItemStack[] contents) {
        final Set<Material> set = this.getSetCreate(player);

        for (final ItemStack stack : contents) {
            if (stack == null) {
                continue;
            }
            if (!stack.hasItemMeta()) {
                continue;
            }
            final ItemMeta meta = stack.getItemMeta();
            if (!(meta.displayName() instanceof TextComponent text)) {
                continue;
            }
            if (!text.content().startsWith("/dev/null:")) {
                continue;
            }
            final String materials = text.content().substring(10).strip();
            for (final String materialName : Arrays.stream(materials.split("\\s+"))
                    .flatMap((Function<String, Stream<String>>) s -> Arrays.stream(s.split(",\\s*")))
                    .map(String::toUpperCase)
                    .filter(s -> !s.isBlank())
                    .collect(Collectors.toSet()))  {
                final Material material = Material.valueOf(materialName.toUpperCase());
                if (material == null) {
                    continue;
                }
                // add material to player's /dev/null
                set.add(material);
            }
        }
    }

    @EventHandler
    public void onJoin(final PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        final PlayerInventory inventory = player.getInventory();

        this.checkContents(player, inventory.getContents());
        this.checkContents(player, inventory.getArmorContents());
        this.checkContents(player, inventory.getExtraContents());
    }

}
