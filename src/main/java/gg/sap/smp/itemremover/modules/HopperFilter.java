package gg.sap.smp.itemremover.modules;

import net.kyori.adventure.text.TextComponent;
import org.bukkit.block.Container;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;

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

    public boolean run(final Container container, final ItemStack stack) {
        if (!(container.customName() instanceof TextComponent component)) {
            return false;
        }
        // allow hopper if no custom name was set
        final String content = component.content().toUpperCase();
        if (content.isBlank()) {
            return false;
        }
        final String itemName = stack.getType().name().toUpperCase();

        // check item type
        return !this.matchesAny(content.split(",\\s*"), itemName);
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
        if (this.run(container, event.getItem())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryPickupItemEvent(final InventoryPickupItemEvent event) {
        if (event.getInventory().getHolder() instanceof Container container) {
            if (this.run(container, event.getItem().getItemStack())) {
                event.setCancelled(true);
            }
        }
    }

}
