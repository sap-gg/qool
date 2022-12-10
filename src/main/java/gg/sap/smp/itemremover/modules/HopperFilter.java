package gg.sap.smp.itemremover.modules;

import net.kyori.adventure.text.TextComponent;
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

    private boolean matchesAny(final String[] filterNames, final String itemName) {
        for (final String name : filterNames) {
            if (this.matches(name, itemName)) {
                return true;
            }
        }
        return false;
    }

    private boolean matches(String filterName, final String itemName) {
        boolean yes = true;
        if (filterName.startsWith("!")) {
            filterName = filterName.substring(1);
            yes = false;
        }
        if (filterName.startsWith("*")) {
            return itemName.endsWith(filterName.substring(1)) == yes;
        }
        if (filterName.endsWith("*")) {
            return itemName.startsWith(filterName.substring(0, filterName.length() - 1)) == yes;
        }
        return itemName.equals(filterName) == yes;
    }

    enum Result {
        ACCEPT,
        REJECT,
        OBLITERATE
    }

    public Result run(final Container container, final ItemStack stack) {
        if (!(container.customName() instanceof TextComponent component)) {
            return Result.ACCEPT;
        }
        // allow hopper if no custom name was set
        String content = component.content().toUpperCase();
        if (content.isBlank()) {
            return Result.ACCEPT;
        }

        final boolean obliterate = content.startsWith("(?D)");
        if (obliterate) {
            content = content.substring(4);
        }

        final String itemName = stack.getType().name().toUpperCase();
        if (this.matchesAny(content.split(",\\s*"), itemName)) {
            return obliterate ? Result.OBLITERATE : Result.ACCEPT;
        }
        return Result.REJECT;
    }

    public void runOn(
            @NotNull final Cancellable cancellable,
            @NotNull final Container container,
            @NotNull final ItemStack stack,
            @Nullable final Item item
    ) {
        switch (this.run(container, stack)) {
            case REJECT -> cancellable.setCancelled(true);
            case OBLITERATE -> {
                cancellable.setCancelled(true);
                if (item != null) {
                    item.remove();
                }
            }
        }
    }

    @EventHandler
    public void onInventoryMoveItemEvent(final InventoryMoveItemEvent event) {
        // only check for hoppers
        if (!InventoryType.HOPPER.equals(event.getDestination().getType())) {
            return;
        }
        if (!(event.getDestination().getHolder() instanceof Container container)) {
            return;
        }
        this.runOn(event, container, event.getItem(), null);
    }

    @EventHandler
    public void onInventoryPickupItemEvent(final InventoryPickupItemEvent event) {
        if (event.getInventory().getHolder() instanceof Container container) {
            this.runOn(event, container, event.getItem().getItemStack(), event.getItem());
        }
    }

}
