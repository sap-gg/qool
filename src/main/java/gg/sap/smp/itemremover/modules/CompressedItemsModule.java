package gg.sap.smp.itemremover.modules;

import gg.sap.smp.itemremover.util.Format;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class CompressedItemsModule implements Listener {

    private final JavaPlugin plugin;
    private final Map<Material, Integer> materials = new HashMap<>();
    private final Map<ItemStack, Set<Material>> craftWhiteList = new HashMap<>();


    public static final NamedTextColor[] LEVEL_COLORS = {
            NamedTextColor.YELLOW,
            NamedTextColor.RED,
            NamedTextColor.LIGHT_PURPLE
    };

    public static final NamespacedKey LEVEL_KEY = new NamespacedKey("sapgg", "zip-level");
    public static final NamespacedKey AMOUNT_KEY = new NamespacedKey("sapgg", "zip-amount");
    public static final NamespacedKey UNZIPPED_KEY = new NamespacedKey("sapgg", "zip-unzipped");

    // Utils

    public CompressedItemsModule(final JavaPlugin plugin) {
        this.plugin = plugin;

        materials.put(Material.GUNPOWDER, 9);
        materials.put(Material.BONE, 9);
        materials.put(Material.TOTEM_OF_UNDYING, 9);

        // Farm Material
        materials.put(Material.STRING, 9);
        materials.put(Material.SUGAR_CANE, 9);
        materials.put(Material.ENDER_PEARL, 9);
        materials.put(Material.ROTTEN_FLESH, 9);
        materials.put(Material.OBSIDIAN, 9);
        materials.put(Material.ARROW, 9);

        // Other

        materials.put(Material.STICK, 9);
        materials.put(Material.BAMBOO, 9);
        materials.put(Material.WITHER_SKELETON_SKULL, 9);
        materials.put(Material.FLINT, 9);

        // Ores
        materials.put(Material.NETHERITE_INGOT, 8);
        materials.put(Material.DIAMOND, 8);
        materials.put(Material.GOLD_INGOT, 8);
        materials.put(Material.IRON_INGOT, 8);
        materials.put(Material.REDSTONE, 8);
        materials.put(Material.COAL, 8);
        materials.put(Material.CHARCOAL, 8);
        materials.put(Material.EMERALD, 8);
        materials.put(Material.LAPIS_LAZULI, 8);
        materials.put(Material.QUARTZ, 8);
        materials.put(Material.AMETHYST_SHARD, 8);
        materials.put(Material.COPPER_INGOT, 8);

        // Blocks
        materials.put(Material.STONE, 9);
        materials.put(Material.COBBLESTONE, 9);
        materials.put(Material.DEEPSLATE, 9);
        materials.put(Material.COBBLED_DEEPSLATE, 9);
        materials.put(Material.GRASS, 9);
        materials.put(Material.DIRT, 9);
        materials.put(Material.SAND, 9);

        // add log types
        Arrays.stream(Material.values())
                .filter(m -> !m.isLegacy())
                .filter(m -> m.name().contains("LOG") || m.name().contains("PLANKS"))
                .forEach(mat -> materials.put(mat, 9));

        // create recipes for <LEVEL_COLORS> levels
        for (int i = 1; i <= LEVEL_COLORS.length; i++) {
            for (final Map.Entry<Material, Integer> entry : materials.entrySet()) {
                final Material material = entry.getKey();
                final int amount = entry.getValue();

                final ItemStack current = createCompressedItemStack(material, i, amount);
                final ItemStack previous = createCompressedItemStack(material, i - 1, amount);

                final ShapelessRecipe zipRecipe = new ShapelessRecipe(
                        createKey(plugin, material, i, "zip"),
                        current
                ).addIngredient(amount, previous);
                Bukkit.addRecipe(zipRecipe);

                final ItemStack previousWithAmount = previous.clone();
                previousWithAmount.setAmount(amount);

                final ShapelessRecipe unzipRecipe = new ShapelessRecipe(
                        createKey(plugin, material, i, "unzip"),
                        previousWithAmount
                ).addIngredient(1, current);
                Bukkit.addRecipe(unzipRecipe);
            }
        }

        // extra: bone blocks
        this.createFastCraftRecipes(3, Material.BONE, 1, Material.BONE_BLOCK);
        this.createFastCraftRecipes(4, Material.STRING, 1, Material.WHITE_WOOL);
        this.createFastCraftRecipes(3, Material.SUGAR_CANE, 3, Material.PAPER);
        this.createFastCraftRecipes(4, Material.GLOWSTONE_DUST, 1, Material.GLOWSTONE);
        this.createFastCraftRecipes(9, Material.NETHERITE_INGOT, 1, Material.NETHERITE_BLOCK);
        this.createFastCraftRecipes(9, Material.DIAMOND, 1, Material.DIAMOND_BLOCK);
        this.createFastCraftRecipes(9, Material.GOLD_INGOT, 1, Material.GOLD_BLOCK);
        this.createFastCraftRecipes(9, Material.IRON_INGOT, 1, Material.IRON_BLOCK);
        this.createFastCraftRecipes(9, Material.REDSTONE, 1, Material.REDSTONE_BLOCK);
        this.createFastCraftRecipes(9, Material.COAL, 1, Material.COAL_BLOCK);
        this.createFastCraftRecipes(9, Material.EMERALD, 1, Material.EMERALD_BLOCK);
        this.createFastCraftRecipes(9, Material.LAPIS_LAZULI, 1, Material.LAPIS_BLOCK);
        this.createFastCraftRecipes(4, Material.QUARTZ, 1, Material.QUARTZ_BLOCK);
        this.createFastCraftRecipes(9, Material.COPPER_INGOT, 1, Material.COPPER_BLOCK);

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /// Events

    @EventHandler
    public void onBlockPlace(final BlockPlaceEvent event) {
        final ItemStack stack = event.getItemInHand();
        if (!stack.hasItemMeta()) {
            return;
        }
        final ItemMeta meta = stack.getItemMeta();
        final PersistentDataContainer persistent = meta.getPersistentDataContainer();
        if (!persistent.has(LEVEL_KEY)) {
            return;
        }

        final Player player = event.getPlayer();
        final Integer totalAmount;
        final Integer unzippedAmount;

        try {
            if (stack.getAmount() > 1) {
                throw new Exception("max. 1 item in hand allowed.");
            }
            if (!persistent.has(AMOUNT_KEY)) {
                throw new Exception("amount key not found.");
            }
            if (!persistent.has(UNZIPPED_KEY)) {
                throw new Exception("unzipped key not found.");
            }
            totalAmount = persistent.get(AMOUNT_KEY, PersistentDataType.INTEGER);
            if (totalAmount == null) {
                throw new Exception("amount key invalid.");
            }
            unzippedAmount = persistent.get(UNZIPPED_KEY, PersistentDataType.INTEGER);
            if (unzippedAmount == null) {
                throw new Exception("unzipped key invalid.");
            }
        } catch (final Exception exception) {
            event.setCancelled(true);
            Format.error(player, "cannot unzip block. &7" + exception.getMessage());
            return;
        }

        // unzip block
        final int newUnzippedAmount = unzippedAmount + 1;
        if (totalAmount - newUnzippedAmount <= 0) {
            // remove block from inventory
            player.getInventory().setItem(event.getHand(), null);
        } else {
            // update block to include new amount
            persistent.set(UNZIPPED_KEY, PersistentDataType.INTEGER, newUnzippedAmount);
            meta.lore(List.of(getZipUsageLore(persistent)));
            meta.displayName(getDisplayName(stack, -1, persistent));
            stack.setItemMeta(meta);
        }
    }

    @EventHandler
    public void onAmountCraft(final PrepareItemCraftEvent event) {
        final ItemStack result = event.getInventory().getResult();
        if (result == null) {
            return;
        }
        for (final ItemStack stack : event.getInventory().getMatrix()) {
            if (stack == null || !stack.hasItemMeta()) {
                continue;
            }

            final ItemMeta meta = stack.getItemMeta();
            final PersistentDataContainer dataContainer = meta.getPersistentDataContainer();
            if (!dataContainer.has(AMOUNT_KEY) || !dataContainer.has(UNZIPPED_KEY)) {
                continue;
            }

            final Integer unzippedAmount = dataContainer.get(UNZIPPED_KEY, PersistentDataType.INTEGER);
            if (unzippedAmount != null && unzippedAmount > 0) {
                // always cancel any crafting with unzipped items
                event.getInventory().setResult(null);
                return;
            }

            // ignore compressed -> raw
            if (result.getType() == stack.getType() && result.getAmount()
                    == this.materials.getOrDefault(stack.getType(), 9)) {
                continue;
            }

            // ignore compressed -> compressed
            if (result.hasItemMeta() && result.getItemMeta().getPersistentDataContainer().has(AMOUNT_KEY)) {
                continue;
            }

            // do not ignore non-whitelisted
            if (!this.isWhiteList(stack, result.getType())) {
                event.getInventory().setResult(null);
                return;
            }
        }
    }

    /// Utilities

    private static Component createLevelComponent(final int unzipped, final int amount) {
        return Component.text("[", NamedTextColor.DARK_GRAY)
                .append(Component.text(amount - unzipped, NamedTextColor.WHITE))
                .append(Component.text("/", NamedTextColor.DARK_GRAY))
                .append(Component.text(amount, NamedTextColor.WHITE))
                .append(Component.text("]", NamedTextColor.DARK_GRAY));
    }

    /**
     * Level {level} [{available}/{total}]
     *
     * @param data PersistentDataContainer
     * @return Lore as list
     */
    private static Component getZipUsageLore(final PersistentDataContainer data) {
        final int level = data.getOrDefault(LEVEL_KEY, PersistentDataType.INTEGER, -1);
        final int amount = data.getOrDefault(AMOUNT_KEY, PersistentDataType.INTEGER, -1);
        final int unzipped = data.getOrDefault(UNZIPPED_KEY, PersistentDataType.INTEGER, -1);
        if (level + amount + unzipped == -3) {
            return Component.text("Invalid Data.", NamedTextColor.RED);
        }
        return Component.text("Level ", NamedTextColor.GRAY)
                .append(Component.text(level, LEVEL_COLORS[Math.min(LEVEL_COLORS.length, level) - 1]))
                .append(Component.text(" "))
                .append(createLevelComponent(unzipped, amount));
    }

    private static Component getDisplayName(
            final ItemStack stack,
            int level,
            final PersistentDataContainer data
    ) {
        if (level < 0) {
            level = data.getOrDefault(LEVEL_KEY, PersistentDataType.INTEGER, 1);
        }
        final NamedTextColor color = LEVEL_COLORS[Math.min(LEVEL_COLORS.length, level) - 1];
        Component component = Component.text("Compressed ", color)
                .append(Component.translatable(stack, color));
        if (data.has(AMOUNT_KEY) && data.has(UNZIPPED_KEY)) {
            final Integer totalAmount = data.get(AMOUNT_KEY, PersistentDataType.INTEGER);
            final Integer unzippedAmount = data.get(UNZIPPED_KEY, PersistentDataType.INTEGER);
            if (totalAmount != null && unzippedAmount != null && unzippedAmount > 0) {
                component = component.append(Component.text(" ")
                        .append(createLevelComponent(unzippedAmount, totalAmount)));
            }
        }
        return component;
    }

    private static ItemStack createCompressedItemStack(final Material material, final int level, final int pow) {
        final ItemStack stack = new ItemStack(material, 1);
        if (level == 0) {
            return stack;
        }

        final ItemMeta meta = stack.getItemMeta();

        // amount of stored items
        final int contains = (int) Math.pow(pow, level);

        // save level to item
        meta.getPersistentDataContainer().set(LEVEL_KEY, PersistentDataType.INTEGER, level);
        meta.getPersistentDataContainer().set(AMOUNT_KEY, PersistentDataType.INTEGER, contains);
        meta.getPersistentDataContainer().set(UNZIPPED_KEY, PersistentDataType.INTEGER, 0);

        // change display name
        meta.displayName(getDisplayName(stack, level, meta.getPersistentDataContainer()));

        // add fake enchantment
        meta.addEnchant(Enchantment.DURABILITY, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

        // add lore
        meta.lore(List.of(getZipUsageLore(meta.getPersistentDataContainer())));

        stack.setItemMeta(meta);
        return stack;
    }

    private static NamespacedKey createKey(final JavaPlugin plugin, final Material material, final int level,
                                           final String extra) {
        return new NamespacedKey(plugin, String.format("recipe-%s-%d-%s", material.name(), level, extra));
    }

    private void addWhiteList(final ItemStack stack, final Material output) {
        final ItemStack transform = stack.clone();
        transform.setAmount(1);

        final Set<Material> set;
        if (this.craftWhiteList.containsKey(transform)) {
            set = this.craftWhiteList.get(transform);
        } else {
            set = new HashSet<>();
            this.craftWhiteList.put(transform, set);
        }
        set.add(output);
    }

    private boolean isWhiteList(final ItemStack input, final Material output) {
        final ItemStack transform = input.clone();
        transform.setAmount(1);
        final Set<Material> set = this.craftWhiteList.get(transform);
        if (set == null) {
            return false;
        }
        return set.contains(output);
    }

    private void createFastCraftRecipes(
            final int inputAmount,
            final Material input,
            final int outputAmount,
            final Material output
    ) {
        final int pow = this.materials.getOrDefault(input, 9);
        for (int i = 1; i <= LEVEL_COLORS.length; i++) {
            final ItemStack inputStack = createCompressedItemStack(input, i, pow);

            for (int j = pow - 1; j > 1; j--) {
                final int amount = (int) Math.pow(pow, i);
                final int totalOutputAmount = j * (amount / inputAmount) * outputAmount;
                if (totalOutputAmount > 64) {
                    continue;
                }
                final NamespacedKey key = createKey(this.plugin, output, i, "fastcraftx" + j + "-from-" + input.name());
                System.out.println("Adding Recipe: " + j + " x " + input.name() + " (Level " + i + ") = " + totalOutputAmount + " x " + output + " with key " + key.asString());
                if (Bukkit.addRecipe(new ShapelessRecipe(key,
                        new ItemStack(output, totalOutputAmount))
                        .addIngredient(j, inputStack))) {
                    System.out.println("Added quick craft recipe for " +
                            j + " x " + input.name() + " = " +
                            totalOutputAmount + " x " + output.name()
                    );

                    this.addWhiteList(inputStack, output);
                }
            }
        }
    }

}
