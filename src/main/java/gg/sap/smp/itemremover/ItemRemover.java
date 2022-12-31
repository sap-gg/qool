package gg.sap.smp.itemremover;

import gg.sap.smp.itemremover.modules.*;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class ItemRemover extends JavaPlugin {

    @Override
    public void onEnable() {
        final PluginManager pluginManager = this.getServer().getPluginManager();

        // devnull
        // /devnull [...]
        final DevNullModule devNull = new DevNullModule();
        Objects.requireNonNull(this.getCommand("devnull")).setExecutor(devNull);
        pluginManager.registerEvents(devNull, this);

        // Trashbin
        // /trash
        Objects.requireNonNull(this.getCommand("trash")).setExecutor(new TrashCommand());

        // Item-Dumper
        // /dump [...]
        Objects.requireNonNull(this.getCommand("dump")).setExecutor(new DumpCommand());

        // Magnet
        // /magnet <radius>
        Objects.requireNonNull(this.getCommand("magnet")).setExecutor(new MagnetCommand());

        // Elevator
        pluginManager.registerEvents(new ElevatorModule(), this);

        // Hopper Filter
        pluginManager.registerEvents(new HopperFilter(), this);

        // Auto Crafter
        Objects.requireNonNull(this.getCommand("quickcraft")).setExecutor(new QuickCraftCommand());

        // Compressed Stuff
        new CompressedItemsModule(this);

        // Anti Phantom
        pluginManager.registerEvents(new AntiPhantomModule(), this);

        // Safari Net
        new SafariNetModule(this);
    }

}
