package gg.sap.smp.itemremover.modules;

import gg.sap.smp.itemremover.util.Format;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.BlockIterator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class QuickCraftModule {

    private final JavaPlugin plugin;

    public final QuickCraftCommand quickCraftCommand;
    public final QuickScheduleCommand quickScheduleCommand;

    public QuickCraftModule(JavaPlugin plugin) {
        this.plugin = plugin;

        this.quickCraftCommand = new QuickCraftCommand();
        this.quickScheduleCommand = new QuickScheduleCommand();
    }

    /**
     * Haven't found a meaningful name for this record.
     *
     * @param matrix Matrix
     * @param recipe Recipe. Yes
     */
    public record A(ItemStack[] matrix, Recipe recipe) {

        /**
         * Parse arguments, create craft matrix and find corresponding recipe
         *
         * @param world World
         * @param args  Arguments
         * @return hopefully an A.
         */
        public static A create(
                @NotNull final World world,
                @NotNull final String[] args
        ) {
            // create craft matrix for separateArgs
            final String matrix = String.join(
                    "", Arrays.copyOfRange(args, 0, 3)
            ).toUpperCase();
            if (matrix.length() != 9) {
                throw new IllegalArgumentException(
                        "matrix of length 9 expected. got " + matrix.length() + " [" + matrix + "]"
                );
            }

            // compile matrix to ItemStacks
            final ItemStack[] craftMatrix = A.compileMatrix(
                    matrix, Arrays.copyOfRange(args, 3, args.length)
            );

            // get recipe for matrix
            final Recipe targetRecipe = Bukkit.getCraftingRecipe(craftMatrix, world);
            if (targetRecipe == null) {
                throw new NullPointerException("recipe not found");
            }

            return new A(craftMatrix, targetRecipe);
        }

        public void process(
                @Nullable final Player player,
                @NotNull final Inventory inventory,
                @NotNull final Inventory output
        ) {
            // which items should be removed from the inventory
            final ItemStack[] toRemove = Arrays.stream(this.matrix())
                    .filter(Objects::nonNull)
                    .toArray(ItemStack[]::new);

            // how many items have been crafted
            long count = 0;
            while (true) {
                // create dummy inventory
                final Inventory dummy = Bukkit.createInventory(null, inventory.getType());
                dummy.setContents(inventory.getContents());

                // remove items required for crafting
                final HashMap<Integer, ItemStack> removeMap = dummy.removeItemAnySlot(toRemove);
                if (removeMap.size() != 0) {
                    break;
                }

                // if items were removed, update "real" inventory
                inventory.setContents(dummy.getContents());

                // execute crafting
                final HashMap<Integer, ItemStack> addMap = output.addItem(this.recipe().getResult());
                if (addMap.size() != 0) {
                    addMap.values().forEach(item -> player.getWorld().dropItem(player.getLocation(), item));
                }
                // count total craft count
                count += this.recipe().getResult().getAmount();
            }
            if (player != null) {
                Format.info(player, "Crafted &e" + count + "&8x&r" + this.recipe().getResult().getType().name());
            }
        }

        public static ItemStack[] compileMatrix(
                final String matrix,
                final String... ingredients
        ) {
            final Map<Character, Material> matrixMaterials = new HashMap<>();
            for (final String ingregient : ingredients) {
                final String[] ingredient = ingregient.split("=");
                if (ingredient.length != 2) {
                    throw new IllegalArgumentException("ingredient in wrong format. expected <char>=<material>");
                }
                if (ingredient[0].length() != 1) {
                    throw new IllegalArgumentException("single char expected");
                }
                final Material targetMaterial = Material.getMaterial(ingredient[1].toUpperCase());
                if (targetMaterial == null) {
                    throw new IllegalArgumentException("&rmaterial &e" + ingredient[1] + "&r not found.");
                }
                matrixMaterials.put(
                        ingredient[0].toUpperCase().charAt(0),
                        targetMaterial
                );
            }
            final ItemStack[] craftMatrix = new ItemStack[9];
            for (int i = 0; i < 9; i++) {
                final Material material = matrixMaterials.get(matrix.charAt(i));
                if (material != null) {
                    craftMatrix[i] = new ItemStack(material);
                }
            }
            return craftMatrix;
        }

        public static Set<A> parseAll(final String[] args, final World world) throws IllegalArgumentException {
            final Set<A> as = new HashSet<>();
            final List<String> argsList = new ArrayList<>();
            for (int i = 0; i < args.length; i++) {
                final String arg = args[i];
                if (";".equals(arg) || i == (args.length) - 1) {
                    // add last argument
                    if (!";".equals(arg) && i == (args.length - 1)) {
                        argsList.add(arg);
                    }

                    final String[] separateArgs = argsList.toArray(new String[0]);
                    argsList.clear();

                    // compile matrix and get recipe for matrix
                    as.add(A.create(world, separateArgs));
                    continue;
                }
                argsList.add(arg);
            }
            return as;
        }

    }

    public record T(Inventory inventory, Inventory output) {
        public static T create(final Inventory inventory, final Inventory output) {
            return new T(inventory, output);
        }

        public static T create(final Inventory inventory) {
            return new T(inventory, inventory);
        }

        public void run(final Player player, final Iterable<A> it) {
            it.forEach(a -> a.process(player, this.inventory(), this.output()));
        }
    }

    private static class QuickCraftCommand implements CommandExecutor {

        @Override
        public boolean onCommand(
                @NotNull final CommandSender sender,
                @NotNull final Command command,
                @NotNull String label,
                @NotNull final String[] args
        ) {
            if (!(sender instanceof final Player player)) {
                sender.sendMessage("This command is not intended for non-players.");
                return true;
            }

            if (args.length == 0) {
                Format.info(player, "/qc <top> <mid> <bot> <k=material>");
                Format.info(player, "/qcc <top> <mid> <bot> <k=material>");
                Format.info(player, "/qccs <top> <mid> <bot> <k=material>");
                Format.info(player, "/qcc-> <top> <mid> <bot> <k=material>");
                Format.info(player, "/qccs-> <top> <mid> <bot> <k=material>");
                return true;
            }

            final Set<A> as;
            try {
                as = A.parseAll(args, player.getWorld());
            } catch (final IllegalArgumentException iaex) {
                Format.error(player, iaex.getMessage());
                return true;
            }

            final Set<T> targets = new HashSet<>();
            switch (label) {
                case "qcc", "qccs", "qcc->", "qccs->" -> {
                    // should multiple containers be scanned?
                    final boolean multiple = label.endsWith("->");
                    if (multiple) {
                        label = label.substring(0, label.length() - 2);
                    }
                    // should the output items be added to the player?
                    final boolean self = label.endsWith("s");

                    // find targeted inventories
                    final BlockIterator iterator = new BlockIterator(player, 10);
                    while (iterator.hasNext()) {
                        final Block block = iterator.next();
                        if (block.getState() instanceof final Container container) {
                            targets.add(self
                                    ? T.create(container.getInventory(), player.getInventory())
                                    : T.create(container.getInventory()
                            ));
                            if (!multiple) {
                                break;
                            }
                        }
                    }
                    if (targets.size() <= 0) {
                        Format.error(player, "no container in range.");
                        return true;
                    }
                }
                default -> targets.add(T.create(player.getInventory()));
            }

            // execute auto-crafting
            targets.forEach(t -> t.run(player, as));
            Format.info(player, String.format("processed %d recipes in %d inventories",
                    as.size(), targets.size()));
            return true;
        }
    }

    private class QuickScheduleCommand implements CommandExecutor {

        private final Set<Task> tasks = new HashSet<>();

        public static final class Task {
            private static int taskIdCounter = 0;

            public final int id;
            public final UUID creator;
            public final Block block;
            public final Set<A> a;
            public final long ticks;
            public long current;

            public Task(
                    final UUID creator,
                    final long ticks,
                    final Block block,
                    final Set<A> a
            ) {
                this.id = Task.taskIdCounter++;
                this.creator = creator;

                // reset ticks
                this.ticks = ticks;
                this.current = this.ticks;

                this.block = block;
                this.a = a;
            }

        }


        public QuickScheduleCommand() {
            Bukkit.getScheduler().runTaskTimer(QuickCraftModule.this.plugin, this::tickTasks, 1, 1);
        }

        private void tickTasks() {
            // run through tasks
            for (final Task task : this.tasks) {
                if (--task.current >= 0) {
                    continue;
                }
                task.current = task.ticks;

                // ignore unloaded blocks
                if (!task.block.getChunk().isLoaded()) {
                    continue;
                }
                final Container container = (Container) task.block.getState();
                for (final A a : task.a) {
                    a.process(null, container.getInventory(), container.getInventory());
                }
                task.block.getWorld().spawnParticle(Particle.NOTE, task.block.getLocation(), 1);
                System.out.println("Ran task " + task.id);
            }
        }

        // /qsb <ticks> <top> <mid> <bot> <k=v...>
        private void handleBlock(final Player player, final String[] args) {
            // check argument count
            if (args.length < 5) {
                Format.info(player, "/qsb <ticks> <top> <mid> <bot> <k=v...>");
                return;
            }

            // get tick time
            final long ticks;
            try {
                ticks = Long.parseLong(args[0]);
            } catch (final NumberFormatException nfex) {
                Format.error(player, "cannot parse ticks to a number");
                return;
            }

            // ray trace container
            Container container = null;
            Block block = null;
            final BlockIterator iterator = new BlockIterator(player, 10);
            while (iterator.hasNext()) {
                final Block b = iterator.next();
                if (b.getState() instanceof final Container c) {
                    container = c;
                    block = b;
                    break;
                }
            }
            if (container == null) {
                Format.error(player, "cannot find container");
                return;
            }

            // parse As
            final Set<A> as;
            try {
                as = A.parseAll(Arrays.copyOfRange(args, 1, args.length), player.getWorld());
            } catch (final IllegalArgumentException iaex) {
                Format.error(player, iaex.getMessage());
                return;
            }

            final Task task = new Task(player.getUniqueId(), ticks, block, as);
            this.tasks.add(task);

            Format.info(player, "task &e" + task.id + "&r created!");
        }

        // /qs list
        private void handleList(final CommandSender sender, final String[] args) {
            if (this.tasks.size() <= 0) {
                Format.warn(sender, "no running tasks");
                return;
            }

            for (final Task task : this.tasks) {
                // get owner name
                final OfflinePlayer ownerPlayer = Bukkit.getOfflinePlayer(task.creator);
                final String owner;
                if (ownerPlayer.getName() != null) {
                    owner = ownerPlayer.getName();
                } else {
                    owner = "<Unknown>";
                }

                // get location string
                final Location blockLocation = task.block.getLocation();
                final String location = String.format("%d %d %d (%s)",
                        blockLocation.getBlockX(), blockLocation.getBlockY(), blockLocation.getBlockY(),
                        blockLocation.getWorld().getName());

                Format.info(sender, "&8- &7" + location + "&r by &e" + owner + "&r id: &b" + task.id);
            }
        }

        // /qs stop <id>
        private void handleStop(final CommandSender sender, final String[] args) {
            if (args.length != 2) {
                Format.error(sender, "/qs stop <id>");
                return;
            }
            // get task id
            final int id;
            try {
                id = Integer.parseInt(args[1]);
            } catch (final NumberFormatException nfex) {
                Format.error(sender, "cannot parse number");
                return;
            }

            // find task by id
            if (this.tasks.stream().noneMatch(t -> t.id == id)) {
                Format.error(sender, "task not found");
                return;
            }

            // remove task
            this.tasks.removeIf(t -> t.id == id);

            Format.info(sender, "removed task");
        }

        @Override
        public boolean onCommand(
                @NotNull final CommandSender sender,
                @NotNull final Command command,
                @NotNull String label,
                @NotNull final String[] args
        ) {
            if (args.length == 0) {
                Format.info(sender, "/qs list");
                Format.info(sender, "/qs stop <id>");
                Format.info(sender, "/qsb <ticks> <top> <mid> <bot> <k=v...>");
                return true;
            }

            // /qsb 3 aaa aaa aaa a=emerald
            if ("qsb".equalsIgnoreCase(label)) {
                if (!(sender instanceof final Player player)) {
                    Format.warn(sender, "this command is only intended for players");
                    return true;
                }
                this.handleBlock(player, args);
                return true;
            }

            switch (args[0]) {
                // /qs list
                case "list" -> this.handleList(sender, args);

                // /qs stop <id>
                case "stop" -> this.handleStop(sender, args);
            }

            return true;
        }
    }

}
