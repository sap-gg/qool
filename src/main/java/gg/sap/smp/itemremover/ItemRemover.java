package gg.sap.smp.itemremover;

import gg.sap.smp.itemremover.commands.DumpCommand;
import gg.sap.smp.itemremover.commands.TrashCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class ItemRemover extends JavaPlugin {

    @Override
    public void onEnable() {
        final DevNull devNull = new DevNull();
        Objects.requireNonNull(this.getCommand("devnull")).setExecutor(devNull);
        this.getServer().getPluginManager().registerEvents(devNull, this);

        Objects.requireNonNull(this.getCommand("trash")).setExecutor(new TrashCommand());

        Objects.requireNonNull(this.getCommand("dump")).setExecutor(new DumpCommand());
    }

}
