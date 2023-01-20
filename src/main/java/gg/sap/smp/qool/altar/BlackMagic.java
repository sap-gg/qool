package gg.sap.smp.qool.altar;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class BlackMagic {

    public static final NamespacedKey ENCHANTMENTS_KEY = new NamespacedKey("sap-gg", "ii-enchantments");

    // you could argue that an exception would be better,
    // but I disagree.
    public enum CombineBookResult {
        SUCCESS,
        NO_NEW_ENCHANTMENTS,
        NO_ENCHANTMENT_STORAGE,
        NOT_A_BOOK
    }

    public static @NotNull CombineBookResult combineBook(
            @NotNull final ItemStack stack,
            @NotNull final ItemStack book
    ) {
        if (book.getType() != Material.ENCHANTED_BOOK) {
            return CombineBookResult.NOT_A_BOOK;
        }
        if (!(book.getItemMeta() instanceof final EnchantmentStorageMeta esm)) {
            return CombineBookResult.NO_ENCHANTMENT_STORAGE;
        }

        // collect enchantments and their levels which have to be added to the stack
        final Map<Enchantment, Integer> toAdd = new HashMap<>();
        for (final Map.Entry<Enchantment, Integer> entry : esm.getStoredEnchants().entrySet()) {
            final Integer currentLevel = stack.getEnchantments().get(entry.getKey());
            if (currentLevel == null || currentLevel < entry.getValue()) {
                toAdd.put(entry.getKey(), entry.getValue());
            }
        }
        if (toAdd.size() == 0) {
            return CombineBookResult.NO_NEW_ENCHANTMENTS;
        }

        final ItemMeta meta = stack.getItemMeta();
        final PersistentDataContainer pdc = meta.getPersistentDataContainer();

        // mark enchantment illegal add add to pdc
        for (final Enchantment enchantment : toAdd.keySet()) {
            addEnchantmentToPersistentDataContainer(pdc, enchantment);
        }

        // add lore to item
        final List<Component> lore = buildLoreForPersistentDataContainer(meta, pdc);
        if (lore != null) {
            meta.lore(lore);
        }

        // add enchantments
        for (final Map.Entry<Enchantment, Integer> entry : toAdd.entrySet()) {
            meta.addEnchant(entry.getKey(), entry.getValue(), true);
        }

        stack.setItemMeta(meta);
        return CombineBookResult.SUCCESS;
    }

    public static void incrementEnchantment(
            @NotNull final ItemStack stack,
            @NotNull final Enchantment enchantment
    ) {
        final ItemMeta meta = stack.getItemMeta();

        // build lore
        final PersistentDataContainer pdc = meta.getPersistentDataContainer();

        // append new enchantment to list
        addEnchantmentToPersistentDataContainer(pdc, enchantment);

        final List<Component> lore = buildLoreForPersistentDataContainer(meta, pdc);
        if (lore != null) {
            meta.lore(lore);
        }

        meta.addEnchant(enchantment, meta.getEnchantLevel(enchantment) + 1, true);
        stack.setItemMeta(meta);
    }

    /// helper methods below 👇

    public static <T> @NotNull T orElse(@Nullable final T inp, @NotNull final T def) {
        if (inp != null) {
            return inp;
        }
        return def;
    }

    public static <T> @NotNull T orElseClamp(final boolean ok, @Nullable final T inp, @NotNull final T def) {
        return ok ? orElse(inp, def) : def;
    }

    public static @Nullable Enchantment enchantmentByHashCode(final int hash) {
        return Arrays.stream(Enchantment.values())
                .filter(e -> e.hashCode() == hash)
                .findFirst()
                .orElse(null);
    }

    public static Set<Enchantment> getIllegalEnchantments(final PersistentDataContainer pdc) {
        final int[] enchantments = orElseClamp(
                pdc.has(ENCHANTMENTS_KEY),
                pdc.get(ENCHANTMENTS_KEY, PersistentDataType.INTEGER_ARRAY),
                new int[0]
        );
        return Arrays.stream(enchantments)
                .mapToObj(BlackMagic::enchantmentByHashCode)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }


    private static void addEnchantmentToPersistentDataContainer(
            final PersistentDataContainer pdc,
            final Enchantment enchantment
    ) {
        final int[] enchantments = orElseClamp(
                pdc.has(ENCHANTMENTS_KEY),
                pdc.get(ENCHANTMENTS_KEY, PersistentDataType.INTEGER_ARRAY),
                new int[0]
        );
        final int[] newEnchantments = new int[enchantments.length + 1];
        System.arraycopy(enchantments, 0, newEnchantments, 0, enchantments.length);
        newEnchantments[newEnchantments.length - 1] = enchantment.hashCode();
        pdc.set(ENCHANTMENTS_KEY, PersistentDataType.INTEGER_ARRAY, newEnchantments);
    }

    private static List<Component> buildLoreForPersistentDataContainer(
            final ItemMeta meta,
            final PersistentDataContainer pdc
    ) {
        // contains all illegal enchantments for the item
        final Set<Enchantment> illegalEnchantmentSet = getIllegalEnchantments(pdc);
        if (illegalEnchantmentSet.size() <= 0) {
            return null;
        }

        final String enchantmentNames = illegalEnchantmentSet.stream()
                .map(Enchantment::getKey)
                .map(NamespacedKey::getKey)
                .collect(Collectors.joining(", "));

        // build lore from list
        final List<Component> lore = orElse(meta.lore(), new ArrayList<>());

        boolean contained = false;
        if (lore.size() > 0) {
            if (lore.get(0) instanceof final TextComponent text) {
                if (text.content().equalsIgnoreCase("Beware! This item contains black magic.")) {
                    contained = true;
                }
            }
        }
        // add placeholder (overwritten in the next lore.set call)
        if (!contained) {
            lore.add(0, Component.text(""));
            lore.add(1, Component.text(""));
        }

        lore.set(0, Component.text("Beware! This item contains black magic.", NamedTextColor.LIGHT_PURPLE));
        lore.set(1, Component.text("The enchantment(s) ", NamedTextColor.LIGHT_PURPLE)
                .append(Component.text(enchantmentNames, NamedTextColor.DARK_PURPLE))
                .append(Component.text(" were illegally changed", NamedTextColor.LIGHT_PURPLE)));
        return lore;
    }

}
