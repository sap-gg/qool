package gg.sap.smp.itemremover.modules;

import org.bukkit.entity.Phantom;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

public class AntiPhantomModule implements Listener {

    @EventHandler
    public void onSpawn(final CreatureSpawnEvent event) {
        if (event.getEntity() instanceof Phantom) {
            event.setCancelled(true);
        }
    }

}
