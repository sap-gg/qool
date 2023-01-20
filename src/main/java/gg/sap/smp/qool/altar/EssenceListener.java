package gg.sap.smp.qool.altar;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.Random;

public class EssenceListener implements Listener {

    public static final Random RANDOM = new Random();

    /*
     * Consume 100 Level
     * Kill Player
     * Sacrifice 10 Mobs around altar
     */

    @EventHandler
    public void onEntityDeath(final EntityDeathEvent event) {
        final Essence essence = Essence.byEntityType(event.getEntityType());
        if (essence != null && essence.poll(RANDOM)) {
            // drop item
            final Location location = event.getEntity().getLocation().clone();
            final World world = location.getWorld();
            world.dropItem(location, Essence.getItem(essence));

            // some particle stuff
            world.spawnParticle(Particle.HEART, location, 100, 10, 10, 10);
            world.spawnParticle(Particle.VILLAGER_HAPPY, location, 100, 10, 10, 10);
            world.spawnParticle(Particle.EXPLOSION_HUGE, location, 1);
        }
    }

}
