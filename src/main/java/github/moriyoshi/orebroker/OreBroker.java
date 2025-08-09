package github.moriyoshi.orebroker;

import org.bukkit.World.Environment;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.plugin.java.JavaPlugin;

import github.moriyoshi.orebroker.command.OreBrokerCommand;
import lombok.Getter;
import lombok.val;

public class OreBroker extends JavaPlugin {
    @Getter
    private static OreBroker instance;

    @Override
    public void onEnable() {
        initWorld();
        initCommand();

        OreBroker.instance = this;
    }

    @Override
    public void onDisable() {

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
