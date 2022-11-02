package gg.sap.smp.itemremover;

import gg.sap.smp.itemremover.modules.DevNullModule;
import gg.sap.smp.itemremover.modules.DumpCommand;
import gg.sap.smp.itemremover.modules.TrashCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class ItemRemover extends JavaPlugin {

    @Override
    public void onEnable() {
        final DevNullModule devNull = new DevNullModule();
        Objects.requireNonNull(this.getCommand("devnull")).setExecutor(devNull);
        this.getServer().getPluginManager().registerEvents(devNull, this);

        Objects.requireNonNull(this.getCommand("trash")).setExecutor(new TrashCommand());

        Objects.requireNonNull(this.getCommand("dump")).setExecutor(new DumpCommand());
    }

}
