package gg.sap.smp.itemremover.modules;

import gg.sap.smp.itemremover.util.Format;
import org.bukkit.Bukkit;
import org.bukkit.Material;
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
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class QuickCraftCommand implements CommandExecutor {

    private void process(
            @NotNull final Player player,
            @NotNull final Inventory inventory,
            final @Nullable ItemStack @NotNull [] craftMatrix,
            @NotNull final Recipe recipe
    ) {
        final ItemStack[] toRemove = Arrays.stream(craftMatrix)
                .filter(Objects::nonNull)
                .toArray(ItemStack[]::new);

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
            final HashMap<Integer, ItemStack> addMap = inventory.addItem(recipe.getResult());
            if (addMap.size() != 0) {
                addMap.values().forEach(item -> player.getWorld().dropItem(player.getLocation(), item));
            }
            // count total craft count
            count += recipe.getResult().getAmount();
        }
        Format.info(player, "Crafted &e" + count + "&8x&r" + recipe.getResult().getType().name());
    }

    private boolean startCrafting(
            @NotNull final Player player,
            @NotNull final Inventory inventory,
            @NotNull final String[] args
    ) {
        // /ac nnn nnn nnn n=gold_nugget
        final String matrix = String.join("", Arrays.copyOfRange(args, 0, 3)).toUpperCase();
        if (matrix.length() != 9) {
            Format.error(player, "matrix of length 9 expected. got " + matrix.length());
            return false;
        }
        final Map<Character, Material> matrixMaterials = new HashMap<>();
        for (int i = 3; i < args.length; i++) {
            final String[] ingredient = args[i].split("=");
            if (ingredient.length != 2) {
                Format.error(player, "ingredient in wrong format. expected <char>=<material>");
                return false;
            }
            if (ingredient[0].length() != 1) {
                Format.error(player, "single char expected.");
                return false;
            }
            final Material targetMaterial = Material.getMaterial(ingredient[1].toUpperCase());
            if (targetMaterial == null) {
                Format.error(player, "&rmaterial &e" + ingredient[1] + "&r not found.");
                return false;
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
        final Recipe targetRecipe = Bukkit.getCraftingRecipe(craftMatrix, player.getWorld());
        if (targetRecipe == null) {
            Format.error(player, "recipe not found.");
            return false;
        }
        System.out.println("Crafting " + matrix + " -> " + targetRecipe.getResult().getType().name());
        this.process(player, inventory, craftMatrix, targetRecipe);
        return true;
    }

    // qcc <...>

    @Override
    public boolean onCommand(
            @NotNull final CommandSender sender,
            @NotNull final Command command,
            @NotNull final String label,
            @NotNull final String[] args
    ) {
        if (!(sender instanceof final Player player)) {
            sender.sendMessage("This command is not intended for non-players.");
            return true;
        }

        if (args.length == 0) {
            Format.info(player, "/qc <top> <mid> <bot> <k=material>");
            Format.info(player, "/qcc <top> <mid> <bot> <k=material>");
            return true;
        }

        // toggle container mode
        Inventory inventory = null;
        if ("qcc".equalsIgnoreCase(label)) {
            final BlockIterator iterator = new BlockIterator(player, 10);
            while(iterator.hasNext()) {
                final Block block = iterator.next();
                if (block.getState() instanceof final Container container) {
                    inventory = container.getInventory();
                    break;
                }
            }
            if (inventory == null) {
                Format.error(player, "no container in range.");
                return true;
            }
        } else {
            inventory = player.getInventory();
        }

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

                if (!this.startCrafting(player, inventory, separateArgs)) {
                    return true;
                }
                continue;
            }
            argsList.add(arg);
        }
        Format.info(player, "&rProcessed &b" + count + " &rcrafting recipes.");
        return true;
    }

}
