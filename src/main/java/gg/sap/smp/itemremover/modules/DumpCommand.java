package gg.sap.smp.itemremover.modules;

import gg.sap.smp.itemremover.util.Util;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static gg.sap.smp.itemremover.util.Util.*;

public class DumpCommand implements CommandExecutor {

    /**
     * Max range to check for containers
     */
    public static final int MAX_ADJ_CHUNK_COUNT = 3;

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
        HOTBAR(0, 8);

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

    private boolean fillContainers(
            final Collection<Container> containers,
            final Player player,
            final String[] args
    ) throws SSIRException {
        // args: [12 <- xz range, 0 <- y range]
        final int xzrange = Util.parseInt(args, 0);
        Util.light(player, "verbose", "xzrange: " + xzrange);

        // calculate amount of adjacent chunks needed to scan to fit args[0]
        final int adjacentChunks = Math.max(1, xzrange >> 4);
        Util.light(player, "verbose", "adjacent chunks: " + adjacentChunks);
        Util.max(adjacentChunks, MAX_ADJ_CHUNK_COUNT, "too many chunks to scan");

        final int yrange = Util.parseInt(args, 1, 0);
        Util.light(player, "verbose", "yrange: " + yrange);
        Util.max(yrange, 255, "y-range cannot be greater than 255");

        final int xzrange2 = xzrange * xzrange;

        final Location location = player.getLocation();
        for (int xOffset = -adjacentChunks; xOffset <= adjacentChunks; xOffset++) {
            for (int zOffset = -adjacentChunks; zOffset <= adjacentChunks; zOffset++) {
                final int x = location.getBlockX() + xOffset * 16;
                final int z = location.getBlockZ() + zOffset * 16;

                Util.light(player, "verbose", "xO:" + xOffset + ",zO:" + zOffset + " :: " + x + " " + z);

                // find every tile entity in chunk
                final Chunk chunk = player.getWorld().getChunkAt(x, z);
                Util.light(player, "verbose", "chunk: " + chunk);
                for (final BlockState state : chunk.getTileEntities(false)) {
                    Util.light(player, "verbose", "&dfound: " + state);
                    if (!(state instanceof Chest chest)) {
                        continue;
                    }
                    // check xz-range
                    if (state.getLocation().distanceSquared(location) > xzrange2) {
                        System.out.println("Skipped " + state.getLocation() + " because of xz-range");
                        continue;
                    }
                    // check y-range
                    if (Math.abs(state.getLocation().getBlockY() - location.getBlockY()) > yrange) {
                        System.out.println("Skipped " + state.getLocation() + " because of y-range");
                        continue;
                    }
                    containers.add(chest);
                }
            }
        }

        Util.light(player, "verbose", "added " + containers.size() + " states.");
        return true;
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

        final Set<Container> containers = new HashSet<>();

        // read & parse orientations from args
        for (final String arg : args[1].split(",")) {
            // method 1: range of containers
            // /dump HOTBAR ALL=32;14 ...
            if (arg.toUpperCase().startsWith("ALL=")) {
                final String[] spl = arg.substring(4).trim().split(";");
                if (spl[0].isBlank()) {
                    error(player, "range " + arg + " invalid.");
                    return true;
                }
                try {
                    if (!this.fillContainers(containers, player, spl)) {
                        return true;
                    }
                } catch (SSIRException e) {
                    error(player, e.getMessage());
                    return true;
                }
                continue;
            }

            // method 2: list of block faces
            // dump HOTBAR NORTH ...
            final BlockFace face = enumGet(BlockFace.values(), arg);
            if (face == null) {
                player.sendMessage(error("block face '&7" + arg + "&r' not found."));
                player.sendMessage(light("available", enumJoin(BlockFace.values())));
                return true;
            }
            final Block block = player.getLocation().getBlock().getRelative(face);
            if (!(block.getState() instanceof Container container)) {
                player.sendMessage(warn("block at " + face.name() + " is not a container."));
                continue;
            }
            containers.add(container);
        }
        if (containers.isEmpty()) {
            player.sendMessage(warn("no containers given. nothing to do."));
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
        for (final Container container : containers) {
            for (final Type type : types) {
                // do transfer and show particles
                switch (type.transfer(inventory, container, materials)) {
                    case MOVED_SOME -> {
                        this.particlify(container.getBlock(), Particle.VILLAGER_HAPPY);
                        player.sendMessage(light("debug", "sent at least 1 item to " +
                                Util.simpleLocation(container)));
                    }
                    case MOVED_NONE -> {
                        this.particlify(container.getBlock(), Particle.VILLAGER_ANGRY);
                        player.sendMessage(light("debug", "sent no item to " +
                                Util.simpleLocation(container)));
                    }
                }
            }
        }
        return true;
    }

}
