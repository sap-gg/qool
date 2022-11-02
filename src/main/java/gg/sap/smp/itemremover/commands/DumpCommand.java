package gg.sap.smp.itemremover.commands;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Container;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Retention;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static gg.sap.smp.itemremover.Util.*;

public class DumpCommand implements CommandExecutor {

    enum Result {
        // The target inventory had no items to move.
        INVENTORY_EMPTY,
        // At least 1 item from the target inventory were moved
        MOVED_SOME,
        // No items were moved
        MOVED_NONE;
    }

    /**
     * This enum defines from which slots items should be moved
     */
    enum Type {
        INVENTORY(9, 35),
        HOTBAR(0, 8),
        ARMOR(100, 103);

        public final int start;
        public final int end;

        /**
         * Marks an inventory type with the starting slot-index and ending slot-index (inclusive)
         *
         * @param start Starting slot-index (inclusive)
         * @param end   Ending slot-index (inclusive)
         */
        Type(final int start, final int end) {
            this.start = start;
            this.end = end;
        }

        /**
         * Execute transfer to the given container
         *
         * @param inventory Inventory to transfer items from
         * @param container Container to send items to
         * @return true if any item was transferred, otherwise false
         */
        Result transfer(final Inventory inventory, final Container container, @Nullable final Set<Material> materials) {
            boolean hasItems = false;

            // indicates if the container at face {face} has received any items
            // used for particles
            boolean send = false;

            // distribute items to container
            for (int i = this.start; i <= this.end; ++i) {
                final ItemStack stack = inventory.getItem(i);
                if (stack == null) {
                    continue;
                }

                if (materials != null && !materials.isEmpty()) {
                    // AIR(special): only transfer matching items
                    if (materials.contains(Material.AIR)) {
                        // check if item in inventory
                        if (!container.getInventory().containsAtLeast(stack, 1)) {
                            continue;
                        }
                    } else if (!materials.contains(stack.getType())) {
                        continue;
                    }
                }

                // mark inventory to have any items
                hasItems = true;

                final Map<Integer, ItemStack> map = container.getInventory().addItem(stack);
                final ItemStack remaining = map.get(0);
                if (remaining == null || remaining.getAmount() != stack.getAmount()) {
                    // mark container to have received any items
                    send = true;
                }

                // delete item from inventory if successfully transferred to container (or set to remaining amount)
                // this works since {Inventory#addItem} returns a map with all items which couldn't be added to the inventory
                // with the key being the index of the parameter (in this case always 0)
                inventory.setItem(i, remaining);
            }

            if (!hasItems) {
                return Result.INVENTORY_EMPTY;
            }
            return send ? Result.MOVED_SOME : Result.MOVED_NONE;
        }

    }

    private void particlify(final Block block, final Particle particle) {
        block.getWorld().spawnParticle(
                particle,
                block.getLocation().clone().add(.5, .5, .5),
                6, .5, .5, .5
        );
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command is only intended for players.");
            return true;
        }

        // required syntax:
        // /dump <INVENTORY,HOTBAR,...> <UP,DOWN,NORTH,...>
        if (args.length != 2 && args.length != 3) {
            player.sendMessage(error(String.format("syntax: /dump &7{type:}&r <%s> &7{direction:}&r <%s>",
                    enumJoin(Type.values(), ","), enumJoin(BlockFace.values(), ",", 3) + "...")
            ));
            return true;
        }

        // read & parse types from args
        final Set<Type> types = new LinkedHashSet<>();
        for (final String arg : args[0].split(",")) {
            final Type type = enumGet(Type.values(), arg);
            if (type == null) {
                player.sendMessage(error("type '&7" + arg + "&r' not found."));
                player.sendMessage(light("available", enumJoin(Type.values())));
                return true;
            }
            types.add(type);
        }
        if (types.isEmpty()) {
            player.sendMessage(warn("no type given. nothing to do."));
            return true;
        }

        // read & parse orientations from args
        final Set<BlockFace> orientations = new LinkedHashSet<>();
        for (final String arg : args[1].split(",")) {
            final BlockFace face = enumGet(BlockFace.values(), arg);
            if (face == null) {
                player.sendMessage(error("block face '&7" + arg + "&r' not found."));
                player.sendMessage(light("available", enumJoin(BlockFace.values())));
                return true;
            }
            orientations.add(face);
        }
        if (orientations.isEmpty()) {
            player.sendMessage(warn("no orientation given. nothing to do."));
            return true;
        }

        final Set<Material> materials = new HashSet<>();
        if (args.length == 3) {
            for (final String arg : args[2].split(",")) {
                final Material material = enumGet(Material.values(), arg);
                if (material == null) {
                    player.sendMessage(error("material '&7" + arg + "&r' not found."));
                    return true;
                }
                materials.add(material);
            }
        }

        final Inventory inventory = player.getInventory();
        for (final BlockFace face : orientations) {
            final Block block = player.getLocation().getBlock().getRelative(face);
            if (!(block.getState() instanceof Container container)) {
                player.sendMessage(warn("block at " + face.name() + " is not a container."));
                continue;
            }
            for (final Type type : types) {
                // do transfer and show particles
                switch (type.transfer(inventory, container, materials)) {
                    case MOVED_SOME -> {
                        this.particlify(block, Particle.VILLAGER_HAPPY);
                        player.sendMessage(light("debug", "sent at least 1 item to " + face));
                    }
                    case MOVED_NONE -> {
                        this.particlify(block, Particle.VILLAGER_ANGRY);
                        player.sendMessage(light("debug", "sent no item to " + face));
                    }
                }
            }
        }
        return true;
    }

}
