package gg.sap.smp.itemremover;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class ItemRemover extends JavaPlugin {

    @Override
    public void onEnable() {
        final DevNull devNull = new DevNull();
        Objects.requireNonNull(this.getCommand("trash")).setExecutor(devNull);
        this.getServer().getPluginManager().registerEvents(devNull, this);

        Objects.requireNonNull(this.getCommand("devnull")).setExecutor(new DevNull());
    }

}
