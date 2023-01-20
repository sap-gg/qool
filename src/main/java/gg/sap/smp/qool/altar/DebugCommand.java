package gg.sap.smp.qool.altar;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

public class DebugCommand implements CommandExecutor, Listener {

    @Override
    public boolean onCommand(final @NotNull CommandSender sender,
                             final @NotNull Command command,
                             final @NotNull String label,
                             final @NotNull String[] args) {
        if (!(sender instanceof final Player player)) {
            sender.sendMessage("This command is intended for players");
            return true;
        }

        // don't allow debug commands on the SMP
        if (!player.isOp()) {
            player.sendMessage("This command is only intended for debugging and not regular play");
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("altar")) {
            final ItemStack stack = new ItemStack(Material.ENCHANTING_TABLE);
            final ItemMeta meta = stack.getItemMeta();
            meta.displayName(Component.text("Altar", NamedTextColor.LIGHT_PURPLE));
            meta.getPersistentDataContainer().set(
                    Altar.ALTAR_BLOCK_KEY,
                    PersistentDataType.BYTE,
                    (byte) 1
            );
            stack.setItemMeta(meta);
            player.getInventory().addItem(stack);
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("book")) {
            // combine item in off-hand with item in hand
            final ItemStack hand = player.getInventory().getItem(EquipmentSlot.HAND);
            final ItemStack offHand = player.getInventory().getItem(EquipmentSlot.OFF_HAND);

            final BlackMagic.CombineBookResult result = BlackMagic.combineBook(offHand, hand);
            if (result == BlackMagic.CombineBookResult.SUCCESS) {
                player.sendMessage("OK!");
                player.getInventory().setItem(EquipmentSlot.HAND, null);
            } else {
                player.sendMessage("ERR: " + result.name());
            }
            return true;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("increment")) {
            final Enchantment enchantment = Enchantment.getByKey(NamespacedKey.minecraft(args[1]));
            if (enchantment == null) {
                player.sendMessage("cannot find that enchantment");
                return true;
            }
            final ItemStack stack = player.getInventory().getItem(EquipmentSlot.HAND);
            BlackMagic.incrementEnchantment(stack, enchantment);
            player.sendMessage("OK!");
            return true;
        }

        return false;
    }

    @EventHandler
    public void onDamage(final EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof final Player player)) {
            return;
        }
        final ItemStack stack = player.getInventory().getItem(EquipmentSlot.HAND);
        if (!stack.hasItemMeta()) {
            return;
        }
        final PersistentDataContainer pdc = stack.getItemMeta().getPersistentDataContainer();

        // check if stack was illegally enchanted
        if (BlackMagic.getIllegalEnchantments(pdc).size() <= 0) {
            return;
        }

        player.sendMessage("Damage to " + event.getEntity().getType().name() + " :: " +
                event.getDamage() + ", final: " + event.getFinalDamage());
    }

}
