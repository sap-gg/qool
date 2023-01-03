package gg.sap.smp.itemremover.modules;

import gg.sap.smp.itemremover.util.Format;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.util.BlockIterator;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class QuickCraftCommand implements CommandExecutor {

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
                @NotNull final Player player,
                @NotNull final Inventory inventory,
                @NotNull final Inventory output
        ) {
            // which items should be removed from the inventory
            final ItemStack[] toRemove = Arrays.stream(this.matrix())
                    .filter(Objects::nonNull)
                    .toArray(ItemStack[]::new);

            // how many items have been crafted
            long count = 0;
            ItemStack[] contents;
            while (true) {
                // copy inventory contents for later rollback
                contents = Arrays.copyOfRange(
                        inventory.getContents(),
                        0, inventory.getContents().length
                );
                // remove items required for crafting
                final HashMap<Integer, ItemStack> removeMap = inventory.removeItemAnySlot(toRemove);
                if (removeMap.size() != 0) {
                    inventory.setContents(contents);
                    break;
                }
                // execute crafting
                final HashMap<Integer, ItemStack> addMap = output.addItem(this.recipe().getResult());
                if (addMap.size() != 0) {
                    addMap.values().forEach(item -> player.getWorld().dropItem(player.getLocation(), item));
                }
                // count total craft count
                count += this.recipe().getResult().getAmount();
            }
            Format.info(player, "Crafted &e" + count + "&8x&r" + this.recipe().getResult().getType().name());
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

        // contains compiled recipes
        final List<A> as = new ArrayList<>();

        // parse arguments
        long count = 0;
        final List<String> argsList = new ArrayList<>();
        for (int i = 0; i < args.length; i++) {
            final String arg = args[i];
            if (";".equals(arg) || i == (args.length) - 1) {
                // add last argument
                if (!";".equals(arg) && i == (args.length - 1)) {
                    argsList.add(arg);
                }
                // add to recipe count
                count++;

                final String[] separateArgs = argsList.toArray(new String[0]);
                argsList.clear();

                // compile matrix and get recipe for matrix
                final A a;
                try {
                    a = A.create(player.getWorld(), separateArgs);
                } catch (final Exception exception) {
                    Format.error(player, exception.getMessage());
                    return true;
                }
                as.add(a);
                continue;
            }
            argsList.add(arg);
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
                count, targets.size()));
        return true;
    }

}
