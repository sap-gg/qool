package gg.sap.smp.qool.altar;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public enum Essence {

    WARDEN(
            EntityType.WARDEN,
            Material.POPPY,
            4,
            "Warden's Energized Heart",
            new NamespacedKey("sap-gg", "ii-warden-essence")
    ),
    DRAGON(
            EntityType.ENDER_DRAGON,
            Material.DRAGON_EGG,
            4,
            "Dragon's Birth Certificate",
            new NamespacedKey("sap-gg", "ii-dragon-essence")
    );

    ///
    public static final Map<Essence, ItemStack> ESSENCE_ITEM_STACK_MAP = new HashMap<>();

    public static ItemStack getItem(final Essence essence) {
        if (ESSENCE_ITEM_STACK_MAP.containsKey(essence)) {
            return ESSENCE_ITEM_STACK_MAP.get(essence);
        }
        final ItemStack stack = essence.item();
        ESSENCE_ITEM_STACK_MAP.put(essence, stack);
        return stack;
    }

    public static Essence byEntityType(final EntityType type) {
        for (final Essence value : values()) {
            if (value.type == type) {
                return value;
            }
        }
        return null;
    }

    ///

    private final EntityType type;
    private final int chance;
    private final String name;
    private final NamespacedKey key;
    private final Material material;

    Essence(final EntityType type, final Material material, final int chance, final String name, final NamespacedKey key) {
        this.type = type;
        this.material = material;
        this.chance = chance;
        this.name = name;
        this.key = key;
    }

    public boolean poll(final Random random) {
        return random.nextInt(this.chance) == (this.chance / 2);
    }

    public TextComponent text() {
        return Component.text(this.name, NamedTextColor.LIGHT_PURPLE);
    }

    private ItemStack item() {
        final ItemStack stack = new ItemStack(this.material, 1);
        final ItemMeta meta = stack.getItemMeta();
        meta.displayName(this.text());
        meta.getPersistentDataContainer().set(this.key, PersistentDataType.BYTE, (byte) 1);
        stack.setItemMeta(meta);
        return stack;
    }

    ///

    public boolean is(final ItemStack stack) {
        if (!stack.hasItemMeta()) {
            return false;
        }
        final PersistentDataContainer pdc = stack.getItemMeta().getPersistentDataContainer();
        return BlackMagic.orElseClamp(
                pdc.has(this.key),
                pdc.get(this.key, PersistentDataType.BYTE),
                (byte) 2
        ) == (byte) 1;
    }

}
