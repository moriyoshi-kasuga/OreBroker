package github.moriyoshi.orebroker;

import org.bukkit.World.Environment;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.plugin.java.JavaPlugin;

import lombok.Getter;

public class OreBroker extends JavaPlugin {
    @Getter
    private static OreBroker instance;

    @Override
    public void onEnable() {
        initWorld();

        OreBroker.instance = this;
    }

    @Override
    public void onDisable() {

    }

    private void initWorld() {
        new WorldCreator("lobby")
                .environment(Environment.NORMAL)
                .type(WorldType.FLAT)
                .generateStructures(false)
                .createWorld();
    }
}
