package gg.sap.smp.qool.altar;

import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public class Bootstrap {

    public static void init(final JavaPlugin plugin) {
        // Listeners
        final PluginManager pluginManager = plugin.getServer().getPluginManager();
        pluginManager.registerEvents(new EssenceListener(), plugin);
        pluginManager.registerEvents(new Altar(), plugin);

        // Commands
        final DebugCommand debugCommand = new DebugCommand();
        Objects.requireNonNull(plugin.getCommand("illegalitems")).setExecutor(debugCommand);
        pluginManager.registerEvents(debugCommand, plugin);
    }

}
