package gg.sap.smp.itemremover.modules;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Phantom;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.inventory.EquipmentSlot;

public class AntiPhantomModule implements Listener {

    @EventHandler
    public void onSpawn(final CreatureSpawnEvent event) {
        if (!(event.getEntity() instanceof Phantom phantom)) {
            return;
        }
        if (!(phantom.getTarget() instanceof Player player)) {
            return;
        }
        // remove phantoms if player is wearing netherite helmet
        if (Material.NETHERITE_HELMET.equals(
                player.getInventory().getItem(EquipmentSlot.HEAD).getType()
        )) {
            phantom.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, event.getLocation(), 1);
            phantom.remove(); // weg mit dem Ficker
        }
    }

}
