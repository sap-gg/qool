package gg.sap.smp.itemremover;

import gg.sap.smp.itemremover.modules.*;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class ItemRemover extends JavaPlugin {

    @Override
    public void onEnable() {
        // devnull
        // /devnull [...]
        final DevNullModule devNull = new DevNullModule();
        Objects.requireNonNull(this.getCommand("devnull")).setExecutor(devNull);
        this.getServer().getPluginManager().registerEvents(devNull, this);

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
        this.getServer().getPluginManager().registerEvents(new ElevatorModule(), this);
    }

}
