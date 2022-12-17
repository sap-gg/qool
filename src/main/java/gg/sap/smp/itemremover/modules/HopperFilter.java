package gg.sap.smp.itemremover.modules;

import net.kyori.adventure.text.TextComponent;
import org.bukkit.Material;
import org.bukkit.block.Container;
import org.bukkit.entity.Item;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

// Idea by LiveOverflow - thanks :)
public class HopperFilter implements Listener {

    public static final ItemStack AIR = new ItemStack(Material.AIR);

    // Note - itemName is in UPPER-CASE!
    // Note - filterNames is in UPPER-CASE!
    private Result matchesAny(@NotNull final String[] filterNames, @NotNull final String itemName) {
        for (final String name : filterNames) {
            final Result matchResult = this.matches(name, itemName);
            if (matchResult != Result.REJECT) {
                return matchResult;
            }
        }
        return Result.REJECT;
    }

    // Note - filterName is in UPPER-CASE!
    // Note - itemName is in UPPER-CASE!
    private Result matches(@NotNull String filterName, @NotNull final String itemName) {
        boolean yes = true;
        if (filterName.startsWith("!")) {
            filterName = filterName.substring(1);
            yes = false;
        }
        boolean obliterate = false;
        if (filterName.startsWith("(?D)")) {
            filterName = filterName.substring(4);
            obliterate = true;
        }
        if (filterName.startsWith("*")) {
            final String checking = filterName.substring(1);
            return Result.of(itemName.endsWith(checking) == yes, obliterate);
        }
        if (filterName.endsWith("*")) {
            final String checking = filterName.substring(0, filterName.length() - 1);
            return Result.of(itemName.startsWith(checking) == yes, obliterate);
        }
        return Result.of(itemName.equals(filterName) == yes, obliterate);
    }

    enum Result {
        ACCEPT,
        REJECT,
        OBLITERATE;

        public static Result of(final boolean b, final boolean obliterate) {
            return b ? (obliterate ? OBLITERATE : ACCEPT) : REJECT;
        }
    }

    public Result run(@NotNull final Container container, @NotNull final ItemStack stack) {
        if (!(container.customName() instanceof final TextComponent component)) {
            return Result.ACCEPT;
        }
        // allow hopper if no custom name was set
        final String content = component.content().toUpperCase();
        if (content.isBlank()) {
            return Result.ACCEPT;
        }
        final String itemName = stack.getType().name().toUpperCase();
        return this.matchesAny(content.split(",\\s*"), itemName);
    }

    public Result runOn(
            @NotNull final Cancellable cancellable,
            @NotNull final Container container,
            @NotNull final ItemStack stack,
            @Nullable final Item item
    ) {
        final Result result = this.run(container, stack);
        switch (result) {
            case REJECT -> cancellable.setCancelled(true);
            case OBLITERATE -> {
                cancellable.setCancelled(true);
                if (item != null) {
                    item.remove();
                }
            }
        }
        return result;
    }

    @EventHandler
    public void onInventoryMoveItemEvent(final InventoryMoveItemEvent event) {
        // only check for hoppers
        if (!InventoryType.HOPPER.equals(event.getDestination().getType())) {
            return;
        }
        if (!(event.getDestination().getHolder() instanceof final Container container)) {
            return;
        }
        if (this.runOn(event, container, event.getItem(), null) == Result.OBLITERATE) {
            event.setItem(AIR);
            event.setCancelled(false); // this might be illegal. beware.
        }
    }

    @EventHandler
    public void onInventoryPickupItemEvent(final InventoryPickupItemEvent event) {
        if (event.getInventory().getHolder() instanceof final Container container) {
            this.runOn(event, container, event.getItem().getItemStack(), event.getItem());
        }
    }

}
