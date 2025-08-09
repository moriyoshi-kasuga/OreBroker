package github.moriyoshi.orebroker;

import org.bukkit.World.Environment;
import org.bukkit.event.HandlerList;
import org.bukkit.Bukkit;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.plugin.java.JavaPlugin;

import github.moriyoshi.orebroker.command.OreBrokerCommand;
import github.moriyoshi.orebroker.lib.MarketEngine;
import github.moriyoshi.orebroker.lib.OreBrokerListener;
import github.moriyoshi.orebroker.lib.ScoreboardManager;
import lombok.Getter;
import lombok.val;

public class OreBroker extends JavaPlugin {
    @Getter
    private static OreBroker instance;

    @Override
    public void onEnable() {
        instance = this;

        initWorld();
        initCommand();
        MarketEngine.init(this);
        ScoreboardManager.getInstance().start();

        Bukkit.getPluginManager().registerEvents(OreBrokerListener.getInstance(), this);
    }

    @Override
    public void onDisable() {
        HandlerList.unregisterAll(OreBrokerListener.getInstance());
        ScoreboardManager.getInstance().stop();
    }

    private void initCommand() {
        val orebroker = new OreBrokerCommand();
        val orebrokerCommand = this.getCommand("orebroker");
        if (orebrokerCommand != null) {
            orebrokerCommand.setTabCompleter(orebroker);
            orebrokerCommand.setExecutor(orebroker);
        }
    }

    private void initWorld() {
        new WorldCreator("lobby")
                .environment(Environment.NORMAL)
                .type(WorldType.FLAT)
                .generateStructures(false)
                .createWorld();
        new WorldCreator("game")
                .environment(Environment.NORMAL)
                .type(WorldType.NORMAL)
                .generateStructures(true)
                .createWorld();
    }
}
