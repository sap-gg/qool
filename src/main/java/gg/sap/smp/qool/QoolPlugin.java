package gg.sap.smp.qool;

import gg.sap.smp.qool.altar.Bootstrap;
import gg.sap.smp.qool.modules.*;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class QoolPlugin extends JavaPlugin {

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
        final QuickCraftModule quickCraftModule = new QuickCraftModule(this);
        Objects.requireNonNull(this.getCommand("quickcraft")).setExecutor(quickCraftModule.quickCraftCommand);
        Objects.requireNonNull(this.getCommand("quicksched")).setExecutor(quickCraftModule.quickScheduleCommand);

        // Compressed Stuff
        new CompressedItemsModule(this);

        // Anti Phantom
        pluginManager.registerEvents(new AntiPhantomModule(), this);

        // Safari Net
        new SafariNetModule(this);

        // Exchange
        final ExchangeCommand exchange = new ExchangeCommand();
        Objects.requireNonNull(this.getCommand("exchange")).setExecutor(exchange);
        pluginManager.registerEvents(exchange, this);

        // Altar
        Bootstrap.init(this);
    }

}
