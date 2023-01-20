package gg.sap.smp.qool.altar;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.EnchantingTable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class Altar implements Listener {

    public static final NamespacedKey ALTAR_BLOCK_KEY = new NamespacedKey("sap-gg", "ii-altar-state");

    public static boolean isAltar(final ItemStack stack) {
        if (!stack.hasItemMeta()) {
            return false;
        }
        return stack.getItemMeta().getPersistentDataContainer().has(Altar.ALTAR_BLOCK_KEY);
    }

    public static boolean isAltar(final Block block) {
        if (!(block.getState() instanceof final EnchantingTable table)) {
            return false;
        }
        return table.getPersistentDataContainer().has(Altar.ALTAR_BLOCK_KEY);
    }

    public static boolean isEnvironmentValid(final Block block) {
        Location location = block.getLocation().clone();
        // check if netherite blocks under enchantment table
        /*
        new BlockChecker(block)
            .expect(END_ROD, NORTH, SOUTH, WEST, EAST)
            .down()
            .expect(NETHERITE_BLOCK, NORTH, SOUTH, WEST, EAST)
            .
         */
        return false;
    }

    @EventHandler
    public void onBlockPlace(final BlockPlaceEvent event) {
        final ItemStack stack = event.getItemInHand();
        if (!isAltar(stack)) {
            return;
        }
        final Block block = event.getBlock();
        if (!(block.getState() instanceof final EnchantingTable table)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("An error occurred.");
            return;
        }
        table.getPersistentDataContainer().set(Altar.ALTAR_BLOCK_KEY, PersistentDataType.BYTE, (byte) 1);
        block.getWorld().spawnParticle(Particle.HEART, block.getLocation().toCenterLocation(), 100, 8, 3, 8);
    }

}
