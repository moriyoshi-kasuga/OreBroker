package github.moriyoshi.orebroker;

import org.bukkit.World.Environment;
import org.bukkit.event.HandlerList;
import org.bukkit.Bukkit;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.plugin.java.JavaPlugin;

import github.moriyoshi.orebroker.command.OreBrokerCommand;
import github.moriyoshi.orebroker.lib.OreBrokerListener;
import lombok.Getter;
import lombok.val;

public class OreBroker extends JavaPlugin {
    @Getter
    private static OreBroker instance;

    @Override
    public void onEnable() {
        initWorld();
        initCommand();

        Bukkit.getPluginManager().registerEvents(OreBrokerListener.getInstance(), this);

        OreBroker.instance = this;
    }

    @Override
    public void onDisable() {
        HandlerList.unregisterAll(OreBrokerListener.getInstance());
    }

    private void initCommand() {
        val orebroker = new OreBrokerCommand(this);
        val orebrokerCommand = this.getCommand("orebroker");
        orebrokerCommand.setTabCompleter(orebroker);
        orebrokerCommand.setExecutor(orebroker);
    }

    private void initWorld() {
        new WorldCreator("lobby")
                .environment(Environment.NORMAL)
                .type(WorldType.FLAT)
                .generateStructures(false)
                .createWorld();
    }
}
